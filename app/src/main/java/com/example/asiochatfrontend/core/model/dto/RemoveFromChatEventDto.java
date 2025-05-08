package com.example.asiochatfrontend.core.model.dto;

public class RemoveFromChatEventDto {
    private String userIdToRemove;
    private String chatIdToRemoveFrom;
    public RemoveFromChatEventDto(String userIdToRemove, String chatIdToRemoveFrom) {
        this.userIdToRemove = userIdToRemove;
        this.chatIdToRemoveFrom = chatIdToRemoveFrom;
    }

    public String getUserIdToRemove() {
        return userIdToRemove;
    }

    public void setUserIdToRemove(String userIdToRemove) {
        this.userIdToRemove = userIdToRemove;
    }

    public String getChatIdToRemoveFrom() {
        return chatIdToRemoveFrom;
    }

    public void setChatIdToRemoveFrom(String chatIdToRemoveFrom) {
        this.chatIdToRemoveFrom = chatIdToRemoveFrom;
    }
}
