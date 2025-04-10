package com.example.asiochatfrontend.data.direct.service;

import android.util.Log;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.core.service.MessageService;
import com.example.asiochatfrontend.data.direct.network.DirectWebSocketClient;
import com.example.asiochatfrontend.domain.repository.ChatRepository;
import com.example.asiochatfrontend.domain.repository.MessageRepository;

import java.util.ArrayList;
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
                Log.d(TAG, "Received message: " + message.getId() + " from " + message.getSenderId());
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
            message.setState(MessageState.DELIVERED);
            message.setDeliveredAt(new Date());

            // If this message is for current user, add it to the repository
            messageRepository.saveMessage(message);

            // Update chat's last message
            ChatDto chat = chatRepository.getChatById(message.getChatId());
            if (chat != null) {
                chatRepository.updateLastMessage(chat.getId(), message.getId());

                // Increment unread count if not from current user
                if (!message.getSenderId().equals(currentUserId)) {
                    chatRepository.updateUnreadCount(chat.getId(), chat.getUnreadCount() + 1);
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
        MessageDto receipt = new MessageDto(
                "receipt-" + message.getId(),
                message.getChatId(),
                currentUserId,
                "DELIVERY_RECEIPT:" + message.getId(),
                null, // No media
                null, // Not a reply
                MessageState.DELIVERED,
                new ArrayList<>(),
                new Date(),
                new Date(),
                null // Not read yet
        );

        try {
            // Send the receipt directly to the sender
            directWebSocketClient.sendMessage(message.getSenderId(), receipt);
        } catch (Exception e) {
            Log.e(TAG, "Error sending delivery receipt", e);
        }
    }

    @Override
    public MessageDto sendMessage(MessageDto message) throws Exception {
        Log.d(TAG, "Sending message: " + message.getId() + " to chat: " + message.getChatId());

        // Make sure the message has a state
        if (message.getState() == null) {
            message.setState(MessageState.PENDING);
        }

        // Save the message to the repository first
        messageRepository.saveMessage(message);
        messageStateCache.put(message.getId(), message.getState());

        // Update chat's last message
        chatRepository.updateLastMessage(message.getChatId(), message.getId());

        // Get the chat to find recipients
        ChatDto chat = chatRepository.getChatById(message.getChatId());
        if (chat == null) {
            throw new Exception("Chat not found: " + message.getChatId());
        }

        // Get online users
        List<String> onlineUsers = connectionManager.getOnlineUsers();
        boolean allDelivered = true;

        // Send to each recipient
        for (String recipientId : chat.getParticipants()) {
            // Skip sending to ourselves
            if (recipientId.equals(message.getSenderId())) {
                continue;
            }

            // Check if recipient is online
            if (onlineUsers.contains(recipientId)) {
                // Send message
                boolean sent = directWebSocketClient.sendMessage(recipientId, message);
                if (!sent) {
                    allDelivered = false;
                    Log.w(TAG, "Failed to send message to recipient: " + recipientId);
                } else {
                    Log.d(TAG, "Message sent to recipient: " + recipientId);
                }
            } else {
                // Recipient is offline
                allDelivered = false;
                Log.d(TAG, "Recipient offline: " + recipientId);
            }
        }

        // Update message state based on delivery status
        MessageState newState = allDelivered ? MessageState.DELIVERED : MessageState.SENT;
        message.setState(newState);
        messageStateCache.put(message.getId(), newState);
        messageRepository.updateMessageState(message.getId(), newState);

        if (allDelivered) {
            message.setDeliveredAt(new Date());
            messageRepository.updateMessageDeliveredAt(message.getId(), new Date());
        }

        return message;
    }

    @Override
    public boolean markMessageAsRead(String messageId, String userId) {
        try {
            MessageDto message = messageRepository.getMessageById(messageId);
            if (message == null) {
                Log.w(TAG, "Message not found: " + messageId);
                return false;
            }

            // Mark as read
            message.setState(MessageState.READ);
            message.setReadAt(new Date());
            messageRepository.updateMessage(message);
            messageStateCache.put(message.getId(), MessageState.READ);

            // Send read receipt to sender
            sendReadReceipt(message);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error marking message as read", e);
            return false;
        }
    }

    private void sendReadReceipt(MessageDto message) {
        // Create a read receipt as a message
        MessageDto receipt = new MessageDto(
                "read-receipt-" + message.getId(),
                message.getChatId(),
                currentUserId,
                "READ_RECEIPT:" + message.getId(),
                null, // No media
                null, // Not a reply
                MessageState.READ,
                new ArrayList<>(),
                new Date(),
                new Date(),
                new Date() // Read now
        );

        try {
            // Send the receipt directly to the sender
            directWebSocketClient.sendMessage(message.getSenderId(), receipt);
        } catch (Exception e) {
            Log.e(TAG, "Error sending read receipt", e);
        }
    }

    @Override
    public boolean resendFailedMessage(String messageId) {
        try {
            MessageDto message = messageRepository.getMessageById(messageId);
            if (message == null) {
                Log.w(TAG, "Message not found: " + messageId);
                return false;
            }

            // Check if message is in a state that can be resent
            MessageState state = messageStateCache.getOrDefault(messageId, message.getState());
            if (state != MessageState.FAILED && state != MessageState.PENDING) {
                Log.w(TAG, "Message is not in a FAILED or PENDING state: " + messageId);
                return false;
            }

            // Update state to pending
            message.setState(MessageState.PENDING);
            messageRepository.updateMessageState(messageId, MessageState.PENDING);
            messageStateCache.put(messageId, MessageState.PENDING);

            // Send the message
            sendMessage(message);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error resending message", e);
            return false;
        }
    }

    @Override
    public List<MessageDto> getMessagesForChat(String chatId) {
        try {
            return messageRepository.getMessagesForChat(chatId);
        } catch (Exception e) {
            Log.e(TAG, "Error getting messages for chat", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<MessageDto> getOfflineMessages(String userId) {
        // In direct mode, there's no server to store offline messages
        // We can only return failed messages from the local database
        try {
            return messageRepository.getFailedMessages();
        } catch (Exception e) {
            Log.e(TAG, "Error getting offline messages", e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean setMessageReadByUser(String messageId, String userId) throws Exception {
        return markMessageAsRead(messageId, userId);
    }

    @Override
    public boolean setMessagesInChatReadByUser(String chatId, String userId) throws Exception {
        try {
            List<MessageDto> messages = messageRepository.getMessagesForChat(chatId);
            for (MessageDto message : messages) {
                // Only mark others' messages as read
                if (!message.getSenderId().equals(userId)) {
                    markMessageAsRead(message.getId(), userId);
                }
            }

            // Reset unread count for the chat
            ChatDto chat = chatRepository.getChatById(chatId);
            if (chat != null) {
                chatRepository.updateUnreadCount(chatId, 0);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error marking chat messages as read", e);
            throw e;
        }
    }
}