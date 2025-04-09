package com.example.asiochatfrontend.data.direct.service;

import android.util.Log;

import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.core.service.MessageService;
import com.example.asiochatfrontend.data.direct.network.DirectWebSocketClient;
import com.example.asiochatfrontend.domain.repository.ChatRepository;
import com.example.asiochatfrontend.domain.repository.MessageRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

public class DirectMessageService implements MessageService {
    private static final String TAG = "DirectMessageService";

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final DirectWebSocketClient webSocketClient;
    private final Map<String, MessageState> messageStateCache = new ConcurrentHashMap<>();

    @Inject
    public DirectMessageService(
            MessageRepository messageRepository,
            ChatRepository chatRepository,
            DirectWebSocketClient webSocketClient) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.webSocketClient = webSocketClient;

        // Setup WebSocket message listener
        webSocketClient.addMessageListener(message -> {
            Log.d(TAG, "Received message: " + message.getId());
            messageRepository.saveMessage(message);
        });
    }

    @Override
    public MessageDto sendMessage(MessageDto messageDto) {
        if (!webSocketClient.isConnected()) {
            webSocketClient.connect();
        }

        messageRepository.saveMessage(messageDto);
        messageStateCache.put(messageDto.getId(), MessageState.PENDING);

        boolean success = webSocketClient.sendMessage(messageDto);
        MessageState newState = success ? MessageState.SENT : MessageState.FAILED;
        messageStateCache.put(messageDto.getId(), newState);
        messageRepository.updateMessageState(messageDto.getId(), newState);

        if (success) {
            // Update last message in chat
            ChatDto chat = chatRepository.getChatById(messageDto.getChatId());
            if (chat != null) {
                chat.setLastMessage(messageDto);
                chat.setUpdatedAt(new java.util.Date());
                chatRepository.updateChat(chat);
            }
        }

        return messageDto;
    }

    @Override
    public boolean resendFailedMessage(String messageId) {
        MessageDto message = messageRepository.getMessageById(messageId);
        if (message == null) return false;

        if (messageStateCache.get(messageId) != MessageState.FAILED) return false;

        if (!webSocketClient.isConnected()) {
            webSocketClient.connect();
        }

        messageStateCache.put(messageId, MessageState.PENDING);
        messageRepository.updateMessageState(messageId, MessageState.PENDING);

        boolean success = webSocketClient.sendMessage(message);
        MessageState newState = success ? MessageState.SENT : MessageState.FAILED;
        messageStateCache.put(messageId, newState);
        messageRepository.updateMessageState(messageId, newState);

        return success;
    }

    @Override
    public boolean markMessageAsRead(String messageId, String userId) {
        messageRepository.updateMessageState(messageId, MessageState.READ);
        return true;
    }

    @Override
    public List<MessageDto> getMessagesForChat(String chatId) {
        return messageRepository.getMessagesForChat(chatId);
    }

    @Override
    public List<MessageDto> getOfflineMessages(String userId) {
        // With WebSocket, offline messages would need to be stored locally
        // and retrieved when connection is re-established
        return Collections.emptyList();
    }
}