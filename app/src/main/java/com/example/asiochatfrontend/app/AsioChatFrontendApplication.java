package com.example.asiochatfrontend.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.asiochatfrontend.app.di.DatabaseModule;
import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.connection.ConnectionMode;
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


public class AsioChatFrontendApplication extends Application {
    private static final String TAG = "AsioChatApp";
    private static final String PREFS_NAME = "AsioChat_Prefs";
    private static final String KEY_CONNECTION_MODE = "connection_mode";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_RELAY_IP = "relay_ip";
    private static final String KEY_PORT = "port";

    private static Context appContext;

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;

        // Check if we have saved user credentials and can initialize services
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String userId = prefs.getString(KEY_USER_ID, null);
        String relayIp = prefs.getString(KEY_RELAY_IP, null);
        String portStr = prefs.getString(KEY_PORT, null);

        if (userId != null && relayIp != null && portStr != null) {
            int port = Integer.parseInt(portStr);

            // Initialize core services
            initializeCoreServices(userId, relayIp, port);

            // Check if we should start in direct mode
            String modeName = prefs.getString(KEY_CONNECTION_MODE, ConnectionMode.RELAY.name());
            if (ConnectionMode.valueOf(modeName) == ConnectionMode.DIRECT) {
                ServiceModule.startUserDiscovery();
            }
        } else {
            Log.d(TAG, "No saved credentials found, waiting for login");
        }
    }

    private void initializeCoreServices(String userId, String relayIp, int port) {
        try {
            AppDatabase db = DatabaseModule.initialize(this);

            // Init repositories
            ChatRepository chatRepository = new ChatRepositoryImpl(db.chatDao());
            MessageRepository messageRepository = new MessageRepositoryImpl(db.messageDao());
            MediaRepository mediaRepository = new MediaRepositoryImpl(db.mediaDao(), new FileUtils(this));
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
                    port
            );

            Log.i(TAG, "Core services initialized with userId: " + userId);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing core services", e);
        }
    }

    public static Context getAppContext() {
        return appContext;
    }
}