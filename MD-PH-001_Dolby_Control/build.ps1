param(
    [string]$SdkRoot = "E:\sdk\tools\android-sdk",
    [string]$JdkRoot = "E:\sdk\tools\jdk17"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path $MyInvocation.MyCommand.Path -Parent
$buildDir = Join-Path $projectRoot "build"
$classesDir = Join-Path $buildDir "classes"
$dexDir = Join-Path $buildDir "dex"
$androidJar = Join-Path $SdkRoot "platforms\android-34\android.jar"
$buildTools = Join-Path $SdkRoot "build-tools\34.0.0"
$manifest = Join-Path $projectRoot "AndroidManifest.xml"
$unsignedApk = Join-Path $buildDir "control-unsigned.apk"
$alignedApk = Join-Path $buildDir "control-aligned.apk"
$outputApk = Join-Path $buildDir "DolbyControl.apk"
$keystore = Join-Path $buildDir "debug.keystore"

$javac = Get-ChildItem -Recurse -File $JdkRoot -Filter javac.exe |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $javac) {
    throw "javac.exe was not found under $JdkRoot"
}
$javaBin = Split-Path $javac -Parent
$jar = Join-Path $javaBin "jar.exe"
$keytool = Join-Path $javaBin "keytool.exe"
$env:JAVA_HOME = Split-Path $javaBin -Parent

New-Item -ItemType Directory -Force -Path $classesDir, $dexDir | Out-Null

$sources = Get-ChildItem -Recurse -File (Join-Path $projectRoot "src") -Filter *.java |
    Select-Object -ExpandProperty FullName
if (-not $sources) {
    throw "No Java sources found"
}

$javacArgs = @(
    "-encoding", "UTF-8",
    "-source", "8",
    "-target", "8",
    "-Xlint:-options",
    "-bootclasspath", $androidJar,
    "-d", $classesDir
) + $sources
$javacProcess = Start-Process -FilePath $javac -ArgumentList $javacArgs `
    -Wait -NoNewWindow -PassThru
if ($javacProcess.ExitCode -ne 0) {
    throw "javac failed: $($javacProcess.ExitCode)"
}

$classesJar = Join-Path $buildDir "classes.jar"
& $jar --create --file $classesJar -C $classesDir .
if ($LASTEXITCODE -ne 0) {
    throw "jar failed: $LASTEXITCODE"
}

& (Join-Path $buildTools "d8.bat") --min-api 23 --lib $androidJar `
    --output $dexDir $classesJar
if ($LASTEXITCODE -ne 0) {
    throw "d8 failed: $LASTEXITCODE"
}

& (Join-Path $buildTools "aapt2.exe") link -o $unsignedApk `
    --manifest $manifest -I $androidJar --min-sdk-version 23 --target-sdk-version 29
if ($LASTEXITCODE -ne 0) {
    throw "aapt2 link failed: $LASTEXITCODE"
}

Push-Location $dexDir
try {
    & (Join-Path $buildTools "aapt.exe") add $unsignedApk "classes.dex"
    if ($LASTEXITCODE -ne 0) {
        throw "aapt add failed: $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

& (Join-Path $buildTools "zipalign.exe") -f 4 $unsignedApk $alignedApk
if ($LASTEXITCODE -ne 0) {
    throw "zipalign failed: $LASTEXITCODE"
}

if (-not (Test-Path -LiteralPath $keystore)) {
    & $keytool -genkeypair -noprompt `
        -keystore $keystore -storepass android -keypass android `
        -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 `
        -dname "CN=Android Debug,O=sdk,C=US"
    if ($LASTEXITCODE -ne 0) {
        throw "keytool failed: $LASTEXITCODE"
    }
}

& (Join-Path $buildTools "apksigner.bat") sign `
    --ks $keystore --ks-pass pass:android --key-pass pass:android `
    --out $outputApk $alignedApk
if ($LASTEXITCODE -ne 0) {
    throw "apksigner failed: $LASTEXITCODE"
}

& (Join-Path $buildTools "apksigner.bat") verify --verbose --print-certs $outputApk
if ($LASTEXITCODE -ne 0) {
    throw "APK signature verification failed: $LASTEXITCODE"
}

Get-Item $outputApk | Select-Object Length, FullName
