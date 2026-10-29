import { test } from "node:test";
import assert from "node:assert/strict";
import { reviewChanges } from "../src/transactionReview.ts";

const entry = { counterpartyOverride: null, categoryId: "provider-category", kind: "EXPENSE" as const, notes: null, ignored: false };
const draft = { counterparty: "", categoryId: "provider-category", kind: "EXPENSE" as const, notes: "", ignored: false, confirmFinancial: false };
test("identifying a merchant does not silently confirm or overwrite financial classification", () => {
  assert.deepEqual(reviewChanges(entry, { ...draft, counterparty: "UFFA" }), { counterparty: "UFFA" });
});
test("financial confirmation is explicit and keeps a separate identity override", () => {
  assert.deepEqual(reviewChanges(entry, { ...draft, confirmFinancial: true }), { categoryId: "provider-category", kind: "EXPENSE" });
});
test("resetting a manual name restores provider identification without changing source data", () => {
  assert.deepEqual(reviewChanges({ ...entry, counterpartyOverride: "UFFA" }, draft), { counterparty: null });
  assert.deepEqual(reviewChanges(entry, draft), {});
});
