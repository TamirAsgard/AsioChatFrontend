package com.example.asiochatfrontend.ui.chat;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.MediaStreamResultDto;
import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
import com.example.asiochatfrontend.core.model.enums.ChatType;
import com.example.asiochatfrontend.core.model.enums.MediaType;
import com.example.asiochatfrontend.core.service.OnWSEventCallback;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.example.asiochatfrontend.domain.usecase.chat.UpdateMessageInChatReadByUserUseCase;
import com.example.asiochatfrontend.ui.chat.adapter.MessageAdapter;
import com.example.asiochatfrontend.ui.chat.dialog.MessageOptionsDialog;
import com.example.asiochatfrontend.ui.group.GroupInfoActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChatActivity extends AppCompatActivity implements OnWSEventCallback, MessageAdapter.OnMessageLongClickListener {

    //================================================================================
    // Constants & Preferences
    //================================================================================
    private static final String TAG = "ChatActivity";
    private static final int REQUEST_SELECT_IMAGE = 1001;
    private static final int REQUEST_SELECT_FILE  = 1002;
    private static final int REQUEST_RECORD_AUDIO = 1003;
    private static final int REQUEST_CAPTURE_IMAGE = 2001;
    private static final int REQUEST_CAPTURE_VIDEO = 2002;
    private static final String PREFS_NAME        = "AsioChat_Prefs";
    private ActivityResultLauncher<Intent> groupInfoLauncher;

    //================================================================================
    // ViewModel & Adapters
    //================================================================================
    private ChatViewModel viewModel;
    private MessageAdapter messageAdapter;

    //================================================================================
    // UI Components
    //================================================================================
    private RecyclerView messageList;
    private EditText messageInput;
    private FloatingActionButton sendButton;
    private MaterialButton cameraButton, attachButton, backButton, moreButton, searchButton, upButton, downButton;
    private ShapeableImageView chatImage, sendBarImage;
    private MaterialTextView chatTitle;
    private LinearLayout respondedToLayout;
    private TextView respondedToText, statusTxt;
    private ImageView closeRespondButton, removeAttachmentButton;
    private View indicator;

    //================================================================================
    // Chat Context
    //================================================================================
    private String chatId;
    private String chatName;
    private ChatType chatType;
    private String currentUserId;
    private List<String> chatParticipants;
    private MessageDto repliedToMessage;
    private boolean isSearchMode = false;

    //================================================================================
    // Search State
    //================================================================================
    private boolean isAtBottom = false;
    private int currentSearchResultIndex = -1;
    private List<Integer> searchResultPositions = new ArrayList<>();
    private String currentSearchQuery = "";
    private boolean pendingScrollToSearch = false;
    private int scrollToPosition = -1;

    //================================================================================
    // Media Handling
    //================================================================================
    private Uri selectedMediaUri;
    private MediaType selectedMediaType;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private File audioFile;
    private boolean isRecording = false;
    private boolean isPlaying   = false;

    //================================================================================
    // Lifecycle
    //================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Retrieve Intent extras and preferences
        extractIntentData();
        loadCurrentUser();

        groupInfoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        // 1) get the new name
                        String updatedName = data.getStringExtra("CHAT_NAME");
                        if (updatedName != null && !updatedName.equals(chatName)) {
                            chatName = updatedName;
                            updateChatHeader();
                        }
                        // 2) if members changed, you could refresh your participants or ViewModel
                        boolean membersChanged = data.getBooleanExtra("MEMBERS_CHANGED", false);
                        if (membersChanged) {
                            viewModel.refresh();  // re-fetch chatData (including recipients)
                        }
                    }
                }
        );

        // Abort if chatId invalid
        if (chatId == null || chatId.isEmpty()) {
            Toast.makeText(this, "Invalid chat ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Wire up the UI & ViewModel
        initializeViews();
        setupViewModel();
        setupUIComponents();
        setupClickListeners();

        // Setup WebSocket health observer
        ServiceModule.getConnectionManager().getOnlineStatus().observe(this, isOnline -> {
            boolean isOnlineValue = Boolean.TRUE.equals(isOnline);
            setOnline(isOnlineValue);
        });

        ServiceModule.addWSEventCallback(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        markMessagesAsRead();
    }

    //================================================================================
    // Data Extraction Helpers
    //================================================================================

    private void extractIntentData() {
        Intent intent = getIntent();
        chatId      = intent.getStringExtra("CHAT_ID");
        chatName    = intent.getStringExtra("CHAT_NAME");
        String type = intent.getStringExtra("CHAT_TYPE");
        chatType    = type != null ? ChatType.valueOf(type) : ChatType.PRIVATE;
    }

    private void loadCurrentUser() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", "");
    }

    //================================================================================
    // View Initialization
    //================================================================================

    private void initializeViews() {
        messageList           = findViewById(R.id.chat_LST_messages);
        messageInput          = findViewById(R.id.chat_ET_message);
        sendButton            = findViewById(R.id.chat_FAB_send);
        cameraButton          = findViewById(R.id.chat_IB_camera);
        attachButton          = findViewById(R.id.chat_IB_attach);
        chatImage             = findViewById(R.id.chat_SIV_img);
        chatTitle             = findViewById(R.id.chat_MTV_title);
        sendBarImage          = findViewById(R.id.send_bar_SIV_img);
        backButton            = findViewById(R.id.search_BTN_back);
        searchButton          = findViewById(R.id.top_bar_BTN_search);
        upButton              = findViewById(R.id.search_BTN_up);
        downButton              = findViewById(R.id.search_BTN_down);
        moreButton            = findViewById(R.id.top_bar_chat_BTN_more);
        respondedToLayout     = findViewById(R.id.responded_to_LLO);
        respondedToText       = findViewById(R.id.responded_to_MTV);
        closeRespondButton    = findViewById(R.id.responded_to_SIV);
        removeAttachmentButton= findViewById(R.id.remove_attachment_button);
        indicator             = findViewById(R.id.connection_indicator);
        statusTxt             = findViewById(R.id.connection_status_text);

        // Initially hide the navigation buttons
        upButton.setVisibility(View.GONE);
        downButton.setVisibility(View.GONE);
    }

    private void setOnline(boolean online) {
        int color = ContextCompat.getColor(this, online ? R.color.green : R.color.red);
        indicator.getBackground().setTint(color);
        statusTxt.setText(online ? "ServerOn" : "ServerOff");
        statusTxt.setTextColor(color);
    }

    //================================================================================
    // ViewModel Setup
    //================================================================================

    private void setupViewModel() {
        ChatViewModelFactory factory = new ChatViewModelFactory(
                ServiceModule.getConnectionManager()
        );
        viewModel = new ViewModelProvider(this, factory)
                .get(ChatViewModel.class);

        viewModel.initialize(chatId, currentUserId);

        // Messages observer
        viewModel.getMessages().observe(this, messages -> {
            messageAdapter.submitList(messages);

            if (!messages.isEmpty()) {
                if (!isSearchMode) {
                    messageList.smoothScrollToPosition(messages.size() - 1);
                    }
            }
        });

        // Chat metadata observer
        viewModel.getChatData().observe(this, chat -> {
            chatParticipants = chat.getRecipients();
            chatParticipants.remove(currentUserId);
            updateChatHeader();
        });

        // Incoming message observers
        viewModel.getIncomingMessageLiveData().observe(this, this::handleIncoming);
        viewModel.getIncomingMediaLiveData().observe(this, this::handleIncoming);

        viewModel.getOutgoingMediaLiveData().observe(this, msg -> {
            if (msg != null && msg.getChatId().equals(chatId)) {
                viewModel.updateMessageInList(msg);
                viewModel.refresh();

                int count = messageAdapter.getItemCount();
                if (count > 0) {
                    if (!isSearchMode) {
                        messageList.smoothScrollToPosition(count - 1);
                    }
                }
            }
        });

        viewModel.getOutgoingMessageLiveData().observe(this, msg -> {
            if (msg != null && msg.getChatId().equals(chatId)) {
                viewModel.updateMessageInList(msg);
                viewModel.refresh();

                int count = messageAdapter.getItemCount();
                if (count > 0) {
                    if (!isSearchMode) {
                        messageList.smoothScrollToPosition(count - 1);
                    }
                }
            }
        });

        // Error observer
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private <T extends MessageDto> void handleIncoming(T msg) {
        if (msg != null && msg.getChatId().equals(chatId)) {
            viewModel.addIncomingMessage(msg);
            if (!msg.getJid().equals(currentUserId)) {
                if (msg instanceof TextMessageDto)
                    viewModel.markMessageAsRead(msg.getId(), msg.getJid());
                else
                    viewModel.markMessageAsRead(((MediaMessageDto) msg).getPayload().getId(), msg.getJid());
            }

            viewModel.refresh();

            // Defer the scroll until after the adapter has updated its list
            messageList.post(() -> {
                int lastPos = messageAdapter.getItemCount() - 1;
                if (lastPos >= 0) {
                    if (!isSearchMode) {
                        messageList.smoothScrollToPosition(lastPos);
                    }
                }
            });
        }
    }

    //================================================================================
    // UI Components Setup
    //================================================================================

    private void setupUIComponents() {
        updateChatHeader();
        respondedToLayout.setVisibility(View.GONE);
        sendBarImage.setVisibility(View.GONE);
        removeAttachmentButton.setVisibility(View.GONE);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        messageList.setLayoutManager(lm);

        messageAdapter = new MessageAdapter(
                this,
                currentUserId,
                this,
                this::openMedia
        );
        messageList.setAdapter(messageAdapter);

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                updateSendButton(!s.toString().trim().isEmpty() || selectedMediaUri != null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        updateSendButton(false);
    }

    private void updateChatHeader() {
        chatTitle.setText(
                (chatName != null && !chatName.isEmpty()) ? chatName : "Chat"
        );
        chatImage.setImageResource(
                chatType == ChatType.GROUP
                        ? R.drawable.groups_icon
                        : R.drawable.default_profile_icon
        );
    }

    //================================================================================
    // Click Listeners
    //================================================================================

    private void setupClickListeners() {
        sendButton.setOnClickListener(v -> onSendClicked());
        cameraButton.setOnClickListener(v -> showCameraChoice());
        attachButton.setOnClickListener(v -> showAttachmentOptions());
        backButton.setOnClickListener(v -> finish());
        moreButton.setOnClickListener(this::showChatOptionsMenu);
        closeRespondButton.setOnClickListener(v -> clearReplyToMessage());
        removeAttachmentButton.setOnClickListener(v -> clearSelectedMedia());
        chatImage.setOnClickListener(v -> openGroupInfo());
        chatTitle.setOnClickListener(v -> openGroupInfo());

        // Search button shows search dialog
        searchButton.setOnClickListener(v -> showSearchDialog());

        // Navigation buttons
        upButton.setOnClickListener(v -> navigateSearchResults(true));
        downButton.setOnClickListener(v -> navigateSearchResults(false));
    }

    private void onSendClicked() {
        if (isRecording) {
            stopRecording();
        } else if (selectedMediaUri != null) {
            sendMediaMessage();
        } else {
            String text = messageInput.getText().toString().trim();
            if (!text.isEmpty()) {
                sendTextMessage(text);
            } else {
                startVoiceRecording();
            }
        }
    }

    //================================================================================
    // Message Options & Menus
    //================================================================================

    private void showMessageOptions(MessageDto message) {
        MessageOptionsDialog dialog = new MessageOptionsDialog(
                this, message, new MessageOptionsDialog.OnMessageOptionSelected() {
            @Override public void onReply() {
                    setReplyToMessage(message);
                }

            // TODO implement delete in the future
//            @Override public void onDelete()  {
//                Toast.makeText(ChatActivity.this, "Delete not implemented yet",
//                        Toast.LENGTH_SHORT).show();
//            }

            // TODO implement forward in the future
//            @Override public void onForward() {
//                Toast.makeText(ChatActivity.this, "Forward not implemented yet",
//                        Toast.LENGTH_SHORT).show();
//            }

            // TODO implement resend in the future
//            @Override public void onResend()  {
//                viewModel.resendMessage(message.getId());
//            }
        }
        );
        dialog.show();
    }

    @Override
    public void onMessageLongClick(MessageDto message) {
        showMessageOptions(message);
    }

    private void openGroupInfo() {
        if (chatType == ChatType.GROUP) {
            Intent intent = new Intent(this, GroupInfoActivity.class);
            intent.putExtra("CHAT_ID", chatId);
            intent.putExtra("CHAT_NAME", chatName);
            groupInfoLauncher.launch(intent);
        }
    }

    private void showChatOptionsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.inflate(R.menu.menu_settings);
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.Direct_Mode) {
                // Switch to direct mode
                viewModel.switchConnectionMode(true);
                return true;
            } else if (itemId == R.id.Relay_Mode) {
                // Switch to relay mode
                viewModel.switchConnectionMode(false);
                return true;
            } else if (itemId == R.id.ReSync) {
                // Refresh messages
                viewModel.refresh();
                return true;
            }
            return false;
        });
        popup.show();
    }

    //================================================================================
    // Attachment & Media Handling
    //================================================================================

    private void showAttachmentOptions() {
        String[] opts = {"Gallery Image", "Document", "Voice Message", "Camera"};
        new AlertDialog.Builder(this)
                .setTitle("Choose attachment type")
                .setItems(opts, (d, which) -> {
                    switch (which) {
                        case 0: selectImage();     break;
                        case 1: selectFile();      break;
                        case 2: startVoiceRecording(); break;
                        case 3: showCameraChoice();   break;
                    }
                })
                .show();
    }

    private void showCameraChoice() {
        String[] opts = {"Take Photo", "Record Video"};
        new AlertDialog.Builder(this)
                .setTitle("Camera")
                .setItems(opts, (d, which) -> {
                    if (which == 0) capturePhoto();
                    else          recordVideo();
                })
                .show();
    }

    private void capturePhoto() {
        FileUtils fileUtils = ServiceModule.getFileUtils();
        File photoFile = fileUtils.createTempFile("media_", ".jpg");
        if (photoFile == null) return;

        Uri photoUri = fileUtils.getUriForFile(photoFile);
        selectedMediaUri  = photoUri;
        selectedMediaType = MediaType.IMAGE;

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        );
        startActivityForResult(intent, REQUEST_CAPTURE_IMAGE);
    }

    private void recordVideo() {
        FileUtils fileUtils = ServiceModule.getFileUtils();
        File videoFile = fileUtils.createTempFile("media_", ".mp4");
        if (videoFile == null) return;

        Uri videoUri = fileUtils.getUriForFile(videoFile);
        selectedMediaUri  = videoUri;
        selectedMediaType = MediaType.VIDEO;

        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        );
        intent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri);
        startActivityForResult(intent, REQUEST_CAPTURE_VIDEO);
    }

    private void selectImage() {
        startActivityForResult(
                new Intent(Intent.ACTION_PICK).setType("image/*"),
                REQUEST_SELECT_IMAGE
        );
    }

    private void selectFile() {
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .setType("*/*")
                .addCategory(Intent.CATEGORY_OPENABLE)
                .addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                );
        startActivityForResult(pick, REQUEST_SELECT_FILE);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK) return;

        if (req == REQUEST_CAPTURE_IMAGE || req == REQUEST_CAPTURE_VIDEO) {
            // selectedMediaUri is already set
            if (selectedMediaType == MediaType.IMAGE) {
                sendBarImage.setImageURI(selectedMediaUri);
            } else {
                sendBarImage.setImageResource(R.drawable.send_icon);
            }

            sendBarImage.setVisibility(View.VISIBLE);
            removeAttachmentButton.setVisibility(View.VISIBLE);
            updateSendButton(true);

        } else if (req == REQUEST_SELECT_IMAGE) {
            handleSelectedImage(data.getData());
        } else if (req == REQUEST_SELECT_FILE) {
            Uri uri = data.getData();
            if (uri != null) {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                handleSelectedFile(uri);
            }
        }
    }

    private void handleSelectedImage(Uri uri) {
        if (uri == null) return;
        selectedMediaUri  = uri;
        selectedMediaType = MediaType.IMAGE;
        sendBarImage.setImageURI(uri);
        sendBarImage.setVisibility(View.VISIBLE);
        removeAttachmentButton.setVisibility(View.VISIBLE);
        updateSendButton(true);
    }

    private void handleSelectedFile(Uri uri) {
        if (uri == null) return;
        selectedMediaUri  = uri;
        selectedMediaType = MediaType.DOCUMENT;
        sendBarImage.setImageResource(R.drawable.file_icon);
        sendBarImage.setVisibility(View.VISIBLE);
        removeAttachmentButton.setVisibility(View.VISIBLE);
        updateSendButton(true);
    }

    private void sendTextMessage(String text) {
        String replyId = (repliedToMessage != null) ? repliedToMessage.getId() : null;
        try {
            viewModel.sendTextMessage(text, replyId);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send message", e);
        }
        messageInput.setText("");
        clearReplyToMessage();
    }

    private void sendMediaMessage() {
        viewModel.sendMediaMessage(
                selectedMediaUri,
                selectedMediaType,
                messageInput.getText().toString().trim(),
                repliedToMessage != null ? repliedToMessage.getId() : null
        );
        messageInput.setText("");
        clearSelectedMedia();
        clearReplyToMessage();
    }

    private void clearSelectedMedia() {
        selectedMediaUri  = null;
        selectedMediaType = null;
        sendBarImage.setVisibility(View.GONE);
        removeAttachmentButton.setVisibility(View.GONE);
        updateSendButton(!messageInput.getText().toString().trim().isEmpty());
    }

    //================================================================================
    // Reply-to Message Helpers
    //================================================================================

    private void setReplyToMessage(MessageDto msg) {
        repliedToMessage = msg;
        respondedToLayout.setVisibility(View.VISIBLE);

        if (msg instanceof TextMessageDto) {
            respondedToText.setText(
                    ((TextMessageDto) msg).getPayload()
            );
        } else if (msg instanceof MediaMessageDto) {
            if (((MediaMessageDto) msg).getPayload() != null) {
                respondedToText.setText(
                        "[Media] " + ((MediaMessageDto) msg).getPayload().getType()
                );
            }
            else {
                respondedToText.setText("[Media]");
            }
        }
    }

    private void clearReplyToMessage() {
        repliedToMessage = null;
        respondedToLayout.setVisibility(View.GONE);
    }

    //================================================================================
    // Voice Recording & Playback
    //================================================================================

    private void startVoiceRecording() {
        if (isRecording) {
            stopRecording();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO
            );
            return;
        }
        try {
            audioFile = new File(getExternalCacheDir(),
                    "voice_" + System.currentTimeMillis() + ".m4a");
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            sendButton.setImageResource(R.drawable.send_icon);
            Toast.makeText(this, "Recording started…", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording", e);
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (!isRecording) return;
        try {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;
            updateSendButton(false);
            if (audioFile.exists()) {
                sendVoiceMessage(audioFile);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop recording", e);
            Toast.makeText(this, "Failed to stop recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendVoiceMessage(File file) {
        viewModel.sendMediaMessage(
                Uri.fromFile(file),
                MediaType.AUDIO,
                "",
                repliedToMessage != null ? repliedToMessage.getId() : null
        );
        clearReplyToMessage();
    }

    private void updateSendButton(boolean hasContent) {
        sendButton.setImageResource(
                hasContent ? R.drawable.ic_send : R.drawable.ic_mic
        );
    }

    //================================================================================
    // Media Playback Dialog
    //================================================================================

    public void openMedia(MediaStreamResultDto dto) {
        if (dto == null) {
            Log.e(TAG, "Cannot open media: dto is null");
            return;
        }
        try {
            File mediaFile = resolveMediaFile(dto);
            if (mediaFile == null) return;

            FileUtils fileUtils = ServiceModule.getFileUtils();
            Uri contentUri = fileUtils.getUriForFile(mediaFile);
            String type = dto.getContentType();
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(contentUri, type)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (type.startsWith("audio/")) {
                playAudioFile(mediaFile);
                return;
            }
            startActivity(intent);

        } catch (Exception e) {
            Log.e(TAG, "Error opening media", e);
            Toast.makeText(this, "Failed to open media file", Toast.LENGTH_SHORT).show();
        }
    }

    private File resolveMediaFile(MediaStreamResultDto dto) throws IOException {
        String path = dto.getAbsolutePath();
        FileUtils fileUtils = ServiceModule.getFileUtils();
        if (path == null || path.isEmpty()) {
            if (dto.getStream() != null) {
                return fileUtils.copyToAppStorage(
                        dto.getStream(), dto.getFileName()
                );
            }
            Toast.makeText(this, "Media stream unavailable", Toast.LENGTH_SHORT).show();
            return null;
        }
        return new File(path);
    }

    private void playAudioFile(File file) {
        try {
            AlertDialog dialog = buildAudioDialog(file);
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Failed to open audio dialog");
        }
    }

    private AlertDialog buildAudioDialog(File file) {
        AlertDialog dialog = null;

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(file.getAbsolutePath());
            mediaPlayer.prepare();

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getLayoutInflater().inflate(R.layout.dialog_audio_player, null);
            builder.setView(view);
            dialog = builder.create();

            Button playPause = view.findViewById(R.id.audio_player_play_pause);
            SeekBar seekBar = view.findViewById(R.id.audio_player_seekbar);
            TextView curTime = view.findViewById(R.id.audio_player_current_time);
            TextView totTime = view.findViewById(R.id.audio_player_total_time);

            int total = mediaPlayer.getDuration();
            seekBar.setMax(total);
            totTime.setText(formatTime(total));

            Handler handler = new Handler();
            Runnable updater = new Runnable() {
                @Override public void run() {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        int pos = mediaPlayer.getCurrentPosition();
                        seekBar.setProgress(pos);
                        curTime.setText(formatTime(pos));
                        // ✅ reschedule *this* updater again in 100ms
                        handler.postDelayed(this, 100);
                    }
                }
            };

            playPause.setOnClickListener(v -> {
                if (isPlaying) {
                    mediaPlayer.pause();
                    playPause.setText("Play");
                    handler.removeCallbacks(updater);
                } else {
                    mediaPlayer.start();
                    playPause.setText("Pause");
                    handler.post(updater);
                }
                isPlaying = !isPlaying;
            });

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar sb, int p, boolean byUser) {
                    if (byUser) {
                        mediaPlayer.seekTo(p);
                        curTime.setText(formatTime(p));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar sb) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar sb) {
                }
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                playPause.setText("Play");
                seekBar.setProgress(0);
                curTime.setText(formatTime(0));
                handler.removeCallbacks(updater);
                isPlaying = false;
            });

            dialog.setOnDismissListener(d -> {
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                    isPlaying = false;
                    handler.removeCallbacks(updater);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to build audio dialog");
        }

        return dialog;
    }

    private String formatTime(int ms) {
        int sec = (ms / 1000) % 60;
        int min = (ms / 60000) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", min, sec);
    }

    //================================================================================
    // Mark Read Use Case
    //================================================================================

    private void markMessagesAsRead() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                new UpdateMessageInChatReadByUserUseCase(
                        ServiceModule.getConnectionManager()
                ).execute(chatId, currentUserId);
                Log.d(TAG, "Messages marked as read for chat: " + chatId);
            } catch (Exception e) {
                Log.e(TAG,
                        "Failed to mark messages as read for chat: " + chatId,
                        e);
            }
        });
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search Messages");

        // Create an EditText for the dialog
        final EditText input = new EditText(this);
        input.setHint("Type to search...");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        if (!currentSearchQuery.isEmpty()) {
            input.setText(currentSearchQuery);
            input.setSelection(currentSearchQuery.length());
        }
        builder.setView(input);

        // Show keyboard automatically
        input.post(() -> {
            input.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        });

        builder.setPositiveButton("Search", (dialog, which) -> {
            String query = input.getText().toString().trim();
            if (!query.isEmpty()) {
                currentSearchQuery = query;
                isSearchMode = true;
                performSearch(query);
            }
        });

        builder.setNegativeButton("Cancel", null);

        // Add a "Clear" button if there's an existing search
        if (!currentSearchQuery.isEmpty()) {
            builder.setNeutralButton("Clear Search", (dialog, which) -> {
                clearSearch();
            });
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Perform the actual search
    private void performSearch(String query) {
        if (query.isEmpty()) {
            clearSearch();
            return;
        }

        // Reset previous search
        searchResultPositions.clear();
        currentSearchResultIndex = -1;

        // Get all messages from the ViewModel
        List<MessageDto> messages = viewModel.getMessages().getValue();
        if (messages == null || messages.isEmpty()) {
            Toast.makeText(this, "No messages to search", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find messages containing the query (case insensitive)
        String lowercaseQuery = query.toLowerCase();
        for (int i = 0; i < messages.size(); i++) {
            MessageDto message = messages.get(i);
            String messageContent = null;

            if (message instanceof TextMessageDto) {
                messageContent = ((TextMessageDto) message).getPayload();
            }

            if (messageContent != null && messageContent.toLowerCase().contains(lowercaseQuery)) {
                searchResultPositions.add(i);
            }
        }

        // Show results
        int resultsCount = searchResultPositions.size();
        if (resultsCount > 0) {
            // Show navigation buttons
            upButton.setVisibility(View.VISIBLE);
            downButton.setVisibility(View.VISIBLE);

            // Show a toast with the number of results
            Toast.makeText(this, resultsCount + " result" + (resultsCount > 1 ? "s" : "") + " found",
                    Toast.LENGTH_SHORT).show();

            // Flag that we're in search mode
            isSearchMode = true;

            // Add scroll listeners to detect when we're done scrolling
            setupSearchScrollListener();

            // Set up the initial result navigation
            currentSearchResultIndex = 0;
            forceSmoothScrollToSearchResult(currentSearchResultIndex);
        } else {
            // Hide navigation buttons
            upButton.setVisibility(View.GONE);
            downButton.setVisibility(View.GONE);

            // Show no results message
            Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show();

            // Clear any existing highlights
            if (messageAdapter != null) {
                messageAdapter.clearHighlighting();
                messageAdapter.notifyDataSetChanged();
            }
        }
    }

    private void setupSearchScrollListener() {
        // Remove any existing listeners first
        messageList.clearOnScrollListeners();

        messageList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                // Only process when scrolling has stopped
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // If we have a pending scroll to search position, handle it
                    if (pendingScrollToSearch && scrollToPosition >= 0) {
                        pendingScrollToSearch = false;

                        // After scrolling completes, check if our target item is visible
                        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                        if (layoutManager != null) {
                            int firstVisible = layoutManager.findFirstVisibleItemPosition();
                            int lastVisible = layoutManager.findLastVisibleItemPosition();

                            // If target position is visible, highlight it
                            if (scrollToPosition >= firstVisible && scrollToPosition <= lastVisible) {
                                View targetView = layoutManager.findViewByPosition(scrollToPosition);
                                if (targetView != null) {
                                    // Apply highlight animation
                                    applyPulseAnimationToView(targetView);
                                }
                            } else {
                                // If not visible, we need to fine-tune our scroll
                                adjustScrollPosition(scrollToPosition);
                            }
                        }
                    }
                }
            }
        });
    }

    // Method to force smooth scrolling to search results with better positioning
    private void forceSmoothScrollToSearchResult(int resultIndex) {
        if (resultIndex < 0 || resultIndex >= searchResultPositions.size()) {
            return;
        }

        // Get the position to scroll to
        int messagePosition = searchResultPositions.get(resultIndex);
        scrollToPosition = messagePosition;

        // Update the highlight in the adapter
        messageAdapter.setHighlightedPosition(messagePosition, currentSearchQuery);
        messageAdapter.notifyDataSetChanged();

        // Flag that we have a pending scroll operation
        pendingScrollToSearch = true;

        // First, we need to break out of any "stuck at bottom" state
        // The most reliable way is to scroll to the top first
        messageList.scrollToPosition(0);

        // After a brief delay, scroll to our target position
        messageList.postDelayed(() -> {
            // Get layout manager
            LinearLayoutManager layoutManager = (LinearLayoutManager) messageList.getLayoutManager();
            if (layoutManager == null) return;

            // Scroll to position - this is a rough scroll that will get us to the general area
            messageList.smoothScrollToPosition(messagePosition);
        }, 100);
    }

    // Method to fine-tune scroll position once the initial scroll completes
    private void adjustScrollPosition(int position) {
        // Get layout manager
        LinearLayoutManager layoutManager = (LinearLayoutManager) messageList.getLayoutManager();
        if (layoutManager == null) return;

        // Find the view for our target position
        View targetView = layoutManager.findViewByPosition(position);
        if (targetView == null) {
            // If view not found, try once more with a precise scroll
            layoutManager.scrollToPositionWithOffset(position, messageList.getHeight() / 3);

            // Wait for layout, then try to find the view again
            messageList.post(() -> {
                View finalView = layoutManager.findViewByPosition(position);
                if (finalView != null) {
                    // Calculate current position
                    int visibleHeight = messageList.getHeight();
                    int viewTop = finalView.getTop();
                    int viewHeight = finalView.getHeight();

                    // Determine if we need to adjust position
                    // We want the view to be about 1/3 from the bottom of the screen
                    int targetTop = (visibleHeight * 2 / 3) - (viewHeight / 2);

                    // Only scroll if necessary
                    if (Math.abs(viewTop - targetTop) > 50) {
                        messageList.smoothScrollBy(0, viewTop - targetTop);
                    }

                    // Highlight the view
                    applyPulseAnimationToView(finalView);
                }
            });
        } else {
            // View is already visible, calculate if it's in a good position
            int visibleHeight = messageList.getHeight();
            int viewTop = targetView.getTop();
            int viewHeight = targetView.getHeight();

            // Ideal position is about 1/3 from the bottom
            int targetTop = (visibleHeight * 2 / 3) - (viewHeight / 2);

            // Only scroll if the view isn't already in a good position
            if (Math.abs(viewTop - targetTop) > 50) {
                // Smooth scroll to center the view
                messageList.smoothScrollBy(0, viewTop - targetTop);

                // Wait for scroll to complete, then highlight
                messageList.postDelayed(() -> applyPulseAnimationToView(targetView), 250);
            } else {
                // Already in a good position, just highlight
                applyPulseAnimationToView(targetView);
            }
        }
    }

    private void setupDefaultScrollListener() {
        messageList.clearOnScrollListeners();
        messageList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                // Add your normal chat scrolling behavior here
            }
        });
    }

    // Clear search state
    private void clearSearch() {
        if (!isSearchMode) {
            return;
        }

        // Reset search state
        searchResultPositions.clear();
        currentSearchResultIndex = -1;
        currentSearchQuery = "";
        isSearchMode = false;
        pendingScrollToSearch = false;
        scrollToPosition = -1;

        // Hide navigation buttons
        upButton.setVisibility(View.GONE);
        downButton.setVisibility(View.GONE);

        // Remove search-specific scroll listener
        messageList.clearOnScrollListeners();

        // Clear highlighting
        if (messageAdapter != null) {
            messageAdapter.clearHighlighting();
            messageAdapter.notifyDataSetChanged();
        }

        // Re-establish normal scroll behavior
        setupDefaultScrollListener();

        // Return to the bottom of the chat
        List<MessageDto> messages = viewModel.getMessages().getValue();
        if (messages != null && !messages.isEmpty()) {
            messageList.smoothScrollToPosition(messages.size() - 1);
        }

        Toast.makeText(this, "Search cleared", Toast.LENGTH_SHORT).show();
    }

    // Navigate between search results
    private void navigateSearchResults(boolean navigateUp) {
        if (searchResultPositions.isEmpty()) {
            return;
        }

        if (navigateUp) {
            // Move to previous result
            currentSearchResultIndex--;
            if (currentSearchResultIndex < 0) {
                currentSearchResultIndex = searchResultPositions.size() - 1;
                Toast.makeText(this, "Wrapped to last result", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Move to next result
            currentSearchResultIndex++;
            if (currentSearchResultIndex >= searchResultPositions.size()) {
                currentSearchResultIndex = 0;
                Toast.makeText(this, "Wrapped to first result", Toast.LENGTH_SHORT).show();
            }
        }

        // Navigate to the new result position
        forceSmoothScrollToSearchResult(currentSearchResultIndex);
    }

    private void applyPulseAnimationToView(View view) {
        if (view == null || !view.isAttachedToWindow()) return;

        // Instead of changing background, apply a subtle scale animation
        // to draw attention to the already highlighted message
        view.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(200)
                .withEndAction(() -> {
                    if (view.isAttachedToWindow()) {
                        view.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(200)
                                .start();
                    }
                })
                .start();

        // Also briefly change alpha for additional visual indication
        view.animate()
                .alpha(0.7f)
                .setDuration(150)
                .withEndAction(() -> {
                    if (view.isAttachedToWindow()) {
                        view.animate()
                                .alpha(1.0f)
                                .setDuration(150)
                                .start();
                    }
                })
                .start();
    }

    //================================================================================
    // Callbacks
    //================================================================================

    @Override
    public void onChatCreateEvent(List<ChatDto> chats) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Fetch chats from the database
                List<ChatDto> newChatList = ServiceModule.getRelayChatService().getChatsForUser(currentUserId);
                for (ChatDto chatDto : newChatList) {
                    if (chatDto.getChatId().equals(chatId)) {
                        chatName = chatDto.getChatName();
                        runOnUiThread(() -> updateChatHeader());
                        break;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch chat data", e);
            }
        });
    }


    @Override
    public void onPendingMessagesSendEvent(List<MessageDto> messages) {
        messages.forEach(viewModel::updateMessageInList);
        viewModel.refresh();
    }

    @Override
    public void onRemovedFromChat(String chatId) {
        if (!chatId.equals(this.chatId)) return;

        runOnUiThread(() -> {
            Toast.makeText(ChatActivity.this,
                            "You have been removed from the chat",
                            Toast.LENGTH_SHORT)
                    .show();
            finish();
        });
    }

    @Override
    public void onDestroy() {
        markMessagesAsRead();
        super.onDestroy();
    }

    // Override back button to handle search mode
    @Override
    public void onBackPressed() {
        if (isSearchMode) {
            clearSearch();
        } else {
            super.onBackPressed();
        }
    }
}
