package com.example.asiochatfrontend.domain.usecase.message;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;

import java.util.List;

public class GetMessagesForChatUseCase {
    private final ConnectionManager connectionManager;

    public GetMessagesForChatUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<TextMessageDto> execute(String chatId) throws Exception {
        return connectionManager.getMessagesForChat(chatId);
    }
}
