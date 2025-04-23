package com.example.asiochatfrontend.core.security;

import android.util.Log;

import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.model.dto.PublicKeyDto;
import com.example.asiochatfrontend.data.relay.network.RelayApiClient;
import com.example.asiochatfrontend.data.relay.service.RelayAuthService;

public class OnStartUserKeyInitialization {
    private static final String TAG = "OnStartUserKeyInitialization";

    public static void executePublicKeyInitialization(
            RelayAuthService authService,
            RelayApiClient relayApiClient) {
        new Thread(() -> {
            try {
                if (authService == null) {
                    Log.e(TAG, "Auth service not initialized");
                    return;
                }

                // Step 1: Generate a key only if missing or expired (returns new if created)
                PublicKeyDto currentKey = ServiceModule.getEncryptionManager().ensureCurrentKeyPairDto();
                if (currentKey == null) {
                    Log.d(TAG, "No valid key pair found, generating a new one.");
                    currentKey = ServiceModule.getEncryptionManager().generateNewKeyPairDto();
                    if (currentKey == null) {
                        Log.e(TAG, "Failed to generate new key pair");
                        return;
                    }
                }

                // Step 2: Check if backend has it
                PublicKeyDto backendKey = relayApiClient
                        .getPublicKeyForTimestamp(currentKey.getJid(), currentKey.getCreatedAt());

                if (backendKey == null || !backendKey.getPublicKey().equals(currentKey.getPublicKey())) {
                    boolean registered = authService.registerPublicKey(currentKey);
                    if (registered) {
                        Log.d(TAG, "Registered missing/updated public key with backend");
                    } else {
                        Log.e(TAG, "Failed to register public key with backend");
                    }
                } else {
                    Log.d(TAG, "Public key is already registered on backend");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error running one-time key rotation check", e);
            }
        }).start();
    }
}
