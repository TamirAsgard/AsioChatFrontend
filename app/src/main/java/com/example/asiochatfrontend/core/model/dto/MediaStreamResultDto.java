package com.example.asiochatfrontend.core.model.dto;

import java.io.InputStream;

public class MediaStreamResultDto {
    public InputStream stream;
    public String contentType;
    public String fileName;
    public String absolutePath;

    public MediaStreamResultDto(
            InputStream stream,
            String fileName,
            String contentType,
            String fileAbsolutePath) {
        this.stream = stream;
        this.fileName = fileName;
        this.contentType = contentType;
        this.absolutePath = fileAbsolutePath;
    }

    public MediaStreamResultDto() {
    }

    public InputStream getStream() {
        return stream;
    }

    public void setStream(InputStream stream) {
        this.stream = stream;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }
}

