package com.example.asiochatfrontend.data.direct.service;

import android.util.Log;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.core.service.MessageService;
import com.example.asiochatfrontend.data.direct.network.DirectWebSocketClient;
import com.example.asiochatfrontend.domain.repository.ChatRepository;
import com.example.asiochatfrontend.domain.repository.MessageRepository;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DirectMessageService implements MessageService {
    private static final String TAG = "DirectMessageService";

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final DirectWebSocketClient directWebSocketClient;
    private ConnectionManager connectionManager;
    private final Map<String, MessageState> messageStateCache = new ConcurrentHashMap<>();
    private String currentUserId;

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

        // Set up message listener
        setupMessageListener();
    }

    /**
     * Set the ConnectionManager (used to fix circular dependency)
     */
    public void setConnectionManager(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Set current user ID
     */
    public void setCurrentUserId(String userId) {
        this.currentUserId = userId;
    }

    private void setupMessageListener() {
        directWebSocketClient.addPeerConnectionListener(new DirectWebSocketClient.PeerConnectionListener() {
            @Override
            public void onMessageReceived(MessageDto message) {
                Log.d(TAG, "Received message: " + message.getId() + " from " + message.getJid());
                processIncomingMessage(message);
            }

            @Override
            public void onPeerDiscovered(String peerId, String peerIp) {
                // Not used here
            }

            @Override
            public void onPeerStatusChanged(String peerId, boolean isOnline) {
                // Not used here
            }
        });
    }

    private void processIncomingMessage(MessageDto message) {
        // Save the message to the repository
        try {
            // Mark as delivered
            message.setStatus(MessageState.SENT);
            message.setTimestamp(new Date());

            // If this message is for current user, add it to the repository
            messageRepository.saveMessage((TextMessageDto) message);

            // Update chat's last message
            ChatDto chat = chatRepository.getChatById(message.getChatId());
            if (chat != null) {
                chatRepository.updateLastMessage(chat.getChatId(), message.getId());

                // Increment unread count if not from current user
                if (!message.getJid().equals(currentUserId)) {
                    chatRepository.updateUnreadCount(chat.getChatName(),  1);
                }
            }

            // Send delivery receipt
            sendDeliveryReceipt(message);

            Log.d(TAG, "Processed incoming message: " + message.getId());
        } catch (Exception e) {
            Log.e(TAG, "Error processing incoming message", e);
        }
    }

    private void sendDeliveryReceipt(MessageDto message) {
        // Create a delivery receipt as a message
        TextMessageDto receipt = new TextMessageDto();

        try {
            // Send the receipt directly to the sender
            directWebSocketClient.sendMessage(message.getJid(), receipt);
        } catch (Exception e) {
            Log.e(TAG, "Error sending delivery receipt", e);
        }
    }

    @Override
    public MessageDto sendMessage(MessageDto messageDto) throws Exception {
        return null;
    }

    @Override
    public List<TextMessageDto> getMessagesForChat(String chatId) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public List<MessageDto> getOfflineMessages(String userId) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public List<MessageDto> sendPendingMessages() throws Exception {
        return Collections.emptyList();
    }

    @Override
    public boolean setMessagesInChatReadByUser(String chatId, String userId) throws Exception {
        return false;
    }

    @Override
    public boolean setMessageReadByUser(String messageId, String userId, String readBy) throws Exception {
        return false;
    }

    @Override
    public boolean markMessageAsRead(String messageId, String userId) throws Exception {
        return false;
    }

    @Override
    public boolean resendFailedMessage(String messageId) throws Exception {
        return false;
    }

    @Override
    public int getUnreadMessagesCount(String chatId, String userId) throws Exception {
        return 0;
    }
}