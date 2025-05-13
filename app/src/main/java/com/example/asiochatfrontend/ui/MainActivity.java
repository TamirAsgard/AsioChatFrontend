package com.example.asiochatfrontend.ui;

import static android.widget.Toast.LENGTH_SHORT;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.multidex.BuildConfig;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.app.di.DatabaseModule;
import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.connection.ConnectionManager;
import com.example.asiochatfrontend.core.connection.ConnectionMode;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
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
import com.example.asiochatfrontend.data.relay.network.RelayWebSocketClient;
import com.example.asiochatfrontend.data.relay.network.WebSocketHealthMonitor;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * MainActivity hosts the chat list and manages connection modes: Relay, Direct, Offline.
 * It also observes WebSocket health and displays a banner when relay mode is disconnected.
 */
public class MainActivity extends AppCompatActivity implements OnWSEventCallback {

    private static final String TAG = "MainActivity";
    private static final String PREFS_NAME = "AsioChat_Prefs";
    private static final String KEY_CONNECTION_MODE = "connection_mode";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_RELAY_IP = "relay_ip";
    private static final String KEY_PORT = "port";
    private static final Logger log = LoggerFactory.getLogger(MainActivity.class);

    //=== UI & ViewModel ===
    private LinearLayout mainContentLayout;
    private RecyclerView chatList;
    private ChatsAdapter adapter;
    private HomeViewModel viewModel;
    private FloatingActionButton fabNewChat;
    private MaterialButton btnAll, btnUnread;
    private MaterialButton searchButton, moreButton;

    //=== Connection Banner ===
    private View connectionStatusBanner, indicator;
    private TextView connectionStatusText, status;
    private Button switchModeButton, backToLoginButton;

    //=== Core & State ===
    private ConnectionManager connectionManager;
    private String currentUserId;
    private String relayHost;
    private String relayPort;
    private boolean isInitialLoadDone = false;

    //=== TCP Relay Check Loop ===
    private volatile boolean keepTryingToConnect = false;
    private volatile boolean isConnectionEstablished = false;
    private ExecutorService connectionExecutor;
    private Future<?> connectionTask;
    private Socket connectionSocket;

    //=== WebSocket Health Monitoring ===
    private RelayWebSocketClient relayWebSocketClient;
    private WebSocketHealthMonitor webSocketHealthMonitor;

    //==========================================================================
    // Activity Lifecycle
    //==========================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentUserId = prefs.getString(KEY_USER_ID, null);
        relayHost     = prefs.getString(KEY_RELAY_IP, null);
        relayPort     = prefs.getString(KEY_PORT,    null);

        // If missing, redirect to login
        if (currentUserId == null || relayHost == null || relayPort == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Prepare UI and ViewModel
        initializeViews();
        initializeCoreServices(currentUserId, relayHost, Integer.parseInt(relayPort));
        setupViewModel();
        setupClickListeners();
        setupChatUpdateObservers();

        // Resume saved mode
        setOnline(Boolean.TRUE.equals(connectionManager.getOnlineStatus().getValue()));
        String modeName = prefs.getString(KEY_CONNECTION_MODE, ConnectionMode.RELAY.name());
        ConnectionMode mode = ConnectionMode.valueOf(modeName);
        connectionManager.setConnectionMode(mode);

        if (mode == ConnectionMode.RELAY) {
            startRelayConnectionCheckLoop(relayHost, Integer.parseInt(relayPort));
        } else if (mode == ConnectionMode.DIRECT) {
            ServiceModule.startUserDiscovery();
        }

        // Setup WebSocket health observer
        connectionManager.getOnlineStatus().observe(this, isOnline -> {
            boolean isOnlineValue = Boolean.TRUE.equals(isOnline);
            setOnline(isOnlineValue);
            if (isOnlineValue) {
                onWebSocketConnected();
            } else {
                onRelayConnectionLost();
            }
        });

        // Delay adapter refresh to allow data loading
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (adapter != null) adapter.notifyDataSetChanged();
        }, 500);

        // Schedule security jobs
        KeyRotationJob.schedule(this, currentUserId);
        OnStartUserKeyInitialization.executePublicKeyInitialization(
                ServiceModule.getConnectionManager().relayAuthService,
                ServiceModule.getRelayApiClient()
        );

        // Handle skipLogin if coming from unreachable relay
        boolean skipLogin = getIntent().getBooleanExtra("skipUserInit", false);
        if (skipLogin) {
            enterRelayBannerState();
            setOnline(false);
            onRelayConnectionLost();
        }

        TextView versionText = findViewById(R.id.version_text);
        versionText.setText(getString(R.string.version_format, "1.0"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop TCP relay loop
        keepTryingToConnect = false;
        stopRelayConnectionCheckLoop();

        // Stop P2P discovery if needed
        if (connectionManager != null
                && connectionManager.connectionMode.getValue() == ConnectionMode.DIRECT) {
            ServiceModule.stopUserDiscovery();
        }

        // Cancel security jobs
        KeyRotationJob.cancel(this);
    }

    //==========================================================================
    // Initialization Helpers
    //==========================================================================

    /** Binds view references */
    private void initializeViews() {
        mainContentLayout    = findViewById(R.id.mainContentLayout);
        chatList             = findViewById(R.id.main_LST_chats);
        fabNewChat           = findViewById(R.id.fab_new_chat);
        btnAll               = findViewById(R.id.button_all);
        btnUnread            = findViewById(R.id.button_unread);
        searchButton         = findViewById(R.id.top_bar_BTN_search);
        moreButton           = findViewById(R.id.top_bar_BTN_more);
        connectionStatusBanner = findViewById(R.id.connectionStatusBanner);
        connectionStatusText = connectionStatusBanner.findViewById(R.id.connectionStatusText);
        switchModeButton     = connectionStatusBanner.findViewById(R.id.switchToPrivateMeshButton);
        backToLoginButton    = connectionStatusBanner.findViewById(R.id.backToLogin);
        indicator            = findViewById(R.id.connection_indicator);
        status               = findViewById(R.id.connection_status_text);
    }

    private void setOnline(boolean online) {
        int color = ContextCompat.getColor(this, online ? R.color.green : R.color.red);
        indicator.getBackground().setTint(color);
        status.setText(online ? "Online" : "Offline");
        status.setTextColor(color);
    }

    /** Sets up data repositories and core services */
    private void initializeCoreServices(
            String userId,
            String relayIp,
            int port
    ) {
        AppDatabase db = DatabaseModule.initialize(this);
        ChatRepository    chatRepo   = new ChatRepositoryImpl(db.chatDao());
        MessageRepository msgRepo    = new MessageRepositoryImpl(db.messageDao());
        MediaRepository   mediaRepo  = new MediaRepositoryImpl(
                db.mediaDao(), msgRepo, new FileUtils(this)
        );
        UserRepository    userRepo   = new UserRepositoryImpl(db.userDao());

        if (!relayIp.startsWith("http://") && !relayIp.startsWith("https://")) {
            relayIp = "http://" + relayIp;
        }

        ServiceModule.initialize(
                this,
                chatRepo,
                msgRepo,
                mediaRepo,
                userRepo,
                userId,
                relayIp,
                port,
                db
        );

        connectionManager = ServiceModule.getConnectionManager();
        ServiceModule.addWSEventCallback(this);
    }

    /** Configures the RecyclerView and ViewModel */
    private void setupViewModel() {
        HomeViewModelFactory factory = new HomeViewModelFactory(
                connectionManager, currentUserId
        );
        viewModel = new ViewModelProvider(this, factory)
                .get(HomeViewModel.class);

        chatList.setLayoutManager(new LinearLayoutManager(this));
        viewModel.loadAllChats();

        MessageRepository msgRepo = ServiceModule.getMessageRepository();
        adapter = new ChatsAdapter(
                this::openChatActivity,
                viewModel,
                msgRepo,
                currentUserId
        );

        chatList.setAdapter(adapter);
        viewModel.setCurrentUserId(currentUserId);

        viewModel.getChats()
                .observe(this, this::onChatsLoaded);

        viewModel.getChatLiveUpdate()
                .observe(this, dto -> {/* update single chat */});

        connectionManager.connectionMode
                .observe(this, this::onConnectionModeChanged);
    }

    /** Sets up button click listeners and menu */
    private void setupClickListeners() {
        fabNewChat.setOnClickListener(v ->
                startActivity(new Intent(this, NewChatActivity.class))
        );

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

        searchButton.setOnClickListener(this::showSearchOptionsMenu);
        moreButton.setOnClickListener(this::showMoreOptionsMenu);

        switchModeButton.setOnClickListener(v -> switchToOfflineMode());
        backToLoginButton.setOnClickListener(v -> backToLogin());
    }

    /** Observes chat updates from ChatUpdateBus */
    private void setupChatUpdateObservers() {
        if (viewModel != null)
            viewModel.loadAllChats();

        ChatUpdateBus.getChatUpdates()
                .observe(this, update -> {
                    if (update != null && update.getChatId() != null) {
                        Log.d(TAG, "Chat update: " + update);
                        viewModel.pushChatUpdate(update);
                    }
                });

        ChatUpdateBus.getLastMessageUpdates()
                .observe(this, msg -> {
                    if (msg != null && msg.getId() != null) {
                        Log.d(TAG, "Last message update: " + msg);
                        // validate message payload isn't null or empty
                        boolean hasPayload = false;
                        if (msg instanceof TextMessageDto) {
                            hasPayload = ((TextMessageDto) msg).getPayload() != null;
                        } else if (msg instanceof MediaMessageDto) {
                            hasPayload = ((MediaMessageDto) msg).getPayload() != null;
                        }
                        if (hasPayload) {
                            adapter.updateLastMessage(msg.chatId);
                        }
                    }
                });

        ChatUpdateBus.getUnreadCountUpdates().observeForever(unreadMap -> {
            if (unreadMap != null && !unreadMap.isEmpty()) {
                Log.d(TAG, "Received unread count update: " + unreadMap.size() + " chats");
                for (Map.Entry<String,Integer> e : unreadMap.entrySet()) {
                    adapter.updateUnreadCount(e.getKey());
                }
            }
        });
    }

    //==========================================================================
    // WebSocket Health Monitoring
    //==========================================================================

    /** Called when WebSocket successfully connects or reconnects. */
    private void onWebSocketConnected() {
        // if we already knew we were online, do nothing
        if (isConnectionEstablished) return;

        runOnUiThread(() -> {
            // now mark ourselves online
            isConnectionEstablished = true;

            // tear down any TCP retry loop
            stopRelayConnectionCheckLoop();

            // hide the “can’t reach server” banner
            connectionStatusBanner.setVisibility(View.GONE);
            mainContentLayout.setVisibility(View.VISIBLE);
            fabNewChat.setVisibility(View.VISIBLE);

            // refresh chat list
            refreshData();
        });
    }

    /** Called when WebSocket connection is lost. */
    public void onRelayConnectionLost() {
        if (connectionManager.connectionMode.getValue() != ConnectionMode.RELAY
                || !isConnectionEstablished) {
            return;
        }

        Log.e(TAG, "Relay connection lost");

        runOnUiThread(() -> {
            isConnectionEstablished = false;
            startRelayConnectionCheckLoop(relayHost, Integer.parseInt(relayPort));
        });
    }

    //==========================================================================
    // Relay TCP Ping Loop
    //==========================================================================

    private void startRelayConnectionCheckLoop(String host, int port) {
        if (connectionExecutor != null && !connectionExecutor.isShutdown()) return;
        keepTryingToConnect = true;
        isConnectionEstablished = false;
        connectionExecutor = Executors.newSingleThreadExecutor();
        connectionTask = connectionExecutor.submit(() -> {
            try {
                while (keepTryingToConnect && !Thread.currentThread().isInterrupted()) {
                    try (Socket sock = new Socket()) {
                        Log.d(TAG, "Checking relay " + host + ":" + port);
                        sock.connect(new InetSocketAddress(host, port), 2000);

                        Log.i(TAG, "Relay reachable");
                        onWebSocketConnected();
                        break;
                    } catch (IOException ignored) {
                        // Relay not reachable
                    }
                    Thread.sleep(5000);
                }
            } catch (InterruptedException ignored) {
            } finally {
                stopRelayConnectionCheckLoop();
            }
        });
    }

    private void stopRelayConnectionCheckLoop() {
        keepTryingToConnect = false;
        if (connectionExecutor != null) {
            connectionExecutor.shutdownNow();
            connectionExecutor = null;
            connectionTask = null;
        }
    }

    //==========================================================================
    // Connection Banner Helpers
    //==========================================================================

    private void enterRelayBannerState() {
        connectionManager.setConnectionMode(ConnectionMode.RELAY);
        isConnectionEstablished = false;
        updateConnectionBanner(ConnectionMode.RELAY);
        startRelayConnectionCheckLoop(relayHost, Integer.parseInt(relayPort));
    }

    private void onConnectionModeChanged(ConnectionMode mode) {
        updateConnectionBanner(mode);
    }

    private void updateConnectionBanner(ConnectionMode mode) {
        if (mode == ConnectionMode.OFFLINE) {
            connectionStatusBanner.setVisibility(View.GONE);
            mainContentLayout.setVisibility(View.VISIBLE);
            fabNewChat.setVisibility(View.VISIBLE);
            return;
        }

        if (!isConnectionEstablished && mode == ConnectionMode.RELAY) {
            connectionStatusText.setText(
                    "Unable to connect to Relay Server at " + relayHost + ":" + relayPort
            );
            switchModeButton.setText("Continue Offline");
            connectionStatusBanner.setVisibility(View.VISIBLE);
            mainContentLayout.setVisibility(View.GONE);
            fabNewChat.setVisibility(View.GONE);
        }
    }

    private void switchToOfflineMode() {
        stopRelayConnectionCheckLoop();
        ServiceModule.stopUserDiscovery();
        saveConnectionMode(ConnectionMode.OFFLINE);
        updateConnectionBanner(ConnectionMode.OFFLINE);
    }

    private void backToLogin() {
        SharedPreferences.Editor editor =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void showMoreOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.menu_settings);
        popup.getMenu().setGroupCheckable(0, true, true);
        popup.getMenu().findItem(
                connectionManager.connectionMode.getValue() == ConnectionMode.DIRECT
                        ? R.id.Direct_Mode : R.id.Relay_Mode
        ).setChecked(true);
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.Direct_Mode) {
                connectionManager.setConnectionMode(ConnectionMode.DIRECT);
                saveConnectionMode(ConnectionMode.DIRECT);
                ServiceModule.startUserDiscovery();
                updateConnectionBanner(ConnectionMode.DIRECT);
            } else if (id == R.id.Relay_Mode) {
                connectionManager.setConnectionMode(ConnectionMode.RELAY);
                saveConnectionMode(ConnectionMode.RELAY);
                startRelayConnectionCheckLoop(relayHost, Integer.parseInt(relayPort));
            } else if (id == R.id.ReSync) {
                refreshData();
            }
            return true;
        });
        popup.show();
    }

    private void showSearchOptionsMenu(View anchor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search Chats");

        // Create an EditText and set it as the dialog’s view
        final EditText input = new EditText(this);
        input.setHint("Type to search…");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Search", (dialog, which) -> {
            String query = input.getText().toString().trim();
            viewModel.filterChats(query);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void saveConnectionMode(ConnectionMode mode) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit().putString(KEY_CONNECTION_MODE, mode.name()).apply();
    }

    //==========================================================================
    // Chat & Navigation Helpers
    //==========================================================================

    private void onChatsLoaded(List<ChatDto> chats) {
        // Insert all chats into the adapter
        adapter.submitList(chats != null ? chats : new ArrayList<>());

        // Initialize values in the ChatBus
        if (chats != null && !isInitialLoadDone) {
            ExecutorService executor = Executors.newSingleThreadExecutor();

            for (ChatDto chat : chats) {
                // capture chatId for the lambda
                final String chatId = chat.getChatId();

                executor.execute(() -> {
                    // fetch last message + unread count
                    MessageDto lastMessage = connectionManager.getLastMessageForChat(chatId);
                    int unreadCount = connectionManager.getUnreadMessagesCount(chatId, currentUserId);

                    // post the unread count update
                    ChatUpdateBus.postUnreadCountUpdate(chatId, unreadCount);

                    // only post a last-message update if it actually has some payload
                    if (lastMessage != null) {
                        boolean hasPayload = false;

                        if (lastMessage instanceof TextMessageDto) {
                            hasPayload = ((TextMessageDto) lastMessage).getPayload() != null;

                        } else if (lastMessage instanceof MediaMessageDto) {
                            hasPayload = ((MediaMessageDto) lastMessage).getPayload() != null;
                        }

                        if (hasPayload) {
                            ChatUpdateBus.postLastMessageUpdate(lastMessage);
                        }
                    }
                });

                isInitialLoadDone = true;
            }
        }
    }

    private void openChatActivity(ChatDto chat) {
        ChatUpdateBus.postUnreadCountUpdate(chat.getChatId(), 0);
        Intent intent = new Intent(this, ChatActivity.class);
        String chatName = chat.getGroup() ? chat.getChatName()
                : chat.getRecipients().stream()
                .filter(id -> !id.equals(currentUserId))
                .findFirst().orElse("Private Chat");
        intent.putExtra("CHAT_ID", chat.getChatId());
        intent.putExtra("CHAT_NAME", chatName);
        intent.putExtra("CHAT_TYPE",
                chat.getGroup() ? "GROUP" : "PRIVATE");
        startActivity(intent);
    }

    private void initializeUnreadCounts() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<ChatDto> chats = ServiceModule.getChatRepository().getChatsForUser(currentUserId);
                if (chats != null) {
                    for (ChatDto chat : chats) {
                        int unreadCount = connectionManager.getUnreadMessagesCount(chat.getChatId(), currentUserId);
                        ChatUpdateBus.postUnreadCountUpdate(chat.getChatId(), unreadCount);
                        Log.d(TAG, "Initialized unread count for chat " + chat.getChatId() + ": " + unreadCount);

                        // Force refresh UI on main thread
                        runOnUiThread(() -> {
                            if (adapter != null) {
                                adapter.updateUnreadCount(chat.getChatId());
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing unread counts", e);
            }
        });
    }

    private void initializeLastMessages() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<ChatDto> chats = ServiceModule.getChatRepository().getChatsForUser(currentUserId);
                if (chats != null) {
                    for (ChatDto chat : chats) {
                        MessageDto lastMessage = connectionManager.getLastMessageForChat(chat.getChatId());
                        if (lastMessage != null) {
                            // only post a last-message update if it actually has some payload
                            boolean hasPayload = false;
                            if (lastMessage instanceof TextMessageDto) {
                                hasPayload = ((TextMessageDto) lastMessage).getPayload() != null;
                            } else if (lastMessage instanceof MediaMessageDto) {
                                hasPayload = ((MediaMessageDto) lastMessage).getPayload() != null;
                            }
                            if (hasPayload) {
                                ChatUpdateBus.postLastMessageUpdate(lastMessage);
                            }
                            Log.d(TAG, "Initialized last message for chat " + chat.getChatId() + ": " + lastMessage.getId());
                        }

                        // Force refresh UI on main thread
                        runOnUiThread(() -> {
                            if (adapter != null) {
                                adapter.updateUnreadCount(chat.getChatId());
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing unread counts", e);
            }
        });
    }

    private void refreshData() {
        viewModel.refresh();
        viewModel.loadAllChats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) {
            // Initialize unread counts after everything else is set up
            initializeUnreadCounts();
            initializeLastMessages();
            adapter.notifyDataSetChanged();
        }
        if (isConnectionEstablished
                || connectionManager.connectionMode.getValue() == ConnectionMode.DIRECT) {
            refreshData();
        }
    }

    @Override
    public void onChatCreateEvent(List<ChatDto> chats) {
        refreshData();
    }

    @Override
    public void onPendingMessagesSendEvent(List<MessageDto> messages) {
        refreshData();
    }

    @Override
    public void onRemovedFromChat(String chatId) { refreshData();}
}
