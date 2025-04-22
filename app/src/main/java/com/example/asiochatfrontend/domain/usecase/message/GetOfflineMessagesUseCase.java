package com.example.asiochatfrontend.domain.usecase.message;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;

import java.util.List;

public class GetOfflineMessagesUseCase {
    private final ConnectionManager connectionManager;

    public GetOfflineMessagesUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<MessageDto> execute(String userId) throws Exception {
        return connectionManager.getOfflineMessages(userId);
    }
}
