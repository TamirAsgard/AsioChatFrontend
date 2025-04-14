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

import java.util.*;
import java.util.concurrent.*;

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
        setupWebSocketListeners();
    }

    private void setupWebSocketListeners() {
        webSocketClient.addListener(event -> {
            try {
                switch (event.getType()) {
                    case MESSAGE: handleIncomingMessage(event); break;
                    case DELIVERY_RECEIPT: handleDeliveryReceipt(event); break;
                    case READ_RECEIPT: handleReadReceipt(event); break;
                    case ERROR: {
                        JsonElement payload = event.getPayload();
                        if (payload != null && payload.isJsonObject() && payload.getAsJsonObject().has("messageId")) {
                            handleMessageError(payload.getAsJsonObject().get("messageId").getAsString());
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "WebSocket event processing failed", e);
            }
        });
    }

    private void handleIncomingMessage(WebSocketEvent event) {
        MessageDto message = gson.fromJson(event.getPayload(), MessageDto.class);
        if (message == null || (currentUserId != null && currentUserId.equals(message.getJid()))) return;

        message.setStatus(MessageState.SENT);
        message.setTimestamp(new Date());
        messageRepository.saveMessage(message);
        sendDeliveryReceipt(message);
    }

    private void handleDeliveryReceipt(WebSocketEvent event) {
        MessageDto receipt = gson.fromJson(event.getPayload(), MessageDto.class);
        if (receipt == null || receipt.getId() == null) return;

        String originalId = receipt.getPayload().replace("DELIVERY_RECEIPT:", "");
        MessageDto originalMessage = messageRepository.getMessageById(originalId);
        if (originalMessage != null) {
            originalMessage.setStatus(MessageState.SENT);
            originalMessage.setTimestamp(new Date());
            messageRepository.updateMessage(originalMessage);
        }
    }

    private void handleReadReceipt(WebSocketEvent event) {
        MessageDto receipt = gson.fromJson(event.getPayload(), MessageDto.class);
        if (receipt == null || receipt.getId() == null) return;

        String originalId = receipt.getPayload().replace("READ_RECEIPT:", "");
        MessageDto originalMessage = messageRepository.getMessageById(originalId);
        if (originalMessage != null) {
            originalMessage.setStatus(MessageState.READ);
            originalMessage.setTimestamp(new Date());
            messageRepository.updateMessage(originalMessage);
        }
    }

    private void handleMessageError(String messageId) {
        MessageDto message = messageRepository.getMessageById(messageId);
        if (message != null) {
            message.setStatus(MessageState.UNKNOWN);
            messageRepository.updateMessage(message);
        }
    }

    @Override
    public MessageDto sendMessage(MessageDto message) {
        if (currentUserId == null && message.getJid() != null) currentUserId = message.getJid();
        if (message.getStatus() == null) message.setStatus(MessageState.UNKNOWN);
        messageRepository.saveMessage(message);

        sendViaWebSocket(message);
        sendViaRestApi(message);
        return message;
    }

    private void sendViaWebSocket(MessageDto message) {
        if (!webSocketClient.isConnected()) return;
        try {
            JsonElement payload = gson.toJsonTree(message);
            WebSocketEvent event = new WebSocketEvent(
                    WebSocketEvent.EventType.MESSAGE,
                    payload,
                    "msg-" + System.currentTimeMillis(),
                    message.getJid()
            );
            webSocketClient.sendEvent(event);
        } catch (Exception e) {
            Log.e(TAG, "WebSocket send failed", e);
        }
    }

    private void sendViaRestApi(MessageDto message) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                MessageDto result = relayApiClient.sendMessage(message);
                if (result != null) {
                    message.setStatus(MessageState.SENT);
                    messageRepository.updateMessage(message);
                } else {
                    scheduleRetry(message);
                }
            } catch (Exception e) {
                Log.e(TAG, "REST send failed", e);
                scheduleRetry(message);
            }
        });
    }

    private void scheduleRetry(MessageDto message) {
        String messageId = message.getId();
        int attempts = messageRetryCount.getOrDefault(messageId, 0);
        if (attempts >= MAX_RETRY_ATTEMPTS) {
            message.setStatus(MessageState.UNKNOWN);
            messageRepository.updateMessage(message);
            messageRetryCount.remove(messageId);
            return;
        }

        messageRetryCount.put(messageId, attempts + 1);
        scheduler.schedule(() -> {
            MessageDto latest = messageRepository.getMessageById(messageId);
            if (latest != null && (latest.getStatus() == MessageState.UNKNOWN || latest.getStatus() == MessageState.UNKNOWN)) {
                latest.setStatus(MessageState.UNKNOWN);
                messageRepository.updateMessage(latest);
                sendViaRestApi(latest);
            }
        }, RETRY_DELAY_SECONDS * (1 << attempts), TimeUnit.SECONDS);
    }

    private void sendDeliveryReceipt(MessageDto message) {
        if (currentUserId == null) return;

        MessageDto receipt = new MessageDto(
                message.getId(),
                message.getWaitingMemebersList(),
                MessageState.READ,
                new Date(),
                message.payload,
                currentUserId,
                message.getChatId()
        );

        JsonElement payload = gson.toJsonTree(receipt);
        WebSocketEvent event = new WebSocketEvent(
                WebSocketEvent.EventType.DELIVERY_RECEIPT,
                payload,
                "delivery-" + System.currentTimeMillis(),
                currentUserId
        );
        webSocketClient.sendEvent(event);
    }

    @Override
    public boolean markMessageAsRead(String messageId, String userId) {
        MessageDto message = messageRepository.getMessageById(messageId);
        if (message == null) return false;

        message.setStatus(MessageState.READ);
        message.setTimestamp(new Date());
        messageRepository.updateMessage(message);

        try {
            boolean success = relayApiClient.markMessageAsRead(messageId, currentUserId);
            JsonElement payload = gson.toJsonTree(message);
            WebSocketEvent event = new WebSocketEvent(
                    WebSocketEvent.EventType.READ_RECEIPT,
                    payload,
                    "read-" + System.currentTimeMillis(),
                    userId
            );
            webSocketClient.sendEvent(event);
            return success || webSocketClient.isConnected();
        } catch (Exception e) {
            Log.e(TAG, "Send read receipt error", e);
            return false;
        }
    }

    @Override
    public boolean resendFailedMessage(String messageId) {
        MessageDto message = messageRepository.getMessageById(messageId);
        if (message == null || message.getStatus() == MessageState.SENT) {
            return false;
        }

        message.setStatus(MessageState.SENT);
        messageRepository.updateMessage(message);
        messageRetryCount.remove(messageId);
        sendMessage(message);
        return true;
    }

    @Override
    public List<MessageDto> getMessagesForChat(String chatId) {
        try {
            List<MessageDto> messages = relayApiClient.getMessagesForChat(chatId);
            if (messages != null) {
                for (MessageDto msg : messages) {
                    messageRepository.saveMessage(msg);
                }
                return messages;
            }
        } catch (Exception e) {
            Log.e(TAG, "getMessagesForChat failed", e);
        }
        return messageRepository.getMessagesForChat(chatId);
    }

    @Override
    public List<MessageDto> getOfflineMessages(String userId) {
        if (currentUserId == null) currentUserId = userId;

        try {
            List<MessageDto> offline = relayApiClient.getOfflineMessages(userId);
            if (offline != null) {
                for (MessageDto message : offline) {
                    messageRepository.saveMessage(message);
                    sendDeliveryReceipt(message);
                }
                return offline;
            }
        } catch (Exception e) {
            Log.e(TAG, "getOfflineMessages failed", e);
        }
        return Collections.emptyList();
    }

    @Override
    public boolean setMessageReadByUser(String messageId, String userId) {
        return markMessageAsRead(messageId, userId);
    }

    @Override
    public boolean setMessagesInChatReadByUser(String chatId, String userId) {
        List<MessageDto> messages = messageRepository.getMessagesForChat(chatId);
        boolean success = true;

        for (MessageDto message : messages) {
            if (userId.equals(message.getJid()) || message.getStatus() == MessageState.READ) continue;
            if (!markMessageAsRead(message.getId(), userId)) success = false;
        }

        return success;
    }
}
