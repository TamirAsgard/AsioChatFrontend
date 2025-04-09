package com.example.asiochatfrontend.domain.usecase.message;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.MessageDto;

public class CreateMessageUseCase {
    private final ConnectionManager connectionManager;

    public CreateMessageUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public MessageDto execute(MessageDto messageDto) throws Exception {
        return connectionManager.sendMessage(messageDto);
    }
}
