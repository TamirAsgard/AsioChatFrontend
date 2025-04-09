package com.example.asiochatfrontend.domain.usecase.media;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.MediaStreamResultDto;

public class GetMediaStreamUseCase {
    private final ConnectionManager connectionManager;

    public GetMediaStreamUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public MediaStreamResultDto execute(String mediaId) throws Exception {
        return connectionManager.getMediaStream(mediaId);
    }
}