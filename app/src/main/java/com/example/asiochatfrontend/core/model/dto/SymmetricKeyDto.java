package com.example.asiochatfrontend.core.model.dto;

public class SymmetricKeyDto {
    private String chatId;
    private String symmetricKey;
    private long createdAt;
    private long expiresAt;

    public SymmetricKeyDto(String chatId, String symmetricKey, long createdAt, long expiresAt) {
        this.chatId = chatId;
        this.symmetricKey = symmetricKey;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getSymmetricKey() {
        return symmetricKey;
    }

    public void setSymmetricKey(String symmetricKey) {
        this.symmetricKey = symmetricKey;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
}
