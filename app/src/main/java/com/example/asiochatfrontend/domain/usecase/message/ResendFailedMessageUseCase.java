package com.example.asiochatfrontend.domain.usecase.message;

import com.example.asiochatfrontend.core.connection.ConnectionManager;

public class ResendFailedMessageUseCase {
    private final ConnectionManager connectionManager;

    public ResendFailedMessageUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public boolean execute(String messageId) throws Exception {
        return connectionManager.resendFailedMessage(messageId);
    }
}