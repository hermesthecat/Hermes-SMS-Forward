package com.keremgok.sms;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Background worker for automatic database cleanup
 * Runs periodically to delete old SMS history and analytics events
 * Retention period is configurable via Settings
 */
public class CleanupWorker extends Worker {

    private static final String TAG = "CleanupWorker";

    // Default retention periods (in days)
    private static final int DEFAULT_SMS_RETENTION_DAYS = 30;
    private static final int DEFAULT_ANALYTICS_RETENTION_DAYS = 90;

    // SharedPreferences keys
    private static final String PREF_SMS_RETENTION_DAYS = "pref_sms_retention_days";
    private static final String PREF_ANALYTICS_RETENTION_DAYS = "pref_analytics_retention_days";

    public CleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            android.util.Log.i(TAG, "Starting periodic cleanup task");

            Context context = getApplicationContext();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            // Get retention periods from settings
            String smsRetentionStr = prefs.getString(PREF_SMS_RETENTION_DAYS, String.valueOf(DEFAULT_SMS_RETENTION_DAYS));
            String analyticsRetentionStr = prefs.getString(PREF_ANALYTICS_RETENTION_DAYS, String.valueOf(DEFAULT_ANALYTICS_RETENTION_DAYS));

            int smsRetentionDays = DEFAULT_SMS_RETENTION_DAYS;
            int analyticsRetentionDays = DEFAULT_ANALYTICS_RETENTION_DAYS;

            try {
                smsRetentionDays = Integer.parseInt(smsRetentionStr);
            } catch (NumberFormatException e) {
                android.util.Log.w(TAG, "Invalid SMS retention value, using default: " + DEFAULT_SMS_RETENTION_DAYS);
            }

            try {
                analyticsRetentionDays = Integer.parseInt(analyticsRetentionStr);
            } catch (NumberFormatException e) {
                android.util.Log.w(TAG, "Invalid analytics retention value, using default: " + DEFAULT_ANALYTICS_RETENTION_DAYS);
            }

            // Perform cleanup
            AppDatabase db = AppDatabase.getInstance(context);

            // Calculate timestamps
            long smsRetentionTimestamp = System.currentTimeMillis() - (smsRetentionDays * 24L * 60 * 60 * 1000);
            long analyticsRetentionTimestamp = System.currentTimeMillis() - (analyticsRetentionDays * 24L * 60 * 60 * 1000);

            // Clean up SMS history
            int deletedSmsCount = db.smsHistoryDao().deleteOldRecords(smsRetentionTimestamp);

            // Clean up analytics events
            int deletedAnalyticsCount = db.analyticsEventDao().deleteOldEvents(analyticsRetentionTimestamp);

            // Log results
            if (deletedSmsCount > 0) {
                android.util.Log.i(TAG, "Deleted " + deletedSmsCount + " old SMS history records (>" + smsRetentionDays + " days)");
            }
            if (deletedAnalyticsCount > 0) {
                android.util.Log.i(TAG, "Deleted " + deletedAnalyticsCount + " old analytics events (>" + analyticsRetentionDays + " days)");
            }

            android.util.Log.i(TAG, "Periodic cleanup task completed successfully");
            return Result.success();

        } catch (Exception e) {
            android.util.Log.e(TAG, "Cleanup task failed: " + e.getMessage(), e);
            return Result.retry(); // Retry on failure
        }
    }
}
