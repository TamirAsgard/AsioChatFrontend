package com.example.asiochatfrontend.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "encryption_keys")
public class EncryptionKeyEntity {

    @PrimaryKey
    public String id;

    public String userId;
    public String chatId; // nullable
    public String publicKey;
    public String privateKey; // nullable
    public long createdAt;
}
