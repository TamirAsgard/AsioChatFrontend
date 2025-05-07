package com.example.asiochatfrontend.core.connection.state;

import android.util.Log;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.*;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * RelayState handles all operations routed via the relay server.
 * Delegates chat, message, media, and user actions to the Relay services.
 */
public class RelayState extends ConnectionState {
    private static final String TAG = "RelayState";
    private String currentUserId;

    //============================================================================
    // Constructor
    //============================================================================
    public RelayState(ConnectionManager connectionManager) {
        super(connectionManager);
        Log.i(TAG, "RelayState initialized");
    }

    //============================================================================
    // ChatService methods
    //============================================================================
    @Override
    public ChatDto createPrivateChat(String chatId, String userId, String otherUserId) throws Exception {
        try {
            ChatDto chat = connectionManager.relayChatService.createPrivateChat(chatId, userId, otherUserId);
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
            ChatDto chat = connectionManager.relayChatService.createGroupChat(chatId, name, memberIds, currentUserId);
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
            if (!connectionManager.isOnline()) {
                throw new Exception("Can't add member to group while offline");
            }

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
            if (!connectionManager.isOnline()) {
                throw new Exception("Can't remove member from group while offline");
            }

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
            if (!connectionManager.isOnline()) {
                throw new Exception("Can't update group name while offline");
            }

            boolean success = connectionManager.relayChatService.updateGroupName(chatId, newName);
            Log.i(TAG, "Updated chat " + chatId + " name to " + newName + ": " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to update group name", e);
            throw e;
        }
    }

    //============================================================================
    // MessageService methods
    //============================================================================
    @Override
    public MessageDto sendMessage(MessageDto message) throws Exception {
        try {
            MessageDto sent = connectionManager.relayMessageService.sendMessage(message);
            Log.i(TAG, "Sent message " + sent.getId());
            return sent;
        } catch (Exception e) {
            Log.e(TAG, "Failed to send message", e);
            throw e;
        }
    }

    @Override
    public boolean markMessageAsRead(String messageId, String userId) {
        try {
            if (!connectionManager.isOnline()) {
                throw new Exception("Can't mark message as read while offline");
            }

            boolean success = connectionManager.relayMessageService.markMessageAsRead(messageId, userId);
            if (success) Log.d(TAG, "Marked text message " + messageId + " as read: " + success);
            success = connectionManager.relayMediaService.markMessageAsRead(messageId, userId);
            if (success) Log.d(TAG, "Marked media message " + messageId + " as read: " + success);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to mark message as read", e);
            return false;
        }
    }

    @Override
    public boolean resendFailedMessage(String messageId) {
        try {
            if (!connectionManager.isOnline()) {
                throw new Exception("Can't resend message while offline");
            }

            boolean success = connectionManager.relayMessageService.resendFailedMessage(messageId);
            Log.d(TAG, "Resent failed message " + messageId + ": " + success);
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Failed to resend message", e);
            return false;
        }
    }

    @Override
    public int getUnreadMessagesCount(String chatId, String userId) {
        try {
            int textCount = connectionManager.relayMessageService.getUnreadMessagesCount(chatId, userId);
            int mediaCount = connectionManager.relayMediaService.getUnreadMessagesCount(chatId, userId);
            return textCount + mediaCount;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get unread messages count", e);
            return 0;
        }
    }

    @Override
    public boolean updateMessageStatus(String messageId, String status) throws Exception {
        return false;
    }

    @Override
    public List<TextMessageDto> getMessagesForChat(String chatId) throws Exception {
        try {
            List<TextMessageDto> msgs = connectionManager.relayMessageService.getMessagesForChat(chatId);
            Log.d(TAG, "Retrieved " + msgs.size() + " messages for chat " + chatId);
            return msgs;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get messages for chat", e);
            throw e;
        }
    }

    @Override
    public List<MessageDto> getOfflineMessages(String userId) throws Exception {
        try {
            if (!connectionManager.isOnline()) {
                throw new Exception("Can't get offline message while not connected to server");
            }

            List<MessageDto> offline = connectionManager.relayMessageService.getOfflineMessages(userId);
            Log.d(TAG, "Retrieved " + offline.size() + " offline messages for " + userId);
            return offline;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get offline messages", e);
            throw e;
        }
    }

    //============================================================================
    // Read Status Helpers
    //============================================================================
    @Override
    public boolean setMessageReadByUser(String messageId, String userId, String readBy) throws Exception {
        try {
            if (!connectionManager.isOnline()) {
                throw new Exception("Can't set message as read while offline");
            }

            connectionManager.relayMessageService.setMessageReadByUser(messageId, userId, readBy);
            connectionManager.relayMediaService.setMessageReadByUser(messageId, userId, readBy);
            Log.d(TAG, "Set message " + messageId + " read by user " + userId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to set message read by user", e);
            throw e;
        }
    }

    @Override
    public boolean setMessagesInChatReadByUser(String chatId, String userId) throws Exception {
        try {
            if (!connectionManager.isOnline()) {
                throw new Exception("Can't set messages in chat as read while offline");
            }

            connectionManager.relayMessageService.setMessagesInChatReadByUser(chatId, userId);
            connectionManager.relayMediaService.setMessagesInChatReadByUser(chatId, userId);
            Log.d(TAG, "Set messages in chat " + chatId + " read by user " + userId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to set messages in chat read by user", e);
            throw e;
        }
    }

    //============================================================================
    // MediaService methods
    //============================================================================
    @Override
    public MediaMessageDto createMediaMessage(MediaMessageDto mediaMsg) {
        try {
            MediaMessageDto media = connectionManager.relayMediaService.createMediaMessage(mediaMsg);
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
            MediaMessageDto msg = connectionManager.relayMediaService.getMediaMessage(mediaId);
            Log.d(TAG, "Retrieved media message " + mediaId);
            return msg;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get media message", e);
            throw e;
        }
    }

    @Override
    public List<MediaMessageDto> getMediaMessageForChat(String chatId) {
        try {
            List<MediaMessageDto> list = connectionManager.relayMediaService.getMediaMessagesForChat(chatId);
            Log.d(TAG, "Retrieved media messages for chat " + chatId);
            return list;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get media messages", e);
            throw e;
        }
    }

    @Override
    public MediaStreamResultDto getMediaStream(String messageId) {
        try {
            MediaStreamResultDto stream = connectionManager.relayMediaService.getMediaStream(messageId);
            Log.d(TAG, "Retrieved media stream " + messageId);
            return stream;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get media stream", e);
            throw e;
        }
    }

    //============================================================================
    // UserService methods
    //============================================================================
    @Override
    public void setCurrentUser(String userId) {
        this.currentUserId = userId;
        Log.d(TAG, "Set current user to " + userId);
    }

    @Override
    public UserDto createUser(UserDto user) throws Exception {
        try {
            UserDto u = connectionManager.relayUserService.createUser(user);
            Log.i(TAG, "Created user " + user.getJid());
            return u;
        } catch (Exception e) {
            Log.e(TAG, "Failed to create user", e);
            throw e;
        }
    }

    @Override
    public UserDto getUserById(String userId) throws Exception {
        try {
            UserDto u = connectionManager.relayUserService.getUserById(userId);
            Log.d(TAG, "Retrieved user " + userId);
            return u;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get user", e);
            throw e;
        }
    }

    @Override
    public UserDto updateUser(String userId, UpdateUserDetailsDto details) throws Exception {
        try {
            UserDto u = connectionManager.relayUserService.updateUser(userId, details);
            Log.i(TAG, "Updated user " + userId);
            return u;
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
            List<String> list = connectionManager.relayUserService.getOnlineUsers();
            Log.d(TAG, "Retrieved " + list.size() + " online users");
            return list;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get online users", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<UserDto> getContacts() {
        try {
            List<UserDto> contacts = connectionManager.relayUserService.getContacts();
            Log.d(TAG, "Retrieved " + contacts.size() + " contacts");
            return contacts;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get contacts", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<MessageDto> sendAllPendingData() {
        try {
            List<MessageDto> textMessages = connectionManager.relayMessageService.sendPendingMessages();
            List<MessageDto> mediaMessages = connectionManager.relayMediaService.sendPendingMessages();

            // Combine them
            List<MessageDto> allPending = new ArrayList<>(textMessages.size() + mediaMessages.size());
            allPending.addAll(textMessages);
            allPending.addAll(mediaMessages);

            // Now you can send or return `allPending`
            return allPending;
        } catch (Exception e) {
            Log.e(TAG, "Failed to send pending messages " + e.getLocalizedMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<ChatDto> sendPendingChats() {
        try {
            List<ChatDto> chats = connectionManager.relayChatService.sendPendingChats();
            return chats;
        } catch (Exception e) {
            Log.e(TAG, "Failed to send pending chats " + e.getLocalizedMessage());
            return Collections.emptyList();
        }
    }
}
