package com.example.asiochatfrontend.data.common.utils;

import android.content.Context;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileUtils {

    private static final int BUFFER_SIZE = 8192;
    private static final String PROVIDER_AUTHORITY = "com.example.asiochatfrontend.fileprovider";

    private final Context context;

    public FileUtils(Context context) {
        this.context = context;
    }

    public File createTempFile(String prefix, String extension) {
        File storageDir = context.getCacheDir();
        try {
            return File.createTempFile(prefix, extension, storageDir);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void copyUriToFile(Uri uri, File destination) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(destination)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream != null ? inputStream.read(buffer) : -1) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File getFileFromUri(Uri uri) {
        try {
            String mimeType = context.getContentResolver().getType(uri);
            if (mimeType == null) return null;

            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extension == null) extension = "";

            File tempFile = createTempFile("media_", "." + extension);
            if (tempFile == null) return null;

            try (InputStream input = context.getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(tempFile)) {

                if (input != null) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int length;
                    while ((length = input.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }
                    output.flush();
                }
            }

            return tempFile;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] readFileToByteArray(File file) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(Uri.fromFile(file))) {
            if (inputStream == null) return null;

            byte[] buffer = new byte[(int) file.length()];
            int bytesRead = inputStream.read(buffer);
            if (bytesRead != buffer.length) {
                throw new RuntimeException("Could not completely read file into byte array.");
            }
            return buffer;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public File getFileFromPath(String path) {
        File file = new File(path);
        return file.exists() ? file : null;
    }

    public Uri getUriForFile(File file) {
        return FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, file);
    }

    public String getMimeType(File file) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
        if (extension == null || extension.isEmpty()) {
            extension = file.getName().substring(file.getName().lastIndexOf('.') + 1);
        }
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    public boolean deleteFile(File file) {
        return file.exists() && file.delete();
    }

    public void clearTempFiles() {
        File tempDir = context.getCacheDir();
        File[] files = tempDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith("media_")) {
                    file.delete();
                }
            }
        }
    }

    public boolean isLocalPath(String path) {
        if (path == null) return false;
        return path.startsWith(context.getFilesDir().getAbsolutePath()) ||
                path.startsWith(context.getCacheDir().getAbsolutePath()) ||
                (context.getExternalFilesDir(null) != null &&
                        path.startsWith(context.getExternalFilesDir(null).getAbsolutePath()));
    }

    public File saveMediaToFile(byte[] data, String fileName, String subDir) {
        try {
            File dir = new File(context.getFilesDir(), subDir);
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, fileName);
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(data);
                out.flush();
            }

            return file;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
