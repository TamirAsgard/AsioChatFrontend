package com.example.asiochatfrontend.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.app.di.DatabaseModule;
import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.connection.ConnectionMode;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.enums.ChatType;
import com.example.asiochatfrontend.core.security.KeyRotationJob;
import com.example.asiochatfrontend.core.security.OnStartUserKeyInitialization;
import com.example.asiochatfrontend.core.service.OnWSEventCallback;
import com.example.asiochatfrontend.data.common.repository.ChatRepositoryImpl;
import com.example.asiochatfrontend.data.common.repository.MediaRepositoryImpl;
import com.example.asiochatfrontend.data.common.repository.MessageRepositoryImpl;
import com.example.asiochatfrontend.data.common.repository.UserRepositoryImpl;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.example.asiochatfrontend.data.database.AppDatabase;
import com.example.asiochatfrontend.data.relay.service.RelayAuthService;
import com.example.asiochatfrontend.domain.repository.ChatRepository;
import com.example.asiochatfrontend.domain.repository.MediaRepository;
import com.example.asiochatfrontend.domain.repository.MessageRepository;
import com.example.asiochatfrontend.domain.repository.UserRepository;
import com.example.asiochatfrontend.ui.chat.ChatActivity;
import com.example.asiochatfrontend.ui.chat.NewChatActivity;
import com.example.asiochatfrontend.ui.chat.bus.ChatUpdateBus;
import com.example.asiochatfrontend.ui.home.HomeViewModel;
import com.example.asiochatfrontend.ui.home.HomeViewModelFactory;
import com.example.asiochatfrontend.ui.home.adapter.ChatsAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements OnWSEventCallback {
    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "AsioChat_Prefs";
    private static final String KEY_CONNECTION_MODE = "connection_mode";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_RELAY_IP = "relay_ip";
    private static final String KEY_PORT = "port";

    // Connection variables
    private volatile boolean keepTryingToConnect = false;
    private volatile boolean isConnectionEstablished = false;
    private ExecutorService connectionExecutor;
    private Future<?> connectionTask;
    private Socket connectionSocket;

    // UI elements
    private RecyclerView chatList;
    private ChatsAdapter adapter;
    private HomeViewModel viewModel;
    private FloatingActionButton fabNewChat;
    private MaterialButton btnAll, btnUnread;
    private MaterialButton searchButton, moreButton;
    private View connectionStatusBanner;
    private TextView connectionStatusText;
    private View mainContentLayout;
    private Button switchModeButton, backToLoginButton;
    private String currentUserId;

    private ConnectionManager connectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DatabaseModule.initialize(this);
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Get user details from preferences
        currentUserId = prefs.getString(KEY_USER_ID, null);
        String relayIp = prefs.getString(KEY_RELAY_IP, null);
        String portStr = prefs.getString(KEY_PORT, null);

        if (currentUserId == null || relayIp == null || portStr == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Else, relay server details are stored, continue with initialization
        // Initialize views and services
        initializeViews();
        initializeCoreServices(currentUserId, relayIp, Integer.parseInt(portStr));
        setupViewModel();
        setupClickListeners();
        setupChatUpdateObservers();

        // Get saved connection mode
        String modeName = prefs.getString(KEY_CONNECTION_MODE, ConnectionMode.RELAY.name());
        ConnectionMode mode = ConnectionMode.valueOf(modeName);

        // Set connection mode
        connectionManager.setConnectionMode(mode);

        // If relay mode, check connection
        if (mode == ConnectionMode.RELAY) {
            startRelayConnectionCheckLoop(relayIp, Integer.parseInt(portStr));
        } else {
            // Start P2P discovery
            ServiceModule.startUserDiscovery();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (adapter != null) {
                adapter.notifyDataSetChanged(); // Force RecyclerView to rebind all
            }
        }, 500); // Slight delay gives LiveData & ViewModel time to populate

        // Schedule key rotation job
        KeyRotationJob.schedule(this, currentUserId);

        // One-time key rotation check on app start
        OnStartUserKeyInitialization.executePublicKeyInitialization(
                ServiceModule.getConnectionManager().relayAuthService,
                ServiceModule.getRelayApiClient()
        );
    }

    private void initializeViews() {
        mainContentLayout = findViewById(R.id.mainContentLayout);
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

    private void setupViewModel() {
        HomeViewModelFactory factory = new HomeViewModelFactory(connectionManager, currentUserId);
        viewModel = new ViewModelProvider(this, factory).get(HomeViewModel.class);

        chatList.setLayoutManager(new LinearLayoutManager(this));

        // Load chats
        viewModel.loadAllChats();

        // Pass the MessageRepository and current user ID to the adapter
        MessageRepository messageRepository = ServiceModule.getMessageRepository();
        adapter = new ChatsAdapter(
                this::openChatActivity,
                viewModel,
                messageRepository,
                currentUserId
        );

        chatList.setAdapter(adapter);
        viewModel.setCurrentUserId(currentUserId);

        // Observe chat list updates
        viewModel.getChats().observe(this, this::onChatsLoaded);

        // Observe individual chat updates for more efficient UI updates
        viewModel.getChatLiveUpdate().observe(this, chatDto -> {
            if (chatDto != null) {
                // TODO adapter.updateChat(chatDto);
            }
        });

        // Observe connection mode changes
        connectionManager.connectionMode.observe(this, this::onConnectionModeChanged);
    }

    private void setupChatUpdateObservers() {
        // Observe chat updates for real-time refreshes
        ChatUpdateBus.getChatUpdates().observe(this, chatDto -> {
            if (chatDto != null) {
                // Push the update to the ViewModel
                viewModel.pushChatUpdate(chatDto);
            }
        });

        // Observe last message updates
        ChatUpdateBus.getLastMessageUpdates().observe(this, messageDto -> {
            if (messageDto != null && messageDto.getChatId() != null) {
                // Need to refresh to get updated chat data
                viewModel.updateLastMessageForChat(messageDto);
                adapter.updateLastMessage(messageDto.getChatId());
            }
        });

        // Observe unread count updates
        ChatUpdateBus.getUnreadCountUpdates().observe(this, chats -> {
            if (chats != null) {
                viewModel.refresh();
            }
        });
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
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putString(KEY_USER_ID, null);
                editor.putString(KEY_RELAY_IP, null);
                editor.putString(KEY_PORT, null);
                editor.apply();

                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    private void showMoreOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.menu_settings);

        // Get current mode
        ConnectionMode currentMode = connectionManager.connectionMode.getValue();

        // Highlight the active connection mode
        if (currentMode == ConnectionMode.DIRECT) {
            popup.getMenu().findItem(R.id.Direct_Mode).setChecked(true);
        } else if (currentMode == ConnectionMode.RELAY) {
            popup.getMenu().findItem(R.id.Relay_Mode).setChecked(true);
        }

        // Enable checkable behavior
        popup.getMenu().setGroupCheckable(0, true, true); // groupId = 0, exclusive = true

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.Direct_Mode) {
                // Stop relay connection attempts first
                stopRelayConnectionCheckLoop();

                // Switch to direct mode
                connectionManager.setConnectionMode(ConnectionMode.DIRECT);
                saveConnectionMode(ConnectionMode.DIRECT);

                // Start user discovery for P2P
                ServiceModule.startUserDiscovery();

                // Hide connection banner
                connectionStatusBanner.setVisibility(View.GONE);
                mainContentLayout.setVisibility(View.VISIBLE);
                fabNewChat.setVisibility(View.VISIBLE);

                item.setChecked(true);
                return true;
            } else if (itemId == R.id.Relay_Mode) {
                // Stop P2P discovery first
                ServiceModule.stopUserDiscovery();

                // Switch to relay mode
                connectionManager.setConnectionMode(ConnectionMode.RELAY);
                saveConnectionMode(ConnectionMode.RELAY);

                // Start relay connection check
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String relayIp = prefs.getString(KEY_RELAY_IP, "192.168.1.100");
                int port = Integer.parseInt(prefs.getString(KEY_PORT, "8081"));
                startRelayConnectionCheckLoop(relayIp, port);

                item.setChecked(true);
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
        ConnectionMode newMode = (currentMode == ConnectionMode.DIRECT) ? ConnectionMode.RELAY : ConnectionMode.DIRECT;

        if (newMode == ConnectionMode.DIRECT) {
            // Stop relay connection attempts first
            stopRelayConnectionCheckLoop();

            // Make absolutely sure the relay WebSocket is shut down
            if (ServiceModule.getRelayWebSocketClient() != null) {
                Log.i(TAG, "MainActivity explicitly shutting down relay WebSocket during mode switch");
                ServiceModule.getRelayWebSocketClient().shutdown();
            }

            // Switch to direct mode
            connectionManager.setConnectionMode(ConnectionMode.DIRECT);
            saveConnectionMode(ConnectionMode.DIRECT);

            // Start user discovery for P2P
            ServiceModule.startUserDiscovery();

            // Hide the banner and show main content
            connectionStatusBanner.setVisibility(View.GONE);
            mainContentLayout.setVisibility(View.VISIBLE);
            fabNewChat.setVisibility(View.VISIBLE);

            connectionStatusText.setText("Connected via Direct Mode");
            switchModeButton.setText("Switch to Relay Mode");
        } else {
            // Stop P2P discovery first
            ServiceModule.stopUserDiscovery();

            // Switch to relay mode
            connectionManager.setConnectionMode(ConnectionMode.RELAY);
            saveConnectionMode(ConnectionMode.RELAY);

            // If switching to relay mode, start retry loop
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String relayIp = prefs.getString(KEY_RELAY_IP, "172.20.10.7");
            int port = Integer.parseInt(prefs.getString(KEY_PORT, "8082"));

            startRelayConnectionCheckLoop(relayIp, port);
        }

        refreshData();
    }

    private void refreshData() {
        viewModel.refresh();
        // DEBUG
        // Load chats
        viewModel.loadAllChats();
        // Toast.makeText(this, "Refreshing data...", Toast.LENGTH_SHORT).show();
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

        if (mode == ConnectionMode.DIRECT) {
            // For direct mode, assume it's always working
            connectionStatusText.setText("Connected via P2P");
            switchModeButton.setText("Switch to Relay Mode");
            connectionStatusBanner.setVisibility(View.GONE);
            mainContentLayout.setVisibility(View.VISIBLE);
            fabNewChat.setVisibility(View.VISIBLE);
        } else {
            // For relay mode, show banner until connection is established
            if (!isConnectionEstablished) {
                connectionStatusText.setText("Trying to connect to Relay Server...");
                switchModeButton.setText("Switch to P2P Mode");
                connectionStatusBanner.setVisibility(View.VISIBLE);
                mainContentLayout.setVisibility(View.GONE);
                fabNewChat.setVisibility(View.GONE);
            } else {
                connectionStatusBanner.setVisibility(View.GONE);
                mainContentLayout.setVisibility(View.VISIBLE);
                fabNewChat.setVisibility(View.VISIBLE);
            }
        }
    }

    private void onChatsLoaded(List<ChatDto> chats) {
        if (chats == null || chats.isEmpty()) {
            adapter.submitList(new ArrayList<>());
        }

        Log.i(TAG, "Loaded chats: " + chats.size());

        // Update the adapter with the new chat list
        adapter.submitList(chats);

        // Load messages for each chat
        ExecutorService executor = Executors.newFixedThreadPool(4); // or newCachedThreadPool()

        for (ChatDto chat : chats) {
            if (connectionManager.connectionMode.getValue() == ConnectionMode.RELAY) {
                executor.execute(() -> {
                    try {
                        List<TextMessageDto> messages = connectionManager
                                .relayMessageService
                                .getMessagesForChat(chat.getChatId());

                        // Optionally update UI or cache here
                        Log.d("MessageFetch", "Fetched " + messages.size() + " messages for chat: " + chat.getChatId());
                    } catch (Exception e) {
                        Log.e("MessageFetch", "Error fetching messages for chat: " + chat.getChatId(), e);
                    }
                });
            }
        }
    }

    private void openChatActivity(ChatDto chat) {
        // Reset unread count for this chat when opening it
        ChatUpdateBus.postUnreadCountUpdate(chat.getChatId(), 0);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // TODO
                // ServiceModule.getChatRepository().resetUnreadCount(chat.getChatId(), currentUserId);
            } catch (Exception e) {
                Log.e(TAG, "Error resetting unread count", e);
            }
        });

        Intent intent = new Intent(this, ChatActivity.class);
        String chatName;

        if (chat.getGroup()) {
            chatName = chat.getChatName();
        } else {
            // Find the other user's name in the participants
            String otherUserId = null;
            for (String participantId : chat.getRecipients()) {
                if (!participantId.equals(currentUserId)) {
                    otherUserId = participantId;
                    break;
                }
            }
            chatName = otherUserId != null ? otherUserId : "Private Chat";
        }

        intent.putExtra("CHAT_ID", chat.getChatId());
        intent.putExtra("CHAT_NAME", chatName);
        intent.putExtra("CHAT_TYPE", chat.getGroup() ? ChatType.GROUP : ChatType.PRIVATE);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        runOnUiThread(() -> {
            if (adapter != null) {
                adapter.notifyDataSetChanged(); // 🔁 Force full rebind
            }
        });

        if (isConnectionEstablished || connectionManager.connectionMode.getValue() == ConnectionMode.DIRECT) {
            refreshData();
        }
    }

    private void initializeCoreServices(String userId, String relayIp, int port) {
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
                this,
                db
        );

        this.connectionManager = ServiceModule.getConnectionManager();
    }

    private void startRelayConnectionCheckLoop(String relayIp, int port) {
        if (connectionExecutor != null && !connectionExecutor.isShutdown()) return;

        keepTryingToConnect = true;
        isConnectionEstablished = false;

        connectionExecutor = Executors.newSingleThreadExecutor();
        connectionTask = connectionExecutor.submit(() -> {
            while (keepTryingToConnect && !Thread.currentThread().isInterrupted()) {
                connectionSocket = new Socket(); // Create a new socket for each attempt
                try {
                    String ipOnly = relayIp.replace("http://", "").replace("https://", "");
                    Log.d(TAG, "Trying to connect to relay server at " + ipOnly + ":" + port);

                    connectionSocket.connect(new InetSocketAddress(ipOnly, port), 2000);
                    connectionSocket.close();

                    Log.i(TAG, "Connected to relay server at " + ipOnly + ":" + port);
                    isConnectionEstablished = true;
                    ServiceModule.setCurrentUser(currentUserId);

                    runOnUiThread(() -> {
                        connectionStatusBanner.setVisibility(View.GONE);
                        mainContentLayout.setVisibility(View.VISIBLE);
                        fabNewChat.setVisibility(View.VISIBLE);
                        refreshData();
                    });
                    break;

                } catch (Exception e) {
                    Log.w(TAG, "Connection failed to " + relayIp + ":" + port + " - " + e.getMessage());

                    runOnUiThread(() -> {
                        connectionStatusText.setText("Unable to connect to Relay Server at " + relayIp + ":" + port);
                        switchModeButton.setText("Switch to Direct Mode");

                        connectionStatusBanner.setVisibility(View.VISIBLE);
                        mainContentLayout.setVisibility(View.GONE);
                        fabNewChat.setVisibility(View.GONE);
                    });
                } finally {
                    try {
                        if (connectionSocket != null && !connectionSocket.isClosed()) {
                            connectionSocket.close();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing socket", e);
                    }
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Retry thread interrupted during sleep");
                    Thread.currentThread().interrupt();
                    break;
                }

                if (!keepTryingToConnect || Thread.currentThread().isInterrupted()) {
                    Log.d(TAG, "Connection retry stopped.");
                    break;
                }
            }
            Log.d(TAG, "Connection retry task terminated.");
        });
    }

    private void stopRelayConnectionCheckLoop() {
        keepTryingToConnect = false;
        if (connectionExecutor != null && !connectionExecutor.isShutdown()) {
            if (connectionTask != null) {
                connectionTask.cancel(true); // Interrupt the task
            }
            try {
                if (connectionSocket != null && !connectionSocket.isClosed()) {
                    connectionSocket.close(); // Force close the socket to unblock connect()
                    Log.d(TAG, "Socket closed to interrupt connection attempt");
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket during shutdown", e);
            }
            connectionExecutor.shutdownNow(); // Forcefully stop the executor
            try {
                if (!connectionExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    Log.w(TAG, "Executor did not terminate in time");
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while awaiting executor termination", e);
            }
            connectionExecutor = null;
            connectionTask = null;
            connectionSocket = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        keepTryingToConnect = false;
        stopRelayConnectionCheckLoop();

        // Stop user discovery
        if (connectionManager != null &&
                connectionManager.connectionMode.getValue() == ConnectionMode.DIRECT) {
            ServiceModule.stopUserDiscovery();
        }

        // Cancel key rotation job
        KeyRotationJob.cancel(this);
    }

    @Override
    public void onChatCreateEvent() {
        // Refresh chat list when a new chat is created
        this.viewModel.refresh();
    }
}