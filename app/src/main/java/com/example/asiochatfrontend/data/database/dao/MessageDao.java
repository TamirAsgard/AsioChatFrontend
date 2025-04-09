package com.example.asiochatfrontend.data.database.dao;

import androidx.room.*;
import com.example.asiochatfrontend.data.database.entity.MessageEntity;
import kotlinx.coroutines.flow.Flow;
import java.util.List;

@Dao
public interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertMessage(MessageEntity message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(List<MessageEntity> messages);

    @Update
    void updateMessage(MessageEntity message);

    @Delete
    void deleteMessage(MessageEntity message);

    @Query("SELECT * FROM messages WHERE id = :messageId")
    MessageEntity getMessageById(String messageId);

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt ASC")
    List<MessageEntity> getMessagesForChat(String chatId);

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY createdAt DESC")
    Flow<List<MessageEntity>> observeMessagesForChat(String chatId);

    @Query("SELECT * FROM messages WHERE state = 'FAILED'")
    List<MessageEntity> getFailedMessages();

    @Query("SELECT * FROM messages WHERE (content LIKE '%' || :query || '%') AND (:chatId IS NULL OR chatId = :chatId)")
    List<MessageEntity> searchMessages(String query, String chatId);

    @Query("UPDATE messages SET state = :state WHERE id = :messageId")
    int updateMessageState(String messageId, String state);

    @Query("UPDATE messages SET readBy = :readBy WHERE id = :messageId")
    int updateMessageReadBy(String messageId, List<String> readBy);

    @Query("UPDATE messages SET deliveredAt = :deliveredAt WHERE id = :messageId")
    int updateMessageDeliveredAt(String messageId, long deliveredAt);

    @Query("UPDATE messages SET readAt = :readAt WHERE id = :messageId")
    int updateMessageReadAt(String messageId, long readAt);

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND :userId NOT IN (readBy)")
    List<MessageEntity> getUnreadMessages(String chatId, String userId);

    @Query("SELECT * FROM messages WHERE state = :state")
    List<MessageEntity> getMessagesByState(String state);
}