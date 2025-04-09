package com.example.asiochatfrontend.data.direct.network;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DirectWebSocketClient {
    private static final String TAG = "PeerDiscoveryClient";
    private static final int DISCOVERY_PORT = 8888;
    private static final int WEBSOCKET_PORT = 8889;

    private org.java_websocket.client.WebSocketClient client;
    private final String userId;
    private final Context context;
    private final List<PeerConnectionListener> peerListeners = new CopyOnWriteArrayList<>();

    public interface PeerConnectionListener {
        void onPeerDiscovered(String peerId, String peerIp);
        void onMessageReceived(MessageDto message);
        void onPeerStatusChanged(String peerId, boolean isOnline);
    }

    public DirectWebSocketClient(Context context, String userId) {
        this.context = context;
        this.userId = userId;
    }

    public void startDiscovery() {
        // Start UDP broadcast discovery
        new Thread(this::broadcastPresence).start();

        // Start WebSocket server for direct communication
        startWebSocketServer();
    }

    private void broadcastPresence() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);

            // Get all network interfaces
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());

            for (NetworkInterface networkInterface : interfaces) {
                if (networkInterface.isLoopback() || !networkInterface.isUp())
                    continue;

                for (InetAddress addr : Collections.list(networkInterface.getInetAddresses())) {
                    if (addr.isLoopbackAddress() || !(addr instanceof Inet4Address))
                        continue;

                    // Broadcast on all network interfaces
                    String broadcastAddress = getBroadcastAddress(addr);

                    JSONObject discoveryMessage = new JSONObject();
                    discoveryMessage.put("type", "discovery");
                    discoveryMessage.put("userId", userId);
                    discoveryMessage.put("ip", addr.getHostAddress());
                    discoveryMessage.put("port", WEBSOCKET_PORT);

                    byte[] sendData = discoveryMessage.toString().getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(
                            sendData,
                            sendData.length,
                            InetAddress.getByName(broadcastAddress),
                            DISCOVERY_PORT
                    );
                    socket.send(sendPacket);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Discovery broadcast error", e);
        }
    }

    private String getBroadcastAddress(InetAddress addr) {
        // Simple broadcast address calculation
        byte[] ip = addr.getAddress();
        ip[3] = (byte) 255;
        try {
            return InetAddress.getByAddress(ip).getHostAddress();
        } catch (Exception e) {
            return "255.255.255.255";
        }
    }

    private void startWebSocketServer() {
        try {
            // Implement a WebSocket server that listens for incoming connections
            WebSocketServer server = new WebSocketServer(new InetSocketAddress(WEBSOCKET_PORT)) {
                @Override
                public void onOpen(WebSocket conn, ClientHandshake handshake) {
                    // Handle new peer connection
                    String peerIp = conn.getRemoteSocketAddress().getAddress().getHostAddress();
                    notifyPeerDiscovered(peerIp, peerIp);
                }

                @Override
                public void onMessage(WebSocket conn, String message) {
                    // Process incoming messages
                    try {
                        JSONObject json = new JSONObject(message);
                        String type = json.getString("type");

                        if ("message".equals(type)) {
                            MessageDto dto = parseMessageFromJson(json);
                            notifyMessageReceived(dto);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing message", e);
                    }
                }

                @Override
                public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                    // Handle peer disconnection
                    String peerIp = conn.getRemoteSocketAddress().getAddress().getHostAddress();
                    notifyPeerStatusChanged(peerIp, false);
                }

                @Override
                public void onError(WebSocket conn, Exception ex) {
                    Log.e(TAG, "WebSocket server error", ex);
                }

                @Override
                public void onStart() {
                    Log.d(TAG, "WebSocket server started on port " + WEBSOCKET_PORT);
                }
            };
            server.start();
        } catch (Exception e) {
            Log.e(TAG, "Error starting WebSocket server", e);
        }
    }

    public void sendMessageToPeer(String peerIp, MessageDto message) {
        try {
            // Establish WebSocket connection to peer
            WebSocket peerConnection = connectToPeer(peerIp);
            if (peerConnection != null && peerConnection.isOpen()) {
                JSONObject messageJson = convertMessageToJson(message);
                peerConnection.send(messageJson.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending message to peer", e);
        }
    }

    private WebSocket connectToPeer(String peerIp) {
        try {
            URI peerUri = new URI("ws://" + peerIp + ":" + WEBSOCKET_PORT);
            WebSocketClient peerClient = new WebSocketClient(peerUri) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.d(TAG, "Connected to peer: " + peerIp);
                }

                @Override
                public void onMessage(String message) {
                    // Handle incoming messages
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "Disconnected from peer: " + peerIp);
                }

                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "Peer connection error", ex);
                }
            };
            peerClient.connect();
            return peerClient.getConnection();
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to peer", e);
            return null;
        }
    }

    private MessageDto parseMessageFromJson(JSONObject json) throws JSONException {
        return new MessageDto(
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
    }

    private JSONObject convertMessageToJson(MessageDto message) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", "message");
        json.put("id", message.getId());
        json.put("chatId", message.getChatId());
        json.put("senderId", message.getSenderId());
        json.put("content", message.getContent());
        json.put("timestamp", message.getCreatedAt().getTime());
        return json;
    }

    public void addPeerConnectionListener(PeerConnectionListener listener) {
        peerListeners.add(listener);
    }

    public void removePeerConnectionListener(PeerConnectionListener listener) {
        peerListeners.remove(listener);
    }

    private void notifyPeerDiscovered(String peerId, String peerIp) {
        for (PeerConnectionListener listener : peerListeners) {
            listener.onPeerDiscovered(peerId, peerIp);
        }
    }

    private void notifyMessageReceived(MessageDto message) {
        for (PeerConnectionListener listener : peerListeners) {
            listener.onMessageReceived(message);
        }
    }

    private void notifyPeerStatusChanged(String peerId, boolean isOnline) {
        for (PeerConnectionListener listener : peerListeners) {
            listener.onPeerStatusChanged(peerId, isOnline);
        }
    }
}