package com.example.asiochatfrontend.core.security;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;

public class EncryptionService {

    private final String rsaAlgorithm = "RSA";
    private final String aesAlgorithm = "AES";
    private final String aesTransformation = "AES/CBC/PKCS5Padding"; // secure and padding-friendly

    private final int aesKeySize = 256;

    @Inject
    public EncryptionService() {
    }

    // ====== RSA KEY PAIR (Asymmetric) ======
    public KeyPair generatePublicPrivateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(rsaAlgorithm);
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    public byte[] encryptPublic(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(rsaAlgorithm);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    public byte[] decryptPrivate(byte[] encryptedData, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(rsaAlgorithm);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encryptedData);
    }

    public byte[] encryptStringPublic(String plainText, PublicKey publicKey) throws Exception {
        return encryptPublic(plainText.getBytes(), publicKey);
    }

    public String decryptToStringPrivate(byte[] encryptedData, PrivateKey privateKey) throws Exception {
        return new String(decryptPrivate(encryptedData, privateKey));
    }

    public PublicKey convertBytesToPublicKey(byte[] keyBytes) throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(rsaAlgorithm);
        return keyFactory.generatePublic(keySpec);
    }

    public PrivateKey convertBytesToPrivateKey(byte[] keyBytes) throws Exception {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(rsaAlgorithm);
        return keyFactory.generatePrivate(keySpec);
    }

    // ====== AES (Symmetric) ======

    public SecretKey generateSymmetricKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(aesAlgorithm);
        keyGen.init(aesKeySize);
        return keyGen.generateKey();
    }

    public byte[] encryptSymmetric(byte[] data, SecretKey secretKey, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(aesTransformation);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }

    public byte[] decryptSymmetric(byte[] encryptedData, SecretKey secretKey, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(aesTransformation);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
        return cipher.doFinal(encryptedData);
    }

    public String encryptStringSymmetric(String plainText, SecretKey secretKey, byte[] iv) throws Exception {
        byte[] encrypted = encryptSymmetric(plainText.getBytes(), secretKey, iv);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decryptStringSymmetric(String base64Encrypted, SecretKey secretKey, byte[] iv) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(base64Encrypted);
        byte[] decrypted = decryptSymmetric(decoded, secretKey, iv);
        return new String(decrypted);
    }

    public SecretKey convertBytesToSecretKey(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, aesAlgorithm);
    }

    public byte[] generateRandomIV() {
        byte[] iv = new byte[16]; // 128-bit IV for AES
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}
