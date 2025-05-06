package com.example.asiochatfrontend.ui.chat.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;
import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.model.dto.MediaStreamResultDto;
import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
import com.example.asiochatfrontend.core.model.enums.MediaType;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.core.service.MediaService;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;

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
        void onMediaClick(MediaStreamResultDto mediaStreamResultDto);
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

    @Override public long getItemId(int position) {
        return getItem(position).getId().hashCode();
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
        private final MaterialButton voicePlayButton;
        private final TextView voiceTimeText;
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
            voicePlayButton = itemView.findViewById(R.id.message_BTN_voice);
            voiceTimeText = itemView.findViewById(R.id.message_TV_time);
        }

        public void bind(MessageDto message, boolean isSentByMe) {
            if (message.getWaitingMemebersList() == null || message.getWaitingMemebersList().isEmpty()) {
                message.setStatus(MessageState.READ);
            }

            // TEXT MESSAGE
            if (message instanceof TextMessageDto) {
                TextMessageDto textMessage = (TextMessageDto) message;
                if (textMessage.getPayload() == null) {
                    messageLayout.setVisibility(View.GONE);
                    return;
                }

                messageText.setVisibility(View.VISIBLE);
                messageText.setText(textMessage.getPayload());

                messageImage.setVisibility(View.GONE);
                voiceLayout.setVisibility(View.GONE);
                attachmentLayout.setVisibility(View.GONE);
            } else {
                messageText.setVisibility(View.GONE);
            }

            // MEDIA MESSAGE (Async)
            if (message instanceof MediaMessageDto) {
                MediaMessageDto mediaMessage = (MediaMessageDto) message;

                if (mediaMessage.getPayload() != null) {
                    attachmentLayout.setVisibility(View.GONE); // Hide until loaded

                    Executors.newSingleThreadExecutor().execute(() -> {
                        MediaStreamResultDto mediaStream = ServiceModule
                                .getConnectionManager()
                                .getMediaStream(mediaMessage.getId());

                        if (mediaStream != null) {
                            String fileName = mediaStream.getFileName().toLowerCase(Locale.ROOT);
                            File file = new File(mediaStream.getAbsolutePath());
                            attachmentLayout.post(() -> {
                                attachmentLayout.setVisibility(View.VISIBLE);

                                // <--- Set attachment image based on file type --->
                                if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
                                    playIcon.setVisibility(View.GONE);
                                    Glide.with(itemView.getContext())
                                            .load(file)
                                            .placeholder(R.drawable.file_icon)
                                            .error(R.drawable.file_icon)
                                            .into(messageImage);

                                    // <--- Set video (with preview image) based on file type --->
                                } else if (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".mp4")) {
                                    playIcon.setVisibility(View.VISIBLE);
                                    RequestOptions opts = new RequestOptions()
                                            .frame(1_000_000)                       // video thumbnail at 1s
                                            .centerCrop()
                                            .disallowHardwareConfig()               // <— key: no HW bitmaps!
                                            .format(DecodeFormat.PREFER_ARGB_8888); // software ARGB_8888

                                    Glide.with(itemView.getContext())
                                            .asBitmap()
                                            .load(file)    // File, Uri, whatever
                                            .apply(opts)
                                            .into(messageImage);

                                    messageImage.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

                                    attachmentLayout.setOnClickListener(v -> {
                                        // hide thumbnail + icon, show & start VideoView
                                        messageImage.setVisibility(View.GONE);
                                        playIcon.setVisibility(View.GONE);
                                        VideoView vv = itemView.findViewById(R.id.message_VV_video);
                                        vv.setVideoPath(file.getAbsolutePath());
                                        vv.setVisibility(View.VISIBLE);
                                        vv.start();
                                    });

                                    // <--- Set audio (with play icon) based on file type --->
                                } else if (fileName.endsWith(".m4a") || fileName.endsWith(".aac")) {
                                    // Handle audio files
                                    attachmentLayout.setVisibility(View.INVISIBLE);
                                    ViewGroup.LayoutParams params = attachmentLayout.getLayoutParams();
                                    params.width = 100; // in pixels
                                    params.height = 50; // in pixels
                                    attachmentLayout.setLayoutParams(params);
                                    voiceLayout.setVisibility(View.VISIBLE);
                                    long durationMs = FileUtils.getDurationOfAudio(mediaStream.getAbsolutePath());
                                    if (durationMs != -1) {
                                        String formatted = String.format("%d:%02d", (durationMs / 1000) / 60, (durationMs / 1000) % 60);
                                        voiceTimeText.setText(formatted);
                                    } else {
                                        voiceTimeText.setText("0:00");
                                    }

                                    voicePlayButton.setOnClickListener(v -> {
                                        if (mediaClickListener != null) {
                                            mediaClickListener.onMediaClick(mediaStream);
                                        }
                                    });

                                    // Unclear, set default file icon
                                } else {
                                    attachmentImage.setImageResource(R.drawable.file_icon);
                                    playIcon.setVisibility(View.GONE);
                                }

                                // <--- Set attachment progress and click listener --->
                                attachmentLayout.setOnClickListener(v -> {
                                    if (mediaClickListener != null) {
                                        mediaClickListener.onMediaClick(mediaStream);
                                    }
                                });
                            });

                        } else {
                            // If media stream is null, hide the attachment layout
                            attachmentLayout.post(() -> attachmentLayout.setVisibility(View.GONE));
                        }
                    });

                } else {
                    // If payload is null, hide the attachment layout
                    attachmentLayout.setVisibility(View.GONE);
                }

            } else {
                // If not a media message, hide the attachment layout
                attachmentLayout.setVisibility(View.GONE);
            }

            // TIMESTAMP
            Date timestamp = message.getTimestamp() != null ? message.getTimestamp() : new Date();
            timeText.setText(timeFormat.format(timestamp));
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
                    case PENDING:
                        singleCheckIcon.setVisibility(View.VISIBLE);
                        break;
                    case SENT:
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
            voiceLayout.setVisibility(View.GONE);
        }

        // --- Helper methods ---

        private Bitmap generateVideoThumbnail(String videoPath) {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(videoPath);
                return retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            } catch (Exception e) {
                Log.e("MessageAdapter", "Failed to generate video thumbnail", e);
                return null;
            } finally {
                try {
                    retriever.release();
                } catch (Exception e) {
                    Log.e("MessageAdapter", "Failed to release MediaMetadataRetriever", e);
                }
            }
        }

        private Bitmap overlayPlayIcon(Context context, Bitmap thumbnail) {
            // Make a mutable copy so we can draw on it
            Bitmap overlay = thumbnail.copy(Bitmap.Config.ARGB_8888, true);
            Canvas canvas = new Canvas(overlay);

            // Load your play-icon drawable
            Drawable icon = ContextCompat.getDrawable(context, R.drawable.play_icon);
            if (icon == null) return overlay;

            int iw = icon.getIntrinsicWidth();
            int ih = icon.getIntrinsicHeight();

            // Center the icon on the thumbnail
            int left = (overlay.getWidth()  - iw) / 2;
            int top  = (overlay.getHeight() - ih) / 2;
            icon.setBounds(left, top, left + iw, top + ih);

            // Draw it
            icon.draw(canvas);

            return overlay;
        }
    }
}
