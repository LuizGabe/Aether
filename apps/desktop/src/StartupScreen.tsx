import { RefreshCw } from "lucide-react";
import appIcon from "./assets/aether-icon.svg";
import type { StartupPhase } from "./startup";

export function Startup({ phase, slow, error, retry }: { phase: StartupPhase; slow: boolean; error: string; retry: () => void }) {
  return <section className={`startup startup-scene ${error ? "startup-failed" : ""}`} aria-busy={!error}>
    <div className="startup-art" aria-hidden="true"><div className="startup-halo" /><div className="startup-orbit"><i /></div><img src={appIcon} alt="" /><span className="startup-spark spark-one" /><span className="startup-spark spark-two" /></div>
    <div className="startup-copy" role={error ? "alert" : "status"} aria-live="polite" aria-atomic="true"><span className="startup-eyebrow">SEU ESPAÇO FINANCEIRO</span><h2>{error ? "Precisamos de mais um instante" : phase === "service" ? "Preparando seu espaço" : "Abrindo seu histórico"}</h2><p>{error || (slow ? "Está levando um pouco mais de tempo. Estamos aguardando seus dados locais." : phase === "service" ? "Deixando tudo pronto para você." : "Suas contas, cartões e investimentos estão chegando.")}</p></div>
    {error ? <button className="button secondary" onClick={retry}><RefreshCw size={16} />Tentar novamente</button> : <><div className="startup-track" role="progressbar" aria-label="Abrindo os dados locais"><span /></div><div className="startup-stages" aria-hidden="true"><span className={phase === "service" ? "current" : "complete"}><i />Preparar</span><span className={phase === "history" ? "current" : ""}><i />Abrir histórico</span></div></>}
    <small className="startup-local">Seu histórico permanece neste computador.</small>
  </section>;
}
