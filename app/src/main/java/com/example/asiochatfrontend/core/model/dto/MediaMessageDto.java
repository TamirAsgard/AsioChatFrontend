package com.example.asiochatfrontend.core.model.dto;

import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;

import java.util.Date;
import java.util.List;

public class MediaMessageDto extends MessageDto {

    private MediaDto payload;

    public MediaMessageDto(String id, List<String> waitingMembersList, MessageState status, Date timestamp, String jid, String chatId, MediaDto payload) {
        super(id, waitingMembersList, status, timestamp, jid, chatId);
        this.payload = payload;
    }

    public MediaMessageDto(String id, List<String> waitingMembersList, MessageState status, Date timestamp, String jid, String chatId, MediaDto payload, String replyTo) {
        super(id, waitingMembersList, status, timestamp, jid, chatId, replyTo);
        this.payload = payload;
    }

    public MediaMessageDto() {
        super();
    }

    public MediaDto getPayload() {
        return payload;
    }

    public void setPayload(MediaDto payload) {
        this.payload = payload;
    }
}