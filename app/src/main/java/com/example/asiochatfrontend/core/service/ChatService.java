package com.example.asiochatfrontend.core.service;

import com.example.asiochatfrontend.core.model.dto.ChatDto;

import java.util.List;

public interface ChatService {
    ChatDto createPrivateChat(String currentUserId, String otherUserId) throws Exception;
    ChatDto createGroupChat(String name, List<String> memberIds, String creatorId) throws Exception;
    List<ChatDto> getChatsForUser(String userId) throws Exception;
    boolean addMemberToGroup(String chatId, String userId) throws Exception;
    boolean removeMemberFromGroup(String chatId, String userId) throws Exception;
    boolean updateGroupName(String chatId, String newName) throws Exception;
    ChatDto getChatById(String chatId);
}
