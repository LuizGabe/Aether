param([string]$Version)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'release-common.ps1')
$aetherRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$rootPackage = Get-Content -LiteralPath (Join-Path $aetherRoot 'package.json') -Raw | ConvertFrom-Json
if (!$Version) { $Version = $rootPackage.version }
if ($Version -notmatch '^\d+\.\d+\.\d+$' -or $Version -ne $rootPackage.version) { throw 'A release deve usar a versao estavel registrada em package.json.' }
$desktopPackage = Get-Content -LiteralPath (Join-Path $aetherRoot 'apps\desktop\package.json') -Raw | ConvertFrom-Json
$tauri = Get-Content -LiteralPath (Join-Path $aetherRoot 'apps\desktop\src-tauri\tauri.conf.json') -Raw | ConvertFrom-Json
$apiPom = [xml](Get-Content -LiteralPath (Join-Path $aetherRoot 'apps\api\pom.xml') -Raw)
$cargoManifest = Get-Content -LiteralPath (Join-Path $aetherRoot 'apps\desktop\src-tauri\Cargo.toml') -Raw
if ($desktopPackage.version -ne $Version -or $tauri.version -ne $Version -or $apiPom.project.version -ne $Version -or $cargoManifest -notmatch ('(?m)^version = "' + [regex]::Escape($Version) + '"\r?$')) { throw 'Sincronize as versoes do workspace, API, Cargo e Tauri antes de publicar.' }
$localRust = Join-Path $aetherRoot '.tools\rustup'
if (Test-Path -LiteralPath $localRust) { $env:RUSTUP_HOME = $localRust; $env:RUSTUP_TOOLCHAIN = 'stable' }
Push-Location $aetherRoot
try {
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'prepare-runtime.ps1')
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao preparar o runtime Java.' }
    & node (Join-Path $PSScriptRoot 'collect-licenses.mjs')
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao coletar os avisos de terceiros.' }
    & pnpm --filter desktop exec tauri build --bundles nsis --config src-tauri/tauri.release.conf.json
    if ($LASTEXITCODE -ne 0) { throw 'Falha no build do instalador. Nenhuma release sera publicada.' }
    $installer = Get-ChildItem -LiteralPath (Join-Path $aetherRoot 'apps\desktop\src-tauri\target\release\bundle\nsis') -Filter "*_${Version}_x64-setup.exe" | Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
    if (!$installer) { throw 'Instalador Windows x64 nao encontrado.' }
    $output = Join-Path $aetherRoot "dist\releases\$Version"
    New-Item -ItemType Directory -Force -Path $output | Out-Null
    $asset = "Aether-v$Version-windows-x64-setup.exe"
    Copy-Item -LiteralPath $installer.FullName -Destination (Join-Path $output $asset) -Force
    Copy-Item -LiteralPath (Join-Path $aetherRoot 'LICENSE') -Destination (Join-Path $output 'LICENSE.txt') -Force
    Copy-Item -LiteralPath (Join-Path $aetherRoot 'THIRD_PARTY_NOTICES.md') -Destination $output -Force
    Copy-Item -LiteralPath (Join-Path $aetherRoot 'docs\release-quickstart.md') -Destination (Join-Path $output 'LEIA-ME.md') -Force
    $runtime = Get-Content -LiteralPath (Join-Path $aetherRoot 'apps\desktop\src-tauri\resources\runtime\aether-runtime.json') -Raw | ConvertFrom-Json
    $revision = (& git rev-parse HEAD).Trim()
    $dirty = @(& git status --porcelain).Count -gt 0
    @{ product = 'Aether'; version = $Version; platform = 'windows-x64'; commit = $revision; workingTreeModified = $dirty; createdAtUtc = [DateTime]::UtcNow.ToString('o'); javaRuntime = $runtime; webview2 = 'offlineInstaller'; applicationTestsExecuted = $false; asset = $asset } | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath (Join-Path $output 'BUILD-MANIFEST.json') -Encoding UTF8
    $files = @($asset, 'LICENSE.txt', 'THIRD_PARTY_NOTICES.md', 'LEIA-ME.md', 'BUILD-MANIFEST.json')
    $checksums = foreach ($file in $files) { '{0}  {1}' -f (Get-AetherSha256 (Join-Path $output $file)), $file }
    [IO.File]::WriteAllLines((Join-Path $output 'SHA256SUMS.txt'), [string[]]$checksums, [Text.UTF8Encoding]::new($false))
    Write-Output "Release compilada: $output"
}
finally { Pop-Location }
