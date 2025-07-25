package com.keremgok.sms;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {
    
    private static final String TAG = "HermesSmsReceiver";
    private static final String PREFS_NAME = "HermesPrefs";
    private static final String KEY_TARGET_NUMBER = "target_number";
    // Debug flag - set to false for production builds
    private static final boolean DEBUG = true;
    
    // SMS Retry Configuration
    private static final int MAX_RETRY_COUNT = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 2000; // 2 seconds
    private static final String ACTION_SMS_SENT = "SMS_SENT";
    private static final String ACTION_SMS_DELIVERED = "SMS_DELIVERED";
    
    /**
     * Masks phone number for secure logging
     * Example: +905551234567 -> +9055***4567
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length() < 8) {
            return "***";
        }
        
        String prefix = phoneNumber.substring(0, Math.min(5, phoneNumber.length() - 4));
        String suffix = phoneNumber.substring(phoneNumber.length() - 4);
        return prefix + "***" + suffix;
    }
    
    /**
     * Secure debug logging - only logs in debug builds
     */
    private void logDebug(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }
    
    /**
     * Secure info logging - only logs in debug builds  
     */
    private void logInfo(String message) {
        if (DEBUG) {
            Log.i(TAG, message);
        }
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        logDebug("SMS received");
        
        if (intent.getAction() == null || !intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            return;
        }
        
        // Get target number from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String targetNumber = prefs.getString(KEY_TARGET_NUMBER, "");
        
        if (TextUtils.isEmpty(targetNumber)) {
            logDebug("No target number configured, SMS forwarding disabled");
            return;
        }
        
        // Extract SMS messages from the intent
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Log.e(TAG, "No bundle data in SMS intent");
            return;
        }
        
        try {
            Object[] pdus = (Object[]) bundle.get("pdus");
            String format = bundle.getString("format");
            
            if (pdus == null || pdus.length == 0) {
                Log.e(TAG, "No PDU data found in SMS intent");
                return;
            }
            
            StringBuilder messageBody = new StringBuilder();
            String senderNumber = "";
            long timestamp = 0;
            
            // Parse all SMS parts
            for (Object pdu : pdus) {
                SmsMessage smsMessage;
                if (format != null) {
                    smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                } else {
                    smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                }
                
                if (smsMessage != null) {
                    if (TextUtils.isEmpty(senderNumber)) {
                        senderNumber = smsMessage.getOriginatingAddress();
                        timestamp = smsMessage.getTimestampMillis();
                    }
                    messageBody.append(smsMessage.getMessageBody());
                }
            }
            
            String finalMessage = messageBody.toString();
            
            if (!TextUtils.isEmpty(finalMessage) && !TextUtils.isEmpty(senderNumber)) {
                logDebug("SMS from: " + maskPhoneNumber(senderNumber) + ", forwarding to: " + maskPhoneNumber(targetNumber));
                forwardSms(context, senderNumber, finalMessage, targetNumber, timestamp);
            } else {
                Log.e(TAG, "Invalid SMS data - sender: " + maskPhoneNumber(senderNumber));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing SMS: " + e.getMessage(), e);
        }
    }
    
    private void forwardSms(Context context, String originalSender, String message, String targetNumber, long timestamp) {
        try {
            // Format the forwarded message with original sender info
            String forwardedMessage = String.format(
                "[Hermes SMS Forward]\nGönderen: %s\nMesaj: %s\nZaman: %s",
                originalSender,
                message,
                new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
                    .format(new java.util.Date(timestamp))
            );
            
            // Check if message is too long and split if necessary
            if (forwardedMessage.length() > 160) {
                sendLongSms(context, originalSender, message, forwardedMessage, targetNumber, timestamp);
            } else {
                sendSingleSms(context, originalSender, message, forwardedMessage, targetNumber, timestamp);
            }
            
            logInfo("SMS successfully forwarded to: " + maskPhoneNumber(targetNumber));
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to forward SMS: " + e.getMessage(), e);
            // Log the failure to database
            logSmsHistory(context, originalSender, message, targetNumber, "", timestamp, false, e.getMessage());
        }
    }
    
    private void sendSingleSms(Context context, String originalSender, String originalMessage, String forwardedMessage, String targetNumber, long timestamp) {
        sendSingleSmsWithRetry(context, originalSender, originalMessage, forwardedMessage, targetNumber, timestamp, 0);
    }
    
    private void sendSingleSmsWithRetry(Context context, String originalSender, String originalMessage, String forwardedMessage, String targetNumber, long timestamp, int retryCount) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            
            // Create PendingIntents for delivery confirmation
            PendingIntent sentPI = createSentPendingIntent(context, originalSender, originalMessage, forwardedMessage, targetNumber, timestamp, retryCount, false);
            
            smsManager.sendTextMessage(targetNumber, null, forwardedMessage, sentPI, null);
            logDebug("Single SMS send attempt " + (retryCount + 1) + "/" + MAX_RETRY_COUNT);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to send single SMS (attempt " + (retryCount + 1) + "): " + e.getMessage(), e);
            handleSmsFailure(context, originalSender, originalMessage, forwardedMessage, targetNumber, timestamp, retryCount, false, e);
        }
    }
    
    private void sendLongSms(Context context, String originalSender, String originalMessage, String forwardedMessage, String targetNumber, long timestamp) {
        sendLongSmsWithRetry(context, originalSender, originalMessage, forwardedMessage, targetNumber, timestamp, 0);
    }
    
    private void sendLongSmsWithRetry(Context context, String originalSender, String originalMessage, String forwardedMessage, String targetNumber, long timestamp, int retryCount) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            java.util.ArrayList<String> parts = smsManager.divideMessage(forwardedMessage);
            
            // Create PendingIntents for delivery confirmation
            PendingIntent sentPI = createSentPendingIntent(context, originalSender, originalMessage, forwardedMessage, targetNumber, timestamp, retryCount, true);
            
            // Create ArrayList of PendingIntents for each part
            java.util.ArrayList<PendingIntent> sentPIs = new java.util.ArrayList<>();
            for (int i = 0; i < parts.size(); i++) {
                sentPIs.add(sentPI);
            }
            
            smsManager.sendMultipartTextMessage(targetNumber, null, parts, sentPIs, null);
            logDebug("Multipart SMS send attempt " + (retryCount + 1) + "/" + MAX_RETRY_COUNT + 
                    " (" + parts.size() + " parts)");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to send multipart SMS (attempt " + (retryCount + 1) + "): " + e.getMessage(), e);
            handleSmsFailure(context, originalSender, originalMessage, forwardedMessage, targetNumber, timestamp, retryCount, true, e);
        }
    }
    
    private PendingIntent createSentPendingIntent(Context context, String originalSender, String originalMessage, String forwardedMessage, String targetNumber, long timestamp, int retryCount, boolean isMultipart) {
        Intent intent = new Intent(ACTION_SMS_SENT);
        intent.putExtra("originalSender", originalSender);
        intent.putExtra("originalMessage", originalMessage);
        intent.putExtra("forwardedMessage", forwardedMessage);
        intent.putExtra("targetNumber", targetNumber);
        intent.putExtra("timestamp", timestamp);
        intent.putExtra("retryCount", retryCount);
        intent.putExtra("isMultipart", isMultipart);
        
        return PendingIntent.getBroadcast(context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    
    private void handleSmsFailure(Context context, String originalSender, String originalMessage, String forwardedMessage, String targetNumber, long timestamp, int retryCount, boolean isMultipart, Exception exception) {
        if (retryCount < MAX_RETRY_COUNT - 1) {
            // Schedule retry with exponential backoff
            long delay = INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, retryCount);
            scheduleRetry(context, originalSender, originalMessage, forwardedMessage, targetNumber, timestamp, retryCount + 1, isMultipart, delay);
            logDebug("SMS failed, scheduling retry " + (retryCount + 2) + "/" + MAX_RETRY_COUNT + 
                    " in " + delay + "ms");
        } else {
            // Max retries reached, log final failure
            Log.e(TAG, "SMS sending failed permanently after " + MAX_RETRY_COUNT + 
                    " attempts to " + maskPhoneNumber(targetNumber));
            logDebug("Final SMS failure: " + exception.getMessage());
            // Log the permanent failure to database
            logSmsHistory(context, originalSender, originalMessage, targetNumber, forwardedMessage, timestamp, false, "Max retries exceeded: " + exception.getMessage());
        }
    }
    
    private void scheduleRetry(Context context, String originalSender, String originalMessage, String forwardedMessage, String targetNumber, long timestamp, int retryCount, boolean isMultipart, long delay) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isMultipart) {
                    sendLongSmsWithRetry(context, originalSender, originalMessage, forwardedMessage, targetNumber, timestamp, retryCount);
                } else {
                    sendSingleSmsWithRetry(context, originalSender, originalMessage, forwardedMessage, targetNumber, timestamp, retryCount);
                }
            }
        }, delay);
    }
    
    /**
     * Log SMS forwarding history to database
     * Runs in background thread to avoid blocking main thread
     */
    private void logSmsHistory(Context context, String senderNumber, String originalMessage, String targetNumber, String forwardedMessage, long timestamp, boolean success, String errorMessage) {
        new Thread(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(context);
                SmsHistory history = new SmsHistory(
                    senderNumber,
                    originalMessage,
                    targetNumber,
                    forwardedMessage,
                    timestamp,
                    success,
                    errorMessage
                );
                database.smsHistoryDao().insert(history);
                
                if (DEBUG) {
                    String status = success ? "SUCCESS" : "FAILED";
                    logDebug("SMS history logged: " + status + " from " + maskPhoneNumber(senderNumber) + 
                            " to " + maskPhoneNumber(targetNumber));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to log SMS history: " + e.getMessage(), e);
            }
        }).start();
    }
}