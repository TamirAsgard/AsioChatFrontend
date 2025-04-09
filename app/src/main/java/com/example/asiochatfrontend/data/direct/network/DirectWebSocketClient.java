package com.example.asiochatfrontend.data.direct.network;

import android.util.Log;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;

import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DirectWebSocketClient {
    private static final String TAG = "WebSocketClient";

    private org.java_websocket.client.WebSocketClient client; // Note the full package qualification
    private final String userId;
    private final URI serverUri;
    private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<>();
    private final List<PresenceListener> presenceListeners = new CopyOnWriteArrayList<>();

    public interface MessageListener {
        void onMessageReceived(MessageDto message);
    }

    public interface PresenceListener {
        void onPresenceChanged(String userId, boolean isOnline);
    }

    public DirectWebSocketClient(String userId, URI serverUri) {
        this.userId = userId;
        this.serverUri = serverUri;
    }

    public boolean connect() {
        try {
            client = new org.java_websocket.client.WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.i(TAG, "WebSocket connected");
                    sendPresence(true);
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.i(TAG, "WebSocket closed: " + reason);
                    sendPresence(false);
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "WebSocket error", ex);
                }
            };
            client.connect();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to connect WebSocket", e);
            return false;
        }
    }

    public void disconnect() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing WebSocket", e);
            }
        }
    }

    public boolean sendMessage(MessageDto dto) {
        if (client == null || !client.isOpen()) {
            Log.w(TAG, "Cannot send message - WebSocket not connected");
            return false;
        }
        try {
            JSONObject json = new JSONObject()
                    .put("type", "message")
                    .put("id", dto.getId())
                    .put("chatId", dto.getChatId())
                    .put("senderId", dto.getSenderId())
                    .put("content", dto.getContent())
                    .put("timestamp", System.currentTimeMillis());

            client.send(json.toString());
            Log.d(TAG, "Sent message: " + dto.getId());
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to send message", e);
            return false;
        }
    }

    private void sendPresence(boolean isOnline) {
        if (client == null || !client.isOpen()) {
            Log.w(TAG, "Cannot send presence - WebSocket not connected");
            return;
        }
        try {
            JSONObject json = new JSONObject()
                    .put("type", "presence")
                    .put("userId", userId)
                    .put("isOnline", isOnline);

            client.send(json.toString());
            Log.d(TAG, "Sent presence: " + isOnline);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send presence", e);
        }
    }

    private void handleMessage(String message) {
        try {
            JSONObject json = new JSONObject(message);
            String type = json.getString("type");

            switch (type) {
                case "message":
                    MessageDto dto = new MessageDto(
                            json.getString("id"),
                            json.getString("chatId"),
                            json.getString("senderId"),
                            json.getString("content"),
                            null,
                            null,
                            MessageState.DELIVERED,
                            new ArrayList<>(),
                            new Date(json.getLong("timestamp")),
                            null,
                            null
                    );
                    for (MessageListener listener : messageListeners) {
                        listener.onMessageReceived(dto);
                    }
                    break;

                case "presence":
                    String userId = json.getString("userId");
                    boolean isOnline = json.getBoolean("isOnline");
                    for (PresenceListener listener : presenceListeners) {
                        listener.onPresenceChanged(userId, isOnline);
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling message", e);
        }
    }

    public void addMessageListener(MessageListener listener) {
        messageListeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }

    public void addPresenceListener(PresenceListener listener) {
        presenceListeners.add(listener);
    }

    public void removePresenceListener(PresenceListener listener) {
        presenceListeners.remove(listener);
    }

    public boolean isConnected() {
        return client != null && client.isOpen();
    }
}