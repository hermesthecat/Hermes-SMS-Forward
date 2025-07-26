package com.keremgok.sms;

import android.content.Context;
import android.util.Log;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkQuery;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * SMS Queue Manager using WorkManager for optimized background processing
 * Handles batch processing, retry logic, and priority-based SMS forwarding
 */
public class SmsQueueManager {
    
    private static final String TAG = "SmsQueueManager";
    private static final boolean DEBUG = true;
    
    // Work tags for different priority levels
    private static final String WORK_TAG_HIGH_PRIORITY = "sms_queue_high";
    private static final String WORK_TAG_NORMAL_PRIORITY = "sms_queue_normal";
    private static final String WORK_TAG_LOW_PRIORITY = "sms_queue_low";
    private static final String WORK_TAG_BATCH = "sms_batch";
    
    // Unique work names
    private static final String WORK_NAME_HIGH = "sms_high_priority";
    private static final String WORK_NAME_NORMAL = "sms_normal_priority";
    private static final String WORK_NAME_LOW = "sms_low_priority";
    
    // Batch processing configuration
    private static final int BATCH_SIZE = 5; // Process 5 SMS at a time
    private static final long BATCH_DELAY_MS = 10000; // 10 seconds delay for batching
    
    private final Context context;
    private final WorkManager workManager;
    
    // Singleton instance
    private static SmsQueueManager instance;
    
    private SmsQueueManager(Context context) {
        this.context = context.getApplicationContext();
        this.workManager = WorkManager.getInstance(this.context);
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized SmsQueueManager getInstance(Context context) {
        if (instance == null) {
            instance = new SmsQueueManager(context);
        }
        return instance;
    }
    
    /**
     * Queue SMS for processing with high priority
     */
    public UUID queueHighPrioritySms(String originalSender, String originalMessage, String targetNumber, long timestamp) {
        return queueSms(originalSender, originalMessage, targetNumber, timestamp, 0, SmsQueueWorker.PRIORITY_HIGH);
    }
    
    /**
     * Queue SMS for processing with normal priority
     */
    public UUID queueNormalPrioritySms(String originalSender, String originalMessage, String targetNumber, long timestamp) {
        return queueSms(originalSender, originalMessage, targetNumber, timestamp, 0, SmsQueueWorker.PRIORITY_NORMAL);
    }
    
    /**
     * Queue SMS for processing with low priority
     */
    public UUID queueLowPrioritySms(String originalSender, String originalMessage, String targetNumber, long timestamp) {
        return queueSms(originalSender, originalMessage, targetNumber, timestamp, 0, SmsQueueWorker.PRIORITY_LOW);
    }
    
    /**
     * Queue SMS for processing with specified priority
     */
    private UUID queueSms(String originalSender, String originalMessage, String targetNumber, 
                         long timestamp, int retryCount, int priority) {
        try {
            // Create input data
            Data inputData = SmsQueueWorker.createInputData(
                originalSender, originalMessage, targetNumber, timestamp, retryCount, priority
            );
            
            // Create constraints for SMS processing
            Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // SMS doesn't require internet
                .setRequiresBatteryNotLow(false) // Allow even on low battery for SMS forwarding
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .setRequiresStorageNotLow(true) // Require storage for database operations
                .build();
            
            // Calculate delay and backoff based on priority and retry count
            long initialDelay = calculateInitialDelay(priority, retryCount);
            long backoffDelay = SmsQueueWorker.calculateBackoffDelay(retryCount);
            
            // Create work request
            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SmsQueueWorker.class)
                .setInputData(inputData)
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, backoffDelay, TimeUnit.MILLISECONDS)
                .addTag(getWorkTag(priority))
                .addTag(WORK_TAG_BATCH)
                .build();
            
            // Enqueue work with policy based on priority
            ExistingWorkPolicy policy = getWorkPolicy(priority);
            String workName = getWorkName(priority);
            
            workManager.enqueueUniqueWork(workName, policy, workRequest);
            
            logDebug("SMS queued for processing: priority=" + priority + ", delay=" + initialDelay + "ms, retry=" + retryCount);
            
            return workRequest.getId();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to queue SMS: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Get queue status and statistics
     */
    public QueueStatus getQueueStatus() {
        try {
            WorkQuery query = WorkQuery.Builder
                .fromTags(List.of(WORK_TAG_BATCH))
                .addStates(List.of(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING))
                .build();
            
            List<WorkInfo> workInfos = workManager.getWorkInfos(query).get();
            
            int totalQueued = 0;
            int highPriority = 0;
            int normalPriority = 0;
            int lowPriority = 0;
            
            for (WorkInfo workInfo : workInfos) {
                totalQueued++;
                
                if (workInfo.getTags().contains(WORK_TAG_HIGH_PRIORITY)) {
                    highPriority++;
                } else if (workInfo.getTags().contains(WORK_TAG_NORMAL_PRIORITY)) {
                    normalPriority++;
                } else if (workInfo.getTags().contains(WORK_TAG_LOW_PRIORITY)) {
                    lowPriority++;
                }
            }
            
            return new QueueStatus(totalQueued, highPriority, normalPriority, lowPriority);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to get queue status: " + e.getMessage(), e);
            return new QueueStatus(0, 0, 0, 0);
        }
    }
    
    /**
     * Cancel all pending SMS in queue
     */
    public void cancelAllPendingSms() {
        try {
            workManager.cancelAllWorkByTag(WORK_TAG_BATCH);
            logDebug("All pending SMS cancelled from queue");
        } catch (Exception e) {
            Log.e(TAG, "Failed to cancel pending SMS: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cancel SMS by work ID
     */
    public void cancelSms(UUID workId) {
        try {
            if (workId != null) {
                workManager.cancelWorkById(workId);
                logDebug("SMS cancelled from queue: " + workId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to cancel SMS: " + e.getMessage(), e);
        }
    }
    
    /**
     * Perform queue maintenance and cleanup
     */
    public void performQueueMaintenance() {
        try {
            // Prune completed and failed work (keep last 100 entries)
            workManager.pruneWork();
            
            // Cancel old failed work that's stuck
            WorkQuery stuckQuery = WorkQuery.Builder
                .fromTags(List.of(WORK_TAG_BATCH))
                .addStates(List.of(WorkInfo.State.FAILED))
                .build();
            
            List<WorkInfo> stuckWork = workManager.getWorkInfos(stuckQuery).get();
            for (WorkInfo workInfo : stuckWork) {
                // Cancel failed work immediately as WorkInfo doesn't provide finish time
                // In a production environment, you might want to use a different strategy
                workManager.cancelWorkById(workInfo.getId());
                logDebug("Cancelled stuck work: " + workInfo.getId());
            }
            
            logDebug("Queue maintenance completed");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to perform queue maintenance: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculate initial delay based on priority and retry count
     */
    private long calculateInitialDelay(int priority, int retryCount) {
        long baseDelay = 0;
        
        switch (priority) {
            case SmsQueueWorker.PRIORITY_HIGH:
                baseDelay = 0; // Immediate processing
                break;
            case SmsQueueWorker.PRIORITY_NORMAL:
                baseDelay = 1000; // 1 second delay
                break;
            case SmsQueueWorker.PRIORITY_LOW:
                baseDelay = 5000; // 5 seconds delay
                break;
        }
        
        // Add exponential backoff for retries
        if (retryCount > 0) {
            baseDelay += SmsQueueWorker.calculateBackoffDelay(retryCount);
        }
        
        return baseDelay;
    }
    
    /**
     * Get work tag based on priority
     */
    private String getWorkTag(int priority) {
        switch (priority) {
            case SmsQueueWorker.PRIORITY_HIGH:
                return WORK_TAG_HIGH_PRIORITY;
            case SmsQueueWorker.PRIORITY_NORMAL:
                return WORK_TAG_NORMAL_PRIORITY;
            case SmsQueueWorker.PRIORITY_LOW:
                return WORK_TAG_LOW_PRIORITY;
            default:
                return WORK_TAG_NORMAL_PRIORITY;
        }
    }
    
    /**
     * Get work name based on priority
     */
    private String getWorkName(int priority) {
        switch (priority) {
            case SmsQueueWorker.PRIORITY_HIGH:
                return WORK_NAME_HIGH;
            case SmsQueueWorker.PRIORITY_NORMAL:
                return WORK_NAME_NORMAL;
            case SmsQueueWorker.PRIORITY_LOW:
                return WORK_NAME_LOW;
            default:
                return WORK_NAME_NORMAL;
        }
    }
    
    /**
     * Get work policy based on priority
     */
    private ExistingWorkPolicy getWorkPolicy(int priority) {
        switch (priority) {
            case SmsQueueWorker.PRIORITY_HIGH:
                return ExistingWorkPolicy.REPLACE; // Replace for urgent SMS
            case SmsQueueWorker.PRIORITY_NORMAL:
                return ExistingWorkPolicy.APPEND; // Append for normal processing
            case SmsQueueWorker.PRIORITY_LOW:
                return ExistingWorkPolicy.APPEND_OR_REPLACE; // Flexible for low priority
            default:
                return ExistingWorkPolicy.APPEND;
        }
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
     * Queue status data class
     */
    public static class QueueStatus {
        public final int totalQueued;
        public final int highPriorityCount;
        public final int normalPriorityCount;
        public final int lowPriorityCount;
        
        public QueueStatus(int totalQueued, int highPriorityCount, int normalPriorityCount, int lowPriorityCount) {
            this.totalQueued = totalQueued;
            this.highPriorityCount = highPriorityCount;
            this.normalPriorityCount = normalPriorityCount;
            this.lowPriorityCount = lowPriorityCount;
        }
        
        @Override
        public String toString() {
            return String.format("QueueStatus{total=%d, high=%d, normal=%d, low=%d}", 
                totalQueued, highPriorityCount, normalPriorityCount, lowPriorityCount);
        }
    }
}