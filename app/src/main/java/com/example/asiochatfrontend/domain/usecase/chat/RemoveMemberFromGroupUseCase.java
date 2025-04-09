package com.example.asiochatfrontend.domain.usecase.chat;

import com.example.asiochatfrontend.core.connection.ConnectionManager;

public class RemoveMemberFromGroupUseCase {
    private final ConnectionManager connectionManager;

    public RemoveMemberFromGroupUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public boolean execute(String chatId, String userId) throws Exception {
        return connectionManager.removeMemberFromGroup(chatId, userId);
    }
}