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
    private final DirectWebSocketClient directWebSocketClient;
    private final Map<String, MessageState> messageStateCache = new ConcurrentHashMap<>();

    @Inject
    public DirectMessageService(
            MessageRepository messageRepository,
            ChatRepository chatRepository,
            DirectWebSocketClient directWebSocketClient) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.directWebSocketClient = directWebSocketClient;

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
        messageRepository.saveMessage(messageDto);
        messageStateCache.put(messageDto.getId(), MessageState.PENDING);

        try {
            // Find the recipient's IP (you might need to implement this logic)
            String recipientIp = findRecipientIp(messageDto.getChatId());

            if (recipientIp != null) {
                // Send message to specific peer
                directWebSocketClient.sendMessageToPeer(recipientIp, messageDto);

                // Update message state
                MessageState newState = MessageState.SENT;
                messageStateCache.put(messageDto.getId(), newState);
                messageRepository.updateMessageState(messageDto.getId(), newState);
            } else {
                // No recipient found, mark as failed
                throw new Exception("Recipient not found");
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

    private String findRecipientIp(String chatId) {
        // Implement logic to find recipient's IP based on chat participants
        // This might involve querying the UserDiscoveryManager or another service
        ChatDto chat = chatRepository.getChatById(chatId);
        if (chat == null || chat.getParticipants().size() < 2) {
            return null;
        }

        // Assuming the first participant is the sender
        // And the second is the recipient for a private chat
        String recipientId = chat.getParticipants().get(1);

        // You'll need to implement a method to get IP from user ID
        return getIpForUserId(recipientId);
    }

    private String getIpForUserId(String userId) {
        // Implement method to get IP address for a given user ID
        // This might involve querying UserDiscoveryManager or a similar service
        return null; // Placeholder
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
}