package com.example.asiochatfrontend.data.direct.service;

import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.enums.ChatType;
import com.example.asiochatfrontend.core.service.ChatService;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.data.direct.network.DirectWebSocketClient;
import com.example.asiochatfrontend.domain.repository.ChatRepository;
import com.example.asiochatfrontend.domain.repository.UserRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

public class DirectChatService implements ChatService {
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final DirectWebSocketClient webSocketClient;

    @Inject
    public DirectChatService(ChatRepository chatRepository, UserRepository userRepository, DirectWebSocketClient webSocketClient) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.webSocketClient = webSocketClient;
    }

    @Override
    public ChatDto createPrivateChat(String currentUserId, String otherUserId) throws Exception {
        return null;
    }

    @Override
    public ChatDto createGroupChat(String name, List<String> memberIds, String creatorId) throws Exception {
        return null;
    }

    @Override
    public List<ChatDto> getChatsForUser(String userId) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public boolean addMemberToGroup(String chatId, String userId) throws Exception {
        return false;
    }

    @Override
    public boolean removeMemberFromGroup(String chatId, String userId) throws Exception {
        return false;
    }

    @Override
    public boolean updateGroupName(String chatId, String newName) throws Exception {
        return false;
    }

    @Override
    public ChatDto getChatById(String chatId) {
        return null;
    }
}