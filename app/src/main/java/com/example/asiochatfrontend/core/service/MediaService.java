package com.example.asiochatfrontend.core.service;

import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
import com.example.asiochatfrontend.core.model.dto.MediaStreamResultDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;

import java.util.List;

public interface MediaService {
    MediaMessageDto createMediaMessage(MediaMessageDto mediaMessageDto) throws Exception;
    MediaMessageDto getMediaMessage(String mediaMessageId) throws Exception;
    MediaStreamResultDto getMediaStream(String mediaId);
    List<MediaMessageDto> getMediaMessagesForChat(String chatId);
    List<MessageDto> sendPendingMessages();

    int getUnreadMessagesCount(String chatId, String userId);
}