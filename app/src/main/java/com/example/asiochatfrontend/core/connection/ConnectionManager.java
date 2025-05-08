package com.example.asiochatfrontend.core.connection;

import android.util.Log;

import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.connection.state.ConnectionState;
import com.example.asiochatfrontend.core.connection.state.DirectState;
import com.example.asiochatfrontend.core.connection.state.RelayState;
import com.example.asiochatfrontend.core.model.dto.*;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.service.*;
import com.example.asiochatfrontend.data.direct.network.DirectWebSocketClient;
import com.example.asiochatfrontend.data.direct.service.*;
import com.example.asiochatfrontend.data.relay.network.RelayWebSocketClient;
import com.example.asiochatfrontend.data.relay.service.*;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.Collections;
import java.util.List;

/**
 * Manages connection mode (RELAY, DIRECT, OFFLINE) and delegates
 * chat, message, media, and user operations to the appropriate service.
 */
@Singleton
public class ConnectionManager implements ChatService, MessageService, MediaService, UserService {
    private static final String TAG = "ConnectionManager";

    //============================================================================
    // Services: Direct and Relay
    //============================================================================
    public final DirectChatService directChatService;
    public final DirectMessageService directMessageService;
    public final DirectMediaService directMediaService;
    public final DirectUserService directUserService;

    public final RelayChatService relayChatService;
    public final RelayMessageService relayMessageService;
    public final RelayMediaService relayMediaService;
    public final RelayUserService relayUserService;
    public final RelayAuthService relayAuthService;

    /** Callback for incoming WebSocket events */
    public OnWSEventCallback onWSEventCallback;

    //============================================================================
    // Connection Mode & Online Status
    //============================================================================
    /** LiveData tracking the current connection mode */
    public final MutableLiveData<ConnectionMode> _connectionMode = new MutableLiveData<>(ConnectionMode.RELAY);
    public final LiveData<ConnectionMode> connectionMode = _connectionMode;

    /** LiveData tracking the online/offline status */
    private final MutableLiveData<Boolean> onlineStatus = new MutableLiveData<>(false);
    public LiveData<Boolean> getOnlineStatus() { return onlineStatus; }
    public void setOnlineStatus(boolean b) { onlineStatus.postValue(b); }

    //============================================================================
    // Current Delegation State
    //============================================================================
    public ConnectionState currentState;
    public String currentUserId;

    //============================================================================
    // Constructor
    //============================================================================
    @Inject
    public ConnectionManager(
            DirectChatService directChatService,
            DirectMessageService directMessageService,
            DirectMediaService directMediaService,
            DirectUserService directUserService,
            RelayChatService relayChatService,
            RelayMessageService relayMessageService,
            RelayMediaService relayMediaService,
            RelayUserService relayUserService,
            RelayAuthService relayAuthService
    ) {
        this.directChatService = directChatService;
        this.directMessageService = directMessageService;
        this.directMediaService = directMediaService;
        this.directUserService = directUserService;

        this.relayChatService = relayChatService;
        this.relayMessageService = relayMessageService;
        this.relayMediaService = relayMediaService;
        this.relayUserService = relayUserService;
        this.relayAuthService = relayAuthService;

        // Default to Relay mode
        this.currentState = new RelayState(this);
        Log.i(TAG, "ConnectionManager initialized in RELAY mode");
    }

    //============================================================================
    // Mode Switching
    //============================================================================
    /**
     * Switches connection mode and tears down previous mode's services.
     */
    public void setConnectionMode(ConnectionMode mode) {
        // No change in mode
        if (_connectionMode.getValue().equals(mode)) return;

        Log.i(TAG, "Switching connection mode from " + _connectionMode.getValue() + " to " + mode);

        // Tear down current mode
        if (_connectionMode.getValue() == ConnectionMode.RELAY) {
            RelayWebSocketClient ws = ServiceModule.getRelayWebSocketClient();
            if (ws != null) {
                Log.i(TAG, "Shutting down relay WebSocket client");
                ws.shutdown();
            }
        } else if (_connectionMode.getValue() == ConnectionMode.DIRECT) {
            DirectWebSocketClient direct = ServiceModule.getDirectWebSocketClient();
            if (direct != null) direct.stopDiscovery();
        }
        // OFFLINE: no extra teardown

        // Update LiveData and delegation state
        _connectionMode.postValue(mode);
        currentState = (mode == ConnectionMode.DIRECT)
                ? new DirectState(this)
                : new RelayState(this);
    }

    //============================================================================
    // Online Status Helpers
    //============================================================================
    public boolean isOnline() {
        Boolean v = onlineStatus.getValue();
        return v != null && v;
    }

    public void updateOnlineStatus(boolean online) {
        onlineStatus.postValue(online);
    }

    @Override
    public List<MessageDto> sendPendingMessages() {
        Log.d(TAG, "Sending any pending message");
        return currentState.sendAllPendingData();
    }

    //============================================================================
    // ChatService Implementation
    //============================================================================
    @Override
    public ChatDto createPrivateChat(String chatId, String userId, String otherUserId) throws Exception {
        Log.d(TAG, "Creating private chat between " + userId + " and " + otherUserId);
        return currentState.createPrivateChat(chatId, userId, otherUserId);
    }

    @Override
    public ChatDto createGroupChat(String chatId, String name, List<String> memberIds, String creatorId) throws Exception {
        Log.d(TAG, "Creating group chat " + name + " with " + memberIds.size() + " members");
        return currentState.createGroupChat(chatId, name, memberIds);
    }

    @Override
    public List<ChatDto> getChatsForUser(String userId) throws Exception {
        Log.d(TAG, "Fetching chats for user " + userId);
        return currentState.getChatsForUser(userId);
    }

    @Override
    public boolean addMemberToGroup(String chatId, String userId) throws Exception {
        Log.d(TAG, "Adding user " + userId + " to chat " + chatId);
        return currentState.addMemberToGroup(chatId, userId);
    }

    @Override
    public boolean removeMemberFromGroup(String chatId, String userId) throws Exception {
        Log.d(TAG, "Removing user " + userId + " from chat " + chatId);
        return currentState.removeMemberFromGroup(chatId, userId);
    }

    @Override
    public boolean updateGroupName(String chatId, String newName) throws Exception {
        Log.d(TAG, "Updating chat " + chatId + " name to " + newName);
        return currentState.updateGroupName(chatId, newName);
    }

    @Override
    public ChatDto getChatById(String chatId) {
        return null;
    }

    @Override
    public List<ChatDto> sendPendingChats() {
        Log.d(TAG, "Sending any pending message");
        return currentState.sendPendingChats();
    }

    @Override
    public String getChatLastMessage(String chatId) {
        return "";
    }

    public MessageDto getLastMessageForChat(String chatId) {
        return currentState.getChatLastMessage(chatId);
    }

    //============================================================================
    // MessageService Implementation
    //============================================================================
    @Override
    public MessageDto sendMessage(MessageDto message) throws Exception {
        Log.d(TAG, "Sending message to chat " + message.getChatId());
        return currentState.sendMessage(message);
    }

    @Override
    public boolean markMessageAsRead(String messageId, String userId) throws Exception {
        Log.d(TAG, "Marking message " + messageId + " as read for " + userId);
        return currentState.markMessageAsRead(messageId, userId);
    }

    @Override
    public boolean resendFailedMessage(String messageId) throws Exception {
        Log.d(TAG, "Resending failed message " + messageId);
        return currentState.resendFailedMessage(messageId);
    }

    @Override
    public int getUnreadMessagesCount(String chatId, String userId) {
        Log.d(TAG, "Getting unread messages count for chat " + chatId + " for user " + userId);
        return currentState.getUnreadMessagesCount(chatId, userId);
    }

    @Override
    public MessageDto getMessageById(String messageId) {
        return null;
    }

    @Override
    public MessageDto getMediaByMessageId(String lastMessageId) {
        return null;
    }

    @Override
    public List<TextMessageDto> getMessagesForChat(String chatId) throws Exception {
        Log.d(TAG, "Fetching messages for chat " + chatId);
        return currentState.getMessagesForChat(chatId);
    }

    @Override
    public List<MessageDto> getOfflineMessages(String userId) throws Exception {
        Log.d(TAG, "Fetching offline messages for " + userId);
        return currentState.getOfflineMessages(userId);
    }

    @Override
    public boolean setMessageReadByUser(String messageId, String userId, String readBy) throws Exception {
        Log.d(TAG, "Set message " + messageId + " read by user " + userId);
        return currentState.setMessageReadByUser(messageId, userId, readBy);
    }

    @Override
    public boolean setMessagesInChatReadByUser(String chatId, String userId) throws Exception {
        Log.d(TAG, "Set messages in chat " + chatId + " read by user " + userId);
        return currentState.setMessagesInChatReadByUser(chatId, userId);
    }

    //============================================================================
    // MediaService Implementation
    //============================================================================
    @Override
    public MediaMessageDto createMediaMessage(MediaMessageDto mediaMessageDto) throws Exception {
        Log.d(TAG, "Creating media message");
        return currentState.createMediaMessage(mediaMessageDto);
    }

    @Override
    public MediaMessageDto getMediaMessage(String mediaId) throws Exception {
        Log.d(TAG, "Fetching media message " + mediaId);
        return currentState.getMediaMessage(mediaId);
    }

    @Override
    public MediaStreamResultDto getMediaStream(String mediaId) {
        Log.d(TAG, "Fetching media stream " + mediaId);
        return currentState.getMediaStream(mediaId);
    }

    @Override
    public List<MediaMessageDto> getMediaMessagesForChat(String chatId) {
        Log.d(TAG, "Fetching media messages for chat " + chatId);
        return currentState.getMediaMessageForChat(chatId);
    }

    //============================================================================
    // UserService Implementation
    //============================================================================
    @Override
    public void setCurrentUser(String userId) {
        Log.d(TAG, "Setting current user to " + userId);
        this.currentUserId = userId;
        currentState.setCurrentUser(userId);
        if (_connectionMode.getValue() == ConnectionMode.DIRECT) {
            directUserService.setCurrentUser(userId);
        } else {
            relayUserService.setCurrentUser(userId);
        }
    }

    @Override
    public UserDto createUser(UserDto userDto) throws Exception {
        Log.d(TAG, "Creating user " + userDto.getJid());
        return currentState.createUser(userDto);
    }

    @Override
    public UserDto getUserById(String userId) throws Exception {
        Log.d(TAG, "Fetching user " + userId);
        return currentState.getUserById(userId);
    }

    @Override
    public List<UserDto> getContacts() {
        Log.d(TAG, "Get all contacts");
        return currentState.getContacts();
    }

    @Override
    public UserDto updateUser(String userId, UpdateUserDetailsDto userDetailsDto) throws Exception {
        Log.d(TAG, "Updating user " + userId);
        return currentState.updateUser(userId, userDetailsDto);
    }

    @Override
    public List<UserDto> observeOnlineUsers() {
        Log.d(TAG, "Observing online users");
        return currentState.observeOnlineUsers();
    }

    @Override
    public void refreshOnlineUsers() {
        Log.d(TAG, "Refreshing online users");
        currentState.refreshOnlineUsers();
    }

    @Override
    public List<String> getOnlineUsers() {
        Log.d(TAG, "Getting online users list");
        return currentState.getOnlineUsers();
    }

    //============================================================================
    // Helper Methods
    //============================================================================
    /**
     * Returns the peer IP for a given user ID in direct mode.
     */
    public String getPeerIpForUser(String userId) {
        return directUserService.getIpForUserId(userId);
    }

    /**
     * Sets callback for WebSocket events.
     */
    public void setOnWSEventCallback(OnWSEventCallback onWSEventCallback) {
        this.onWSEventCallback = onWSEventCallback;
    }
}
