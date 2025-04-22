package com.example.asiochatfrontend.core.security;

import android.util.Log;

import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.data.database.AppDatabase;
import com.example.asiochatfrontend.data.database.dao.EncryptionKeyDao;
import com.example.asiochatfrontend.data.database.entity.EncryptionKeyEntity;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;

import java.security.KeyPair;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages encryption key rotation and message encryption/decryption
 */
public class EncryptionManager {
    private static final String TAG = "EncryptionManager";
    private static final long KEY_VALIDITY_DAYS = 7; // Key validity period in days

    private final EncryptionService encryptionService;
    private final EncryptionKeyDao encryptionKeyDao;
    private String currentUserId;

    public EncryptionManager(EncryptionService encryptionService, EncryptionKeyDao encryptionKeyDao, String currentUserId) {
        this.encryptionService = encryptionService;
        this.encryptionKeyDao = encryptionKeyDao;
        this.currentUserId = currentUserId;
    }

    /**
     * Generates a new key pair if the current one has expired or doesn't exist.
     * Returns the public key that should be sent to the backend.
     */
    public String ensureCurrentKeyPair() {
        try {
            // Check if we have a valid key pair
            EncryptionKeyEntity currentKey = encryptionKeyDao.getKeyForUser(currentUserId);

            if (isKeyValid(currentKey)) {
                // Key is still valid, return its public key
                return currentKey.publicKey;
            }

            // Generate new key pair
            return generateNewKeyPair();

        } catch (Exception e) {
            Log.e(TAG, "Error ensuring current key pair", e);
            return null;
        }
    }

    /**
     * Generate a new key pair and store it in the database
     */
    private String generateNewKeyPair() throws Exception {
        // Generate new key pair
        KeyPair keyPair = encryptionService.generateKeyPair();

        // Encode keys to Base64
        String publicKeyBase64 = android.util.Base64.encodeToString(keyPair.getPublic().getEncoded(), android.util.Base64.NO_WRAP);
        String privateKeyBase64 = android.util.Base64.encodeToString(keyPair.getPrivate().getEncoded(), android.util.Base64.NO_WRAP);

        // Create entity
        EncryptionKeyEntity entity = new EncryptionKeyEntity();
        entity.id = UuidGenerator.generate();
        entity.userId = currentUserId;
        entity.publicKey = publicKeyBase64;
        entity.privateKey = privateKeyBase64;
        entity.createdAt = System.currentTimeMillis();

        // Save to database
        encryptionKeyDao.insertKey(entity);

        Log.d(TAG, "Generated new key pair for user: " + currentUserId);

        return publicKeyBase64;
    }

    /**
     * Get the private key corresponding to a specific public key
     *
     * @param publicKeyBase64 The Base64 encoded public key
     * @return The corresponding Base64 encoded private key or null if not found
     */
    public String getPrivateKeyForPublicKey(String publicKeyBase64) {
        try {
            // Get all keys for this user
            List<EncryptionKeyEntity> allKeys = encryptionKeyDao.getAllKeysForUser(currentUserId);

            // Find the key entity with the matching public key
            for (EncryptionKeyEntity key : allKeys) {
                if (key.publicKey.equals(publicKeyBase64)) {
                    return key.privateKey;
                }
            }

            Log.e(TAG, "No matching private key found for public key");
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error finding private key for public key", e);
            return null;
        }
    }

    /**
     * Find the key entity by its public key
     *
     * @param publicKeyBase64 The Base64 encoded public key
     * @return The key entity or null if not found
     */
    public EncryptionKeyEntity getKeyEntityByPublicKey(String publicKeyBase64) {
        try {
            List<EncryptionKeyEntity> allKeys = encryptionKeyDao.getAllKeysForUser(currentUserId);

            for (EncryptionKeyEntity key : allKeys) {
                if (key.publicKey.equals(publicKeyBase64)) {
                    return key;
                }
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error finding key entity by public key", e);
            return null;
        }
    }

    /**
     * Check if the given key is still valid
     */
    private boolean isKeyValid(EncryptionKeyEntity key) {
        if (key == null) {
            return false;
        }

        // Calculate expiration date
        long expirationTime = key.createdAt + TimeUnit.DAYS.toMillis(KEY_VALIDITY_DAYS);
        long currentTime = System.currentTimeMillis();

        return currentTime < expirationTime;
    }

    /**
     * Encrypt a message for a specific recipient
     *
     * @param plainText The message to encrypt
     * @param recipientPublicKey The recipient's public key (Base64 encoded)
     * @return Base64 encoded encrypted message
     */
    public String encryptMessage(String plainText, String recipientPublicKey) {
        try {
            // Convert Base64 string to PublicKey
            byte[] keyBytes = android.util.Base64.decode(recipientPublicKey, android.util.Base64.NO_WRAP);
            PublicKey publicKey = encryptionService.convertBytesToPublicKey(keyBytes);

            // Encrypt message
            byte[] encryptedBytes = encryptionService.encryptString(plainText, publicKey);

            // Return as Base64 string
            return android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.NO_WRAP);

        } catch (Exception e) {
            Log.e(TAG, "Error encrypting message", e);
            return null;
        }
    }

    /**
     * Decrypt a message with the user's private key
     *
     * @param encryptedBase64 The Base64 encoded encrypted message
     * @param messageTimestamp The timestamp of when the message was created
     * @return Decrypted message text
     */
    public String decryptMessage(String encryptedBase64, long messageTimestamp) {
        try {
            // Find the appropriate key based on the message timestamp
            EncryptionKeyEntity key = findKeyForTimestamp(messageTimestamp);
            if (key == null) {
                Log.e(TAG, "No valid key found for message timestamp: " + messageTimestamp);
                return null;
            }

            // Convert Base64 string to PrivateKey
            byte[] keyBytes = android.util.Base64.decode(key.privateKey, android.util.Base64.NO_WRAP);
            PrivateKey privateKey = encryptionService.convertBytesToPrivateKey(keyBytes);

            // Decrypt message
            byte[] encryptedBytes = android.util.Base64.decode(encryptedBase64, android.util.Base64.NO_WRAP);
            return encryptionService.decryptToString(encryptedBytes, privateKey);

        } catch (Exception e) {
            Log.e(TAG, "Error decrypting message", e);
            return null;
        }
    }

    /**
     * Find the appropriate key for a given message timestamp
     */
    private EncryptionKeyEntity findKeyForTimestamp(long messageTimestamp) {
        // Get all keys for this user
        List<EncryptionKeyEntity> userKeys = encryptionKeyDao.getAllKeysForUser(currentUserId);

        // Sort by creation time (newest first)
        Collections.sort(userKeys, (k1, k2) -> Long.compare(k2.createdAt, k1.createdAt));

        // Find the key that was valid at the time the message was created
        for (EncryptionKeyEntity key : userKeys) {
            if (key.createdAt <= messageTimestamp) {
                // This key was created before or at the same time as the message
                long keyExpirationTime = key.createdAt + TimeUnit.DAYS.toMillis(KEY_VALIDITY_DAYS);
                if (messageTimestamp <= keyExpirationTime) {
                    // Message was created while this key was valid
                    return key;
                }
            }
        }

        // No valid key found, return the oldest key as a fallback
        return userKeys.isEmpty() ? null : userKeys.get(userKeys.size() - 1);
    }

    /**
     * Get the user's current public key
     */
    public String getCurrentPublicKey() {
        EncryptionKeyEntity currentKey = encryptionKeyDao.getKeyForUser(currentUserId);
        return currentKey != null ? currentKey.publicKey : null;
    }
}