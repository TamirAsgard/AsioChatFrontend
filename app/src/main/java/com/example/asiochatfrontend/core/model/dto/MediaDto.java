package com.example.asiochatfrontend.core.model.dto;

import com.example.asiochatfrontend.core.model.enums.MediaType;

import java.util.Date;

public class MediaDto {
    public String id;
    public MediaType type;
    public String localUri;
    public String fileName;
    public long fileSize;
    public String mimeType;
    public Long duration;
    public String thumbnailUri;
    public Date createdAt;
    public Date uploadedAt;

    public MediaDto(String id, MediaType type, String localUri, String fileName, long fileSize, String mimeType, Long duration, String thumbnailUri, Date createdAt, Date uploadedAt) {
        this.id = id;
        this.type = type;
        this.localUri = localUri;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.duration = duration;
        this.thumbnailUri = thumbnailUri;
        this.createdAt = createdAt;
        this.uploadedAt = uploadedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Date uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getThumbnailUri() {
        return thumbnailUri;
    }

    public void setThumbnailUri(String thumbnailUri) {
        this.thumbnailUri = thumbnailUri;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
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

    public String getLocalUri() {
        return localUri;
    }

    public void setLocalUri(String localUri) {
        this.localUri = localUri;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}
