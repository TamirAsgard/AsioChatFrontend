package com.example.asiochatfrontend.ui.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.model.dto.MessageDto;
import com.example.asiochatfrontend.ui.chat.adapter.MessageAdapter;
import com.example.asiochatfrontend.ui.chat.ChatViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ChatActivity extends AppCompatActivity {
    private ChatViewModel viewModel;
    private RecyclerView messageList;
    private MessageAdapter adapter;
    private EditText inputMessage;
    private FloatingActionButton sendButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        messageList = findViewById(R.id.chat_LST_messages);
        inputMessage = findViewById(R.id.chat_ET_message);
        sendButton = findViewById(R.id.chat_FAB_send);

        adapter = new MessageAdapter();
        messageList.setAdapter(adapter);

        viewModel.getMessages().observe(this, messages -> adapter.submitList(messages));

        sendButton.setOnClickListener(v -> {
            String text = inputMessage.getText().toString();
            if (!text.isEmpty()) {
                viewModel.sendMessage(text);
                inputMessage.setText("");
            }
        });
    }
}