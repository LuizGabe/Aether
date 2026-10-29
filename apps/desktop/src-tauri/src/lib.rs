use std::path::{Path, PathBuf};
use std::net::{SocketAddr, TcpStream};
use std::process::{Child, Command, Stdio};
use std::sync::Mutex;
use std::time::Duration;
use tauri::{Manager, RunEvent};

struct BackendProcess(Mutex<Option<Child>>);

#[derive(Clone, serde::Serialize)]
#[serde(rename_all = "camelCase")]
struct RuntimeConfig {
    data_dir: PathBuf,
    api_port: u16,
    isolated: bool,
    #[serde(skip)]
    credential_service: String,
}

fn runtime_config() -> Result<RuntimeConfig, String> {
    let args: Vec<String> = std::env::args().collect();
    let value = |flag: &str| -> Result<Option<String>, String> {
        if let Some(index) = args.iter().position(|arg| arg == flag) {
            return args.get(index + 1).filter(|v| !v.starts_with("--")).cloned().map(Some)
                .ok_or_else(|| format!("Falta um valor para {flag}."));
        }
        Ok(None)
    };
    let custom_dir = value("--data-dir")?;
    let custom_port = value("--api-port")?;
    if custom_dir.is_some() != custom_port.is_some() { return Err("Use --data-dir e --api-port juntos para um perfil isolado.".into()); }
    if custom_dir.is_none() && std::env::current_exe().ok().and_then(|p|p.parent().map(|d|d.join("aether-isolated-only"))).is_some_and(|p|p.exists()) {
        return Err("Esta cópia é de laboratório. Abra o atalho Primeiro acesso ou Restaurado, que informa a pasta e a porta isoladas.".into());
    }
    let normal_dir = resolve_backend_work_dir()?;
    let isolated = custom_dir.is_some();
    let data_dir = if let Some(path) = custom_dir {
        let path = PathBuf::from(path);
        if !path.is_absolute() { return Err("A pasta isolada deve ter um caminho absoluto.".into()); }
        std::fs::create_dir_all(&path).map_err(|e|e.to_string())?;
        let path = path.canonicalize().map_err(|e|e.to_string())?;
        if path.to_string_lossy().to_lowercase() == normal_dir.canonicalize().map_err(|e|e.to_string())?.to_string_lossy().to_lowercase() {
            return Err("O perfil isolado não pode usar a pasta principal.".into());
        }
        path
    } else { normal_dir };
    let api_port = custom_port.map(|p|p.parse::<u16>().map_err(|_|"Porta inválida.".to_string())).transpose()?.unwrap_or(38472);
    if api_port < 1024 || (isolated && api_port == 38472) { return Err("Escolha uma porta entre 1024 e 65535, diferente de 38472 no perfil isolado.".into()); }
    let credential_service = if isolated { format!("Aether Pluggy isolated {}", data_dir.to_string_lossy().to_lowercase()) } else { "Aether Pluggy".into() };
    Ok(RuntimeConfig { data_dir, api_port, isolated, credential_service })
}

#[tauri::command]
fn get_runtime_config(app: tauri::AppHandle) -> RuntimeConfig { app.state::<RuntimeConfig>().inner().clone() }

#[derive(serde::Deserialize)]
struct PluggyCredentials {
    client_id: String,
    client_secret: String,
}

#[derive(serde::Serialize)]
struct PluggyCredentialStatus {
    configured: bool,
}

pub fn run() {
    let config = runtime_config().expect("Não foi possível configurar o perfil do Aether");
    let mut context = tauri::generate_context!();
    if config.isolated {
        for window in &mut context.config_mut().app.windows {
            window.data_directory = Some(config.data_dir.join("webview"));
            window.title = format!("Aether — Teste isolado ({})", config.api_port);
        }
    }
    tauri::Builder::default()
        .manage(config)
        .manage(BackendProcess(Mutex::new(None)))
        .invoke_handler(tauri::generate_handler![
            get_runtime_config,
            get_pluggy_credential_status,
            set_pluggy_credentials,
            delete_pluggy_credentials,
            open_meu_pluggy
        ])
        .setup(|app| {
            if app.state::<RuntimeConfig>().isolated {
                if let Some(window) = app.get_webview_window("main") { let _ = window.set_title(&format!("Aether — Teste isolado ({})", app.state::<RuntimeConfig>().api_port)); }
            }
            start_backend(app.handle()).map_err(std::io::Error::other)?;
            Ok(())
        })
        .build(context)
        .expect("error while building tauri application")
        .run(|app_handle, event| {
            if let RunEvent::ExitRequested { .. } = event {
                stop_backend(app_handle);
            }
        });
}

#[tauri::command]
fn open_meu_pluggy() -> Result<(), String> {
    #[cfg(target_os = "windows")]
    {
        use std::os::windows::process::CommandExt;
        Command::new("rundll32.exe")
            .args(["url.dll,FileProtocolHandler", "https://meu.pluggy.ai/connections"])
            .creation_flags(0x08000000)
            .spawn().map_err(|error| error.to_string())?;
    }
    Ok(())
}

fn start_backend(app: &tauri::AppHandle) -> Result<(), String> {
    let config = app.state::<RuntimeConfig>();
    if local_api_is_reachable(config.api_port) {
        if config.isolated { return Err("A porta do perfil isolado já está em uso. Feche esse teste ou escolha outra porta.".into()); }
        return Ok(());
    }

    let state = app.state::<BackendProcess>();
    let mut process = state.0.lock().map_err(|_| "backend process lock poisoned")?;

    if process.is_some() {
        return Ok(());
    }

    let desktop_dir = std::env::current_dir().map_err(|error| error.to_string())?;
    let api_dir = resolve_api_dir(&desktop_dir);
    let jar_path = resolve_backend_jar(app, &api_dir);
    let data_dir = config.data_dir.clone();
    let database_url = database_url(&data_dir);

    let credentials = read_pluggy_credentials(app).ok();

    let child = if jar_path.exists() {
        let mut command = Command::new(resolve_java_command(&jar_path));
        command
            .arg("-XX:TieredStopAtLevel=1")
            .arg("-jar")
            .arg(&jar_path)
            .arg(format!("--spring.datasource.url={database_url}"))
            .arg(format!("--server.port={}", config.api_port))
            .current_dir(&data_dir)
            .stdout(backend_log(&data_dir)?)
            .stderr(backend_log(&data_dir)?);
        apply_pluggy_env(&mut command, credentials.as_ref(), config.isolated);
        hide_backend_console(&mut command);
        command.spawn()
    } else {
        let mut command = Command::new(resolve_maven_command(&api_dir));
        command
            .arg("spring-boot:run")
            .current_dir(&api_dir)
            .env("AETHER_DB_URL", &database_url)
            .env("AETHER_API_PORT", config.api_port.to_string())
            .stdout(backend_log(&data_dir)?)
            .stderr(backend_log(&data_dir)?);
        apply_pluggy_env(&mut command, credentials.as_ref(), config.isolated);
        hide_backend_console(&mut command);
        command.spawn()
    }
    .map_err(|error| error.to_string())?;

    *process = Some(child);
    Ok(())
}

fn resolve_java_command(jar_path: &Path) -> PathBuf {
    // Installed releases carry their own runtime; development can use JAVA on PATH.
    if let Some(directory) = jar_path.parent() {
        let bundled = directory.join("runtime").join("bin").join(if cfg!(windows) { "java.exe" } else { "java" });
        if bundled.is_file() { return bundled; }
    }
    PathBuf::from("java")
}

fn hide_backend_console(command: &mut Command) {
    #[cfg(target_os = "windows")]
    {
        use std::os::windows::process::CommandExt;
        command.creation_flags(0x08000000);
    }
}

fn stop_backend(app: &tauri::AppHandle) {
    let state = app.state::<BackendProcess>();
    let process_lock = state.0.lock();

    if let Ok(mut process) = process_lock {
        if let Some(child) = process.as_mut() {
            #[cfg(target_os = "windows")]
            if matches!(child.try_wait(), Ok(None)) {
                // Java's Windows launcher can spawn another JVM. Close the owned tree.
                use std::os::windows::process::CommandExt;
                let _ = Command::new("taskkill.exe")
                    .args(["/PID", &child.id().to_string(), "/T", "/F"])
                    .creation_flags(0x08000000)
                    .status();
            }
            let _ = child.kill();
            let _ = child.wait();
        }

        *process = None;
    }
}

fn resolve_api_dir(desktop_dir: &Path) -> PathBuf {
    let from_src_tauri = desktop_dir.join("..").join("..").join("api");
    if from_src_tauri.exists() {
        return from_src_tauri;
    }

    desktop_dir.join("..").join("api")
}

fn resolve_maven_command(api_dir: &Path) -> PathBuf {
    let repo_root = api_dir.join("..").join("..");
    let local_maven = repo_root
        .join(".tools")
        .join("apache-maven-3.9.16")
        .join("bin")
        .join(if cfg!(windows) { "mvn.cmd" } else { "mvn" });

    if local_maven.exists() {
        return local_maven;
    }

    PathBuf::from(if cfg!(windows) { "mvn.cmd" } else { "mvn" })
}

fn resolve_backend_jar(app: &tauri::AppHandle, api_dir: &Path) -> PathBuf {
    if let Ok(resource_dir) = app.path().resource_dir() {
        let bundled_jar = resource_dir.join("aether-api.jar");
        if bundled_jar.exists() {
            return bundled_jar;
        }

        let nested_bundled_jar = resource_dir.join("resources").join("aether-api.jar");
        if nested_bundled_jar.exists() {
            return nested_bundled_jar;
        }
    }

    let dev_resource_jar = api_dir
        .join("..")
        .join("desktop")
        .join("src-tauri")
        .join("resources")
        .join("aether-api.jar");

    if dev_resource_jar.exists() {
        return dev_resource_jar;
    }

    api_dir.join("target").join("aether-api.jar")
}

fn resolve_backend_work_dir() -> Result<PathBuf, String> {
    // MSIX hosts can virtualize AppData for their child processes. USERPROFILE
    // outside AppData is shared by Explorer/taskbar and packaged development tools.
    let profile = std::env::var_os("USERPROFILE")
        .ok_or("Não foi possível identificar a pasta do usuário para abrir os dados.")?;
    let data_dir = PathBuf::from(profile).join("Aether").join("data");
    std::fs::create_dir_all(&data_dir).map_err(|error| format!("Não foi possível abrir a pasta de dados: {error}"))?;
    Ok(data_dir)
}

fn database_url(data_dir: &Path) -> String {
    let path = data_dir.join("aether.db").to_string_lossy().replace("\\\\?\\", "").replace('\\', "/");
    format!("jdbc:sqlite:{path}")
}

fn backend_log(data_dir: &Path) -> Result<Stdio, String> {
    std::fs::OpenOptions::new().create(true).append(true)
        .open(data_dir.join("backend.log"))
        .map(Stdio::from).map_err(|error| format!("Não foi possível abrir o log local: {error}"))
}

fn local_api_is_reachable(port: u16) -> bool {
    let address = SocketAddr::from(([127, 0, 0, 1], port));
    TcpStream::connect_timeout(&address, Duration::from_millis(250)).is_ok()
}

#[tauri::command]
fn get_pluggy_credential_status(app: tauri::AppHandle) -> PluggyCredentialStatus {
    PluggyCredentialStatus {
        configured: read_pluggy_credentials(&app).is_ok(),
    }
}

#[tauri::command]
fn set_pluggy_credentials(app: tauri::AppHandle, credentials: PluggyCredentials) -> Result<PluggyCredentialStatus, String> {
    credential_entry(&app, "client_id")?
        .set_password(credentials.client_id.trim())
        .map_err(|error| error.to_string())?;
    credential_entry(&app, "client_secret")?
        .set_password(credentials.client_secret.trim())
        .map_err(|error| error.to_string())?;

    Ok(PluggyCredentialStatus { configured: true })
}

#[tauri::command]
fn delete_pluggy_credentials(app: tauri::AppHandle) -> Result<PluggyCredentialStatus, String> {
    for name in ["client_id", "client_secret"] {
        match credential_entry(&app, name)?.delete_credential() {
            Ok(()) | Err(keyring::Error::NoEntry) => (),
            Err(error) => return Err(format!("Não foi possível remover a credencial do cofre: {error}")),
        }
    }
    Ok(PluggyCredentialStatus { configured: false })
}

fn read_pluggy_credentials(app: &tauri::AppHandle) -> Result<PluggyCredentials, String> {
    let client_id = credential_entry(&app, "client_id")?
        .get_password()
        .map_err(|error| error.to_string())?;
    let client_secret = credential_entry(&app, "client_secret")?
        .get_password()
        .map_err(|error| error.to_string())?;

    if client_id.trim().is_empty() || client_secret.trim().is_empty() {
        return Err("stored Pluggy credentials are empty".to_string());
    }

    Ok(PluggyCredentials { client_id, client_secret })
}

fn apply_pluggy_env(command: &mut Command, credentials: Option<&PluggyCredentials>, isolated: bool) {
    if isolated {
        for name in ["AETHER_PLUGGY_CLIENT_ID", "AETHER_PLUGGY_CLIENT_SECRET", "PLUGGY_CLIENT_ID", "PLUGGY_CLIENT_SECRET"] { command.env_remove(name); }
    }
    if let Some(credentials) = credentials {
        command.env("AETHER_PLUGGY_CLIENT_ID", credentials.client_id.trim());
        command.env("AETHER_PLUGGY_CLIENT_SECRET", credentials.client_secret.trim());
    }
}

fn credential_entry(app: &tauri::AppHandle, name: &str) -> Result<keyring::Entry, String> {
    keyring::Entry::new(&app.state::<RuntimeConfig>().credential_service, name).map_err(|error| error.to_string())
}
