package com.example.asiochatfrontend.domain.usecase.media;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;

public class GetMediaMessageUseCase {
    private final ConnectionManager connectionManager;

    public GetMediaMessageUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public MediaMessageDto execute(String mediaId) throws Exception {
        return connectionManager.getMediaMessage(mediaId);
    }
}