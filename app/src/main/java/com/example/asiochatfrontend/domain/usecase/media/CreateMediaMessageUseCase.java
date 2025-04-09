package com.example.asiochatfrontend.domain.usecase.media;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;

public class CreateMediaMessageUseCase {
    private final ConnectionManager connectionManager;

    public CreateMediaMessageUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public MediaMessageDto execute(MediaMessageDto dto) throws Exception {
        return connectionManager.createMediaMessage(dto);
    }
}
