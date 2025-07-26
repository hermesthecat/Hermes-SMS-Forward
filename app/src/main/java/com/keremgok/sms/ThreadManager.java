package com.keremgok.sms;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Centralized thread management for performance optimization
 * Replaces scattered new Thread() calls with managed thread pools
 */
public class ThreadManager {
    
    private static ThreadManager instance;
    
    // Thread pools for different types of operations
    private final ExecutorService databaseExecutor;
    private final ExecutorService networkExecutor;
    private final ExecutorService backgroundExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    
    // Main thread handler for UI updates
    private final Handler mainHandler;
    
    private ThreadManager() {
        // Database operations pool - single thread to avoid conflicts
        databaseExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("HermesDB-Thread");
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });
        
        // Network/SMS operations pool - small pool for concurrent operations
        networkExecutor = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r);
            t.setName("HermesNetwork-Thread");
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });
        
        // General background operations pool
        backgroundExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setName("HermesBackground-Thread");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        });
        
        // Scheduled operations pool
        scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r);
            t.setName("HermesScheduled-Thread");
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        });
        
        // Main thread handler for UI updates
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized ThreadManager getInstance() {
        if (instance == null) {
            instance = new ThreadManager();
        }
        return instance;
    }
    
    /**
     * Execute database operation in background
     * Use this for Room database operations
     */
    public void executeDatabase(Runnable task) {
        databaseExecutor.execute(task);
    }
    
    /**
     * Execute network/SMS operation in background
     * Use this for SMS sending, network calls
     */
    public void executeNetwork(Runnable task) {
        networkExecutor.execute(task);
    }
    
    /**
     * Execute general background operation
     * Use this for file operations, calculations, etc.
     */
    public void executeBackground(Runnable task) {
        backgroundExecutor.execute(task);
    }
    
    /**
     * Schedule task with delay
     */
    public void schedule(Runnable task, long delay, TimeUnit timeUnit) {
        scheduledExecutor.schedule(task, delay, timeUnit);
    }
    
    /**
     * Schedule recurring task
     */
    public void scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit timeUnit) {
        scheduledExecutor.scheduleAtFixedRate(task, initialDelay, period, timeUnit);
    }
    
    /**
     * Execute task on main UI thread
     * Use this for UI updates from background threads
     */
    public void executeOnMainThread(Runnable task) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on main thread, execute immediately
            task.run();
        } else {
            // Post to main thread
            mainHandler.post(task);
        }
    }
    
    /**
     * Execute task on main thread with delay
     */
    public void executeOnMainThreadDelayed(Runnable task, long delayMillis) {
        mainHandler.postDelayed(task, delayMillis);
    }
    
    /**
     * Remove pending main thread task
     */
    public void removeMainThreadTask(Runnable task) {
        mainHandler.removeCallbacks(task);
    }
    
    /**
     * Get thread pool stats for monitoring
     */
    public ThreadPoolStats getStats() {
        ThreadPoolStats stats = new ThreadPoolStats();
        
        if (databaseExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor dbPool = (ThreadPoolExecutor) databaseExecutor;
            stats.databaseActiveThreads = dbPool.getActiveCount();
            stats.databaseQueueSize = dbPool.getQueue().size();
        }
        
        if (networkExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor netPool = (ThreadPoolExecutor) networkExecutor;
            stats.networkActiveThreads = netPool.getActiveCount();
            stats.networkQueueSize = netPool.getQueue().size();
        }
        
        if (backgroundExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor bgPool = (ThreadPoolExecutor) backgroundExecutor;
            stats.backgroundActiveThreads = bgPool.getActiveCount();
            stats.backgroundQueueSize = bgPool.getQueue().size();
        }
        
        if (scheduledExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor schedPool = (ThreadPoolExecutor) scheduledExecutor;
            stats.scheduledActiveThreads = schedPool.getActiveCount();
            stats.scheduledQueueSize = schedPool.getQueue().size();
        }
        
        return stats;
    }
    
    /**
     * Shutdown all thread pools (call this when app is destroyed)
     */
    public void shutdown() {
        try {
            databaseExecutor.shutdown();
            networkExecutor.shutdown();
            backgroundExecutor.shutdown();
            scheduledExecutor.shutdown();
            
            // Wait for threads to terminate
            if (!databaseExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                databaseExecutor.shutdownNow();
            }
            if (!networkExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                networkExecutor.shutdownNow();
            }
            if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                backgroundExecutor.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
            
        } catch (InterruptedException e) {
            // Restore interrupted status
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Thread pool statistics
     */
    public static class ThreadPoolStats {
        public int databaseActiveThreads = 0;
        public int databaseQueueSize = 0;
        public int networkActiveThreads = 0;
        public int networkQueueSize = 0;
        public int backgroundActiveThreads = 0;
        public int backgroundQueueSize = 0;
        public int scheduledActiveThreads = 0;
        public int scheduledQueueSize = 0;
        
        @Override
        public String toString() {
            return String.format(
                "ThreadPoolStats{DB: %d/%d, NET: %d/%d, BG: %d/%d, SCHED: %d/%d}",
                databaseActiveThreads, databaseQueueSize,
                networkActiveThreads, networkQueueSize,
                backgroundActiveThreads, backgroundQueueSize,
                scheduledActiveThreads, scheduledQueueSize
            );
        }
    }
}