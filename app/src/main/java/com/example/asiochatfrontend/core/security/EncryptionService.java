package com.example.asiochatfrontend.core.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

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
}
