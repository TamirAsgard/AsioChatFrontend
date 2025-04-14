package com.example.asiochatfrontend.core.model.dto;

import com.example.asiochatfrontend.core.model.enums.MessageState;

import java.util.Date;
import java.util.List;

public class MessageDto {
    public String id;
    public String chatId;
    public String jid;
    public String payload;
    public Date timestamp;
    public MessageState Status;
    public List<String> WaitingMemebersList;

    public MessageDto(String id, List<String> waitingMemebersList, MessageState status, Date timestamp, String payload, String jid, String chatId) {
        this.id = id;
        WaitingMemebersList = waitingMemebersList;
        Status = status;
        this.timestamp = timestamp;
        this.payload = payload;
        this.jid = jid;
        this.chatId = chatId;
    }

    public MessageDto() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getWaitingMemebersList() {
        return WaitingMemebersList;
    }

    public void setWaitingMemebersList(List<String> waitingMemebersList) {
        WaitingMemebersList = waitingMemebersList;
    }

    public MessageState getStatus() {
        return Status;
    }

    public void setStatus(MessageState status) {
        Status = status;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}