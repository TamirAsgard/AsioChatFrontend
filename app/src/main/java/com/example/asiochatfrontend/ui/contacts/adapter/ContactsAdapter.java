package com.example.asiochatfrontend.ui.contacts.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.model.dto.UserDto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {
    private final List<UserDto> contacts = new ArrayList<>();
    private final Set<String> selectedIds = new HashSet<>();

    public void submitList(List<UserDto> newContacts) {
        contacts.clear();
        contacts.addAll(newContacts);
        notifyDataSetChanged();
    }

    public List<String> getSelectedUserIds() {
        return new ArrayList<>(selectedIds);
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_item, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        UserDto user = contacts.get(position);
        holder.title.setText(user.getJid());
        holder.itemView.setOnClickListener(v -> {
            if (selectedIds.contains(user.getJid())) {
                selectedIds.remove(user.getJid());
                holder.selector.setImageResource(R.drawable.check_box_empty_icon);
            } else {
                selectedIds.add(user.getJid());
                holder.selector.setImageResource(R.drawable.check_box_full_icon);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView selector;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.contact_item_MTV_title);
            selector = itemView.findViewById(R.id.contact_V_SIV_img);
        }
    }
}