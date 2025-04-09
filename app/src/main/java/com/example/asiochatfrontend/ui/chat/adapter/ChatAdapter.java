package com.example.asiochatfrontend.ui.chat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<ChatDto> chatList = new ArrayList<>();

    public void submitList(List<ChatDto> newList) {
        this.chatList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatDto chat = chatList.get(position);
        holder.title.setText(chat.name);
        holder.lastMessage.setText(chat.lastMessage != null ? chat.lastMessage.content : "");
        holder.time.setText(chat.updatedAt != null ? chat.updatedAt.toString() : "");
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView title, lastMessage, time;
        ImageView profile;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.chat_item_MTV_title);
            lastMessage = itemView.findViewById(R.id.chat_item_MTV_last_message);
            time = itemView.findViewById(R.id.chat_item_MTV_time);
            profile = itemView.findViewById(R.id.chat_item_SIV_img);
        }
    }
}