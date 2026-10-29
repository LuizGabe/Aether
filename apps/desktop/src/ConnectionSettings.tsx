import { BankAvatar, BankName } from "./VisualIdentity";
import React from "react";
import { invoke } from "@tauri-apps/api/core";
import { Cable, Check, ChevronDown, ExternalLink, KeyRound, RefreshCw } from "lucide-react";
import { request, body } from "./api";
import { readPluggyError } from "./pluggy-recovery";
import { Badge, date, Empty, Note, Panel } from "./ui";
import type { Connection } from "./types";

type Settings = { pluggyCredentialsConfigured: boolean; pluggyCredentialsSource: string };
type RemoteItem = { id: string; connector?: { name?: string }; status?: string };
type Job = { id: string; status: string; errorMessage: string | null; createdAt: string | null; metadata: string | null };
type Sync = { status: string; message: string; accountsSynced: number; transactionsSynced: number; investmentsSynced: number };
type Widget = { init(): Promise<void>; destroy(): void };
declare global { interface Window { PluggyConnect?: new (options: Record<string, unknown>) => Widget } }

export function ConnectionSettings({ connections, onChange, notify }: { connections: Connection[]; onChange: () => Promise<void>; notify: (message: string, error?: boolean) => void }) {
  const [settings, setSettings] = React.useState<Settings | null>(null), [jobs, setJobs] = React.useState<Job[]>([]);
  const [clientId, setClientId] = React.useState(""), [clientSecret, setClientSecret] = React.useState(""), [itemId, setItemId] = React.useState("");
  const [items, setItems] = React.useState<RemoteItem[]>([]), [busy, setBusy] = React.useState(false), [widgetOpen, setWidgetOpen] = React.useState(false);
  const [connectionName, setConnectionName] = React.useState("");
  const captured = React.useRef(new Set<string>());
  const refresh = React.useCallback(async () => { const [s, j] = await Promise.all([request<Settings>("/api/settings"), request<Job[]>("/api/sync-jobs")]); setSettings(s); setJobs(j); }, []);
  React.useEffect(() => { void refresh().catch(e => notify(e.message, true)); }, [refresh]);

  async function run(action: () => Promise<void>) { setBusy(true); try { await action(); } catch (e) { notify(e instanceof Error ? e.message : "Não foi possível concluir.", true); } finally { setBusy(false); await refresh().catch(() => {}); await onChange(); } }
  async function sync(item: RemoteItem) {
    const result = await request<Sync>("/api/connections/sync-by-item-id", { method: "POST", ...body({ itemId: item.id, displayName: connectionName.trim() || (item.connector?.name === "MeuPluggy" ? undefined : item.connector?.name) }) });
    notify(result.status === "SUCCESS" ? `${result.accountsSynced} produtos e ${result.transactionsSynced} transações importados.` : result.message, result.status !== "SUCCESS");
  }
  async function capture(item: RemoteItem) {
    if (captured.current.has(item.id)) return;
    captured.current.add(item.id);
    try { await request("/api/connections", { method: "POST", ...body({ itemId: item.id, displayName: connectionName.trim() || undefined }) }); } catch (e) { captured.current.delete(item.id); throw e; }
  }
  async function recover(error: unknown) {
    const recovered = readPluggyError(error);
    if (!recovered.itemIds.length) { notify(recovered.duplicate ? "Essa conta já está conectada. Importe o Item ID existente nas opções avançadas ou confira o compartilhamento no Meu Pluggy." : recovered.message, true); return; }
    await run(async () => {
      const found = await Promise.allSettled(recovered.itemIds.map(async id => { const item = await request<RemoteItem>(`/api/pluggy/items/${encodeURIComponent(id)}`); await capture(item); return item; }));
      const available = found.flatMap(r => r.status === "fulfilled" ? [r.value] : []);
      setItems(available);
      if (available.length === 1) await sync(available[0]); else notify(available.length ? "Conexões recuperadas. Escolha uma nas opções avançadas." : "Não foi possível recuperar os IDs informados pela Pluggy.", !available.length);
    });
  }
  async function connect() {
    if (widgetOpen || busy) return;
    await run(async () => {
      const { accessToken } = await request<{ accessToken: string }>("/api/pluggy/connect-tokens", { method: "POST", ...body({}) });
      if (!window.PluggyConnect) await new Promise<void>((resolve, reject) => {
        const script = document.createElement("script"); script.src = "https://cdn.pluggy.ai/pluggy-connect/v2.8.2/pluggy-connect.js";
        script.onload = () => resolve(); script.onerror = () => { script.remove(); reject(new Error("Não foi possível carregar o Pluggy Connect.")); }; document.head.appendChild(script);
      });
      if (!window.PluggyConnect) throw new Error("Widget de conexão indisponível.");
      const widget = new window.PluggyConnect({ connectToken: accessToken, includeSandbox: false, selectedConnectorId: 200, language: "pt", theme: "light", forceOauthInBrowser: true,
        onEvent: async (event: { event: string; item?: RemoteItem }) => { if (event.event === "ITEM_RESPONSE" && event.item?.id) try { await capture(event.item); } catch (e) { notify((e as Error).message, true); } },
        onSuccess: async ({ item }: { item?: RemoteItem }) => { if (item?.id) await run(async () => { await capture(item); await sync(item); }); },
        onError: recover, onClose: () => { setWidgetOpen(false); void onChange(); void refresh(); },
      });
      setWidgetOpen(true); try { await widget.init(); } catch (e) { setWidgetOpen(false); throw e; }
    });
  }
  async function openPassport() { try { if ("__TAURI_INTERNALS__" in window) await invoke("open_meu_pluggy"); else window.open("https://meu.pluggy.ai/connections", "_blank", "noopener,noreferrer"); } catch { notify("Abra https://meu.pluggy.ai/connections no navegador para gerenciar seus compartilhamentos.", true); } }

  return <div className="stack">
    <Panel title="Instituições conectadas" detail="Gerencie o compartilhamento dos seus bancos com o Aether." action={<button className="button primary" disabled={busy || widgetOpen || !settings?.pluggyCredentialsConfigured || !connectionName.trim()} onClick={() => void connect()}><Cable size={16} />Conectar instituição</button>}>
      <label className="new-connection-name">Nome do banco para a próxima conexão<input maxLength={100} value={connectionName} onChange={e => setConnectionName(e.target.value)} placeholder="Ex.: C6 Bank ou Inter" disabled={busy || widgetOpen} /><small>Conecte um banco por vez. O Meu Pluggy pode fornecer um nome genérico; este nome identifica o banco no Aether.</small></label>
      <div className="connection-list">{connections.filter(c => c.status !== "MISSING").map(c => <div className="connection-row" key={c.id}><BankAvatar name={c.name} /><div className="grow"><strong><BankName name={c.name} /></strong><small>Última importação: {date(c.lastSuccessAt)} · Último lançamento: {date(c.lastTransaction)}</small>{c.error && <small className="text-warning">{c.error}</small>}</div><Badge tone={c.error ? "warning" : "success"}>{c.error ? "Verificar" : "Conectado"}</Badge><button className="button secondary small" disabled={busy} onClick={() => void run(async () => { const result = await request<Sync>(`/api/connections/${c.id}/sync`, { method: "POST" }); notify(result.message, result.status !== "SUCCESS"); })}><RefreshCw size={14} />Sincronizar</button></div>)}</div>
      {connections.every(c => c.status === "MISSING") && <Empty title="Conecte sua primeira instituição">Escolha MeuPluggy e autorize o compartilhamento de cada banco.</Empty>}
      <div className="panel-footer"><Note>Os dados disponíveis dependem do consentimento e da atualização no Meu Pluggy. Importar no Aether não força uma nova consulta ao banco.</Note><button className="text-button" onClick={() => void openPassport()}>Gerenciar no Meu Pluggy<ExternalLink size={14} /></button></div>
    </Panel>
    <Panel title="Credenciais da integração" detail="O segredo fica no cofre do sistema operacional no aplicativo desktop." action={<Badge tone={settings?.pluggyCredentialsConfigured ? "success" : "warning"}>{settings?.pluggyCredentialsConfigured ? <><Check size={12} />Configuradas</> : "Não configuradas"}</Badge>}>
      <form className="form-grid" onSubmit={e => { e.preventDefault(); void run(async () => {
        const desktop = "__TAURI_INTERNALS__" in window;
        if (desktop) await invoke("set_pluggy_credentials", { credentials: { client_id: clientId, client_secret: clientSecret } });
        await request("/api/settings/pluggy-credentials", { method: "PUT", ...body({ clientId, clientSecret }) }); setClientSecret(""); setClientId("");
        notify(desktop ? "Credenciais salvas no cofre do sistema." : "Credenciais aplicadas apenas à sessão atual do backend.");
      }); }}><label>Client ID<input autoComplete="off" value={clientId} onChange={e => setClientId(e.target.value)} placeholder="Informe para alterar" required /></label><label>Client Secret<input type="password" autoComplete="new-password" value={clientSecret} onChange={e => setClientSecret(e.target.value)} placeholder="••••••••••••••••" required /></label><div><button className="button secondary" disabled={busy}><KeyRound size={15} />Salvar credenciais</button></div></form>
    </Panel>
    <details className="panel advanced"><summary>Opções avançadas<ChevronDown size={17} /></summary><div className="stack"><Note>IDs e informações técnicas ficam aqui. A consulta geral de itens pode não estar habilitada no seu plano Pluggy.</Note><form className="inline-form" onSubmit={e => { e.preventDefault(); void run(() => sync({ id: itemId.trim() })); }}><label className="grow">Importar uma conexão existente<input value={itemId} onChange={e => setItemId(e.target.value)} placeholder="Item ID da aplicação" required /></label><button className="button secondary" disabled={busy}>Importar</button></form><button className="text-button" disabled={busy} onClick={() => void run(async () => { const response = await request<{ results: RemoteItem[] }>("/api/pluggy/items"); setItems(response.results); notify(`${response.results.length} conexões encontradas.`); })}>Consultar itens pela API</button>{items.map(item => <div className="connection-row" key={item.id}><code className="grow">{item.id}</code><button className="button secondary small" disabled={busy} onClick={() => void run(() => sync(item))}>Importar</button></div>)}
      <h3>Referências indisponíveis</h3>{connections.filter(c => c.status === "MISSING").map(c => <p className="muted" key={c.id}>{c.name} — referência antiga, preservada para manter o histórico.</p>)}
      <h3>Histórico de importação</h3><div className="table-scroll"><table><thead><tr><th>Data</th><th>Resultado</th><th>Detalhe</th></tr></thead><tbody>{jobs.slice(0,15).map(j => <tr key={j.id}><td>{date(j.createdAt)}</td><td><Badge tone={j.status === "SUCCESS" ? "success" : j.status === "FAILED" ? "danger" : "warning"}>{j.status === "SUCCESS" ? "Concluída" : j.status === "FAILED" ? "Falhou" : j.status === "PARTIAL" ? "Parcial" : "Aguardando"}</Badge></td><td className="muted">{j.errorMessage ?? "Dados locais atualizados"}</td></tr>)}</tbody></table></div></div></details>
  </div>;
}
