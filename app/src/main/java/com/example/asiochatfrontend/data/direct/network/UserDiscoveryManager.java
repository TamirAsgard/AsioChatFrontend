package com.example.asiochatfrontend.data.direct.network;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Singleton
public class UserDiscoveryManager {
    private static final String TAG = "UserDiscoveryManager";

    private final DirectWebSocketClient directWebSocketClient;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final MutableLiveData<List<String>> _onlineUsers = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<String>> onlineUsers = _onlineUsers;

    @Inject
    public UserDiscoveryManager(DirectWebSocketClient directWebSocketClient) {
        this.directWebSocketClient = directWebSocketClient;
        initialize();
    }

    private void initialize() {
        directWebSocketClient.addPeerConnectionListener(new DirectWebSocketClient.PeerConnectionListener() {
            @Override
            public void onPeerDiscovered(String peerId, String peerIp) {
                updateOnlineUsers(peerId, true);
            }

            @Override
            public void onMessageReceived(MessageDto message) {
                // Handle received messages if needed
            }

            @Override
            public void onPeerStatusChanged(String peerId, boolean isOnline) {
                updateOnlineUsers(peerId, isOnline);
            }
        });

        // Start discovery
        directWebSocketClient.startDiscovery();
    }

    private synchronized void updateOnlineUsers(String userId, boolean isOnline) {
        List<String> current = new ArrayList<>(_onlineUsers.getValue() != null ? _onlineUsers.getValue() : new ArrayList<>());

        if (isOnline && !current.contains(userId)) {
            current.add(userId);
            Log.d(TAG, "User online: " + userId);
        } else if (!isOnline) {
            current.remove(userId);
            Log.d(TAG, "User offline: " + userId);
        }

        _onlineUsers.postValue(current);
    }

    public void refreshOnlineUsers() {
        executorService.execute(() -> {
            List<String> currentUsers = _onlineUsers.getValue();
            if (currentUsers != null) {
                _onlineUsers.postValue(new ArrayList<>(currentUsers));
            }
        });
    }
}