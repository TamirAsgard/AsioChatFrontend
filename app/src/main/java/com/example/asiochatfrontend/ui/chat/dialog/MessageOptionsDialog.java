package com.example.asiochatfrontend.ui.chat.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.example.asiochatfrontend.R;

public class MessageOptionsDialog extends Dialog {
    private final OnMessageOptionSelected listener;

    public interface OnMessageOptionSelected {
        void onReply();
        void onDelete();
        void onForward();
    }

    public MessageOptionsDialog(Context context, OnMessageOptionSelected listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_message_options);

        Button replyBtn = findViewById(R.id.option_reply);
        Button deleteBtn = findViewById(R.id.option_delete);
        Button forwardBtn = findViewById(R.id.option_forward);

        replyBtn.setOnClickListener(v -> {
            listener.onReply();
            dismiss();
        });

        deleteBtn.setOnClickListener(v -> {
            listener.onDelete();
            dismiss();
        });

        forwardBtn.setOnClickListener(v -> {
            listener.onForward();
            dismiss();
        });
    }
}
