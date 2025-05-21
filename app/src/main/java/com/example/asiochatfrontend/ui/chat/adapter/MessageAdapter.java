package com.example.asiochatfrontend.ui.chat.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.MediaMetadataRetriever;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.util.LruCache;
import android.util.TypedValue;
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
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.model.dto.MediaStreamResultDto;
import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
import com.example.asiochatfrontend.core.model.enums.MessageState;
import com.example.asiochatfrontend.data.common.utils.FileUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MessageAdapter extends ListAdapter<MessageDto, MessageAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;
    private static final int VIEW_TYPE_REPLY_SENT     = 3;
    private static final int VIEW_TYPE_REPLY_RECEIVED = 4;

    // Define default image dimensions for thumbnails to avoid decoding full images
    private static final int THUMBNAIL_WIDTH = 300;
    private static final int THUMBNAIL_HEIGHT = 300;

    // Retry mechanism constants
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int INITIAL_RETRY_DELAY_MS = 300;

    // Create a thread pool with a fixed number of threads to limit concurrent operations
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    // Create an LRU cache for thumbnails to avoid regenerating them
    private final LruCache<String, Bitmap> thumbnailCache;

    // Single Glide instance for the adapter
    private final RequestManager glideRequestManager;

    // Default request options to optimize Glide image loading
    private final RequestOptions defaultGlideOptions;

    private static final DiffUtil.ItemCallback<MessageDto> DIFF_CALLBACK = new DiffUtil.ItemCallback<MessageDto>() {
        @Override
        public boolean areItemsTheSame(@NonNull MessageDto oldItem, @NonNull MessageDto newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull MessageDto oldItem, @NonNull MessageDto newItem) {
            if (!oldItem.getId().equals(newItem.getId())) {
                return false;
            }

            if (!oldItem.getStatus().name().equals(newItem.getStatus().name())) {
                return false;
            }

            List<String> oldWaiting = oldItem.getWaitingMemebersList();
            List<String> newWaiting = newItem.getWaitingMemebersList();

            // if one is null and the other isn't → not the same
            if (oldWaiting == null ^ newWaiting == null) {
                return false;
            }

            // if both null → OK
            if (oldWaiting == null) {
                return true;
            }

            // both non-null → rely on .equals() for deep comparison
            return oldWaiting.equals(newWaiting);
        }
    };

    private final String currentUserId;
    private final OnMessageLongClickListener longClickListener;
    private final OnMediaClickListener mediaClickListener;
    private int highlightedPosition = -1;
    private String highlightText = "";

    public interface OnMessageLongClickListener {
        void onMessageLongClick(MessageDto message);
    }

    public interface OnMediaClickListener {
        void onMediaClick(MediaStreamResultDto mediaStreamResultDto);
    }

    public MessageAdapter(
            Context context,
            String currentUserId,
            OnMessageLongClickListener longClickListener,
            OnMediaClickListener mediaClickListener
    ) {
        super(DIFF_CALLBACK);
        this.currentUserId = currentUserId;
        this.longClickListener = longClickListener;
        this.mediaClickListener = mediaClickListener;

        // Initialize the thumbnail cache (using 1/8 of available memory)
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;
        thumbnailCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        // Initialize Glide with optimized defaults
        glideRequestManager = Glide.with(context);
        defaultGlideOptions = new RequestOptions()
                .format(DecodeFormat.PREFER_RGB_565) // Use less memory than ARGB_8888
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT) // Resize images
                .centerCrop()
                .encodeQuality(80); // Reduce quality slightly for better performance

        // Enable item ID for stability
        setHasStableIds(true);
    }

    @Override
    public int getItemViewType(int position) {
        MessageDto m = getItem(position);

        boolean isReply = m instanceof TextMessageDto
                && ((TextMessageDto) m).getReplyTo() != null;
        if (isReply) {
            return m.getJid().equals(currentUserId)
                    ? VIEW_TYPE_REPLY_SENT
                    : VIEW_TYPE_REPLY_RECEIVED;
        }

        return m.getJid().equals(currentUserId)
                ? VIEW_TYPE_SENT
                : VIEW_TYPE_RECEIVED;
    }

    @Override public long getItemId(int position) {
        return getItem(position).getId().hashCode();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == VIEW_TYPE_REPLY_SENT || viewType == VIEW_TYPE_REPLY_RECEIVED)
                ? R.layout.responded_message_item
                : R.layout.message_item;

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false);

        return new MessageViewHolder(
                view,
                longClickListener,
                mediaClickListener,
                glideRequestManager,
                thumbnailCache,
                executorService
        );
    }

    public void setHighlightedPosition(int position, String highlightText) {
        // Check if values have actually changed before notifying
        boolean hasChanged = (this.highlightedPosition != position) ||
                !Objects.equals(this.highlightText, highlightText);

        this.highlightedPosition = position;
        this.highlightText = highlightText;

        if (hasChanged) {
            notifyDataSetChanged();
        }
    }

    public void clearHighlighting() {
        // Only notify if there was actually a highlight to clear
        boolean hadHighlight = (this.highlightedPosition != -1) ||
                (this.highlightText != null && !this.highlightText.isEmpty());

        this.highlightedPosition = -1;
        this.highlightText = "";

        if (hadHighlight) {
            notifyDataSetChanged();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        int vt = getItemViewType(position);
        boolean isReply      = vt == VIEW_TYPE_REPLY_SENT || vt == VIEW_TYPE_REPLY_RECEIVED;
        boolean isSentByMe   = (vt == VIEW_TYPE_SENT   || vt == VIEW_TYPE_REPLY_SENT);
        boolean isHighlighted= position == highlightedPosition && !highlightText.isEmpty();

        if (isReply) {
            TextMessageDto reply = (TextMessageDto)getItem(position);
            MessageDto original = null;
            for (MessageDto m : getCurrentList()) {
                  if (m.getId().equals(reply.getReplyTo())) {
                           original = m;
                           break;
                       }
              }
            holder.bindReply(reply, original, isSentByMe);
        } else {
            holder.bind(
                    getItem(position),
                    isSentByMe,
                    isHighlighted,
                    highlightText);
        }
    }

    @Override
    public void onViewRecycled(@NonNull MessageViewHolder holder) {
        super.onViewRecycled(holder);
        // Cancel any Glide requests to avoid memory leaks
        holder.cancelImageLoading();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        // Shutdown the executor service when adapter is detached
        executorService.shutdown();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout messageLayout;
        private final MaterialTextView senderNameText;
        private final MaterialTextView messageText;
        private final MaterialTextView timeText;
        private final LinearLayout voiceLayout;
        private final MaterialButton voicePlayButton;
        private final TextView voiceTimeText;
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

        private final LinearLayout respondRoot;
        private final LinearLayout respondedWrapper;
        private final MaterialTextView origUser, origText, origTime;
        private final MaterialTextView replyText, replyTime;
        private final ShapeableImageView replyStatus;

        private final OnMessageLongClickListener longClickListener;
        private final OnMediaClickListener mediaClickListener;
        private final RequestManager glideRequestManager;
        private final LruCache<String, Bitmap> thumbnailCache;
        private final ExecutorService executorService;
        private Runnable pendingThumbnailTask;

        public MessageViewHolder(@NonNull View itemView,
                                 OnMessageLongClickListener longClickListener,
                                 OnMediaClickListener mediaClickListener,
                                 RequestManager glideRequestManager,
                                 LruCache<String, Bitmap> thumbnailCache,
                                 ExecutorService executorService) {
            super(itemView);
            this.longClickListener = longClickListener;
            this.mediaClickListener = mediaClickListener;
            this.glideRequestManager = glideRequestManager;
            this.thumbnailCache = thumbnailCache;
            this.executorService = executorService;

            messageLayout = itemView.findViewById(R.id.message_LLO_message);
            senderNameText = itemView.findViewById(R.id.message_MTV_sender_name);
            messageText = itemView.findViewById(R.id.message_MTV_message);
            timeText = itemView.findViewById(R.id.message_MTV_time);
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

            respondRoot = itemView.findViewById(R.id.responded_message_LLO_message);
            respondedWrapper = itemView.findViewById(R.id.responded_message_LLO_responded);
            if (respondedWrapper != null) {
                origUser    = itemView.findViewById(R.id.user_MTV_responded_message);
                origText    = itemView.findViewById(R.id.responded_message_MTV_responded_message);
                origTime    = itemView.findViewById(R.id.nested_message_MTV_timestamp);
                replyText   = itemView.findViewById(R.id.responded_message_MTV_message);
                replyTime   = itemView.findViewById(R.id.responded_message_MTV_time);
                replyStatus = itemView.findViewById(R.id.responded_message_SIV_status);
            } else {
                origUser = origText = origTime = replyText = replyTime = null;
                replyStatus = null;
            }
        }

        public void bind(MessageDto message, boolean isSentByMe, boolean isHighlighted, String highlightText) {
            // Call your existing bind method first - this handles all the basic message display
            bind(message, isSentByMe);

            // Apply highlighting if needed
            if (isHighlighted && !highlightText.isEmpty() && message instanceof TextMessageDto) {
                String text = ((TextMessageDto) message).getPayload();
                if (text != null && !text.isEmpty()) {
                    // Create spannable string for highlighting
                    SpannableString spannableString = new SpannableString(text);

                    // Find all occurrences of the search text (case insensitive)
                    String messageTextLower = text.toLowerCase();
                    String highlightTextLower = highlightText.toLowerCase();

                    int index = 0;
                    while (index >= 0) {
                        index = messageTextLower.indexOf(highlightTextLower, index);
                        if (index >= 0) {
                            // Highlight this occurrence
                            spannableString.setSpan(
                                    new BackgroundColorSpan(Color.YELLOW),
                                    index,
                                    index + highlightTextLower.length(),
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            );

                            // Move to next occurrence
                            index += highlightTextLower.length();
                        }
                    }

                    // Set the highlighted text to the message TextView
                    messageText.setText(spannableString);

                    // Make the message layout stand out
                    messageLayout.setBackgroundResource(isSentByMe ?
                            R.drawable.highlighted_sent_message_border :
                            R.drawable.highlighted_received_message_borde);
                }
            }
        }

        public void bind(MessageDto message, boolean isSentByMe) {
            if (pendingThumbnailTask != null) {
                attachmentImage.removeCallbacks(pendingThumbnailTask);
                pendingThumbnailTask = null;
            }

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
                    attachmentProgress.setVisibility(View.VISIBLE); // Hide until loaded

                    Executors.newSingleThreadExecutor().execute(() -> {
                        MediaStreamResultDto mediaStream = ServiceModule
                                .getConnectionManager()
                                .getMediaStream(mediaMessage.getId());

                        if (mediaStream != null) {
                            String fileName = mediaStream.getFileName().toLowerCase(Locale.ROOT);
                            File file = new File(mediaStream.getAbsolutePath());

                            // Check if we're still bound to the same message
                            if (!isViewStillValid(message)) return;

                            attachmentLayout.post(() -> {
                                // Check again if view is still valid before updating UI
                                if (!isViewStillValid(message)) return;

                                attachmentLayout.setVisibility(View.VISIBLE);
                                attachmentProgress.setVisibility(View.GONE);

                                // <--- Set attachment image based on file type --->
                                if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
                                    playIcon.setVisibility(View.GONE);

                                    // Check if image is cached
                                    String cacheKey = "image_" + mediaMessage.getId();
                                    Bitmap cachedImage = thumbnailCache.get(cacheKey);

                                    if (cachedImage != null) {
                                        attachmentImage.setImageBitmap(cachedImage);
                                    } else {
                                        // Create and cache video thumbnail
                                        BitmapFactory.decodeStream(mediaStream.getStream());
                                        loadImageWithGlide(file, false, mediaMessage);
                                    }

                                    // <--- Set video (with preview image) based on file type --->
                                } else if (fileName.endsWith(".wav") || fileName.endsWith(".mp4")) {
                                    playIcon.setVisibility(View.VISIBLE);

                                    // Check if thumbnail is cached
                                    String cacheKey = "video_" + mediaMessage.getId();
                                    Bitmap cachedThumbnail = thumbnailCache.get(cacheKey);

                                    if (cachedThumbnail != null) {
                                        attachmentImage.setImageBitmap(cachedThumbnail);
                                    } else {
                                        // Create and cache video thumbnail
                                        loadVideoThumbnail(file, cacheKey, mediaMessage);
                                    }

                                    attachmentLayout.setOnClickListener(v -> {
                                        // hide thumbnail + icon, show & start VideoView
                                        attachmentProgress.setVisibility(View.VISIBLE);
                                        playIcon.setVisibility(View.GONE);

                                        VideoView vv = itemView.findViewById(R.id.message_VV_video);
                                        vv.setVideoPath(file.getAbsolutePath());
                                        vv.setVisibility(View.VISIBLE);
                                        vv.start();
                                    });

                                    // <--- Set audio (with play icon) based on file type --->
                                } else if (fileName.endsWith(".mp3") || fileName.endsWith(".m4a") || fileName.endsWith(".aac")) {
                                    // Handle audio files
                                    attachmentLayout.setVisibility(View.GONE);
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
                                    Drawable fileIcon = ContextCompat.getDrawable(itemView.getContext(), R.drawable.file_icon);
                                    attachmentImage.setImageDrawable(fileIcon);
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
                            if (!isViewStillValid(message)) return;
                            attachmentLayout.post(() -> {
                                // Check again if view is still valid before updating UI
                                if (!isViewStillValid(message)) return;
                                attachmentProgress.setVisibility(View.GONE);
                                attachmentLayout.setVisibility(View.GONE);
                            });
                        }
                    });

                } else {
                    // If payload is null, hide the attachment layout
                    attachmentLayout.setVisibility(View.GONE);
                    attachmentProgress.setVisibility(View.GONE);
                }

            } else {
                // If not a media message, hide the attachment layout
                attachmentLayout.setVisibility(View.GONE);
                attachmentProgress.setVisibility(View.GONE);
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

        /**
         * New, only‐for replies.
         * @param reply     the message that has replyTo≠null
         * @param original  the looked‐up original message (might be null)
         * @param isSentByMe alignment
         */
        void bindReply(TextMessageDto reply, MessageDto original, boolean isSentByMe) {
            // Hide the normal bubble (if present)
            try {
                if (messageLayout != null) {
                    messageLayout.setVisibility(View.GONE);
                }
                // 2) show and populate the responded UI
                respondRoot.setVisibility(View.VISIBLE);
                respondedWrapper.setVisibility(View.VISIBLE);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) respondRoot.getLayoutParams();

                if (original instanceof TextMessageDto) {
                    origUser.setText(original.getJid());
                    origText.setText(((TextMessageDto) original).getPayload());
                    // TIMESTAMP
                    Date timestamp = original.getTimestamp() != null ? original.getTimestamp() : new Date();
                    origTime.setText(timeFormat.format(timestamp));

                } else if (original instanceof MediaMessageDto) {
                    MediaMessageDto media = (MediaMessageDto) original;
                    origUser.setText(media.getJid());
                    String type = media.getPayload().getType().name().toLowerCase(Locale.ROOT);
                    origText.setText("[Media] " + type);
                    Date ts = media.getTimestamp() != null
                            ? media.getTimestamp()
                            : new Date();
                    origTime.setText(timeFormat.format(ts));
                } else {
                    origUser.setText(original.getJid());
                    origText.setText("[Unknown type]");
                    Date timestamp = original.getTimestamp() != null ? original.getTimestamp() : new Date();
                    origTime.setText(timeFormat.format(timestamp));
                }

                replyText.setText(reply.getPayload());
                Date timestamp = reply.getTimestamp() != null ? reply.getTimestamp() : new Date();
                replyTime.setText(timeFormat.format(timestamp));

                // status icon exactly as before
                switch (reply.getStatus()) {
                    case UNKNOWN:
                        replyStatus.setImageResource(R.drawable.ic_timer);
                        break;
                    case PENDING:
                        replyStatus.setImageResource(R.drawable.ic_check);
                        break;
                    case SENT:
                        replyStatus.setImageResource(R.drawable.ic_double_check);
                        break;
                    case READ: {
                        // 1) load & tint both drawables
                        Drawable check1 = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_check).mutate();
                        Drawable check2 = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_check).mutate();
                        DrawableCompat.wrap(check1);
                        DrawableCompat.wrap(check2);
                        DrawableCompat.setTint(check1, ContextCompat.getColor(itemView.getContext(), R.color.blue));
                        DrawableCompat.setTint(check2, ContextCompat.getColor(itemView.getContext(), R.color.blue));

                        // 2) layer them into one LayerDrawable
                        LayerDrawable doubleCheck = new LayerDrawable(new Drawable[]{check1, check2});

                        // 3) move the second check a few dp to the right
                        int offsetPx = (int) TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 4, itemView.getResources().getDisplayMetrics()
                        );
                        doubleCheck.setLayerInset(1, offsetPx, 0, 0, 0);

                        // 4) apply to your ImageView
                        replyStatus.setImageDrawable(doubleCheck);
                        break;
                    }
                }

                // 3) align left / right
                if (isSentByMe) {
                    params.removeRule(RelativeLayout.ALIGN_PARENT_START);
                    params.addRule(RelativeLayout.ALIGN_PARENT_END);
                    respondRoot.setBackgroundResource(R.drawable.message_border);
                } else {
                    params.removeRule(RelativeLayout.ALIGN_PARENT_END);
                    params.addRule(RelativeLayout.ALIGN_PARENT_START);
                    respondRoot.setBackgroundResource(R.drawable.received_message_border);
                }
                respondRoot.setLayoutParams(params);
            } catch (Exception e) {
                Log.e("MessageAdapter", "Error binding reply message", e);
            } finally {
                // 4) set the click listener
                respondRoot.setOnLongClickListener(v -> {
                    if (longClickListener != null) {
                        longClickListener.onMessageLongClick(reply);
                        return true;
                    }
                    return false;
                });
            }
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

        /**
         * Check if this ViewHolder is still bound to the same message
         * Prevents updating recycled views with old data
         */
        private boolean isViewStillValid(MessageDto message) {
            int position = getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                return false;
            };

            try {
                MessageDto currentMessage = ((MessageAdapter) getBindingAdapter()).getItem(position);
                return currentMessage != null && currentMessage.getId().equals(message.getId());
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * Load image with optimized Glide settings
         */
        /**
         * Load image with optimized Glide settings with retry mechanism
         */
        private void loadImageWithGlide(File file, boolean isVideo, MediaMessageDto messageDto) {
            loadImageWithGlideAndRetry(file, isVideo, 0, messageDto);
        }

        /**
         * Load image with Glide with retry mechanism
         * @param file File to load
         * @param isVideo Whether it's a video thumbnail
         * @param retryCount Current retry count
         */
        private void loadImageWithGlideAndRetry(File file, boolean isVideo, int retryCount, MediaMessageDto messageDto) {
            // Fix: Get placeholder drawable safely
            Drawable placeholderDrawable = ContextCompat.getDrawable(itemView.getContext(), R.drawable.file_icon);
            ColorDrawable fallbackDrawable = new ColorDrawable(Color.GRAY);

            RequestOptions options = new RequestOptions()
                    .placeholder(placeholderDrawable != null ? placeholderDrawable : fallbackDrawable)
                    .error(placeholderDrawable != null ? placeholderDrawable : fallbackDrawable)
                    .format(DecodeFormat.PREFER_RGB_565)  // Uses less memory than ARGB_8888
                    .override(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL);

            if (isVideo) {
                options = options.frame(1_000_000); // 1 second mark for video thumbnails
            }

            glideRequestManager
                    .asBitmap()
                    .load(file)
                    .apply(options)
                    .listener(new com.bumptech.glide.request.RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(
                                com.bumptech.glide.load.engine.GlideException e,
                                Object model,
                                com.bumptech.glide.request.target.Target<Bitmap> target,
                                boolean isFirstResource
                        ) {
                            // Implement retry logic on failure
                            if (retryCount < MAX_RETRY_ATTEMPTS) {
                                Log.w("MessageAdapter", "Image load failed, retrying (" + (retryCount + 1) + "/" + MAX_RETRY_ATTEMPTS + "): " + file.getAbsolutePath());

                                // Use exponential backoff for retries
                                int delayMs = INITIAL_RETRY_DELAY_MS * (1 << retryCount);

                                attachmentImage.postDelayed(() -> {
                                    // Check if view is still valid before retrying
                                    if (attachmentImage.isAttachedToWindow()) {
                                        loadImageWithGlideAndRetry(file, isVideo, retryCount + 1, messageDto);
                                    }
                                }, delayMs);
                            } else {
                                Log.e("MessageAdapter", "Failed to load image after " + MAX_RETRY_ATTEMPTS + " attempts: " + file.getAbsolutePath());
                            }
                            return false; // Let Glide handle the failure
                        }

                        @Override
                        public boolean onResourceReady(
                                Bitmap resource,
                                Object model,
                                com.bumptech.glide.request.target.Target<Bitmap> target,
                                com.bumptech.glide.load.DataSource dataSource,
                                boolean isFirstResource
                        ) {
                            // Successfully loaded image
                            String cacheKey = "image_" + messageDto.getId();
                            thumbnailCache.put(cacheKey, resource);
                            return false; // Let Glide set the resource
                        }
                    })
                    .into(attachmentImage);
        }

        /**
         * Load and cache video thumbnail efficiently with retry mechanism
         */
        private void loadVideoThumbnail(File file, String cacheKey, MessageDto message) {
            loadVideoThumbnailWithRetry(file, cacheKey, message, 0);
        }

        /**
         * Load and cache video thumbnail with retry mechanism
         * @param file File to load
         * @param cacheKey Cache key for the thumbnail
         * @param message Message associated with the thumbnail
         * @param retryCount Current retry count
         */
        private void loadVideoThumbnailWithRetry(File file, String cacheKey, MessageDto message, int retryCount) {
            executorService.execute(() -> {
                MediaMetadataRetriever retriever = null;
                try {
                    retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(file.getAbsolutePath());

                    // Get the frame at 1 second mark
                    Bitmap originalBitmap = retriever.getFrameAtTime(
                            1_000_000, // 1 second
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    );

                    // Handle null bitmap case with retry
                    if (originalBitmap == null) {
                        // Release retriever before retry
                        if (retriever != null) {
                            try {
                                retriever.release();
                            } catch (Exception ignored) {}
                            retriever = null;
                        }

                        if (retryCount < MAX_RETRY_ATTEMPTS) {
                            Log.w("MessageAdapter", "Could not retrieve video frame for " + file.getName() +
                                    ", retrying (" + (retryCount + 1) + "/" + MAX_RETRY_ATTEMPTS + ")");

                            // Calculate delay with exponential backoff
                            int delayMs = INITIAL_RETRY_DELAY_MS * (1 << retryCount);

                            // Retry after delay
                            attachmentImage.postDelayed(() -> {
                                // Check if view is still valid before retrying
                                if (isViewStillValid(message) && attachmentImage.isAttachedToWindow()) {
                                    loadVideoThumbnailWithRetry(file, cacheKey, message, retryCount + 1);
                                }
                            }, delayMs);
                            return;
                        }

                        // If max retries reached, use default icon
                        Log.e("MessageAdapter", "Failed to retrieve video frame after " + MAX_RETRY_ATTEMPTS +
                                " attempts: " + file.getName());

                        // Post on UI thread to set default drawable
                        pendingThumbnailTask = () -> {
                            if (!isViewStillValid(message)) return;

                            Drawable fileIcon = ContextCompat.getDrawable(itemView.getContext(), R.drawable.file_icon);
                            if (fileIcon != null) {
                                attachmentImage.setImageDrawable(fileIcon);
                            } else {
                                attachmentImage.setImageDrawable(new ColorDrawable(Color.GRAY));
                            }
                        };

                        attachmentImage.post(pendingThumbnailTask);
                        return;
                    }

                    // Successfully retrieved frame, resize bitmap to save memory
                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                            originalBitmap,
                            THUMBNAIL_WIDTH,
                            THUMBNAIL_HEIGHT,
                            true
                    );

                    if (originalBitmap != resizedBitmap) {
                        originalBitmap.recycle();
                    }

                    // Add play button indicator
                    Bitmap thumbnailWithPlay = addPlayIndicator(itemView.getContext(), resizedBitmap);
                    if (resizedBitmap != thumbnailWithPlay && thumbnailWithPlay != null) {
                        resizedBitmap.recycle();
                    }

                    // Cache the final bitmap
                    if (thumbnailWithPlay != null) {
                        thumbnailCache.put(cacheKey, thumbnailWithPlay);

                        // Fix: Track the UI update Runnable
                        final Bitmap finalThumbnail = thumbnailWithPlay;
                        pendingThumbnailTask = () -> {
                            if (isViewStillValid(message)) {
                                attachmentImage.setImageBitmap(finalThumbnail);
                            }
                        };

                        // Update UI on main thread if view is still valid
                        attachmentImage.post(pendingThumbnailTask);
                    }
                } catch (Exception e) {
                    Log.e("MessageAdapter", "Error loading video thumbnail", e);

                    if (retryCount < MAX_RETRY_ATTEMPTS) {
                        Log.w("MessageAdapter", "Video thumbnail load error, retrying (" +
                                (retryCount + 1) + "/" + MAX_RETRY_ATTEMPTS + "): " + file.getName());

                        // Calculate delay with exponential backoff
                        int delayMs = INITIAL_RETRY_DELAY_MS * (1 << retryCount);

                        // Retry after delay
                        attachmentImage.postDelayed(() -> {
                            // Check if view is still valid before retrying
                            if (isViewStillValid(message) && attachmentImage.isAttachedToWindow()) {
                                loadVideoThumbnailWithRetry(file, cacheKey, message, retryCount + 1);
                            }
                        }, delayMs);
                    } else {
                        // Fix: Set default image after max retries
                        pendingThumbnailTask = () -> {
                            if (!isViewStillValid(message)) return;

                            Drawable fileIcon = ContextCompat.getDrawable(itemView.getContext(), R.drawable.file_icon);
                            if (fileIcon != null) {
                                attachmentImage.setImageDrawable(fileIcon);
                            } else {
                                attachmentImage.setImageDrawable(new ColorDrawable(Color.GRAY));
                            }
                        };

                        attachmentImage.post(pendingThumbnailTask);
                    }
                } finally {
                    // Fix: Ensure retriever is always released
                    if (retriever != null) {
                        try {
                            retriever.release();
                        } catch (Exception ignored) {}
                    }
                }
            });
        }

        /**
         * Add play indicator to bitmap more efficiently
         */
        private Bitmap addPlayIndicator(Context context, Bitmap thumbnail) {
            if (thumbnail == null) return null;

            // Create a smaller play indicator
            Drawable playDrawable = ContextCompat.getDrawable(context, R.drawable.play_icon);
            if (playDrawable == null) return thumbnail;

            try {
                // Create a new bitmap for drawing
                Bitmap result = thumbnail.copy(Bitmap.Config.RGB_565, true);
                Canvas canvas = new Canvas(result);

                // Calculate icon size and position
                int iconSize = Math.min(result.getWidth(), result.getHeight()) / 3;
                int left = (result.getWidth() - iconSize) / 2;
                int top = (result.getHeight() - iconSize) / 2;

                playDrawable.setBounds(left, top, left + iconSize, top + iconSize);
                playDrawable.draw(canvas);

                return result;
            } catch (Exception e) {
                Log.e("MessageAdapter", "Error adding play indicator", e);
                return thumbnail;
            }
        }

        /**
         * Cancel any pending image loading requests when view is recycled
         */
        public void cancelImageLoading() {
            // Only clear if we actually have an ImageView in this layout
            if (attachmentImage != null) {
                glideRequestManager.clear(attachmentImage);

                if (pendingThumbnailTask != null) {
                    attachmentImage.removeCallbacks(pendingThumbnailTask);
                    pendingThumbnailTask = null;
                }
            }
        }
    }
}
