package com.example.asiochatfrontend.ui.chat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
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
import com.example.asiochatfrontend.core.model.dto.MediaStreamResultDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
import com.example.asiochatfrontend.core.model.enums.ChatType;
import com.example.asiochatfrontend.core.model.enums.MediaType;
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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private static final int REQUEST_SELECT_IMAGE = 1001;
    private static final int REQUEST_SELECT_FILE = 1002;
    private static final int REQUEST_RECORD_AUDIO = 1003;
    private static final String PREFS_NAME = "AsioChat_Prefs";

    private ChatViewModel viewModel;
    private RecyclerView messageList;
    private MessageAdapter messageAdapter;
    private EditText messageInput;
    private FloatingActionButton sendButton;
    private MaterialButton cameraButton, attachButton;
    private ShapeableImageView chatImage, sendBarImage;
    private MaterialTextView chatTitle;
    private MaterialButton searchButton, moreButton;
    private MaterialButton backButton;
    private View connectionStatusBanner;
    private LinearLayout respondedToLayout;
    private MaterialTextView respondedToText;
    private ImageView closeRespondButton;
    private ImageView removeAttachmentButton;

    private String chatId;
    private String chatName;
    private ChatType chatType;
    private String currentUserId;
    private List<String> chatParticipants;
    private MessageDto repliedToMessage;
    private Uri selectedMediaUri;
    private MediaType selectedMediaType;

    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private File audioFile;
    private boolean isRecording = false;
    private boolean isPlaying = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get intent data
        chatId = getIntent().getStringExtra("CHAT_ID");
        chatName = getIntent().getStringExtra("CHAT_NAME");
        String chatTypeStr = getIntent().getStringExtra("CHAT_TYPE");
        if (chatTypeStr != null) {
            chatType = ChatType.valueOf(chatTypeStr);
        } else {
            chatType = ChatType.PRIVATE;
        }

        // Get current user ID from shared preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", "");

        if (chatId == null || chatId.isEmpty()) {
            Toast.makeText(this, "Invalid chat ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Set up view model
        setupViewModel();

        // Set up UI components
        setupUIComponents();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        messageList = findViewById(R.id.chat_LST_messages);
        messageInput = findViewById(R.id.chat_ET_message);
        sendButton = findViewById(R.id.chat_FAB_send);
        cameraButton = findViewById(R.id.chat_IB_camera);
        attachButton = findViewById(R.id.chat_IB_attach);
        chatImage = findViewById(R.id.chat_SIV_img);
        chatTitle = findViewById(R.id.chat_MTV_title);
        sendBarImage = findViewById(R.id.send_bar_SIV_img);
        backButton = findViewById(R.id.search_BTN_back);
        moreButton = findViewById(R.id.top_bar_chat_BTN_more);
        connectionStatusBanner = findViewById(R.id.connectionStatusBanner);
        respondedToLayout = findViewById(R.id.responded_to_LLO);
        respondedToText = findViewById(R.id.responded_to_MTV);
        closeRespondButton = findViewById(R.id.responded_to_SIV);
        removeAttachmentButton = findViewById(R.id.remove_attachment_button);
    }

    private void setupViewModel() {
        ChatViewModelFactory factory = new ChatViewModelFactory(ServiceModule.getConnectionManager());
        viewModel = new ViewModelProvider(this, factory).get(ChatViewModel.class);
        viewModel.initialize(chatId, currentUserId);

        viewModel.getMessages().observe(this, messages -> {
            messageAdapter.submitList(messages);
            if (!messages.isEmpty()) {
                messageList.smoothScrollToPosition(messages.size() - 1);
            }
        });

        viewModel.getChatData().observe(this, chat -> {
            chatParticipants = chat.getRecipients();
            chatParticipants.remove(currentUserId);

            if (chat.getGroup()) {
                chatName = chat.getChatName();
            } else {
                chatName = chatParticipants.get(0);
            }

            updateChatHeader();

            // Rebind incoming observer
            viewModel.getIncomingMessageLiveData().observe(this, newMessage -> {
                if (newMessage != null && newMessage.getChatId().equals(chat.getChatId())) {
                    viewModel.addIncomingMessage(newMessage);

                    // ✅ Mark as read (local + WebSocket)
                    if (!newMessage.getJid().equals(currentUserId)) {
                        viewModel.markMessageAsRead(newMessage.getId());
                    }

                    // Force refresh just in case (especially after reentry)
                    viewModel.refresh();

                    if (messageAdapter.getItemCount() > 0) {
                        messageList.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                    }
                }
            });

            // Rebind outgoing observer
            viewModel.getOutgoingMessageLiveData().observe(this, newMessage -> {
                if (newMessage != null && newMessage.getChatId().equals(chat.getChatId())) {
                    viewModel.updateMessageInList(newMessage);
                    viewModel.refresh();
                    if (messageAdapter.getItemCount() > 0) {
                        messageList.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                    }
                }
            });
        });

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupUIComponents() {
        // Set chat title and image
        updateChatHeader();

        // Hide respond to layout initially
        respondedToLayout.setVisibility(View.GONE);
        sendBarImage.setVisibility(View.GONE);
        removeAttachmentButton.setVisibility(View.GONE);

        // Set up RecyclerView for messages
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        messageList.setLayoutManager(layoutManager);

        messageAdapter = new MessageAdapter(
                currentUserId,
                this::showMessageOptions,
                this::openMedia
        );

        messageList.setAdapter(messageAdapter);

        // Change send button icon based on input
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendButton(!s.toString().trim().isEmpty() || selectedMediaUri != null);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Initially set to mic icon
        updateSendButton(false);
    }

    private void updateChatHeader() {
        chatTitle.setText(chatName != null && !chatName.isEmpty() ? chatName : "Chat");

        // Set appropriate icon based on chat type
        if (chatType == ChatType.GROUP) {
            chatImage.setImageResource(R.drawable.groups_icon);
        } else {
            chatImage.setImageResource(R.drawable.default_profile_icon);
        }
    }

    private void setupClickListeners() {
        // Send button
        sendButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            }
            else if (selectedMediaUri != null) {
                sendMediaMessage();
            } else {
                String text = messageInput.getText().toString().trim();
                if (!text.isEmpty()) {
                    sendTextMessage(text);
                } else {
                    // No text and no media, start voice recording
                    startVoiceRecording();
                }
            }
        });

        // Camera button
        cameraButton.setOnClickListener(v -> selectImage());

        // Attach button
        attachButton.setOnClickListener(v -> showAttachmentOptions());

        // More button
        if (moreButton != null) {
            moreButton.setOnClickListener(v -> showChatOptionsMenu(v));
        }

        // Back button
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Close reply button
        closeRespondButton.setOnClickListener(v -> clearReplyToMessage());

        // Remove attachment button
        removeAttachmentButton.setOnClickListener(v -> clearSelectedMedia());

        // Chat image click (for group info)
        chatImage.setOnClickListener(v -> {
            if (chatType == ChatType.GROUP) {
                openGroupInfo();
            }
        });

        // Chat title click (for group info)
        chatTitle.setOnClickListener(v -> {
            if (chatType == ChatType.GROUP) {
                openGroupInfo();
            }
        });
    }

    private void showMessageOptions(MessageDto message) {
        // Show popup with options for the message
        MessageOptionsDialog dialog = new MessageOptionsDialog(this, message, new MessageOptionsDialog.OnMessageOptionSelected() {
            @Override
            public void onReply() {
                if (message instanceof MediaMessageDto)
                    setReplyToMessage((MediaMessageDto) message);
            }

            @Override
            public void onDelete() {
                // Not implemented in this version
                Toast.makeText(ChatActivity.this, "Delete not implemented yet", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onForward() {
                // Not implemented in this version
                Toast.makeText(ChatActivity.this, "Forward not implemented yet", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResend() {
                // Resend failed message
                viewModel.resendMessage(message.getId());
            }
        });
        dialog.show();
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

    private void showAttachmentOptions() {
        String[] options = {"Image", "Document", "Voice Message"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose attachment type");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Image
                    selectImage();
                    break;
                case 1: // Document
                    selectFile();
                    break;
                case 2: // Voice Message
                    startVoiceRecording();
                    break;
            }
        });
        builder.show();
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_SELECT_IMAGE);
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_SELECT_FILE);
    }

    private void startVoiceRecording() {
        if (isRecording) {
            stopRecording();
            return;
        }

        // Request permission if needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO);
            return;
        }

        try {
            // Create file for recording
            String fileName = "voice_" + System.currentTimeMillis() + ".m4a";
            audioFile = new File(getExternalCacheDir(), fileName);

            // Set up MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());

            // Start recording
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;

            // Change send button icon to stop recording
            sendButton.setImageResource(R.drawable.send_icon);

            // Show recording indicator
            Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            Log.e(TAG, "Failed to start recording", e);
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (!isRecording) return;

        try {
            // Stop recording
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;
            isRecording = false;

            // Reset button icon
            updateSendButton(false);

            // Send the recorded audio
            if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
                sendVoiceMessage(audioFile);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to stop recording", e);
            Toast.makeText(this, "Failed to stop recording", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendVoiceMessage(File audioFile) {
        try {
            // Create URI from file
            Uri audioUri = Uri.fromFile(audioFile);

            // Send as media message
            viewModel.sendMediaMessage(
                    audioUri,
                    MediaType.AUDIO,
                    "", // No caption for voice messages
                    repliedToMessage != null ? repliedToMessage.getId() : null
            );

            // Clear UI state
            clearReplyToMessage();

        } catch (Exception e) {
            Log.e(TAG, "Failed to send voice message", e);
            Toast.makeText(this, "Failed to send voice message", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendTextMessage(String text) {
        String replyToId = repliedToMessage != null ? repliedToMessage.getId() : null;
            try {
                viewModel.sendTextMessage(text, replyToId);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send message", e);
            }

        // Clear UI
        messageInput.setText("");
        clearReplyToMessage();
    }

    private void sendMediaMessage() {
        if (selectedMediaUri != null) {
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
    }

    private void setReplyToMessage(MediaMessageDto message) {
        repliedToMessage = (MessageDto) message;
        respondedToLayout.setVisibility(View.VISIBLE);

        // If payload is null or empty and it actually includes media
        if (message.getPayload() != null) {
            String content = "[Media] " + message.getPayload().getFileName();
            respondedToText.setText(content);
        }
    }

    private void clearReplyToMessage() {
        repliedToMessage = null;
        respondedToLayout.setVisibility(View.GONE);
    }

    private void clearSelectedMedia() {
        selectedMediaUri = null;
        selectedMediaType = null;
        sendBarImage.setVisibility(View.GONE);
        removeAttachmentButton.setVisibility(View.GONE);
        updateSendButton(!messageInput.getText().toString().trim().isEmpty());
    }

    private void updateSendButton(boolean hasContent) {
        if (hasContent) {
            // Show send icon
            sendButton.setImageResource(R.drawable.ic_send);
        } else {
            // Show mic icon
            sendButton.setImageResource(R.drawable.ic_mic);
        }
    }

    private void openGroupInfo() {
        if (chatType == ChatType.GROUP) {
            Intent intent = new Intent(this, GroupInfoActivity.class);
            intent.putExtra("CHAT_ID", chatId);
            intent.putExtra("CHAT_NAME", chatName);
            startActivity(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_SELECT_IMAGE) {
                handleSelectedImage(data.getData());
            } else if (requestCode == REQUEST_SELECT_FILE) {
                handleSelectedFile(data.getData());
            }
        }
    }

    private void handleSelectedImage(Uri imageUri) {
        if (imageUri != null) {
            selectedMediaUri = imageUri;
            selectedMediaType = MediaType.IMAGE;

            // Show preview
            sendBarImage.setImageURI(imageUri);
            sendBarImage.setVisibility(View.VISIBLE);
            removeAttachmentButton.setVisibility(View.VISIBLE);

            // Update send button
            updateSendButton(true);
        }
    }

    public void openMedia(MediaStreamResultDto mediaStreamResultDto) {
        if (mediaStreamResultDto == null) {
            Log.e(TAG, "Cannot open media: mediaStreamResultDto is null");
            return;
        }

        try {
            // Get file info
            String fileName = mediaStreamResultDto.getFileName();
            String contentType = mediaStreamResultDto.getContentType();
            String absolutePath = mediaStreamResultDto.getAbsolutePath();

            if (absolutePath == null || absolutePath.isEmpty()) {
                // If no absolute path, we need to save the stream to a file first
                if (mediaStreamResultDto.getStream() != null) {
                    FileUtils fileUtils = ServiceModule.getFileUtils();
                    File mediaFile = fileUtils.copyToAppStorage(
                            mediaStreamResultDto.getStream(),
                            fileName != null ? fileName : "media_" + System.currentTimeMillis());

                    if (mediaFile != null) {
                        absolutePath = mediaFile.getAbsolutePath();
                    } else {
                        Toast.makeText(this, "Failed to save media file", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    Toast.makeText(this, "Media stream is not available", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // Create a file object from the absolute path
            File mediaFile = new File(absolutePath);
            if (!mediaFile.exists()) {
                Toast.makeText(this, "Media file not found", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get content type if not provided
            if (contentType == null || contentType.isEmpty()) {
                contentType = FileUtils.getMimeType(mediaFile);
            }

            // Check if this is an audio file
            if (contentType.startsWith("audio/")) {
                // Play audio directly in the app
                playAudioFile(mediaFile);
                return;
            }

            // Create URI using FileProvider for Android 7+ compatibility
            FileUtils fileUtils = ServiceModule.getFileUtils();
            Uri contentUri = fileUtils.getUriForFile(mediaFile);

            // Create intent to open the media
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, contentType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Check if there's an app that can handle this type of file
            if (intent.resolveActivity(this.getPackageManager()) != null) {
                this.startActivity(intent);
            } else {
                // No app can handle this type of file, try with a more generic approach
                intent.setDataAndType(contentUri, "*/*");

                // Create a chooser
                Intent chooserIntent = Intent.createChooser(intent, "Open with");
                if (chooserIntent.resolveActivity(this.getPackageManager()) != null) {
                    this.startActivity(chooserIntent);
                } else {
                    Toast.makeText(this, "No app found to open this file", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening media: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to open media file", Toast.LENGTH_SHORT).show();
        }
    }

    private void playAudioFile(File audioFile) {
        try {
            // Release any existing MediaPlayer
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
                isPlaying = false;
            }

            // Create a new MediaPlayer
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioFile.getAbsolutePath());

            // Create and show a dialog with playback controls
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_audio_player, null);
            builder.setView(dialogView);

            // Find views in dialog
            Button playPauseButton = dialogView.findViewById(R.id.audio_player_play_pause);
            SeekBar seekBar = dialogView.findViewById(R.id.audio_player_seekbar);
            TextView currentTimeText = dialogView.findViewById(R.id.audio_player_current_time);
            TextView totalTimeText = dialogView.findViewById(R.id.audio_player_total_time);

            // Create dialog
            AlertDialog dialog = builder.create();
            dialog.show();

            // Prepare player and setup UI
            mediaPlayer.prepare();

            // Set total duration
            int totalDuration = mediaPlayer.getDuration();
            seekBar.setMax(totalDuration);
            totalTimeText.setText(formatTime(totalDuration));

            // Update progress periodically
            Handler handler = new Handler();
            Runnable updateProgress = new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        seekBar.setProgress(currentPosition);
                        currentTimeText.setText(formatTime(currentPosition));
                        handler.postDelayed(this, 100);
                    }
                }
            };

            // Set up play/pause button
            playPauseButton.setOnClickListener(v -> {
                if (isPlaying) {
                    mediaPlayer.pause();
                    playPauseButton.setText("Play");
                    handler.removeCallbacks(updateProgress);
                } else {
                    mediaPlayer.start();
                    playPauseButton.setText("Pause");
                    handler.post(updateProgress);
                }
                isPlaying = !isPlaying;
            });

            // Set up seek bar
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && mediaPlayer != null) {
                        mediaPlayer.seekTo(progress);
                        currentTimeText.setText(formatTime(progress));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            // Start playing
            mediaPlayer.start();
            isPlaying = true;
            playPauseButton.setText("Pause");
            handler.post(updateProgress);

            // Handle completion
            mediaPlayer.setOnCompletionListener(mp -> {
                playPauseButton.setText("Play");
                isPlaying = false;
                seekBar.setProgress(0);
                currentTimeText.setText(formatTime(0));
                handler.removeCallbacks(updateProgress);
            });

            // Clean up when dialog is dismissed
            dialog.setOnDismissListener(dialogInterface -> {
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                    mediaPlayer = null;
                    isPlaying = false;
                    handler.removeCallbacks(updateProgress);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error playing audio file", e);
            Toast.makeText(this, "Failed to play audio file", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private void handleSelectedFile(Uri fileUri) {
        if (fileUri != null) {
            selectedMediaUri = fileUri;
            selectedMediaType = MediaType.DOCUMENT;

            // Show preview icon
            sendBarImage.setImageResource(R.drawable.file_icon);
            sendBarImage.setVisibility(View.VISIBLE);
            removeAttachmentButton.setVisibility(View.VISIBLE);

            // Update send button
            updateSendButton(true);
        }
    }

    private void markMessagesAsRead() {
        String chatId = getIntent().getStringExtra("CHAT_ID");
        String userId = currentUserId;

        UpdateMessageInChatReadByUserUseCase useCase =
                new UpdateMessageInChatReadByUserUseCase(ServiceModule.getConnectionManager());

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                boolean success = useCase.execute(chatId, userId);
                if (success) {
                    Log.d("ChatActivity", "Messages marked as read by " + userId);
                }
            } catch (Exception e) {
                Log.e("ChatActivity", "Failed to mark messages as read", e);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.refresh();
        markMessagesAsRead();
    }
}