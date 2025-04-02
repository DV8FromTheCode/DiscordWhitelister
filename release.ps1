# PowerShell script for releasing a new version of Discord Whitelister

param (
    [Parameter(Mandatory=$true)]
    [string]$Version,
    
    [Parameter(Mandatory=$false)]
    [string]$Message = "Release version $Version"
)

# Validate version format (should be semver)
if ($Version -notmatch '^\d+\.\d+\.\d+$') {
    Write-Error "Version must be in format X.Y.Z (e.g., 1.0.0)"
    exit 1
}

# Check if git is installed
try {
    git --version | Out-Null
} catch {
    Write-Error "Git is not installed or not in PATH. Please install Git."
    exit 1
}

# Check if working directory is clean
$status = git status --porcelain
if ($status) {
    Write-Error "Working directory is not clean. Please commit or stash your changes."
    exit 1
}

# Update version.txt
$versionFile = "version.txt"
$Version | Out-File -FilePath $versionFile -NoNewline

# Update CHANGELOG.md
$changelogFile = "CHANGELOG.md"
$changelog = Get-Content $changelogFile -Raw

# Replace [Unreleased] with the new version
$date = Get-Date -Format "yyyy-MM-dd"
$newChangelog = $changelog -replace "\[Unreleased\]", "[$Version] - $date"

# Add new Unreleased section
$unreleasedSection = @"
## [Unreleased]

### Added

### Changed

### Fixed

"@

$newChangelog = $newChangelog -replace "# Changelog", "# Changelog`n`n$unreleasedSection"

# Write updated changelog
$newChangelog | Out-File -FilePath $changelogFile -Encoding utf8

# Build the project to make sure everything works
Write-Host "Building project..."
./gradlew.bat clean build

if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed. Please fix the errors before releasing."
    git checkout -- $versionFile $changelogFile
    exit 1
}

# Commit changes
git add $versionFile $changelogFile
git commit -m "Prepare release $Version"

# Create tag
git tag -a "v$Version" -m "$Message"

# Push changes and tag
Write-Host "Do you want to push changes to remote repository? (y/n)"
$push = Read-Host

if ($push -eq "y") {
    git push origin master
    git push origin "v$Version"
    Write-Host "Changes pushed to remote repository."
    Write-Host "GitHub Actions will automatically build and create a release."
} else {
    Write-Host "Changes were not pushed to remote repository."
    Write-Host "To push manually, run:"
    Write-Host "git push origin master"
    Write-Host "git push origin v$Version"
}

Write-Host "Release $Version prepared successfully!"
