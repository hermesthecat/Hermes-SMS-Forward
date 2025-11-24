package com.keremgok.sms;

import android.app.Application;
import android.util.Log;

/**
 * Application class for Hermes SMS Forward
 * Manages app-wide initialization and cleanup
 */
public class HermesApplication extends Application {
    
    private static final String TAG = "HermesApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Hermes SMS Forward application starting");
        
        // Application initialization here if needed
        // ThreadManager, WorkManager, etc. are initialized lazily
    }
    
    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.i(TAG, "Hermes SMS Forward application terminating");
        
        // Clean up ThreadManager resources
        try {
            ThreadManager.getInstance().shutdown();
            Log.i(TAG, "ThreadManager shutdown completed");
        } catch (Exception e) {
            Log.e(TAG, "Error shutting down ThreadManager: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.w(TAG, "Low memory warning received");
        
        // Could trigger cache clearing or other memory optimizations here
        // For now, just log the warning
    }
    
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        
        switch (level) {
            case TRIM_MEMORY_RUNNING_CRITICAL:
                Log.w(TAG, "Memory trim: RUNNING_CRITICAL");
                break;
            case TRIM_MEMORY_RUNNING_LOW:
                Log.w(TAG, "Memory trim: RUNNING_LOW");
                break;
            case TRIM_MEMORY_RUNNING_MODERATE:
                Log.i(TAG, "Memory trim: RUNNING_MODERATE");
                break;
            case TRIM_MEMORY_UI_HIDDEN:
                Log.i(TAG, "Memory trim: UI_HIDDEN");
                break;
            case TRIM_MEMORY_BACKGROUND:
                Log.i(TAG, "Memory trim: BACKGROUND");
                break;
            case TRIM_MEMORY_MODERATE:
                Log.w(TAG, "Memory trim: MODERATE");
                break;
            case TRIM_MEMORY_COMPLETE:
                Log.w(TAG, "Memory trim: COMPLETE");
                // Clear non-essential caches
                SimManager.clearCache();
                break;
        }
    }
}
