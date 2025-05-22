package com.example.asiochatfrontend.core.model.dto;

public class VideoUploadDto {
    private String chatId;
    private String messageId;

    public VideoUploadDto() {
    }

    public VideoUploadDto(String chatId, String messageId) {
        this.chatId = chatId;
        this.messageId = messageId;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
}
