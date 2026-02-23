package com.projects.sharefiles.util;

import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

import java.awt.*;
import java.io.File;

public class FileUtil {

    public static MediaType getMediaType(String fileName) {
        MediaType mediaType;
        if (fileName.endsWith(".pdf")) {
            mediaType = MediaType.APPLICATION_PDF;
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            mediaType = MediaType.IMAGE_JPEG;
        } else if (fileName.endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        } else if (fileName.endsWith(".gif")) {
            mediaType = MediaType.IMAGE_GIF;
        } else if (fileName.endsWith(".mp4")) {
            mediaType = MediaType.valueOf("video/mp4");
        } else if (fileName.endsWith(".webm")) {
            mediaType = MediaType.valueOf("video/webm");
        } else if (fileName.endsWith(".mp3")) {
            mediaType = MediaType.valueOf("audio/mpeg");
        } else {
            // Fallback for anything else
            mediaType = MediaTypeFactory.getMediaType(fileName).orElse(MediaType.APPLICATION_OCTET_STREAM);
        }
        return mediaType;
    }

}
