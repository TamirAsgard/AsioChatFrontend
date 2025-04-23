package com.example.asiochatfrontend.data.database.dao;

import androidx.room.*;
import com.example.asiochatfrontend.data.database.entity.EncryptionKeyEntity;

import java.util.List;

@Dao
public interface EncryptionKeyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertKey(EncryptionKeyEntity key);

    // ======== RSA Public/Private Key Methods (User) ========

    @Query("SELECT * FROM encryption_keys WHERE userId = :userId AND publicKey IS NOT NULL ORDER BY createdAt DESC LIMIT 1")
    EncryptionKeyEntity getLatestPublicKeyForUser(String userId);

    @Query("SELECT * FROM encryption_keys WHERE userId = :userId AND publicKey IS NOT NULL ORDER BY createdAt DESC")
    List<EncryptionKeyEntity> getAllPublicKeysForUser(String userId);

    @Query("SELECT * FROM encryption_keys WHERE userId = :userId AND createdAt <= :timestamp AND publicKey IS NOT NULL ORDER BY createdAt DESC LIMIT 1")
    EncryptionKeyEntity getPublicKeyForTimestamp(String userId, long timestamp);

    @Query("DELETE FROM encryption_keys WHERE userId = :userId AND publicKey IS NOT NULL")
    void deletePublicKeysForUser(String userId);

    @Query("DELETE FROM encryption_keys WHERE userId = :userId AND createdAt < :timestamp AND publicKey IS NOT NULL")
    void deleteOldPublicKeys(String userId, long timestamp);

    // ======== AES Symmetric Key Methods (Chat) ========

    @Query("SELECT * FROM encryption_keys WHERE chatId = :chatId AND symmetricKey IS NOT NULL ORDER BY createdAt DESC LIMIT 1")
    EncryptionKeyEntity getLatestSymmetricKeyForChat(String chatId);

    @Query("SELECT * FROM encryption_keys WHERE chatId = :chatId AND symmetricKey IS NOT NULL ORDER BY createdAt DESC")
    List<EncryptionKeyEntity> getAllSymmetricKeysForChat(String chatId);

    @Query("SELECT * FROM encryption_keys WHERE chatId = :chatId AND createdAt <= :timestamp AND symmetricKey IS NOT NULL ORDER BY createdAt DESC LIMIT 1")
    EncryptionKeyEntity getSymmetricKeyForTimestamp(String chatId, long timestamp);

    @Query("DELETE FROM encryption_keys WHERE chatId = :chatId AND symmetricKey IS NOT NULL")
    void deleteSymmetricKeysForChat(String chatId);

    @Query("DELETE FROM encryption_keys WHERE chatId = :chatId AND createdAt < :timestamp AND symmetricKey IS NOT NULL")
    void deleteOldSymmetricKeys(String chatId, long timestamp);

    // ======== Shared Operations ========

    @Update
    void updateKey(EncryptionKeyEntity key);

    @Delete
    void deleteKey(EncryptionKeyEntity key);
}
