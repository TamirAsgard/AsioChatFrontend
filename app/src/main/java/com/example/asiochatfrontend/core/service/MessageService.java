package com.example.asiochatfrontend.core.service;

import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;

import java.util.List;

public interface MessageService {
    MessageDto sendMessage(MessageDto messageDto) throws Exception;

    List<TextMessageDto> getMessagesForChat(String chatId) throws Exception;

    List<MessageDto> getOfflineMessages(String userId) throws Exception;
    boolean setMessagesInChatReadByUser(String chatId, String userId) throws Exception;

    boolean setMessageReadByUser(String messageId, String userId) throws Exception;

    boolean markMessageAsRead(String messageId, String userId) throws Exception;

    boolean resendFailedMessage(String messageId) throws Exception;
}