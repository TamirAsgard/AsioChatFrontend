package com.example.asiochatfrontend.data.common.repository;

import android.util.Log;

import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.enums.ChatType;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;
import com.example.asiochatfrontend.data.database.dao.ChatDao;
import com.example.asiochatfrontend.data.database.entity.ChatEntity;
import com.example.asiochatfrontend.domain.repository.ChatRepository;
import com.example.asiochatfrontend.ui.chat.bus.ChatUpdateBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ChatRepositoryImpl implements ChatRepository {
    private static final String TAG = "ChatRepositoryImpl";

    private final ChatDao chatDao;

    @Inject
    public ChatRepositoryImpl(ChatDao chatDao) {
        this.chatDao = chatDao;
    }

    @Override
    public ChatDto createChat(ChatDto chatDto) {
        try {
            // Convert to entity and save
            ChatEntity entity = convertToEntity(chatDto, null, 0);

            // Set creation timestamp if not already set
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(new Date());
            }

            // Save to database
            long id = chatDao.insertChat(entity);

            if (id > 0) {
                // Chat created successfully
                Log.d(TAG, "Chat created: " + chatDto.getChatId());

                // Notify observers about new chat
                ChatUpdateBus.postChatUpdate(chatDto);

                return chatDto;
            } else {
                Log.e(TAG, "Failed to create chat: " + chatDto.getChatId());
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating chat", e);
            return null;
        }
    }

    @Override
    public ChatDto getChatById(String chatId) {
        try {
            ChatEntity entity = chatDao.getChatById(chatId);

            if (entity != null) {
                return mapEntityToDto(entity);
            } else {
                Log.d(TAG, "Chat not found: " + chatId);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting chat by ID", e);
            return null;
        }
    }

    @Override
    public List<ChatDto> getChatsForUser(String userId) {
        return chatDao.getChatsForUser(userId)
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Override
    public ChatDto updateChat(ChatDto chatDto) {
        try {
            ChatEntity existingEntity = chatDao.getChatById(chatDto.getChatId());

            if (existingEntity == null) {
                Log.e("ChatRepository", "Chat not found for ID: " + chatDto.getChatId());
                return null;
            }

            Log.d(TAG, "Chat updated: " + chatDto.getChatId());

            existingEntity.setUpdatedAt(new Date());
            if (!chatDto.getRecipients().equals(existingEntity.getParticipants())) {
                existingEntity.setParticipants(new ArrayList<>(chatDto.getRecipients()));
            }

            if (!chatDto.getChatName().equals(existingEntity.getName())) {
                existingEntity.setName(chatDto.getChatName());
            }

            chatDao.updateChat(existingEntity);
            ChatUpdateBus.postChatUpdate(chatDto);
            return chatDto;
        } catch (Exception e) {
            Log.e(TAG, "Error updating chat", e);
            return null;
        }
    }
    @Override
    public boolean deleteChat(String chatId) {
        try {
            int deleted = chatDao.deleteChatById(chatId);

            if (deleted > 0) {
                Log.d(TAG, "Chat deleted: " + chatId);
                return true;
            } else {
                Log.e(TAG, "Failed to delete chat: " + chatId + " - not found in database");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting chat", e);
            return false;
        }
    }
    @Override
    public ChatDto findChatByParticipants(String userOneId, String userTwoId) {
        return null;
    }

    @Override
    public boolean updateGroupName(String chatId, String newName) {
        return chatDao.updateGroupName(chatId, newName) > 0;
    }

    @Override
    public boolean updateParticipants(String chatId, List<String> participants) {
        return chatDao.updateParticipants(chatId, participants) > 0;
    }

    @Override
    public boolean updateUnreadCount(String chatId, int unreadCount) {
        try {
            int updated = chatDao.updateUnreadCount(chatId, unreadCount);

            if (updated > 0) {
                Log.d(TAG, String.format("Chat unread count updated: %s -> %d for user %s",
                        chatId, unreadCount));

                // Notify about unread count update
                ChatUpdateBus.postUnreadCountUpdate(chatId);

                // Also notify with full chat update for UI refresh
                Executors.newSingleThreadExecutor().execute(() -> {
                    ChatEntity updatedEntity = chatDao.getChatById(chatId);
                    if (updatedEntity != null) {
                        ChatDto updatedChat = mapEntityToDto(updatedEntity);
                        ChatUpdateBus.postChatUpdate(updatedChat);
                    }
                });

                return true;
            } else {
                Log.e(TAG, "Failed to update chat unread count: " + chatId);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating chat unread count", e);
            return false;
        }
    }

    @Override
    public List<ChatDto> searchChats(String query) {
        return Collections.emptyList();
    }

    @Override
    public boolean updateLastMessage(String chatId, String messageId) {
        try {
            int updated = chatDao.updateLastMessageId(chatId, messageId);

            if (updated > 0) {
                Log.d(TAG, "Chat last message updated: " + chatId + " -> " + messageId);

                // Notify UI about the last message update
                // We'll use a background thread to fetch the updated chat
                Executors.newSingleThreadExecutor().execute(() -> {
                    ChatDto updatedChat = getChatById(chatId);
                    if (updatedChat != null) {
                        ChatEntity updatedChatEntity = chatDao.getChatById(updatedChat.getChatId());
                        ChatDto updatedChatDto = mapEntityToDto(updatedChatEntity);
                        ChatUpdateBus.postChatUpdate(updatedChatDto);
                    }
                });

                return true;
            } else {
                Log.e(TAG, "Failed to update chat last message: " + chatId);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating chat last message", e);
            return false;
        }
    }

    private ChatEntity convertToEntity(ChatDto dto, String lastMessageId, int unreadCount) {
        ChatEntity entity = new ChatEntity();
        entity.setId(dto.getChatId());
        entity.setName(dto.getChatName());
        entity.setType(dto.getGroup() ? ChatType.GROUP : ChatType.PRIVATE);
        entity.setLastMessageId(lastMessageId);
        entity.setUnreadCount(unreadCount);
        entity.setParticipants(dto.getRecipients());

        entity.setCreatedAt(new Date());
        entity.setUpdatedAt(new Date());

        return entity;
    }

    private ChatDto mapEntityToDto(ChatEntity entity) {
        return new ChatDto(
                entity.id,
                entity.type == ChatType.GROUP,
                entity.participants,
                entity.name
        );
    }
}
