package com.example.asiochatfrontend.core.security;

import android.util.Log;

import com.example.asiochatfrontend.core.model.dto.PublicKeyDto;
import com.example.asiochatfrontend.core.model.dto.SymmetricKeyDto;
import com.example.asiochatfrontend.data.database.dao.EncryptionKeyDao;
import com.example.asiochatfrontend.data.database.entity.EncryptionKeyEntity;
import com.example.asiochatfrontend.data.common.utils.UuidGenerator;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

/**
 * Manages encryption key rotation and message encryption/decryption (RSA and AES)
 */
public class EncryptionManager {
    private static final String TAG = "EncryptionManager";
    private static final long KEY_VALIDITY_DAYS = 7; // Key validity period in days

    private final EncryptionService encryptionService;
    private final EncryptionKeyDao encryptionKeyDao;
    private final String currentUserId;

    public EncryptionManager(EncryptionService encryptionService, EncryptionKeyDao encryptionKeyDao, String currentUserId) {
        this.encryptionService = encryptionService;
        this.encryptionKeyDao = encryptionKeyDao;
        this.currentUserId = currentUserId;
    }

    // ====== RSA Public/Private Key Operations ======

    public PublicKeyDto ensureCurrentKeyPairDto() {
        try {
            EncryptionKeyEntity currentKey = encryptionKeyDao.getLatestPublicKeyForUser(currentUserId);
            if (isKeyValid(currentKey)) {
                return new PublicKeyDto(currentUserId, currentKey.publicKey, currentKey.createdAt, currentKey.createdAt + TimeUnit.DAYS.toMillis(KEY_VALIDITY_DAYS));
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error ensuring current key pair", e);
            return null;
        }
    }

    public PublicKeyDto generateNewKeyPairDto() throws Exception {
        KeyPair keyPair = encryptionService.generatePublicPrivateKeyPair();
        String publicKeyBase64 = android.util.Base64.encodeToString(keyPair.getPublic().getEncoded(), android.util.Base64.NO_WRAP);
        String privateKeyBase64 = android.util.Base64.encodeToString(keyPair.getPrivate().getEncoded(), android.util.Base64.NO_WRAP);

        long createdAt = System.currentTimeMillis();
        long expiresAt = createdAt + TimeUnit.DAYS.toMillis(KEY_VALIDITY_DAYS);

        EncryptionKeyEntity entity = new EncryptionKeyEntity();
        entity.id = UuidGenerator.generate();
        entity.userId = currentUserId;
        entity.publicKey = publicKeyBase64;
        entity.privateKey = privateKeyBase64;
        entity.createdAt = createdAt;

        encryptionKeyDao.insertKey(entity);
        Log.d(TAG, "Generated new RSA key pair for user: " + currentUserId);
        return new PublicKeyDto(currentUserId, publicKeyBase64, createdAt, expiresAt);
    }

    public SymmetricKeyDto generateSymmetricKeyDtoForChat(String chatId) {
        try {
            SecretKey secretKey = encryptionService.generateSymmetricKey();
            String keyBase64 = android.util.Base64.encodeToString(secretKey.getEncoded(), android.util.Base64.NO_WRAP);

            long createdAt = System.currentTimeMillis();
            long expiresAt = createdAt + TimeUnit.DAYS.toMillis(KEY_VALIDITY_DAYS);

            EncryptionKeyEntity entity = new EncryptionKeyEntity();
            entity.id = UuidGenerator.generate();
            entity.chatId = chatId;
            entity.symmetricKey = keyBase64;
            entity.createdAt = createdAt;

            encryptionKeyDao.insertKey(entity);
            Log.d(TAG, "Generated new AES key for chat: " + chatId);
            return new SymmetricKeyDto(chatId, keyBase64, createdAt, expiresAt);
        } catch (Exception e) {
            Log.e(TAG, "Error generating symmetric key", e);
            return null;
        }
    }

    public SecretKey getSymmetricKeyObjectForChat(String chatId, long timestamp) {
        try {
            EncryptionKeyEntity key = encryptionKeyDao.getSymmetricKeyForTimestamp(chatId, timestamp);
            if (key != null && key.symmetricKey != null) {
                byte[] keyBytes = android.util.Base64.decode(key.symmetricKey, android.util.Base64.NO_WRAP);
                return encryptionService.convertBytesToSecretKey(keyBytes);
            }
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving symmetric key for chat: " + chatId, e);
            return null;
        }
    }

    public boolean isKeyValid(EncryptionKeyEntity key) {
        if (key == null) return false;
        long expirationTime = key.createdAt + TimeUnit.DAYS.toMillis(KEY_VALIDITY_DAYS);
        return System.currentTimeMillis() < expirationTime;
    }

    public String encryptMessagePublic(String plainText, String recipientPublicKey) {
        try {
            byte[] keyBytes = android.util.Base64.decode(recipientPublicKey, android.util.Base64.NO_WRAP);
            PublicKey publicKey = encryptionService.convertBytesToPublicKey(keyBytes);
            byte[] encryptedBytes = encryptionService.encryptStringPublic(plainText, publicKey);
            return android.util.Base64.encodeToString(encryptedBytes, android.util.Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting message", e);
            return null;
        }
    }

    public String decryptMessagePrivate(String encryptedBase64, long messageTimestamp) {
        try {
            EncryptionKeyEntity key = encryptionKeyDao.getPublicKeyForTimestamp(currentUserId, messageTimestamp);
            if (key == null) {
                Log.e(TAG, "No valid RSA key found in repo for timestamp: " + messageTimestamp);
                return null;
            }

            byte[] keyBytes = android.util.Base64.decode(key.privateKey, android.util.Base64.NO_WRAP);
            PrivateKey privateKey = encryptionService.convertBytesToPrivateKey(keyBytes);

            byte[] encryptedBytes = android.util.Base64.decode(encryptedBase64, android.util.Base64.NO_WRAP);
            return encryptionService.decryptToStringPrivate(encryptedBytes, privateKey);
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting message", e);
            return null;
        }
    }

    public String encryptMessageSymmetric(String plainText, String chatId) {
        try {
            SecretKey secretKey = getSymmetricKeyObjectForChat(chatId, System.currentTimeMillis());
            byte[] iv = encryptionService.generateRandomIV();
            String encrypted = encryptionService.encryptStringSymmetric(plainText, secretKey, iv);
            return android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP) + ":" + encrypted;
        } catch (Exception e) {
            Log.e(TAG, "Error encrypting symmetric message", e);
            return null;
        }
    }

    public String decryptMessageSymmetric(String combinedIvCiphertext, String chatId, long timestamp) {
        try {
            String[] parts = combinedIvCiphertext.split(":");
            if (parts.length != 2) return null;
            byte[] iv = android.util.Base64.decode(parts[0], android.util.Base64.NO_WRAP);
            String encryptedText = parts[1];

            SecretKey secretKey = getSymmetricKeyObjectForChat(chatId, timestamp);
            return encryptionService.decryptStringSymmetric(encryptedText, secretKey, iv);
        } catch (Exception e) {
            Log.e(TAG, "Error decrypting symmetric message, key might be outdated", e);
            return null;
        }
    }

    public void insertSymmetricKey(SymmetricKeyDto symmetricKeyDto) {
        EncryptionKeyEntity entity = encryptionKeyDao.getLatestSymmetricKeyForChat(symmetricKeyDto.getChatId());
        if (entity != null) {
            entity.symmetricKey = symmetricKeyDto.getSymmetricKey();
            entity.createdAt = symmetricKeyDto.getCreatedAt();
            encryptionKeyDao.updateKey(entity);
            return;
        }

        // If no existing key found, create a new one
        entity = new EncryptionKeyEntity();
        entity.id = UuidGenerator.generate();
        entity.chatId = symmetricKeyDto.getChatId();
        entity.symmetricKey = symmetricKeyDto.getSymmetricKey();
        entity.createdAt = symmetricKeyDto.getCreatedAt();
        encryptionKeyDao.insertKey(entity);
    }

    public void insertPublicKey(PublicKeyDto publicKeyDto) {
        EncryptionKeyEntity entity = new EncryptionKeyEntity();
        entity.id = UuidGenerator.generate();
        entity.userId = publicKeyDto.getJid();
        entity.publicKey = publicKeyDto.getPublicKey();
        entity.createdAt = publicKeyDto.getCreatedAt();
        encryptionKeyDao.insertKey(entity);
    }

    public EncryptionKeyEntity getPublicKeyForTimestamp(String userId, long timestamp) {
        return encryptionKeyDao.getPublicKeyForTimestamp(userId, timestamp);
    }

    public EncryptionKeyEntity getSymmetricKeyForTimestamp(String chatId, long timestamp) {
        return encryptionKeyDao.getSymmetricKeyForTimestamp(chatId, timestamp);
    }
}
