param(
    [string]$ProjectRoot = (Split-Path $PSScriptRoot -Parent)
)

$ErrorActionPreference = "Stop"
$buildScript = Get-Content -Raw -LiteralPath (Join-Path $ProjectRoot "build.ps1")

if ($buildScript -notmatch '\$KeystorePath\s*=\s*"E:\\Codex\\keystore"') {
    throw "build.ps1 must default to E:\Codex\keystore"
}
if ($buildScript -notmatch '\$KeystoreAlias\s*=\s*"keystore"') {
    throw "build.ps1 must default to the keystore alias"
}
if ($buildScript -match 'debug\.keystore' -or $buildScript -match 'genkeypair') {
    throw "build.ps1 must not create or use a debug keystore"
}
if ($buildScript -match 'pass:android') {
    throw "build.ps1 must not contain the Android debug password"
}
if ($buildScript -notmatch 'env:MDPH_KEYSTORE_PASSWORD') {
    throw "build.ps1 must pass the signing password through an environment variable"
}
if ($buildScript -notmatch '--lineage') {
    throw "build.ps1 must include the signing lineage for upgrades from version 1.0.1"
}
if (-not (Test-Path -LiteralPath (Join-Path $ProjectRoot "signing-lineage.bin") -PathType Leaf)) {
    throw "signing-lineage.bin is missing"
}

Write-Host "verify-signing-config PASS"
