package com.example.asiochatfrontend.domain.usecase.chat;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.ChatDto;

import java.util.List;

public class CreateGroupChatUseCase {
    private final ConnectionManager connectionManager;

    public CreateGroupChatUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public ChatDto execute(String chatId, String name, List<String> memberIds, String creatorId) throws Exception {
        return connectionManager.createGroupChat(chatId, name, memberIds, creatorId);
    }
}