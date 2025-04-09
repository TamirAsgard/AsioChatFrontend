package com.example.asiochatfrontend.ui.media;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import com.example.asiochatfrontend.R;

public class AudioRecorderView extends LinearLayout {
    public AudioRecorderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public AudioRecorderView(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_audio_recorder, this, true);
        // Additional recorder setup here
    }
}
