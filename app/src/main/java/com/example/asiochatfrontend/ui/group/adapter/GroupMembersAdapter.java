package com.example.asiochatfrontend.ui.group.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

public class GroupMembersAdapter extends ListAdapter<UserDto, GroupMembersAdapter.GroupMemberViewHolder> {

    private static final DiffUtil.ItemCallback<UserDto> DIFF_CALLBACK = new DiffUtil.ItemCallback<UserDto>() {
        @Override
        public boolean areItemsTheSame(@NonNull UserDto oldItem, @NonNull UserDto newItem) {
            return oldItem.getJid().equals(newItem.getJid());
        }

        @Override
        public boolean areContentsTheSame(@NonNull UserDto oldItem, @NonNull UserDto newItem) {
            return oldItem.getJid().equals(newItem.getJid());
        }
    };

    private final OnMemberClickListener clickListener;
    private final String currentUserId;

    public interface OnMemberClickListener {
        void onMemberClick(UserDto member);
    }

    public GroupMembersAdapter(OnMemberClickListener clickListener, String currentUserId) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public GroupMemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item, parent, false);
        return new GroupMemberViewHolder(view, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupMemberViewHolder holder, int position) {
        UserDto user = getItem(position);
        holder.bind(user, user.getJid().equals(currentUserId));
    }

    static class GroupMemberViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView profileImage;
        private final MaterialTextView nameText;
        private final OnMemberClickListener listener;

        GroupMemberViewHolder(@NonNull View itemView, OnMemberClickListener listener) {
            super(itemView);
            this.listener = listener;

            profileImage = itemView.findViewById(R.id.user_item_SIV_img);
            nameText = itemView.findViewById(R.id.user_item_MTV_title);
        }

        void bind(UserDto user, boolean isCurrentUser) {
            // Set user name
            String displayName = user.getJid();
            if (displayName == null || displayName.isEmpty()) {
                displayName = "User " + user.getJid().substring(0, 8);
            }

            // Add "(You)" suffix if this is the current user
            if (isCurrentUser) {
                displayName += " (You)";
            }

            nameText.setText(displayName);

            // Set profile image
            profileImage.setImageResource(R.drawable.default_profile_icon);

            // TODO Set online status indicator
//            if (user.isOnline()) {
//                profileImage.setAlpha(1.0f);
//            } else {
//                profileImage.setAlpha(0.5f);
//            }

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMemberClick(user);
                }
            });
        }
    }
}