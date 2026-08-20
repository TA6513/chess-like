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
# Java / JDK settings
# --------------------------------------------

$JavaHome = "C:\Program Files\Java\jdk-26.0.2"

$JavaExe = "$JavaHome\bin\java.exe"
$JavacExe = "$JavaHome\bin\javac.exe"
$JpackageExe = "$JavaHome\bin\jpackage.exe"

# --------------------------------------------
# Check JDK
# --------------------------------------------

Write-Host "[1/7] Checking JDK..."
Write-Host ""

if (-not (Test-Path $JavaExe)) {
    throw "Java was not found at: $JavaExe"
}

if (-not (Test-Path $JavacExe)) {
    throw "javac was not found at: $JavacExe"
}

if (-not (Test-Path $JpackageExe)) {
    throw "jpackage was not found at: $JpackageExe"
}

# Make sure Maven and other tools use this JDK.
$env:JAVA_HOME = $JavaHome

# Put this JDK first on PATH for this script.
$env:Path = "$JavaHome\bin;$env:Path"

Write-Host "JAVA_HOME:"
Write-Host "    $env:JAVA_HOME"
Write-Host ""

Write-Host "Java:"
& $JavaExe --version

if ($LASTEXITCODE -ne 0) {
    throw "Java could not be executed."
}

Write-Host ""

Write-Host "javac:"
& $JavacExe --version

if ($LASTEXITCODE -ne 0) {
    throw "javac could not be executed."
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

& $JpackageExe --version

if ($LASTEXITCODE -ne 0) {
    throw "jpackage could not be executed."
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

# Maven now creates both:
#
#   chess-like-game-X.X.X.jar
#   chess-like-game-X.X.X-server.jar
#
# Only the normal game JAR should be passed
# to jpackage.
$JarFiles = @(
    Get-ChildItem "$TargetDir\*.jar" |
    Where-Object {
        $_.Name -notlike "*-server.jar"
    }
)

if ($JarFiles.Count -eq 0) {
    throw "No application JAR was found in $TargetDir."
}

if ($JarFiles.Count -gt 1) {

    Write-Host ""
    Write-Host "Application JAR candidates found:"

    foreach ($File in $JarFiles) {

        Write-Host "    $($File.Name)"
    }

    throw "Multiple application JAR files were found in $TargetDir."
}

$JarFile = $JarFiles[0]
$JarName = $JarFile.Name

Write-Host "Application JAR:"
Write-Host "    $JarName"

# --------------------------------------------
# Find dedicated server JAR
# --------------------------------------------

$ServerJarFiles = @(
    Get-ChildItem "$TargetDir\*-server.jar"
)

if ($ServerJarFiles.Count -eq 1) {

    Write-Host ""
    Write-Host "Dedicated server JAR:"
    Write-Host "    $($ServerJarFiles[0].Name)"

}
elseif ($ServerJarFiles.Count -gt 1) {

    Write-Host ""
    Write-Host "WARNING: Multiple server JARs were found."

}
else {

    Write-Host ""
    Write-Host "WARNING: No dedicated server JAR was found."
}

# --------------------------------------------
# Recreate temporary package directory
# --------------------------------------------

if (Test-Path $PackageDir) {

    Remove-Item `
        $PackageDir `
        -Recurse `
        -Force
}

New-Item `
    -ItemType Directory `
    -Path "$PackageDir\lib" `
    -Force |
Out-Null

# --------------------------------------------
# Copy application JAR
# --------------------------------------------

Copy-Item `
    $JarFile.FullName `
    "$PackageDir\$JarName" `
    -Force

# --------------------------------------------
# Copy JavaFX dependencies
# --------------------------------------------

if (-not (Test-Path "$TargetDir\lib")) {

    throw "Dependency directory was not found: $TargetDir\lib"
}

$JavaFxJars = @(
    Get-ChildItem "$TargetDir\lib\*.jar"
)

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

    Remove-Item `
        $ReleaseDir `
        -Recurse `
        -Force
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

$JpackageArgs = @(
    "--type"
    "app-image"

    "--name"
    $ProjectName

    "--input"
    $PackageDir

    "--main-jar"
    $JarName

    "--main-class"
    $MainClass

    "--module-path"
    "$TargetDir\lib"

    "--add-modules"
    "javafx.controls,javafx.graphics,javafx.base"

    "--java-options"
    "--enable-native-access=javafx.graphics"

    "--dest"
    $ReleaseDir

    "--verbose"
)

& $JpackageExe @JpackageArgs

if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed."
}

# --------------------------------------------
# Verify application image
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

Write-Host "Windows application:"
Write-Host "    $ExePath"
Write-Host ""

Write-Host "Windows ZIP:"
Write-Host "    $ZipPath"
Write-Host ""

if ($ServerJarFiles.Count -eq 1) {

    Write-Host "Dedicated server JAR:"
    Write-Host "    $($ServerJarFiles[0].FullName)"
    Write-Host ""
}

Write-Host "============================================"
Write-Host ""