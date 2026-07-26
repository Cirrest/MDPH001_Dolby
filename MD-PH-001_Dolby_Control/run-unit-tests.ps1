param(
    [string]$JdkRoot = "E:\sdk\_tools\jdk17"
)

$ErrorActionPreference = "Stop"
$projectRoot = Split-Path $MyInvocation.MyCommand.Path -Parent
$testBuildDir = Join-Path $projectRoot "build\unit-tests"

$javac = Get-ChildItem -Recurse -File $JdkRoot -Filter javac.exe |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $javac) {
    throw "javac.exe was not found under $JdkRoot"
}
$java = Join-Path (Split-Path $javac -Parent) "java.exe"

New-Item -ItemType Directory -Force -Path $testBuildDir | Out-Null

$sources = @(
    Join-Path $projectRoot "src\com\mdph\dolbycontrol\DaxParameterProtocol.java"
    Join-Path $projectRoot "src\com\mdph\dolbycontrol\GeqGainMapper.java"
    Join-Path $projectRoot "src\com\mdph\dolbycontrol\ModePolicy.java"
    Join-Path $projectRoot "src\com\mdph\dolbycontrol\ControlValuePolicy.java"
    Join-Path $projectRoot "src\com\mdph\dolbycontrol\DolbySnapshot.java"
    Join-Path $projectRoot "tests\com\mdph\dolbycontrol\DaxParameterProtocolTest.java"
    Join-Path $projectRoot "tests\com\mdph\dolbycontrol\GeqGainMapperTest.java"
    Join-Path $projectRoot "tests\com\mdph\dolbycontrol\ModePolicyTest.java"
    Join-Path $projectRoot "tests\com\mdph\dolbycontrol\ControlValuePolicyTest.java"
    Join-Path $projectRoot "tests\com\mdph\dolbycontrol\DolbySnapshotTest.java"
)

& $javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options `
    -d $testBuildDir $sources
if ($LASTEXITCODE -ne 0) {
    throw "unit test compilation failed: $LASTEXITCODE"
}

$tests = @(
    "com.mdph.dolbycontrol.DaxParameterProtocolTest"
    "com.mdph.dolbycontrol.GeqGainMapperTest"
    "com.mdph.dolbycontrol.ModePolicyTest"
    "com.mdph.dolbycontrol.ControlValuePolicyTest"
    "com.mdph.dolbycontrol.DolbySnapshotTest"
)

foreach ($test in $tests) {
    & $java -cp $testBuildDir $test
    if ($LASTEXITCODE -ne 0) {
        throw "unit test failed: $test"
    }
}
