package com.example.asiochatfrontend.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.connection.ConnectionMode;
import com.example.asiochatfrontend.core.model.dto.UserDto;

import java.util.Date;
import java.util.UUID;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "AsioChat_Prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_RELAY_IP = "relay_ip";
    private static final String KEY_PORT = "port";

    private EditText userIdEditText;
    private EditText relayIpEditText;
    private EditText portEditText;
    private EditText displayNameEditText;
    private Button submitButton;

    @Inject
    ConnectionManager connectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Check if user is already logged in
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedUserId = prefs.getString(KEY_USER_ID, null);
        if (savedUserId != null) {
            // User already logged in, proceed to MainActivity
            proceedToMainActivity(savedUserId);
            return;
        }

        // Initialize views
        userIdEditText = findViewById(R.id.login_ET_UID);
        relayIpEditText = findViewById(R.id.login_ET_relay_ip);
        portEditText = findViewById(R.id.login_ET_port);
        displayNameEditText = findViewById(R.id.login_ET_display_name);
        submitButton = findViewById(R.id.login_BTN_submit);

        // Load saved preferences if any
        loadPreferences();

        // Set up click listener for submit button
        submitButton.setOnClickListener(v -> loginUser());
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String relayIp = prefs.getString(KEY_RELAY_IP, "");
        String port = prefs.getString(KEY_PORT, "8081");
        String displayName = prefs.getString(KEY_USER_NAME, "");

        relayIpEditText.setText(relayIp);
        portEditText.setText(port);
        displayNameEditText.setText(displayName);
    }

    private void loginUser() {
        String userId = userIdEditText.getText().toString().trim();
        String relayIp = relayIpEditText.getText().toString().trim();
        String port = portEditText.getText().toString().trim();
        String displayName = displayNameEditText.getText().toString().trim();

        // Generate a user ID if not provided
        if (userId.isEmpty()) {
            userId = UUID.randomUUID().toString();
            userIdEditText.setText(userId);
        }

        // Validate inputs
        if (displayName.isEmpty()) {
            Toast.makeText(this, "Please enter a display name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (relayIp.isEmpty()) {
            Toast.makeText(this, "Please enter a relay server IP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (port.isEmpty()) {
            port = "8081"; // Default port
            portEditText.setText(port);
        }

        // Save preferences
        savePreferences(userId, relayIp, port, displayName);

        // Create user if not exists
        try {
            createUserIfNotExists(userId, displayName);
            // Proceed to MainActivity
            proceedToMainActivity(userId);
        } catch (Exception e) {
            Log.e(TAG, "Error creating user", e);
            Toast.makeText(this, "Error creating user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void savePreferences(String userId, String relayIp, String port, String displayName) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_RELAY_IP, relayIp);
        editor.putString(KEY_PORT, port);
        editor.putString(KEY_USER_NAME, displayName);
        editor.apply();
    }

    private void createUserIfNotExists(String userId, String displayName) throws Exception {
        // Create a UserDto object
        UserDto userDto = new UserDto(
                userId,
                displayName,
                null, // No profile picture initially
                "Hey there! I'm using AsioChat", // Default status
                true, // Online by default
                new Date(), // Last seen now
                new Date(), // Created now
                new Date() // Updated now
        );

        // Set connection mode to relay by default
        connectionManager.setConnectionMode(ConnectionMode.RELAY);

        // Set current user in the connection manager
        connectionManager.setCurrentUser(userId);

        // Create user through the connection manager
        connectionManager.createUser(userDto);
    }

    private void proceedToMainActivity(String userId) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String relayIp = prefs.getString(KEY_RELAY_IP, "");
        String port = prefs.getString(KEY_PORT, "8081");

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("USER_ID", userId);
        intent.putExtra("RELAY_IP", relayIp);
        intent.putExtra("PORT", Integer.parseInt(port));
        startActivity(intent);
        finish();
    }
}