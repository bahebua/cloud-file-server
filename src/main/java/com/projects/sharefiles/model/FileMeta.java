package com.projects.sharefiles.model;

import java.time.Instant;

public class FileMeta {
    private String fileKey;
    private String originalFileName;
    private long size;
    private String contentType;
    private Instant uploadedAt;
    private String storedFileName;

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    private Instant expiresAt;

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getStoredFileName() {
        return storedFileName;
    }

    public void setStoredFileName(String storedFileName) {
        this.storedFileName = storedFileName;
    }

    public FileMeta(String fileKey, String originalFileName, long size, String contentType) {
        this.fileKey = fileKey;
        this.originalFileName = originalFileName;
        this.size = size;
        this.contentType = contentType;
    }

    public FileMeta() {

    }

    public String getFileKey() {
        return fileKey;
    }

    public void setFileKey(String fileKey) {
        this.fileKey = fileKey;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String toString() {
        return "FileMeta{" +
                "fileKey='" + fileKey + '\'' +
                ", originalFileName='" + originalFileName + '\'' +
                ", size=" + size +
                ", contentType='" + contentType + '\'' +
                ", uploadedAt=" + uploadedAt +
                ", storedFileName='" + storedFileName + '\'' +
                '}';
    }
}
