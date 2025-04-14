package com.example.asiochatfrontend.data.database.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "users")
public class UserEntity {

    @PrimaryKey
    @NonNull
    public String id;

    public String firstName;
    public String lastName;
    public boolean isOnline;
    public Date lastSeen;     // nullable
    public Date createdAt;
    public Date updatedAt;

    public UserEntity() {
        id = "";
    }
}
