@echo off
setlocal enabledelayedexpansion
title Cloud File Server Launcher 🚀
color 0A

:MENU
cls
echo =====================================================
echo    CLOUD FILE SERVER - CONTROL PANEL
echo =====================================================
echo.
echo  [1] Run Locally (Spring Boot only)
echo      - Good for: Quick testing, no Docker needed.
echo      - Requires: Java 21 installed.
echo.
echo  [2] Deploy to Private Cloud (Kubernetes + Tunnel)
echo      - Good for: Full production setup, external access.
echo      - Requires: Docker Desktop running.
echo.
echo  [3] Stop Everything (Cleanup)
echo.
set /p option="Select an option (1-3): "

if "%option%"=="1" goto LOCAL_MODE
if "%option%"=="2" goto K8S_MODE
if "%option%"=="3" goto CLEANUP
goto MENU

:: =====================================================
:: MODE 1: LOCAL SPRING BOOT
:: =====================================================
:LOCAL_MODE
cls
echo [INFO] Building application using Maven Wrapper...
echo -----------------------------------------------------
:: Use 'call' to prevent the script from exiting after mvnw finishes
call mvnw.cmd clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed! Check the logs above.
    pause
    goto MENU
)

echo.
echo [INFO] Build Successful! Starting Server...
echo [INFO] Access Local: http://localhost:8080/rest/cloudfileserver
echo -----------------------------------------------------
java -jar target/*.jar
pause
goto MENU

:: =====================================================
:: MODE 2: KUBERNETES DEPLOYMENT
:: =====================================================
:K8S_MODE
cls
echo [INFO] Checking Docker status...
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker is NOT running! Please start Docker Desktop and try again.
    pause
    goto MENU
)

echo [INFO] Building Docker Image (Multi-Stage)...
echo -----------------------------------------------------
docker build -t cloud-file-server:v2 .

echo.
echo [INFO] Deploying to Kubernetes...
echo -----------------------------------------------------
:: Force delete old pods to ensure we get a fresh tunnel
kubectl delete pod -l app=cloud-file-server --ignore-not-found=true

:: Apply all configs
kubectl apply -f k8s/storage.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml

echo.
echo [INFO] Waiting for the Cloudflare Tunnel...
echo        (This may take 15-30 seconds)
echo -----------------------------------------------------

:WAIT_LOOP
timeout /t 5 /nobreak >nul
for /f "tokens=*" %%i in ('kubectl logs -l app^=cloud-file-server -c cloudflared --tail^=20 2^>nul ^| findstr "trycloudflare.com"') do (
    set TUNNEL_URL=%%i
)

if not defined TUNNEL_URL (
    echo    ... Still waiting for tunnel URL ...
    goto WAIT_LOOP
)

cls
echo =====================================================
echo    🚀 DEPLOYMENT SUCCESSFUL!
echo =====================================================
echo.
echo [LOCAL ACCESS]  http://localhost:8080/rest/cloudfileserver
echo [PUBLIC URL]    %TUNNEL_URL%
echo.
echo [NOTE] Your persistent files are in: D:\k8s-data
echo.
pause
goto MENU

:: =====================================================
:: MODE 3: CLEANUP
:: =====================================================
:CLEANUP
echo [INFO] Stopping Kubernetes resources...
kubectl delete -f k8s/service.yaml --ignore-not-found=true
kubectl delete -f k8s/deployment.yaml --ignore-not-found=true
echo [INFO] Cleanup complete.
pause
exit