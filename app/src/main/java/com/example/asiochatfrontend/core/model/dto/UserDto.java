package com.example.asiochatfrontend.core.model.dto;

import java.util.Date;

public class UserDto {
    public String id;
    public String name;
    public String profilePicture;
    public String status;
    public String phoneNumber;
    public boolean isOnline;
    public Date lastSeen;
    public Date createdAt;
    public Date updatedAt;

    public UserDto(String id, String name, String profilePicture, String status, boolean isOnline, Date lastSeen, Date createdAt, Date updatedAt) {
        this.id = id;
        this.name = name;
        this.profilePicture = profilePicture;
        this.status = status;
        this.isOnline = isOnline;
        this.lastSeen = lastSeen;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}