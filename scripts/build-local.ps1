param([switch]$DebugBuild)
$ErrorActionPreference = 'Stop'

$aetherRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$aetherDesktop = Join-Path $aetherRoot 'apps\desktop'
$localRust = Join-Path $aetherRoot '.tools\rustup'
if (Test-Path -LiteralPath $localRust) {
    $env:RUSTUP_HOME = $localRust
    $env:RUSTUP_TOOLCHAIN = 'stable'
}

Push-Location $aetherRoot
try {
    $buildArgs = @('--filter', 'desktop', 'exec', 'tauri', 'build', '--no-bundle')
    if ($DebugBuild) { $buildArgs += '--debug' }
    & pnpm @buildArgs
    if ($LASTEXITCODE -ne 0) { throw 'A compilação falhou. A cópia de uso diário não foi alterada.' }

    $profile = if ($DebugBuild) { 'debug' } else { 'release' }
    $binary = Join-Path $aetherDesktop "src-tauri\target\$profile\aether_desktop.exe"
    $jar = Join-Path $aetherDesktop 'src-tauri\resources\aether-api.jar'
    if (!(Test-Path -LiteralPath $binary) -or !(Test-Path -LiteralPath $jar)) { throw 'A compilação não produziu os arquivos esperados.' }

    # Fixed launch path: a taskbar pin remains valid after future local builds.
    $dailyDir = Join-Path $aetherRoot 'dist\Aether'
    $dailyExe = Join-Path $dailyDir 'Aether.exe'
    foreach ($running in @(Get-Process -Name Aether -ErrorAction SilentlyContinue)) {
        if ($running.Path -eq $dailyExe) {
            $null = $running.CloseMainWindow()
            if (!$running.WaitForExit(15000)) { throw 'Feche o Aether para substituir a versão de uso diário.' }
        }
    }
    $dailyResources = Join-Path $dailyDir 'resources'
    New-Item -ItemType Directory -Force -Path $dailyResources | Out-Null
    Copy-Item -LiteralPath $binary -Destination $dailyExe -Force
    Copy-Item -LiteralPath $jar -Destination (Join-Path $dailyResources 'aether-api.jar') -Force
    @"
Aether — uso diário

Abra Aether.exe. Mantenha a pasta resources junto do executável.
Com o app aberto, clique com o botão direito no ícone da barra de tarefas e escolha Fixar na barra de tarefas.

Requer o Java já instalado neste computador. Os dados e credenciais continuam no perfil do usuário, fora desta pasta.
Para atualizar esta cópia a partir do código local: pnpm build:app
Este processo ainda não instala um serviço de atualizações automáticas.
"@ | Set-Content -LiteralPath (Join-Path $dailyDir 'LEIA-ME.txt') -Encoding UTF8
    Write-Output "Aplicativo pronto: $dailyExe"
}
finally { Pop-Location }
