package com.example.asiochatfrontend.core.model.dto;

public class MessageReadByDto {
    private String messageId;
    private String readBy;

    public MessageReadByDto(String messageId, String readBy) {
        this.messageId = messageId;
        this.readBy = readBy;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getReadBy() {
        return readBy;
    }

    public void setReadBy(String readBy) {
        this.readBy = readBy;
    }

    @Override
    public String toString() {
        return "MessageReadByDto{" +
                "messageId='" + messageId + '\'' +
                ", readBy='" + readBy + '\'' +
                '}';
    }
}
