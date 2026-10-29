import { invoke } from "@tauri-apps/api/core";
export let API = import.meta.env.VITE_AETHER_API_URL ?? "http://127.0.0.1:38472";
export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  try {
    const response = await fetch(`${API}${path}`, { ...init, signal: init.signal ?? AbortSignal.timeout(180_000), headers: { "Content-Type": "application/json", Accept: "application/json", ...init.headers } });
    if (!response.ok) {
      const body = await response.json().catch(() => null);
      throw new Error(body?.message ?? `Não foi possível concluir a operação (${response.status}).`);
    }
    return await response.json() as T;
  } catch (error) {
    if (error instanceof TypeError) throw new Error("Não foi possível acessar o Aether. Aguarde a inicialização ou tente atualizar.");
    throw error;
  }
}
export const body = (value: unknown): RequestInit => ({ body: JSON.stringify(value) });

export type RuntimeProfile = { dataDir: string; apiPort: number; isolated: boolean };
export let runtimeProfile: RuntimeProfile | null = null;
export async function initializeApi() {
  if ("__TAURI_INTERNALS__" in window) {
    runtimeProfile = await invoke<RuntimeProfile>("get_runtime_config");
    API = `http://127.0.0.1:${runtimeProfile.apiPort}`;
  }
}
