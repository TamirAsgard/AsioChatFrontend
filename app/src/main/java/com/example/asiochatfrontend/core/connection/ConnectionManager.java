package com.example.asiochatfrontend.core.connection;

import android.util.Log;
import com.example.asiochatfrontend.core.connection.state.ConnectionState;
import com.example.asiochatfrontend.core.connection.state.DirectState;
import com.example.asiochatfrontend.core.connection.state.RelayState;
import com.example.asiochatfrontend.core.model.dto.*;
import com.example.asiochatfrontend.core.service.*;
import com.example.asiochatfrontend.data.direct.service.*;
import com.example.asiochatfrontend.data.relay.service.*;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class ConnectionManager implements ChatService, MessageService, MediaService, UserService {
    private static final String TAG = "ConnectionManager";

    // Direct services
    private final DirectChatService directChatService;
    private final DirectMessageService directMessageService;
    private final DirectMediaService directMediaService;
    private final DirectUserService directUserService;

    // Relay services
    private final RelayChatService relayChatService;
    private final RelayMessageService relayMessageService;
    private final RelayMediaService relayMediaService;
    private final RelayUserService relayUserService;

    // Connection mode
    private final MutableLiveData<ConnectionMode> _connectionMode = new MutableLiveData<>(ConnectionMode.RELAY);
    public final LiveData<ConnectionMode> connectionMode = _connectionMode;

    // Current state based on mode
    private ConnectionState currentState;
    private String currentUserId;

    @Inject
    public ConnectionManager(
            DirectChatService directChatService,
            DirectMessageService directMessageService,
            DirectMediaService directMediaService,
            DirectUserService directUserService,
            RelayChatService relayChatService,
            RelayMessageService relayMessageService,
            RelayMediaService relayMediaService,
            RelayUserService relayUserService) {

        this.directChatService = directChatService;
        this.directMessageService = directMessageService;
        this.directMediaService = directMediaService;
        this.directUserService = directUserService;
        this.relayChatService = relayChatService;
        this.relayMessageService = relayMessageService;
        this.relayMediaService = relayMediaService;
        this.relayUserService = relayUserService;

        // Default to relay mode
        this.currentState = new RelayState(this);
        Log.i(TAG, "ConnectionManager initialized in RELAY mode");
    }

    public void setConnectionMode(ConnectionMode mode) {
        if (_connectionMode.getValue() == mode) {
            // Already in this mode
            return;
        }

        Log.i(TAG, "Switching connection mode from " + _connectionMode.getValue() + " to " + mode);

        // Update mode and state
        _connectionMode.setValue(mode);

        if (mode == ConnectionMode.DIRECT) {
            currentState = new DirectState(this);

            // When switching to direct mode, make sure to set the current user
            // for the direct services
            if (currentUserId != null) {
                directUserService.setCurrentUser(currentUserId);
            }

        } else {
            currentState = new RelayState(this);

            // When switching to relay mode, make sure to set the current user
            // for the relay services
            if (currentUserId != null) {
                relayUserService.setCurrentUser(currentUserId);
            }
        }
    }

    // ChatService implementations
    @Override
    public ChatDto createPrivateChat(String userId, String otherUserId) throws Exception {
        Log.d(TAG, "Creating private chat between " + userId + " and " + otherUserId);
        return currentState.createPrivateChat(userId, otherUserId);
    }

    @Override
    public ChatDto createGroupChat(String name, List<String> memberIds, String creatorId) throws Exception {
        Log.d(TAG, "Creating group chat " + name + " with " + memberIds.size() + " members");
        return currentState.createGroupChat(name, memberIds);
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

    // MessageService implementations
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
    public List<MessageDto> getMessagesForChat(String chatId) throws Exception {
        Log.d(TAG, "Fetching messages for chat " + chatId);
        return currentState.getMessagesForChat(chatId);
    }

    @Override
    public List<MessageDto> getOfflineMessages(String userId) throws Exception {
        Log.d(TAG, "Fetching offline messages for " + userId);
        return currentState.getOfflineMessages(userId);
    }

    @Override
    public boolean setMessageReadByUser(String messageId, String userId) throws Exception {
        Log.d(TAG, "Set message " + messageId + " read by user " + userId);
        return currentState.setMessageReadByUser(messageId, userId);
    }

    @Override
    public boolean setMessagesInChatReadByUser(String chatId, String userId) throws Exception {
        Log.d(TAG, "Set messages in chat " + chatId + " read by user " + userId);
        return currentState.setMessagesInChatReadByUser(chatId, userId);
    }

    // MediaService implementations
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
    public MediaStreamResultDto getMediaStream(String mediaId) throws Exception {
        Log.d(TAG, "Fetching media stream " + mediaId);
        return currentState.getMediaStream(mediaId);
    }

    // UserService implementations
    @Override
    public void setCurrentUser(String userId) {
        Log.d(TAG, "Setting current user to " + userId);
        this.currentUserId = userId;
        currentState.setCurrentUser(userId);

        // Make sure both service types know about the current user
        if (_connectionMode.getValue() == ConnectionMode.DIRECT) {
            directUserService.setCurrentUser(userId);
        } else {
            relayUserService.setCurrentUser(userId);
        }
    }

    @Override
    public UserDto createUser(UserDto userDto) throws Exception {
        Log.d(TAG, "Creating user " + userDto.getId());
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

    // Helper methods for P2P communication
    public String getPeerIpForUser(String userId) {
        return directUserService.getIpForUserId(userId);
    }
}