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

@Singleton
public class WebSocketMessageListener {
    private static final String TAG = "WebSocketMsgListener";

    private final DirectWebSocketClient directWebSocketClient;
    private final MessageService messageService;

    @Inject
    public WebSocketMessageListener(
            DirectWebSocketClient peerDiscoveryClient,
            MessageService messageService
    ) {
        this.directWebSocketClient = peerDiscoveryClient;
        this.messageService = messageService;
    }

    public void initialize() {
        directWebSocketClient.addPeerConnectionListener(new DirectWebSocketClient.PeerConnectionListener() {
            @Override
            public void onMessageReceived(MessageDto message) {
                try {
                    messageService.sendMessage(message);
                    sendDeliveryAcknowledgment(message);
                } catch (Exception e) {
                    Log.e(TAG, "Error processing message", e);
                }
            }

            @Override
            public void onPeerDiscovered(String peerId, String peerIp) {
                // Handle peer discovery if needed
            }

            @Override
            public void onPeerStatusChanged(String peerId, boolean isOnline) {
                // Handle peer status changes if needed
            }
        });
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
    }
}