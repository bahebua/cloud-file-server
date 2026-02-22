package com.projects.sharefiles.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigSyncUtil {

    public static void injectPort(String targetDir, String port) {
        // Now tracking Dockerfiles alongside K8s files!
        String[] filesToUpdate = {
                "deployment.yaml",
                "service.yaml",
                "Dockerfile",
                "InstallerDockerfile"
        };

        for (String fileName : filesToUpdate) {
            try {
                Path filePath = Paths.get(targetDir, fileName);
                if (Files.exists(filePath)) {
                    String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
                    // Replace ALL instances of the placeholder
                    content = content.replaceAll("\\$\\{SERVER_PORT\\}", port);
                    content = content.replaceAll("containerPort: \\d+", "containerPort: " + port);
                    content = content.replaceAll("targetPort: \\d+", "targetPort: " + port);
                    content = content.replaceAll("http://localhost:\\d+", "http://localhost:" + port);
                    content = content.replaceAll("EXPOSE \\d+", "EXPOSE " + port);
                    Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                System.err.println("Failed to inject port into " + fileName + ": " + e.getMessage());
            }
        }
    }
}