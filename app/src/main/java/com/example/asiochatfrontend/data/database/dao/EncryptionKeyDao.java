package com.example.asiochatfrontend.data.database.dao;

import androidx.room.*;
import com.example.asiochatfrontend.data.database.entity.EncryptionKeyEntity;

@Dao
public interface EncryptionKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertKey(EncryptionKeyEntity key);

    @Query("SELECT * FROM encryption_keys WHERE userId = :userId")
    EncryptionKeyEntity getKeyForUser(String userId);

    @Query("SELECT * FROM encryption_keys WHERE userId = :userId AND chatId = :chatId")
    EncryptionKeyEntity getKeyForChat(String userId, String chatId);

    @Update
    void updateKey(EncryptionKeyEntity key);

    @Delete
    void deleteKey(EncryptionKeyEntity key);

    @Query("DELETE FROM encryption_keys WHERE userId = :userId")
    void deleteKeysForUser(String userId);
}
