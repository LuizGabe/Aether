import { test } from "node:test";
import assert from "node:assert/strict";
import { loadStartup, startupPause } from "../src/startup.ts";
const ready = { status: "ok", database: "ok" };

test("startup waits for the database and loads once without overlapping requests", async () => {
  let probes = 0, active = 0, maximum = 0, loads = 0, clock = 0;
  const phases: string[] = [];
  const result = await loadStartup({
    signal: new AbortController().signal, onPhase: p => phases.push(p), now: () => clock,
    pause: async ms => { clock += ms; },
    health: async () => { active++; maximum = Math.max(maximum, active); await Promise.resolve(); active--; probes++; return probes < 3 ? { status: "ok", database: "unavailable" } : ready; },
    load: async () => { loads++; return "history"; },
  });
  assert.equal(result, "history"); assert.equal(probes, 3); assert.equal(loads, 1); assert.equal(maximum, 1);
  assert.deepEqual(phases, ["service", "history"]); assert.equal(clock, 500);
});
test("already available history is returned immediately without animation delay", async () => {
  let pauses = 0;
  const value = await loadStartup({ signal: new AbortController().signal, onPhase: () => {}, health: async () => ready, load: async () => 42, pause: async () => { pauses++; } });
  assert.equal(value, 42); assert.equal(pauses, 0);
});
test("cancelling a stale startup prevents its workspace request", async () => {
  const controller = new AbortController(); let loads = 0;
  let release!: (value: typeof ready) => void;
  const pending = loadStartup({ signal: controller.signal, onPhase: () => {}, health: () => new Promise<typeof ready>(resolve => { release = resolve; }), load: async () => { loads++; return 1; } });
  controller.abort(); release(ready);
  await assert.rejects(pending, { name: "AbortError" }); assert.equal(loads, 0);
});
test("unavailable service stops at the deadline and can be retried afresh", async () => {
  let clock = 0, loads = 0;
  await assert.rejects(loadStartup({ signal: new AbortController().signal, onPhase: () => {}, now: () => clock, timeoutMs: 500, pause: async ms => { clock += ms; }, health: async () => { throw new Error("Service unavailable"); }, load: async () => { loads++; return 1; } }), /Service unavailable/);
  assert.equal(loads, 0); assert.equal(clock, 500);
  assert.equal(await loadStartup({ signal: new AbortController().signal, onPhase: () => {}, health: async () => ready, load: async () => 7 }), 7);
});
test("a transient workspace failure recovers serially", async () => {
  let loads = 0, clock = 0;
  const data = await loadStartup({ signal: new AbortController().signal, onPhase: () => {}, now: () => clock, pause: async ms => { clock += ms; }, health: async () => ready, load: async () => { if (++loads === 1) throw new Error("Restarting"); return 9; } });
  assert.equal(data, 9); assert.equal(loads, 2); assert.equal(clock, 250);
});
test("waiting between probes is abortable", async () => {
  const controller = new AbortController(); const waiting = startupPause(10_000, controller.signal); controller.abort();
  await assert.rejects(waiting, { name: "AbortError" });
});
