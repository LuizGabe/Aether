import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const output = path.join(root, "apps/desktop/src-tauri/resources/third-party-licenses");
fs.mkdirSync(output, { recursive: true });
const legalName = /^(licen[cs]e(?:[-_.].*)?|copying(?:[-_.].*)?|notice(?:[-_.].*)?|authors(?:[-_.].*)?)$/i;
const inventory = [];
function collect(directory, ecosystem, name, version, license, repository) {
  if (!directory || !fs.existsSync(directory)) return;
  const safeName = `${ecosystem}-${name}-${version}`.replace(/[^A-Za-z0-9._-]/g, "_");
  const destination = path.join(output, safeName);
  const files = fs.readdirSync(directory, { withFileTypes: true }).filter(e => e.isFile() && legalName.test(e.name));
  for (const file of files) {
    fs.mkdirSync(destination, { recursive: true });
    fs.copyFileSync(path.join(directory, file.name), path.join(destination, file.name));
  }
  inventory.push({ ecosystem, name, version, license, repository: typeof repository === "string" ? repository : repository?.url, notices: files.map(f => f.name) });
}
const pnpm = process.platform === "win32" ? "pnpm.cmd" : "pnpm";
const options = { cwd: root, encoding: "utf8", maxBuffer: 32 * 1024 * 1024 };
const dependencyOutput = process.platform === "win32"
  ? execFileSync(process.env.ComSpec || "cmd.exe", ["/d", "/s", "/c", "pnpm list -r --prod --depth Infinity --json"], options)
  : execFileSync(pnpm, ["list", "-r", "--prod", "--depth", "Infinity", "--json"], options);
const projects = JSON.parse(dependencyOutput);
const seen = new Set();
function walk(dependencies) {
  for (const [name, info] of Object.entries(dependencies ?? {})) {
    if (info.path && !seen.has(info.path)) {
      seen.add(info.path);
      const manifest = path.join(info.path, "package.json");
      if (fs.existsSync(manifest)) {
        const metadata = JSON.parse(fs.readFileSync(manifest, "utf8"));
        collect(info.path, "npm", metadata.name || name, metadata.version, metadata.license, metadata.repository);
      }
      walk(info.dependencies);
    }
  }
}
for (const project of projects) walk(project.dependencies);
const cargo = JSON.parse(execFileSync("cargo", ["metadata", "--locked", "--format-version", "1", "--filter-platform", "x86_64-pc-windows-msvc", "--manifest-path", path.join(root, "apps/desktop/src-tauri/Cargo.toml")], { cwd: root, encoding: "utf8", maxBuffer: 64 * 1024 * 1024 }));
for (const pkg of cargo.packages) if (pkg.source?.startsWith("registry+")) collect(path.dirname(pkg.manifest_path), "cargo", pkg.name, pkg.version, pkg.license, pkg.repository);
fs.writeFileSync(path.join(output, "COMPONENTS.json"), JSON.stringify(inventory, null, 2) + "\n");
console.log(`Avisos de ${inventory.length} componentes reunidos para distribuicao.`);
