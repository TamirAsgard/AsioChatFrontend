package com.example.asiochatfrontend.domain.usecase.message;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.MessageDto;

import java.util.List;

public class GetMessagesForChatUseCase {
    private final ConnectionManager connectionManager;

    public GetMessagesForChatUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<MessageDto> execute(String chatId) throws Exception {
        return connectionManager.getMessagesForChat(chatId);
    }
}
