package com.example.asiochatfrontend.data.direct.network;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.connection.ConnectionMode;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class WebSocketConnectionManager {
    private static final String TAG = "WebSocketConnManager";

    private final DirectWebSocketClient directWebSocketClient;
    private final ConnectionManager connectionManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final LiveData<ConnectionMode> connectionModeLiveData;

    @Inject
    public WebSocketConnectionManager(DirectWebSocketClient directWebSocketClient, ConnectionManager connectionManager) {
        this.directWebSocketClient = directWebSocketClient;
        this.connectionManager = connectionManager;
        this.connectionModeLiveData = connectionManager.connectionMode;
        observeConnectionMode();
    }

    private void observeConnectionMode() {
        connectionModeLiveData.observeForever(new Observer<ConnectionMode>() {
            @Override
            public void onChanged(ConnectionMode mode) {
                if (mode == ConnectionMode.DIRECT) {
                    connectWebSocket();
                } else {
                    disconnectWebSocket();
                }
            }
        });
    }

    public void connectWebSocket(String userId, String serverUri) {
        executor.execute(() -> {
            directWebSocketClient.connect();
            Log.d(TAG, "WebSocket connected for user: " + userId);
        });
    }

    private void connectWebSocket() {
        executor.execute(() -> {
            directWebSocketClient.connect();
            Log.d(TAG, "WebSocket connected via mode switch");
        });
    }

    private void disconnectWebSocket() {
        executor.execute(() -> {
            directWebSocketClient.disconnect();
            Log.d(TAG, "WebSocket disconnected");
        });
    }

    public boolean isConnected() {
        return directWebSocketClient.isConnected();
    }
}