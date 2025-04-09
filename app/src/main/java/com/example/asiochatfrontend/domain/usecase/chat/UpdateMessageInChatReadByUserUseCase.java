package com.example.asiochatfrontend.domain.usecase.chat;

import com.example.asiochatfrontend.core.connection.ConnectionManager;

public class UpdateMessageInChatReadByUserUseCase {
    private final ConnectionManager connectionManager;

    public UpdateMessageInChatReadByUserUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public boolean execute(String chatId, String newName) throws Exception {
        return connectionManager.setMessagesInChatReadByUser(chatId, newName);
    }
}
