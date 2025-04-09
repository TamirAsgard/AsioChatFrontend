package com.example.asiochatfrontend.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import java.util.Date;
import java.util.List;

@Entity(tableName = "messages")
public class MessageEntity {

    @PrimaryKey
    public String id;

    public String chatId;
    public String senderId;
    public String content;         // nullable
    public String mediaId;         // nullable
    public String replyToMessageId; // nullable
    public MessageState state;
    public List<String> readBy;
    public Date createdAt;
    public Date deliveredAt;      // nullable
    public Date readAt;           // nullable
}
