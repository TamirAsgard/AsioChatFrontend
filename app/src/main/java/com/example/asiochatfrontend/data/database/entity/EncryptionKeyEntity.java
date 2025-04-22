package com.example.asiochatfrontend.data.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "encryption_keys")
public class EncryptionKeyEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String userId;
    public String chatId; // nullable
    public String publicKey;
    public String privateKey; // nullable
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
}
