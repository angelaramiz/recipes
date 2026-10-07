# release.ps1 — Pipeline de release: firma, versionado, APK a la raiz y version.json.
# Uso: .\release.ps1 -VersionCode 2 -VersionName "1.1.0"
# Requiere: android/keystore.properties (lo genera solo la primera vez con keytool del JBR).
# NO commitea: revisa recetas.apk + version.json y haz commit + push tu.
param(
  [Parameter(Mandatory = $true)][int]$VersionCode,
  [Parameter(Mandatory = $true)][string]$VersionName
)

$ErrorActionPreference = "Stop"
$AndroidDir = $PSScriptRoot
$RootDir = Split-Path -Parent $AndroidDir
$JbrBin = "C:\Program Files\Android\Android Studio\jbr\bin"
$KsProps = Join-Path $AndroidDir "keystore.properties"
$KsFile = Join-Path $AndroidDir "release.keystore"

function Paso([string]$m) { Write-Host ""; Write-Host "=== $m ===" -ForegroundColor Cyan }

# keytool/gradle hablan por stderr: no dejar que $ErrorActionPreference=Stop los mate.
function Invoke-Nativo {
  param([scriptblock]$Cmd)
  $prev = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try { & $Cmd 2>&1 | Out-Null } finally { $ErrorActionPreference = $prev }
  if ($LASTEXITCODE -ne 0) { throw "fallo nativo (exit $LASTEXITCODE)" }
}
# 1. Keystore (solo primera vez; GUARDA release.keystore + keystore.properties o pierdes las updates)
if (-not (Test-Path -LiteralPath $KsFile)) {
  Paso "Generando keystore (solo primera vez)"
  $chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
  $pass = -join ((1..24) | ForEach-Object { $chars[(Get-Random -Maximum $chars.Length)] })
  Invoke-Nativo { & "$JbrBin\keytool.exe" -genkeypair -keystore $KsFile -alias recetas -keyalg RSA -keysize 2048 `
    -validity 9125 -storepass $pass -keypass $pass -dname "CN=recetas" }
  Set-Content -LiteralPath $KsProps ("storeFile=release.keystore`nstorePassword=$pass`nkeyAlias=recetas`nkeyPassword=$pass")
  Write-Host "  Keystore creado. RESPALDA android/release.keystore (sin el no hay updates)." -ForegroundColor Yellow
}

# 2. Bump versionCode/versionName en app/build.gradle.kts
Paso "Version $VersionName (code $VersionCode)"
$GradleFile = Join-Path $AndroidDir "app\build.gradle.kts"
$t = Get-Content -LiteralPath $GradleFile -Raw
$t = $t -replace "versionCode = \d+", "versionCode = $VersionCode"
$t = $t -replace 'versionName = "[^"]+"', "versionName = `"$VersionName`""
Set-Content -LiteralPath $GradleFile $t -NoNewline

# 3. Build release
Paso "assembleRelease"
Push-Location $AndroidDir
try {
  Invoke-Nativo { & .\gradlew.bat :app:assembleRelease --console=plain }
} finally { Pop-Location }

# 4. Copiar APK a la raiz (lo sirve Render) + version.json
$Apk = Join-Path $AndroidDir "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path -LiteralPath $Apk)) { throw "no se genero el APK" }
Paso "Publicando"
Copy-Item -LiteralPath $Apk -Destination (Join-Path $RootDir "recetas.apk") -Force
$v = @{ versionCode = $VersionCode; versionName = $VersionName; apkUrl = "/recetas.apk" } | ConvertTo-Json -Compress
Set-Content -LiteralPath (Join-Path $RootDir "version.json") $v -NoNewline
$mb = [math]::Round((Get-Item (Join-Path $RootDir "recetas.apk")).Length / 1MB, 2)
Write-Host "  recetas.apk ($mb MB) + version.json listos." -ForegroundColor Green

# 5. Verificar firma
$Apksigner = Join-Path $env:LOCALAPPDATA "Android\Sdk\build-tools\36.0.0\apksigner.bat"
if (Test-Path -LiteralPath $Apksigner) {
  & $Apksigner verify --print-certs (Join-Path $RootDir "recetas.apk") 2>&1 | Select-String -Pattern "Signer #1" | Select-Object -First 2
} else {
  Write-Host "  (apksigner no encontrado, salto verificacion)" -ForegroundColor Yellow
}
Write-Host ""
Write-Host "Siguiente: commit + push (Render despliega solo). La app instalada detectara v$VersionName por OTA." -ForegroundColor Green
