Write-Host "Running DailyRead pre-commit verification..." -ForegroundColor Cyan
Set-Location "$PSScriptRoot\.."

Write-Host "1. Verifying :core-domain unit tests..."
.\gradlew.bat :core-domain:test --quiet
if ($LASTEXITCODE -ne 0) {
    Write-Error "Pre-commit failure: :core-domain tests failed."
    exit 1
}

Write-Host "2. Verifying :app unit tests & security checks..."
.\gradlew.bat :app:testDebugUnitTest --quiet
if ($LASTEXITCODE -ne 0) {
    Write-Error "Pre-commit failure: :app unit tests failed."
    exit 1
}

Write-Host "All pre-commit verification checks passed cleanly!" -ForegroundColor Green
exit 0
