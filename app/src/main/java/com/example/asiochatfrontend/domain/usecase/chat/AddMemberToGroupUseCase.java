package com.example.asiochatfrontend.domain.usecase.chat;

import com.example.asiochatfrontend.core.connection.ConnectionManager;

public class AddMemberToGroupUseCase {
    private final ConnectionManager connectionManager;

    public AddMemberToGroupUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public boolean execute(String chatId, String userId) throws Exception {
        return connectionManager.addMemberToGroup(chatId, userId);
    }
}