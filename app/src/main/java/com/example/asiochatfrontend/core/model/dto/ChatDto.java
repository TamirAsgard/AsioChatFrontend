package com.example.asiochatfrontend.core.model.dto;

import com.example.asiochatfrontend.core.model.enums.ChatType;

import java.util.Date;
import java.util.List;

public class ChatDto {
    public String id;
    public String name;
    public ChatType type;
    public List<String> participants;
    public MessageDto lastMessage;
    public int unreadCount;
    public Date createdAt;
    public Date updatedAt;

    public ChatDto(String id, String name, ChatType type, List<String> participants, MessageDto lastMessage, int unreadCount, Date createdAt, Date updatedAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.participants = participants;
        this.lastMessage = lastMessage;
        this.unreadCount = unreadCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public MessageDto getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(MessageDto lastMessage) {
        this.lastMessage = lastMessage;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public ChatType getType() {
        return type;
    }

    public void setType(ChatType type) {
        this.type = type;
    }
}