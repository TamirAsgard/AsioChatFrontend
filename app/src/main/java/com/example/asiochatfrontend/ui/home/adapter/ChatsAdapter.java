package com.example.asiochatfrontend.ui.home.adapter;

import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.MediaMessageDto;
import com.example.asiochatfrontend.core.model.dto.TextMessageDto;
import com.example.asiochatfrontend.core.model.dto.abstracts.MessageDto;
import com.example.asiochatfrontend.domain.repository.MessageRepository;
import com.example.asiochatfrontend.ui.chat.bus.ChatUpdateBus;
import com.example.asiochatfrontend.ui.home.HomeViewModel;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class ChatsAdapter extends ListAdapter<ChatDto, ChatsAdapter.ChatViewHolder> {

    private static final DiffUtil.ItemCallback<ChatDto> DIFF_CALLBACK = new DiffUtil.ItemCallback<ChatDto>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatDto oldItem, @NonNull ChatDto newItem) {
            return oldItem.getChatId().equals(newItem.getChatId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatDto oldItem, @NonNull ChatDto newItem) {
            boolean isParticipantsTheSame = oldItem.getRecipients() == null ? newItem.getRecipients() == null : oldItem.getRecipients().equals(newItem.getRecipients());
            boolean isSameName = oldItem.getChatName().equals(newItem.getChatName());
            return isParticipantsTheSame && isSameName;
        }
    };

    private final OnChatClickListener clickListener;
    private final HomeViewModel viewModel;
    private final MessageRepository messageRepository;
    private final String currentUserId;

    /**
     * Update a specific chat item with its new last message
     */
    public void updateLastMessage(String chatId) {
        // Find the chat in the current list
        for (int i = 0; i < getCurrentList().size(); i++) {
            ChatDto chat = getCurrentList().get(i);
            if (chat.getChatId().equals(chatId)) {
                // Notify item changed to reload the message
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void updateUnreadCount(String chatId) {
        List<ChatDto> list = getCurrentList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getChatId().equals(chatId)) {
                // Notify item changed to reload the count
                notifyItemChanged(i);
                return;
            }
        }
    }

    public interface OnChatClickListener {
        void onChatClick(ChatDto chat);
    }

    public ChatsAdapter(OnChatClickListener clickListener, HomeViewModel viewModel,
                        MessageRepository messageRepository, String currentUserId) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
        this.viewModel = viewModel;
        this.messageRepository = messageRepository;
        this.currentUserId = currentUserId;
    }

    @Override
    public void submitList(@Nullable List<ChatDto> list) {
        if (list != null) {
            // copy & sort ascending by lastMessage.timestamp
            List<ChatDto> sorted = new ArrayList<>(list);
            List<MessageDto> lastMessages = new ArrayList<>();

            sorted.sort((chat1, chat2) -> {
                // 1) fetch the two “last” messages (may be null)
                MessageDto last1 = viewModel.getLastMessageForChat(chat1.getChatId());
                MessageDto last2 = viewModel.getLastMessageForChat(chat2.getChatId());

                // 2) extract millis, defaulting to Long.MIN_VALUE if we’re missing data
                long t1 = (last1 != null && last1.getTimestamp() != null)
                        ? last1.getTimestamp().getTime()
                        : Long.MIN_VALUE;
                long t2 = (last2 != null && last2.getTimestamp() != null)
                        ? last2.getTimestamp().getTime()
                        : Long.MIN_VALUE;

                // 3) compare so that highest (newest) comes first
                return Long.compare(t2, t1);
            });

            super.submitList(sorted);
        } else {
            super.submitList(null);
        }
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);
        return new ChatViewHolder(view, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatDto chat = getItem(position);

        // Get the last message with priority:
        // 1. From latest messages map in ChatUpdateBus
        // 2. From ViewModel's getLastMessageForChat method
        MessageDto lastMessage = null;
        Map<String, MessageDto> latestMessages =
                ChatUpdateBus.getLatestMessagesMap().getValue();

        if (latestMessages != null && latestMessages.containsKey(chat.getChatId())) {
            lastMessage = latestMessages.get(chat.getChatId());
        }

        if (lastMessage == null) {
            lastMessage = viewModel.getLastMessageForChat(chat.getChatId());
        }
        // Get the unread count from the ChatUpdateBus
        Map<String, Integer> unreadMap = ChatUpdateBus.getUnreadCountUpdates().getValue();
        int unreadCount = 0;

        if (unreadMap != null && unreadMap.containsKey(chat.getChatId())) {
            unreadCount = unreadMap.get(chat.getChatId());
        } else {
            // Always update the unread count in background to ensure it's fresh
            // This will both initialize missing values and refresh existing ones
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    int freshCount = viewModel.getUnreadMessageCountForChat(chat.getChatId());
                    if (freshCount > 0) {
                        ChatUpdateBus.postUnreadCountUpdate(chat.getChatId(), freshCount);
                    }
                } catch (Exception e) {
                    Log.e("ChatsAdapter", "Error updating unread count: " +
                            (e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Unknown error"));
                }
            });
        }

        // Pass the current unread count to bind
        holder.bind(chat, lastMessage, unreadCount, currentUserId);
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView profileImage;
        private final MaterialTextView titleText;
        private final MaterialTextView senderNameText;
        private final MaterialTextView lastMessageText;
        private final MaterialTextView timeText;
        private final MaterialTextView messageCounterText;
        private final SimpleDateFormat timeFormat;
        private final SimpleDateFormat dateFormat;
        private final OnChatClickListener listener;

        ChatViewHolder(@NonNull View itemView, OnChatClickListener listener) {
            super(itemView);
            this.listener = listener;

            profileImage = itemView.findViewById(R.id.chat_item_SIV_img);
            titleText = itemView.findViewById(R.id.chat_item_MTV_title);
            senderNameText = itemView.findViewById(R.id.chat_item_MTV_name);
            lastMessageText = itemView.findViewById(R.id.chat_item_MTV_last_message);
            timeText = itemView.findViewById(R.id.chat_item_MTV_time);
            messageCounterText = itemView.findViewById(R.id.chat_item_MTV_message_counter);

            timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        }

        void bind(ChatDto chat, MessageDto lastMessage, int unreadCounts, String currentUserId) {
            // Set chat title
            titleText.setText(getChatDisplayName(chat, currentUserId));

            // Set profile image based on chat type
            if (chat.getGroup()) {
                profileImage.setImageResource(R.drawable.groups_icon);
            } else {
                profileImage.setImageResource(R.drawable.default_profile_icon);
            }

            // Handle last message
            if (lastMessage != null) {
                // validate payload isn't null or empty
                boolean hasPayload = false;
                if (lastMessage instanceof TextMessageDto) {
                    hasPayload = ((TextMessageDto) lastMessage).getPayload() != null;
                } else if (lastMessage instanceof MediaMessageDto) {
                    hasPayload = ((MediaMessageDto) lastMessage).getPayload() != null;
                }

                if (!hasPayload) {
                    senderNameText.setText("");
                    lastMessageText.setText("No messages yet");
                    timeText.setText("");
                } else {

                    // Determine sender name display
                    String senderPrefix = "";
                    if (chat.getGroup()) {
                        // In group chats, show sender's name
                        senderPrefix = lastMessage.getJid().equals(currentUserId) ?
                                "You: " : "";
                    } else if (lastMessage.getJid().equals(currentUserId)) {
                        // In private chats, only show "You: " for your own messages
                        senderPrefix = "You: ";
                    }
                    senderNameText.setText(senderPrefix);

                    // Set message content
                    String content;
                    if (lastMessage instanceof TextMessageDto) {
                        content = ((TextMessageDto) lastMessage).getPayload();
                    } else {
                        MediaMessageDto mediaMessageDto = (MediaMessageDto) lastMessage;
                        content = "[Media] ";
                        if (mediaMessageDto.getPayload() != null && mediaMessageDto.getPayload().getType() != null) {
                            content += mediaMessageDto.getPayload().getType();
                        }
                    }

                    String lastMessagePlainText = !lastMessage.getJid().equals(currentUserId) ?
                            lastMessage.getJid() + ": " + content : content;
                    lastMessageText.setText(lastMessagePlainText);

                    // Set time
                    if (lastMessage.getTimestamp() != null) {
                        timeText.setText(formatMessageTime(lastMessage.getTimestamp()));
                    } else {
                        lastMessage.setTimestamp(new Date());
                        timeText.setText(formatMessageTime(lastMessage.getTimestamp()));
                    }
                }
            } else {
                senderNameText.setText("");
                lastMessageText.setText("No messages yet");
                timeText.setText("");
            }

            if (unreadCounts > 0) {
                messageCounterText.setVisibility(View.VISIBLE);
                messageCounterText.setText(String.valueOf(unreadCounts));
            } else {
                messageCounterText.setVisibility(View.GONE);
            }

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onChatClick(chat);
                }
            });
        }

        private String getChatDisplayName(ChatDto chat, String currentUserId) {
            if (chat.getGroup()) {
                return chat.getChatName();
            } else {

                String recipientId = chat.getRecipients().
                        stream()
                        .filter(id -> !id.equals(currentUserId))
                        .collect(Collectors.toList())
                        .get(0);

                return recipientId.substring(0, 1).toUpperCase() + recipientId.substring(1);
            }
        }

        private String formatMessageTime(Date timestamp) {
            if (timestamp == null) return "";

            Date now = new Date();
            long diffMillis = now.getTime() - timestamp.getTime();
            long diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis);

            if (diffDays == 0) {
                // Today - show time
                return timeFormat.format(timestamp);
            } else if (diffDays == 1) {
                // Yesterday
                return "Yesterday";
            } else if (diffDays < 7) {
                // Last week - show day of week
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                return dayFormat.format(timestamp);
            } else {
                // Older - show date
                return dateFormat.format(timestamp);
            }
        }
    }
}