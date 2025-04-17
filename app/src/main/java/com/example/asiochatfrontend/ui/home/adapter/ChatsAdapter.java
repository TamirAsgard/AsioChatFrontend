package com.example.asiochatfrontend.ui.home.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.core.model.enums.ChatType;
import com.example.asiochatfrontend.domain.repository.MessageRepository;
import com.example.asiochatfrontend.ui.home.HomeViewModel;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ChatsAdapter extends ListAdapter<ChatDto, ChatsAdapter.ChatViewHolder> {

    private static final DiffUtil.ItemCallback<ChatDto> DIFF_CALLBACK = new DiffUtil.ItemCallback<ChatDto>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatDto oldItem, @NonNull ChatDto newItem) {
            return oldItem.getChatId().equals(newItem.getChatId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatDto oldItem, @NonNull ChatDto newItem) {
            boolean isParticipantsTheSame = oldItem.getRecipients() == null ? newItem.getRecipients() == null : oldItem.getRecipients().equals(newItem.getRecipients());
            return isParticipantsTheSame;
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

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);
        return new ChatViewHolder(view, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatDto chat = getItem(position);
        MessageDto lastMessage = null;

        // Try to get last message from ViewModel cache
        lastMessage = viewModel.getLastMessageForChat(chat.getChatId());

        // If not in cache, try to load from repository
        if (lastMessage == null) {
            try {
                lastMessage = messageRepository.getMessageById(lastMessage.getId());
            } catch (Exception e) {
                // Silent catch - we'll just show "No messages yet"
            }
        }

        holder.bind(chat, lastMessage, currentUserId);
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

        void bind(ChatDto chat, MessageDto lastMessage, String currentUserId) {
            // Set chat title
            titleText.setText(getChatDisplayName(chat));

            // Set profile image based on chat type
            if (chat.getGroup()) {
                profileImage.setImageResource(R.drawable.groups_icon);
            } else {
                profileImage.setImageResource(R.drawable.default_profile_icon);
            }

            // Handle last message
            if (lastMessage != null) {
                // Determine sender name display
                String senderPrefix = "";
                if (chat.getGroup()) {
                    // In group chats, show sender's name
                    senderPrefix = lastMessage.getJid().equals(currentUserId) ?
                            "You: " : lastMessage.getJid() + ": ";
                } else if (lastMessage.getJid().equals(currentUserId)) {
                    // In private chats, only show "You: " for your own messages
                    senderPrefix = "You: ";
                }
                senderNameText.setText(senderPrefix);

                // Set message content
                String content = lastMessage.getPayload();
                lastMessageText.setText(content);

                // Set time
                if (lastMessage.getTimestamp() != null) {
                    timeText.setText(formatMessageTime(lastMessage.getTimestamp()));
                } else {
                    timeText.setText("");
                }
            } else {
                senderNameText.setText("");
                lastMessageText.setText("No messages yet");
                timeText.setText("");
            }

            // Set unread message counter
            int unreadCount = 0;
            if (unreadCount > 0) {
                messageCounterText.setVisibility(View.VISIBLE);
                messageCounterText.setText(String.valueOf(unreadCount));
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
        private String getChatDisplayName(ChatDto chat) {
            if (chat.getGroup()) {
                return chat.getChatName();
            } else {
                String recipientId = chat.getRecipients().get(0);
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