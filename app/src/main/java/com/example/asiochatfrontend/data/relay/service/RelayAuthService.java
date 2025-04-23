package com.example.asiochatfrontend.data.relay.service;

import android.util.Log;

import com.example.asiochatfrontend.core.model.dto.PublicKeyDto;
import com.example.asiochatfrontend.core.model.dto.SymmetricKeyDto;
import com.example.asiochatfrontend.core.security.EncryptionManager;
import com.example.asiochatfrontend.core.service.AuthService;
import com.example.asiochatfrontend.data.relay.network.RelayApiClient;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RelayAuthService implements AuthService {
    private static final String TAG = "RelayAuthService";

    private final RelayApiClient relayApiClient;
    private final EncryptionManager encryptionManager;
    private final ConcurrentHashMap<String, PublicKeyDto> PublicKeyCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SymmetricKeyDto> SymmetricKeyCache = new ConcurrentHashMap<>();
    private final String currentUserId;

    @Inject
    public RelayAuthService(RelayApiClient relayApiClient, EncryptionManager encryptionManager, String currentUserId) {
        this.relayApiClient = relayApiClient;
        this.encryptionManager = encryptionManager;
        this.currentUserId = currentUserId;
    }

    @Override
    public boolean registerPublicKey() {
        try {
            PublicKeyDto keyDto = encryptionManager.ensureCurrentKeyPairDto();

            if (keyDto == null) {
                Log.d(TAG, "No valid key pair found, generating a new one.");
                keyDto = encryptionManager.generateNewKeyPairDto(); // Saves the new key pair to the database

                if (keyDto == null) {
                    // Failed to generate new key pair
                    Log.e(TAG, "Failed to generate new key pair");
                    return false;
                } else {
                    // Register the new key pair
                    boolean success = relayApiClient.registerPublicKey(keyDto);
                    Log.d(TAG, success ? "Registered public key" : "Failed to register public key");
                    return success;
                }
            } else {
                Log.d(TAG, "Using existing key pair");
                return true; // Key already registered, no need to register again
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering public key", e);
            return false;
        }
    }

    @Override
    public boolean registerPublicKey(PublicKeyDto keyDto) {
        try {
            // Register the new key pair
            boolean success = relayApiClient.registerPublicKey(keyDto);
            Log.d(TAG, success ? "Registered public key" : "Failed to register public key");
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error registering public key", e);
            return false;
        }
    }

    @Override
    public boolean registerSymmetricKey(String chatId) {
        try {
            SymmetricKeyDto keyDto = encryptionManager.generateSymmetricKeyDtoForChat(chatId);
            if (keyDto == null) return false;
            boolean success = relayApiClient.registerSymmetricKey(keyDto);
            Log.d(TAG, success ? "Registered symmetric key for chat: " + chatId : "Failed to register symmetric key.");
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error registering symmetric key", e);
            return false;
        }
    }

    @Override
    public String getPublicKey(String userId, long messageTimestamp) {
        try {
            if (PublicKeyCache.containsKey(userId)) {
                return Objects.requireNonNull(PublicKeyCache.get(userId)).getPublicKey();
            }

            PublicKeyDto keyDto = relayApiClient.getPublicKeyForTimestamp(userId, messageTimestamp);
            if (keyDto != null) {
                PublicKeyCache.put(userId, keyDto);
                encryptionManager.insertPublicKey(keyDto);
                return keyDto.getPublicKey();
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error fetching public key", e);
            return null;
        }
    }

    @Override
    public String getSymmetricKey(String chatId, long messageTimestamp) {
        try {
            if (SymmetricKeyCache.containsKey(chatId)) {
                return Objects.requireNonNull(SymmetricKeyCache.get(chatId)).getSymmetricKey();
            }

            SymmetricKeyDto keyDto = relayApiClient.getSymmetricKeyForTimestamp(chatId, messageTimestamp);
            if (keyDto != null) {
                SymmetricKeyCache.put(chatId, keyDto);
                encryptionManager.insertSymmetricKey(keyDto);
                return keyDto.getSymmetricKey();
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error fetching symmetric key for chat: " + chatId, e);
            return null;
        }
    }

    @Override
    public String encryptWithPublicKey(String plainText, String recipientId, long timestamp) {
        try {
            String publicKey = getPublicKey(recipientId, timestamp);
            return publicKey != null ? encryptionManager.encryptMessagePublic(plainText, publicKey) : null;
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting message with public key", e);
            return null;
        }
    }

    @Override
    public String decryptWithPrivateKey(String encryptedText, long timestamp) {
        try {
            return encryptionManager.decryptMessagePrivate(encryptedText, timestamp);
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting message with private key", e);
            return null;
        }
    }

    @Override
    public String encryptWithSymmetricKey(String plainText, String chatId, long timestamp) {
        try {
            getSymmetricKey(chatId, timestamp);
            return encryptionManager.encryptMessageSymmetric(plainText, chatId);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting message with symmetric key", e);
            return null;
        }
    }

    @Override
    public String decryptWithSymmetricKey(String combinedIvCiphertext, String chatId, long timestamp) {
        try {
            getSymmetricKey(chatId, timestamp);
            return encryptionManager.decryptMessageSymmetric(combinedIvCiphertext, chatId, timestamp);
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting message with symmetric key", e);
            return null;
        }
    }

    public void clearCache() {
        PublicKeyCache.clear();
        SymmetricKeyCache.clear();
    }
}