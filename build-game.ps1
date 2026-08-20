# ============================================
# Chess Like Game - Release Build Script
# ============================================

param (

    # Add -DeployServer when you want the newly
    # built server JAR uploaded to Linux.
    [switch]$DeployServer
)

$SshKey =
"$env:USERPROFILE\.ssh\chess-like-server-deploy"


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
# JDK settings
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

# Make Maven use this JDK.
$env:JAVA_HOME = $JavaHome

# Put this JDK first on PATH for this script.
$env:Path =
"$JavaHome\bin;$env:Path"

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
# Find normal application JAR
# --------------------------------------------

Write-Host ""
Write-Host "[5/7] Preparing application files..."
Write-Host ""

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

if ($ServerJarFiles.Count -eq 0) {

    throw "No dedicated server JAR was found."
}

if ($ServerJarFiles.Count -gt 1) {

    Write-Host ""
    Write-Host "Dedicated server JAR candidates:"

    foreach ($File in $ServerJarFiles) {

        Write-Host "    $($File.Name)"
    }

    throw "Multiple dedicated server JARs were found."
}

$ServerJarFile =
$ServerJarFiles[0]

Write-Host ""
Write-Host "Dedicated server JAR:"
Write-Host "    $($ServerJarFile.Name)"

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
# Create Windows application image
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
)

& $JpackageExe @JpackageArgs

if ($LASTEXITCODE -ne 0) {

    throw "jpackage failed."
}

# --------------------------------------------
# Verify Windows application image
# --------------------------------------------

if (-not (Test-Path $AppDir)) {

    throw "Application directory was not created: $AppDir"
}

$ExePath =
"$AppDir\$ProjectName.exe"

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
# Build complete
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

Write-Host "Dedicated server JAR:"
Write-Host "    $($ServerJarFile.FullName)"
Write-Host ""

# ============================================
# Optional dedicated-server deployment
# ============================================

if ($DeployServer) {

    Write-Host ""
    Write-Host "============================================"
    Write-Host "       Deploying Dedicated Server"
    Write-Host "============================================"
    Write-Host ""

    # ----------------------------------------
    # Load local deployment configuration
    # ----------------------------------------

    $DeployConfigPath =
    Join-Path `
        $PSScriptRoot `
        "deploy-config.ps1"

    if (-not (Test-Path $DeployConfigPath)) {

        throw @"
Deployment configuration was not found:

    $DeployConfigPath

Copy deploy-config.example.ps1 to deploy-config.ps1
and configure your Linux server settings.
"@
    }

    $DeployConfig =
    & $DeployConfigPath

    if ($null -eq $DeployConfig) {

        throw "Deployment configuration could not be loaded."
    }

    # ----------------------------------------
    # Validate deployment configuration
    # ----------------------------------------

    $RequiredSettings = @(
        "ServerUser",
        "ServerHost",
        "SshKey",
        "RemoteTempJar",
        "RemoteServerJar",
        "ServerService"
    )

    foreach ($Setting in $RequiredSettings) {

        if (-not $DeployConfig.ContainsKey($Setting)) {

            throw "Deployment setting '$Setting' is missing."
        }

        if ([string]::IsNullOrWhiteSpace(
                $DeployConfig[$Setting])) {

            throw "Deployment setting '$Setting' is empty."
        }
    }

    if (-not (Test-Path $DeployConfig.SshKey)) {

        throw "SSH private key was not found: $($DeployConfig.SshKey)"
    }

    if (-not (Test-Path $SshKey)) {

        throw "SSH private key was not found: $SshKey"
    }

    # ----------------------------------------
    # Check SSH tools
    # ----------------------------------------

    $SshCommand =
    Get-Command ssh `
        -ErrorAction SilentlyContinue

    if ($null -eq $SshCommand) {

        throw "ssh could not be found."
    }

    $ScpCommand =
    Get-Command scp `
        -ErrorAction SilentlyContinue

    if ($null -eq $ScpCommand) {

        throw "scp could not be found."
    }

    # ----------------------------------------
    # Check SSH key
    # ----------------------------------------

    if (-not (Test-Path $SshKey)) {

        throw "SSH key was not found: $SshKey"
    }

    # ----------------------------------------
    # Test SSH authentication
    # ----------------------------------------

    Write-Host "[SERVER 1/4] Testing SSH connection..."
    Write-Host ""

    ssh `
        -i $DeployConfig.SshKey `
        -o IdentitiesOnly=yes `
        -o BatchMode=yes `
        "$($DeployConfig.ServerUser)@$($DeployConfig.ServerHost)" `
        "echo SSH connection successful"

    if ($LASTEXITCODE -ne 0) {

        throw "SSH key authentication failed."
    }

    # ----------------------------------------
    # Upload new JAR
    # ----------------------------------------

    Write-Host ""
    Write-Host "[SERVER 2/4] Uploading server JAR..."
    Write-Host ""

    scp `
        -i $DeployConfig.SshKey `
        -o IdentitiesOnly=yes `
        -o BatchMode=yes `
        $ServerJarFile.FullName `
        "$($DeployConfig.ServerUser)@$($DeployConfig.ServerHost):$($DeployConfig.RemoteTempJar)"

    if ($LASTEXITCODE -ne 0) {

        throw "Dedicated server upload failed."
    }

    # ----------------------------------------
    # Install new JAR
    # ----------------------------------------

    Write-Host ""
    Write-Host "[SERVER 3/4] Installing new server..."
    Write-Host ""

    $InstallCommand =
    "mv '$($DeployConfig.RemoteTempJar)' " +
    "'$($DeployConfig.RemoteServerJar)' && " +
    "chmod 644 '$($DeployConfig.RemoteServerJar)'"

    ssh `
        -i $DeployConfig.SshKey `
        -o IdentitiesOnly=yes `
        -o BatchMode=yes `
        "$($DeployConfig.ServerUser)@$($DeployConfig.ServerHost)" `
        $InstallCommand

    if ($LASTEXITCODE -ne 0) {

        throw "Could not install the new dedicated server JAR."
    }

    # ----------------------------------------
    # Restart server
    # ----------------------------------------

    Write-Host ""
    Write-Host "[SERVER 4/4] Restarting dedicated server..."
    Write-Host ""

    ssh `
        -i $DeployConfig.SshKey `
        -o IdentitiesOnly=yes `
        -o BatchMode=yes `
        "$($DeployConfig.ServerUser)@$($DeployConfig.ServerHost)" `
        "sudo -n systemctl restart $($DeployConfig.ServerService)"

    if ($LASTEXITCODE -ne 0) {

        throw "Could not restart the dedicated server."
    }

    # ----------------------------------------
    # Verify server
    # ----------------------------------------

    Start-Sleep -Seconds 2

    $ServerStatus =
    ssh `
        -i $DeployConfig.SshKey `
        -o IdentitiesOnly=yes `
        -o BatchMode=yes `
        "$($DeployConfig.ServerUser)@$($DeployConfig.ServerHost)" `
        "sudo -n systemctl is-active $ServerService"

    if ($LASTEXITCODE -ne 0 `
            -or $ServerStatus.Trim() -ne "active") {

        Write-Host ""
        Write-Host "Dedicated server failed to start."
        Write-Host ""

        Write-Host "Recent server logs:"
        Write-Host ""

        ssh `
            -i $DeployConfig.SshKey `
            -o IdentitiesOnly=yes `
            -o BatchMode=yes `
            "$($DeployConfig.ServerUser)@$($DeployConfig.ServerHost)" `
            "journalctl -u $ServerService -n 30 --no-pager"

        throw "Dedicated server deployment failed."
    }

    Write-Host ""
    Write-Host "============================================"
    Write-Host "       SERVER DEPLOYMENT SUCCESSFUL"
    Write-Host "============================================"
    Write-Host ""

    Write-Host "Server:"
    Write-Host "    $ServerHost"
    Write-Host ""

    Write-Host "Service:"
    Write-Host "    $ServerService"
    Write-Host ""

    Write-Host "Status:"
    Write-Host "    $ServerStatus"
    Write-Host ""

    Write-Host "Recent server logs:"
    Write-Host ""

    ssh `
        -i $DeployConfig.SshKey `
        -o IdentitiesOnly=yes `
        -o BatchMode=yes `
        "$($DeployConfig.ServerUser)@$($DeployConfig.ServerHost)" `
        "journalctl -u $ServerService -n 10 --no-pager"

    Write-Host ""
}

Write-Host "============================================"
Write-Host ""