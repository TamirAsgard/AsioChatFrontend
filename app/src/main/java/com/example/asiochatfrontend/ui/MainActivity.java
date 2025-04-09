import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.example.asiochatfrontend.R;
import com.example.asiochatfrontend.core.model.dto.ChatDto;
import com.example.asiochatfrontend.ui.home.HomeViewModel;
import com.example.asiochatfrontend.ui.home.adapter.ChatsAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView chatList;
    private ChatsAdapter adapter;
    private HomeViewModel viewModel;
    private FloatingActionButton fabNewChat;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatList = findViewById(R.id.main_LST_chats);
        fabNewChat = findViewById(R.id.fab_new_chat);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        adapter = new ChatsAdapter();
        chatList.setAdapter(adapter);

        viewModel.getChats().observe(this, chats -> adapter.submitList(chats));

        fabNewChat.setOnClickListener(v -> startActivity(new Intent(this, NewChatActivity.class)));
    }
}