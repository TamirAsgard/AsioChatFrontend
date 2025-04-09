package com.example.asiochatfrontend.domain.usecase.chat;

import com.example.asiochatfrontend.core.connection.ConnectionManager;

public class UpdateGroupNameUseCase {
    private final ConnectionManager connectionManager;

    public UpdateGroupNameUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public boolean execute(String chatId, String newName) throws Exception {
        return connectionManager.updateGroupName(chatId, newName);
    }
}