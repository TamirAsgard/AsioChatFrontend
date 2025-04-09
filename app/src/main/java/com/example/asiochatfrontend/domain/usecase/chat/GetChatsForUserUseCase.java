package com.example.asiochatfrontend.domain.usecase.chat;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.ChatDto;

import java.util.List;

public class GetChatsForUserUseCase {
    private final ConnectionManager connectionManager;

    public GetChatsForUserUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<ChatDto> execute(String userId) throws Exception {
        return connectionManager.getChatsForUser(userId);
    }
}