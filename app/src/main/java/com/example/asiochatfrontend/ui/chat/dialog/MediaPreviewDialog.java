package com.example.asiochatfrontend.ui.chat.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;

import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.model.dto.MediaDto;
import com.example.asiochatfrontend.core.model.enums.MediaType;

import java.io.File;

/**
 * Dialog for displaying media previews (images, videos, documents).
 */
public class MediaPreviewDialog extends Dialog {
    private static final String TAG = "MediaPreviewDialog";

    private final MediaDto media;
    private ImageView imageView;
    private VideoView videoView;
    private ImageButton playPauseButton;
    private TextView fileNameText;
    private TextView fileSizeText;
    private ProgressBar loadingProgressBar;
    private RelativeLayout documentLayout;
    private RelativeLayout mediaControlsLayout;
    private boolean isVideoPlaying = false;

    /**
     * Constructor for MediaPreviewDialog.
     *
     * @param context The context
     * @param media The media to display
     */
    public MediaPreviewDialog(@NonNull Context context, MediaDto media) {
        super(context);
        this.media = media;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_media_preview);

        // Initialize views
        imageView = findViewById(R.id.dialog_preview_image);
        videoView = findViewById(R.id.dialog_preview_video);
        playPauseButton = findViewById(R.id.dialog_preview_play_pause);
        fileNameText = findViewById(R.id.dialog_preview_file_name);
        fileSizeText = findViewById(R.id.dialog_preview_file_size);
        loadingProgressBar = findViewById(R.id.dialog_preview_loading);
        documentLayout = findViewById(R.id.dialog_preview_document_layout);
        mediaControlsLayout = findViewById(R.id.dialog_preview_media_controls);

        // Set dialog to be full width
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Set up close button
        ImageButton closeButton = findViewById(R.id.dialog_preview_close);
        closeButton.setOnClickListener(v -> dismiss());

        // Display media based on type
        displayMedia();

        // Set up media controls
        setupMediaControls();
    }

    private void displayMedia() {
        if (media == null) {
            Log.e(TAG, "Media is null");
            showError("Media not available");
            return;
        }

        // Set common media info
        fileNameText.setText(media.getFileName() != null ? media.getFileName() : "Unnamed media");
        fileSizeText.setText(formatFileSize(media.getFileSize()));

        // Show loading indicator
        loadingProgressBar.setVisibility(View.VISIBLE);

        // Handle based on media type
        if (media.getType() == null) {
            Log.e(TAG, "Media type is null");
            showError("Unknown media type");
            return;
        }

        switch (media.getType()) {
            case IMAGE:
                displayImage();
                break;
            case VIDEO:
                displayVideo();
                break;
            case AUDIO:
                displayAudio();
                break;
            case DOCUMENT:
                displayDocument();
                break;
            default:
                showError("Unsupported media type");
                break;
        }
    }

    private void displayImage() {
        // Hide video view and document layout
        videoView.setVisibility(View.GONE);
        documentLayout.setVisibility(View.GONE);
        mediaControlsLayout.setVisibility(View.GONE);

        // Show image view
        imageView.setVisibility(View.VISIBLE);

        // Load image
        String localUri = media.getLocalUri();
        if (localUri != null && !localUri.isEmpty()) {
            try {
                File imageFile = new File(localUri);
                if (imageFile.exists()) {
                    imageView.setImageBitmap(BitmapFactory.decodeFile(localUri));
                } else {
                    imageView.setImageResource(R.drawable.default_media_icon);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading image", e);
                imageView.setImageResource(R.drawable.default_media_icon);
            }
        } else {
            imageView.setImageResource(R.drawable.default_media_icon);
        }

        // Hide loading indicator
        loadingProgressBar.setVisibility(View.GONE);
    }

    private void displayVideo() {
        // Hide image view and document layout
        imageView.setVisibility(View.GONE);
        documentLayout.setVisibility(View.GONE);

        // Show video view and media controls
        videoView.setVisibility(View.VISIBLE);
        mediaControlsLayout.setVisibility(View.VISIBLE);

        // Load video
        String localUri = media.getLocalUri();
        if (localUri != null && !localUri.isEmpty()) {
            try {
                File videoFile = new File(localUri);
                if (videoFile.exists()) {
                    videoView.setVideoPath(localUri);
                    videoView.setOnPreparedListener(mp -> {
                        loadingProgressBar.setVisibility(View.GONE);
                        mp.setLooping(false);
                    });
                    videoView.setOnCompletionListener(mp -> {
                        isVideoPlaying = false;
                        updatePlayPauseButton();
                    });
                    videoView.setOnErrorListener((mp, what, extra) -> {
                        Log.e(TAG, "Video playback error: " + what + ", " + extra);
                        showError("Error playing video");
                        return true;
                    });
                    videoView.requestFocus();
                } else {
                    showError("Video file not found");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting up video", e);
                showError("Error loading video");
            }
        } else {
            showError("Video URI not available");
        }
    }

    private void displayAudio() {
        // Hide image view and video view
        imageView.setVisibility(View.GONE);
        videoView.setVisibility(View.GONE);

        // Show document layout for audio info and media controls
        documentLayout.setVisibility(View.VISIBLE);
        mediaControlsLayout.setVisibility(View.VISIBLE);

        // Set audio icon
        ImageView docIcon = findViewById(R.id.dialog_preview_doc_icon);
        docIcon.setImageResource(R.drawable.ic_mic);

        // Set up audio player (we'll use MediaPlayer in a real implementation)
        // For now, we'll just show the controls and hide loading indicator
        loadingProgressBar.setVisibility(View.GONE);
    }

    private void displayDocument() {
        // Hide image view and video view
        imageView.setVisibility(View.GONE);
        videoView.setVisibility(View.GONE);
        mediaControlsLayout.setVisibility(View.GONE);

        // Show document layout
        documentLayout.setVisibility(View.VISIBLE);

        // Set document icon
        ImageView docIcon = findViewById(R.id.dialog_preview_doc_icon);
        docIcon.setImageResource(R.drawable.file_icon);

        // Hide loading indicator
        loadingProgressBar.setVisibility(View.GONE);
    }

    private void setupMediaControls() {
        // Set up play/pause button
        playPauseButton.setOnClickListener(v -> {
            if (media.getType() == MediaType.VIDEO) {
                toggleVideoPlayback();
            } else if (media.getType() == MediaType.AUDIO) {
                toggleAudioPlayback();
            }
        });

        updatePlayPauseButton();
    }

    private void toggleVideoPlayback() {
        if (isVideoPlaying) {
            videoView.pause();
        } else {
            videoView.start();
        }
        isVideoPlaying = !isVideoPlaying;
        updatePlayPauseButton();
    }

    private void toggleAudioPlayback() {
        // In a real implementation, this would control the MediaPlayer
        isVideoPlaying = !isVideoPlaying;
        updatePlayPauseButton();
    }

    private void updatePlayPauseButton() {
        playPauseButton.setImageResource(isVideoPlaying ?
                R.drawable.pause_icon :
                R.drawable.play_icon);
    }

    private void showError(String errorMessage) {
        // Hide all media views
        imageView.setVisibility(View.GONE);
        videoView.setVisibility(View.GONE);
        documentLayout.setVisibility(View.GONE);
        mediaControlsLayout.setVisibility(View.GONE);
        loadingProgressBar.setVisibility(View.GONE);

        // Show error message
        TextView errorText = findViewById(R.id.dialog_preview_error);
        errorText.setVisibility(View.VISIBLE);
        errorText.setText(errorMessage);
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Clean up resources
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }
}