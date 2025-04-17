package com.example.asiochatfrontend.domain.repository;

import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.data.database.entity.MessageEntity;

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
    MessageDto saveMessage(MessageDto message);

    /**
     * Get a message by its unique identifier
     *
     * @param messageId The id of the message to retrieve
     * @return The message if found, null otherwise
     */
    MessageDto getMessageById(String messageId);

    /**
     * Get all messages for a specific chat
     *
     * @param chatId The id of the chat
     * @return List of messages in the chat, ordered by creation time
     */
    List<MessageDto> getMessagesForChat(String chatId);

    /**
     * Get paginated messages for a chat
     *
     * @param chatId The id of the chat
     * @param offset The starting position
     * @param limit Maximum number of messages to return
     * @return List of messages, paginated
     */
    List<MessageDto> getMessagesForChat(String chatId, int offset, int limit);

    List<MessageDto> getFailedMessages();

    boolean updateMessage(MessageDto message);

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

    MessageDto getLastMessageForChat(String chatId);

    int getUnreadMessagesCount(String chatId, String userId);
}