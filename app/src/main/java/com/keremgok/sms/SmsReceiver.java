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
        long processingStartTime = System.currentTimeMillis(); // Start performance tracking
        logDebug("SMS received");
        
        // Get StatisticsManager instance for analytics
        StatisticsManager statsManager = StatisticsManager.getInstance(context);
        
        if (intent.getAction() == null || !intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            return;
        }
        
        // Record SMS received event
        statsManager.recordSmsReceived();
        
        // Get enabled target numbers from database
        AppDatabase database = AppDatabase.getInstance(context);
        TargetNumberDao targetNumberDao = database.targetNumberDao();
        
        java.util.List<TargetNumber> targetNumbers = targetNumberDao.getEnabledTargetNumbers();
        
        if (targetNumbers == null || targetNumbers.isEmpty()) {
            logDebug("No enabled target numbers configured, SMS forwarding disabled");
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
                logDebug("SMS from: " + maskPhoneNumber(senderNumber) + ", applying filters before forwarding to " + targetNumbers.size() + " targets");
                
                // Apply SMS filters before forwarding
                FilterEngine filterEngine = new FilterEngine(context);
                FilterEngine.FilterResult filterResult = filterEngine.applyFilters(senderNumber, finalMessage, timestamp);
                
                if (filterResult.shouldForward()) {
                    logDebug("SMS passed filters: " + filterResult.getReason() + " - forwarding to targets");
                    queueSmsForwardingToMultipleTargets(context, senderNumber, finalMessage, targetNumbers, timestamp);
                    
                    // Record performance metrics
                    long processingTime = System.currentTimeMillis() - processingStartTime;
                    statsManager.recordPerformanceMetric("sms_processing_time", processingTime, "ms");
                    statsManager.recordSmsForwardSuccess(processingTime);
                } else {
                    logDebug("SMS blocked by filter: " + filterResult.getReason() + " - not forwarding");
                    
                    // Record filter application
                    statsManager.recordFilterApplication(filterResult.getFilterType(), StatisticsManager.EventAction.FILTERED);
                    
                    // Log blocked SMS to history with filter reason
                    for (TargetNumber target : targetNumbers) {
                        logSmsHistory(context, senderNumber, finalMessage, target.getPhoneNumber(), 
                                     "", timestamp, false, "Blocked by filter: " + filterResult.getReason());
                    }
                }
            } else {
                Log.e(TAG, "Invalid SMS data - sender: " + maskPhoneNumber(senderNumber));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing SMS: " + e.getMessage(), e);
            
            // Record error in statistics
            statsManager.recordSmsForwardFailure(StatisticsManager.ErrorCode.UNKNOWN_ERROR, e.getMessage());
            statsManager.recordPerformanceMetric("sms_processing_error", 1, "count");
        }
    }
    
    /**
     * Queue SMS for forwarding to multiple targets with parallel/sequential mode support
     * This method handles SMS forwarding to multiple target numbers
     */
    private void queueSmsForwardingToMultipleTargets(Context context, String originalSender, String message, java.util.List<TargetNumber> targetNumbers, long timestamp) {
        try {
            // Get sending mode preference
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String sendingMode = prefs.getString("sending_mode", "sequential");
            
            if ("parallel".equals(sendingMode)) {
                // Parallel sending - queue all targets simultaneously
                queueParallelForwarding(context, originalSender, message, targetNumbers, timestamp);
            } else {
                // Sequential sending - queue targets one by one with delay
                queueSequentialForwarding(context, originalSender, message, targetNumbers, timestamp);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error queuing SMS for multiple targets: " + e.getMessage(), e);
            // Fallback to direct forwarding for all targets
            fallbackDirectForwardingToMultipleTargets(context, originalSender, message, targetNumbers, timestamp);
        }
    }
    
    /**
     * Queue SMS for parallel forwarding to all targets simultaneously
     */
    private void queueParallelForwarding(Context context, String originalSender, String message, java.util.List<TargetNumber> targetNumbers, long timestamp) {
        logDebug("Using parallel forwarding mode for " + targetNumbers.size() + " targets");
        
        for (TargetNumber target : targetNumbers) {
            queueSmsForwardingToSingleTarget(context, originalSender, message, target, timestamp);
        }
    }
    
    /**
     * Queue SMS for sequential forwarding to targets with delays
     */
    private void queueSequentialForwarding(Context context, String originalSender, String message, java.util.List<TargetNumber> targetNumbers, long timestamp) {
        logDebug("Using sequential forwarding mode for " + targetNumbers.size() + " targets");
        
        // Process primary target first, then others with increasing delays
        TargetNumber primaryTarget = null;
        java.util.List<TargetNumber> otherTargets = new java.util.ArrayList<>();
        
        for (TargetNumber target : targetNumbers) {
            if (target.isPrimary()) {
                primaryTarget = target;
            } else {
                otherTargets.add(target);
            }
        }
        
        // Send to primary target immediately
        if (primaryTarget != null) {
            queueSmsForwardingToSingleTarget(context, originalSender, message, primaryTarget, timestamp);
        }
        
        // Send to other targets with 2-second intervals
        for (int i = 0; i < otherTargets.size(); i++) {
            TargetNumber target = otherTargets.get(i);
            long delay = (i + 1) * 2000; // 2 seconds delay between each target
            queueSmsForwardingToSingleTargetWithDelay(context, originalSender, message, target, timestamp, delay);
        }
    }
    
    /**
     * Queue SMS for forwarding to a single target (original method adapted)
     */
    private void queueSmsForwardingToSingleTarget(Context context, String originalSender, String message, TargetNumber targetNumber, long timestamp) {
        try {
            String targetPhoneNumber = targetNumber.getPhoneNumber();
            
            // Get SMS queue manager instance
            SmsQueueManager queueManager = SmsQueueManager.getInstance(context);
            
            // Determine priority based on SMS characteristics
            int priority = determineSmsPriority(originalSender, message);
            
            // Queue the SMS for background processing
            java.util.UUID workId = null;
            switch (priority) {
                case SmsQueueWorker.PRIORITY_HIGH:
                    workId = queueManager.queueHighPrioritySms(originalSender, message, targetPhoneNumber, timestamp);
                    logDebug("SMS queued with HIGH priority for target: " + maskPhoneNumber(targetPhoneNumber));
                    break;
                case SmsQueueWorker.PRIORITY_NORMAL:
                    workId = queueManager.queueNormalPrioritySms(originalSender, message, targetPhoneNumber, timestamp);
                    logDebug("SMS queued with NORMAL priority for target: " + maskPhoneNumber(targetPhoneNumber));
                    break;
                case SmsQueueWorker.PRIORITY_LOW:
                    workId = queueManager.queueLowPrioritySms(originalSender, message, targetPhoneNumber, timestamp);
                    logDebug("SMS queued with LOW priority for target: " + maskPhoneNumber(targetPhoneNumber));
                    break;
            }
            
            if (workId != null) {
                logInfo("SMS successfully queued for forwarding to: " + maskPhoneNumber(targetPhoneNumber) + ". Work ID: " + workId);
                // Update last used timestamp
                updateTargetLastUsed(context, targetNumber.getId(), timestamp);
            } else {
                Log.e(TAG, "Failed to queue SMS for forwarding to: " + maskPhoneNumber(targetPhoneNumber));
                // Fallback to direct processing if queue fails
                fallbackDirectForwardingToSingleTarget(context, originalSender, message, targetNumber, timestamp);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error queuing SMS for forwarding to target: " + e.getMessage(), e);
            // Fallback to direct processing if queue fails
            fallbackDirectForwardingToSingleTarget(context, originalSender, message, targetNumber, timestamp);
        }
    }
    
    /**
     * Queue SMS for forwarding to a single target with delay
     */
    private void queueSmsForwardingToSingleTargetWithDelay(Context context, String originalSender, String message, TargetNumber targetNumber, long timestamp, long delay) {
        // For now, implement as immediate sending with fallback
        // In a full implementation, this would use WorkManager with scheduled delays
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            queueSmsForwardingToSingleTarget(context, originalSender, message, targetNumber, timestamp);
        }, delay);
    }
    
    /**
     * Update target number last used timestamp
     */
    private void updateTargetLastUsed(Context context, int targetId, long timestamp) {
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(context);
                database.targetNumberDao().updateLastUsedTimestamp(targetId, timestamp);
            } catch (Exception e) {
                Log.e(TAG, "Error updating target last used timestamp: " + e.getMessage());
            }
        });
    }
    
    /**
     * Fallback direct forwarding for multiple targets when queue system fails
     */
    private void fallbackDirectForwardingToMultipleTargets(Context context, String originalSender, String message, java.util.List<TargetNumber> targetNumbers, long timestamp) {
        logDebug("Using fallback direct forwarding for " + targetNumbers.size() + " targets");
        
        for (TargetNumber target : targetNumbers) {
            fallbackDirectForwardingToSingleTarget(context, originalSender, message, target, timestamp);
        }
    }
    
    /**
     * Fallback direct forwarding method for a single target when queue system fails
     */
    private void fallbackDirectForwardingToSingleTarget(Context context, String originalSender, String message, TargetNumber targetNumber, long timestamp) {
        try {
            String targetPhoneNumber = targetNumber.getPhoneNumber();
            logDebug("Using fallback direct forwarding for target: " + maskPhoneNumber(targetPhoneNumber));
            
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
                smsManager.sendMultipartTextMessage(targetPhoneNumber, null, parts, null, null);
                logDebug("Fallback multipart SMS sent to " + maskPhoneNumber(targetPhoneNumber) + " (" + parts.size() + " parts)");
            } else {
                // Send single SMS
                smsManager.sendTextMessage(targetPhoneNumber, null, forwardedMessage, null, null);
                logDebug("Fallback single SMS sent to " + maskPhoneNumber(targetPhoneNumber));
            }
            
            // Log success to database
            logSmsHistory(context, originalSender, message, targetPhoneNumber, forwardedMessage, timestamp, true, null);
            // Update last used timestamp
            updateTargetLastUsed(context, targetNumber.getId(), timestamp);
            logInfo("Fallback SMS forwarding completed successfully for target: " + maskPhoneNumber(targetPhoneNumber));
            
        } catch (Exception e) {
            String targetPhoneNumber = targetNumber.getPhoneNumber();
            Log.e(TAG, "Fallback SMS forwarding failed for target " + maskPhoneNumber(targetPhoneNumber) + ": " + e.getMessage(), e);
            // Log failure to database
            logSmsHistory(context, originalSender, message, targetPhoneNumber, "", timestamp, false, "Fallback forwarding failed: " + e.getMessage());
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