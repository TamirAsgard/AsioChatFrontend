package com.example.asiochatfrontend.core.model.dto;

import com.example.asiochatfrontend.core.model.enums.ChatType;

import java.util.Date;
import java.util.List;

public class ChatDto {
    public String chatId;
    public String chatName;
    public List<String> recipients;
    public Boolean isGroup;

    public ChatDto(String chatId, Boolean isGroup, List<String> recipients, String chatName) {
        this.chatId = chatId;
        this.isGroup = isGroup;
        this.recipients = recipients;
        this.chatName = chatName;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public Boolean getGroup() {
        return isGroup;
    }

    public void setGroup(Boolean group) {
        isGroup = group;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public void setRecipients(List<String> recipients) {
        this.recipients = recipients;
    }

    public String getChatName() {
        return chatName;
    }

    public void setChatName(String chatName) {
        this.chatName = chatName;
    }
}