Write-Host ""
Write-Host "====================================="
Write-Host "       Git Commit"
Write-Host "====================================="
Write-Host ""

# Make sure we're in a Git repository
git rev-parse --is-inside-work-tree 2>$null

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: This is not a Git repository."
    Read-Host "Press Enter to exit"
    exit 1
}

# Show current changes
Write-Host "Current changes:"
Write-Host ""

git status --short

Write-Host ""

# Stage all changes
Write-Host "Staging all changes..."
git add -A

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to stage changes."
    Read-Host "Press Enter to exit"
    exit 1
}

# Show staged changes
Write-Host ""
Write-Host "Changes staged for commit:"
Write-Host ""

git status --short

Write-Host ""

# Ask for commit message
$commitMessage = Read-Host "Enter commit message"

if ([string]::IsNullOrWhiteSpace($commitMessage)) {
    Write-Host ""
    Write-Host "ERROR: Commit message cannot be empty."
    Read-Host "Press Enter to exit"
    exit 1
}

# Commit
Write-Host ""
Write-Host "Creating commit..."

git commit -m "$commitMessage"

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: Commit failed."
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "Commit created successfully."
Write-Host ""

git log -1 --oneline

Write-Host ""
Read-Host "Press Enter to exit"