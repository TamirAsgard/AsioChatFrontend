package com.example.asiochatfrontend.data.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "encryption_keys")
public class EncryptionKeyEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String userId; // nullable
    public String chatId; // nullable
    public String publicKey; // nullable
    public String privateKey; // nullable
    public String symmetricKey; // nullable
    public long createdAt;

    public EncryptionKeyEntity() {
        id = "";
    }

    public EncryptionKeyEntity(long createdAt, String privateKey, String publicKey, String userId, @NonNull String id) {
        this.createdAt = createdAt;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.userId = userId;
        this.id = id;
    }

    public EncryptionKeyEntity(long createdAt, String symmetricKey, String chatId, @NonNull String id) {
        this.createdAt = createdAt;
        this.symmetricKey = symmetricKey;
        this.chatId = chatId;
        this.id = id;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getChatId() {
        return chatId;
    }

    public String getUserId() {
        return userId;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getSymmetricKey() {
        return symmetricKey;
    }

    public void setSymmetricKey(String symmetricKey) {
        this.symmetricKey = symmetricKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }
}
