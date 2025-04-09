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
import java.util.concurrent.TimeUnit;

public class RelayWebSocketClient {

    private static final String TAG = "RelayWebSocketClient";

    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private WebSocket webSocket;
    private String serverUrl;
    private String userId;
    private String authToken;
    private boolean isConnected = false;

    private final CopyOnWriteArrayList<RelayWebSocketListener> listeners = new CopyOnWriteArrayList<>();

    public RelayWebSocketClient(String serverUrl, String userId) {
        this.client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();

        this.connect(serverUrl, userId, "");
    }

    public void connect(String serverUrl, String userId, String authToken) {
        this.serverUrl = serverUrl;
        this.userId = userId;
        this.authToken = authToken;

        String url = serverUrl + "/ws?userId=" + userId + "&token=" + authToken;
        Request request = new Request.Builder().url(url).build();

        this.webSocket = client.newWebSocket(request, createListener());
    }

    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Disconnecting");
            webSocket = null;
        }
        isConnected = false;
    }

    public void sendEvent(WebSocketEvent event) {
        if (!isConnected || webSocket == null) {
            Log.e(TAG, "Not connected to WebSocket");
            return;
        }

        try {
            String json = gson.toJson(event);
            webSocket.send(json);
        } catch (Exception e) {
            Log.e(TAG, "Error sending WebSocket event", e);
        }
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
                Log.d(TAG, "WebSocket opened");
                isConnected = true;

                JsonObject payload = new JsonObject();
                payload.addProperty("userId", userId);

                WebSocketEvent connectEvent = new WebSocketEvent(
                        EventType.CONNECT,
                        payload,
                        "connect-" + System.currentTimeMillis(),
                        userId
                );

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
                isConnected = false;

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
            }

            @Override
            public void onFailure(@NonNull WebSocket socket, @NonNull Throwable t, Response response) {
                Log.e(TAG, "WebSocket failure", t);
                isConnected = false;

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

    public interface RelayWebSocketListener {
        void onEvent(WebSocketEvent event);
    }
}
