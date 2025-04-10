package com.example.asiochatfrontend.core.model.dto;

import java.util.Date;

public class UserDto {
    public String jid;
    public UserDetailsDto userDetailsDto;
    public Date createdAt;
    public Date UpdatedAt;

    public UserDto(UserDetailsDto userDetailsDto, String jid, Date createdAt, Date updatedAt) {
        this.userDetailsDto = userDetailsDto;
        this.jid = jid;
        this.createdAt = createdAt;
        UpdatedAt = updatedAt;
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public Date getUpdatedAt() {
        return UpdatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        UpdatedAt = updatedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public UserDetailsDto getUserDetailsDto() {
        return userDetailsDto;
    }

    public void setUserDetailsDto(UserDetailsDto userDetailsDto) {
        this.userDetailsDto = userDetailsDto;
    }
}

