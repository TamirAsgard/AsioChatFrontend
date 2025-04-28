package com.example.asiochatfrontend.domain.usecase.chat;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.ChatDto;

public class CreatePrivateChatUseCase {
    private final ConnectionManager connectionManager;

    public CreatePrivateChatUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public ChatDto execute(String chatId, String userId, String othersId) throws Exception {
        return connectionManager.createPrivateChat(chatId, userId, othersId);
    }
}