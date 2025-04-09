package com.example.asiochatfrontend.data.direct.network;

import android.util.Log;
import androidx.annotation.NonNull;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.core.service.MessageService;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class WebSocketMessageListener {
    private static final String TAG = "WebSocketMsgListener";

    private final DirectWebSocketClient directWebSocketClient;
    private final MessageService messageService;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Inject
    public WebSocketMessageListener(@NonNull DirectWebSocketClient directWebSocketClient, @NonNull MessageService messageService) {
        this.directWebSocketClient = directWebSocketClient;
        this.messageService = messageService;
    }

    public void initialize() {
        directWebSocketClient.addMessageListener(message -> executorService.execute(() -> {
            try {
                messageService.sendMessage(message);
                sendDeliveryAcknowledgment(message);
            } catch (Exception e) {
                Log.e(TAG, "Error processing message", e);
            }
        }));
    }

    private void sendDeliveryAcknowledgment(@NonNull MessageDto message) {
        MessageDto ackMessage = new MessageDto(
                "ack-" + message.getId(),
                message.getChatId(),
                message.getSenderId(),
                "DELIVERY_ACK:" + message.getId(),
                null,
                null,
                MessageState.SENT,
                new ArrayList<>(),
                new Date(),
                null,
                null
        );

        boolean success = directWebSocketClient.sendMessage(ackMessage);
        Log.d(TAG, "Acknowledgment for " + message.getId() + " sent: " + success);
    }
}