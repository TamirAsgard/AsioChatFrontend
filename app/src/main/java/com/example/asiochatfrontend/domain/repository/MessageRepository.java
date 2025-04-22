package com.example.asiochatfrontend.domain.repository;

import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;

import java.util.Date;
import java.util.List;

/**
 * Repository interface for handling message-related data operations.
 */
public interface MessageRepository {

    /**
     * Save a message to the repository
     *
     * @param message The message to save
     * @return The saved message with any updated fields
     */
    TextMessageDto saveMessage(TextMessageDto message);

    /**
     * Get a message by its unique identifier
     *
     * @param messageId The id of the message to retrieve
     * @return The message if found, null otherwise
     */
    TextMessageDto getMessageById(String messageId);

    /**
     * Get all messages for a specific chat
     *
     * @param chatId The id of the chat
     * @return List of messages in the chat, ordered by creation time
     */
    List<TextMessageDto> getMessagesForChat(String chatId);

    /**
     * Get paginated messages for a chat
     *
     * @param chatId The id of the chat
     * @param offset The starting position
     * @param limit Maximum number of messages to return
     * @return List of messages, paginated
     */
    List<TextMessageDto> getMessagesForChat(String chatId, int offset, int limit);

    List<TextMessageDto> getFailedMessages();

    boolean updateMessage(TextMessageDto message);

    /**
     * Update the state of a message
     *
     * @param messageId The id of the message
     * @param state The new state
     * @return The updated message
     */
    boolean updateMessageState(String messageId, MessageState state);

    boolean updateMessageDeliveredAt(String messageId, Date deliveredAt);

    boolean updateMessageReadAt(String messageId, Date readAt);

    TextMessageDto getLastMessageForChat(String chatId);

    int getUnreadMessagesCount(String chatId, String userId);
}