package com.example.asiochatfrontend.ui.chat.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import com.example.asiochatfrontend.R;

public class MediaPreviewDialog extends Dialog {
    private final String mediaPath;

    public MediaPreviewDialog(@NonNull Context context, String mediaPath) {
        super(context);
        this.mediaPath = mediaPath;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_media_preview);

        ImageView previewImage = findViewById(R.id.dialog_preview_image);
        previewImage.setImageBitmap(BitmapFactory.decodeFile(mediaPath));
    }
}
