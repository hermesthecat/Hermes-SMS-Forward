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
        
        // Extract SIM information from bundle (dual SIM support)
        int sourceSubscriptionId = -1;
        int sourceSimSlot = -1;
        
        try {
            // Extract subscription ID and slot index from bundle
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                sourceSubscriptionId = bundle.getInt("subscription", -1);
                sourceSimSlot = bundle.getInt("slot", -1);
                
                // Alternative keys for different Android versions/manufacturers
                if (sourceSubscriptionId == -1) {
                    sourceSubscriptionId = bundle.getInt("android.telephony.extra.SUBSCRIPTION_INDEX", -1);
                }
                if (sourceSimSlot == -1) {
                    sourceSimSlot = bundle.getInt("android.telephony.extra.SLOT_INDEX", -1);
                }
                
                logDebug("SMS SIM info - Subscription ID: " + sourceSubscriptionId + ", Slot: " + sourceSimSlot);
                
                // Validate and get SIM information using SimManager
                if (sourceSubscriptionId != -1) {
                    SimManager.SimInfo simInfo = SimManager.getSimInfo(context, sourceSubscriptionId);
                    if (simInfo != null) {
                        sourceSimSlot = simInfo.slotIndex; // Ensure consistency
                        logDebug("SMS received from " + simInfo.displayName + " (Slot " + sourceSimSlot + ")");
                    } else {
                        logDebug("Warning: Could not retrieve SIM info for subscription " + sourceSubscriptionId);
                    }
                } else {
                    logDebug("No subscription ID found in SMS bundle - single SIM device or older Android version");
                }
            } else {
                logDebug("Dual SIM APIs not available (Android < 5.1) - using single SIM mode");
            }
        } catch (Exception simExtractionError) {
            Log.w(TAG, "Error extracting SIM information: " + simExtractionError.getMessage());
            // Continue processing SMS even if SIM extraction fails
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
                
                // Apply SMS filters before forwarding (with SIM information for dual SIM support)
                FilterEngine filterEngine = new FilterEngine(context);
                FilterEngine.FilterResult filterResult = filterEngine.applyFilters(senderNumber, finalMessage, timestamp, sourceSubscriptionId, sourceSimSlot);
                
                if (filterResult.shouldForward()) {
                    logDebug("SMS passed filters: " + filterResult.getReason() + " - forwarding to targets");
                    queueSmsForwardingToMultipleTargets(context, senderNumber, finalMessage, targetNumbers, timestamp, sourceSubscriptionId, sourceSimSlot);
                    
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
                                     "", timestamp, false, "Blocked by filter: " + filterResult.getReason(),
                                     sourceSimSlot, -1, sourceSubscriptionId, -1);
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
     * This method handles SMS forwarding to multiple target numbers with dual SIM support
     */
    private void queueSmsForwardingToMultipleTargets(Context context, String originalSender, String message, java.util.List<TargetNumber> targetNumbers, long timestamp, int sourceSubscriptionId, int sourceSimSlot) {
        try {
            // Get sending mode preference
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String sendingMode = prefs.getString("sending_mode", "sequential");
            
            if ("parallel".equals(sendingMode)) {
                // Parallel sending - queue all targets simultaneously
                queueParallelForwarding(context, originalSender, message, targetNumbers, timestamp, sourceSubscriptionId, sourceSimSlot);
            } else {
                // Sequential sending - queue targets one by one with delay
                queueSequentialForwarding(context, originalSender, message, targetNumbers, timestamp, sourceSubscriptionId, sourceSimSlot);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error queuing SMS for multiple targets: " + e.getMessage(), e);
            // Fallback to direct forwarding for all targets
            fallbackDirectForwardingToMultipleTargets(context, originalSender, message, targetNumbers, timestamp, sourceSubscriptionId, sourceSimSlot);
        }
    }
    
    /**
     * Queue SMS for parallel forwarding to all targets simultaneously
     */
    private void queueParallelForwarding(Context context, String originalSender, String message, java.util.List<TargetNumber> targetNumbers, long timestamp, int sourceSubscriptionId, int sourceSimSlot) {
        logDebug("Using parallel forwarding mode for " + targetNumbers.size() + " targets");
        
        for (TargetNumber target : targetNumbers) {
            queueSmsForwardingToSingleTarget(context, originalSender, message, target, timestamp, sourceSubscriptionId, sourceSimSlot);
        }
    }
    
    /**
     * Queue SMS for sequential forwarding to targets with delays
     */
    private void queueSequentialForwarding(Context context, String originalSender, String message, java.util.List<TargetNumber> targetNumbers, long timestamp, int sourceSubscriptionId, int sourceSimSlot) {
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
            queueSmsForwardingToSingleTarget(context, originalSender, message, primaryTarget, timestamp, sourceSubscriptionId, sourceSimSlot);
        }
        
        // Send to other targets with 2-second intervals
        for (int i = 0; i < otherTargets.size(); i++) {
            TargetNumber target = otherTargets.get(i);
            long delay = (i + 1) * 2000; // 2 seconds delay between each target
            queueSmsForwardingToSingleTargetWithDelay(context, originalSender, message, target, timestamp, delay, sourceSubscriptionId, sourceSimSlot);
        }
    }
    
    /**
     * Queue SMS for forwarding to a single target (original method adapted)
     */
    private void queueSmsForwardingToSingleTarget(Context context, String originalSender, String message, TargetNumber targetNumber, long timestamp, int sourceSubscriptionId, int sourceSimSlot) {
        try {
            String targetPhoneNumber = targetNumber.getPhoneNumber();
            
            // Get SMS queue manager instance
            SmsQueueManager queueManager = SmsQueueManager.getInstance(context);
            
            // Determine priority based on SMS characteristics
            int priority = determineSmsPriority(originalSender, message);
            
            // Determine forwarding SIM based on SIM selection logic (placeholder for now)
            // TODO: AŞAMA 6 will implement proper SIM selection logic
            int forwardingSubscriptionId = -1; // Will be determined by SIM selection logic
            int forwardingSimSlot = -1; // Will be determined by SIM selection logic
            
            // Queue the SMS for background processing with dual SIM support
            java.util.UUID workId = null;
            switch (priority) {
                case SmsQueueWorker.PRIORITY_HIGH:
                    workId = queueManager.queueHighPrioritySms(originalSender, message, targetPhoneNumber, timestamp,
                                                              sourceSubscriptionId, forwardingSubscriptionId, sourceSimSlot, forwardingSimSlot);
                    logDebug("SMS queued with HIGH priority for target: " + maskPhoneNumber(targetPhoneNumber));
                    break;
                case SmsQueueWorker.PRIORITY_NORMAL:
                    workId = queueManager.queueNormalPrioritySms(originalSender, message, targetPhoneNumber, timestamp,
                                                                sourceSubscriptionId, forwardingSubscriptionId, sourceSimSlot, forwardingSimSlot);
                    logDebug("SMS queued with NORMAL priority for target: " + maskPhoneNumber(targetPhoneNumber));
                    break;
                case SmsQueueWorker.PRIORITY_LOW:
                    workId = queueManager.queueLowPrioritySms(originalSender, message, targetPhoneNumber, timestamp,
                                                             sourceSubscriptionId, forwardingSubscriptionId, sourceSimSlot, forwardingSimSlot);
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
                fallbackDirectForwardingToSingleTarget(context, originalSender, message, targetNumber, timestamp, sourceSubscriptionId, sourceSimSlot);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error queuing SMS for forwarding to target: " + e.getMessage(), e);
            // Fallback to direct processing if queue fails
            fallbackDirectForwardingToSingleTarget(context, originalSender, message, targetNumber, timestamp, sourceSubscriptionId, sourceSimSlot);
        }
    }
    
    /**
     * Queue SMS for forwarding to a single target with delay
     */
    private void queueSmsForwardingToSingleTargetWithDelay(Context context, String originalSender, String message, TargetNumber targetNumber, long timestamp, long delay, int sourceSubscriptionId, int sourceSimSlot) {
        // For now, implement as immediate sending with fallback
        // In a full implementation, this would use WorkManager with scheduled delays
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            queueSmsForwardingToSingleTarget(context, originalSender, message, targetNumber, timestamp, sourceSubscriptionId, sourceSimSlot);
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
    private void fallbackDirectForwardingToMultipleTargets(Context context, String originalSender, String message, java.util.List<TargetNumber> targetNumbers, long timestamp, int sourceSubscriptionId, int sourceSimSlot) {
        logDebug("Using fallback direct forwarding for " + targetNumbers.size() + " targets");
        
        for (TargetNumber target : targetNumbers) {
            fallbackDirectForwardingToSingleTarget(context, originalSender, message, target, timestamp, sourceSubscriptionId, sourceSimSlot);
        }
    }
    
    /**
     * Fallback direct forwarding method for a single target when queue system fails
     */
    private void fallbackDirectForwardingToSingleTarget(Context context, String originalSender, String message, TargetNumber targetNumber, long timestamp, int sourceSubscriptionId, int sourceSimSlot) {
        String targetPhoneNumber = targetNumber.getPhoneNumber();
        
        // Determine forwarding SIM based on SIM selection logic (placeholder for now)
        // TODO: AŞAMA 6 will implement proper SIM selection logic
        int forwardingSubscriptionId = -1; // Will be determined by SIM selection logic
        int forwardingSimSlot = -1; // Will be determined by SIM selection logic
        
        try {
            logDebug("Using fallback direct forwarding for target: " + maskPhoneNumber(targetPhoneNumber));
            
            // Format the forwarded message with original sender info
            String forwardedMessage = String.format(
                "[Hermes SMS Forward]\nGönderen: %s\nMesaj: %s\nZaman: %s",
                originalSender,
                message,
                new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
                    .format(new java.util.Date(timestamp))
            );
            
            // Get appropriate SmsManager based on subscription ID (dual SIM support)
            SmsManager smsManager;
            if (forwardingSubscriptionId != -1 && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                try {
                    smsManager = SmsManager.getSmsManagerForSubscriptionId(forwardingSubscriptionId);
                    logDebug("Fallback using subscription-specific SmsManager for subscription " + forwardingSubscriptionId);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to get SmsManager for subscription " + forwardingSubscriptionId + 
                             " in fallback, using default: " + e.getMessage());
                    smsManager = SmsManager.getDefault();
                }
            } else {
                smsManager = SmsManager.getDefault();
                logDebug("Fallback using default SmsManager (subscription ID: " + forwardingSubscriptionId + ")");
            }
            
            if (forwardedMessage.length() > 160) {
                // Send multipart SMS
                java.util.ArrayList<String> parts = smsManager.divideMessage(forwardedMessage);
                smsManager.sendMultipartTextMessage(targetPhoneNumber, null, parts, null, null);
                String subscriptionInfo = forwardingSubscriptionId != -1 ? " via subscription " + forwardingSubscriptionId : " via default SIM";
                logDebug("Fallback multipart SMS sent to " + maskPhoneNumber(targetPhoneNumber) + " (" + parts.size() + " parts)" + subscriptionInfo);
            } else {
                // Send single SMS
                smsManager.sendTextMessage(targetPhoneNumber, null, forwardedMessage, null, null);
                String subscriptionInfo = forwardingSubscriptionId != -1 ? " via subscription " + forwardingSubscriptionId : " via default SIM";
                logDebug("Fallback single SMS sent to " + maskPhoneNumber(targetPhoneNumber) + subscriptionInfo);
            }
            
            // Log success to database with dual SIM information
            logSmsHistory(context, originalSender, message, targetPhoneNumber, forwardedMessage, timestamp, true, null, sourceSimSlot, forwardingSimSlot, sourceSubscriptionId, forwardingSubscriptionId);
            // Update last used timestamp
            updateTargetLastUsed(context, targetNumber.getId(), timestamp);
            String subscriptionInfo = forwardingSubscriptionId != -1 ? " via subscription " + forwardingSubscriptionId : " via default SIM";
            logInfo("Fallback SMS forwarding completed successfully for target: " + maskPhoneNumber(targetPhoneNumber) + subscriptionInfo);
            
        } catch (Exception e) {
            String subscriptionInfo = forwardingSubscriptionId != -1 ? " via subscription " + forwardingSubscriptionId : "";
            Log.e(TAG, "Fallback SMS forwarding failed for target " + maskPhoneNumber(targetPhoneNumber) + subscriptionInfo + ": " + e.getMessage(), e);
            // Log failure to database with dual SIM information
            logSmsHistory(context, originalSender, message, targetPhoneNumber, "", timestamp, false, "Fallback forwarding failed: " + e.getMessage(), sourceSimSlot, forwardingSimSlot, sourceSubscriptionId, forwardingSubscriptionId);
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
     * Log SMS forwarding history to database (backward compatibility)
     * Runs in optimized background thread to avoid blocking main thread
     */
    private void logSmsHistory(Context context, String senderNumber, String originalMessage, String targetNumber, String forwardedMessage, long timestamp, boolean success, String errorMessage) {
        logSmsHistory(context, senderNumber, originalMessage, targetNumber, forwardedMessage, timestamp, success, errorMessage, -1, -1, -1, -1);
    }
    
    /**
     * Log SMS forwarding history to database with dual SIM support
     * Runs in optimized background thread to avoid blocking main thread
     */
    private void logSmsHistory(Context context, String senderNumber, String originalMessage, String targetNumber, String forwardedMessage, long timestamp, boolean success, String errorMessage, int sourceSimSlot, int forwardingSimSlot, int sourceSubscriptionId, int forwardingSubscriptionId) {
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
                    errorMessage,
                    sourceSimSlot,
                    forwardingSimSlot,
                    sourceSubscriptionId,
                    forwardingSubscriptionId
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