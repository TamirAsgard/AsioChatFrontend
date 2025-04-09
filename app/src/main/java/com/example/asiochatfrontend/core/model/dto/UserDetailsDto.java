package com.example.asiochatfrontend.core.model.dto;

public class UserDetailsDto {
    public String name;
    public String profilePicture;
    public String status;

    public UserDetailsDto(String name, String profilePicture, String status) {
        this.name = name;
        this.profilePicture = profilePicture;
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}