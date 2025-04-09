package com.example.asiochatfrontend.data.direct.service;

import android.util.Log;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.core.service.MessageService;
import com.example.asiochatfrontend.data.database.entity.MessageEntity;
import com.example.asiochatfrontend.data.direct.network.DirectWebSocketClient;
import com.example.asiochatfrontend.domain.repository.ChatRepository;
import com.example.asiochatfrontend.domain.repository.MessageRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

public class DirectMessageService implements MessageService {
    private static final String TAG = "DirectMessageService";

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final DirectWebSocketClient directWebSocketClient;
    private final ConnectionManager connectionManager;
    private final Map<String, MessageState> messageStateCache = new ConcurrentHashMap<>();

    @Inject
    public DirectMessageService(
            MessageRepository messageRepository,
            ChatRepository chatRepository,
            DirectWebSocketClient directWebSocketClient,
            ConnectionManager connectionManager
    ) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.directWebSocketClient = directWebSocketClient;
        this.connectionManager = connectionManager;

        // Setup peer message listener
        directWebSocketClient.addPeerConnectionListener(new DirectWebSocketClient.PeerConnectionListener() {
            @Override
            public void onMessageReceived(MessageDto message) {
                Log.d(TAG, "Received message: " + message.getId());
                processIncomingMessage(message);
            }

            @Override
            public void onPeerDiscovered(String peerId, String peerIp) {
                // Optional: handle peer discovery
            }

            @Override
            public void onPeerStatusChanged(String peerId, boolean isOnline) {
                // Optional: handle peer status changes
            }
        });
    }

    private void processIncomingMessage(MessageDto message) {
        // Save the incoming message
        messageRepository.saveMessage(message);

        // Update chat's last message
        ChatDto chat = chatRepository.getChatById(message.getChatId());
        if (chat != null) {
            chat.setLastMessage(message);
            chat.setUpdatedAt(new java.util.Date());
            chatRepository.updateChat(chat);
        }

        // Automatically mark as delivered
        message.setState(MessageState.DELIVERED);
        messageRepository.updateMessageState(message.getId(), MessageState.DELIVERED);
    }

    @Override
    public MessageDto sendMessage(MessageDto messageDto) {
        // Ensure message is saved locally first
        try {
            messageRepository.saveMessage(messageDto);
            messageStateCache.put(messageDto.getId(), MessageState.PENDING);

            ChatDto chat = chatRepository.getChatById(messageDto.getChatId());
            List<String> onlineUsers = connectionManager.getO nlineUsers();

            if (chat != null && onlineUsers != null) {
                for (String participantId : chat.getParticipants()) {
                    if (!participantId.equals(messageDto.getSenderId()) && onlineUsers.contains(participantId)) {
                        String peerIp = connectionManager.getPeerIpForUser(participantId);
                        if (peerIp != null) {
                            directWebSocketClient.sendMessageToPeer(peerIp, messageDto);
                        }
                    }
                }
            }

            // Update last message in chat
            updateChatLastMessage(messageDto);

        } catch (Exception e) {
            Log.e(TAG, "Failed to send message", e);

            // Mark message as failed
            MessageState failedState = MessageState.FAILED;
            messageStateCache.put(messageDto.getId(), failedState);
            messageRepository.updateMessageState(messageDto.getId(), failedState);
        }

        return messageDto;
    }

    @Override
    public boolean resendFailedMessage(String messageId) {
        MessageDto message = messageRepository.getMessageById(messageId);
        if (message == null) return false;

        if (messageStateCache.get(messageId) != MessageState.FAILED) return false;

        // Attempt to resend the message
        try {
            return sendMessage(message) != null;
        } catch (Exception e) {
            Log.e(TAG, "Failed to resend message", e);
            return false;
        }
    }

    private void updateChatLastMessage(MessageDto messageDto) {
        ChatDto chat = chatRepository.getChatById(messageDto.getChatId());
        if (chat != null) {
            chat.setLastMessage(messageDto);
            chat.setUpdatedAt(new java.util.Date());
            chatRepository.updateChat(chat);
        }
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
        // Retrieve locally stored undelivered messages
        return messageRepository.getFailedMessages();
    }

    @Override
    public boolean setMessagesInChatReadByUser(String chatId, String userId) throws Exception {
        List<MessageDto> messages = messageRepository.getMessagesForChat(chatId);
        for (MessageDto message : messages) {
            if (message.getSenderId().equals(userId)) {
                continue; // Skip messages sent by the user
            }

            if (message.getState() == MessageState.READ) {
                continue; // Already read
            }

            List<String> waitingMembers = new ArrayList<>(message.waitingMembersList);
            waitingMembers.remove(userId);

            if (waitingMembers.isEmpty()) {
                message.setState(MessageState.READ);
                message.waitingMembersList = Collections.emptyList();
            } else {
                message.setWaitingMembersList(waitingMembers);
            }

            messageRepository.updateMessage(message);
        }

        return true;
    }

    @Override
    public boolean setMessageReadByUser(String messageId, String userId) throws Exception {
        MessageDto message = messageRepository.getMessageById(messageId);
        List<String> updated = new ArrayList<>(message.waitingMembersList);
        updated.remove(userId);
        if (updated.isEmpty()) {
            message.setState(MessageState.READ);
        }
        message.waitingMembersList = updated;
        messageRepository.updateMessage(message);
        return true;
    }
}