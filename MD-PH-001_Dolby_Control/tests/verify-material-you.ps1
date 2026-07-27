param(
    [string]$ProjectRoot = (Split-Path $PSScriptRoot -Parent)
)

$ErrorActionPreference = "Stop"
$manifest = Get-Content -Raw -LiteralPath (Join-Path $ProjectRoot "AndroidManifest.xml")
$mainActivity = Get-Content -Raw -LiteralPath (
    Join-Path $ProjectRoot "src\com\mdph\dolbycontrol\MainActivity.java")
$geqEditor = Get-Content -Raw -LiteralPath (
    Join-Path $ProjectRoot "src\com\mdph\dolbycontrol\GeqEditorView.java")
$service = Get-Content -Raw -LiteralPath (
    Join-Path $ProjectRoot "src\com\mdph\dolbycontrol\DolbyControlService.java")
$schemePath = Join-Path $ProjectRoot "src\com\mdph\dolbycontrol\MaterialColorScheme.java"

if ($manifest -notmatch 'android:label="MIAD01 Dolby Atoms"') {
    throw "Application label must be MIAD01 Dolby Atoms"
}
if ($manifest -notmatch 'android:versionCode="107"' -or
    $manifest -notmatch 'android:versionName="1\.0\.7"') {
    throw "Material You APK version must be 1.0.7 (107)"
}
if ($manifest -match 'Theme\.Material\.Light') {
    throw "Manifest must not force the light-only Material theme"
}
if (-not (Test-Path -LiteralPath $schemePath -PathType Leaf)) {
    throw "MaterialColorScheme.java is missing"
}

$scheme = Get-Content -Raw -LiteralPath $schemePath
$themeSources = $mainActivity + "`n" + $scheme
foreach ($required in @(
    "UI_MODE_NIGHT_MASK",
    "system_accent1_",
    "system_neutral1_",
    "system_neutral2_")) {
    if ($themeSources -notmatch [regex]::Escape($required)) {
        throw "Material You integration is missing: $required"
    }
}
if ($mainActivity -notmatch 'applySystemBars') {
    throw "System bars are not themed"
}
if ($mainActivity -match 'setStatusBarColor\(Color\.rgb' -or
    $mainActivity -match 'scroll\.setBackgroundColor\(Color\.rgb') {
    throw "MainActivity still forces a fixed light palette"
}
if ($geqEditor -notmatch 'setColorScheme') {
    throw "Graphic equalizer does not receive the Material color scheme"
}
if ($service -notmatch 'setContentTitle\(uiText\.get\(UiText\.Key\.APP_TITLE\)\)') {
    throw "Foreground notification does not use the MIAD01 Dolby Atoms app title"
}

if ($mainActivity -notmatch 'THEME_LIGHT' -or $mainActivity -notmatch 'THEME_DARK' -or
    $mainActivity -notmatch 'THEME_SYSTEM') {
    throw "Theme selector is missing"
}
if ($mainActivity -notmatch 'statusConnected' -or $mainActivity -notmatch 'statusDisconnected') {
    throw "Semantic status colors are missing"
}
$dax = Get-Content -Raw -LiteralPath (
    Join-Path $ProjectRoot "src\com\mdph\dolbycontrol\DaxController.java")
if ($service -notmatch 'syncOutputDevice' -or $dax -notmatch 'syncOutputDevice') {
    throw "Global DAP output route synchronization is missing"
}
if ($dax -notmatch 'getDeclaredMethod\(') {
    throw "Android 14 hidden DAP parameter methods are not accessed safely"
}

$oldBrand = Get-ChildItem -LiteralPath (Join-Path $ProjectRoot "src") -Recurse -File |
    Select-String -Pattern "MD-PH-001 Dolby"
if ($oldBrand -or $manifest -match 'MD-PH-001 Dolby') {
    throw "Old MD-PH-001 Dolby branding remains"
}

Write-Host "verify-material-you PASS"
