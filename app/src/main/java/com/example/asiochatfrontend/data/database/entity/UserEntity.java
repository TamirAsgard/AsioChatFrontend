package com.example.asiochatfrontend.data.database.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey
    public String id;

    public String name;
    public String profilePicture; // nullable
    public String status;         // nullable
    public String phoneNumber;
    public boolean isOnline;
    public Date lastSeen;         // nullable
    public Date createdAt;
    public Date updatedAt;
}
