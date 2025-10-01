package com.keremgok.sms;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

/**
 * NotificationHelper - Manages all app notifications
 * Handles SMS forwarding notifications with user-configurable sound and vibration
 */
public class NotificationHelper {

    private static final String TAG = "NotificationHelper";

    // Notification Channels
    private static final String CHANNEL_SMS_SUCCESS = "sms_success_channel";
    private static final String CHANNEL_SMS_ERROR = "sms_error_channel";
    private static final String CHANNEL_MISSED_CALL = "missed_call_channel";

    // Notification IDs
    private static final int NOTIFICATION_ID_SMS_SUCCESS = 1001;
    private static final int NOTIFICATION_ID_SMS_ERROR = 1002;
    private static final int NOTIFICATION_ID_MISSED_CALL = 1003;

    private Context context;
    private NotificationManager notificationManager;
    private SharedPreferences prefs;

    /**
     * Constructor
     */
    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);

        // Create notification channels for Android O+
        createNotificationChannels();
    }

    /**
     * Create notification channels for Android 8.0+
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // SMS Success Channel
            NotificationChannel successChannel = new NotificationChannel(
                CHANNEL_SMS_SUCCESS,
                context.getString(R.string.notification_channel_sms_success),
                NotificationManager.IMPORTANCE_LOW
            );
            successChannel.setDescription(context.getString(R.string.notification_channel_sms_success_desc));
            successChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(successChannel);

            // SMS Error Channel
            NotificationChannel errorChannel = new NotificationChannel(
                CHANNEL_SMS_ERROR,
                context.getString(R.string.notification_channel_sms_error),
                NotificationManager.IMPORTANCE_HIGH
            );
            errorChannel.setDescription(context.getString(R.string.notification_channel_sms_error_desc));
            errorChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(errorChannel);

            // Missed Call Channel
            NotificationChannel missedCallChannel = new NotificationChannel(
                CHANNEL_MISSED_CALL,
                context.getString(R.string.notification_channel_missed_call),
                NotificationManager.IMPORTANCE_DEFAULT
            );
            missedCallChannel.setDescription(context.getString(R.string.notification_channel_missed_call_desc));
            missedCallChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(missedCallChannel);
        }
    }

    /**
     * Show SMS forwarding success notification
     * @param targetNumber Target phone number
     * @param senderNumber Original sender number
     */
    public void showSmsSuccessNotification(String targetNumber, String senderNumber) {
        // Check if notifications are enabled
        if (!isNotificationEnabled()) {
            return;
        }

        // Mask phone numbers for privacy
        String maskedTarget = maskPhoneNumber(targetNumber);
        String maskedSender = maskPhoneNumber(senderNumber);

        // Create intent for notification tap
        Intent intent = new Intent(context, HistoryActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_SMS_SUCCESS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_sms_forwarded_title))
            .setContentText(context.getString(R.string.notification_sms_forwarded_body, maskedSender, maskedTarget))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        // Apply sound settings
        if (isNotificationSoundEnabled()) {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(soundUri);
        }

        // Apply vibration settings
        if (isNotificationVibrationEnabled()) {
            builder.setVibrate(new long[]{0, 300, 200, 300});
        }

        // Show notification
        notificationManager.notify(NOTIFICATION_ID_SMS_SUCCESS, builder.build());
    }

    /**
     * Show SMS forwarding error notification
     * @param targetNumber Target phone number
     * @param errorMessage Error description
     */
    public void showSmsErrorNotification(String targetNumber, String errorMessage) {
        // Check if notifications are enabled
        if (!isNotificationEnabled()) {
            return;
        }

        // Mask phone number for privacy
        String maskedTarget = maskPhoneNumber(targetNumber);

        // Create intent for notification tap
        Intent intent = new Intent(context, HistoryActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_SMS_ERROR)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_sms_error_title))
            .setContentText(context.getString(R.string.notification_sms_error_body, maskedTarget))
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(context.getString(R.string.notification_sms_error_body, maskedTarget) + "\n" + errorMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        // Apply sound settings (always play sound for errors if notifications enabled)
        if (isNotificationSoundEnabled()) {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(soundUri);
        }

        // Apply vibration settings (always vibrate for errors if enabled)
        if (isNotificationVibrationEnabled()) {
            builder.setVibrate(new long[]{0, 500, 200, 500, 200, 500});
        }

        // Show notification
        notificationManager.notify(NOTIFICATION_ID_SMS_ERROR, builder.build());
    }

    /**
     * Show missed call notification
     * @param callerNumber Caller phone number
     */
    public void showMissedCallNotification(String callerNumber) {
        // Check if missed call notifications are enabled
        if (!isMissedCallNotificationEnabled()) {
            return;
        }

        // Mask phone number for privacy
        String maskedCaller = maskPhoneNumber(callerNumber);

        // Create intent for notification tap
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_MISSED_CALL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_missed_call_title))
            .setContentText(context.getString(R.string.notification_missed_call_body, maskedCaller))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        // Apply sound settings
        if (isNotificationSoundEnabled()) {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(soundUri);
        }

        // Apply vibration settings
        if (isNotificationVibrationEnabled()) {
            builder.setVibrate(new long[]{0, 400, 200, 400});
        }

        // Show notification
        notificationManager.notify(NOTIFICATION_ID_MISSED_CALL, builder.build());
    }

    /**
     * Cancel all notifications
     */
    public void cancelAllNotifications() {
        notificationManager.cancelAll();
    }

    /**
     * Check if notifications are enabled in settings
     */
    private boolean isNotificationEnabled() {
        return prefs.getBoolean("pref_show_notifications", true);
    }

    /**
     * Check if notification sound is enabled in settings
     */
    private boolean isNotificationSoundEnabled() {
        return prefs.getBoolean("pref_notification_sound", false);
    }

    /**
     * Check if notification vibration is enabled in settings
     */
    private boolean isNotificationVibrationEnabled() {
        return prefs.getBoolean("pref_notification_vibration", false);
    }

    /**
     * Check if missed call notifications are enabled in settings
     */
    private boolean isMissedCallNotificationEnabled() {
        return prefs.getBoolean("missed_call_notifications_enabled", false);
    }

    /**
     * Mask phone number for privacy (show only last 4 digits)
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() <= 4) {
            return "****";
        }
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
