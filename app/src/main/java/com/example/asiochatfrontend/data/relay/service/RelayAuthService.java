package com.example.asiochatfrontend.data.relay.service;

import android.util.Log;

import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.model.dto.PublicKeyDto;
import com.example.asiochatfrontend.core.security.EncryptionManager;
import com.example.asiochatfrontend.core.service.AuthService;
import com.example.asiochatfrontend.data.database.dao.EncryptionKeyDao;
import com.example.asiochatfrontend.data.database.entity.EncryptionKeyEntity;
import com.example.asiochatfrontend.data.relay.network.RelayApiClient;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RelayAuthService implements AuthService {
    private static final String TAG = "RelayAuthService";

    private final RelayApiClient relayApiClient;
    private final EncryptionManager encryptionManager;
    private final ConcurrentHashMap<String, PublicKeyDto> keyCache = new ConcurrentHashMap<>();
    private final String currentUserId;

    @Inject
    public RelayAuthService(RelayApiClient relayApiClient, String currentUserId) {
        this.relayApiClient = relayApiClient;
        this.encryptionManager = ServiceModule.getEncryptionManager();
        this.currentUserId = currentUserId;
    }

    /**
     * Initialize keys for current user - should be called after login
     * This method ensures the user has valid keys and registers them with the backend
     */
    public void initializeUserKeys() {
        if (currentUserId == null || encryptionManager == null) {
            Log.e(TAG, "Cannot initialize keys: user ID or encryption manager not set");
            return;
        }

        try {
            // Ensure we have a valid key pair for this user
            String publicKey = encryptionManager.ensureCurrentKeyPair();
            if (publicKey != null) {
                // Register the public key with the backend
                long createdAt = System.currentTimeMillis();
                registerPublicKey(currentUserId, publicKey, createdAt, 7);
                Log.d(TAG, "Initialized encryption keys for user: " + currentUserId);
            } else {
                Log.e(TAG, "Failed to generate or retrieve key pair");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing user keys", e);
        }
    }

    /**
     * Register the user's current public key with the backend
     *
     * @param userId User ID
     * @param publicKey Base64 encoded public key
     * @param createdAt Creation timestamp of the key
     * @param expiresDays Number of days until the key expires
     * @return true if successful
     */
    public boolean registerPublicKey(String userId, String publicKey, long createdAt, int expiresDays) {
        try {
            long expiresAt = createdAt + TimeUnit.DAYS.toMillis(expiresDays);
            PublicKeyDto keyDto = new PublicKeyDto(userId, publicKey, createdAt, expiresAt);

            boolean success = relayApiClient.registerPublicKey(keyDto);

            if (success) {
                // Update local cache
                keyCache.put(getCacheKey(userId, createdAt), keyDto);
                Log.d(TAG, "Successfully registered public key for user: " + userId);
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error registering public key", e);
            return false;
        }
    }

    /**
     * Get a recipient's public key that was valid at the time the message was created
     *
     * @param userId User ID
     * @param messageTimestamp Message creation timestamp
     * @return Base64 encoded public key or null if not found
     */
    public String getPublicKey(String userId, long messageTimestamp) {
        try {
            // Check cache first
            String cacheKey = getCacheKey(userId, messageTimestamp);
            PublicKeyDto cachedKey = keyCache.get(cacheKey);

            if (cachedKey != null) {
                return cachedKey.getPublicKey();
            }

            PublicKeyDto keyDto = relayApiClient.getPublicKeyForTimestamp(userId, messageTimestamp);

            if (keyDto != null) {
                // Update cache
                keyCache.put(cacheKey, keyDto);
                return keyDto.getPublicKey();
            }

            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error getting public key", e);
            return null;
        }
    }

    /**
     * Encrypt a message for a specific recipient
     *
     * @param plainText The message to encrypt
     * @param recipientId The recipient's user ID
     * @param timestamp Current timestamp (used to get the appropriate public key)
     * @return Encrypted message as Base64 string, or null if encryption fails
     */
    public String encryptForRecipient(String plainText, String recipientId, long timestamp) {
        if (encryptionManager == null) {
            Log.e(TAG, "Encryption manager not initialized");
            return null;
        }

        try {
            // Get recipient's public key
            String publicKey = getPublicKey(recipientId, timestamp);
            if (publicKey == null) {
                Log.e(TAG, "Could not find public key for recipient: " + recipientId);
                return null;
            }

            // Encrypt the message
            return encryptionManager.encryptMessage(plainText, publicKey);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting message for recipient: " + recipientId, e);
            return null;
        }
    }

    /**
     * Decrypt a message that was sent to the current user
     *
     * @param encryptedText The encrypted message (Base64 encoded)
     * @param timestamp When the message was encrypted
     * @return Decrypted plain text, or null if decryption fails
     */
    public String decryptMessage(String encryptedText, long timestamp) {
        if (encryptionManager == null) {
            Log.e(TAG, "Encryption manager not initialized");
            return null;
        }

        try {
            return encryptionManager.decryptMessage(encryptedText, timestamp);
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting message", e);
            return null;
        }
    }

    /**
     * Rotate encryption keys for the current user
     * @return true if successful
     */
    public boolean rotateKeys() {
        if (currentUserId == null || encryptionManager == null) {
            Log.e(TAG, "Cannot rotate keys: user ID or encryption manager not set");
            return false;
        }

        try {
            // Generate new key pair
            String newPublicKey = encryptionManager.ensureCurrentKeyPair();
            if (newPublicKey == null) {
                Log.e(TAG, "Failed to generate new key pair");
                return false;
            }

            // Register with backend
            long createdAt = System.currentTimeMillis();
            return registerPublicKey(currentUserId, newPublicKey, createdAt, 7);
        } catch (Exception e) {
            Log.e(TAG, "Error rotating keys", e);
            return false;
        }
    }

    /**
     * Generate a cache key for a user ID and timestamp
     */
    private String getCacheKey(String userId, long timestamp) {
        return userId + "_" + timestamp;
    }

    /**
     * Clear the public key cache
     */
    public void clearCache() {
        keyCache.clear();
    }
}