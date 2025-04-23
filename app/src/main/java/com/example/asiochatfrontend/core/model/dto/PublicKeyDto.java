package com.example.asiochatfrontend.core.model.dto;

public class PublicKeyDto {
    private String jid;
    private String publicKey;
    private long createdAt;
    private long expiresAt;

    public PublicKeyDto(String jid, String publicKey, long createdAt, long expiresAt) {
        this.jid = jid;
        this.publicKey = publicKey;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
}