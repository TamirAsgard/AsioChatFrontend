package com.example.asiochatfrontend.data.relay.service;

import android.util.Log;

import com.example.asiochatfrontend.core.model.dto.PublicKeyDto;
import com.example.asiochatfrontend.core.model.dto.SymmetricKeyDto;
import com.example.asiochatfrontend.core.security.EncryptionManager;
import com.example.asiochatfrontend.core.service.AuthService;
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
    private final ConcurrentHashMap<String, PublicKeyDto> PublicKeyCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SymmetricKeyDto> SymmetricKeyCache = new ConcurrentHashMap<>();
    private final String currentUserId;

    @Inject
    public RelayAuthService(RelayApiClient relayApiClient, EncryptionManager encryptionManager, String currentUserId) {
        this.relayApiClient = relayApiClient;
        this.encryptionManager = encryptionManager;
        this.currentUserId = currentUserId;
    }

    // Registers the current public key if needed
    @Override
    public boolean registerPublicKey() {
        try {
            PublicKeyDto keyDto = encryptionManager.ensureCurrentKeyPairDto();

            if (keyDto == null) {
                Log.d(TAG, "No existing public key pair, generating a new one...");
                keyDto = encryptionManager.generateNewKeyPairDto();
                if (keyDto == null) {
                    Log.e(TAG, "Public key generation failed.");
                    return false;
                }
                boolean success = relayApiClient.registerPublicKey(keyDto);
                Log.d(TAG, success ? "Successfully registered new public key." : "Failed to register new public key.");
                return success;
            }

            Log.d(TAG, "Public key already exists and is valid.");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Exception while registering public key", e);
            return false;
        }
    }

    // Allows manual registration of a specific public key
    @Override
    public boolean registerPublicKey(PublicKeyDto keyDto) {
        try {
            boolean success = relayApiClient.registerPublicKey(keyDto);
            Log.d(TAG, success ? "Manually registered public key." : "Manual registration of public key failed.");
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Exception while manually registering public key", e);
            return false;
        }
    }

    // Generates and registers a symmetric key for a chat
    @Override
    public boolean registerSymmetricKey(String chatId) {
        try {
            SymmetricKeyDto keyDto = encryptionManager.generateSymmetricKeyDtoForChat(chatId);
            if (keyDto == null) return false;
            boolean success = relayApiClient.registerSymmetricKey(keyDto);
            Log.d(TAG, success ? "Registered symmetric key for chat: " + chatId : "Failed to register symmetric key.");
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Exception while registering symmetric key for chat: " + chatId, e);
            return false;
        }
    }

    @Override
    public boolean resendSymmetricKey(String chatId, SymmetricKeyDto keyDto) {
        try {
            if (keyDto == null) return false;
            boolean success = relayApiClient.registerSymmetricKey(keyDto);
            Log.d(TAG, success ? "Registered symmetric key for chat: " + chatId : "Failed to register symmetric key.");
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Exception while registering symmetric key for chat: " + chatId, e);
            return false;
        }
    }

    // Retrieves a public key for a user and specific timestamp
    @Override
    public String getPublicKey(String userId, long messageTimestamp) {
        try {
            if (PublicKeyCache.containsKey(userId)) {
                EncryptionKeyEntity cachedKey = encryptionManager.getPublicKeyForTimestamp(userId, messageTimestamp);
                if (cachedKey != null && encryptionManager.isKeyValid(cachedKey)) {
                    Log.d(TAG, "Using cached public key for user: " + userId);
                    return cachedKey.getPublicKey();
                }
            }

            Log.d(TAG, "Fetching public key from relay for user: " + userId);
            PublicKeyDto keyDto = relayApiClient.getPublicKeyForTimestamp(userId, messageTimestamp);
            if (keyDto != null) {
                PublicKeyCache.put(userId, keyDto);
                encryptionManager.insertPublicKey(keyDto);
                return keyDto.getPublicKey();
            }

            Log.w(TAG, "No public key returned from relay for user: " + userId);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception while fetching public key for user: " + userId, e);
            return null;
        }
    }

    // Retrieves a symmetric key for a chat and specific timestamp
    @Override
    public String getSymmetricKey(String chatId, long messageTimestamp) {
        try {
            if (SymmetricKeyCache.containsKey(chatId)) {
                EncryptionKeyEntity cachedKey = encryptionManager.getSymmetricKeyForTimestamp(chatId, messageTimestamp);
                if (cachedKey != null && encryptionManager.isKeyValid(cachedKey)) {
                    SymmetricKeyDto latestBackendSymmetric = relayApiClient.getSymmetricKeyForTimestamp(chatId, messageTimestamp);
                    if (latestBackendSymmetric != null) {
                        SymmetricKeyCache.put(chatId, latestBackendSymmetric);
                        encryptionManager.insertSymmetricKey(latestBackendSymmetric);
                        return latestBackendSymmetric.getSymmetricKey();
                    }
                    Log.d(TAG, "Using cached symmetric key for chat: " + chatId);
                    return cachedKey.getSymmetricKey();
                }
            }

            Log.d(TAG, "Fetching symmetric key from relay for chat: " + chatId);
            SymmetricKeyDto keyDto = relayApiClient.getSymmetricKeyForTimestamp(chatId, messageTimestamp);
            if (keyDto != null) {
                SymmetricKeyCache.put(chatId, keyDto);
                encryptionManager.insertSymmetricKey(keyDto);
                return keyDto.getSymmetricKey();
            }

            Log.w(TAG, "No symmetric key returned from relay for chat: " + chatId);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception while fetching symmetric key for chat: " + chatId, e);
            return null;
        }
    }

    @Override
    public SymmetricKeyDto getSymmetricKeyDto(String chatId, long messageTimestamp) {
        try {
            if (SymmetricKeyCache.containsKey(chatId)) {
                EncryptionKeyEntity cachedKey = encryptionManager.getSymmetricKeyForTimestamp(chatId, messageTimestamp);
                if (cachedKey != null && encryptionManager.isKeyValid(cachedKey)) {
                    Log.d(TAG, "Using cached symmetric key for chat: " + chatId);
                    return symmetricEntityToDto(cachedKey);
                }
            }

            Log.d(TAG, "Fetching symmetric key from relay for chat: " + chatId);
            SymmetricKeyDto keyDto = relayApiClient.getSymmetricKeyForTimestamp(chatId, messageTimestamp);
            if (keyDto != null) {
                SymmetricKeyCache.put(chatId, keyDto);
                encryptionManager.insertSymmetricKey(keyDto);
                return keyDto;
            }

            Log.w(TAG, "No symmetric key returned from relay for chat: " + chatId);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Exception while fetching symmetric key for chat: " + chatId, e);
            return null;
        }
    }

    @Override
    public String encryptWithPublicKey(String plainText, String recipientId, long timestamp) {
        try {
            String publicKey = getPublicKey(recipientId, timestamp);
            return publicKey != null ? encryptionManager.encryptMessagePublic(plainText, publicKey) : null;
        } catch (Exception e) {
            Log.e(TAG, "Exception while encrypting with public key", e);
            return null;
        }
    }

    @Override
    public String decryptWithPrivateKey(String encryptedText, long timestamp) {
        try {
            return encryptionManager.decryptMessagePrivate(encryptedText, timestamp);
        } catch (Exception e) {
            Log.e(TAG, "Exception while decrypting with private key", e);
            return null;
        }
    }

    @Override
    public String encryptWithSymmetricKey(String plainText, String chatId, long timestamp) {
        try {
            getSymmetricKey(chatId, timestamp); // fetch or refresh if needed
            return encryptionManager.encryptMessageSymmetric(plainText, chatId);
        } catch (Exception e) {
            Log.e(TAG, "Exception while encrypting with symmetric key", e);
            return null;
        }
    }

    @Override
    public String decryptWithSymmetricKey(String combinedIvCiphertext, String chatId, long timestamp) {
        try {
            getSymmetricKey(chatId, timestamp); // fetch or refresh if needed
            return encryptionManager.decryptMessageSymmetric(combinedIvCiphertext, chatId, timestamp);
        } catch (Exception e) {
            Log.e(TAG, "Exception while decrypting with symmetric key", e);
            return null;
        }
    }

    public void clearCache() {
        Log.d(TAG, "Clearing public and symmetric key caches...");
        PublicKeyCache.clear();
        SymmetricKeyCache.clear();
    }

    private SymmetricKeyDto symmetricEntityToDto(EncryptionKeyEntity entity) {
        return new SymmetricKeyDto(
                entity.getChatId(),
                entity.getSymmetricKey(),
                entity.getCreatedAt(),
                entity.createdAt + TimeUnit.DAYS.toMillis(7)
        );
    }
}
