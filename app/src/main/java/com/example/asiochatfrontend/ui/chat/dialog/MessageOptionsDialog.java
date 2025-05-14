package com.example.asiochatfrontend.ui.chat.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;

/**
 * Dialog for showing message options (reply, delete, forward, etc.)
 */
public class MessageOptionsDialog extends Dialog {

    private final OnMessageOptionSelected listener;
    private final MessageDto message;
    private Button replyButton;
    private Button deleteButton;
    private Button forwardButton;
    private Button resendButton;
    private TextView messagePreviewText;

    /**
     * Interface for handling message option selections
     */
    public interface OnMessageOptionSelected {
        void onReply();

        // Future options can be added here
        // void onDelete();
        // void onForward();
        // void onResend();
    }

    /**
     * Constructor for MessageOptionsDialog
     *
     * @param context The context
     * @param message The message to show options for
     * @param listener Listener for option selections
     */
    public MessageOptionsDialog(@NonNull Context context, MessageDto message, OnMessageOptionSelected listener) {
        super(context);
        this.message = message;
        this.listener = listener;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    /**
     * Constructor for MessageOptionsDialog without message preview
     *
     * @param context The context
     * @param listener Listener for option selections
     */
    public MessageOptionsDialog(@NonNull Context context, OnMessageOptionSelected listener) {
        super(context);
        this.message = null;
        this.listener = listener;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_message_preview);

        // Set dialog to be full width
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        // Initialize views
        replyButton = findViewById(R.id.option_reply);
//        deleteButton = findViewById(R.id.option_delete);
//        forwardButton = findViewById(R.id.option_forward);
//        resendButton = findViewById(R.id.option_resend);
        messagePreviewText = findViewById(R.id.message_preview_text);

        // Set up message preview if available
        if (message != null) {
            setupMessagePreview();
        } else {
            hideMessagePreview();
        }

        // Set up option buttons
        setupButtons();
    }

    private void setupMessagePreview() {
        if (messagePreviewText != null) {
            String content = null;
            if (message instanceof TextMessageDto) {
                content = ((TextMessageDto) message).getPayload();
            } else if (message instanceof MediaMessageDto) {
                if (((MediaMessageDto) message).getPayload() != null) {
                    content =  "[Media] " + ((MediaMessageDto) message).getPayload().getType();
                }
                else {
                    content = "[Media]";
                }
            }

            // Limit preview length
            if (content.length() > 100) {
                content = content.substring(0, 97) + "...";
            }

            messagePreviewText.setText(content);
            messagePreviewText.setVisibility(View.VISIBLE);
        }
    }

    private void hideMessagePreview() {
        if (messagePreviewText != null) {
            messagePreviewText.setVisibility(View.GONE);
        }
    }

    private void setupButtons() {
        // Reply button
        replyButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReply();
            }
            dismiss();
        });

        // Set appropriate button visibilities based on message context
        if (message != null) {
            adjustButtonsBasedOnMessage();
        }
    }

    private void adjustButtonsBasedOnMessage() {
        // For example, maybe don't allow forwarding of system messages
        boolean isSystemMessage = message.getJid() == null || message.getJid().isEmpty();
        if (isSystemMessage) {
            forwardButton.setVisibility(View.GONE);
        }
    }
}