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
                logDebug("SMS from: " + maskPhoneNumber(senderNumber) + ", queuing for forwarding to: " + maskPhoneNumber(targetNumber));
                queueSmsForwarding(context, senderNumber, finalMessage, targetNumber, timestamp);
            } else {
                Log.e(TAG, "Invalid SMS data - sender: " + maskPhoneNumber(senderNumber));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing SMS: " + e.getMessage(), e);
        }
    }
    
    /**
     * Queue SMS for optimized background forwarding using WorkManager
     * This replaces the old direct SMS sending approach for better performance
     */
    private void queueSmsForwarding(Context context, String originalSender, String message, String targetNumber, long timestamp) {
        try {
            // Get SMS queue manager instance
            SmsQueueManager queueManager = SmsQueueManager.getInstance(context);
            
            // Determine priority based on SMS characteristics
            int priority = determineSmsPriority(originalSender, message);
            
            // Queue the SMS for background processing
            java.util.UUID workId = null;
            switch (priority) {
                case SmsQueueWorker.PRIORITY_HIGH:
                    workId = queueManager.queueHighPrioritySms(originalSender, message, targetNumber, timestamp);
                    logDebug("SMS queued with HIGH priority");
                    break;
                case SmsQueueWorker.PRIORITY_NORMAL:
                    workId = queueManager.queueNormalPrioritySms(originalSender, message, targetNumber, timestamp);
                    logDebug("SMS queued with NORMAL priority");
                    break;
                case SmsQueueWorker.PRIORITY_LOW:
                    workId = queueManager.queueLowPrioritySms(originalSender, message, targetNumber, timestamp);
                    logDebug("SMS queued with LOW priority");
                    break;
            }
            
            if (workId != null) {
                logInfo("SMS successfully queued for forwarding. Work ID: " + workId);
            } else {
                Log.e(TAG, "Failed to queue SMS for forwarding");
                // Fallback to direct processing if queue fails
                fallbackDirectForwarding(context, originalSender, message, targetNumber, timestamp);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error queuing SMS for forwarding: " + e.getMessage(), e);
            // Fallback to direct processing if queue fails
            fallbackDirectForwarding(context, originalSender, message, targetNumber, timestamp);
        }
    }
    
    /**
     * Determine SMS priority based on sender and content analysis
     */
    private int determineSmsPriority(String originalSender, String message) {
        // High priority for emergency-like keywords
        String messageLower = message.toLowerCase();
        if (messageLower.contains("acil") || messageLower.contains("emergency") || 
            messageLower.contains("urgent") || messageLower.contains("önemli")) {
            return SmsQueueWorker.PRIORITY_HIGH;
        }
        
        // High priority for specific sender patterns (banks, government, etc.)
        if (originalSender != null) {
            String senderLower = originalSender.toLowerCase();
            if (senderLower.contains("bank") || senderLower.matches("\\d{4}") || // 4-digit short codes
                senderLower.contains("gov") || senderLower.contains("official")) {
                return SmsQueueWorker.PRIORITY_HIGH;
            }
        }
        
        // Low priority for promotional/marketing messages
        if (messageLower.contains("kampanya") || messageLower.contains("indirim") || 
            messageLower.contains("reklam") || messageLower.contains("promotion")) {
            return SmsQueueWorker.PRIORITY_LOW;
        }
        
        // Default to normal priority
        return SmsQueueWorker.PRIORITY_NORMAL;
    }
    
    /**
     * Fallback direct forwarding method for when queue system fails
     * Uses simplified approach for reliability
     */
    private void fallbackDirectForwarding(Context context, String originalSender, String message, String targetNumber, long timestamp) {
        try {
            logDebug("Using fallback direct forwarding");
            
            // Format the forwarded message with original sender info
            String forwardedMessage = String.format(
                "[Hermes SMS Forward]\nGönderen: %s\nMesaj: %s\nZaman: %s",
                originalSender,
                message,
                new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
                    .format(new java.util.Date(timestamp))
            );
            
            // Send SMS directly using SmsManager
            SmsManager smsManager = SmsManager.getDefault();
            
            if (forwardedMessage.length() > 160) {
                // Send multipart SMS
                java.util.ArrayList<String> parts = smsManager.divideMessage(forwardedMessage);
                smsManager.sendMultipartTextMessage(targetNumber, null, parts, null, null);
                logDebug("Fallback multipart SMS sent (" + parts.size() + " parts)");
            } else {
                // Send single SMS
                smsManager.sendTextMessage(targetNumber, null, forwardedMessage, null, null);
                logDebug("Fallback single SMS sent");
            }
            
            // Log success to database
            logSmsHistory(context, originalSender, message, targetNumber, forwardedMessage, timestamp, true, null);
            logInfo("Fallback SMS forwarding completed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Fallback SMS forwarding failed: " + e.getMessage(), e);
            // Log failure to database
            logSmsHistory(context, originalSender, message, targetNumber, "", timestamp, false, "Fallback forwarding failed: " + e.getMessage());
        }
    }
    
    /**
     * Log SMS forwarding history to database
     * Runs in optimized background thread to avoid blocking main thread
     */
    private void logSmsHistory(Context context, String senderNumber, String originalMessage, String targetNumber, String forwardedMessage, long timestamp, boolean success, String errorMessage) {
        ThreadManager.getInstance().executeDatabase(() -> {
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
        });
    }
}