package com.example.asiochatfrontend.ui.chat;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.ui.contacts.adapter.ContactsAdapter;
import com.example.asiochatfrontend.ui.contacts.ContactsViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class NewChatActivity extends AppCompatActivity {
    private ContactsViewModel viewModel;
    private RecyclerView contactList;
    private ContactsAdapter adapter;
    private FloatingActionButton startChatFab;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_chat);

        viewModel = new ViewModelProvider(this).get(ContactsViewModel.class);
        contactList = findViewById(R.id.new_chat_LST_chats);
        startChatFab = findViewById(R.id.fab_start_new_chat);

        adapter = new ContactsAdapter();
        contactList.setAdapter(adapter);

        viewModel.getContacts().observe(this, adapter::submitList);

        startChatFab.setOnClickListener(v -> {
            List<String> selectedIds = adapter.getSelectedUserIds();
            viewModel.startPrivateChat(selectedIds);
            finish();
        });
    }
}