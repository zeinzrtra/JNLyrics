Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " Building JNLyrics Multi-Version Artifacts " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Build 26.x (Java 25)
Write-Host "[1/2] Building Minecraft 26.x (Java 25)..." -ForegroundColor Yellow
Push-Location "26.x"
& ..\gradlew build
Pop-Location

# 2. Build 1.21-modern (Java 21)
Write-Host "[2/2] Building Minecraft 1.21.2 - 1.21.11 (Java 21)..." -ForegroundColor Yellow
Push-Location "1.21-modern"
& ..\gradlew remapJar
Pop-Location

# 3. Collect into dist/
$distDir = "dist"
if (!(Test-Path $distDir)) { New-Item -ItemType Directory -Path $distDir -Force }

Copy-Item "26.x\build\libs\jnlyrics-1.0.0+26.2.jar" "$distDir\jnlyrics-1.0.0+26.2.jar" -Force
Copy-Item "1.21-modern\build\libs\jnlyrics-1.0.0+1.21.4.jar" "$distDir\jnlyrics-1.0.0+1.21.4.jar" -Force

Write-Host "`nAll JARs successfully built into dist/ folder:" -ForegroundColor Green
Get-ChildItem $distDir | Select-Object Name, Length, LastWriteTime | Format-Table -AutoSize
