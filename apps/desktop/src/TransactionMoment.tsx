import React from "react";
import type { Entry } from "./types";
import { dayPhase } from "./dayPhase";

const skies = {
  dawn: { base: "#ff947f", middle: "#ffb58b", orbit: "#f2ba63", glow: "#ffd77a", sunTop: "#fff6a0", sunBottom: "#ff9a43", back: "#ffd38f", front: "#ffb188" },
  day: { base: "#19bce9", middle: "#6eddf2", orbit: "#66cedd", glow: "#ffe878", sunTop: "#fff993", sunBottom: "#ffcf32", back: "#c5f6fc", front: "#effeff" },
  dusk: { base: "#f77248", middle: "#ab548a", orbit: "#f99b50", glow: "#ff9b47", sunTop: "#ffb545", sunBottom: "#ff4b32", back: "#b44b78", front: "#65325f" },
  night: { base: "#233f67", middle: "#404379", orbit: "#8fa8e4", glow: "#95c9f2", sunTop: "#edffff", sunBottom: "#f7da9b", back: "#3e5c8b", front: "#2c4c72" },
};
const stars = [[30,46,1.3],[50,24,1],[142,25,1.5],[170,50,1.1],[24,91,1],[153,77,1.3],[47,80,1.1],[177,111,1],[121,65,.9]];

export function TransactionMoment({ entry, expanded = false }: { entry: Entry; expanded?: boolean }) {
  const id = React.useId().replace(/:/g, "");
  const phase = dayPhase(entry.localTime);
  if (!phase) return <span className="moment-unknown" title="A instituição não forneceu um horário confiável para este lançamento.">Horário não informado</span>;
  const color = skies[phase.scene];
  const source = entry.timeSource === "purchase" ? "Horário da compra" : "Horário do lançamento informado pelo banco";
  return <div className={`transaction-moment sky-${phase.scene} ${expanded ? "expanded" : ""}`} title={`${source} · ${entry.localTime} · Brasília. Trajeto ilustrativo; não indica nascer ou pôr do sol.`}>
    <div className="moment-scene">
      <svg className="moment-art" viewBox="0 0 200 200" preserveAspectRatio="none" role="img" aria-label={`${entry.localTime}, ${phase.label}, horário de Brasília`}>
        <defs>
          <linearGradient id={`${id}-sky`} x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor={color.middle} stopOpacity="0"/><stop offset=".22" stopColor={color.middle} stopOpacity=".07"/><stop offset=".62" stopColor={color.middle} stopOpacity=".55"/><stop offset="1" stopColor={color.base}/></linearGradient>
          <linearGradient id={`${id}-orb`} x1=".2" y1="0" x2=".8" y2="1"><stop stopColor={color.sunTop}/><stop offset="1" stopColor={color.sunBottom}/></linearGradient>
          <linearGradient id={`${id}-cloud`} x1="0" y1="0" x2="0" y2="1"><stop stopColor={color.front} stopOpacity=".9"/><stop offset="1" stopColor={color.base} stopOpacity=".8"/></linearGradient>
          <radialGradient id={`${id}-halo`}><stop stopColor={color.glow} stopOpacity=".85"/><stop offset=".4" stopColor={color.glow} stopOpacity=".38"/><stop offset="1" stopColor={color.glow} stopOpacity="0"/></radialGradient>
        </defs>
        <rect width="200" height="200" fill={`url(#${id}-sky)`}/>
        {phase.scene === "night" && <g fill="#82b8df">{stars.map(([x,y,r],i) => <g key={i}><circle cx={x} cy={y} r={r} opacity=".65"/>{i%3===0 && <path d={`M${x-3} ${y}h6M${x} ${y-3}v6`} stroke="#a6d8ef" strokeWidth=".6" opacity=".7"/>}</g>)}</g>}
        <path className="sky-orbit remaining" d="M31 138 A75 75 0 1 1 169 138" fill="none" stroke={color.orbit} strokeWidth="2.8" strokeLinecap="round" pathLength="100" strokeDasharray="1 3.7" opacity=".42"/>
        <path className="sky-orbit completed" d="M31 138 A75 75 0 1 1 169 138" fill="none" stroke={color.orbit} strokeWidth="3" strokeLinecap="round" pathLength="100" strokeDasharray="84 100"/>
        <g className="sky-body" transform={`translate(${phase.x} ${phase.y})`}>
          <circle r="38" fill={`url(#${id}-halo)`}/>
          {phase.scene === "day" && <g stroke="#f6d34c" strokeWidth="2.6" strokeLinecap="round">{Array.from({length:10},(_,i)=><path key={i} d="M0 -22 L0 -25" transform={`rotate(${i*36})`}/>)}</g>}
          {phase.daylight ? <circle r="17" fill={`url(#${id}-orb)`}/> : <path d="M7 -18 C-7 -17 -19 -6 -17 8 C-15 23 6 27 20 12 C7 16 -7 0 7 -18Z" fill={`url(#${id}-orb)`} stroke="#cae7ee" strokeWidth=".45"/>}
        </g>
        {phase.scene === "day" ? <g fill={`url(#${id}-cloud)`}>
          <path d="M-8 151c5-12 17-16 23-6 4-5 13-2 16 4 6-2 11 1 15 7H-8Z"/>
          <path d="M146 164c5-9 11-12 17-5 3-8 14-12 20-4 7-13 22-12 26 2v13h-63Z"/>
          <path d="M34 183c5-7 11-9 15-4 4-10 16-12 23-4 8-5 18-2 20 8Z"/>
        </g> : <>
          <path d="M-12 166c8-20 25-27 38-13 9-6 24-3 30 8 10-8 25-2 31 7 12-13 30-11 39 1 8-8 19-13 29-6 10-19 25-24 38-11 7-6 15-3 23 1v61H-12Z" fill={color.back} opacity=".62"/>
          <path d="M-12 186c10-17 24-22 38-11 9-4 24-1 30 11 11-8 28-2 34 10 12-13 25-16 37-7 11-14 27-17 39-5 10-11 23-16 38-8v38H-12Z" fill={`url(#${id}-cloud)`}/>
        </>}
      </svg>
    </div>
    <div className="moment-heading"><time dateTime={entry.occurredAt ?? undefined}>{entry.localTime}</time><span>{phase.label}</span></div>
    {expanded && <small>{source} · Brasília<br/>Trajeto ilustrativo do período do dia</small>}
  </div>;
}
export function OriginLabel({ entry }: { entry: Entry }) {
  return entry.originStatus === "missing" || entry.originStatus === "document_only" ? <span className="origin-warning">{entry.originStatus === "document_only" ? "Contraparte sem nome · documento disponível" : "Estabelecimento não informado pelo banco"}</span> : entry.originSource === "manual" ? <span className="origin-manual">Identificação manual</span> : null;
}
