package com.example.asiochatfrontend.data.relay.network;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.asiochatfrontend.data.relay.model.WebSocketEvent;
import com.example.asiochatfrontend.data.relay.model.WebSocketEvent.EventType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RelayWebSocketClient {

    private static final String TAG = "RelayWebSocketClient";
    private static final int RECONNECT_DELAY_SECONDS = 5;
    private static final int MAX_RECONNECT_ATTEMPTS = 12; // 1 minute total retry time

    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private WebSocket webSocket;
    private String serverUrl;
    private String userId;
    private String authToken;
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private AtomicBoolean isConnecting = new AtomicBoolean(false);
    private int reconnectAttempts = 0;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final CopyOnWriteArrayList<RelayWebSocketListener> listeners = new CopyOnWriteArrayList<>();

    public RelayWebSocketClient(String serverUrl, String userId) {
        this.client = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .build();

        this.serverUrl = serverUrl;
        this.userId = userId;
        this.connect();
    }

    public void connect() {
        connect(null);
    }

    public void connect(String authToken) {
        if (isConnecting.get()) {
            Log.d(TAG, "Connection attempt already in progress");
            return;
        }

        isConnecting.set(true);
        this.authToken = authToken;

        // Construct WebSocket URL
        String wsPrefix = serverUrl.startsWith("https") ? "wss://" : "ws://";
        String baseUrl = serverUrl.replace("https://", "").replace("http://", "");
        String wsUrl = wsPrefix + baseUrl + "/message-broker/live-chat";

        if (authToken != null && !authToken.isEmpty()) {
            wsUrl += "&token=" + authToken;
        }

        Log.d(TAG, "Connecting to WebSocket: " + wsUrl);
        Request request = new Request.Builder().url(wsUrl).build();

        // Close any existing connection
        disconnect();

        // Create new WebSocket connection
        this.webSocket = client.newWebSocket(request, createListener());
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Disconnecting");
            webSocket = null;
        }
        isConnected.set(false);
        isConnecting.set(false);
        reconnectAttempts = 0;
    }

    public void sendEvent(WebSocketEvent event) {
        if (!isConnected.get() || webSocket == null) {
            Log.e(TAG, "Not connected to WebSocket, queuing event for later");
            scheduleReconnect();
            return;
        }

        try {
            String json = gson.toJson(event);
            boolean sent = webSocket.send(json);
            if (!sent) {
                Log.e(TAG, "Failed to send WebSocket event");
                scheduleReconnect();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending WebSocket event", e);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.e(TAG, "Max reconnection attempts reached, giving up");
            isConnecting.set(false);
            notifyError("Max reconnection attempts reached");
            return;
        }

        reconnectAttempts++;
        Log.d(TAG, "Scheduling reconnect attempt " + reconnectAttempts + " in " + RECONNECT_DELAY_SECONDS + " seconds");

        scheduler.schedule(() -> {
            if (!isConnected.get()) {
                Log.d(TAG, "Attempting to reconnect...");
                isConnecting.set(false);
                connect(authToken);
            }
        }, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    public void addListener(RelayWebSocketListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    public void removeListener(RelayWebSocketListener listener) {
        listeners.remove(listener);
    }

    private WebSocketListener createListener() {
        return new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket socket, @NonNull Response response) {
                Log.d(TAG, "WebSocket connected");
                isConnected.set(true);
                isConnecting.set(false);
                reconnectAttempts = 0;

                // Send identification message
                JsonObject payload = new JsonObject();
                payload.addProperty("userId", userId);

                WebSocketEvent connectEvent = new WebSocketEvent(
                        EventType.CONNECT,
                        payload,
                        "connect-" + System.currentTimeMillis(),
                        userId
                );

                // Send directly without using sendEvent to avoid recursive check
                String json = gson.toJson(connectEvent);
                webSocket.send(json);

                // Notify listeners
                dispatchEvent(connectEvent);
            }

            @Override
            public void onMessage(@NonNull WebSocket socket, @NonNull String text) {
                try {
                    WebSocketEvent event = gson.fromJson(text, WebSocketEvent.class);
                    dispatchEvent(event);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing WebSocket message", e);
                }
            }

            @Override
            public void onClosed(@NonNull WebSocket socket, int code, @NonNull String reason) {
                Log.d(TAG, "WebSocket closed: " + reason);
                isConnected.set(false);
                isConnecting.set(false);

                JsonObject payload = new JsonObject();
                payload.addProperty("code", code);
                payload.addProperty("reason", reason);

                WebSocketEvent event = new WebSocketEvent(
                        EventType.DISCONNECT,
                        payload,
                        "disconnect-" + System.currentTimeMillis(),
                        userId
                );

                dispatchEvent(event);

                // Attempt reconnect if not a normal closure
                if (code != 1000) {
                    scheduleReconnect();
                }
            }

            @Override
            public void onFailure(@NonNull WebSocket socket, @NonNull Throwable t, Response response) {
                Log.e(TAG, "WebSocket failure", t);
                isConnected.set(false);

                JsonObject payload = new JsonObject();
                payload.addProperty("message", t.getMessage() != null ? t.getMessage() : "Unknown error");
                payload.addProperty("code", response != null ? response.code() : -1);

                WebSocketEvent event = new WebSocketEvent(
                        EventType.ERROR,
                        payload,
                        "error-" + System.currentTimeMillis(),
                        userId
                );

                dispatchEvent(event);
                scheduleReconnect();
            }
        };
    }

    private void dispatchEvent(WebSocketEvent event) {
        for (RelayWebSocketListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                Log.e(TAG, "Error dispatching WebSocket event", e);
            }
        }
    }

    private void notifyError(String errorMessage) {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", errorMessage);
        payload.addProperty("code", -1);

        WebSocketEvent event = new WebSocketEvent(
                EventType.ERROR,
                payload,
                "error-" + System.currentTimeMillis(),
                userId
        );

        dispatchEvent(event);
    }

    public interface RelayWebSocketListener {
        void onEvent(WebSocketEvent event);
    }

    public boolean isConnected() {
        return isConnected.get();
    }

    public void shutdown() {
        Log.d(TAG, "RelayWebSocketClient.shutdown() called - permanently stopping all connections");

        // Set a flag to indicate we've been shut down
        boolean wasPreviouslyConnecting = isConnecting.getAndSet(false);
        boolean wasPreviouslyConnected = isConnected.getAndSet(false);

        // Cancel any reconnection tasks
        if (scheduler != null && !scheduler.isShutdown()) {
            Log.d(TAG, "Shutting down WebSocket reconnect scheduler");
            try {
                scheduler.shutdownNow();
            } catch (Exception e) {
                Log.e(TAG, "Error shutting down scheduler", e);
            }
        }

        // Close the socket
        if (webSocket != null) {
            try {
                webSocket.close(1000, "Connection mode changed");
                webSocket = null;
            } catch (Exception e) {
                Log.e(TAG, "Error closing WebSocket", e);
            }
        }

        reconnectAttempts = MAX_RECONNECT_ATTEMPTS; // Prevent auto-reconnect

        Log.d(TAG, "RelayWebSocketClient shutdown complete. Was connecting: " +
                wasPreviouslyConnecting + ", Was connected: " + wasPreviouslyConnected);
    }
}