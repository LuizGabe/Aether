import { test } from "node:test";
import assert from "node:assert/strict";
import { readPluggyError } from "../src/pluggy-recovery.ts";

test("recovers duplicate IDs in the documented response", () => {
  assert.deepEqual(readPluggyError({ codeDescription: "ITEM_USER_ALREADY_EXISTS", data: { items: ["one", "two", "one", null] } }).itemIds, ["one", "two"]);
});
test("handles SDK envelopes and serialized API bodies", () => {
  const result = readPluggyError({ data: { error: { body: JSON.stringify({ codeDescription: "ITEM_USER_ALREADY_EXISTS", data: { items: ["existing"] } }) } } });
  assert.equal(result.duplicate, true);
  assert.deepEqual(result.itemIds, ["existing"]);
});
test("keeps an item reference from an unsuccessful connection", () => {
  assert.deepEqual(readPluggyError({ message: "MFA pendente", data: { item: { id: "pending" } } }).itemIds, ["pending"]);
});
test("handles empty, malformed and cyclic errors safely", () => {
  assert.deepEqual(readPluggyError(null).itemIds, []);
  const cyclic: Record<string, unknown> = { message: "ITEM_USER_ALREADY_EXISTS" };
  cyclic.data = cyclic;
  assert.equal(readPluggyError(cyclic).duplicate, true);
  assert.deepEqual(readPluggyError({ data: { items: [42, {}, ""] } }).itemIds, []);
});
