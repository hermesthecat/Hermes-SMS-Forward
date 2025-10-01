package com.keremgok.sms;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * WorkManager Worker for handling SMS forwarding queue
 * Provides optimized background SMS processing with batch support
 */
public class SmsQueueWorker extends Worker {
    
    private static final String TAG = "SmsQueueWorker";
    private static final boolean DEBUG = false; // Production safe - no debug logs
    
    // Input Data Keys
    public static final String KEY_ORIGINAL_SENDER = "original_sender";
    public static final String KEY_ORIGINAL_MESSAGE = "original_message";
    public static final String KEY_TARGET_NUMBER = "target_number";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_RETRY_COUNT = "retry_count";
    public static final String KEY_PRIORITY = "priority";
    
    // Dual SIM Support Keys
    public static final String KEY_SOURCE_SUBSCRIPTION_ID = "source_subscription_id";
    public static final String KEY_FORWARDING_SUBSCRIPTION_ID = "forwarding_subscription_id";
    public static final String KEY_SOURCE_SIM_SLOT = "source_sim_slot";
    public static final String KEY_FORWARDING_SIM_SLOT = "forwarding_sim_slot";
    
    // Priority levels for SMS processing
    public static final int PRIORITY_HIGH = 0;
    public static final int PRIORITY_NORMAL = 1;
    public static final int PRIORITY_LOW = 2;
    
    // Retry configuration
    private static final int MAX_RETRY_COUNT = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 2000; // 2 seconds
    
    public SmsQueueWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        try {
            logDebug("SMS queue worker started");
            
            // Get input data
            Data inputData = getInputData();
            String originalSender = inputData.getString(KEY_ORIGINAL_SENDER);
            String originalMessage = inputData.getString(KEY_ORIGINAL_MESSAGE);
            String targetNumber = inputData.getString(KEY_TARGET_NUMBER);
            long timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis());
            int retryCount = inputData.getInt(KEY_RETRY_COUNT, 0);
            int priority = inputData.getInt(KEY_PRIORITY, PRIORITY_NORMAL);
            
            // Get dual SIM data
            int sourceSubscriptionId = inputData.getInt(KEY_SOURCE_SUBSCRIPTION_ID, -1);
            int forwardingSubscriptionId = inputData.getInt(KEY_FORWARDING_SUBSCRIPTION_ID, -1);
            int sourceSimSlot = inputData.getInt(KEY_SOURCE_SIM_SLOT, -1);
            int forwardingSimSlot = inputData.getInt(KEY_FORWARDING_SIM_SLOT, -1);
            
            // Log SIM information for debugging
            if (forwardingSubscriptionId != -1 || forwardingSimSlot != -1) {
                logDebug("Processing SMS with dual SIM - Forwarding via subscription " + forwardingSubscriptionId + 
                         ", slot " + forwardingSimSlot);
                SimLogger.logSimOperation("SMS_QUEUE_PROCESS", sourceSubscriptionId, forwardingSubscriptionId,
                    "Priority: " + priority + ", Retry: " + retryCount, SimLogger.LEVEL_INFO);
            } else {
                SimLogger.logSimOperation("SMS_QUEUE_PROCESS", -1, -1,
                    "Single SIM mode, Priority: " + priority + ", Retry: " + retryCount, SimLogger.LEVEL_DEBUG);
            }
            
            // Validate input data
            if (originalSender == null || originalMessage == null || targetNumber == null) {
                Log.e(TAG, "Invalid input data for SMS queue worker");
                logSmsHistoryFailure(originalSender != null ? originalSender : "Unknown", 
                                   originalMessage != null ? originalMessage : "Unknown", 
                                   targetNumber != null ? targetNumber : "Unknown", 
                                   timestamp, "Invalid input data", 
                                   sourceSimSlot, forwardingSimSlot, sourceSubscriptionId, forwardingSubscriptionId);
                return Result.failure();
            }
            
            // Format forwarded message using SmsFormatter
            SmsFormatter formatter = new SmsFormatter(getApplicationContext());
            String forwardedMessage = formatter.formatMessage(originalSender, originalMessage, timestamp,
                                                            sourceSimSlot, forwardingSimSlot,
                                                            sourceSubscriptionId, forwardingSubscriptionId);
            
            // Process SMS based on priority with dual SIM support
            boolean success = processSmsWithPriority(forwardedMessage, targetNumber, priority, forwardingSubscriptionId);
            
            if (success) {
                logDebug("SMS successfully queued for sending in queue worker");
                long processingTime = System.currentTimeMillis() - timestamp;
                SimLogger.logSmsForwarding(originalSender, targetNumber, sourceSimSlot, forwardingSimSlot, 
                    true, processingTime);
                // Don't log to history here - let SmsCallbackReceiver handle the actual result
                return Result.success();
            } else {
                logDebug("SMS processing failed in queue worker, retry count: " + retryCount);
                long processingTime = System.currentTimeMillis() - timestamp;
                SimLogger.logSmsForwarding(originalSender, targetNumber, sourceSimSlot, forwardingSimSlot, 
                    false, processingTime);
                
                // Check if we should retry
                if (retryCount < MAX_RETRY_COUNT) {
                    // Only log failure if immediate processing error, not SMS sending failure
                    // SmsCallbackReceiver will handle actual SMS status
                    return Result.retry();
                } else {
                    Log.e(TAG, "SMS processing failed permanently after " + MAX_RETRY_COUNT + " attempts");
                    // Log immediate processing failure to history
                    logSmsHistoryFailure(originalSender, originalMessage, targetNumber, timestamp, 
                        "Max retries exceeded (" + MAX_RETRY_COUNT + " attempts) - processing error",
                        sourceSimSlot, forwardingSimSlot, sourceSubscriptionId, forwardingSubscriptionId);
                    return Result.failure();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Exception in SMS queue worker: " + e.getMessage(), e);
            
            // Log exception failure to history
            Data inputData = getInputData();
            String originalSender = inputData.getString(KEY_ORIGINAL_SENDER);
            String originalMessage = inputData.getString(KEY_ORIGINAL_MESSAGE);
            String targetNumber = inputData.getString(KEY_TARGET_NUMBER);
            long timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis());
            int sourceSimSlot = inputData.getInt(KEY_SOURCE_SIM_SLOT, -1);
            int forwardingSimSlot = inputData.getInt(KEY_FORWARDING_SIM_SLOT, -1);
            int sourceSubscriptionId = inputData.getInt(KEY_SOURCE_SUBSCRIPTION_ID, -1);
            int forwardingSubscriptionId = inputData.getInt(KEY_FORWARDING_SUBSCRIPTION_ID, -1);
            
            logSmsHistoryFailure(originalSender, originalMessage, targetNumber, timestamp, 
                "Worker exception: " + e.getMessage(), sourceSimSlot, forwardingSimSlot, 
                sourceSubscriptionId, forwardingSubscriptionId);
            
            return Result.failure();
        }
    }
    
    /**
     * Process SMS with priority-based handling and dual SIM support
     * @param message The message to send
     * @param targetNumber The target phone number
     * @param priority The SMS priority level
     * @param forwardingSubscriptionId The subscription ID for forwarding (-1 for default)
     * @return true if SMS was sent successfully, false otherwise
     */
    private boolean processSmsWithPriority(String message, String targetNumber, int priority, int forwardingSubscriptionId) {
        try {
            // Add delay based on priority to manage system resources
            switch (priority) {
                case PRIORITY_HIGH:
                    // No delay for high priority
                    break;
                case PRIORITY_NORMAL:
                    Thread.sleep(500); // 500ms delay
                    break;
                case PRIORITY_LOW:
                    Thread.sleep(1000); // 1s delay
                    break;
            }
            
            // Get appropriate SmsManager with enhanced fallback mechanism
            SmsManager smsManager = getSmsManagerWithFallback(forwardingSubscriptionId);
            
            // Check if message needs to be split
            if (message.length() > 160) {
                return sendMultipartSms(smsManager, message, targetNumber, forwardingSubscriptionId);
            } else {
                return sendSingleSms(smsManager, message, targetNumber, forwardingSubscriptionId);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing SMS with priority " + priority + ": " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Send single SMS message with dual SIM support and callback tracking
     * @param smsManager The SmsManager instance to use
     * @param message The message to send
     * @param targetNumber The target phone number
     * @param subscriptionId The subscription ID being used (-1 if default)
     * @return true if SMS was sent successfully, false otherwise
     */
    private boolean sendSingleSms(SmsManager smsManager, String message, String targetNumber, int subscriptionId) {
        try {
            // Create callback intents for SMS status tracking
            Data inputData = getInputData();
            String originalSender = inputData.getString(KEY_ORIGINAL_SENDER);
            String originalMessage = inputData.getString(KEY_ORIGINAL_MESSAGE);
            long timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis());
            int retryCount = inputData.getInt(KEY_RETRY_COUNT, 0);
            
            PendingIntent sentIntent = createSentIntent(originalSender, originalMessage, message, 
                                                       targetNumber, timestamp, retryCount, false);
            PendingIntent deliveredIntent = createDeliveredIntent(targetNumber);
            
            smsManager.sendTextMessage(targetNumber, null, message, sentIntent, deliveredIntent);
            String subscriptionInfo = subscriptionId != -1 ? " via subscription " + subscriptionId : " via default SIM";
            logDebug("Single SMS queued for sending to " + maskPhoneNumber(targetNumber) + subscriptionInfo);
            
            // Don't immediately return true - let the callback determine success
            // For now, assume success as the callback will handle actual status updates
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to send single SMS" + (subscriptionId != -1 ? " via subscription " + subscriptionId : "") + 
                      ": " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Send multipart SMS message with dual SIM support and callback tracking
     * @param smsManager The SmsManager instance to use
     * @param message The message to send
     * @param targetNumber The target phone number
     * @param subscriptionId The subscription ID being used (-1 if default)
     * @return true if SMS was sent successfully, false otherwise
     */
    private boolean sendMultipartSms(SmsManager smsManager, String message, String targetNumber, int subscriptionId) {
        try {
            ArrayList<String> parts = smsManager.divideMessage(message);
            
            // Create callback intents for multipart SMS status tracking
            Data inputData = getInputData();
            String originalSender = inputData.getString(KEY_ORIGINAL_SENDER);
            String originalMessage = inputData.getString(KEY_ORIGINAL_MESSAGE);
            long timestamp = inputData.getLong(KEY_TIMESTAMP, System.currentTimeMillis());
            int retryCount = inputData.getInt(KEY_RETRY_COUNT, 0);
            
            // Create arrays of intents for each part
            ArrayList<PendingIntent> sentIntents = new ArrayList<>();
            ArrayList<PendingIntent> deliveredIntents = new ArrayList<>();
            
            for (int i = 0; i < parts.size(); i++) {
                PendingIntent sentIntent = createSentIntent(originalSender, originalMessage, message, 
                                                           targetNumber, timestamp, retryCount, true);
                PendingIntent deliveredIntent = createDeliveredIntent(targetNumber);
                sentIntents.add(sentIntent);
                deliveredIntents.add(deliveredIntent);
            }
            
            smsManager.sendMultipartTextMessage(targetNumber, null, parts, sentIntents, deliveredIntents);
            String subscriptionInfo = subscriptionId != -1 ? " via subscription " + subscriptionId : " via default SIM";
            logDebug("Multipart SMS queued for sending to " + maskPhoneNumber(targetNumber) + 
                    " (" + parts.size() + " parts)" + subscriptionInfo);
            
            // Don't immediately return true - let the callbacks determine success
            // For now, assume success as the callbacks will handle actual status updates
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to send multipart SMS" + (subscriptionId != -1 ? " via subscription " + subscriptionId : "") + 
                      ": " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Format the forwarded message with original sender info (deprecated - use SmsFormatter)
     * @deprecated Use SmsFormatter.formatMessage() instead
     */
    @Deprecated
    private String formatForwardedMessage(String originalSender, String originalMessage, long timestamp) {
        // Fallback to SmsFormatter for consistency
        SmsFormatter formatter = new SmsFormatter(getApplicationContext());
        return formatter.formatMessage(originalSender, originalMessage, timestamp);
    }
    
    /**
     * Log successful SMS to history database with dual SIM support
     */
    private void logSmsHistorySuccess(String originalSender, String originalMessage, String targetNumber, 
                                    String forwardedMessage, long timestamp, int sourceSimSlot, int forwardingSimSlot,
                                    int sourceSubscriptionId, int forwardingSubscriptionId) {
        try {
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            SmsHistory history = new SmsHistory(
                originalSender,
                originalMessage,
                targetNumber,
                forwardedMessage,
                timestamp,
                true, // success
                null, // no error message
                sourceSimSlot,
                forwardingSimSlot,
                sourceSubscriptionId,
                forwardingSubscriptionId
            );
            database.smsHistoryDao().insert(history);
            
            if (DEBUG) {
                logDebug("SMS history logged: SUCCESS from " + maskPhoneNumber(originalSender) + 
                        " to " + maskPhoneNumber(targetNumber));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to log successful SMS history: " + e.getMessage(), e);
        }
    }
    
    /**
     * Log failed SMS to history database with dual SIM support
     */
    private void logSmsHistoryFailure(String originalSender, String originalMessage, String targetNumber, 
                                    long timestamp, String errorMessage, int sourceSimSlot, int forwardingSimSlot,
                                    int sourceSubscriptionId, int forwardingSubscriptionId) {
        try {
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            SmsHistory history = new SmsHistory(
                originalSender,
                originalMessage,
                targetNumber,
                "", // no forwarded message on failure
                timestamp,
                false, // failure
                errorMessage,
                sourceSimSlot,
                forwardingSimSlot,
                sourceSubscriptionId,
                forwardingSubscriptionId
            );
            database.smsHistoryDao().insert(history);
            
            if (DEBUG) {
                logDebug("SMS history logged: FAILURE from " + maskPhoneNumber(originalSender) + 
                        " to " + maskPhoneNumber(targetNumber) + " - " + errorMessage);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to log failed SMS history: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get SmsManager with comprehensive fallback mechanism
     * @param preferredSubscriptionId Preferred subscription ID
     * @return SmsManager instance with fallback to default if needed
     */
    private SmsManager getSmsManagerWithFallback(int preferredSubscriptionId) {
        try {
            // If no specific subscription requested, use default
            if (preferredSubscriptionId == -1) {
                logDebug("No specific subscription requested, using default SmsManager");
                return SmsManager.getDefault();
            }
            
            // Check if dual SIM API is supported
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                logDebug("Dual SIM API not supported (API < 22), using default SmsManager");
                return SmsManager.getDefault();
            }
            
            // Validate subscription before using it
            if (!SimManager.isSubscriptionValid(getApplicationContext(), preferredSubscriptionId)) {
                Log.w(TAG, "Preferred subscription " + preferredSubscriptionId + " is not valid, finding fallback");
                int fallbackSubscriptionId = SimManager.getFallbackSubscriptionId(getApplicationContext(), preferredSubscriptionId);
                
                if (fallbackSubscriptionId != -1 && fallbackSubscriptionId != preferredSubscriptionId) {
                    logDebug("Using fallback subscription " + fallbackSubscriptionId + " instead of " + preferredSubscriptionId);
                    return SmsManager.getSmsManagerForSubscriptionId(fallbackSubscriptionId);
                } else {
                    logDebug("No valid fallback subscription found, using default SmsManager");
                    return SmsManager.getDefault();
                }
            }
            
            // Try to get subscription-specific SmsManager
            try {
                SmsManager subscriptionManager = SmsManager.getSmsManagerForSubscriptionId(preferredSubscriptionId);
                logDebug("Successfully created SmsManager for subscription " + preferredSubscriptionId);
                return subscriptionManager;
            } catch (Exception e) {
                Log.w(TAG, "Failed to create SmsManager for subscription " + preferredSubscriptionId + 
                           ", trying fallback: " + e.getMessage());
                
                // Try fallback subscription
                int fallbackSubscriptionId = SimManager.getFallbackSubscriptionId(getApplicationContext(), preferredSubscriptionId);
                if (fallbackSubscriptionId != -1 && fallbackSubscriptionId != preferredSubscriptionId) {
                    try {
                        SmsManager fallbackManager = SmsManager.getSmsManagerForSubscriptionId(fallbackSubscriptionId);
                        logDebug("Successfully created fallback SmsManager for subscription " + fallbackSubscriptionId);
                        return fallbackManager;
                    } catch (Exception fallbackException) {
                        Log.w(TAG, "Fallback subscription " + fallbackSubscriptionId + " also failed: " + fallbackException.getMessage());
                    }
                }
                
                // Ultimate fallback to default
                logDebug("All subscription attempts failed, using default SmsManager");
                return SmsManager.getDefault();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in getSmsManagerWithFallback: " + e.getMessage(), e);
            // Ultimate fallback
            return SmsManager.getDefault();
        }
    }
    
    /**
     * Mask phone number for secure logging
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "***";
        }
        
        String prefix = phoneNumber.substring(0, Math.min(5, phoneNumber.length() - 4));
        String suffix = phoneNumber.substring(phoneNumber.length() - 4);
        return prefix + "***" + suffix;
    }
    
    /**
     * Secure debug logging
     */
    private void logDebug(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }
    
    /**
     * Create input data for queue worker with dual SIM support
     */
    public static Data createInputData(String originalSender, String originalMessage, 
                                     String targetNumber, long timestamp, int retryCount, int priority,
                                     int sourceSubscriptionId, int forwardingSubscriptionId,
                                     int sourceSimSlot, int forwardingSimSlot) {
        return new Data.Builder()
            .putString(KEY_ORIGINAL_SENDER, originalSender)
            .putString(KEY_ORIGINAL_MESSAGE, originalMessage)
            .putString(KEY_TARGET_NUMBER, targetNumber)
            .putLong(KEY_TIMESTAMP, timestamp)
            .putInt(KEY_RETRY_COUNT, retryCount)
            .putInt(KEY_PRIORITY, priority)
            .putInt(KEY_SOURCE_SUBSCRIPTION_ID, sourceSubscriptionId)
            .putInt(KEY_FORWARDING_SUBSCRIPTION_ID, forwardingSubscriptionId)
            .putInt(KEY_SOURCE_SIM_SLOT, sourceSimSlot)
            .putInt(KEY_FORWARDING_SIM_SLOT, forwardingSimSlot)
            .build();
    }
    
    /**
     * Create input data for queue worker (backward compatibility)
     */
    public static Data createInputData(String originalSender, String originalMessage, 
                                     String targetNumber, long timestamp, int retryCount, int priority) {
        return createInputData(originalSender, originalMessage, targetNumber, timestamp, retryCount, priority,
                              -1, -1, -1, -1);
    }
    
    /**
     * Calculate exponential backoff delay
     */
    public static long calculateBackoffDelay(int retryCount) {
        return INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, retryCount);
    }
    
    /**
     * Create PendingIntent for SMS sent callback
     */
    private PendingIntent createSentIntent(String originalSender, String originalMessage, String forwardedMessage, 
                                         String targetNumber, long timestamp, int retryCount, boolean isMultipart) {
        Intent intent = new Intent(getApplicationContext(), SmsCallbackReceiver.class);
        intent.setAction("SMS_SENT");
        intent.putExtra("originalSender", originalSender);
        intent.putExtra("originalMessage", originalMessage);
        intent.putExtra("forwardedMessage", forwardedMessage);
        intent.putExtra("targetNumber", targetNumber);
        intent.putExtra("timestamp", timestamp);
        intent.putExtra("retryCount", retryCount);
        intent.putExtra("isMultipart", isMultipart);
        
        // Use timestamp and target as unique identifier
        int requestCode = (int) ((timestamp % 100000) + targetNumber.hashCode() % 1000);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(getApplicationContext(), requestCode, intent, flags);
    }
    
    /**
     * Create PendingIntent for SMS delivered callback
     */
    private PendingIntent createDeliveredIntent(String targetNumber) {
        Intent intent = new Intent(getApplicationContext(), SmsCallbackReceiver.class);
        intent.setAction("SMS_DELIVERED");
        intent.putExtra("targetNumber", targetNumber);
        int requestCode = (int) (System.currentTimeMillis() % 100000 + targetNumber.hashCode() % 1000);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(getApplicationContext(), requestCode, intent, flags);
    }
}