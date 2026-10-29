// Accept the documented API error and SDK envelopes without retaining credentials.
export function readPluggyError(value: unknown): { duplicate: boolean; itemIds: string[]; message: string } {
  const ids = new Set<string>();
  const visited = new Set<object>();
  let duplicate = false;
  let message = "Pluggy Connect retornou erro.";
  function visit(node: unknown, depth: number) {
    if (depth > 6 || node == null) return;
    if (typeof node === "string") {
      if (node.includes("ITEM_USER_ALREADY_EXISTS")) duplicate = true;
      try { visit(JSON.parse(node), depth + 1); } catch { /* Plain message. */ }
      return;
    }
    if (typeof node !== "object" || visited.has(node)) return;
    visited.add(node);
    const record = node as Record<string, unknown>;
    if (record.codeDescription === "ITEM_USER_ALREADY_EXISTS" || record.code === "ITEM_USER_ALREADY_EXISTS") duplicate = true;
    if (Array.isArray(record.items)) {
      for (const id of record.items) if (typeof id === "string" && id.trim()) ids.add(id.trim());
    }
    if (record.item && typeof record.item === "object" && "id" in record.item && typeof record.item.id === "string") {
      ids.add(record.item.id);
    }
    if (typeof record.message === "string") {
      message = record.message;
      visit(record.message, depth + 1);
    }
    for (const key of ["data", "error", "response", "body", "metadata"]) visit(record[key], depth + 1);
  }
  visit(value, 0);
  return { duplicate, itemIds: [...ids], message };
}
