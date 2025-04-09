package com.example.asiochatfrontend.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.connection.ConnectionMode;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.ui.chat.ChatActivity;
import com.example.asiochatfrontend.ui.chat.NewChatActivity;
import com.example.asiochatfrontend.ui.home.HomeViewModel;
import com.example.asiochatfrontend.ui.home.adapter.ChatsAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "AsioChat_Prefs";
    private static final String KEY_CONNECTION_MODE = "connection_mode";

    private RecyclerView chatList;
    private ChatsAdapter adapter;
    private HomeViewModel viewModel;
    private FloatingActionButton fabNewChat;
    private MaterialButton btnAll, btnUnread;
    private MaterialButton searchButton, moreButton;
    private View connectionStatusBanner;
    private TextView connectionStatusText;
    private Button switchModeButton, backToLoginButton;
    private String currentUserId;

    @Inject
    ConnectionManager connectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the user ID from the intent
        currentUserId = getIntent().getStringExtra("USER_ID");
        if (currentUserId == null) {
            // If not provided, check shared preferences
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            currentUserId = prefs.getString("user_id", null);

            if (currentUserId == null) {
                // Still null, redirect to login
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return;
            }
        }

        // Initialize views
        initializeViews();

        // Set up connection manager
        setupConnectionManager();

        // Set up view model
        setupViewModel();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        chatList = findViewById(R.id.main_LST_chats);
        fabNewChat = findViewById(R.id.fab_new_chat);
        btnAll = findViewById(R.id.button_all);
        btnUnread = findViewById(R.id.button_unread);
        searchButton = findViewById(R.id.top_bar_BTN_search);
        moreButton = findViewById(R.id.top_bar_BTN_more);
        connectionStatusBanner = findViewById(R.id.connectionStatusBanner);

        if (connectionStatusBanner != null) {
            connectionStatusText = connectionStatusBanner.findViewById(R.id.connectionStatusText);
            switchModeButton = connectionStatusBanner.findViewById(R.id.switchToPrivateMeshButton);
            backToLoginButton = connectionStatusBanner.findViewById(R.id.backToLogin);
        }
    }

    private void setupConnectionManager() {
        // Get saved connection mode
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String modeName = prefs.getString(KEY_CONNECTION_MODE, ConnectionMode.RELAY.name());
        ConnectionMode mode = ConnectionMode.valueOf(modeName);

        // Set connection mode
        connectionManager.setConnectionMode(mode);

        // Set current user
        connectionManager.setCurrentUser(currentUserId);

        // Observe connection mode changes
        connectionManager.connectionMode.observe(this, this::onConnectionModeChanged);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Set up RecyclerView
        chatList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ChatsAdapter(chat -> {
            // Handle chat item click
            openChatActivity(chat);
        });
        chatList.setAdapter(adapter);

        // Observe chats
        viewModel.getChats().observe(this, this::onChatsLoaded);
    }

    private void setupClickListeners() {
        // New chat button
        fabNewChat.setOnClickListener(v -> startActivity(new Intent(this, NewChatActivity.class)));

        // Filter buttons
        btnAll.setOnClickListener(v -> {
            btnAll.setBackgroundResource(R.drawable.greenish_background_with_radius);
            btnUnread.setBackgroundResource(R.drawable.grey_background_with_radius);
            viewModel.loadAllChats();
        });

        btnUnread.setOnClickListener(v -> {
            btnAll.setBackgroundResource(R.drawable.grey_background_with_radius);
            btnUnread.setBackgroundResource(R.drawable.greenish_background_with_radius);
            viewModel.loadUnreadChats();
        });

        // Search button
        searchButton.setOnClickListener(v -> {
            Toast.makeText(this, "Search functionality coming soon", Toast.LENGTH_SHORT).show();
        });

        // More button
        moreButton.setOnClickListener(this::showMoreOptionsMenu);

        // Connection banner buttons
        if (switchModeButton != null) {
            switchModeButton.setOnClickListener(v -> switchConnectionMode());
        }

        if (backToLoginButton != null) {
            backToLoginButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    private void showMoreOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.menu_settings);

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.Direct_Mode) {
                connectionManager.setConnectionMode(ConnectionMode.DIRECT);
                saveConnectionMode(ConnectionMode.DIRECT);
                return true;
            } else if (itemId == R.id.Relay_Mode) {
                connectionManager.setConnectionMode(ConnectionMode.RELAY);
                saveConnectionMode(ConnectionMode.RELAY);
                return true;
            } else if (itemId == R.id.ReSync) {
                refreshData();
                return true;
            }

            return false;
        });

        popup.show();
    }

    private void saveConnectionMode(ConnectionMode mode) {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putString(KEY_CONNECTION_MODE, mode.name());
        editor.apply();
    }

    private void switchConnectionMode() {
        ConnectionMode currentMode = connectionManager.connectionMode.getValue();
        ConnectionMode newMode = (currentMode == ConnectionMode.DIRECT) ?
                ConnectionMode.RELAY : ConnectionMode.DIRECT;

        connectionManager.setConnectionMode(newMode);
        saveConnectionMode(newMode);

        refreshData();
    }

    private void refreshData() {
        viewModel.refresh();
        Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show();
    }

    private void onConnectionModeChanged(ConnectionMode mode) {
        String modeName = mode == ConnectionMode.DIRECT ? "Direct (P2P)" : "Relay (Server)";
        Toast.makeText(this, "Connection mode: " + modeName, Toast.LENGTH_SHORT).show();

        // Update UI based on connection mode
        updateConnectionBanner(mode);
    }

    private void updateConnectionBanner(ConnectionMode mode) {
        if (connectionStatusBanner == null || connectionStatusText == null || switchModeButton == null) {
            return;
        }

        boolean isConnected = false;

        if (mode == ConnectionMode.DIRECT) {
            // For direct mode, we'd check if the P2P connection is established
            // This would involve checking the DirectWebSocketClient
            // For now, let's assume it's connected
            isConnected = true;
            connectionStatusText.setText("Connected via P2P");
            switchModeButton.setText("Switch to Relay Mode");
        } else {
            // For relay mode, check if connection to server is established
            // This would involve checking the RelayWebSocketClient
            // For now, let's assume it's connected
            isConnected = true;
            connectionStatusText.setText("Connected via Relay Server");
            switchModeButton.setText("Switch to P2P Mode");
        }

        connectionStatusBanner.setVisibility(isConnected ? View.GONE : View.VISIBLE);
    }

    private void onChatsLoaded(List<ChatDto> chats) {
        adapter.submitList(chats);

        if (chats.isEmpty()) {
            // Show empty state
            Toast.makeText(this, "No chats found. Start a new chat!", Toast.LENGTH_SHORT).show();
        }
    }

    private void openChatActivity(ChatDto chat) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("CHAT_ID", chat.getId());
        intent.putExtra("CHAT_NAME", chat.getName());
        intent.putExtra("CHAT_TYPE", chat.getType().name());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when coming back to this activity
        refreshData();
    }
}