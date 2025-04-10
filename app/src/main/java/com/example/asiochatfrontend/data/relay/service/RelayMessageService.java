package com.example.asiochatfrontend.data.relay.service;

import android.util.Log;

import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.core.service.MessageService;
import com.example.asiochatfrontend.data.relay.model.WebSocketEvent;
import com.example.asiochatfrontend.data.relay.network.RelayApiClient;
import com.example.asiochatfrontend.data.relay.network.RelayWebSocketClient;
import com.example.asiochatfrontend.domain.repository.MessageRepository;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RelayMessageService implements MessageService {

    private static final String TAG = "RelayMessageService";
    private static final int RETRY_DELAY_SECONDS = 5;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final MessageRepository messageRepository;
    private final RelayApiClient relayApiClient;
    private final RelayWebSocketClient webSocketClient;
    private final Gson gson;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, Integer> messageRetryCount = new ConcurrentHashMap<>();
    private String currentUserId;

    @Inject
    public RelayMessageService(
            MessageRepository messageRepository,
            RelayApiClient relayApiClient,
            RelayWebSocketClient webSocketClient,
            Gson gson
    ) {
        this.messageRepository = messageRepository;
        this.relayApiClient = relayApiClient;
        this.webSocketClient = webSocketClient;
        this.gson = gson;

        Log.d(TAG, "Initializing WebSocket listeners");
        setupWebSocketListeners();
    }

    private void setupWebSocketListeners() {
        webSocketClient.addListener(event -> {
            try {
                switch (event.getType()) {
                    case MESSAGE:
                        Log.d(TAG, "Received MESSAGE event: " + event.getEventId());
                        handleIncomingMessage(event);
                        break;

                    case DELIVERY_RECEIPT:
                        Log.d(TAG, "Received DELIVERY_RECEIPT event: " + event.getEventId());
                        handleDeliveryReceipt(event);
                        break;

                    case READ_RECEIPT:
                        Log.d(TAG, "Received READ_RECEIPT event: " + event.getEventId());
                        handleReadReceipt(event);
                        break;

                    case ERROR:
                        Log.e(TAG, "Received ERROR event: " + event.getEventId());
                        if (event.getPayload().isJsonObject() &&
                                event.getPayload().getAsJsonObject().has("messageId")) {
                            String messageId = event.getPayload().getAsJsonObject().get("messageId").getAsString();
                            handleMessageError(messageId);
                        }
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing WebSocket event: " + event.getType(), e);
            }
        });
    }

    private void handleIncomingMessage(WebSocketEvent event) {
        try {
            MessageDto message = gson.fromJson(event.getPayload(), MessageDto.class);
            if (message == null) {
                Log.e(TAG, "Failed to parse message from event");
                return;
            }

            // Don't process our own messages received from WebSocket
            if (currentUserId != null && currentUserId.equals(message.getSenderId())) {
                return;
            }

            // Save to repository
            message.setState(MessageState.DELIVERED);
            message.setDeliveredAt(new Date());
            messageRepository.saveMessage(message);

            // Send delivery receipt
            sendDeliveryReceipt(message);

            Log.d(TAG, "Saved incoming message: " + message.getId());
        } catch (Exception e) {
            Log.e(TAG, "Error handling incoming message", e);
        }
    }

    private void handleDeliveryReceipt(WebSocketEvent event) {
        try {
            MessageDto receipt = gson.fromJson(event.getPayload(), MessageDto.class);
            if (receipt == null || receipt.getId() == null) {
                Log.e(TAG, "Invalid delivery receipt");
                return;
            }

            // Extract original message ID if this is a receipt
            String originalMessageId = receipt.getId();
            if (receipt.getContent() != null && receipt.getContent().startsWith("DELIVERY_RECEIPT:")) {
                originalMessageId = receipt.getContent().substring("DELIVERY_RECEIPT:".length());
            }

            // Update original message as delivered
            MessageDto originalMessage = messageRepository.getMessageById(originalMessageId);
            if (originalMessage != null) {
                originalMessage.setState(MessageState.DELIVERED);
                originalMessage.setDeliveredAt(new Date());
                messageRepository.updateMessage(originalMessage);
                Log.d(TAG, "Updated message as delivered: " + originalMessageId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling delivery receipt", e);
        }
    }

    private void handleReadReceipt(WebSocketEvent event) {
        try {
            MessageDto receipt = gson.fromJson(event.getPayload(), MessageDto.class);
            if (receipt == null || receipt.getId() == null) {
                Log.e(TAG, "Invalid read receipt");
                return;
            }

            // Extract original message ID if this is a receipt
            String originalMessageId = receipt.getId();
            if (receipt.getContent() != null && receipt.getContent().startsWith("READ_RECEIPT:")) {
                originalMessageId = receipt.getContent().substring("READ_RECEIPT:".length());
            }

            // Update original message as read
            MessageDto originalMessage = messageRepository.getMessageById(originalMessageId);
            if (originalMessage != null) {
                originalMessage.setState(MessageState.READ);
                originalMessage.setReadAt(new Date());
                messageRepository.updateMessage(originalMessage);
                Log.d(TAG, "Updated message as read: " + originalMessageId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling read receipt", e);
        }
    }

    private void handleMessageError(String messageId) {
        try {
            MessageDto message = messageRepository.getMessageById(messageId);
            if (message != null) {
                message.setState(MessageState.FAILED);
                messageRepository.updateMessage(message);
                Log.w(TAG, "Message failed to send: " + messageId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling message error", e);
        }
    }

    @Override
    public MessageDto sendMessage(MessageDto message) throws Exception {
        Log.d(TAG, "Sending message: " + message.getId() + " to chat: " + message.getChatId());

        // Set current user ID if not already set
        if (currentUserId == null && message.getSenderId() != null) {
            currentUserId = message.getSenderId();
        }

        // Save message to local repository first with PENDING state
        if (message.getState() == null) {
            message.setState(MessageState.PENDING);
        }
        messageRepository.saveMessage(message);

        // Send via WebSocket first for real-time delivery
        sendViaWebSocket(message);

        // Also send via REST API for reliability
        sendViaRestApi(message);

        return message;
    }

    private void sendViaWebSocket(MessageDto message) {
        if (!webSocketClient.isConnected()) {
            Log.w(TAG, "WebSocket not connected, message will be sent when connection is restored");
            return;
        }

        try {
            JsonElement payload = gson.toJsonTree(message);
            WebSocketEvent event = new WebSocketEvent(
                    WebSocketEvent.EventType.MESSAGE,
                    payload,
                    "message-" + System.currentTimeMillis(),
                    message.getSenderId()
            );
            webSocketClient.sendEvent(event);
            Log.d(TAG, "Message sent via WebSocket: " + message.getId());
        } catch (Exception e) {
            Log.e(TAG, "Error sending message via WebSocket", e);
        }
    }

    private void sendViaRestApi(MessageDto message) {
        // Send via REST API in background
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                MessageDto sentMessage = relayApiClient.sendMessage(message);
                if (sentMessage != null) {
                    message.setState(MessageState.SENT);
                    messageRepository.updateMessage(message);
                    Log.d(TAG, "Message sent via REST API: " + message.getId());
                } else {
                    scheduleRetry(message);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending message via REST API", e);
                scheduleRetry(message);
            }
        });
    }

    private void scheduleRetry(MessageDto message) {
        String messageId = message.getId();
        int attempts = messageRetryCount.getOrDefault(messageId, 0);

        if (attempts >= MAX_RETRY_ATTEMPTS) {
            Log.w(TAG, "Max retry attempts reached for message: " + messageId);
            message.setState(MessageState.FAILED);
            messageRepository.updateMessage(message);
            messageRetryCount.remove(messageId);
            return;
        }

        messageRetryCount.put(messageId, attempts + 1);
        int delay = RETRY_DELAY_SECONDS * (1 << attempts); // Exponential backoff

        Log.d(TAG, "Scheduling retry " + (attempts + 1) + " for message " + messageId + " in " + delay + " seconds");

        scheduler.schedule(() -> {
            try {
                MessageDto latestMessage = messageRepository.getMessageById(messageId);
                if (latestMessage != null &&
                        (latestMessage.getState() == MessageState.PENDING || latestMessage.getState() == MessageState.FAILED)) {
                    Log.d(TAG, "Retrying message: " + messageId);
                    latestMessage.setState(MessageState.PENDING);
                    messageRepository.updateMessage(latestMessage);
                    sendViaRestApi(latestMessage);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error during message retry", e);
            }
        }, delay, TimeUnit.SECONDS);
    }

    private void sendDeliveryReceipt(MessageDto message) {
        if (currentUserId == null) {
            Log.w(TAG, "Current user ID not set, can't send delivery receipt");
            return;
        }

        // Create delivery receipt message
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

        // Send via WebSocket only
        try {
            JsonElement payload = gson.toJsonTree(receipt);
            WebSocketEvent event = new WebSocketEvent(
                    WebSocketEvent.EventType.DELIVERY_RECEIPT,
                    payload,
                    "delivery-" + System.currentTimeMillis(),
                    currentUserId
            );
            webSocketClient.sendEvent(event);
            Log.d(TAG, "Delivery receipt sent for: " + message.getId());
        } catch (Exception e) {
            Log.e(TAG, "Error sending delivery receipt", e);
        }
    }

    @Override
    public boolean markMessageAsRead(String messageId, String userId) throws Exception {
        MessageDto message = messageRepository.getMessageById(messageId);
        if (message == null) {
            Log.w(TAG, "Message not found: " + messageId);
            return false;
        }

        // Update message in local repository
        message.setState(MessageState.READ);
        message.setReadAt(new Date());
        messageRepository.updateMessage(message);

        // Send read receipt via WebSocket
        try {
            // First try REST API
            boolean success = relayApiClient.markMessageAsRead(messageId);

            // Then send via WebSocket for real-time notification
            JsonElement payload = gson.toJsonTree(message);
            WebSocketEvent event = new WebSocketEvent(
                    WebSocketEvent.EventType.READ_RECEIPT,
                    payload,
                    "read-" + System.currentTimeMillis(),
                    userId
            );
            webSocketClient.sendEvent(event);
            Log.d(TAG, "Read receipt sent for: " + messageId);

            return success || webSocketClient.isConnected();
        } catch (Exception e) {
            Log.e(TAG, "Error sending read receipt", e);
            return false;
        }
    }

    @Override
    public boolean resendFailedMessage(String messageId) throws Exception {
        MessageDto message = messageRepository.getMessageById(messageId);
        if (message == null) {
            Log.w(TAG, "Message not found for resend: " + messageId);
            return false;
        }

        // Only resend if message is failed or pending
        if (message.getState() != MessageState.FAILED && message.getState() != MessageState.PENDING) {
            Log.w(TAG, "Message is not in FAILED or PENDING state: " + messageId);
            return false;
        }

        // Reset message state
        message.setState(MessageState.PENDING);
        messageRepository.updateMessage(message);
        messageRetryCount.remove(messageId); // Reset retry count

        // Attempt to resend
        sendMessage(message);
        return true;
    }

    @Override
    public List<MessageDto> getMessagesForChat(String chatId) throws Exception {
        Log.d(TAG, "Getting messages for chat: " + chatId);

        // First try from server
        try {
            List<MessageDto> serverMessages = relayApiClient.getMessagesForChat(chatId);
            if (serverMessages != null && !serverMessages.isEmpty()) {
                // Save messages to local repository
                for (MessageDto message : serverMessages) {
                    messageRepository.saveMessage(message);
                }
                Log.d(TAG, "Retrieved " + serverMessages.size() + " messages from server");
                return serverMessages;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting messages from server", e);
        }

        // Fall back to local repository
        Log.d(TAG, "Falling back to local repository for messages");
        return messageRepository.getMessagesForChat(chatId);
    }

    @Override
    public List<MessageDto> getOfflineMessages(String userId) throws Exception {
        Log.d(TAG, "Getting offline messages for user: " + userId);

        // Set current user ID if not already set
        if (currentUserId == null) {
            currentUserId = userId;
        }

        try {
            List<MessageDto> offlineMessages = relayApiClient.getOfflineMessages(userId);
            if (offlineMessages != null && !offlineMessages.isEmpty()) {
                // Save messages to local repository
                for (MessageDto message : offlineMessages) {
                    messageRepository.saveMessage(message);

                    // Send delivery receipts
                    sendDeliveryReceipt(message);
                }
                Log.d(TAG, "Retrieved " + offlineMessages.size() + " offline messages");
                return offlineMessages;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting offline messages", e);
        }

        return new ArrayList<>();
    }

    @Override
    public boolean setMessageReadByUser(String messageId, String userId) throws Exception {
        return markMessageAsRead(messageId, userId);
    }

    @Override
    public boolean setMessagesInChatReadByUser(String chatId, String userId) throws Exception {
        Log.d(TAG, "Marking all messages in chat as read: " + chatId);

        List<MessageDto> messages = messageRepository.getMessagesForChat(chatId);
        boolean success = true;

        for (MessageDto message : messages) {
            // Skip messages sent by this user
            if (message.getSenderId().equals(userId)) {
                continue;
            }

            // Skip already read messages
            if (message.getState() == MessageState.READ) {
                continue;
            }

            // Mark as read
            boolean result = markMessageAsRead(message.getId(), userId);
            if (!result) {
                success = false;
            }
        }

        return success;
    }
}