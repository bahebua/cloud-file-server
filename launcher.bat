@echo off
setlocal enabledelayedexpansion
title Cloud File Server ☁️
cd /d "%~dp0"

:MENU
cls
echo ==========================================
echo   CLOUD FILE SERVER - LAUNCHER
echo ==========================================
echo.
echo  [1] Run Locally (Simple Mode)
echo      - Requires Java 21
echo      - Files stored in: .\data
echo.
echo  [2] Run in Private Cloud (Docker Mode)
echo      - Requires Docker Desktop
echo      - Files stored in: D:\k8s-data
echo      - Public URL via Cloudflare
echo.
set /p mode="Select Mode (1 or 2): "

if "%mode%"=="1" goto LOCAL
if "%mode%"=="2" goto DOCKER
goto MENU

:LOCAL
echo [INFO] Starting Local Server...
:: Get the current folder path for data
set "LOCAL_DATA_PATH=%cd%\data"
if not exist "data" mkdir data

echo [INFO] Storage Path: %LOCAL_DATA_PATH%
:: Pass the environment variable to Java
set STORAGE_PATH=%LOCAL_DATA_PATH%
java -jar app.jar
pause
exit

:DOCKER
echo [INFO] Checking Docker...
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker is not running. Please start Docker Desktop.
    pause
    goto MENU
)

echo [INFO] --- Private Cloud Configuration ---
set "DEF_K8S_PATH=/mnt/host/d/k8s-data/cloud-file-server-storage"
echo Default Windows Path: D:\k8s-data\cloud-file-server-storage
echo (Note: Use Docker-style paths like /mnt/host/c/... or /mnt/host/d/...)
set /p "USER_K8S_PATH=Enter Storage Path [%DEF_K8S_PATH%]: " || set "USER_K8S_PATH=%DEF_K8S_PATH%"

echo [INFO] Updating Kubernetes Manifests...
:: We use PowerShell to create a temporary 'live' version of your storage config
powershell -Command "(Get-Content k8s/storage.yaml) -replace '/mnt/host/d/k8s-data/cloud-file-server-storage', '%USER_K8S_PATH%' | Set-Content k8s/live-storage.yaml"

echo [INFO] Deploying to Kubernetes...
:: Build and Deploy commands
docker build -t cloud-file-server:v1 . >nul 2>&1
kubectl delete pod -l app=cloud-file-server --ignore-not-found=true >nul 2>&1
kubectl apply -f k8s/live-storage.yaml >nul 2>&1
kubectl apply -f k8s/deployment.yaml >nul 2>&1
kubectl apply -f k8s/service.yaml >nul 2>&1

:: Clean up the temporary file
del k8s\live-storage.yaml

echo.
echo [INFO] Waiting for the Cloudflare Tunnel URL...
echo        (This usually takes 10-20 seconds)
echo -----------------------------------------------------

:: Loop to check the logs for the URL
:WAIT_LOOP
timeout /t 3 /nobreak >nul
set "TUNNEL_URL="

:: Extract the URL from the pod logs using PowerShell regex
for /f "tokens=*" %%i in ('powershell -Command "kubectl logs -l app=cloud-file-server -c cloudflared --tail 50 | Select-String -Pattern 'https://[a-zA-Z0-9.-]+\.trycloudflare\.com' | ForEach-Object { $_.Matches.Value } | Select-Object -First 1"') do (
    set "TUNNEL_URL=%%i"
)

if "%TUNNEL_URL%"=="" (
    echo    ... Tunnel still initializing ...
    goto WAIT_LOOP
)

:: Final Output Screen
cls
echo =====================================================
echo    🚀 SERVER IS LIVE (PRIVATE CLOUD MODE)
echo =====================================================
echo.
echo  [LOCAL ACCESS]  http://localhost/rest/cloudfileserver
echo  [PUBLIC ACCESS] %TUNNEL_URL%/rest/cloudfileserver
echo.
echo  [STORAGE PATH]  %USER_K8S_PATH%
echo =====================================================
echo Press any key to stop monitoring and return to menu...
pause >nul
goto MENU
exit