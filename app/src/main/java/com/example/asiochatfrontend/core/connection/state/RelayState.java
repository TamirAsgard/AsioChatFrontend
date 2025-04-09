package com.example.asiochatfrontend.core.connection.state;

import android.util.Log;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.*;

import java.util.Collections;
import java.util.List;

public class RelayState extends ConnectionState {
    private static final String TAG = "RelayState";
    private String currentUserId;

    public RelayState(ConnectionManager connectionManager) {
        super(connectionManager);
        Log.i(TAG, "RelayState initialized");
    }

    @Override
    public ChatDto createPrivateChat(String userId, String otherUserId) throws Exception {
        try {
            ChatDto chat = connectionManager.relayChatService.createPrivateChat(userId, otherUserId);
            Log.i(TAG, "Created private chat " + chat.getId());
            return chat;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create private chat", e);
            throw e;
        }
    }

    @Override
    public ChatDto createGroupChat(String name, List<String> memberIds) throws Exception {
        try {
            ChatDto chat = connectionManager.relayChatService.createGroupChat(name, memberIds, currentUserId);
            Log.i(TAG, "Created group chat " + chat.getId());
            return chat;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create group chat", e);
            throw e;
        }
    }

    @Override
    public List<ChatDto> getChatsForUser(String userId) throws Exception {
        try {
            List<ChatDto> chats = connectionManager.relayChatService.getChatsForUser(userId);
            Log.d(TAG, "Retrieved " + chats.size() + " chats for user " + userId);
            return chats;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get chats for user", e);
            throw e;
        }
    }

    @Override
    public boolean addMemberToGroup(String chatId, String userId) throws Exception {
        try {
            boolean success = connectionManager.relayChatService.addMemberToGroup(chatId, userId);
            Log.i(TAG, "Added member " + userId + " to chat " + chatId + ": " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to add member to group", e);
            throw e;
        }
    }

    @Override
    public boolean removeMemberFromGroup(String chatId, String userId) throws Exception {
        try {
            boolean success = connectionManager.relayChatService.removeMemberFromGroup(chatId, userId);
            Log.i(TAG, "Removed member " + userId + " from chat " + chatId + ": " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to remove member from group", e);
            throw e;
        }
    }

    @Override
    public boolean updateGroupName(String chatId, String newName) throws Exception {
        try {
            boolean success = connectionManager.relayChatService.updateGroupName(chatId, newName);
            Log.i(TAG, "Updated chat " + chatId + " name to " + newName + ": " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to update group name", e);
            throw e;
        }
    }

    @Override
    public MessageDto sendMessage(MessageDto message) throws Exception {
        try {
            MessageDto sentMessage = connectionManager.relayMessageService.sendMessage(message);
            Log.i(TAG, "Sent message " + sentMessage.getId());
            return sentMessage;
        } catch (Exception e) {
            Log.e(TAG, "Failed to send message", e);
            throw e;
        }
    }

    @Override
    public boolean markMessageAsRead(String messageId, String userId) {
        try {
            boolean success = connectionManager.relayMessageService.markMessageAsRead(messageId, userId);
            Log.d(TAG, "Marked message " + messageId + " as read: " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to mark message as read", e);
            return false;
        }
    }

    @Override
    public boolean resendFailedMessage(String messageId) {
        try {
            boolean success = connectionManager.relayMessageService.resendFailedMessage(messageId);
            Log.d(TAG, "Resent failed message " + messageId + ": " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to resend message", e);
            return false;
        }
    }

    @Override
    public boolean updateMessageStatus(String messageId, String status) throws Exception {
        return false;
    }

    @Override
    public List<MessageDto> getMessagesForChat(String chatId) throws Exception {
        try {
            List<MessageDto> messages = connectionManager.relayMessageService.getMessagesForChat(chatId);
            Log.d(TAG, "Retrieved " + messages.size() + " messages for chat " + chatId);
            return messages;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get messages for chat", e);
            throw e;
        }
    }

    @Override
    public List<MessageDto> getOfflineMessages(String userId) throws Exception {
        try {
            List<MessageDto> messages = connectionManager.relayMessageService.getOfflineMessages(userId);
            Log.d(TAG, "Retrieved " + messages.size() + " offline messages for " + userId);
            return messages;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get offline messages", e);
            throw e;
        }
    }

    @Override
    public MediaMessageDto createMediaMessage(MediaMessageDto mediaMessageDto) {
        try {
            MediaMessageDto media = connectionManager.relayMediaService.createMediaMessage(mediaMessageDto);
            Log.i(TAG, "Created media message " + media.getMessage().getId());
            return media;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create media message", e);
            return null;
        }
    }

    @Override
    public MediaMessageDto getMediaMessage(String mediaId) throws Exception {
        try {
            MediaMessageDto media = connectionManager.relayMediaService.getMediaMessage(mediaId);
            Log.d(TAG, "Retrieved media message " + mediaId);
            return media;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get media message", e);
            throw e;
        }
    }

    @Override
    public MediaStreamResultDto getMediaStream(String mediaId) throws Exception {
        try {
            MediaStreamResultDto stream = connectionManager.relayMediaService.getMediaStream(mediaId);
            Log.d(TAG, "Retrieved media stream " + mediaId);
            return stream;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get media stream", e);
            throw e;
        }
    }

    @Override
    public UserDto createUser(UserDto userDto) throws Exception {
        try {
            UserDto user = connectionManager.relayUserService.createUser(userDto);
            Log.i(TAG, "Created user " + user.getId());
            return user;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create user", e);
            throw e;
        }
    }

    @Override
    public void setCurrentUser(String userId) {
        this.currentUserId = userId;
        Log.d(TAG, "Set current user to " + userId);
    }

    @Override
    public UserDto getUserById(String userId) throws Exception {
        try {
            UserDto user = connectionManager.relayUserService.getUserById(userId);
            Log.d(TAG, "Retrieved user " + userId);
            return user;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get user", e);
            throw e;
        }
    }

    @Override
    public UserDto updateUser(String userId, UpdateUserDetailsDto userDetailsDto) throws Exception {
        try {
            UserDto user = connectionManager.relayUserService.updateUser(userId, userDetailsDto);
            Log.i(TAG, "Updated user " + userId);
            return user;
        } catch (Exception e) {
            Log.e(TAG, "Failed to update user", e);
            throw e;
        }
    }

    @Override
    public List<UserDto> observeOnlineUsers() {
        try {
            List<UserDto> users = connectionManager.relayUserService.observeOnlineUsers();
            Log.d(TAG, "Observing " + users.size() + " online users");
            return users;
        } catch (Exception e) {
            Log.e(TAG, "Failed to observe online users", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void refreshOnlineUsers() {
        try {
            connectionManager.relayUserService.refreshOnlineUsers();
            Log.d(TAG, "Refreshed online users");
        } catch (Exception e) {
            Log.e(TAG, "Failed to refresh online users", e);
        }
    }

    @Override
    public List<String> getOnlineUsers() {
        try {
            List<String> users = connectionManager.relayUserService.getOnlineUsers();
            Log.d(TAG, "Retrieved " + users.size() + " online users");
            return users;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get online users", e);
            return Collections.emptyList();
        }
    }
}