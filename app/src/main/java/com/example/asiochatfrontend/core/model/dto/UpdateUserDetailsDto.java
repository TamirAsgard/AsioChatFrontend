package com.example.asiochatfrontend.core.model.dto;

public class UpdateUserDetailsDto {
    public String name;
    public String profilePicture;
    public boolean isOnline;

    public UpdateUserDetailsDto(String name, String profilePicture, boolean isOnline) {
        this.name = name;
        this.profilePicture = profilePicture;
        this.isOnline = isOnline;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
}
