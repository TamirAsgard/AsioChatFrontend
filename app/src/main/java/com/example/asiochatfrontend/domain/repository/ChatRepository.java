package com.example.asiochatfrontend.domain.repository;

import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.enums.ChatType;

import java.util.Date;
import java.util.List;

/**
 * Repository interface for handling chat-related data operations.
 */
public interface ChatRepository {

    /**
     * Create a new chat
     *
     * @param chatDto The chat to create
     * @return The created chat with any server-assigned properties
     */
    ChatDto createChat(ChatDto chatDto);

    /**
     * Get a chat by its unique identifier
     *
     * @param chatId The id of the chat to retrieve
     * @return The chat if found, null otherwise
     */
    ChatDto getChatById(String chatId);

    /**
     * Get all chats for a specific user
     *
     * @param userId The id of the user
     * @return List of chats the user is a participant in
     */
    List<ChatDto> getChatsForUser(String userId);

    /**
     * Update an existing chat
     *
     * @param chat The updated chat object
     * @return The updated chat
     */
    ChatDto updateChat(ChatDto chat);

    /**
     * Delete a chat
     *
     * @param chatId The id of the chat to delete
     * @return true if successful, false otherwise
     */
    boolean deleteChat(String chatId);

    /**
     * Find a private chat between two participants
     *
     * @param userOneId First user id
     * @param userTwoId Second user id
     * @return The chat if it exists, null otherwise
     */
    ChatDto findChatByParticipants(String userOneId, String userTwoId);

    /**
     * Update the last message shown in a chat
     *
     * @param chatId The id of the chat to update
     * @param messageId The id of the new last message
     * @return The updated chat
     */
    boolean updateLastMessage(String chatId, String messageId);

    /**
     * Update unread count for a chat
     *
     * @param chatId The id of the chat
     * @param count The new unread count
     * @return The updated chat
     */
    boolean updateUnreadCount(String chatId, int count);

    /**
     * Search for chats by name
     *
     * @param query The search query
     * @return List of matching chats
     */
    List<ChatDto> searchChats(String query);

    boolean updateParticipants(String chatId, List<String> participants);

    boolean updateGroupName(String chatId, String newName);

    List<ChatDto> getPendingChats();

    void updateCreatedAt(String chatId, Date date);
}