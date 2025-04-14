package com.example.asiochatfrontend.core.model.dto;

public class AuthRequestCredentialsDto {
    public String Jid;

    public AuthRequestCredentialsDto(String jid) {
        this.Jid = jid;
    }

    public String getJid() {
        return Jid;
    }

    public void setJid(String jid) {
        this.Jid = jid;
    }
}
