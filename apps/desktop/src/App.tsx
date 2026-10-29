import { Welcome } from "./DataSettings";
import { runtimeProfile } from "./api";
import { Startup } from "./StartupScreen";
import { loadStartup, type Health, type StartupPhase } from "./startup";
import { VisualIdentity } from "./VisualIdentity";
import appIcon from "./assets/aether-icon.svg";
import React from "react";
import { ArrowLeftRight, BarChart3, CreditCard, Eye, EyeOff, LayoutDashboard, Leaf, RefreshCw, Settings2, Wallet, X } from "lucide-react";
import { request, body } from "./api";
import type { Entry, Filters, Workspace } from "./types";
import { Overview, Accounts, Cards, Investments, Analytics } from "./FinancialViews";
import { Transactions, EntryEditor } from "./Transactions";
import { Settings } from "./Settings";
import { monthLabel, Privacy } from "./ui";

type View = "overview" | "transactions" | "accounts" | "cards" | "investments" | "analytics" | "settings";
const items: { id: View; name: string; icon: React.ReactNode; subtitle: string }[] = [
  { id: "overview", name: "Visão geral", icon: <LayoutDashboard size={19} />, subtitle: "Seu dinheiro, em perspectiva." },
  { id: "transactions", name: "Transações", icon: <ArrowLeftRight size={19} />, subtitle: "Organize o histórico e dê sentido a cada movimento." },
  { id: "accounts", name: "Contas", icon: <Wallet size={19} />, subtitle: "Os saldos e movimentos das suas contas correntes." },
  { id: "cards", name: "Cartões", icon: <CreditCard size={19} />, subtitle: "Compras, faturas e compromissos, sem misturar os saldos." },
  { id: "investments", name: "Investimentos", icon: <Leaf size={19} />, subtitle: "Acompanhe suas posições e a evolução da carteira." },
  { id: "analytics", name: "Análises", icon: <BarChart3 size={19} />, subtitle: "Encontre padrões e acompanhe sua vida financeira." },
];
function currentMonth() { const now = new Date(); return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`; }
function emptyFilters(month: string): Filters { const last = new Date(Number(month.slice(0, 4)), Number(month.slice(5)), 0).getDate(); return { q: "", from: `${month}-01`, to: `${month}-${last}`, accountId: "", connectionId: "", categoryId: "", kind: "", product: "", review: "", ignored: "", billId: "", origin: "" }; }

export default function App() {
  const [view, setView] = React.useState<View>("overview"), [month, setMonth] = React.useState(currentMonth), [data, setData] = React.useState<Workspace | null>(null);
  const [syncing, setSyncing] = React.useState(false), [startupError, setStartupError] = React.useState("");
  const [startupPhase, setStartupPhase] = React.useState<StartupPhase>("service"), [startupSlow, setStartupSlow] = React.useState(false), [startupAttempt, setStartupAttempt] = React.useState(0);
  const [toast, setToast] = React.useState<{ message: string; error: boolean } | null>(null), [privacy, setPrivacy] = React.useState(false), [editing, setEditing] = React.useState<Entry | null>(null);
  const [settingsTab, setSettingsTab] = React.useState("connections");
  const [filters, setFilters] = React.useState<Filters>(() => emptyFilters(currentMonth()));
  const refreshNumber = React.useRef(0), syncLock = React.useRef(false);
  const notify = React.useCallback((message: string, error = false) => setToast({ message, error }), []);
  const refresh = React.useCallback(async () => { const number = ++refreshNumber.current; try { const next = await request<Workspace>(`/api/mvp/workspace?month=${encodeURIComponent(month)}`); if (refreshNumber.current === number) { setData(next); setStartupError(""); } } catch (e) { if (refreshNumber.current === number) setStartupError((e as Error).message); } }, [month]);
  React.useEffect(() => {
    // Month changes refresh loaded data; the initial path has its own serial readiness loop.
    if (data) { void refresh(); return; }
    const controller = new AbortController();
    setStartupError(""); setStartupSlow(false); setStartupPhase("service");
    const slowTimer = setTimeout(() => setStartupSlow(true), 8000);
    void loadStartup<Workspace>({
      health: signal => request<Health>("/api/health", { signal }),
      load: signal => request<Workspace>(`/api/mvp/workspace?month=${encodeURIComponent(month)}`, { signal }),
      signal: controller.signal, onPhase: setStartupPhase,
    }).then(next => {
      if (!controller.signal.aborted) { setData(next); setStartupError(""); }
    }).catch(error => {
      if (!controller.signal.aborted) setStartupError((error as Error).message);
    }).finally(() => clearTimeout(slowTimer));
    return () => { clearTimeout(slowTimer); controller.abort(); };
  }, [refresh, startupAttempt]);
  React.useEffect(() => { setFilters(f => ({ ...f, from: emptyFilters(month).from, to: emptyFilters(month).to, billId: "" })); }, [month]);
  React.useEffect(() => { if (!toast || toast.error) return; const timer = setTimeout(() => setToast(null), 6500); return () => clearTimeout(timer); }, [toast]);
  React.useEffect(() => { const listener = () => { if (!document.hidden && data) void refresh(); }; document.addEventListener("visibilitychange", listener); return () => document.removeEventListener("visibilitychange", listener); }, [refresh, !!data]);
  function navigate(filter: Partial<Filters>) { setFilters({ ...emptyFilters(month), ...filter }); setView("transactions"); }
  async function syncAll() {
    if (!data || syncLock.current) return;
    syncLock.current = true; setSyncing(true); let success = 0; const warnings: string[] = [];
    try {
      const connections = data.connections.filter(c => c.status !== "MISSING");
      if (!connections.length) { setView("settings"); notify("Conecte sua primeira instituição nas Configurações."); return; }
      for (const c of connections) { try { const result = await request<{ status: string; message: string }>(`/api/connections/${c.id}/sync`, { method: "POST" }); if (result.status === "SUCCESS") success++; else warnings.push(`${c.name}: ${result.message}`); } catch (e) { warnings.push(`${c.name}: ${(e as Error).message}`); } await refresh(); }
      notify(warnings.length ? `${success} instituições importadas. ${warnings.join(" ")}` : "Tudo atualizado. Seus dados financeiros estão disponíveis.", warnings.length > 0);
    } finally { syncLock.current = false; setSyncing(false); }
  }
  const active = items.find(i => i.id === view) ?? { name: "Configurações", subtitle: "Conexões, preferências e organização do aplicativo." };
  return <VisualIdentity.Provider value={{ connections: data?.connections ?? [], visualPreferences: data?.visualPreferences ?? [] }}><Privacy.Provider value={privacy}><div className="app-shell"><aside className="sidebar"><div className="brand"><img className="brand-symbol" src={appIcon} alt="" aria-hidden="true" /><div><strong>aether</strong><small>finanças pessoais</small></div></div><div className="sidebar-label">SEU ESPAÇO</div><nav aria-label="Navegação principal">{items.map(item => <button key={item.id} aria-label={item.name} title={item.name} className={`nav-item ${view === item.id ? "active" : ""}`} onClick={() => setView(item.id)} aria-current={view === item.id ? "page" : undefined}>{item.icon}<span>{item.name}</span>{item.id === "transactions" && !!data?.summary.reviewCount && <span className="nav-count">{data.summary.reviewCount}</span>}</button>)}</nav><div className="sidebar-bottom"><div className="local-card"><span className="status-dot" /><strong>No seu computador</strong><small>Seu histórico permanece local.</small></div><button aria-label="Configurações" title="Configurações" className={`nav-item ${view === "settings" ? "active" : ""}`} onClick={() => setView("settings")} aria-current={view === "settings" ? "page" : undefined}><Settings2 size={19} /><span>Configurações</span></button><div className="profile"><span aria-hidden="true">A</span><div><strong>Meu espaço pessoal</strong><small>Aether · versão 0.2</small></div></div></div></aside>
    <main className="workspace">{runtimeProfile?.isolated && <div className="test-profile-banner">Ambiente de teste isolado · porta {runtimeProfile.apiPort} · {runtimeProfile.dataDir}</div>}<header className="topbar"><div><div className="breadcrumbs">Meu espaço<span>/</span>{active.name}</div><h1>{active.name}</h1><p>{active.subtitle}</p></div><div className="header-actions">{view !== "settings" && <label className="month-picker"><span className="sr-only">Período de análise</span><input type="month" value={month} min="2000-01" max="2100-12" onChange={e => { if (/^\d{4}-\d{2}$/.test(e.target.value)) setMonth(e.target.value); }} /></label>}<button className="icon-button privacy-button" aria-label={privacy ? "Mostrar valores" : "Ocultar valores"} title={privacy ? "Mostrar valores" : "Ocultar valores"} onClick={() => setPrivacy(!privacy)}>{privacy ? <EyeOff size={18} /> : <Eye size={18} />}</button><button className="button primary sync-button" onClick={() => void syncAll()} disabled={syncing || !data}><RefreshCw size={16} className={syncing ? "spin" : ""} />{syncing ? "Sincronizando…" : "Sincronizar"}</button></div></header>
      {toast && <div className={`toast ${toast.error ? "warning" : "success"}`} role={toast.error ? "alert" : "status"}><span>{toast.message}</span><button className="icon-button" aria-label="Dispensar aviso" onClick={() => setToast(null)}><X size={16} /></button></div>}{startupError && data && <div className="offline-notice" role="status">{startupError} Seu último carregamento continua visível.<button className="text-button" onClick={() => void refresh()}>Tentar novamente</button></div>}
      {!data ? <Startup phase={startupPhase} slow={startupSlow} error={startupError} retry={() => setStartupAttempt(a => a + 1)} /> : <div className="page-content">{view === "overview" && (data.firstRun ? <Welcome setup={() => { setSettingsTab("connections"); setView("settings"); }} restore={() => { setSettingsTab("data"); setView("settings"); }} /> : <Overview data={data} navigate={navigate} edit={setEditing} />)}{view === "accounts" && <Accounts data={data} navigate={navigate} />}{view === "cards" && <Cards data={data} navigate={navigate} />}{view === "investments" && <Investments data={data} />}{view === "analytics" && <Analytics data={data} navigate={navigate} />}{view === "transactions" && <Transactions data={data} filters={filters} setFilters={setFilters} edit={setEditing} onChange={refresh} notify={notify} />}{view === "settings" && <Settings initialTab={settingsTab} data={data} refresh={refresh} notify={notify} />}<footer className="page-footer"><span><span className="status-dot" />Dados locais · BRL nas análises consolidadas</span><span>{view === "settings" ? "Aether 0.2.0" : monthLabel(month)}</span></footer></div>}
    </main>{editing && data && <EntryEditor key={editing.id} entry={editing} categories={data.categories} onClose={() => setEditing(null)} onSave={async changes => { await request(`/api/mvp/transactions/${editing.id}`, { method: "PATCH", ...body(changes) }); setEditing(null); await refresh(); notify("Revisão salva. Sua edição será preservada nas sincronizações."); }} />}</div></Privacy.Provider></VisualIdentity.Provider>;
}
