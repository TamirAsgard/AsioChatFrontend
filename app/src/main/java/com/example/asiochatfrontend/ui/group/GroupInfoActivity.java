package com.example.asiochatfrontend.ui.group;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.model.dto.UserDto;
import com.example.asiochatfrontend.ui.group.adapter.GroupMembersAdapter;
import com.example.asiochatfrontend.ui.group.GroupViewModel;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.material.imageview.ShapeableImageView;

public class GroupInfoActivity extends AppCompatActivity {
    private GroupViewModel viewModel;
    private GroupMembersAdapter adapter;
    private RecyclerView userList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_settings);

        viewModel = new ViewModelProvider(this).get(GroupViewModel.class);
        userList = findViewById(R.id.chat_setting_LST_users);
        adapter = new GroupMembersAdapter();
        userList.setAdapter(adapter);

        viewModel.getGroupMembers().observe(this, adapter::submitList);

        Button addMemberButton = findViewById(R.id.chat_setting_BTN_new_user);
        addMemberButton.setOnClickListener(v -> viewModel.addDummyUser());
    }
}