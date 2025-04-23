package com.example.asiochatfrontend.core.security;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.asiochatfrontend.app.di.DatabaseModule;
import com.example.asiochatfrontend.app.di.ServiceModule;
import com.example.asiochatfrontend.data.database.dao.EncryptionKeyDao;
import com.example.asiochatfrontend.data.relay.service.RelayAuthService;

import java.util.concurrent.TimeUnit;

/**
 * Background job to handle key rotation
 */
public class KeyRotationJob extends JobService {
    private static final String TAG = "KeyRotationJob";
    private static final int JOB_ID = 1000;
    private static final long ONE_DAY_MILLIS = TimeUnit.DAYS.toMillis(1);
    private static final long RETENTION_PERIOD_MILLIS = TimeUnit.DAYS.toMillis(30); // Keep keys for 30 days

    private RelayAuthService authService;
    private EncryptionKeyDao encryptionKeyDao;
    private String currentUserId;

    /**
     * Schedule the key rotation job
     */
    public static void schedule(Context context, String userId) {
        ComponentName componentName = new ComponentName(context, KeyRotationJob.class);
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, componentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(ONE_DAY_MILLIS) // Check once per day
                .build();

        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        int resultCode = jobScheduler.schedule(jobInfo);

        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d(TAG, "Key rotation job scheduled successfully");
        } else {
            Log.e(TAG, "Failed to schedule key rotation job");
        }
    }

    /**
     * Cancel the key rotation job
     */
    public static void cancel(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(JOB_ID);
        Log.d(TAG, "Key rotation job canceled");
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Key rotation job started");

        SharedPreferences prefs = getApplicationContext().getSharedPreferences("AsioChat_Prefs", MODE_PRIVATE);
        currentUserId = prefs.getString("user_id", null);

        // Initialize services
        encryptionKeyDao = DatabaseModule.getInstance().encryptionKeyDao();
        authService = ServiceModule.getConnectionManager().relayAuthService;

        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "Current user ID is not set, aborting key rotation");
            return false;
        }

        // Run key rotation on a background thread
        new Thread(() -> {
            try {
                rotatePublicKeyIfNeeded();

                // Tell the system the job is finished
                jobFinished(params, false);
            } catch (Exception e) {
                Log.e(TAG, "Error during key rotation", e);
                // Schedule a retry
                jobFinished(params, true);
            }
        }).start();

        // Return true to indicate the job is still running in a background thread
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        // Return true to reschedule the job
        return true;
    }

    /**
     * Check if keys need to be rotated and create new ones if needed
     */
    private void rotatePublicKeyIfNeeded() {
        try {
            // This will check if a new key pair is needed and create one if necessary
            // then register it with the backend and store it in the database
            boolean registered = authService.registerPublicKey();
            if (registered) {
                Log.d(TAG, "Successfully registered new public key or using existing one");
            } else {
                Log.e(TAG, "Failed to register new public key");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while rotating public key", e);
        }
    }
}