package com.example.asiochatfrontend.domain.usecase.media;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;

import java.util.List;

public class GetMediaMessagesUseCase {
    private final ConnectionManager connectionManager;

    public GetMediaMessagesUseCase(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public List<MediaMessageDto> execute(String chatId) throws Exception {
        return connectionManager.getMediaMessagesForChat(chatId);
    }
}
