package com.example.asiochatfrontend.core.model.dto;

import com.example.asiochatfrontend.core.model.enums.MessageState;

import java.util.Date;
import java.util.List;

public class MessageDto {
    public String id;
    public String chatId;
    public String senderId;
    public String content;
    public String mediaId;
    public String replyToMessageId;
    public MessageState state;
    public List<String> waitingMembersList;
    public Date createdAt;
    public Date deliveredAt;
    public Date readAt;

    public MessageDto(String id, String chatId, String senderId, String content, String mediaId, String replyToMessageId, MessageState state, List<String> waitingMembersList, Date createdAt, Date deliveredAt, Date readAt) {
        this.id = id;
        this.chatId = chatId;
        this.senderId = senderId;
        this.content = content;
        this.mediaId = mediaId;
        this.replyToMessageId = replyToMessageId;
        this.state = state;
        this.waitingMembersList = waitingMembersList;
        this.createdAt = createdAt;
        this.deliveredAt = deliveredAt;
        this.readAt = readAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getReadAt() {
        return readAt;
    }

    public void setReadAt(Date readAt) {
        this.readAt = readAt;
    }

    public Date getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Date deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public List<String> getWaitingMembersList() {
        return waitingMembersList;
    }

    public void setWaitingMembersList(List<String> waitingMembersList) {
        this.waitingMembersList = waitingMembersList;
    }

    public String getMediaId() {
        return mediaId;
    }

    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getReplyToMessageId() {
        return replyToMessageId;
    }

    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }

    public MessageState getState() {
        return state;
    }

    public void setState(MessageState state) {
        this.state = state;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}