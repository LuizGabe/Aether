$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'release-common.ps1')
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$aetherRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$runtimeDirectory = Join-Path $aetherRoot 'apps\desktop\src-tauri\resources\runtime'
$cacheDirectory = Join-Path $aetherRoot '.tools\runtime-cache'
New-Item -ItemType Directory -Force -Path $cacheDirectory | Out-Null
$api = 'https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&heap_size=normal&image_type=jre&jvm_impl=hotspot&os=windows&vendor=eclipse'
$release = @(Invoke-RestMethod -Uri $api -Headers @{ 'User-Agent' = 'Aether-release-builder' })[0]
$package = $release.binary.package
if (!$package -or $package.checksum -notmatch '^[a-fA-F0-9]{64}$' -or $package.name -notmatch '^[A-Za-z0-9_.+%-]+\.zip$') { throw 'Metadados do runtime Java invalidos.' }
$metadataPath = Join-Path $runtimeDirectory 'aether-runtime.json'
if (Test-Path -LiteralPath $metadataPath) {
    $existing = Get-Content -LiteralPath $metadataPath -Raw | ConvertFrom-Json
    if ($existing.sha256 -eq $package.checksum -and (Test-Path -LiteralPath (Join-Path $runtimeDirectory 'bin\java.exe'))) {
        Write-Output "Runtime Java reutilizado: $($existing.version)"
        exit 0
    }
}
$archive = Join-Path $cacheDirectory $package.name
if (!(Test-Path -LiteralPath $archive) -or (Get-AetherSha256 $archive) -ne $package.checksum) {
    Invoke-WebRequest -UseBasicParsing -Uri $package.link -OutFile $archive
}
if ((Get-AetherSha256 $archive) -ne $package.checksum) { throw 'SHA-256 do runtime Java nao confere; o pacote nao sera usado.' }
$extractDirectory = Join-Path $cacheDirectory ('extract-' + [guid]::NewGuid().ToString('N'))
Add-Type -AssemblyName System.IO.Compression.FileSystem
[IO.Compression.ZipFile]::ExtractToDirectory($archive, $extractDirectory)
$roots = @(Get-ChildItem -LiteralPath $extractDirectory -Directory | Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\java.exe') })
if ($roots.Count -ne 1 -or !(Test-Path -LiteralPath (Join-Path $roots[0].FullName 'legal'))) { throw 'O arquivo oficial nao contem o runtime e seus avisos legais esperados.' }
if (Test-Path -LiteralPath $runtimeDirectory) {
    $resolved = (Resolve-Path -LiteralPath $runtimeDirectory).Path
    $expected = [IO.Path]::GetFullPath((Join-Path $aetherRoot 'apps\desktop\src-tauri\resources\runtime'))
    if ($resolved -ne $expected -or !(Test-Path -LiteralPath $metadataPath)) { throw 'Pasta existente sem marcador de runtime gerado; remocao automatica recusada.' }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}
New-Item -ItemType Directory -Path $runtimeDirectory | Out-Null
Get-ChildItem -LiteralPath $roots[0].FullName -Force | Copy-Item -Destination $runtimeDirectory -Recurse -Force
@{ vendor = 'Eclipse Adoptium'; version = $release.version.semver; release = $release.release_name; source = $package.link; sha256 = $package.checksum; license = 'GPL-2.0 WITH Classpath-exception-2.0' } | ConvertTo-Json | Set-Content -LiteralPath $metadataPath -Encoding UTF8
Write-Output "Runtime Java incluido: $($release.release_name), checksum conferido e avisos legais preservados."
