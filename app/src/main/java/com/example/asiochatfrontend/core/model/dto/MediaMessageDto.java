package com.example.asiochatfrontend.core.model.dto;

public class MediaMessageDto {
    public MessageDto message;
    public MediaDto media;

    public MediaMessageDto(MessageDto message, MediaDto media) {
        this.message = message;
        this.media = media;
    }

    public MessageDto getMessage() {
        return message;
    }

    public void setMessage(MessageDto message) {
        this.message = message;
    }

    public MediaDto getMedia() {
        return media;
    }

    public void setMedia(MediaDto media) {
        this.media = media;
    }
}
