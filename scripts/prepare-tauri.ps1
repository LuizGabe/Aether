$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$apiDir = Join-Path $repoRoot "apps\api"
$desktopDir = Join-Path $repoRoot "apps\desktop"
$resourcesDir = Join-Path $desktopDir "src-tauri\resources"
$apiJar = Join-Path $apiDir "target\aether-api-0.0.1-SNAPSHOT.jar"
$bundledJar = Join-Path $resourcesDir "aether-api.jar"

Push-Location $apiDir
try {
    & (Join-Path $PSScriptRoot "mvn.ps1") package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}
finally {
    Pop-Location
}

New-Item -ItemType Directory -Force -Path $resourcesDir | Out-Null
Copy-Item -LiteralPath $apiJar -Destination $bundledJar -Force

Push-Location $desktopDir
try {
    pnpm build
    exit $LASTEXITCODE
}
finally {
    Pop-Location
}
