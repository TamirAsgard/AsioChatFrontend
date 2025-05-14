package com.example.asiochatfrontend.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.multidex.BuildConfig;

import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.app.di.DatabaseModule;
import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.connection.ConnectionMode;
import com.example.asiochatfrontend.core.model.dto.UserDetailsDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.data.common.repository.ChatRepositoryImpl;
import com.example.asiochatfrontend.data.common.repository.MediaRepositoryImpl;
import com.example.asiochatfrontend.data.common.repository.MessageRepositoryImpl;
import com.example.asiochatfrontend.data.common.repository.UserRepositoryImpl;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.example.asiochatfrontend.data.database.AppDatabase;
import com.example.asiochatfrontend.domain.repository.ChatRepository;
import com.example.asiochatfrontend.domain.repository.MediaRepository;
import com.example.asiochatfrontend.domain.repository.MessageRepository;
import com.example.asiochatfrontend.domain.repository.UserRepository;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.Executors;

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

        // 1) Check prefs and possibly shortcut to MainActivity:
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userId  = prefs.getString(KEY_USER_ID,  null);
        String relayIp = prefs.getString(KEY_RELAY_IP, null);
        String portStr = prefs.getString(KEY_PORT,     null);

        if (userId != null && relayIp != null && portStr != null) {
            Executors.newSingleThreadExecutor().submit(() -> {
                boolean reachable = isRelayServerReachable(relayIp, Integer.parseInt(portStr));
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                if (!reachable) {
                    intent.putExtra("skipUserInit", true);
                }
                startActivity(intent);
                finish();
            });
        }

        // 2) Only if we didn’t early‐exit do we show the login screen:
        setContentView(R.layout.activity_login);
        userIdEditText      = findViewById(R.id.login_ET_UID);
        relayIpEditText     = findViewById(R.id.login_ET_relay_ip);
        portEditText        = findViewById(R.id.login_ET_port);
        submitButton        = findViewById(R.id.login_BTN_submit);

        loadPreferences();
        submitButton.setOnClickListener(v -> loginUser());

        TextView versionText = findViewById(R.id.version_text);
        versionText.setText(getString(R.string.version_format, "1.3"));
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String relayIp = prefs.getString(KEY_RELAY_IP, "51.124.125.16");
        String port = prefs.getString(KEY_PORT, "8081");

        relayIpEditText.setText(relayIp);
        portEditText.setText(port);
    }

    private void loginUser() {
        String userId = userIdEditText.getText().toString().trim();
        String relayIp = relayIpEditText.getText().toString().trim();
        String port = portEditText.getText().toString().trim();

        // Generate a user ID if not provided
        if (userId.isEmpty()) {
            Toast.makeText(this, "Please enter a user unique ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (relayIp.isEmpty()) {
            Toast.makeText(this, "Please enter a relay server IP", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate port is a valid number
        int portNumber;
        try {
            portNumber = Integer.parseInt(port);
            if (portNumber <= 0 || portNumber > 65535) {
                Toast.makeText(this, "Please enter a valid port number (1-65535)", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid port number", Toast.LENGTH_SHORT).show();
            return;
        }


        // Create user if not exists
        final String finalUserId = userId;
        final String finalRelayIp = relayIp;
        final String finalPortStr = port;

        Executors.newSingleThreadExecutor().submit(() -> {
            boolean isRelayServerReachable = this.isRelayServerReachable(finalRelayIp, portNumber);
            if (!isRelayServerReachable) {
                // force MainActivity into RELAY mode so its banner shows immediately
                Intent intent = new Intent(this, MainActivity.class);
                // tell MainActivity not to auto‐create the user again
                savePreferences(finalUserId, finalRelayIp, finalPortStr, "");
                intent.putExtra("skipUserInit", true);
                startActivity(intent);
                finish();
                return;
            }
        });

        initializeCoreServices(finalUserId, finalRelayIp, portNumber);
        connectionManager = ServiceModule.getConnectionManager();

        new Thread(() -> {
            try {
                createUserIfNotExists(finalUserId, "");
                savePreferences(finalUserId, finalRelayIp, finalPortStr, "");
                // Proceed to MainActivity on the UI thread
                runOnUiThread(() -> proceedToMainActivity(finalUserId));
            } catch (Exception e) {
                Log.e(TAG, "Error creating user", e);
            }
        }).start();
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
        // Set connection mode to relay by default
        connectionManager.setConnectionMode(ConnectionMode.RELAY);

        // Check if user already exists
        if (connectionManager.getUserById(userId) != null) {
            Log.i(TAG, "User exists: " + userId);
            connectionManager.setCurrentUser(userId);
            return;
        }

        // Create a UserDto object
        UserDto userDto = new UserDto(
                new UserDetailsDto(displayName, ""),
                userId,
                new Date(), // Created now
                new Date() // Updated now
        );

        // Set current user in the connection manager
        UserDto createdUser = connectionManager.createUser(userDto);
        if (createdUser == null) {
            throw new Exception("Failed to create user");
        }

        Log.i(TAG, "User created: " + createdUser.getJid());
        connectionManager.setCurrentUser(userId);
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

    private void initializeCoreServices(String userId, String relayIp, int port) {
        try {
            AppDatabase db = DatabaseModule.initialize(this);

            // Init repositories
            ChatRepository chatRepository = new ChatRepositoryImpl(db.chatDao());
            MessageRepository messageRepository = new MessageRepositoryImpl(db.messageDao());
            MediaRepository mediaRepository = new MediaRepositoryImpl(db.mediaDao(), messageRepository, new FileUtils(this));
            UserRepository userRepository = new UserRepositoryImpl(db.userDao());

            // Make sure relayIp has http:// prefix if missing
            if (!relayIp.startsWith("http://") && !relayIp.startsWith("https://")) {
                relayIp = "http://" + relayIp;
            }

            // Init core logic layer
            ServiceModule.initialize(
                    this,
                    chatRepository,
                    messageRepository,
                    mediaRepository,
                    userRepository,
                    userId,
                    relayIp,
                    port,
                    db
            );

            Log.i(TAG, "Core services initialized with userId: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing core services", e);
        }
    }

    /**
     * Returns true if a TCP connection can be established to the given host and port within the timeout.
     */
    private boolean isRelayServerReachable(String relayIp, int port) {
        // Strip any protocol prefix
        String host = relayIp.replaceFirst("^https?://", "");
        final int TIMEOUT_MS = 2000;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            Log.w(TAG, "Relay unreachable at " + host + ":" + port + " → " + e.getMessage());
            return false;
        }
    }
}