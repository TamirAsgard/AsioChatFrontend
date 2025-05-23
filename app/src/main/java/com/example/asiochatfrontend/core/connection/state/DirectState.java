package com.example.asiochatfrontend.core.connection.state;

import android.util.Log;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.*;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;

import java.util.Collections;
import java.util.List;

public class DirectState extends ConnectionState {
    private static final String TAG = "DirectState";
    private String currentUserId;

    public DirectState(ConnectionManager connectionManager) {
        super(connectionManager);
        Log.i(TAG, "DirectState initialized");
    }

    @Override
    public ChatDto createPrivateChat(String chatId, String userId, String otherUserId) throws Exception {
        try {
            ChatDto chat = connectionManager.directChatService.createPrivateChat(chatId, userId, otherUserId);
            Log.i(TAG, "Created private chat " + chat.getChatId());
            return chat;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create private chat", e);
            throw e;
        }
    }

    @Override
    public ChatDto createGroupChat(String chatId, String name, List<String> memberIds) throws Exception {
        try {
            ChatDto chat = connectionManager.directChatService.createGroupChat(chatId, name, memberIds, currentUserId);
            Log.i(TAG, "Created group chat " + chat.getChatId());
            return chat;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create group chat", e);
            throw e;
        }
    }

    @Override
    public List<ChatDto> getChatsForUser(String userId) throws Exception {
        try {
            List<ChatDto> chats = connectionManager.directChatService.getChatsForUser(userId);
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
            boolean success = connectionManager.directChatService.addMemberToGroup(chatId, userId);
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
            boolean success = connectionManager.directChatService.removeMemberFromGroup(chatId, userId);
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
            boolean success = connectionManager.directChatService.updateGroupName(chatId, newName);
            Log.i(TAG, "Updated chat " + chatId + " name to " + newName + ": " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to update group name", e);
            throw e;
        }
    }

    @Override
    public MessageDto getChatLastMessage(String chatId) {
        return null;
    }

    @Override
    public MessageDto sendMessage(MessageDto message) throws Exception {
        try {
            MessageDto sentMessage = connectionManager.directMessageService.sendMessage(message);
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
            boolean success = connectionManager.directMessageService.markMessageAsRead(messageId, userId);
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
            connectionManager.directMessageService.resendFailedMessage(messageId);
            Log.d(TAG, "Resent failed message " + messageId + ": " + true);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to resend message", e);
            return false;
        }
    }

    @Override
    public int getUnreadMessagesCount(String chatId, String userId) {
        return 0;
    }

    @Override
    public boolean updateMessageStatus(String messageId, String status) throws Exception {
        return false;
    }

    @Override
    public List<TextMessageDto> getMessagesForChat(String chatId) throws Exception {
        try {
            List<TextMessageDto> messages = connectionManager.directMessageService.getMessagesForChat(chatId);
            Log.d(TAG, "Retrieved " + messages.size() + " messages for chat " + chatId);
            return messages;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get messages for chat", e);
            throw e;
        }
    }

    @Override
    public boolean setMessageReadByUser(String messageId, String userId, String readBy) throws Exception {
        try {
            boolean success = connectionManager.directMessageService.setMessageReadByUser(messageId, userId, readBy);
            Log.d(TAG, "Set message " + messageId + " read by user " + userId + ": " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to set message read by user", e);
            throw e;
        }
    }

    @Override
    public boolean setMessagesInChatReadByUser(String chatId, String userId) throws Exception {
        try {
            Log.d(TAG, "Set messages in chat " + chatId + " read by user " + userId);
            return connectionManager.directMessageService.setMessagesInChatReadByUser(chatId, userId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set messages in chat read by user", e);
            throw e;
        }
    }

    @Override
    public List<MessageDto> getOfflineMessages(String userId) throws Exception {
        try {
            List<MessageDto> messages = connectionManager.directMessageService.getOfflineMessages(userId);
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
            MediaMessageDto media = connectionManager.directMediaService.createMediaMessage(mediaMessageDto);
            Log.i(TAG, "Created media message " + media.getId());
            return media;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create media message", e);
            return null;
        }
    }

    @Override
    public MediaMessageDto getMediaMessage(String mediaId) throws Exception {
        try {
            MediaMessageDto media = connectionManager.directMediaService.getMediaMessage(mediaId);
            Log.d(TAG, "Retrieved media message " + mediaId);
            return media;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get media message", e);
            throw e;
        }
    }

    @Override
    public List<MediaMessageDto> getMediaMessageForChat(String chatId) {
        return Collections.emptyList();
    }

    @Override
    public MediaStreamResultDto getMediaStream(String mediaId) {
        try {
            MediaStreamResultDto stream = connectionManager.directMediaService.getMediaStream(mediaId);
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
            UserDto user = connectionManager.directUserService.createUser(userDto);
            Log.i(TAG, "Created user " + user.getJid());
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
            UserDto user = connectionManager.directUserService.getUserById(userId);
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
            UserDto user = connectionManager.directUserService.updateUser(userId, userDetailsDto);
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
            List<UserDto> users = connectionManager.directUserService.observeOnlineUsers();
            Log.d(TAG, "Observing " + users.size() + " online users");
            return users;
        } catch (Exception e) {
            Log.e(TAG, "Failed to observe online users", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void refreshOnlineUsers() {

    }

    @Override
    public List<String> getOnlineUsers() {
        try {
            List<String> users = connectionManager.directUserService.getOnlineUsers();
            Log.d(TAG, "Retrieved " + users.size() + " online users");
            return users;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get online users", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserDto> getContacts() {
        try {
            List<UserDto> users = connectionManager.directUserService.getContacts();
            Log.d(TAG, "Retrieved " + users.size() + " contacts");
            return users;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get contacts", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<MessageDto> sendAllPendingData() {
        return Collections.emptyList();
    }

    @Override
    public List<ChatDto> sendPendingChats() {
        return Collections.emptyList();
    }
}