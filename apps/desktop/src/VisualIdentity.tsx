import React from "react";
import { CalendarDays, ImagePlus, RotateCcw } from "lucide-react";
import c6Logo from "./assets/banks/c6.svg";
import interLogo from "./assets/banks/inter.svg";
import { request, body } from "./api";
import type { VisualPreference, Workspace } from "./types";
import { Empty, Note, Panel } from "./ui";

export const VisualIdentity = React.createContext<Pick<Workspace, "connections" | "visualPreferences">>({ connections: [], visualPreferences: [] });
const bankLabel = (name: string) => name === "MeuPluggy" ? "PicPay" : name;
const defaultBankImage = (name: string) => name.toLowerCase().includes("inter") ? interLogo : name.toLowerCase().includes("c6") ? c6Logo : null;
export function useBankIdentity(name: string) {
  const context = React.useContext(VisualIdentity);
  const connection = context.connections.find(c => c.name === name);
  const preference = context.visualPreferences?.find(p => p.scope === "institution" && p.targetId === connection?.id);
  return { name: preference?.alias || bankLabel(name), image: preference?.image || defaultBankImage(name) };
}
export function IdentityImage({ image, name, recurrence = false }: { image?: string | null; name: string; recurrence?: boolean }) {
  const [failed, setFailed] = React.useState(false);
  React.useEffect(() => setFailed(false), [image]);
  return <span className="identity-image" aria-hidden="true">{image && !failed ? <img src={image} alt="" onError={() => setFailed(true)} /> : recurrence ? <CalendarDays size={18} /> : <span>{name.toLowerCase().includes("c6") ? "C6" : name.toLowerCase().includes("picpay") ? "P" : name.slice(0,2).toUpperCase()}</span>}</span>;
}
export function BankAvatar({ name }: { name: string }) { const identity = useBankIdentity(name); return <IdentityImage image={identity.image} name={identity.name} />; }
export function BankName({ name }: { name: string }) { const identity = useBankIdentity(name); return <span title={bankLabel(name)}>{identity.name}</span>; }
export function RecurrenceIdentity({ id, description }: { id: string; description: string }) {
  const context = React.useContext(VisualIdentity), preference = context.visualPreferences?.find(p => p.scope === "recurrence" && p.targetId === id);
  return <><IdentityImage image={preference?.image} name={description} recurrence /><span className="grow truncate"><strong>{preference?.alias || description}</strong>{preference?.alias && <small title={description}>{description}</small>}</span></>;
}
async function readImage(file: File): Promise<string> {
  if(!["image/png", "image/jpeg", "image/webp"].includes(file.type)) throw new Error("Escolha uma imagem PNG, JPG ou WebP.");
  if(file.size > 8 * 1024 * 1024) throw new Error("A imagem deve ter até 8 MB.");
  const bitmap = await createImageBitmap(file);
  try {
    const canvas = document.createElement("canvas"), scale = Math.min(1, 256 / Math.max(bitmap.width, bitmap.height));
    canvas.width = Math.max(1, Math.round(bitmap.width * scale)); canvas.height = Math.max(1, Math.round(bitmap.height * scale));
    const context=canvas.getContext("2d"); if(!context)throw new Error("Não foi possível preparar a imagem.");
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
    return canvas.toDataURL("image/png");
  } finally { bitmap.close(); }
}
export function VisualSettings({ data, refresh, notify }: { data: Workspace; refresh: () => Promise<void>; notify: (message: string, error?: boolean) => void }) {
  return <div className="stack"><Panel title="Instituições" detail="Apelidos e imagens locais, compartilhados entre contas e cartões."><div className="identity-settings">{data.connections.filter(c => c.status !== "MISSING").map(c => <IdentityEditor key={c.id} scope="institution" targetId={c.id} original={bankLabel(c.name)} fallback={defaultBankImage(c.name)} preference={data.visualPreferences?.find(p=>p.scope==="institution" && p.targetId===c.id)} refresh={refresh} notify={notify} />)}</div><Note>Os logos do C6 e do Inter foram importados e ficam disponíveis offline. Você pode escolher outra imagem para qualquer instituição.</Note></Panel>
    <Panel title="Recorrências" detail="Dê um nome reconhecível e uma imagem aos padrões identificados.">{data.recurrences.length ? <div className="identity-settings">{data.recurrences.map(r => <IdentityEditor key={r.id} scope="recurrence" targetId={r.id} original={r.description} detail={`${bankLabel(r.institution)} · ${r.months} meses identificados`} preference={data.visualPreferences?.find(p=>p.scope==="recurrence" && p.targetId===r.id)} refresh={refresh} notify={notify} />)}</div> : <Empty title="Nenhuma recorrência identificada">Quando houver um padrão no período selecionado, você poderá personalizá-lo aqui.</Empty>}<Note>O apelido é apenas visual. A descrição original, os valores e a identificação do padrão permanecem preservados, inclusive após novas sincronizações.</Note></Panel></div>;
}
function IdentityEditor({ scope, targetId, original, detail, fallback, preference, refresh, notify }: { scope: VisualPreference["scope"]; targetId: string; original: string; detail?: string; fallback?: string | null; preference?: VisualPreference; refresh: () => Promise<void>; notify: (message: string, error?: boolean) => void }) {
  const [alias, setAlias] = React.useState(preference?.alias || ""), [image, setImage] = React.useState(preference?.image || ""), [busy,setBusy] = React.useState(false), [error,setError] = React.useState("");
  const fileInput=React.useRef<HTMLInputElement>(null);
  async function save(reset=false) {
    setBusy(true); setError("");
    try { await request("/api/mvp/visual-preferences", { method: "PUT", ...body({scope,targetId,alias:reset?"":alias,image:reset?"":image}) }); if(reset){setAlias("");setImage("");} await refresh(); notify(reset?"Aparência original restaurada.":"Nome e imagem salvos."); } catch(e){setError((e as Error).message);} finally{setBusy(false);}
  }
  return <form className="identity-setting" onSubmit={e=>{e.preventDefault();void save();}}><div className="identity-source"><IdentityImage image={image || fallback} name={alias || original} recurrence={scope === "recurrence"} /><div><strong>{original}</strong>{detail && <small>{detail}</small>}</div></div><label>Apelido visual<input maxLength={100} placeholder={original} value={alias} onChange={e=>setAlias(e.target.value)} /></label><div className="identity-actions"><input ref={fileInput} className="sr-only" type="file" accept="image/png,image/jpeg,image/webp" aria-label={`Imagem de ${original}`} disabled={busy} onChange={e=>{const file=e.target.files?.[0];e.target.value="";if(file){setBusy(true);setError("");void readImage(file).then(setImage).catch(e=>setError(e.message)).finally(()=>setBusy(false));}}} /><button type="button" className="button secondary small" disabled={busy} onClick={()=>fileInput.current?.click()}><ImagePlus size={15} />Foto</button>{image && <button type="button" className="text-button" disabled={busy} onClick={()=>setImage("")}>Remover foto</button>}<button type="button" className="icon-button" title="Restaurar aparência original" aria-label={`Restaurar ${original}`} disabled={busy} onClick={()=>void save(true)}><RotateCcw size={15} /></button><button className="button primary small" disabled={busy}>{busy?"Aguarde…":"Salvar"}</button></div>{error && <p className="inline-error" role="alert">{error}</p>}</form>;
}
