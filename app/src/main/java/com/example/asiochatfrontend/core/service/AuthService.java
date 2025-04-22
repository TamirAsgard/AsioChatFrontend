package com.example.asiochatfrontend.core.service;

public interface AuthService {
    boolean registerPublicKey(String userId, String publicKey, long createdAt, int expiresDays);
    String getPublicKey(String userId, long messageTimestamp);
}
