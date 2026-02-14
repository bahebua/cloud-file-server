package com.projects.sharefiles.service;

import com.projects.sharefiles.model.FileMeta;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class FileStorageService {

    private static final String SEPARATOR = "__";

    @Value("${file.storage.path}")
    private String storagePath;

    private final Map<String, FileMeta> fileIndex = new ConcurrentHashMap<>();

    public FileMeta save(MultipartFile file, Long ttlMinutes) throws IOException {

        String key = UUID.randomUUID().toString();
        String originalName = file.getOriginalFilename();

        String storedName = key + SEPARATOR + originalName;
        Path target = Paths.get(storagePath, storedName);

        Files.copy(file.getInputStream(), target);

        Instant now = Instant.now();

        FileMeta meta = new FileMeta();
        meta.setFileKey(key);
        meta.setOriginalFileName(originalName);
        meta.setSize(file.getSize());
        meta.setUploadedAt(now);
        meta.setStoredFileName(storedName);

        if (ttlMinutes != null && ttlMinutes > 0) {
            meta.setExpiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES));
        }

        fileIndex.put(key, meta);
        return meta;
    }


    public File getFile(String key) {
        FileMeta meta = fileIndex.get(key);
        if (meta == null) return null;
        return Paths.get(storagePath, meta.getStoredFileName()).toFile();
    }


    public FileMeta getMeta(String key) {
        return fileIndex.get(key);
    }

    public Collection<FileMeta> list() {
        return fileIndex.values().stream().toList();
    }

    public boolean deleteByKey(String key) {

        FileMeta meta = fileIndex.get(key);
        if (meta == null) {
            return false;
        }

        Path filePath = Paths.get(storagePath, meta.getStoredFileName());

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }

        fileIndex.remove(key);
        return true;
    }


    @PostConstruct
    public void rebuildIndex() {

        File dir = new File(storagePath);
        if (!dir.exists()) {
            dir.mkdirs();
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            String name = f.getName();

            if (!name.contains(SEPARATOR)) continue;

            String[] parts = name.split(SEPARATOR, 2);
            String key = parts[0];
            String originalName = parts[1];

            FileMeta meta = new FileMeta();
            meta.setFileKey(key);
            meta.setOriginalFileName(originalName);
            meta.setSize(f.length());
            meta.setStoredFileName(name);
            meta.setUploadedAt(
                    Instant.ofEpochMilli(f.lastModified())
            );
            meta.setExpiresAt(null);

            fileIndex.put(key, meta);
        }
    }

    @Scheduled(fixedDelay = 60_000) // every 1 minute
    public void cleanupExpiredFiles() {

        Instant now = Instant.now();

        for (FileMeta meta : new ArrayList<>(fileIndex.values())) {

            Instant expiry = meta.getExpiresAt();
            if (expiry == null) continue;

            if (expiry.isBefore(now)) {
                deleteByKey(meta.getFileKey());
            }
        }
    }

    public Map<String, Object> storageStats() {

        File dir = new File(storagePath);
        File[] files = dir.listFiles();

        long count = files == null ? 0 : files.length;
        long size = 0;

        if (files != null) {
            for (File f : files) {
                size += f.length();
            }
        }

        return Map.of(
                "fileCount", count,
                "usedMB", size / 1024 / 1024
        );
    }



}

