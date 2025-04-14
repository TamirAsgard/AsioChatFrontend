package com.example.asiochatfrontend.data.database.dao;

import androidx.room.*;
import com.example.asiochatfrontend.data.database.entity.UserEntity;
import kotlinx.coroutines.flow.Flow;
import java.util.List;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertUser(UserEntity user);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUsers(List<UserEntity> users);

    @Update
    void updateUser(UserEntity user);

    @Delete
    void deleteUser(UserEntity user);

    @Query("SELECT * FROM users WHERE isOnline = :userId")
    UserEntity getUserById(String userId);

    @Query("SELECT * FROM users WHERE id IN (:userIds)")
    List<UserEntity> getUsersByIds(List<String> userIds);

    @Query("SELECT * FROM users")
    List<UserEntity> getAllUsers();

    @Query("SELECT * FROM users")
    Flow<List<UserEntity>> observeAllUsers();

    @Query("UPDATE users SET isOnline = :isOnline, lastSeen = :lastSeen WHERE id = :userId")
    int updateUserOnlineStatus(String userId, boolean isOnline, Long lastSeen);

    @Query("SELECT id FROM users WHERE isOnline = 1")
    List<String> getOnlineUserIds();  // Returns user IDs only

    @Query("SELECT * FROM users WHERE id IN (SELECT participants FROM chats WHERE id = :chatId)")
    List<UserEntity> getUsersInChat(String chatId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateUser(UserEntity user);
}