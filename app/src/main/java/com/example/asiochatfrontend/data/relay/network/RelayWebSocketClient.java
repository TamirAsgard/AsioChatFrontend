package com.example.asiochatfrontend.data.relay.network;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.asiochatfrontend.data.relay.model.WebSocketEvent;
import com.example.asiochatfrontend.data.relay.model.WebSocketEvent.EventType;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * Manages a persistent WebSocket connection to the relay server.
 * Automatically attempts reconnection on failure.
 */
public class RelayWebSocketClient {

    //==============================
    // Constants
    //==============================
    private static final String TAG = "RelayWebSocketClient";
    private static final int RECONNECT_DELAY_SECONDS = 5;

    //==============================
    // Dependencies & State
    //==============================
    private final OkHttpClient client;
    private final Gson gson = new Gson();
    private WebSocket webSocket;
    private final String serverUrl;
    private final String userId;
    private String authToken;

    //==============================
    // Connection Flags & Executors
    //==============================
    private final AtomicBoolean isConnected   = new AtomicBoolean(false);
    private final AtomicBoolean isConnecting  = new AtomicBoolean(false);
    private int reconnectAttempts              = 0;
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    //==============================
    // Event Listeners
    //==============================
    private final CopyOnWriteArrayList<RelayWebSocketListener> listeners =
            new CopyOnWriteArrayList<>();

    //==============================
    // Construction
    //==============================
    public RelayWebSocketClient(String serverUrl, String userId) {
        this.serverUrl = serverUrl;
        this.userId    = userId;

        // build OkHttp client with a ping interval
        this.client = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .pingInterval(30, TimeUnit.SECONDS)
                .build();

        // Start initial connection
        connect();
    }

    //==============================
    // Public API
    //==============================

    /** (Re)connect without token */
    public void connect() {
        connect(null);
    }

    /**
     * Connect (or reconnect) with optional auth token.
     * Skips if a connection attempt is already in flight.
     */
    public void connect(String authToken) {
        if (isConnecting.get()) {
            Log.d(TAG, "Connection attempt already in progress");
            return;
        }
        isConnecting.set(true);
        this.authToken = authToken;

        // build WebSocket URL
        String wsPrefix = serverUrl.startsWith("https") ? "wss://" : "ws://";
        String baseUrl  = serverUrl.replace("https://", "").replace("http://", "");
        String wsUrl    = wsPrefix + baseUrl + "/message-broker/live-chat";
        if (authToken != null && !authToken.isEmpty()) {
            wsUrl += "&token=" + authToken;
        }

        Log.d(TAG, "Connecting to WebSocket: " + wsUrl);
        Request request = new Request.Builder().url(wsUrl).build();

        // tear down any existing
        disconnect();

        // open new WebSocket
        this.webSocket = client.newWebSocket(request, createListener());
        isConnected.set(true);
    }

    /** Gracefully close and reset state */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Disconnecting");
            webSocket = null;
        }
        isConnected.set(false);
        isConnecting.set(false);
        reconnectAttempts = 0;
    }

    /**
     * Send a WebSocketEvent if connected; otherwise log and drop it.
     */
    public void sendEvent(WebSocketEvent event) {
        if (!isConnected.get() || webSocket == null) {
            // Fallback, try again
            connect();
            if (!isConnected.get()) {
                Log.e(TAG, "Not connected; event dropped");
                return;
            }
        }

        try {
            boolean sent = webSocket.send(gson.toJson(event));
            if (!sent) Log.e(TAG, "Failed to send event");
        } catch (Exception e) {
            Log.e(TAG, "Error sending WebSocket event", e);
        }
    }

    /**
     * Schedule an automatic reconnect after a fixed delay,
     * unless we're already connected.
     */
    public void scheduleReconnect() {
        reconnectAttempts++;
        Log.d(TAG, "Scheduling reconnect #" + reconnectAttempts
                + " in " + RECONNECT_DELAY_SECONDS + "s");

        try {
            scheduler.schedule(() -> {
                if (!isConnected.get()) {
                    Log.d(TAG, "Performing scheduled reconnect...");
                    isConnecting.set(false);
                    connect(authToken);
                }
            }, RECONNECT_DELAY_SECONDS, TimeUnit.SECONDS);
        } catch (RejectedExecutionException ignored) {
            // executor shut down: ignore
        }
    }

    /** Add a listener for incoming WebSocketEvent callbacks */
    public void addListener(RelayWebSocketListener listener) {
        listeners.addIfAbsent(listener);
    }

    /** Remove a previously added listener */
    public void removeListener(RelayWebSocketListener listener) {
        listeners.remove(listener);
    }

    /** Returns true if currently connected */
    public boolean isConnected() {
        return isConnected.get();
    }

    /** Cleanly tear down everything and prevent reconnection */
    public void shutdown() {
        Log.d(TAG, "Shutdown called; stopping all reconnection attempts");
        isConnected.set(false);
        isConnecting.set(false);
        reconnectAttempts = Integer.MAX_VALUE;

        scheduler.shutdownNow();
        if (webSocket != null) {
            webSocket.close(1000, "Shutdown");
            webSocket = null;
        }
    }

    //==============================
    // Internal WebSocket Listener
    //==============================
    private WebSocketListener createListener() {
        return new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket socket, @NonNull Response response) {
                Log.d(TAG, "WebSocket connected");
                isConnected.set(true);
                isConnecting.set(false);
                reconnectAttempts = 0;

                // announce ourselves
                JsonObject payload = new JsonObject();
                payload.addProperty("jid", userId);
                WebSocketEvent connectEvent = new WebSocketEvent(EventType.CONNECT, payload, userId);
                socket.send(gson.toJson(connectEvent));

                // notify observers
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
            }

            @Override
            public void onFailure(@NonNull WebSocket socket, @NonNull Throwable t, Response response) {
                Log.e(TAG, "WebSocket failure", t);
                isConnected.set(false);
                scheduleReconnect();
            }
        };
    }

    /** Dispatches a WebSocketEvent to all registered listeners */
    private void dispatchEvent(WebSocketEvent event) {
        for (RelayWebSocketListener l : listeners) {
            try {
                l.onEvent(event);
            } catch (Exception e) {
                Log.e(TAG, "Listener error", e);
            }
        }
    }

    //==============================
    // Listener interface
    //==============================
    public interface RelayWebSocketListener {
        void onEvent(WebSocketEvent event);
    }
}
