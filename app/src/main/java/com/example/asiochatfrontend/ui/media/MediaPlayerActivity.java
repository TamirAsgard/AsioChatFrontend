package com.example.asiochatfrontend.ui.media;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.asiochatfrontend.R;

public class MediaPlayerActivity extends AppCompatActivity {
    private MediaPlayer mediaPlayer;
    private TextView audioTitle;
    private ImageView playButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);

        playButton = findViewById(R.id.player_play_button);
        audioTitle = findViewById(R.id.player_audio_title);

        String audioPath = getIntent().getStringExtra("AUDIO_PATH");
        mediaPlayer = MediaPlayer.create(this, android.net.Uri.parse(audioPath));

        playButton.setOnClickListener(v -> {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.start();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
