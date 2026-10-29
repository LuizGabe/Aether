import React from "react";
import { ArrowDownLeft, ArrowUpRight, ArrowLeftRight, CircleHelp, Inbox, X } from "lucide-react";
import type { Entry, Series } from "./types";
export const Privacy = React.createContext(false);
export const currency = (v: number | null | undefined, code = "BRL") => v == null ? "—" : new Intl.NumberFormat("pt-BR", { style: "currency", currency: code }).format(v);
export const date = (value: string | null | undefined) => {
  if (!value) return "Não informado";
  const parsed = new Date(value.length === 10 ? `${value}T12:00:00` : value.includes("T") ? value : `${value.replace(" ", "T")}Z`);
  return Number.isNaN(parsed.getTime()) ? "Não informado" : new Intl.DateTimeFormat("pt-BR").format(parsed);
};
export const monthLabel = (v: string, short = false) => new Intl.DateTimeFormat("pt-BR", { month: short ? "short" : "long", ...(short ? {} : { year: "numeric" }) }).format(new Date(`${v}-15T12:00:00`));
export function Money({ value, code = "BRL", className = "" }: { value: number | null | undefined; code?: string; className?: string }) { const hidden = React.useContext(Privacy); return <span className={`money ${className}`}>{hidden ? "R$ ••••" : currency(value, code)}</span>; }
export function Empty({ title, children }: { title: string; children?: React.ReactNode }) { return <div className="empty"><span><Inbox size={24} /></span><h3>{title}</h3>{children && <p>{children}</p>}</div>; }
export function Badge({ children, tone = "neutral" }: { children: React.ReactNode; tone?: string }) { return <span className={`badge ${tone}`}>{children}</span>; }
export function Stat({ label, value, detail, icon, className = "" }: { label: string; value: React.ReactNode; detail?: React.ReactNode; icon?: React.ReactNode; className?: string }) { return <article className={`stat ${className}`}><div className="stat-label">{label}{icon}</div><strong>{value}</strong>{detail && <small>{detail}</small>}</article>; }
export function Panel({ title, detail, action, children, className = "" }: { title: string; detail?: React.ReactNode; action?: React.ReactNode; children: React.ReactNode; className?: string }) { return <section className={`panel ${className}`}><div className="panel-heading"><div><h2>{title}</h2>{detail && <p>{detail}</p>}</div>{action}</div>{children}</section>; }
export function EntryIcon({ entry }: { entry: Entry }) { return <span className={`entry-icon ${entry.kind.toLowerCase()}`}>{entry.kind === "EXPENSE" ? <ArrowUpRight size={17} /> : entry.kind === "INCOME" || entry.kind === "REFUND" ? <ArrowDownLeft size={17} /> : <ArrowLeftRight size={17} />}</span>; }
export function Trend({ series, cash = false }: { series: Series[]; cash?: boolean }) {
  const hidden = React.useContext(Privacy);
  const max = Math.max(1, ...series.flatMap(s => cash ? [s.cashIn, s.cashOut] : [s.income, Math.max(0, s.expenses)]));
  return <div className="trend"><div className="chart-legend"><span><i />{cash ? "Entradas" : "Receitas"}</span><span><i className="expense" />{cash ? "Saídas" : "Gastos líquidos"}</span></div><div className={`bar-chart ${hidden ? "masked-chart" : ""}`} role="img" aria-label={hidden ? "Valores ocultos" : `Comparativo de ${cash ? "entradas e saídas" : "receitas e gastos"} dos últimos seis meses`}>
    {[.25, .5, .75, 1].map(n => <div className="gridline" key={n} style={{ bottom: `${n * 80 + 12}%` }} />)}
    {series.map(s => <div className="bar-column" key={s.month}><div className="bar-pair"><div className="bar income" style={{ height: `${Math.max(1, (cash ? s.cashIn : s.income) / max * 100)}%` }} title={hidden ? "Oculto" : `${monthLabel(s.month)}: ${currency(cash ? s.cashIn : s.income)}`} /><div className="bar expense" style={{ height: `${Math.max(1, Math.max(0, cash ? s.cashOut : s.expenses) / max * 100)}%` }} title={hidden ? "Oculto" : `${monthLabel(s.month)}: ${currency(cash ? s.cashOut : s.expenses)}`} /></div><span>{monthLabel(s.month, true).replace(".", "")}</span></div>)}
  </div></div>;
}
export function CategoryBars({ rows, onSelect }: { rows: { name: string; amount: number }[]; onSelect?: (name: string) => void }) {
  const total = rows.reduce((s, r) => s + r.amount, 0), colors = ["#184b40", "#6c8b63", "#b3c78d", "#d2aa68", "#b99b85", "#ced4c5"];
  if (!total) return <Empty title="Sem gastos neste período">Troque o mês ou sincronize suas instituições.</Empty>;
  const top = rows.slice(0, 5); if (rows.length > 5) top.push({ name: "Demais categorias", amount: rows.slice(5).reduce((s, r) => s + r.amount, 0) });
  return <div className="category-bars">{top.map((r, i) => <button type="button" className="category-bar-row" key={r.name} disabled={!onSelect || r.name === "Demais categorias"} onClick={() => onSelect?.(r.name)}><div><span><i style={{ background: colors[i] }} />{r.name}</span><Money value={r.amount} /></div><div className="category-track"><span style={{ width: `${r.amount / total * 100}%`, background: colors[i] }} /></div></button>)}</div>;
}
export function Note({ children }: { children: React.ReactNode }) { return <p className="note"><CircleHelp size={15} /><span>{children}</span></p>; }
export function Modal({ title, children, onClose, wide = false }: { title: string; children: React.ReactNode; onClose: () => void; wide?: boolean }) {
  const dialog = React.useRef<HTMLDialogElement>(null);
  const titleId = React.useId();
  React.useEffect(() => { const el = dialog.current; el?.showModal(); el?.querySelector<HTMLElement>("input, select, textarea")?.focus(); return () => { el?.close(); }; }, []);
  return <dialog ref={dialog} aria-labelledby={titleId} className={`modal ${wide ? "wide" : ""}`} onCancel={onClose} onClick={e => { if (e.target === e.currentTarget) onClose(); }}><div className="modal-heading"><h2 id={titleId}>{title}</h2><button className="icon-button" aria-label="Fechar" onClick={onClose}><X size={19} /></button></div>{children}</dialog>;
}
