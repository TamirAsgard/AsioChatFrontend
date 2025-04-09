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
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class ChatsAdapter extends ListAdapter<ChatDto, ChatsAdapter.ChatViewHolder> {

    private static final DiffUtil.ItemCallback<ChatDto> DIFF_CALLBACK = new DiffUtil.ItemCallback<ChatDto>() {
        @Override
        public boolean areItemsTheSame(@NonNull ChatDto oldItem, @NonNull ChatDto newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull ChatDto oldItem, @NonNull ChatDto newItem) {
            return oldItem.getLastMessage().getId().equals(newItem.getLastMessage().getId()) &&
                    oldItem.getUnreadCount() == newItem.getUnreadCount() &&
                    oldItem.getName().equals(newItem.getName());
        }
    };

    private final OnChatClickListener clickListener;

    public interface OnChatClickListener {
        void onChatClick(ChatDto chat);
    }

    public ChatsAdapter(OnChatClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);
        return new ChatViewHolder(view, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView profileImage;
        private final MaterialTextView titleText;
        private final MaterialTextView senderNameText;
        private final MaterialTextView lastMessageText;
        private final MaterialTextView timeText;
        private final MaterialTextView messageCounterText;
        private final SimpleDateFormat timeFormat;
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
        }

        void bind(ChatDto chat) {
            // Set chat title
            titleText.setText(getChatDisplayName(chat));

            // Set profile image based on chat type
            if (chat.getType() == ChatType.GROUP) {
                profileImage.setImageResource(R.drawable.groups_icon);
            } else {
                profileImage.setImageResource(R.drawable.default_profile_icon);
            }

            // Set last message information
            MessageDto lastMessage = chat.getLastMessage();
            if (lastMessage != null) {
                // Determine sender name display
                String senderPrefix = lastMessage.getSenderId() + ": ";
                senderNameText.setText(senderPrefix);

                // Set message content
                String content = lastMessage.getContent();
                if (content == null || content.isEmpty()) {
                    if (lastMessage.getMediaId() != null) {
                        content = "[Media attachment]";
                    } else {
                        content = "";
                    }
                }
                lastMessageText.setText(content);

                // Set time
                if (lastMessage.getCreatedAt() != null) {
                    timeText.setText(timeFormat.format(lastMessage.getCreatedAt()));
                } else {
                    timeText.setText("");
                }
            } else {
                senderNameText.setText("");
                lastMessageText.setText("No messages yet");
                timeText.setText("");
            }

            // Set unread message counter
            int unreadCount = chat.getUnreadCount();
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
            if (chat.getType() == ChatType.GROUP) {
                return chat.getName();
            } else {
                // For private chats, we should ideally get the name of the other user
                // For now, we'll use the chat name or ID as fallback
                return chat.getName() != null && !chat.getName().isEmpty()
                        ? chat.getName()
                        : "Chat " + chat.getId().substring(0, 8);
            }
        }
    }
}