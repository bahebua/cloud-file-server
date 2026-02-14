package com.projects.sharefiles.controller;

import com.projects.sharefiles.model.FileMeta;
import com.projects.sharefiles.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
public class FileController {

    @Autowired
    private FileStorageService service;

    @PostMapping("upload")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "ttlMinutes", required = false) Long ttlMinutes)
            throws IOException {

        FileMeta meta = service.save(file, ttlMinutes);

        String url = "http://localhost:8080/f/" + meta.getFileKey();

        Map<String, String> response = new HashMap<>();
        response.put("key", meta.getFileKey());
        response.put("url", url);
        response.put("name", meta.getOriginalFileName());

        return response;
    }

    @GetMapping("f/{key}")
    public ResponseEntity<FileSystemResource> download(@PathVariable String key,
                                                       @RequestHeader HttpHeaders headers) throws IOException {

        FileMeta meta = service.getMeta(key);

        File file = service.getFile(key);

        if (meta == null || file == null || !file.exists()) {
            return ResponseEntity.notFound().build();
        }
        if (meta.getExpiresAt() != null &&
                meta.getExpiresAt().isBefore(Instant.now())) {

            service.deleteByKey(key);
            return ResponseEntity.notFound().build();
        }

//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION,
//                        "attachment; filename=\"" + meta.getOriginalFileName() + "\"")
//                .contentLength(meta.getSize())
//                .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                .body(new FileSystemResource(file));
        long fileLength = file.length();
        String range = headers.getFirst(HttpHeaders.RANGE);

        if (range == null) {
            // Normal full download
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + meta.getOriginalFileName() + "\"")
                    .contentLength(fileLength)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new FileSystemResource(file));
        }

        // Example: "bytes=500-"
        long start;
        long end = fileLength - 1;

        String[] parts = range.replace("bytes=", "").split("-");
        start = Long.parseLong(parts[0]);

        if (parts.length > 1 && !parts[1].isEmpty()) {
            end = Long.parseLong(parts[1]);
        }

        if (end >= fileLength) {
            end = fileLength - 1;
        }

        long contentLength = end - start + 1;

        InputStream inputStream = new FileInputStream(file);
        inputStream.skip(start);

        Resource resource = new InputStreamResource(inputStream);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.getOriginalFileName() + "\"")
                .header(HttpHeaders.CONTENT_RANGE,
                        "bytes " + start + "-" + end + "/" + fileLength)
                .contentLength(contentLength)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body((FileSystemResource) resource);
    }

    @DeleteMapping("files/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {

        boolean deleted = service.deleteByKey(key);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }



    @GetMapping("files")
    public Collection<FileMeta> listFiles() {
        return service.list();
    }


}

