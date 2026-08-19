# ============================================
# Chess Like Game - Release Build Script
# ============================================

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "============================================"
Write-Host "       Chess Like Game Release Build"
Write-Host "============================================"
Write-Host ""

# --------------------------------------------
# Project settings
# --------------------------------------------

$ProjectName = "Chess Like Game"
$MainClass = "game.Main"

$TargetDir = "target"
$PackageDir = "$TargetDir\package"

$ReleaseDir = "release"
$AppDir = "$ReleaseDir\$ProjectName"
$ZipPath = "$ReleaseDir\$ProjectName.zip"

# --------------------------------------------
# Check Java
# --------------------------------------------

Write-Host "[1/7] Checking Java..."
Write-Host ""

java --version

if ($LASTEXITCODE -ne 0) {
    throw "Java could not be found."
}

# --------------------------------------------
# Check Maven
# --------------------------------------------

Write-Host ""
Write-Host "[2/7] Checking Maven..."
Write-Host ""

mvn --version

if ($LASTEXITCODE -ne 0) {
    throw "Maven could not be found."
}

# --------------------------------------------
# Check jpackage
# --------------------------------------------

Write-Host ""
Write-Host "[3/7] Checking jpackage..."
Write-Host ""

jpackage --version

if ($LASTEXITCODE -ne 0) {
    throw "jpackage could not be found."
}

# --------------------------------------------
# Build Maven project
# --------------------------------------------

Write-Host ""
Write-Host "[4/7] Building Maven project..."
Write-Host ""

mvn clean package

if ($LASTEXITCODE -ne 0) {
    throw "Maven build failed."
}

# --------------------------------------------
# Find application JAR
# --------------------------------------------

Write-Host ""
Write-Host "[5/7] Preparing application files..."
Write-Host ""

$JarFiles = Get-ChildItem "$TargetDir\*.jar"

if ($JarFiles.Count -eq 0) {
    throw "No application JAR was found in $TargetDir."
}

if ($JarFiles.Count -gt 1) {
    throw "Multiple JAR files were found in $TargetDir. Please specify which one should be packaged."
}

$JarFile = $JarFiles[0]
$JarName = $JarFile.Name

Write-Host "Application JAR:"
Write-Host "    $JarName"

# --------------------------------------------
# Recreate temporary package directory
# --------------------------------------------

if (Test-Path $PackageDir) {
    Remove-Item $PackageDir -Recurse -Force
}

New-Item `
    -ItemType Directory `
    -Path "$PackageDir\lib" `
    -Force |
    Out-Null

# Copy application JAR

Copy-Item `
    $JarFile.FullName `
    "$PackageDir\$JarName"

# Copy JavaFX dependencies

$JavaFxJars = Get-ChildItem "$TargetDir\lib\*.jar"

if ($JavaFxJars.Count -eq 0) {
    throw "No JavaFX dependencies were found in $TargetDir\lib."
}

Copy-Item `
    "$TargetDir\lib\*.jar" `
    "$PackageDir\lib\" `
    -Force

# --------------------------------------------
# Recreate release directory
# --------------------------------------------

if (Test-Path $ReleaseDir) {
    Remove-Item $ReleaseDir -Recurse -Force
}

New-Item `
    -ItemType Directory `
    -Path $ReleaseDir `
    -Force |
    Out-Null

# --------------------------------------------
# Create application image
# --------------------------------------------

Write-Host ""
Write-Host "Creating application image..."
Write-Host ""

jpackage `
    --type app-image `
    --name "$ProjectName" `
    --input "$PackageDir" `
    --main-jar "$JarName" `
    --main-class "$MainClass" `
    --module-path "$TargetDir\lib" `
    --add-modules javafx.controls,javafx.graphics,javafx.base `
    --java-options "--enable-native-access=javafx.graphics" `
    --dest "$ReleaseDir"

if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed."
}

# --------------------------------------------
# jpackage creates:
#
# release\
# └── Chess Like Game\
#     ├── Chess Like Game.exe
#     ├── app\
#     └── runtime\
#
# --------------------------------------------

if (-not (Test-Path $AppDir)) {
    throw "Application directory was not created: $AppDir"
}

$ExePath = "$AppDir\$ProjectName.exe"

if (-not (Test-Path $ExePath)) {
    throw "Application EXE was not created: $ExePath"
}

# --------------------------------------------
# Create ZIP
# --------------------------------------------

Write-Host ""
Write-Host "[6/7] Creating ZIP archive..."
Write-Host ""

Compress-Archive `
    -Path $AppDir `
    -DestinationPath $ZipPath `
    -Force

if (-not (Test-Path $ZipPath)) {
    throw "ZIP archive was not created."
}

# --------------------------------------------
# Final information
# --------------------------------------------

Write-Host ""
Write-Host "[7/7] Release build complete!"
Write-Host ""

Write-Host "============================================"
Write-Host "             BUILD SUCCESSFUL"
Write-Host "============================================"
Write-Host ""

Write-Host "Application:"
Write-Host "    $ExePath"
Write-Host ""

Write-Host "ZIP:"
Write-Host "    $ZipPath"
Write-Host ""

Write-Host "============================================"
Write-Host ""