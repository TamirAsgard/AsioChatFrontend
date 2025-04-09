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
import java.util.Collections;
import java.util.List;

public class ConnectionManager implements ChatService, MessageService, MediaService, UserService {
    private static final String TAG = "ConnectionManager";

    public final DirectChatService directChatService;
    public final DirectMessageService directMessageService;
    public final DirectMediaService directMediaService;
    public final DirectUserService directUserService;
    public final RelayChatService relayChatService;
    public final RelayMessageService relayMessageService;
    public final RelayMediaService relayMediaService;
    public final RelayUserService relayUserService;

    private final MutableLiveData<ConnectionMode> _connectionMode = new MutableLiveData<>(ConnectionMode.RELAY);
    public final LiveData<ConnectionMode> connectionMode = _connectionMode;

    private ConnectionState currentState;

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
        this.currentState = new RelayState(this);
        Log.i(TAG, "Initialized in RELAY mode");
    }

    public void setConnectionMode(ConnectionMode mode) {
        Log.i(TAG, "Switching connection mode from " + _connectionMode.getValue() + " to " + mode);
        _connectionMode.setValue(mode);
        currentState = mode == ConnectionMode.DIRECT ? new DirectState(this) : new RelayState(this);
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
        currentState.resendFailedMessage(messageId);
        return true;
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

    @Override
    public void setCurrentUser(String userId) {
        Log.d(TAG, "Setting current user to " + userId);
        currentState.setCurrentUser(userId);
    }

    // UserService implementations
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
        Log.w(TAG, "Get contacts not implemented");
        return Collections.emptyList();
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
        // currentState.refreshOnlineUsers();
    }

    @Override
    public List<String> getOnlineUsers() {
        Log.d(TAG, "Getting online users list");
        return Collections.emptyList();
    }

    // Helper methods
    public ChatDto directCreatePrivateChat(String userId, String otherUserId) {
        return directChatService.createPrivateChat(userId, otherUserId);
    }

    public ChatDto relayCreatePrivateChat(String userId, String otherUserId) {
        return relayChatService.createPrivateChat(userId, otherUserId);
    }
}