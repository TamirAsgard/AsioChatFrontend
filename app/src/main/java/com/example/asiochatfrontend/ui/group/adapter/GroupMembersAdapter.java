package com.example.asiochatfrontend.ui.group.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import java.util.ArrayList;
import java.util.List;

public class GroupMembersAdapter extends RecyclerView.Adapter<GroupMembersAdapter.GroupMemberViewHolder> {
    private final List<UserDto> users = new ArrayList<>();

    public void submitList(List<UserDto> newUsers) {
        users.clear();
        users.addAll(newUsers);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GroupMemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item, parent, false);
        return new GroupMemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupMemberViewHolder holder, int position) {
        UserDto user = users.get(position);
        holder.name.setText(user.name);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class GroupMemberViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView avatar;

        GroupMemberViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.user_item_MTV_title);
            avatar = itemView.findViewById(R.id.user_item_SIV_img);
        }
    }
}