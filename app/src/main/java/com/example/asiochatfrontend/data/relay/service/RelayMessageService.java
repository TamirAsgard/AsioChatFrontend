package com.example.asiochatfrontend.data.relay.service;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.core.service.AuthService;
import com.example.asiochatfrontend.core.service.MessageService;
import com.example.asiochatfrontend.core.model.dto.MessageReadByDto;
import com.example.asiochatfrontend.data.relay.model.WebSocketEvent;
import com.example.asiochatfrontend.data.relay.network.RelayApiClient;
import com.example.asiochatfrontend.data.relay.network.RelayWebSocketClient;
import com.example.asiochatfrontend.domain.repository.ChatRepository;
import com.example.asiochatfrontend.domain.repository.MessageRepository;
import com.example.asiochatfrontend.ui.chat.bus.ChatUpdateBus;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.w3c.dom.Text;

import java.util.*;
import java.util.concurrent.*;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RelayMessageService implements MessageService, RelayWebSocketClient.RelayWebSocketListener {

    private static final String TAG = "RelayMessageService";
    private static final int RETRY_DELAY_SECONDS = 5;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final AuthService authService;
    private final RelayApiClient relayApiClient;
    private final RelayWebSocketClient webSocketClient;
    private final Gson gson;
    private String currentUserId;

    // LiveData for real-time message updates by chat ID
    private final MutableLiveData<MessageDto> incomingMessageLiveData = new MutableLiveData<>();
    private final MutableLiveData<MessageDto> outgoingMessageLiveData = new MutableLiveData<>();

    // Cache to avoid duplicate message processing
    private final Set<String> recentlyProcessedMessageIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Inject
    public RelayMessageService(
            MessageRepository messageRepository,
            ChatRepository chatRepository,
            AuthService authService,
            RelayApiClient relayApiClient,
            RelayWebSocketClient webSocketClient,
            Gson gson,
            String currentUserId
    ) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.authService = authService;
        this.relayApiClient = relayApiClient;
        this.webSocketClient = webSocketClient;
        this.gson = gson;
        this.currentUserId = currentUserId;

        // Register this service as a WebSocket listener
        this.webSocketClient.addListener(this);
    }

    /**
     * Handles WebSocket events
     */
    @Override
    public void onEvent(WebSocketEvent event) {
        try {
            Log.i("TAG", "WebSocket event received: " + event.toString());
            switch (event.getType()) {
                case INCOMING:
                    handleIncomingMessage(event);
                    break;
                case MESSAGE_READ:
                    // Handle message read event
                    MessageReadByDto readByDto = gson.fromJson(event.getPayload(), MessageReadByDto.class);
                    markMessageAsRead(readByDto.getMessageId(), readByDto.getReadBy());
                    break;
                default:
                    Log.d(TAG, "Unhandled WebSocket event type: " + event.getType());
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing WebSocket event: " + event.getType(), e);
        }
    }

    private void handleIncomingMessage(WebSocketEvent event) {
        try {
            if (event.getPayload() == null) {
                Log.e(TAG, "Received null payload in WebSocket event");
                return;
            }
            TextMessageDto message = gson.fromJson(event.getPayload(), TextMessageDto.class);
            if (message == null) return;

            // Process the message
            message = processRemoteMessage(message, message.getChatId());
            if (message == null) {
                Log.e(TAG, "Failed to process incoming message");
                return;
            }

            // Skip messages from self
            if (currentUserId != null && currentUserId.equals(message.getJid())) {
                return;
            }

            // Avoid duplicate message processing
            if (recentlyProcessedMessageIds.contains(message.getId())) {
                return;
            }

            recentlyProcessedMessageIds.add(message.getId());

            Log.d(TAG, "Received message via WebSocket: " + message.getId() + " for chat: " + message.getChatId());

            // Process message
            message.setStatus(message.getStatus());
            if (message.getTimestamp() == null) {
                message.setTimestamp(new Date());
            }

            // Save to repository
            messageRepository.saveMessage((TextMessageDto) message);

            // Update chat's last message
            if (message.getChatId() != null) {
                chatRepository.updateLastMessage(message.getChatId(), message.getId());
            }

            // Add message to LiveData for real-time display
            incomingMessageLiveData.postValue(message);
            ChatUpdateBus.postLastMessageUpdate(message);

            TextMessageDto finalMessage = message;
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    int unreadMessagesCount = messageRepository.getUnreadMessagesCount(finalMessage.getChatId(), currentUserId);
                    ChatUpdateBus.postUnreadCountUpdate(finalMessage.getChatId(), unreadMessagesCount);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to get unread count", e);
                }
            });

            // Schedule cleanup of processed ID after a delay
            CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS).execute(() -> {
                recentlyProcessedMessageIds.remove(finalMessage.getId());
            });
        } catch (Exception e) {
            Log.e(TAG, "Error handling incoming message", e);
        }
    }

    @Override
    public boolean markMessageAsRead(String messageId, String userId) {
        try {
            MessageDto message = messageRepository.getMessageById(messageId);
            if (message == null) return false;

            // Skip if this is your own message
            if (currentUserId != null && currentUserId.equals(userId)) {
                return true;
            }

            List<String> waitingMembersList = new ArrayList<>(message.getWaitingMemebersList());
            waitingMembersList.remove(userId);
            message.setWaitingMemebersList(waitingMembersList);

            // Update local status
            if (message.getWaitingMemebersList().isEmpty()) {
                message.setStatus(MessageState.READ);
            }

            messageRepository.updateMessage((TextMessageDto) message);

            // Update LiveData
            outgoingMessageLiveData.postValue(message);
            Log.d(TAG, "Message marked as read: " + messageId + " by user: " + userId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error marking message as read", e);
            return false;
        }
    }

    @Override
    public MessageDto sendMessage(MessageDto messageDto) throws Exception {
        // Set current user if not already set
        if (currentUserId == null && messageDto.getJid() != null) {
            currentUserId = messageDto.getJid();
        }

        // Generate message ID if not provided
        if (messageDto.getId() == null) {
            messageDto.setId(UUID.randomUUID().toString());
        }

        // Set message defaults
        if (messageDto.getStatus() == null) {
            messageDto.setStatus(MessageState.UNKNOWN);
        }

        // Save in local repository
        messageRepository.saveMessage((TextMessageDto) messageDto);

        // Send via WebSocket for real-time delivery
        try {
            // Message payload encryption //
            ChatDto targetChat = chatRepository.getChatById(messageDto.getChatId());
            String messagePayload = ((TextMessageDto) messageDto).getPayload();
            long currentTimestamp = System.currentTimeMillis();
            String encryptedPayload = null;

            if (targetChat.getGroup()) {
                // <--- Group chat, encrypt message with group symmetric key ---->
                encryptedPayload = authService.encryptWithSymmetricKey(messagePayload, targetChat.getChatId(), currentTimestamp);

            } else {
                // <--- Private chat, encrypt message with recipient's public key ---->
                String recipientId = messageDto
                        .getWaitingMemebersList()
                        .stream().filter(id -> !id.equals(currentUserId))
                        .findFirst()
                        .get();

                encryptedPayload = authService.encryptWithPublicKey(messagePayload, recipientId, currentTimestamp);
            }

            // Set the encrypted payload back to the message DTO
            ((TextMessageDto) messageDto).setPayload(encryptedPayload);
            JsonObject messageJson = gson.toJsonTree(messageDto).getAsJsonObject();

            // Send the message payload via WebSocket
            WebSocketEvent event = new WebSocketEvent(
                    WebSocketEvent.EventType.CHAT,
                    messageJson,
                    messageDto.getJid()
            );

            webSocketClient.sendEvent(event);
            Log.d(TAG, "Message sent via WebSocket: " + messageDto.getId());

            // Mark as sent
            messageDto.setStatus(MessageState.SENT);
            // Keep the original payload for local storage
            ((TextMessageDto) messageDto).setPayload(messagePayload);
            messageRepository.updateMessage((TextMessageDto) messageDto);
            chatRepository.updateLastMessage(messageDto.getChatId(), messageDto.getId());
            ChatUpdateBus.postLastMessageUpdate(messageDto);

            return messageDto;
        } catch (Exception e) {
            Log.e(TAG, "Error sending message via WebSocket, message status unknown", e);
            messageDto.setStatus(MessageState.UNKNOWN);
            messageRepository.updateMessage((TextMessageDto) messageDto);
            return messageDto;
        }
    }

    @Override
    public List<TextMessageDto> getMessagesForChat(String chatId) {
        try {
            List<TextMessageDto> remoteMessages = relayApiClient.getMessagesForChat(chatId);
            List<TextMessageDto> chatMessages = new ArrayList<>();

            if (remoteMessages != null && !remoteMessages.isEmpty()) {
                for (TextMessageDto remoteMessage : remoteMessages) {
                    TextMessageDto processedMessage = processRemoteMessage(remoteMessage, chatId);
                    if (processedMessage != null) {
                        chatMessages.add(processedMessage);
                    }
                }
                return chatMessages;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get messages from server, using local cache", e);
        }

        return messageRepository.getMessagesForChat(chatId);
    }

    private TextMessageDto processRemoteMessage(TextMessageDto remoteMessage, String chatId) {
        try {
            ChatDto targetChat = chatRepository.getChatById(chatId);
            String decryptedPayload = null;

            if (remoteMessage.getJid().equals(currentUserId)) {
                // Self message, update only status, waiting members list and timestamp
                TextMessageDto messageToUpdate = messageRepository.getMessageById(remoteMessage.getId());
                messageToUpdate.setStatus(remoteMessage.getStatus());
                messageToUpdate.setWaitingMemebersList(remoteMessage.getWaitingMemebersList());
                messageToUpdate.setTimestamp(remoteMessage.getTimestamp());
                messageRepository.saveMessage(messageToUpdate);
                return messageToUpdate;
            } else {
                // Not a self message, decrypt payload
                long messageTimestamp = System.currentTimeMillis();
                if (remoteMessage.getTimestamp() != null) {
                    messageTimestamp = remoteMessage.getTimestamp().getTime();
                }
                if (targetChat.getGroup()) {
                    // Group chat: decrypt with group symmetric key
                    decryptedPayload = authService.decryptWithSymmetricKey(remoteMessage.getPayload(), chatId, messageTimestamp);
                } else {
                    // Private chat: decrypt with recipient's public key
                    decryptedPayload = authService.decryptWithPrivateKey(remoteMessage.getPayload(), messageTimestamp);
                }
                remoteMessage.setPayload(decryptedPayload);
                messageRepository.saveMessage(remoteMessage);
                return remoteMessage;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing remote message", e);
            return null;
        }
    }

    @Override
    public List<MessageDto> getOfflineMessages(String userId) throws Exception {
        if (currentUserId == null) {
            currentUserId = userId;
        }

        try {
            List<MessageDto> offlineMessages = relayApiClient.getOfflineMessages(userId);
            if (offlineMessages != null) {
                for (MessageDto message : offlineMessages) {
                    // Save to repository
                    messageRepository.saveMessage((TextMessageDto) message);

                    // Send delivery receipt
                    // sendDeliveryReceipt(message);
                }
                return offlineMessages;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get offline messages", e);
        }

        return Collections.emptyList();
    }

    public LiveData<MessageDto> getIncomingMessageLiveData() {
        return incomingMessageLiveData;
    }

    public MutableLiveData<MessageDto> getOutgoingMessageLiveData() {
        return outgoingMessageLiveData;
    }

    @Override
    public boolean resendFailedMessage(String messageId) {
        try {
            MessageDto message = messageRepository.getMessageById(messageId);
            if (message == null) return false;

            // Reset message status
            message.setStatus(MessageState.UNKNOWN);
            messageRepository.updateMessage((TextMessageDto) message);

            // Try to send again
            sendMessage(message);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error resending message", e);
            return false;
        }
    }

    public boolean sendMessageReadEvent(String messageId, String readBy) {
        try {
            if (messageId == null || readBy == null) {
                Log.e(TAG, "Cannot send read event: missing message ID or reader ID");
                return false;
            }

            MessageDto message = messageRepository.getMessageById(messageId);
            if (message == null) {
                Log.e(TAG, "Cannot send read event: message not found in repository: " + messageId);
                return false;
            }

            // Create the message read payload
            JsonObject readPayload = new JsonObject();
            readPayload.addProperty("messageId", messageId);
            readPayload.addProperty("readBy", readBy);
            readPayload.addProperty("sendBy", message.getJid());

            // Create and send the WebSocket event
            WebSocketEvent event = new WebSocketEvent(
                    WebSocketEvent.EventType.MESSAGE_READ,
                    readPayload,
                    readBy
            );

            webSocketClient.sendEvent(event);
            Log.d(TAG, "Message read event sent for message: " + messageId + " read by: " + readBy);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending message read event", e);
            return false;
        }
    }

    @Override
    public boolean setMessageReadByUser(String messageId, String userId) throws Exception {
        MessageDto message = messageRepository.getMessageById(messageId);

        // Send read event to webSocket
        // Create the message read payload
        JsonObject readPayload = new JsonObject();
        readPayload.addProperty("messageId", messageId);
        readPayload.addProperty("sendBy", message.getJid());
        readPayload.addProperty("readBy", userId);

        // Create the WebSocket event
        WebSocketEvent event = new WebSocketEvent(
                WebSocketEvent.EventType.MESSAGE_READ,
                readPayload,
                userId
        );

        // Send the event through the WebSocket client
        webSocketClient.sendEvent(event);
        Log.d(TAG, "Message read event sent: message " + message.getId() +
                " sent by " + message.getJid() + " was read by " + userId);

        return true;
    }

    @Override
    public boolean setMessagesInChatReadByUser(String chatId, String userId) throws Exception {
        try {
            List<TextMessageDto> messages = messageRepository.getMessagesForChat(chatId);
            boolean success = true;

            for (MessageDto message : messages) {
                // Skip messages from the current user or already read
                // or waiting members does not contain userId
                if (message.getJid().equals(userId)
                        || message.getStatus() == MessageState.READ
                        || !message.getWaitingMemebersList().contains(userId)) {
                    continue;
                }

                // Send read event to webSocket
                // Create the message read payload
                JsonObject readPayload = new JsonObject();
                readPayload.addProperty("messageId", message.getId());
                readPayload.addProperty("sendBy", message.getJid());
                readPayload.addProperty("readBy", userId);

                // Create the WebSocket event
                WebSocketEvent event = new WebSocketEvent(
                        WebSocketEvent.EventType.MESSAGE_READ,
                        readPayload,
                        userId
                );

                // Send the event through the WebSocket client
                webSocketClient.sendEvent(event);
                Log.d(TAG, "Message read event sent: message " + message.getId() +
                        " sent by " + message.getJid() + " was read by " + userId);
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error marking all messages as read", e);
            return false;
        }
    }
}