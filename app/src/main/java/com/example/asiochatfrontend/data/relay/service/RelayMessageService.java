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

import javax.inject.Inject;

public class RelayMessageService implements MessageService {

    private static final String TAG = "RelayMessageService";

    private final MessageRepository messageRepository;
    private final RelayApiClient relayApiClient;
    private final RelayWebSocketClient webSocketClient;
    private final Gson gson;

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

        webSocketClient.addListener(event -> {
            try {
                switch (event.getType()) {
                    case MESSAGE:
                        Log.d(TAG, "Received MESSAGE event");
                        MessageDto message = gson.fromJson(event.getPayload(), MessageDto.class);
                        messageRepository.saveMessage(message);
                        break;

                    case DELIVERY_RECEIPT:
                    case READ_RECEIPT:
                        Log.d(TAG, "Received receipt event: " + event.getType());
                        MessageDto update = gson.fromJson(event.getPayload(), MessageDto.class);
                        MessageDto existing = messageRepository.getMessageById(update.getId());
                        if (existing != null) {
                            existing.setState(update.getState());
                            existing.setReadBy(update.getReadBy());
                            existing.setDeliveredAt(update.getDeliveredAt());
                            existing.setReadAt(update.getReadAt());
                            messageRepository.saveMessage(existing);
                        }
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing WebSocket event: " + event.getType(), e);
            }
        });
    }

    @Override
    public MessageDto sendMessage(MessageDto messageDto) {
        Log.d(TAG, "Sending message: " + messageDto.getContent());
        messageRepository.saveMessage(messageDto);

        new Thread(() -> {
            try {
                MessageDto sentMessage = relayApiClient.sendMessage(messageDto);
                if (sentMessage != null) {
                    sentMessage.setState(MessageState.SENT);
                    messageRepository.saveMessage(sentMessage);
                    broadcastMessage(sentMessage);
                    Log.d(TAG, "Message sent successfully: " + sentMessage.getId());
                } else {
                    messageDto.setState(MessageState.FAILED);
                    messageRepository.saveMessage(messageDto);
                    Log.w(TAG, "Message sending failed, marked as FAILED");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending message", e);
            }
        }).start();

        return messageDto;
    }

    @Override
    public List<MessageDto> getMessagesForChat(String chatId) {
        Log.d(TAG, "Fetching messages for chatId: " + chatId);
        List<MessageDto> serverMessages = relayApiClient.getMessagesForChat(chatId);
        if (serverMessages != null && !serverMessages.isEmpty()) {
            for (MessageDto message : serverMessages) {
                messageRepository.saveMessage(message);
            }
            Log.d(TAG, "Messages fetched from server: " + serverMessages.size());
            return serverMessages;
        }
        Log.w(TAG, "Server messages empty, falling back to local DB");
        return messageRepository.getMessagesForChat(chatId);
    }

    @Override
    public List<MessageDto> getOfflineMessages(String userId) {
        Log.d(TAG, "Fetching offline messages for userId: " + userId);
        List<MessageDto> offlineMessages = relayApiClient.getOfflineMessages(userId);
        if (offlineMessages != null) {
            for (MessageDto message : offlineMessages) {
                messageRepository.saveMessage(message);
            }
            Log.d(TAG, "Offline messages retrieved: " + offlineMessages.size());
        } else {
            Log.w(TAG, "No offline messages received");
        }
        return offlineMessages != null ? offlineMessages : new ArrayList<>();
    }

    @Override
    public boolean markMessageAsRead(String messageId, String userId) {
        Log.d(TAG, "Marking message as read: " + messageId + " by user: " + userId);
        MessageDto message = messageRepository.getMessageById(messageId);
        if (message == null) {
            Log.w(TAG, "Message not found: " + messageId);
            return false;
        }

        if (message.getReadBy() == null) {
            message.setReadBy(new ArrayList<>());
        }
        if (!message.getReadBy().contains(userId)) {
            message.getReadBy().add(userId);
        }
        if (message.getReadAt() == null) {
            message.setReadAt(new Date());
        }

        message.setState(MessageState.READ);
        messageRepository.saveMessage(message);

        boolean success = relayApiClient.markMessageAsRead(messageId);
        if (success) {
            JsonElement payload = gson.toJsonTree(message);
            WebSocketEvent event = new WebSocketEvent(
                    WebSocketEvent.EventType.READ_RECEIPT,
                    payload,
                    "read-receipt-" + System.currentTimeMillis(),
                    userId
            );
            webSocketClient.sendEvent(event);
            Log.d(TAG, "Read receipt sent for message: " + messageId);
        } else {
            Log.w(TAG, "Failed to notify server of read receipt");
        }

        return success;
    }

    @Override
    public boolean resendFailedMessage(String messageId) {
        Log.d(TAG, "Resending failed message: " + messageId);
        MessageDto message = messageRepository.getMessageById(messageId);
        if (message == null || message.getState() != MessageState.FAILED) {
            Log.w(TAG, "Message not found or not in FAILED state");
            return false;
        }

        message.setState(MessageState.PENDING);
        messageRepository.saveMessage(message);

        new Thread(() -> {
            try {
                MessageDto sentMessage = relayApiClient.sendMessage(message);
                if (sentMessage != null) {
                    sentMessage.setState(MessageState.SENT);
                    messageRepository.saveMessage(sentMessage);
                    broadcastMessage(sentMessage);
                    Log.d(TAG, "Message resent successfully: " + messageId);
                } else {
                    message.setState(MessageState.FAILED);
                    messageRepository.saveMessage(message);
                    Log.w(TAG, "Message resend failed, set back to FAILED");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error resending message", e);
            }
        }).start();

        return true;
    }

    private void broadcastMessage(MessageDto message) {
        JsonElement payload = gson.toJsonTree(message);
        WebSocketEvent event = new WebSocketEvent(
                WebSocketEvent.EventType.MESSAGE,
                payload,
                "message-" + System.currentTimeMillis(),
                message.getSenderId()
        );
        webSocketClient.sendEvent(event);
        Log.d(TAG, "Broadcasted message: " + message.getId());
    }
}