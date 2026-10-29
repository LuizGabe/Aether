import type { Entry, Kind } from "./types";

type Editable = Pick<Entry, "counterpartyOverride" | "categoryId" | "kind" | "notes" | "ignored">;
type Draft = { counterparty: string; categoryId: string; kind: Kind; notes: string; ignored: boolean; confirmFinancial: boolean };
export function reviewChanges(entry: Editable, draft: Draft): Record<string, unknown> {
  const changes: Record<string, unknown> = {};
  if (draft.counterparty !== (entry.counterpartyOverride ?? "")) changes.counterparty = draft.counterparty || null;
  if (draft.confirmFinancial || draft.categoryId !== (entry.categoryId ?? "")) changes.categoryId = draft.categoryId || null;
  if (draft.confirmFinancial || draft.kind !== entry.kind) changes.kind = draft.kind;
  if (draft.notes !== (entry.notes ?? "")) changes.notes = draft.notes;
  if (draft.ignored !== entry.ignored) changes.ignored = draft.ignored;
  return changes;
}
