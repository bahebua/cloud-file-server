package com.projects.sharefiles; // Ensure this matches your project structure

import org.springframework.boot.builder.SpringApplicationBuilder;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.regex.*;

public class CloudFileServerLauncher {

    private static JTextArea outputBox;
    private static boolean isDockerRunning = false;
    private static JFrame frame;

    // Cache OS check for cleaner ProcessBuilder calls
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");

    public static void main(String[] args) {

        if ("headless".equals(System.getenv("APP_MODE"))) {
            System.out.println("☁️ Cloud environment detected. Starting Headless Server...");
            // Start Spring Boot directly without the GUI
            new SpringApplicationBuilder(SharefilesApplication.class).run(args);
            return; // Stop execution here so the GUI code never runs
        }

        // Spring Boot usually disables GUI modes. We must explicitly enable it.
        System.setProperty("java.awt.headless", "false");
        SwingUtilities.invokeLater(() -> createAndShowGUI());
    }

    private static void createAndShowGUI() {
        frame = new JFrame("Cloud File Server - Control Panel ☁️");

        try {
            URL iconUrl = CloudFileServerLauncher.class.getResource("/static/icon.png");
            if (iconUrl != null) {
                Image image = Toolkit.getDefaultToolkit().getImage(iconUrl);
                frame.setIconImage(image);
            }
        } catch (Exception e) {
            // Ignore silently, it will just fall back to the default Java icon
        }

        frame.setSize(550, 450);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(10, 10));

        // --- Top Panel: Folder Selection ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Storage Folder:"));

        String defaultPath = IS_WINDOWS ? "D:\\k8s-data" : System.getProperty("user.home") + "/k8s-data";
        JTextField pathBox = new JTextField(defaultPath, 25);
        topPanel.add(pathBox);

        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                pathBox.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        topPanel.add(browseBtn);
        frame.add(topPanel, BorderLayout.NORTH);

        // --- Center Panel: Action Buttons & Logs ---
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JButton btnLocal = new JButton("🚀 Start Local Server");
        JButton btnDocker = new JButton("☁️ Deploy to Private Cloud");
        buttonPanel.add(btnLocal);
        buttonPanel.add(btnDocker);
        centerPanel.add(buttonPanel, BorderLayout.NORTH);

        outputBox = new JTextArea();
        outputBox.setEditable(false);
        outputBox.setFont(new Font("Monospaced", Font.PLAIN, 12));
        centerPanel.add(new JScrollPane(outputBox), BorderLayout.CENTER);

        frame.add(centerPanel, BorderLayout.CENTER);

        // --- Networking Variables ---
        String localIp = getLocalIP();
        String hostName = getHostName();

        // --- Action: Local Mode ---
        btnLocal.addActionListener(e -> {
            btnLocal.setEnabled(false);
            btnDocker.setEnabled(false);
            String selectedPath = pathBox.getText();

            log("Starting Local Server...");
            System.setProperty("STORAGE_PATH", selectedPath);

            new Thread(() -> {
                try {
                    new SpringApplicationBuilder(SharefilesApplication.class).headless(false).run();
                    log("\n✅ Server is running!");
                    log("--------------------------------");
                    log("Local:   http://localhost:9080/rest/cloudfileserver");
                    log("Network: http://" + localIp + ":9080/rest/cloudfileserver");
                    log("Device:  http://" + hostName + ".local/rest/cloudfileserver");
                } catch (Exception ex) {
                    log("Error starting local server: " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        btnLocal.setEnabled(true);
                        btnDocker.setEnabled(true);
                    });
                }
            }).start();
        });

        // --- Action: Docker/K8s Mode ---
        btnDocker.addActionListener(e -> {
            btnLocal.setEnabled(false);
            btnDocker.setEnabled(false);
            isDockerRunning = true;
            String winPath = pathBox.getText();

            new Thread(() -> {
                try {
                    String k8sPath = translatePathForDocker(winPath);
                    log("Translating path for Kubernetes...");
                    log("Mapped to: " + k8sPath);

                    // 1. Update YAML
                    String k8sDir = getK8sDir();
                    Path templatePath = Paths.get(k8sDir, "storage.yaml");
                    Path livePath = Paths.get(k8sDir, "live-storage.yaml");
                    String content = new String(Files.readAllBytes(templatePath));
                    content = content.replace("REPLACE_ME_STORAGE_PATH", k8sPath);
                    Files.write(livePath, content.getBytes());

                    // 2. Execute K8s Commands
                    log("\nBuilding and Deploying... (This may take a moment)");
                    String dockerDir = Files.exists(Paths.get("Dockerfile")) ? "." : "app";
                    String dockerFile = Files.exists(Paths.get("Dockerfile")) ? "Dockerfile" : "app/Dockerfile";
                    runCommand("docker build -t cloud-file-server:v1 "+ dockerDir);
                    runCommand("docker build -t cloud-file-server:v1 -f " + dockerFile + " " + dockerDir);

                    runCommand("kubectl apply -f " + Paths.get(k8sDir, "live-storage.yaml").toString());
                    runCommand("kubectl apply -f " + Paths.get(k8sDir, "deployment.yaml").toString());
                    runCommand("kubectl apply -f " + Paths.get(k8sDir, "service.yaml").toString());

                    // 3. Wait for Cloudflare
                    log("\nWaiting for Cloudflare Tunnel...");
                    Thread.sleep(10000);
                    String tunnelUrl = getCloudflareUrl();

                    log("\n✅ DEPLOYMENT SUCCESSFUL!");
                    log("--------------------------------");
                    log("Local:   http://localhost/rest/cloudfileserver");
                    log("Network: http://" + localIp + "/rest/cloudfileserver");
                    log("Device:  http://" + hostName + ".local/rest/cloudfileserver");
                    log("Public:  " + tunnelUrl + "/rest/cloudfileserver");

                } catch (Exception ex) {
                    log("Error: " + ex.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        btnLocal.setEnabled(true);
                        btnDocker.setEnabled(true);
                    });
                }
            }).start();
        });

        setupSystemTray();
        frame.setVisible(true);
    }

    // --- SYSTEM TRAY LOGIC ---
    private static void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            return;
        }

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                frame.setVisible(false);
            }
        });

        try {
            SystemTray tray = SystemTray.getSystemTray();
            URL iconUrl = CloudFileServerLauncher.class.getResource("/static/icon.png");
            Image image = (iconUrl != null) ? Toolkit.getDefaultToolkit().getImage(iconUrl) : new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);

            PopupMenu popup = new PopupMenu();
            MenuItem openItem = new MenuItem("Open Control Panel");
            openItem.addActionListener(e -> frame.setVisible(true));
            MenuItem exitItem = new MenuItem("Quit & Stop Server");
            exitItem.addActionListener(e -> quitApplication());

            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);

            TrayIcon trayIcon = new TrayIcon(image, "Cloud File Server", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> frame.setVisible(true));

            tray.add(trayIcon);
        } catch (AWTException e) {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
    }

    private static void quitApplication() {
        log("Shutting down and cleaning up infrastructure...");

        if (isDockerRunning) {
            new Thread(() -> {
                try {
                    // 🎯 THE FIX: Dynamically resolve the K8s folder location
                    String k8sDir = getK8sDir();

                    log(" > Removing Kubernetes Deployments & Services...");
                    runCommand("kubectl delete -f " + Paths.get(k8sDir, "deployment.yaml").toString() + " --ignore-not-found=true");
                    runCommand("kubectl delete -f " + Paths.get(k8sDir, "service.yaml").toString() + " --ignore-not-found=true");

                    log(" > Removing Kubernetes Volume Configs (Your data on disk is safe!)...");
                    runCommand("kubectl delete -f " + Paths.get(k8sDir, "live-storage.yaml").toString() + " --ignore-not-found=true");

                    log(" > Removing Docker Image for a fresh start next time...");
                    runCommand("docker rmi cloud-file-server:v1 --force");

                } catch (Exception e) {
                    log("Cleanup error: " + e.getMessage());
                } finally {
                    System.exit(0); // Cleanly kill the app after cleanup finishes
                }
            }).start();
        } else {
            System.exit(0); // If Local mode, just kill it immediately
        }
    }

    // --- Helper Methods ---
    private static String translatePathForDocker(String path) {
        if (IS_WINDOWS) {
            String drive = path.substring(0, 1).toLowerCase();
            String rest = path.substring(2).replace("\\", "/");
            return "/mnt/host/" + drive + rest;
        }
        return path;
    }

    private static void runCommand(String command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(IS_WINDOWS ? "cmd.exe" : "bash", IS_WINDOWS ? "/c" : "-c", command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        while ((line = reader.readLine()) != null) {
//            if (!line.trim().isEmpty()) {
            if (line.contains("Successfully") || line.contains("created") || line.contains("configured") || line.contains("deleted")) {
                log(" > " + line);
            }
        }
        process.waitFor();
    }

    private static String getCloudflareUrl() throws Exception {
        Pattern pattern = Pattern.compile("https://[a-zA-Z0-9.-]+\\.trycloudflare\\.com");
        for (int i = 0; i < 15; i++) {
        ProcessBuilder builder = new ProcessBuilder(IS_WINDOWS ? "cmd.exe" : "bash", IS_WINDOWS ? "/c" : "-c", "kubectl logs -l app=cloud-file-server -c cloudflared --tail 50");
        Process process = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;

        while ((line = reader.readLine()) != null) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) return matcher.group(0);
        }
            Thread.sleep(3000); // Wait 3 seconds and check the logs again
        }
        return "URL not found yet. Check 'kubectl logs' manually.";
    }

    private static String getLocalIP() {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) { return "127.0.0.1"; }
    }

    private static String getHostName() {
        try { return InetAddress.getLocalHost().getHostName(); }
        catch (Exception e) { return "localhost"; }
    }

    private static void log(String message) {
        SwingUtilities.invokeLater(() -> {
            outputBox.append(message + "\n");
            outputBox.setCaretPosition(outputBox.getDocument().getLength());
        });
    }

    private static String getK8sDir() {
        // If running in IDE, it's "k8s". If running via .exe, it's "app/k8s"
        return Files.exists(Paths.get("k8s")) ? "k8s" : "app/k8s";
    }
}