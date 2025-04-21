package com.example.asiochatfrontend.core.service;

import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
import com.example.asiochatfrontend.core.model.dto.MediaStreamResultDto;

public interface MediaService {
    MediaMessageDto createMediaMessage(MediaMessageDto mediaMessageDto) throws Exception;
    MediaMessageDto getMediaMessage(String mediaMessageId) throws Exception;
    MediaStreamResultDto getMediaStream(String mediaId) throws Exception;
}