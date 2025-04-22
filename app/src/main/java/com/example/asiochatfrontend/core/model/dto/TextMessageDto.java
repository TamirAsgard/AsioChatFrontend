package com.example.asiochatfrontend.core.model.dto;

import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;

import java.util.Date;
import java.util.List;

public class TextMessageDto extends MessageDto {
    public String payload;

    public TextMessageDto(String id, List<String> waitingMemebersList, MessageState status, Date timestamp, String jid, String chatId, String payload) {
        super(id, waitingMemebersList, status, timestamp, jid, chatId);
        this.payload = payload;
    }

    public TextMessageDto() {
        super();
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}