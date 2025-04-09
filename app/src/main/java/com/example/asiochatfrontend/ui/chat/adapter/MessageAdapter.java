package com.example.asiochatfrontend.ui.chat.adapter;

import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class MessageAdapter extends ListAdapter<MessageDto, MessageAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private static final DiffUtil.ItemCallback<MessageDto> DIFF_CALLBACK = new DiffUtil.ItemCallback<MessageDto>() {
        @Override
        public boolean areItemsTheSame(@NonNull MessageDto oldItem, @NonNull MessageDto newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull MessageDto oldItem, @NonNull MessageDto newItem) {
            return oldItem.getContent().equals(newItem.getContent()) &&
                    Objects.equals(oldItem.getState(), newItem.getState()) &&
                    Objects.equals(oldItem.getWaitingMembersList(), newItem.getWaitingMembersList());
        }
    };

    private final String currentUserId;
    private final OnMessageLongClickListener longClickListener;
    private final OnMediaClickListener mediaClickListener;

    public interface OnMessageLongClickListener {
        void onMessageLongClick(MessageDto message);
    }

    public interface OnMediaClickListener {
        void onMediaClick(String mediaId);
    }

    public MessageAdapter(String currentUserId, OnMessageLongClickListener longClickListener, OnMediaClickListener mediaClickListener) {
        super(DIFF_CALLBACK);
        this.currentUserId = currentUserId;
        this.longClickListener = longClickListener;
        this.mediaClickListener = mediaClickListener;
    }

    @Override
    public int getItemViewType(int position) {
        MessageDto message = getItem(position);
        return message.getSenderId().equals(currentUserId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item, parent, false);
        return new MessageViewHolder(view, longClickListener, mediaClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        MessageDto message = getItem(position);
        holder.bind(message, getItemViewType(position) == VIEW_TYPE_SENT);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout messageLayout;
        private final MaterialTextView senderNameText;
        private final MaterialTextView messageText;
        private final MaterialTextView timeText;
        private final ShapeableImageView messageImage;
        private final MaterialTextView nestedMessageText;
        private final MaterialTextView nestedMessageTimestamp;
        private final LinearLayout nestedMessageLayout;
        private final ShapeableImageView nestedMessageImage;
        private final MaterialTextView userRespondedText;
        private final RelativeLayout attachmentLayout;
        private final ShapeableImageView attachmentImage;
        private final ProgressBar attachmentProgress;
        private final ImageView playIcon;
        private final LinearLayout voiceLayout;
        private final Button voiceButton;
        private final TextView voiceTimeText;
        private final Button showMoreButton;

        // Message status indicators
        private final ShapeableImageView timerIcon;
        private final ShapeableImageView failedIcon;
        private final ShapeableImageView singleCheckIcon;
        private final FrameLayout deliveredChecksLayout;
        private final FrameLayout readChecksLayout;

        private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        private final OnMessageLongClickListener longClickListener;
        private final OnMediaClickListener mediaClickListener;

        public MessageViewHolder(@NonNull View itemView, OnMessageLongClickListener longClickListener, OnMediaClickListener mediaClickListener) {
            super(itemView);
            this.longClickListener = longClickListener;
            this.mediaClickListener = mediaClickListener;

            messageLayout = itemView.findViewById(R.id.message_LLO_message);
            senderNameText = itemView.findViewById(R.id.message_MTV_sender_name);
            messageText = itemView.findViewById(R.id.message_MTV_message);
            timeText = itemView.findViewById(R.id.message_MTV_time);
            messageImage = itemView.findViewById(R.id.message_SIV_img);
            nestedMessageLayout = itemView.findViewById(R.id.nested_message_LLO_message);
            nestedMessageText = itemView.findViewById(R.id.nested_message_MTV_message);
            nestedMessageTimestamp = itemView.findViewById(R.id.nested_message_MTV_timestamp);
            nestedMessageImage = itemView.findViewById(R.id.nested_message_SIV_img);
            userRespondedText = itemView.findViewById(R.id.user_MTV_responded_message);
            attachmentLayout = itemView.findViewById(R.id.message_RLO_attachment);
            attachmentImage = itemView.findViewById(R.id.message_SIV_img);
            attachmentProgress = itemView.findViewById(R.id.message_PB_progress);
            playIcon = itemView.findViewById(R.id.message_IV_play_icon);
            voiceLayout = itemView.findViewById(R.id.message_LLO_voice);
            voiceButton = itemView.findViewById(R.id.message_BTN_voice);
            voiceTimeText = itemView.findViewById(R.id.message_TV_time);
            showMoreButton = itemView.findViewById(R.id.message_BTN_show_more);

            // Status indicators
            timerIcon = itemView.findViewById(R.id.message_SIV_timer);
            failedIcon = itemView.findViewById(R.id.message_SIV_failed);
            singleCheckIcon = itemView.findViewById(R.id.message_SIV_double_check_1);
            deliveredChecksLayout = itemView.findViewById(R.id.checkmarks_delivered);
            readChecksLayout = itemView.findViewById(R.id.checkmarks_read);
        }

        public void bind(MessageDto message, boolean isSentByMe) {
            // Set message content
            String content = message.getContent();
            if (content != null && !content.isEmpty()) {
                messageText.setVisibility(View.VISIBLE);
                messageText.setText(content);
            } else {
                messageText.setVisibility(View.GONE);
            }

            // Set time
            if (message.getCreatedAt() != null) {
                timeText.setVisibility(View.VISIBLE);
                timeText.setText(timeFormat.format(message.getCreatedAt()));
            } else {
                timeText.setVisibility(View.GONE);
            }

            // Adjust layout for sender/receiver
            adjustLayoutForSenderReceiver(isSentByMe);

            // Handle reply (nested message)
            handleReplyMessage(message);

            // Handle media
            handleMediaAttachment(message);

            // Handle voice messages
            handleVoiceMessage(message);

            // Set message status
            handleMessageStatus(message, isSentByMe);

            // Set sender name for received messages in groups
            if (!isSentByMe) {
                senderNameText.setVisibility(View.VISIBLE);
                senderNameText.setText(message.getSenderId()); // Ideally, this should be the sender's display name
            } else {
                senderNameText.setVisibility(View.GONE);
            }

            // Set up long click listener for message options
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onMessageLongClick(message);
                    return true;
                }
                return false;
            });

            // Set click listener for "Show More" button if needed
            if (showMoreButton != null) {
                showMoreButton.setVisibility(View.GONE); // Hide by default
            }
        }

        private void adjustLayoutForSenderReceiver(boolean isSentByMe) {
            // Set message layout alignment
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messageLayout.getLayoutParams();
            if (isSentByMe) {
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.removeRule(RelativeLayout.ALIGN_PARENT_START);
                messageLayout.setBackgroundResource(R.drawable.message_border);
            } else {
                params.addRule(RelativeLayout.ALIGN_PARENT_START);
                params.removeRule(RelativeLayout.ALIGN_PARENT_END);
                messageLayout.setBackgroundResource(R.drawable.received_message_border);
            }
            messageLayout.setLayoutParams(params);
        }

        private void handleReplyMessage(MessageDto message) {
            // Check if this message is replying to another message
            if (message.getReplyToMessageId() != null && !message.getReplyToMessageId().isEmpty()) {
                // In a real app, you would fetch the replied-to message
                // For now, we'll just show a placeholder
                nestedMessageLayout.setVisibility(View.VISIBLE);
                userRespondedText.setText("Reply to message");
                nestedMessageText.setVisibility(View.VISIBLE);
                nestedMessageText.setText("[Original message content not available]");

                // For simplicity, we'll not handle nested message images here
                nestedMessageImage.setVisibility(View.GONE);

                // Set timestamp if available
                nestedMessageTimestamp.setVisibility(View.GONE);
            } else {
                nestedMessageLayout.setVisibility(View.GONE);
            }
        }

        private void handleMediaAttachment(MessageDto message) {
            // Check if message has media
            if (message.getMediaId() != null && !message.getMediaId().isEmpty()) {
                attachmentLayout.setVisibility(View.VISIBLE);

                // In a real app, you would load the image from the media ID
                // For now, we'll just show a placeholder
                attachmentImage.setImageResource(R.drawable.default_media_icon);

                // Hide progress bar by default
                attachmentProgress.setVisibility(View.GONE);

                // Determine if we need to show play icon (for videos/audio)
                playIcon.setVisibility(View.GONE); // Hide by default, would check media type in real app

                // Set click listener to open media
                attachmentLayout.setOnClickListener(v -> {
                    if (mediaClickListener != null) {
                        mediaClickListener.onMediaClick(message.getMediaId());
                    }
                });
            } else {
                attachmentLayout.setVisibility(View.GONE);
            }
        }

        private void handleVoiceMessage(MessageDto message) {
            // Check if this is a voice message (would need a flag in real implementation)
            boolean isVoiceMessage = false; // Placeholder logic

            if (isVoiceMessage) {
                voiceLayout.setVisibility(View.VISIBLE);

                // In a real app, you would set up voice player functionality
                voiceButton.setOnClickListener(v -> {
                    // Handle play/pause of voice message
                    voiceButton.setBackgroundResource(
                            voiceButton.getBackground().getConstantState().equals(
                                    itemView.getContext().getResources().getDrawable(R.drawable.play_icon).getConstantState())
                                    ? R.drawable.pause_icon : R.drawable.play_icon
                    );
                });

                // Set voice duration
                voiceTimeText.setText("0:00 / 0:30"); // Placeholder
            } else {
                voiceLayout.setVisibility(View.GONE);
            }
        }

        private void handleMessageStatus(MessageDto message, boolean isSentByMe) {
            // Only show status indicators for sent messages
            if (!isSentByMe) {
                hideAllStatusIndicators();
                return;
            }

            // Reset all indicators first
            hideAllStatusIndicators();

            // Show appropriate indicator based on message state
            if (message.getState() != null) {
                switch (message.getState()) {
                    case PENDING:
                        timerIcon.setVisibility(View.VISIBLE);
                        break;
                    case FAILED:
                        failedIcon.setVisibility(View.VISIBLE);
                        break;
                    case SENT:
                        singleCheckIcon.setVisibility(View.VISIBLE);
                        break;
                    case DELIVERED:
                        deliveredChecksLayout.setVisibility(View.VISIBLE);
                        break;
                    case READ:
                        readChecksLayout.setVisibility(View.VISIBLE);
                        break;
                }
            }
        }

        private void hideAllStatusIndicators() {
            timerIcon.setVisibility(View.GONE);
            failedIcon.setVisibility(View.GONE);
            singleCheckIcon.setVisibility(View.GONE);
            deliveredChecksLayout.setVisibility(View.GONE);
            readChecksLayout.setVisibility(View.GONE);
        }
    }
}