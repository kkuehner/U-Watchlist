# Starts both processes the app needs: the yfinance sidecar (Python/Flask, port 5000)
# and the Spring Boot app (port 8080). Run this instead of starting them separately.

$root = $PSScriptRoot
$sidecarDir = Join-Path $root "yfinance-sidecar"
$appDir = Join-Path $root "Investment"
$sidecarLog = Join-Path $sidecarDir "sidecar.log"

function Test-Sidecar {
    try {
        Invoke-WebRequest -Uri "http://localhost:5000/health" -UseBasicParsing -TimeoutSec 2 | Out-Null
        return $true
    } catch {
        return $false
    }
}

$startedSidecar = $false

if (Test-Sidecar) {
    Write-Host "yfinance sidecar already running on port 5000."
} else {
    Write-Host "Starting yfinance sidecar..."
    # Routed through cmd.exe so both stdout and stderr land in one log file (2>&1),
    # the same way Python's own `> file 2>&1` works.
    $sidecarProcess = Start-Process -FilePath "cmd.exe" `
        -ArgumentList "/c python app.py > `"$sidecarLog`" 2>&1" `
        -WorkingDirectory $sidecarDir -WindowStyle Hidden -PassThru
    $startedSidecar = $true

    $ready = $false
    for ($i = 0; $i -lt 15; $i++) {
        Start-Sleep -Seconds 1
        if (Test-Sidecar) { $ready = $true; break }
    }

    if ($ready) {
        Write-Host "Sidecar ready."
    } else {
        Write-Warning "Sidecar didn't respond after 15s - check $sidecarLog for errors."
    }
}

Write-Host "Starting Spring Boot app... (Ctrl+C here stops both)"
try {
    Push-Location $appDir
    & .\mvnw.cmd spring-boot:run
} finally {
    Pop-Location
    if ($startedSidecar -and $sidecarProcess) {
        Write-Host "Stopping yfinance sidecar..."
        Stop-Process -Id $sidecarProcess.Id -Force -ErrorAction SilentlyContinue
    }
}
