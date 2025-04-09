package com.example.asiochatfrontend.data.direct.network;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.connection.ConnectionMode;
import com.example.asiochatfrontend.core.model.dto.MessageDto;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;

@Singleton
public class WebSocketConnectionManager {
    private static final String TAG = "WebSocketConnManager";

    private final DirectWebSocketClient directWebSocketClient;
    private final ConnectionManager connectionManager;
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final LiveData<ConnectionMode> connectionModeLiveData;
    private String currentUserId;

    @Inject
    public WebSocketConnectionManager(
            Context context,
            DirectWebSocketClient directWebSocketClient,
            ConnectionManager connectionManager
    ) {
        this.context = context;
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
                    connectPeerNetwork();
                } else {
                    disconnectPeerNetwork();
                }
            }
        });
    }

    public void connectPeerNetwork() {
        if (currentUserId == null) {
            Log.e(TAG, "Cannot connect - user ID not set");
            return;
        }

        executor.execute(() -> {
            // Initialize peer discovery with current user ID
            directWebSocketClient.startDiscovery();
            Log.d(TAG, "Peer network started for user: " + currentUserId);
        });
    }

    public void connectPeerNetwork(String userId) {
        this.currentUserId = userId;
        connectPeerNetwork();
    }

    private void disconnectPeerNetwork() {
        executor.execute(() -> {
            // Implement any necessary cleanup or disconnection logic
            Log.d(TAG, "Peer network stopped");
        });
    }

    public void sendMessageToPeer(String peerIp, MessageDto message) {
        if (!isConnected()) {
            Log.w(TAG, "Cannot send message - peer network not connected");
            return;
        }

        directWebSocketClient.sendMessageToPeer(peerIp, message);
    }

    public boolean isConnected() {
        // Implement your connection check logic
        return currentUserId != null;
    }

    public void addPeerConnectionListener(DirectWebSocketClient.PeerConnectionListener listener) {
        directWebSocketClient.addPeerConnectionListener(listener);
    }

    public void removePeerConnectionListener(DirectWebSocketClient.PeerConnectionListener listener) {
        directWebSocketClient.removePeerConnectionListener(listener);
    }
}