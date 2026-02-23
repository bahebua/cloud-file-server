package com.projects.sharefiles.controller;

import com.projects.sharefiles.model.FileMeta;
import com.projects.sharefiles.service.FileStorageService;
import com.projects.sharefiles.util.FileUtil;
import com.projects.sharefiles.util.PropertiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class FileController {

    @Autowired
    private FileStorageService service;

    String port = PropertiesUtil.getValue("server.port", "9080");

    @PostMapping("upload")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "ttlMinutes", required = false, defaultValue = "0") Long ttlMinutes)
            throws IOException {

        FileMeta meta = service.save(file, ttlMinutes);

        String url = "http://localhost:"+port+"/f/" + meta.getFileKey();

        Map<String, String> response = new HashMap<>();
        response.put("key", meta.getFileKey());
        response.put("url", url);
        response.put("name", meta.getOriginalFileName());

        return response;
    }

    // 🎯 THE DOWNLOADER: Forces OS-level save, fixes iPad bug
    @GetMapping("f/{key}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String key) {
        FileMeta meta = service.getMeta(key);
        File file = service.getFile(key);

        if (meta == null || file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }
        if (meta.getExpiresAt() != null && meta.getExpiresAt().isBefore(Instant.now())) {
            service.deleteByKey(key);
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getOriginalFileName() + "\"")
                // 🛡️ THE FIX: Force raw binary data so Safari/iPad doesn't try to preview it!
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(resource);
    }

    // 🎯 THE VIEWER: Fixes the Windows Chrome Hang
    @GetMapping("view/{key}")
    public ResponseEntity<Resource> viewInBrowser(@PathVariable String key) {
        FileMeta meta = service.getMeta(key);
        File file = service.getFile(key);

        if (meta == null || file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }
        if (meta.getExpiresAt() != null && meta.getExpiresAt().isBefore(Instant.now())) {
            service.deleteByKey(key);
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String fileName = meta.getOriginalFileName().toLowerCase();
        MediaType mediaType = FileUtil.getMediaType(fileName);

        // 🍿 THE MAGIC: Let Spring Boot handle the HTTP 206 Chunking natively!
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + meta.getOriginalFileName() + "\"")
                .contentType(mediaType)
                // 🚫 REMOVED contentLength() and acceptRanges() headers!
                // By leaving them out, Spring's ResourceHttpMessageConverter will safely
                // intercept chunk requests and generate the headers for you automatically.
                .body(resource);
    }
    
    @DeleteMapping("files/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {

        boolean deleted = service.deleteByKey(key);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    // 🎯 NEW ENDPOINT: Streams multiple files directly into a ZIP archive
    @GetMapping("download/zip")
    public ResponseEntity<StreamingResponseBody> downloadZip(@RequestParam("keys") List<String> keys) {

        // 1. Ask the service to build the stream
        StreamingResponseBody stream = service.createZipStream(keys);

        // 2. Return the clean HTTP response
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Cloud_Files.zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream);
    }

    // 🎯 SURGICAL INJECTION: Bulk Delete API
    @DeleteMapping("bulk")
    public ResponseEntity<?> deleteBulk(@RequestParam("keys") List<String> keys) {
        for (String key : keys) {
            service.deleteByKey(key);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("thumb/{key}")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable String key) {
        try {
            // Ask the service for the processed image bytes
            byte[] thumbnailBytes = service.generateThumbnail(key);

            // If the service returned null (file missing or not an image), send a 404
            if (thumbnailBytes == null) {
                return ResponseEntity.notFound().build();
            }

            // Return the bytes as a JPEG
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(thumbnailBytes);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @GetMapping("files")
    public Collection<FileMeta> listFiles() {
        return service.list();
    }


}

