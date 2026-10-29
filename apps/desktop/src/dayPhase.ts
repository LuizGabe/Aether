export function dayPhase(time: string | null | undefined) {
  if (!time || !/^([01]\d|2[0-3]):[0-5]\d$/.test(time)) return null;
  const [hour, minute] = time.split(":").map(Number), minutes = hour * 60 + minute;
  const scene: "dawn" | "day" | "dusk" | "night" = minutes >= 300 && minutes < 480 ? "dawn" : minutes >= 480 && minutes < 960 ? "day" : minutes >= 960 && minutes < 1140 ? "dusk" : "night";
  const daylight = scene !== "night";
  // Two illustrative sky passages: sun 05–19h, moon 19–05h. Never astronomical forecasts.
  const progress = daylight ? (minutes - 300) / 840 : ((minutes - 1140 + 1440) % 1440) / 600;
  const angle = Math.PI * (1.13 - 1.26 * progress);
  return { minutes, position: minutes / 1440 * 100, daylight, scene, progress,
    x: 100 + 75 * Math.cos(angle), y: 108 - 75 * Math.sin(angle),
    emoji: daylight ? "☀️" : "🌙", label: scene === "dawn" ? "Amanhecer" : scene === "day" ? "Dia" : scene === "dusk" ? "Entardecer" : hour < 5 ? "Madrugada" : "Noite" };
}
