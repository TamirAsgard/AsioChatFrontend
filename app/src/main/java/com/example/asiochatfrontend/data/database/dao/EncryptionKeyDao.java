package com.example.asiochatfrontend.data.database.dao;

import androidx.room.*;
import com.example.asiochatfrontend.data.database.entity.EncryptionKeyEntity;

import java.util.List;

@Dao
public interface EncryptionKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertKey(EncryptionKeyEntity key);

    @Query("SELECT * FROM encryption_keys WHERE userId = :userId ORDER BY createdAt DESC LIMIT 1")
    EncryptionKeyEntity getKeyForUser(String userId);

    @Query("SELECT * FROM encryption_keys WHERE userId = :userId AND chatId = :chatId ORDER BY createdAt DESC LIMIT 1")
    EncryptionKeyEntity getKeyForChat(String userId, String chatId);

    @Query("SELECT * FROM encryption_keys WHERE userId = :userId ORDER BY createdAt DESC")
    List<EncryptionKeyEntity> getAllKeysForUser(String userId);

    @Query("SELECT * FROM encryption_keys WHERE userId = :userId AND createdAt <= :timestamp ORDER BY createdAt DESC LIMIT 1")
    EncryptionKeyEntity getKeyForTimestamp(String userId, long timestamp);

    @Update
    void updateKey(EncryptionKeyEntity key);

    @Delete
    void deleteKey(EncryptionKeyEntity key);

    @Query("DELETE FROM encryption_keys WHERE userId = :userId")
    void deleteKeysForUser(String userId);

    @Query("DELETE FROM encryption_keys WHERE userId = :userId AND createdAt < :timestamp")
    void deleteOldKeys(String userId, long timestamp);
}