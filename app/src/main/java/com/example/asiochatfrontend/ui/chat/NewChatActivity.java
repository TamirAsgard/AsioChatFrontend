package com.example.asiochatfrontend.ui.chat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
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
import com.example.asiochatfrontend.core.model.enums.ChatType;
import com.example.asiochatfrontend.ui.contacts.ContactsViewModel;
import com.example.asiochatfrontend.ui.contacts.ContactsViewModelFactory;
import com.example.asiochatfrontend.ui.contacts.adapter.ContactsAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

    private Handler handler;
    private boolean inSearchMode = false;

    private final Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (viewModel != null && !inSearchMode) {
                viewModel.refresh();      // <— your "refresh" method
                handler.postDelayed(this, 30_000); // schedule again in 30s
            }
        }
    };

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

        // Start the refresh handler
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // kick off the first run as soon as the Activity is visible
        try {
            if (handler != null)
                handler.post(refreshRunnable);
        } catch (IllegalStateException e) {
            // This can happen if the activity is not in a valid state to post a runnable
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // make sure we don’t keep the callbacks running once the Activity is gone
        try {
            if (handler != null)
                handler.removeCallbacks(refreshRunnable);
        } catch (IllegalStateException e) {
            // This can happen if the activity is not in a valid state to remove callbacks
            e.printStackTrace();
        }
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
                intent.putExtra("CHAT_ID", chat.getChatId());
                intent.putExtra("CHAT_NAME", chat.getChatName());
                intent.putExtra("CHAT_TYPE", chat.isGroup ? ChatType.GROUP.name() : ChatType.PRIVATE.name());
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
    }

    private void setupClickListeners() {
        // Back button
        backButton.setOnClickListener(v -> finish());

        // Search button
        searchButton.setOnClickListener(v -> showSearchContactsDialog());

        // Start chat button
        startChatFab.setOnClickListener(v -> {
            List<String> selectedIds = adapter.getSelectedUserIds();
            if (selectedIds.isEmpty()) {
                Toast.makeText(this, "Please select at least one contact", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedIds.size() == 1) {
                viewModel.createPrivateChat(selectedIds.get(0));
            } else {
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

    // Helpers

    /**
     * Pops up a simple AlertDialog with an EditText,
     * and when the user taps “Search” we call filterContacts().
     */
    private void showSearchContactsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Search Contacts");

        final EditText input = new EditText(this);
        input.setHint("Type contact name…");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Search", (dialog, which) -> {
            String query = input.getText().toString().trim();

            if (query.isEmpty()) {
                // empty → exit search mode
                inSearchMode = false;
                viewModel.filterContacts("");          // shows all again
                handler.post(refreshRunnable);         // optionally kick off an immediate refresh
            } else {
                // non-empty → enter search mode
                inSearchMode = true;
                viewModel.filterContacts(query);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // user cancelled → exit search mode
            inSearchMode = false;
            viewModel.filterContacts("");              // restore full list
            handler.post(refreshRunnable);
        });

        builder.show();
    }
}