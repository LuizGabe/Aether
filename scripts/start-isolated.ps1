param([ValidateSet('PrimeiroAcesso','Restaurado')][string]$Perfil = 'PrimeiroAcesso')
$ErrorActionPreference = 'Stop'
$aetherRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$aetherPackage = Join-Path $aetherRoot 'dist\Laboratorio-Aether\app'
$aetherRelative = if ($Perfil -eq 'PrimeiroAcesso') { '.tools\first-run-lab\zero' } else { '.tools\first-run-lab\restaurado' }
$aetherData = Join-Path $aetherRoot $aetherRelative
$aetherPort = if ($Perfil -eq 'PrimeiroAcesso') { 38482 } else { 38483 }
$aetherExe = Join-Path $aetherPackage 'Aether.exe'
if (!(Test-Path -LiteralPath $aetherExe)) { throw 'A cópia de laboratório não foi preparada. Consulte docs/tutorial-primeiro-acesso.md.' }
if (Get-NetTCPConnection -LocalPort $aetherPort -State Listen -ErrorAction SilentlyContinue) { throw "O perfil já está aberto ou a porta $aetherPort está em uso." }
New-Item -ItemType Directory -Force -Path $aetherData | Out-Null
Start-Process -FilePath $aetherExe -ArgumentList @('--data-dir', ('"' + $aetherData + '"'), '--api-port', $aetherPort) -WorkingDirectory $aetherPackage -WindowStyle Hidden
Write-Output "Perfil $Perfil aberto em $aetherData (porta $aetherPort)."
