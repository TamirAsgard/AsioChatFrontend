package com.example.asiochatfrontend.core.model.dto;

import java.util.Objects;

public class MediaStreamResultDto {
    public String mediaId;
    public byte[] contentStream;
    public String mimeType;

    public MediaStreamResultDto(String mediaId, byte[] contentStream, String mimeType) {
        this.mediaId = mediaId;
        this.contentStream = contentStream;
        this.mimeType = mimeType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MediaStreamResultDto that = (MediaStreamResultDto) o;
        return mediaId.equals(that.mediaId) && mimeType.equals(that.mimeType) &&
                java.util.Arrays.equals(contentStream, that.contentStream);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mediaId, mimeType);
        result = 31 * result + java.util.Arrays.hashCode(contentStream);
        return result;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public byte[] getContentStream() {
        return contentStream;
    }

    public void setContentStream(byte[] contentStream) {
        this.contentStream = contentStream;
    }
}