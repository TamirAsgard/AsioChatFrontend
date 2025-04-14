package com.example.asiochatfrontend.data.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.asiochatfrontend.core.model.enums.MediaType;

import java.util.Date;

@Entity(tableName = "media")
public class MediaEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public MediaType type;
    public String messageId;
    public String chatId;
    public String senderId;
    public String localUri;
    public String remoteUri;
    public String fileName;
    public long fileSize;
    public String mimeType;
    public Long duration;         // nullable
    public String thumbnailUri;   // nullable
    public Boolean isProcessed;
    public Date createdAt;
    public Date uploadedAt;       // nullable

    public MediaEntity() {
        id = "";
    }
}
