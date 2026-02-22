======================================================
☁️ CLOUD FILE SERVER - CONTROL PANEL
======================================================

Welcome to the Cloud File Server! This application allows you to turn
any local folder on your computer into a fully functional, instantly
deployable private cloud storage drive.

✨ FEATURES:
- Cross-Platform UI: A simple control panel to manage your server.
- Dual Deployment: Run locally for network access, or deploy to a Private Cloud.
- Zero-Config Networking: Automatically generates secure trycloudflare.com URLs.
- Self-Cleaning: Safely removes background containers when you quit the app.
- Persistent Storage: Your files are completely safe, even if the server is deleted.

⚠️ PREREQUISITES (For Cloud Mode):
To use the "Deploy to Private Cloud" feature, your machine must have:
1. Docker Desktop installed and running.
2. Kubernetes enabled (Go to Docker Desktop Settings -> Kubernetes -> Enable).

======================================================
🚀 INSTALLATION INSTRUCTIONS
======================================================

🟦 WINDOWS:
1. Extract this ZIP file.
2. Double-click "CloudFileServer.exe".
3. (Optional) Right-click and "Pin to Taskbar" for easy access.

🍎 MACOS:
1. Double-click the .dmg file inside this ZIP to mount it.
2. Drag the "CloudFileServer" app icon into your Applications folder.
3. Open your Launchpad and click CloudFileServer.
   *Note: If macOS blocks the app because it is from an "unidentified developer",
    go to System Settings -> Privacy & Security -> click "Open Anyway".

🐧 LINUX:
1. Extract this ZIP file to find the .deb installer.
2. Open your terminal in that folder and run:
   sudo dpkg -i cloud-file-server*.deb
3. Launch the app from your application menu.

======================================================
💡 HOW TO USE:
1. Click "Browse..." and select the folder you want to turn into a cloud drive.
2. Click "Deploy to Private Cloud".
3. Wait ~30 seconds for the engine to build and establish the secure tunnel.
4. Click the generated "Public:" URL to access your files from anywhere in the world!