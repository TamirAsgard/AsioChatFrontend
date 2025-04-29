package com.example.asiochatfrontend.ui.group;

import android.content.Intent;
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
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.core.model.enums.ChatType;
import com.example.asiochatfrontend.domain.usecase.user.GetAllUsersUseCase;
import com.example.asiochatfrontend.ui.chat.ChatActivity;
import com.example.asiochatfrontend.ui.group.adapter.GroupMembersAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

        viewModel.getCreatedPrivateChat().observe(this, chat -> {
            if (chat != null) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("CHAT_ID", chat.getChatId());
                String chatName = chat.getChatName();
                if (!chat.isGroup) {
                    chatName = chat.getRecipients().stream()
                            .filter(id -> !id.equals(currentUserId))
                            .map(name ->
                                    Character.toUpperCase(name.charAt(0)) + name.substring(1)
                            )
                            .collect(Collectors.joining(", "));
                }

                intent.putExtra("CHAT_NAME", chatName);
                intent.putExtra("CHAT_TYPE", chat.isGroup ? ChatType.GROUP.name() : ChatType.PRIVATE.name());
                startActivity(intent);
                finish();
            }
        });
    }

    private void setupUIComponents() {
        // Set group name and image
        updateGroupHeader();

        // Set up RecyclerView for members
        membersList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GroupMembersAdapter(
                member -> {
                    if (!member.getJid().equals(currentUserId)) {
                        showMemberOptionsDialog(member.getJid());
                    }
                },
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
        // fetch & filter off the UI thread
        new Thread(() -> {
            try {
                // 1. load all users
                List<UserDto> allUsers = new GetAllUsersUseCase(
                        ServiceModule.getConnectionManager()
                ).execute();

                // 2. build exclude‐set (current + existing members)
                ChatDto group = viewModel.getGroupData().getValue();
                Set<String> exclude = new HashSet<>(group.getRecipients());
                exclude.add(currentUserId);

                // 3. filter candidates
                List<UserDto> candidates = allUsers.stream()
                        .filter(u -> !exclude.contains(u.getJid()))
                        .collect(Collectors.toList());

                // nothing to add?
                if (candidates.isEmpty()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "No contacts available", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                // 4. prepare name/jid arrays for the dialog
                String[] names     = new String[candidates.size()];
                String[] jids      = new String[candidates.size()];
                boolean[] checked  = new boolean[candidates.size()];
                for (int i = 0; i < candidates.size(); i++) {
                    UserDto u = candidates.get(i);
                    // capitalize first letter for display
                    String display = Character.toUpperCase(u.getJid().charAt(0))
                            + u.getJid().substring(1);
                    names[i]    = display;
                    jids[i]     = u.getJid();
                    checked[i]  = false;
                }

                // 5. show the multi-choice dialog on the UI thread
                runOnUiThread(() -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Add members to group")
                            .setMultiChoiceItems(names, checked, (dialog, which, isChecked) -> {
                                checked[which] = isChecked;
                            })
                            .setPositiveButton("Add", (dialog, which) -> {
                                // for each checked, call your use-case
                                for (int i = 0; i < jids.length; i++) {
                                    if (checked[i]) {
                                        viewModel.addMemberToGroup(jids[i]);
                                    }
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to load contacts", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
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
            messageButton.setEnabled(false);
            removeButton.setEnabled(false);
            adminButton.setEnabled(false);
        }

        dialog.show();
    }
}