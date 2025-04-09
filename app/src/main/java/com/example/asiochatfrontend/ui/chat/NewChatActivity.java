package com.example.asiochatfrontend.ui.chat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.core.model.enums.ChatType;
import com.example.asiochatfrontend.ui.contacts.ContactsViewModel;
import com.example.asiochatfrontend.ui.contacts.ContactsViewModelFactory;
import com.example.asiochatfrontend.ui.contacts.adapter.ContactsAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NewChatActivity extends AppCompatActivity {
    private static final String TAG = "NewChatActivity";
    private static final String PREFS_NAME = "AsioChat_Prefs";

    private ContactsViewModel viewModel;
    private RecyclerView contactList;
    private ContactsAdapter adapter;
    private FloatingActionButton startChatFab;
    private MaterialButton backButton;
    private EditText searchInput;
    private MaterialButton searchButton;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_chat);

        // Get current user ID from shared preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", "");

        if (currentUserId.isEmpty()) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        initializeViews();

        // Set up view model
        setupViewModel();

        // Set up UI components
        setupUIComponents();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        contactList = findViewById(R.id.new_chat_LST_chats);
        startChatFab = findViewById(R.id.fab_start_new_chat);
        backButton = findViewById(R.id.new_search_BTN_back);
        searchButton = findViewById(R.id.new_top_bar_BTN_search);
    }

    private void setupViewModel() {
        ContactsViewModelFactory factory = new ContactsViewModelFactory(ServiceModule.getConnectionManager());
        viewModel = new ViewModelProvider(this, factory).get(ContactsViewModel.class);
        viewModel.setCurrentUserId(currentUserId);

        // Observe contacts
        viewModel.getContacts().observe(this, contacts -> adapter.submitList(contacts));

        // Observe chat created
        viewModel.getCreatedChat().observe(this, chat -> {
            if (chat != null) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("CHAT_ID", chat.getId());
                intent.putExtra("CHAT_NAME", chat.getName());
                intent.putExtra("CHAT_TYPE", chat.getType().name());
                startActivity(intent);
                finish();
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupUIComponents() {
        // Set up RecyclerView
        contactList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactsAdapter();
        contactList.setAdapter(adapter);

        // TODO future improvement: add search functionality
        // Set up search functionality
//        searchInput.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                // Filter contacts as user types
//                viewModel.filterContacts(s.toString());
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {}
//        });
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Search button
        searchButton.setOnClickListener(v -> {
            // String query = searchInput.getText().toString().trim();
            // viewModel.filterContacts(query);
        });

        // Start chat button
        startChatFab.setOnClickListener(v -> {
            List<String> selectedIds = adapter.getSelectedUserIds();

            if (selectedIds.isEmpty()) {
                Toast.makeText(this, "Please select at least one contact", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedIds.size() == 1) {
                // Create private chat
                viewModel.createPrivateChat(selectedIds.get(0));
            } else {
                // Show dialog to get group name
                showGroupNameDialog(selectedIds);
            }
        });
    }

    private void showGroupNameDialog(List<String> memberIds) {
        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.popup_group_title, null);
        EditText nameInput = dialogView.findViewById(R.id.ET_Title);
        MaterialButton okButton = dialogView.findViewById(R.id.ok_button);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        okButton.setOnClickListener(v -> {
            String groupName = nameInput.getText().toString().trim();
            if (groupName.isEmpty()) {
                Toast.makeText(this, "Please enter a group name", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create group chat
            viewModel.createGroupChat(groupName, memberIds);
            dialog.dismiss();
        });

        dialog.show();
    }
}