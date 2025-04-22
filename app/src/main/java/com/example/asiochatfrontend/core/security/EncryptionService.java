package com.example.asiochatfrontend.core.security;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.inject.Inject;

public class EncryptionService {

    private final String algorithm = "RSA";

    @Inject
    public EncryptionService() {
    }

    public KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    public byte[] encrypt(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    public byte[] decrypt(byte[] encryptedData, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encryptedData);
    }

    public byte[] encryptString(String plainText, PublicKey publicKey) throws Exception {
        return encrypt(plainText.getBytes(), publicKey);
    }

    public String decryptToString(byte[] encryptedData, PrivateKey privateKey) throws Exception {
        return new String(decrypt(encryptedData, privateKey));
    }

    /**
     * Convert bytes to a PublicKey object
     */
    public PublicKey convertBytesToPublicKey(byte[] keyBytes) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * Convert bytes to a PrivateKey object
     */
    public PrivateKey convertBytesToPrivateKey(byte[] keyBytes) throws Exception {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        return keyFactory.generatePrivate(keySpec);
    }
}