import React from "react";
import { invoke } from "@tauri-apps/api/core";
import { Download, Upload, Trash2, Cable, ArrowRight } from "lucide-react";
import { API, request, body, runtimeProfile } from "./api";
import { Badge, Modal, Note, Panel } from "./ui";

export function Welcome({ setup, restore }: { setup: () => void; restore: () => void }) {
  return <section className="welcome-card page-enter"><span className="asset-icon"><Cable size={25} /></span><span className="startup-eyebrow">BEM-VINDO AO AETHER</span><h2>Seu espaço financeiro começa aqui</h2><p>Conecte seus bancos para organizar contas, cartões e investimentos. Se você já usava o Aether, pode trazer seu histórico com um backup.</p><div className="welcome-actions"><button className="button primary" onClick={setup}>Configurar minhas conexões<ArrowRight size={16} /></button><button className="button secondary" onClick={restore}><Upload size={16} />Restaurar um backup</button></div><small>O histórico fica neste computador. As credenciais são configuradas separadamente.</small></section>;
}
export function DataSettings() {
  const [operation,setOperation]=React.useState<"export" | "reset" | "restore" | null>(null), [confirmation,setConfirmation]=React.useState(""), [file,setFile]=React.useState<File | null>(null), [busy,setBusy]=React.useState(false), [error,setError]=React.useState(""), [resetDone,setResetDone]=React.useState(false);
  const [password,setPassword]=React.useState(""), [repeatPassword,setRepeatPassword]=React.useState("");
  const [info,setInfo]=React.useState<{backupDirectory: string} | null>(null);
  React.useEffect(()=>{void request<{backupDirectory: string}>("/api/data").then(setInfo).catch(e=>setError(e.message));},[]);
  function close(){ if(!busy){setOperation(null);setPassword("");setRepeatPassword("");setError("");} }
  function open(next: "export" | "restore" | "reset"){setOperation(next);setConfirmation("");setPassword("");setRepeatPassword("");setError("");}
  async function exportBackup() {
    const response=await fetch(`${API}/api/data/export`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({password}),cache:"no-store"});
    if(!response.ok){const result=await response.json().catch(()=>null);throw new Error(result?.message || "Não foi possível exportar o backup.");}
    const url=URL.createObjectURL(await response.blob());const link=document.createElement("a");link.href=url;link.download=`aether-backup-${new Date().toISOString().slice(0,10)}.aetherbackup`;link.click();setTimeout(()=>URL.revokeObjectURL(url),60_000);
  }
  async function apply() {
    setBusy(true);setError("");
    try {
      if(password.length<12 || password.length>1024 || !password.trim())throw new Error("Use uma senha de pelo menos 12 caracteres.");
      if(password!==repeatPassword)throw new Error("As senhas não coincidem.");
      if(operation === "export") {
        await exportBackup();setOperation(null);setPassword("");setRepeatPassword("");return;
      }
      if(operation === "reset") {
        await request("/api/data/reset",{method:"POST",...body({confirmation,password})});
        setResetDone(true);
        if("__TAURI_INTERNALS__" in window) {
          try { await invoke("delete_pluggy_credentials"); }
          catch { throw new Error("Os dados foram removidos e a integração foi desativada. Não foi possível remover as credenciais do cofre do Windows. Remova a entrada deste perfil em Credenciais do Windows antes de compartilhar o computador."); }
        }
      } else {
        if(!file)throw new Error("Escolha um backup antes de continuar.");
        const form=new FormData();form.append("file",file);form.append("confirmation",confirmation);form.append("password",password);
        const response=await fetch(`${API}/api/data/restore`,{method:"POST",body:form,cache:"no-store"});
        if(!response.ok){const result=await response.json().catch(()=>null);throw new Error(result?.message || "Não foi possível restaurar o backup.");}
      }
      setPassword("");setRepeatPassword("");window.location.reload();
    }catch(e){setError((e as Error).message);}finally{setBusy(false);}
  }
  const passwordValid=password.length>=12&&password.length<=1024&&!!password.trim()&&password===repeatPassword;
  return <div className="stack"><Panel title="Dados e backup" detail="Seu histórico protegido por senha, de uma instalação à outra."><Badge tone="success">Backups criptografados · AES-256-GCM</Badge><div className="data-actions"><div><h3>Exportar histórico protegido</h3><p>Inclui contas, transações, investimentos, categorias, regras, nomes e imagens em um arquivo .aetherbackup criptografado. As credenciais do cofre não fazem parte do arquivo.</p></div><button className="button secondary" disabled={busy} onClick={()=>open("export")}><Download size={16} />Exportar backup</button></div><div className="data-actions"><div><h3>Restaurar em outra instalação</h3><p>Escolha o arquivo protegido e informe sua senha. Backups antigos .sqlite/.db ainda podem ser importados; as novas cópias serão criptografadas.</p><label>Arquivo de backup<input type="file" accept=".aetherbackup,.sqlite,.db,application/octet-stream,application/vnd.sqlite3,application/x-sqlite3" disabled={busy} onChange={e=>{setError("");const selected=e.target.files?.[0] || null;if(selected && selected.size>100*1024*1024+1024){setError("O arquivo deve ter até 100 MB de dados.");setFile(null);e.target.value="";}else setFile(selected);}} /></label></div><button className="button secondary" disabled={busy || !file} onClick={()=>open("restore")}><Upload size={16} />Restaurar backup</button></div><Note>As cópias automáticas antes de restaurar ou remover também são criptografadas com a senha informada na operação. A senha não é salva nem pode ser recuperada pelo Aether.</Note><Note>Arquivos .sqlite/.db criados anteriormente continuam sem criptografia. Para protegê-los, restaure em um perfil isolado e exporte no novo formato. O banco local em uso continua separado desta proteção dos backups.</Note>{runtimeProfile && <p className="data-path"><strong>{runtimeProfile.isolated ? "Perfil de teste isolado" : "Dados deste perfil"}</strong><code>{runtimeProfile.dataDir}</code></p>}{info && <p className="data-path"><strong>Backups automáticos</strong><code>{info.backupDirectory}</code></p>}</Panel>
    <Panel className="danger-panel" title="Voltar ao primeiro acesso" detail="Remova os dados deste perfil e comece do zero."><div className="data-actions"><div><h3>Remover todos os dados</h3><p>Apaga conexões locais, contas, cartões, transações, investimentos, categorias, regras, nomes e imagens. Remove as credenciais salvas deste perfil e desativa a integração. Compartilhamentos no Meu Pluggy e arquivos de backup permanecem disponíveis.</p></div><button className="button secondary danger-text" disabled={busy} onClick={()=>open("reset")}><Trash2 size={16} />Remover todos os dados</button></div><Note>Para revogar também os consentimentos bancários, gerencie os compartilhamentos no Meu Pluggy. A remoção local não faz essa revogação.</Note></Panel>
    {error && !operation && <p className="inline-error" role="alert">{error}</p>}
    {operation && <Modal title={operation === "export" ? "Proteger e exportar seu histórico" : operation === "reset" ? "Remover todos os dados deste perfil?" : "Restaurar um backup protegido"} onClose={close}><form className="form-stack" onSubmit={e=>{e.preventDefault();void apply();}}><Badge tone={operation === "export" ? "success" : "warning"}>{runtimeProfile?.isolated ? "AMBIENTE DE TESTE ISOLADO" : "SEU PERFIL PRINCIPAL"}</Badge><p className="muted">{operation === "export" ? "Crie uma senha exclusiva para este arquivo. Você precisará dela para abrir o histórico em qualquer instalação." : operation === "reset" ? "O Aether voltará à tela de boas-vindas. Sua cópia automática será protegida pela senha abaixo." : `O arquivo ${file?.name} substituirá o histórico deste perfil. Informe a senha dele; ela também protegerá a cópia automática dos dados atuais. Para um arquivo antigo sem criptografia, crie uma senha agora.`}</p><label>{operation === "restore" ? "Senha do backup e da cópia automática" : "Crie uma senha para o backup"}<input type="password" value={password} onChange={e=>setPassword(e.target.value)} minLength={12} maxLength={1024} autoComplete={operation === "restore" ? "current-password" : "new-password"} spellCheck={false} disabled={busy || resetDone} required /></label><label>Confirme a senha<input type="password" value={repeatPassword} onChange={e=>setRepeatPassword(e.target.value)} minLength={12} maxLength={1024} autoComplete="new-password" disabled={busy || resetDone} required /></label><Note>Guarde a senha em um gerenciador de senhas. Sem ela, não será possível recuperar este backup.</Note>{operation !== "export" && <label>Digite {operation === "reset" ? "APAGAR" : "RESTAURAR"} para confirmar<input value={confirmation} onChange={e=>setConfirmation(e.target.value)} autoComplete="off" spellCheck={false} disabled={busy || resetDone} /></label>}{error && <p className="inline-error" role="alert">{error}</p>}<div className="modal-actions">{resetDone ? <button type="button" className="button primary" onClick={()=>window.location.reload()}>Voltar ao início</button> : <><button type="button" className="button secondary" disabled={busy} onClick={close}>Cancelar</button><button className="button primary" disabled={busy || !passwordValid || (operation !== "export" && confirmation !== (operation === "reset" ? "APAGAR" : "RESTAURAR"))}>{busy ? "Protegendo seus dados…" : operation === "export" ? "Criptografar e exportar" : operation === "reset" ? "Remover dados" : "Restaurar histórico"}</button></>}</div></form></Modal>}
  </div>;
}
