$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$localMaven = Join-Path $repoRoot ".tools\apache-maven-3.9.16\bin\mvn.cmd"
$localRepo = Join-Path $repoRoot ".tools\m2"
$mavenArgs = @("-Dmaven.repo.local=$localRepo") + $args

if (Test-Path $localMaven) {
    & $localMaven @mavenArgs
    exit $LASTEXITCODE
}

& mvn @mavenArgs
exit $LASTEXITCODE
