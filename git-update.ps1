# ============================================
# Chess Like Game - Git Update Script
# ============================================

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "============================================"
Write-Host "       Chess Like Game - Git Update"
Write-Host "============================================"
Write-Host ""

# --------------------------------------------
# Check Git
# --------------------------------------------

Write-Host "[1/5] Checking Git..."
Write-Host ""

git --version

if ($LASTEXITCODE -ne 0) {
    throw "Git could not be found."
}

# --------------------------------------------
# Check repository
# --------------------------------------------

Write-Host ""
Write-Host "[2/5] Checking repository..."
Write-Host ""

if (-not (Test-Path ".git")) {
    throw "This directory is not a Git repository."
}

# --------------------------------------------
# Show current status
# --------------------------------------------

Write-Host ""
Write-Host "[3/5] Current changes:"
Write-Host ""

git status --short

Write-Host ""

# --------------------------------------------
# Ask for commit message
# --------------------------------------------

$CommitMessage = Read-Host "Enter commit message"

if ([string]::IsNullOrWhiteSpace($CommitMessage)) {
    throw "Commit message cannot be empty."
}

# --------------------------------------------
# Add changes
# --------------------------------------------

Write-Host ""
Write-Host "[4/5] Adding changes..."
Write-Host ""

git add .

if ($LASTEXITCODE -ne 0) {
    throw "Git add failed."
}

# --------------------------------------------
# Commit
# --------------------------------------------

Write-Host ""
Write-Host "Creating commit..."
Write-Host ""

git commit -m "$CommitMessage"

if ($LASTEXITCODE -ne 0) {
    throw "Git commit failed."
}

# --------------------------------------------
# Push
# --------------------------------------------

Write-Host ""
Write-Host "[5/5] Pushing to GitHub..."
Write-Host ""

git push

if ($LASTEXITCODE -ne 0) {
    throw "Git push failed."
}

# --------------------------------------------
# Finished
# --------------------------------------------

Write-Host ""
Write-Host "============================================"
Write-Host "          GIT UPDATE SUCCESSFUL"
Write-Host "============================================"
Write-Host ""

Write-Host "Your changes have been committed and pushed."
Write-Host ""

git status

Write-Host ""