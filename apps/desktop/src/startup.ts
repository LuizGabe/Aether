export type StartupPhase = "service" | "history";
export type Health = { status: string; database: string; version?: string };
export function startupPause(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    signal.throwIfAborted();
    const onAbort = () => { clearTimeout(timer); reject(signal.reason); };
    const timer = setTimeout(() => { signal.removeEventListener("abort", onAbort); resolve(); }, ms);
    signal.addEventListener("abort", onAbort, { once: true });
  });
}
/** Probe sequentially, then fetch once readiness is real. Never hold ready data for an animation. */
export async function loadStartup<T>({ health, load, signal, onPhase, timeoutMs = 30_000, now = () => performance.now(), pause = startupPause }: {
  health: (signal: AbortSignal) => Promise<Health>;
  load: (signal: AbortSignal) => Promise<T>;
  signal: AbortSignal;
  onPhase: (phase: StartupPhase) => void;
  timeoutMs?: number;
  now?: () => number;
  pause?: (ms: number, signal: AbortSignal) => Promise<void>;
}): Promise<T> {
  const deadline = now() + timeoutMs;
  let lastError: unknown;
  signal.throwIfAborted(); onPhase("service");
  while (now() < deadline) {
    signal.throwIfAborted();
    try {
      const probeSignal = AbortSignal.any([signal, AbortSignal.timeout(Math.max(1, Math.min(800, deadline - now())))]);
      const status = await health(probeSignal);
      signal.throwIfAborted();
      if (status.status === "ok" && status.database === "ok") {
        onPhase("history");
        const data = await load(AbortSignal.any([signal, AbortSignal.timeout(Math.max(1, Math.min(15_000, deadline - now())))]));
        signal.throwIfAborted();
        return data;
      }
    } catch (error) { signal.throwIfAborted(); lastError = error; }
    const remaining = deadline - now();
    if (remaining > 0) await pause(Math.min(250, remaining), signal);
  }
  throw new Error(lastError instanceof Error ? `Não foi possível abrir seu histórico. ${lastError.message}` : "O serviço local está demorando para responder. Tente novamente.");
}
