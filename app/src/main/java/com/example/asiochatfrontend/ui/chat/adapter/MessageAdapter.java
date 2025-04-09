package com.example.asiochatfrontend.ui.chat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import java.util.ArrayList;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<MessageDto> messageList = new ArrayList<>();

    public void submitList(List<MessageDto> messages) {
        this.messageList = messages;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        MessageDto message = messageList.get(position);
        holder.message.setText(message.content);
        holder.timestamp.setText(message.createdAt != null ? message.createdAt.toString() : "");
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView message, timestamp;
        ImageView statusIcon;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.message_MTV_message);
            timestamp = itemView.findViewById(R.id.message_MTV_time);
            statusIcon = itemView.findViewById(R.id.message_SIV_double_check_1);
        }
    }
}