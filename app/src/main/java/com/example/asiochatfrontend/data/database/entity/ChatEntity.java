package com.example.asiochatfrontend.data.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.asiochatfrontend.core.model.enums.ChatType;

import java.util.Date;
import java.util.List;

@Entity(tableName = "chats")
public class ChatEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String name;
    public ChatType type;
    public List<String> participants;
    public String lastMessageId;
    public int unreadCount;
    public Date createdAt;
    public Date updatedAt;

    public ChatEntity() {
        id = "";
    }
}
