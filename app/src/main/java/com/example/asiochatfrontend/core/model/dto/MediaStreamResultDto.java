package com.example.asiochatfrontend.core.model.dto;

import java.io.InputStream;
import java.util.Objects;
import java.util.stream.Stream;

public class MediaStreamResultDto {
    public InputStream stream;
    public String contentType;
    public String fileName;

    public MediaStreamResultDto(InputStream stream, String fileName, String contentType) {
        this.stream = stream;
        this.fileName = fileName;
        this.contentType = contentType;
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
}

