package com.example.asiochatfrontend.data.database.dao;

import androidx.room.*;

import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.data.database.entity.ChatEntity;
import kotlinx.coroutines.flow.Flow;
import java.util.List;

@Dao
public interface ChatDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertChat(ChatEntity chat);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChats(List<ChatEntity> chats);

    @Update
    void updateChat(ChatEntity chat);

    @Delete
    void deleteChat(ChatEntity chat);

    @Query("SELECT * FROM chats WHERE id = :chatId")
    ChatEntity getChatById(String chatId);

    @Query("SELECT * FROM chats WHERE participants LIKE '%\"' || :userId || '\"%'")
    List<ChatEntity> getChatsForUser(String userId);

    @Query("SELECT * FROM chats WHERE :userId IN (participants)")
    Flow<List<ChatEntity>> observeChatsForUser(String userId);

    @Query("UPDATE chats SET name = :newName WHERE id = :chatId")
    int updateGroupName(String chatId, String newName);

    @Query("UPDATE chats SET participants = :participants WHERE id = :chatId")
    int updateParticipants(String chatId, List<String> participants);

    @Query("UPDATE chats SET unreadCount = :unreadCount WHERE id = :chatId")
    int updateUnreadCount(String chatId, int unreadCount);

    @Query("UPDATE chats SET lastMessageId = :lastMessageId WHERE id = :chatId")
    int updateLastMessageId(String chatId, String lastMessageId);

    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    Flow<List<ChatEntity>> observeAllChats();

    @Query("SELECT * FROM chats WHERE (participants LIKE '%' || :userOneId || '%' AND participants LIKE '%' || :userTwoId || '%') AND type = 'PRIVATE' LIMIT 1")
    ChatEntity findPrivateChatBetween(String userOneId, String userTwoId);

    @Query("DELETE FROM chats WHERE id = :chatId")
    int deleteChatById(String chatId);

    @Query("SELECT * FROM chats WHERE createdAt IS NULL")
    List<ChatEntity> getPendingChats();
}