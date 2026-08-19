Write-Host ""
Write-Host "====================================="
Write-Host "        Git Push"
Write-Host "====================================="
Write-Host ""

# Make sure we're in a Git repository
git rev-parse --is-inside-work-tree 2>$null

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: This is not a Git repository."
    Read-Host "Press Enter to exit"
    exit 1
}

# Get current branch
$branch = git branch --show-current

if ([string]::IsNullOrWhiteSpace($branch)) {
    Write-Host "ERROR: Could not determine current branch."
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host "Current branch: $branch"
Write-Host ""

# Show commits that haven't been pushed
Write-Host "Commits waiting to be pushed:"
Write-Host ""

git log "origin/$branch..$branch" --oneline 2>$null

Write-Host ""

# Confirm
$confirm = Read-Host "Push these commits to GitHub? (y/n)"

if ($confirm -ne "y") {
    Write-Host ""
    Write-Host "Push cancelled."
    Read-Host "Press Enter to exit"
    exit 0
}

# Push
Write-Host ""
Write-Host "Pushing to GitHub..."
Write-Host ""

git push origin $branch

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "ERROR: Push failed."
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "Push successful!"
Write-Host ""

Read-Host "Press Enter to exit"