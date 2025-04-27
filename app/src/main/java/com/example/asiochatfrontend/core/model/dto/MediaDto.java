package com.example.asiochatfrontend.core.model.dto;

import com.example.asiochatfrontend.core.model.enums.MediaType;

import java.io.File;
import java.util.Date;

public class MediaDto {
    public String id;
    public File file;
    public String fileName;
    public String contentType;
    public MediaType type;
    public Long size;
    public String thumbnailPath;
    public Boolean isProcessed;

    public MediaDto(String id, String fileName, File file, String contentType, MediaType type, Long size, String thumbnailPath, Boolean isProcessed) {
        this.id = id;
        this.fileName = fileName;
        this.file = file;
        this.contentType = contentType;
        this.type = type;
        this.size = size;
        this.thumbnailPath = thumbnailPath;
        this.isProcessed = isProcessed;
    }

    public MediaDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public MediaType getType() {
        return type;
    }

    public void setType(MediaType type) {
        this.type = type;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Boolean getProcessed() {
        return isProcessed;
    }

    public void setProcessed(Boolean processed) {
        isProcessed = processed;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }
}
