package com.example.asiochatfrontend.ui.chat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
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
            return oldItem.getId().equals(newItem.getId()) &&
                    Objects.equals(oldItem.getStatus(), newItem.getStatus()) &&
                    Objects.equals(oldItem.getWaitingMemebersList(), newItem.getWaitingMemebersList());
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
        return message.getJid().equals(currentUserId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
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
        private final LinearLayout voiceLayout;
        private final ShapeableImageView messageImage;
        private final RelativeLayout attachmentLayout;
        private final ShapeableImageView attachmentImage;
        private final ProgressBar attachmentProgress;
        private final ImageView playIcon;
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
            attachmentLayout = itemView.findViewById(R.id.message_RLO_attachment);
            attachmentImage = itemView.findViewById(R.id.message_SIV_img);
            attachmentProgress = itemView.findViewById(R.id.message_PB_progress);
            playIcon = itemView.findViewById(R.id.message_IV_play_icon);
            timerIcon = itemView.findViewById(R.id.message_SIV_timer);
            failedIcon = itemView.findViewById(R.id.message_SIV_failed);
            singleCheckIcon = itemView.findViewById(R.id.message_SIV_double_check_1);
            deliveredChecksLayout = itemView.findViewById(R.id.checkmarks_delivered);
            readChecksLayout = itemView.findViewById(R.id.checkmarks_read);
            voiceLayout = itemView.findViewById(R.id.message_LLO_voice);
        }

        public void bind(MessageDto message, boolean isSentByMe) {
            // TEXT MESSAGE
            if (message instanceof TextMessageDto) {
                TextMessageDto textMessage = (TextMessageDto) message;
                messageText.setVisibility(View.VISIBLE);
                messageImage.setVisibility(View.GONE);
                voiceLayout.setVisibility(View.GONE);
                attachmentLayout.setVisibility(View.GONE);
                messageText.setText(textMessage.getPayload());
            } else {
                messageText.setVisibility(View.GONE);
            }

            // MEDIA MESSAGE
            if (message instanceof MediaMessageDto) {
                MediaMessageDto mediaMessage = (MediaMessageDto) message;

                if (mediaMessage.getPayload() != null && mediaMessage.getPayload().getFileName() != null) {
                    attachmentLayout.setVisibility(View.VISIBLE);
                    // You can customize the file type check
                    String fileName = mediaMessage.getPayload().getFileName().toLowerCase();
                    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
                        attachmentImage.setImageResource(R.drawable.file_icon);
                        playIcon.setVisibility(View.GONE);
                    } else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav")) {
                        attachmentImage.setImageResource(R.drawable.play_icon);
                        playIcon.setVisibility(View.VISIBLE);
                    } else {
                        attachmentImage.setImageResource(R.drawable.file_icon);
                        playIcon.setVisibility(View.GONE);
                    }

                    // Add click listener to preview
                    attachmentLayout.setOnClickListener(v -> {
                        if (mediaClickListener != null) {
                            mediaClickListener.onMediaClick(mediaMessage.getId());
                        }
                    });

                } else {
                    attachmentLayout.setVisibility(View.GONE);
                }
            } else {
                attachmentLayout.setVisibility(View.GONE);
            }

            // TIMESTAMP
            if (message.getTimestamp() != null) {
                timeText.setText(timeFormat.format(message.getTimestamp()));
            } else {
                timeText.setText(timeFormat.format(new Date()));
            }

            timeText.setVisibility(View.VISIBLE);

            // STATUS + ALIGNMENT
            adjustLayoutForSenderReceiver(isSentByMe);
            handleMessageStatus(message, isSentByMe);

            // SENDER NAME
            if (!isSentByMe) {
                senderNameText.setVisibility(View.VISIBLE);
                senderNameText.setText(message.getJid());
            } else {
                senderNameText.setVisibility(View.GONE);
            }

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onMessageLongClick(message);
                    return true;
                }
                return false;
            });
        }

        private void adjustLayoutForSenderReceiver(boolean isSentByMe) {
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

        private void handleMessageStatus(MessageDto message, boolean isSentByMe) {
            if (!isSentByMe) {
                hideAllStatusIndicators();
                return;
            }

            hideAllStatusIndicators();

            if (message.getStatus() != null) {
                switch (message.getStatus()) {
                    case UNKNOWN:
                        timerIcon.setVisibility(View.VISIBLE);
                        break;
                    case SENT:
                        singleCheckIcon.setVisibility(View.VISIBLE);
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
            voiceLayout.setVisibility(View.GONE);
        }
    }
}
