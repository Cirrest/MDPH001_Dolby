param(
    [string]$ProjectRoot = (Split-Path $PSScriptRoot -Parent)
)

$ErrorActionPreference = "Stop"
$sourceRoot = Join-Path $ProjectRoot "src"
$forbiddenPattern = '(?i)(/system_ext/bin/su|\bsu\b|dumpsys\s+media\.audio_flinger|setprop\s+ctl\.restart)'
$matches = Get-ChildItem -LiteralPath $sourceRoot -Recurse -File -Filter *.java |
    Select-String -Pattern $forbiddenPattern

if ($matches) {
    $locations = $matches | ForEach-Object {
        "$($_.Path):$($_.LineNumber):$($_.Line.Trim())"
    }
    throw "APK source must not invoke root or system-service shell commands:`n$($locations -join "`n")"
}

Write-Host "verify-no-root PASS"
