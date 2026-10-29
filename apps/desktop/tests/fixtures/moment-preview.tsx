import React from "react";
import { createRoot } from "react-dom/client";
import { TransactionMoment } from "../../src/TransactionMoment";
import type { Entry } from "../../src/types";
import "../../src/styles.css";

const entry = (time: string | null) => ({ localTime: time, timeSource: "purchase", occurredAt: time ? `2026-10-23T${time}:00-03:00` : null } as Entry);
function Preview() {
  const [time, setTime] = React.useState("19:08");
  return <main style={{ maxWidth: 1120, margin: "auto", padding: "32px 24px 50px" }}>
    <p style={{ color: "#718166", fontSize: 12, marginBottom: 8 }}>AETHER · VALIDAÇÃO VISUAL</p><h1 style={{ fontSize: 28 }}>O horário desenhado no céu</h1>
    <p style={{ color: "#6c7766", fontSize: 13, marginTop: 10 }}>Quatro cenários, com transparência no topo e cor na base. Horários ilustrativos, sem dados bancários.</p>
    <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit,minmax(228px,1fr))", gap: 20, marginTop: 25 }}>
      {["06:30", "12:00", "17:45", "00:00"].map(value => <section key={value} style={{ padding: "10px 0", background: "white", borderRadius: 20 }}><TransactionMoment entry={entry(value)} expanded /></section>)}
    </div>
    <section style={{ marginTop: 24, background: "white", padding: 24, borderRadius: 20 }}>
      <h2>Na lista de transações</h2><div style={{ display: "flex", gap: 26, flexWrap: "wrap", marginTop: 12 }}>{["06:30","09:20","12:00","17:45","19:08","23:15"].map(value=><TransactionMoment key={value} entry={entry(value)} />)}</div>
    </section>
    <section style={{ marginTop: 24, display: "flex", flexWrap: "wrap", gap: 36, alignItems: "center", background: "white", borderRadius: 20, padding: 24 }}>
      <div><h2>Teste outro horário</h2><label style={{ marginTop: 16 }}>Horário<input type="time" value={time} onChange={e => setTime(e.target.value)} /></label><div style={{ display: "flex", gap: 8, marginTop: 12 }}>{["05:00","12:00","18:59","19:00", ""].map(value=><button className="button secondary small" key={value} onClick={()=>setTime(value)}>{value || "Sem horário"}</button>)}</div></div>
      <TransactionMoment entry={entry(time)} /><TransactionMoment entry={entry(null)} />
    </section>
  </main>;
}
const root = createRoot(document.getElementById("root")!);
if (import.meta.hot) import.meta.hot.dispose(() => root.unmount());
root.render(<Preview />);
