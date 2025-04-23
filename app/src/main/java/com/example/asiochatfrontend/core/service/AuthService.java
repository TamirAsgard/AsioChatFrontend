package com.example.asiochatfrontend.core.service;

import com.example.asiochatfrontend.core.model.dto.PublicKeyDto;

public interface AuthService {
    // === Public Key Methods ===
    boolean registerPublicKey(); // Automatically generates a new key if needed
    boolean registerPublicKey(PublicKeyDto keyDto); // Register a specific key
    String getPublicKey(String userId, long messageTimestamp); // Get public key for a specific user and timestamp

    // === Symmetric Key Methods ===
    boolean registerSymmetricKey(String chatId);
    String getSymmetricKey(String chatId, long messageTimestamp);

    // === Public/Private Keys Encryption Methods ===
    String encryptWithPublicKey(String plainText, String recipientId, long timestamp);
    String decryptWithPrivateKey(String encryptedText, long timestamp);

    // === Symmetric Key Encryption Methods ===
    String encryptWithSymmetricKey(String plainText, String chatId, long timestamp);
    String decryptWithSymmetricKey(String combinedIvCiphertext, String chatId, long timestamp);
}
