package com.example.asiochatfrontend.data.common.utils;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class FileUtils {
    private static final String TAG = "FileUtils";
    private static final int BUFFER_SIZE = 8192;
    private static final String PROVIDER_AUTHORITY = "com.example.asiochatfrontend.fileprovider";

    private final Context context;

    public FileUtils(Context context) {
        this.context = context;
    }

    public static long getDurationOfAudio(String absolutePath) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(absolutePath);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return durationStr != null ? Long.parseLong(durationStr) : -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            try {
                retriever.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
            if (mimeType == null) {
                // Try to infer MIME type from uri path if not provided by content resolver
                String path = uri.getPath();
                if (path != null) {
                    String extension = MimeTypeMap.getFileExtensionFromUrl(path);
                    if (extension != null) {
                        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.US));
                    }
                }

                // If still null, use a safe default
                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }
            }

            // Get extension from mime type, defaulting to bin if unknown
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extension == null) extension = "bin";

            // Create a temporary file with proper extension
            File tempFile = createTempFile("media_", "." + extension);
            if (tempFile == null) {
                Log.e(TAG, "Failed to create temp file");
                return null;
            }

            // Copy the content from Uri to the temp file
            try (InputStream input = context.getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(tempFile)) {

                if (input == null) {
                    Log.e(TAG, "Failed to open input stream for URI: " + uri);
                    return null;
                }

                byte[] buffer = new byte[BUFFER_SIZE];
                int length;
                while ((length = input.read(buffer)) > 0) {
                    output.write(buffer, 0, length);
                }
                output.flush();

                Log.d(TAG, "Successfully copied file from URI to temp file: " + tempFile.getAbsolutePath());
            }

            return tempFile;

        } catch (Exception e) {
            Log.e(TAG, "Error getting file from URI: " + uri, e);
            return null;
        }
    }

    public static byte[] readFileToByteArray(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("File does not exist or is null");
        }

        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        int readBytes = fis.read(data);
        fis.close();

        if (readBytes != data.length) {
            throw new IOException("Could not completely read the file");
        }

        return data;
    }

    public File getFileFromPath(String path) {
        File file = new File(path);
        return file.exists() ? file : null;
    }

    public Uri getUriForFile(File file) {
        return FileProvider.getUriForFile(context, PROVIDER_AUTHORITY, file);
    }

    public static String getMimeType(File file) {
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

    public String getExtensionFromFileName(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    public File copyToAppStorage(InputStream input, String fileName) {
        try {
            File dir = new File(context.getFilesDir(), "media"); // app-private media folder
            if (!dir.exists()) dir.mkdirs();

            File outFile = new File(dir, fileName);
            try (FileOutputStream output = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int length;
                while ((length = input.read(buffer)) != -1) {
                    output.write(buffer, 0, length);
                }
                output.flush();
            }

            return outFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
