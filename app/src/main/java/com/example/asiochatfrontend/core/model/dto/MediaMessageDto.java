package com.example.asiochatfrontend.core.model.dto;

import com.example.asiochatfrontend.core.model.enums.MessageState;

import java.util.Date;
import java.util.List;

public class MediaMessageDto extends MessageDto {

    private MediaDto mediaPayload;

    public MediaMessageDto() {
        super();
    }

    public MediaMessageDto(String id, List<String> waitingMembersList, MessageState status, Date timestamp, MediaDto mediaPayload, String jid, String chatId) {
        super(id, waitingMembersList, status, timestamp, null, jid, chatId);
        this.mediaPayload = mediaPayload;
    }

    public MediaDto getMediaPayload() {
        return mediaPayload;
    }

    public void setMediaPayload(MediaDto mediaPayload) {
        this.mediaPayload = mediaPayload;
    }
}