package com.example.asiochatfrontend.core.connection.state;

import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.model.dto.*;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;

import java.util.List;

public abstract class ConnectionState {
    protected final ConnectionManager connectionManager;

    public ConnectionState(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public abstract ChatDto createPrivateChat(String userId, String othersId) throws Exception;
    public abstract ChatDto createGroupChat(String name, List<String> memberIds) throws Exception;
    public abstract List<ChatDto> getChatsForUser(String userId) throws Exception;
    public abstract boolean addMemberToGroup(String chatId, String userId) throws Exception;
    public abstract boolean removeMemberFromGroup(String chatId, String userId) throws Exception;
    public abstract boolean updateGroupName(String chatId, String newName) throws Exception;

    public abstract MessageDto sendMessage(MessageDto message) throws Exception;
    public abstract boolean markMessageAsRead(String messageId, String userId);
    public abstract boolean resendFailedMessage(String messageId);
    public abstract boolean updateMessageStatus(String messageId, String status) throws Exception;
    public abstract List<TextMessageDto> getMessagesForChat(String chatId) throws Exception;
    public abstract boolean setMessageReadByUser(String messageId, String userId) throws Exception;
    public abstract boolean setMessagesInChatReadByUser(String chatId, String userId) throws Exception;
    public abstract List<MessageDto> getOfflineMessages(String userId) throws Exception;
    public abstract MediaMessageDto createMediaMessage(MediaMessageDto mediaMessageDto);
    public abstract MediaMessageDto getMediaMessage(String mediaId) throws Exception;
    public abstract List<MediaMessageDto> getMediaMessageForChat(String chatId);
    public abstract MediaStreamResultDto getMediaStream(String mediaId);

    public abstract UserDto createUser(UserDto userDto) throws Exception;
    public abstract void setCurrentUser(String userId);
    public abstract UserDto getUserById(String userId) throws Exception;
    public abstract UserDto updateUser(String userId, UpdateUserDetailsDto userDetailsDto) throws Exception;
    public abstract List<UserDto> observeOnlineUsers();
    public abstract void refreshOnlineUsers();
    public abstract List<String> getOnlineUsers();
    public abstract List<UserDto> getContacts();
}