package com.keremgok.sms;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.util.Log;
import java.text.DecimalFormat;

/**
 * Performance monitoring utility for tracking memory usage and app performance
 * Used to test and validate performance optimizations in Task 15
 */
public class PerformanceMonitor {
    
    private static final String TAG = "HermesPerformance";
    private static final boolean ENABLE_MONITORING = false; // Set to false for production
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    
    private static PerformanceMonitor instance;
    private long startTime;
    private long initialMemory;
    
    private PerformanceMonitor() {
        reset();
    }
    
    public static synchronized PerformanceMonitor getInstance() {
        if (instance == null) {
            instance = new PerformanceMonitor();
        }
        return instance;
    }
    
    /**
     * Reset monitoring counters
     */
    public void reset() {
        startTime = System.currentTimeMillis();
        initialMemory = getCurrentMemoryUsage();
    }
    
    /**
     * Get current memory usage in MB
     */
    public long getCurrentMemoryUsage() {
        Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memoryInfo);
        return memoryInfo.getTotalPrivateDirty() / 1024; // Convert KB to MB
    }
    
    /**
     * Get memory usage using ActivityManager
     */
    public long getSystemMemoryUsage(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        
        long usedMemory = memoryInfo.totalMem - memoryInfo.availMem;
        return usedMemory / (1024 * 1024); // Convert bytes to MB
    }
    
    /**
     * Log current memory usage and performance metrics
     */
    public void logCurrentStatus(Context context, String tag) {
        if (!ENABLE_MONITORING) return;
        
        long currentMemory = getCurrentMemoryUsage();
        long memoryDelta = currentMemory - initialMemory;
        long elapsed = System.currentTimeMillis() - startTime;
        
        // Get thread pool statistics
        ThreadManager.ThreadPoolStats threadStats = ThreadManager.getInstance().getStats();
        
        String logMessage = String.format(
            "[%s] Performance Status:\n" +
            "  Memory: %d MB (Δ%+d MB)\n" +
            "  Elapsed: %s seconds\n" +
            "  Thread Pools: %s\n" +
            "  System Memory: %d MB",
            tag,
            currentMemory,
            memoryDelta,
            DECIMAL_FORMAT.format(elapsed / 1000.0),
            threadStats.toString(),
            getSystemMemoryUsage(context)
        );
        
        Log.i(TAG, logMessage);
    }
    
    /**
     * Start monitoring a specific operation
     */
    public OperationMonitor startOperation(String operationName) {
        return new OperationMonitor(operationName);
    }
    
    /**
     * Monitor specific operations for detailed performance tracking
     */
    public static class OperationMonitor {
        private final String operationName;
        private final long startTime;
        private final long startMemory;
        
        private OperationMonitor(String operationName) {
            this.operationName = operationName;
            this.startTime = System.currentTimeMillis();
            this.startMemory = getInstance().getCurrentMemoryUsage();
        }
        
        /**
         * End monitoring and log results
         */
        public void end() {
            if (!ENABLE_MONITORING) return;
            
            long endTime = System.currentTimeMillis();
            long endMemory = getInstance().getCurrentMemoryUsage();
            long duration = endTime - startTime;
            long memoryDelta = endMemory - startMemory;
            
            String logMessage = String.format(
                "[%s] Operation completed in %d ms, Memory Δ%+d MB",
                operationName,
                duration,
                memoryDelta
            );
            
            if (duration > 1000) {
                Log.w(TAG, logMessage + " (SLOW OPERATION)");
            } else {
                Log.d(TAG, logMessage);
            }
        }
    }
    
    /**
     * Run comprehensive performance test
     */
    public void runPerformanceTest(Context context) {
        if (!ENABLE_MONITORING) return;
        
        Log.i(TAG, "=== Starting Performance Test ===");
        
        reset();
        logCurrentStatus(context, "Initial State");
        
        // Test database operations
        OperationMonitor dbTest = startOperation("Database Operations Test");
        try {
            AppDatabase database = AppDatabase.getInstance(context);
            
            // Simulate some database operations
            ThreadManager.getInstance().executeDatabase(() -> {
                try {
                    // Test database read
                    database.smsHistoryDao().getAllHistory();
                    
                    // Skip test data insertion to avoid polluting history
                    
                } catch (Exception e) {
                    Log.e(TAG, "Database test error: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Performance test error: " + e.getMessage());
        }
        dbTest.end();
        
        // Test queue operations
        OperationMonitor queueTest = startOperation("Queue Operations Test");
        try {
            SmsQueueManager queueManager = SmsQueueManager.getInstance(context);
            
            // Test queue stats
            SmsQueueManager.QueueStatus stats = queueManager.getQueueStatus();
            Log.d(TAG, "Queue Stats: " + stats.toString());
            
        } catch (Exception e) {
            Log.e(TAG, "Queue test error: " + e.getMessage());
        }
        queueTest.end();
        
        logCurrentStatus(context, "Final State");
        Log.i(TAG, "=== Performance Test Completed ===");
    }
    
    /**
     * Monitor memory leaks by forcing garbage collection and checking memory
     */
    public void checkMemoryLeaks(Context context) {
        if (!ENABLE_MONITORING) return;
        
        Log.i(TAG, "=== Memory Leak Check ===");
        
        long beforeGC = getCurrentMemoryUsage();
        System.gc();
        
        // Wait a bit for GC to complete
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long afterGC = getCurrentMemoryUsage();
        long memoryFreed = beforeGC - afterGC;
        
        Log.i(TAG, String.format(
            "Memory before GC: %d MB, after GC: %d MB, freed: %d MB",
            beforeGC, afterGC, memoryFreed
        ));
        
        if (memoryFreed < 0) {
            Log.w(TAG, "Memory usage increased after GC - possible memory leak");
        } else if (memoryFreed > 10) {
            Log.w(TAG, "Large amount of memory freed - check for memory waste");
        }
        
        logCurrentStatus(context, "After Memory Check");
    }
    
    /**
     * Clean up any remaining test data from performance tests
     */
    public static void cleanupTestData(Context context) {
        try {
            AppDatabase database = AppDatabase.getInstance(context);
            ThreadManager.getInstance().executeDatabase(() -> {
                try {
                    // Remove any test data that might have been left behind
                    int deletedCount = 0;
                    deletedCount += database.smsHistoryDao().deleteTestData("TEST_SENDER");
                    deletedCount += database.smsHistoryDao().deleteTestData("TEST_TARGET");
                    deletedCount += database.smsHistoryDao().deleteTestData("test sender");
                    deletedCount += database.smsHistoryDao().deleteTestData("Test Sender");
                    
                    if (deletedCount > 0) {
                        Log.i(TAG, "Test data cleanup completed: " + deletedCount + " records removed");
                    } else {
                        Log.d(TAG, "Test data cleanup completed: No test records found");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error during test data cleanup: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to cleanup test data: " + e.getMessage());
        }
    }
    
    /**
     * Enable or disable performance monitoring
     */
    public static boolean isMonitoringEnabled() {
        return ENABLE_MONITORING;
    }
}