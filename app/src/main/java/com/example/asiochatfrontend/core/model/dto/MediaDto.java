package com.example.asiochatfrontend.core.model.dto;

import com.example.asiochatfrontend.core.model.enums.MediaType;

import java.io.File;
import java.util.Date;

public class MediaDto {
    public String id;
    public File file;
    public String fileName;
    public String ContentType;
    public MediaType type;
    public Long size;
    public String ThumbnailPath;
    public Boolean IsProcessed;

    public MediaDto(String id, String fileName, File file, String contentType, MediaType type, Long size, String thumbnailPath, Boolean isProcessed) {
        this.id = id;
        this.fileName = fileName;
        this.file = file;
        ContentType = contentType;
        this.type = type;
        this.size = size;
        ThumbnailPath = thumbnailPath;
        IsProcessed = isProcessed;
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

    public String getContentType() {
        return ContentType;
    }

    public void setContentType(String contentType) {
        ContentType = contentType;
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

    public String getThumbnailPath() {
        return ThumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        ThumbnailPath = thumbnailPath;
    }

    public Boolean getProcessed() {
        return IsProcessed;
    }

    public void setProcessed(Boolean processed) {
        IsProcessed = processed;
    }
}
