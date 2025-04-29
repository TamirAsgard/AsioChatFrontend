package com.example.asiochatfrontend.ui.group;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.ui.group.adapter.GroupMembersAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GroupInfoActivity extends AppCompatActivity {
    private static final String TAG = "GroupInfoActivity";
    private static final String PREFS_NAME = "AsioChat_Prefs";

    private GroupViewModel viewModel;
    private RecyclerView membersList;
    private GroupMembersAdapter adapter;
    private MaterialTextView groupNameText;
    private ShapeableImageView groupImage;
    private MaterialButton editButton;
    private Button addMemberButton;

    private String chatId;
    private String groupName;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_settings);

        // Get intent data
        chatId = getIntent().getStringExtra("CHAT_ID");
        groupName = getIntent().getStringExtra("CHAT_NAME");

        // Get current user ID from shared preferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", "");

        if (chatId == null || chatId.isEmpty()) {
            Toast.makeText(this, "Invalid group ID", Toast.LENGTH_SHORT).show();
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
        membersList = findViewById(R.id.chat_setting_LST_users);
        groupNameText = findViewById(R.id.chat_setting_MTV_title);
        groupImage = findViewById(R.id.chat_setting_SIV_img);
        editButton = findViewById(R.id.chat_setting_BTN_search);
        addMemberButton = findViewById(R.id.chat_setting_BTN_new_user);
    }

    private void setupViewModel() {
        GroupViewModelFactory factory =
                new GroupViewModelFactory(ServiceModule.getConnectionManager());

        viewModel = new ViewModelProvider(this, factory)
                .get(GroupViewModel.class);

        viewModel.initialize(chatId, currentUserId);

        // Observe group data
        viewModel.getGroupData().observe(this, group -> {
            if (group != null) {
                groupName = group.getChatName();
                updateGroupHeader();
            }
        });

        // Observe group members
        viewModel.getGroupMembers().observe(this, members -> {
            adapter.submitList(members);
        });

        // Observe errors
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupUIComponents() {
        // Set group name and image
        updateGroupHeader();

        // Set up RecyclerView for members
        membersList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GroupMembersAdapter(
                member -> showMemberOptionsDialog(member.getJid()),
                currentUserId
        );
        membersList.setAdapter(adapter);
    }

    private void updateGroupHeader() {
        groupNameText.setText(groupName != null && !groupName.isEmpty() ? groupName : "Group");
        groupImage.setImageResource(R.drawable.groups_icon);
    }

    private void setupClickListeners() {
        // Edit button click
        editButton.setOnClickListener(v -> showEditGroupNameDialog());

        // Add member button click
        addMemberButton.setOnClickListener(v -> showAddMemberDialog());
    }

    private void showEditGroupNameDialog() {
        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.popup_edit_title, null);
        EditText nameInput = dialogView.findViewById(R.id.edit_ET_Title);
        MaterialButton saveButton = dialogView.findViewById(R.id.save_button);

        // Set current group name
        nameInput.setText(groupName);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        saveButton.setOnClickListener(v -> {
            String newName = nameInput.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "Group name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update group name
            viewModel.updateGroupName(newName);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showAddMemberDialog() {
        // In a real app, this would show a list of contacts to add
        // For now, we'll just show a message
        Toast.makeText(this, "Add member functionality coming soon", Toast.LENGTH_SHORT).show();
    }

    private void showMemberOptionsDialog(String memberId) {
        // Inflate the dialog layout
        View dialogView = getLayoutInflater().inflate(R.layout.popup_user_chat, null);
        MaterialButton messageButton = dialogView.findViewById(R.id.popup_user_BTN_message);
        MaterialButton removeButton = dialogView.findViewById(R.id.popup_user_BTN_remove);
        MaterialButton adminButton = dialogView.findViewById(R.id.popup_user_BTN_admin);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Set up buttons
        messageButton.setOnClickListener(v -> {
            // Start private chat with this member
            viewModel.startPrivateChat(memberId);
            dialog.dismiss();
            finish(); // Close this activity
        });

        removeButton.setOnClickListener(v -> {
            // Remove member from group
            viewModel.removeMemberFromGroup(memberId);
            dialog.dismiss();
        });

        adminButton.setOnClickListener(v -> {
            // Make member an admin (not implemented in this version)
            Toast.makeText(this, "Admin functionality coming soon", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        // Disable remove/admin buttons if the member is the current user
        if (memberId.equals(currentUserId)) {
            removeButton.setEnabled(false);
            adminButton.setEnabled(false);
        }

        dialog.show();
    }
}