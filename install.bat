@echo off
setlocal
title Cloud File Server Installer 📦
color 1F

echo ===================================================
echo      CLOUD FILE SERVER INSTALLATION WIZARD
echo ===================================================
echo.

:: 1. Ask for Destination
set "DEFAULT_DIR=C:\CloudFileServer"
set /p "INSTALL_DIR=Destination Folder [%DEFAULT_DIR%]: " || set "INSTALL_DIR=%DEFAULT_DIR%"

if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"
echo.
echo [1/3] Copying files to %INSTALL_DIR%...
echo ------------------------------------------

:: Copy critical files
copy /Y "target\sharefiles-0.0.1-SNAPSHOT.jar" "%INSTALL_DIR%\app.jar" >nul
copy /Y "launcher.bat" "%INSTALL_DIR%\" >nul
copy /Y "Dockerfile" "%INSTALL_DIR%\" >nul

:: Copy k8s folder recursively
xcopy /E /I /Y "k8s" "%INSTALL_DIR%\k8s" >nul

if %ERRORLEVEL% EQU 0 (
    echo    [OK] Files copied successfully.
) else (
    echo    [ERROR] Could not copy files. Run as Administrator?
    pause
    exit /b
)

:: 2. Create Desktop Shortcut (Using PowerShell magic)
echo.
echo [2/3] Creating Desktop Shortcut...
echo ------------------------------------------
set "SHORTCUT_SCRIPT=%temp%\CreateShortcut.vbs"
echo Set oWS = WScript.CreateObject("WScript.Shell") > "%SHORTCUT_SCRIPT%"
echo sLinkFile = "%USERPROFILE%\Desktop\Cloud File Server.lnk" >> "%SHORTCUT_SCRIPT%"
echo Set oLink = oWS.CreateShortcut(sLinkFile) >> "%SHORTCUT_SCRIPT%"
echo oLink.TargetPath = "%INSTALL_DIR%\launcher.bat" >> "%SHORTCUT_SCRIPT%"
echo oLink.WorkingDirectory = "%INSTALL_DIR%" >> "%SHORTCUT_SCRIPT%"
echo oLink.Description = "Launch Cloud File Server" >> "%SHORTCUT_SCRIPT%"
echo oLink.IconLocation = "shell32.dll, 3" >> "%SHORTCUT_SCRIPT%"
echo oLink.Save >> "%SHORTCUT_SCRIPT%"

cscript /nologo "%SHORTCUT_SCRIPT%"
del "%SHORTCUT_SCRIPT%"
echo    [OK] Shortcut created on Desktop.

:: 3. Docker Check
echo.
echo [3/3] System Check...
echo ------------------------------------------
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [WARNING] Java 21 is NOT installed. The Local Mode will not work.
) else (
    echo [OK] Java detected.
)

docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [WARNING] Docker is NOT running. The Cloud Mode will not work.
) else (
    echo [OK] Docker detected.
)

echo.
echo ===================================================
echo      INSTALLATION COMPLETE! ✅
echo ===================================================
echo.
set /p "RUN_NOW=Launch application now? (Y/N): "
if /I "%RUN_NOW%"=="Y" (
    start "" "%INSTALL_DIR%\launcher.bat"
)
exit