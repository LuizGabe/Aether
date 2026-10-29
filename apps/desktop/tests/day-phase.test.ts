import { test } from "node:test";
import assert from "node:assert/strict";
import { dayPhase } from "../src/dayPhase.ts";

test("day marker follows the actual minute on a continuous 24-hour scale", () => {
  assert.equal(dayPhase("00:00")?.position, 0);
  assert.equal(dayPhase("12:00")?.position, 50);
  assert.ok((dayPhase("19:08")?.position ?? 0) > 79);
  assert.ok((dayPhase("23:59")?.position ?? 100) < 100);
});
test("sun and moon change at the declared symbolic period boundaries", () => {
  assert.equal(dayPhase("04:59")?.label, "Madrugada");
  assert.equal(dayPhase("06:00")?.emoji, "☀️");
  assert.equal(dayPhase("12:00")?.label, "Dia");
  assert.equal(dayPhase("19:00")?.emoji, "🌙");
  assert.equal(dayPhase("19:08")?.label, "Noite");
});
test("unknown or malformed time has no fabricated sun or moon position", () => {
  for (const value of [null, undefined, "", "24:00", "12:60", "foo"]) assert.equal(dayPhase(value), null);
});

test("reference scenes cover dawn, day, dusk and night without gaps", () => {
  for (const [time, scene] of [["04:59", "night"], ["05:00", "dawn"], ["07:59", "dawn"], ["08:00", "day"], ["15:59", "day"], ["16:00", "dusk"], ["18:59", "dusk"], ["19:00", "night"]]) assert.equal(dayPhase(time)?.scene, scene);
});
test("sun and moon actually travel on the arc and remain inside the artwork", () => {
  assert.ok((dayPhase("06:00")?.x ?? 100) < (dayPhase("12:00")?.x ?? 0));
  assert.ok((dayPhase("17:00")?.x ?? 0) > (dayPhase("12:00")?.x ?? 200));
  assert.ok((dayPhase("12:00")?.y ?? 200) < (dayPhase("06:00")?.y ?? 0));
  assert.ok(Math.abs((dayPhase("00:00")?.x ?? 0)-100)<.001);
  for(let minute=0;minute<1440;minute++) {
    const p=dayPhase(`${String(Math.floor(minute/60)).padStart(2,"0")}:${String(minute%60).padStart(2,"0")}`)!;
    assert.ok(p.progress>=0 && p.progress<=1);
    assert.ok(p.x>=24 && p.x<=176 && p.y>=32 && p.y<=139);
  }
});
