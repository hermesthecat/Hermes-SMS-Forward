package com.keremgok.sms;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

/**
 * SIM Selection Helper for Dual SIM Support
 * Handles the logic for determining which SIM to use for SMS forwarding
 * based on various selection modes and target configurations
 */
public class SmsSimSelectionHelper {
    
    private static final String TAG = "SmsSimSelectionHelper";
    private static final boolean DEBUG = true;
    private static final String PREFS_NAME = "HermesPrefs";
    
    /**
     * SIM Selection Result Class
     */
    public static class SimSelectionResult {
        private final int subscriptionId;
        private final int simSlot;
        private final String selectionReason;
        private final boolean isValid;
        
        public SimSelectionResult(int subscriptionId, int simSlot, String selectionReason, boolean isValid) {
            this.subscriptionId = subscriptionId;
            this.simSlot = simSlot;
            this.selectionReason = selectionReason;
            this.isValid = isValid;
        }
        
        public int getSubscriptionId() { return subscriptionId; }
        public int getSimSlot() { return simSlot; }
        public String getSelectionReason() { return selectionReason; }
        public boolean isValid() { return isValid; }
        
        @Override
        public String toString() {
            return String.format("SimSelectionResult{subscriptionId=%d, simSlot=%d, reason='%s', valid=%s}", 
                               subscriptionId, simSlot, selectionReason, isValid);
        }
    }
    
    /**
     * Determine which SIM to use for forwarding based on target configuration and source SIM
     * @param context Application context
     * @param targetNumber Target phone number being forwarded to
     * @param sourceSubscriptionId Subscription ID of the source SIM that received the SMS
     * @param targetConfig Target number configuration with SIM preferences
     * @return SimSelectionResult indicating which SIM to use and why
     */
    public static SimSelectionResult determineForwardingSim(Context context, String targetNumber, 
                                                          int sourceSubscriptionId, 
                                                          TargetNumber targetConfig) {
        try {
            logDebug("Determining forwarding SIM for target: " + maskPhoneNumber(targetNumber) + 
                    ", source subscription: " + sourceSubscriptionId);
            
            // Check if dual SIM is supported
            if (!SimManager.isDualSimSupported(context)) {
                logDebug("Dual SIM not supported, using default SIM");
                return new SimSelectionResult(-1, -1, "Single SIM device", true);
            }
            
            // Get the SIM selection mode from target configuration
            String simSelectionMode = targetConfig != null ? targetConfig.getSimSelectionMode() : "auto";
            if (TextUtils.isEmpty(simSelectionMode)) {
                simSelectionMode = "auto";
            }
            
            logDebug("Using SIM selection mode: " + simSelectionMode + " for target: " + maskPhoneNumber(targetNumber));
            
            // Apply the appropriate selection logic based on mode
            switch (simSelectionMode.toLowerCase()) {
                case "auto":
                    return handleAutoMode(context, sourceSubscriptionId);
                    
                case "source_sim":
                    return handleSourceSimMode(context, sourceSubscriptionId);
                    
                case "specific_sim":
                    return handleSpecificSimMode(context, targetConfig);
                    
                default:
                    Log.w(TAG, "Unknown SIM selection mode: " + simSelectionMode + ", falling back to auto");
                    return handleAutoMode(context, sourceSubscriptionId);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error determining forwarding SIM: " + e.getMessage(), e);
            // Fallback to default SIM on error with validation
            int fallbackSubscriptionId = SimManager.getFallbackSubscriptionId(context, -1);
            return new SimSelectionResult(fallbackSubscriptionId, -1, 
                "Error fallback: " + e.getMessage(), fallbackSubscriptionId != -1);
        }
    }
    
    /**
     * Handle Auto Mode: Use the default SMS SIM
     * @param context Application context
     * @param sourceSubscriptionId Source SIM subscription ID (for logging)
     * @return SimSelectionResult for auto mode
     */
    private static SimSelectionResult handleAutoMode(Context context, int sourceSubscriptionId) {
        try {
            // Get default SMS subscription ID
            int defaultSmsSubscriptionId = SimManager.getDefaultSmsSubscriptionId(context);
            
            if (defaultSmsSubscriptionId != -1) {
                SimManager.SimInfo simInfo = SimManager.getSimInfo(context, defaultSmsSubscriptionId);
                if (simInfo != null && simInfo.isActive) {
                    logDebug("Auto mode selected default SIM: " + simInfo.displayName + 
                            " (subscription " + defaultSmsSubscriptionId + ", slot " + simInfo.slotIndex + ")");
                    return new SimSelectionResult(defaultSmsSubscriptionId, simInfo.slotIndex, 
                                                "Auto mode - default SMS SIM", true);
                }
            }
            
            // Fallback: try to use any available active SIM
            java.util.List<SimManager.SimInfo> activeSims = SimManager.getActiveSimCards(context);
            if (activeSims != null && !activeSims.isEmpty()) {
                SimManager.SimInfo firstActiveSim = activeSims.get(0);
                logDebug("Auto mode fallback to first active SIM: " + firstActiveSim.displayName);
                return new SimSelectionResult(firstActiveSim.subscriptionId, firstActiveSim.slotIndex,
                                            "Auto mode - first active SIM fallback", true);
            }
            
            // No active SIMs available
            Log.w(TAG, "Auto mode: No active SIMs available, using default");
            return new SimSelectionResult(-1, -1, "Auto mode - no active SIMs", false);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in auto mode SIM selection: " + e.getMessage(), e);
            int fallbackSubscriptionId = SimManager.getFallbackSubscriptionId(context, -1);
            return new SimSelectionResult(fallbackSubscriptionId, -1, 
                "Auto mode error fallback: " + e.getMessage(), fallbackSubscriptionId != -1);
        }
    }
    
    /**
     * Handle Source SIM Mode: Use the same SIM that received the SMS
     * @param context Application context
     * @param sourceSubscriptionId Subscription ID of the source SIM
     * @return SimSelectionResult for source SIM mode
     */
    private static SimSelectionResult handleSourceSimMode(Context context, int sourceSubscriptionId) {
        try {
            if (sourceSubscriptionId == -1) {
                logDebug("Source SIM mode: No source subscription ID available, falling back to auto");
                return handleAutoMode(context, sourceSubscriptionId);
            }
            
            // Verify that the source SIM is still active
            SimManager.SimInfo sourceSimInfo = SimManager.getSimInfo(context, sourceSubscriptionId);
            if (sourceSimInfo != null && sourceSimInfo.isActive) {
                logDebug("Source SIM mode selected source SIM: " + sourceSimInfo.displayName + 
                        " (subscription " + sourceSubscriptionId + ", slot " + sourceSimInfo.slotIndex + ")");
                return new SimSelectionResult(sourceSubscriptionId, sourceSimInfo.slotIndex,
                                            "Source SIM mode - same as received", true);
            } else {
                Log.w(TAG, "Source SIM mode: Source SIM (subscription " + sourceSubscriptionId + 
                          ") is not available, falling back to auto");
                return handleAutoMode(context, sourceSubscriptionId);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in source SIM mode selection: " + e.getMessage(), e);
            return handleAutoMode(context, sourceSubscriptionId);
        }
    }
    
    /**
     * Handle Specific SIM Mode: Use the SIM specified in target configuration
     * @param context Application context
     * @param targetConfig Target number configuration
     * @return SimSelectionResult for specific SIM mode
     */
    private static SimSelectionResult handleSpecificSimMode(Context context, TargetNumber targetConfig) {
        try {
            if (targetConfig == null) {
                Log.w(TAG, "Specific SIM mode: No target configuration available, falling back to auto");
                return handleAutoMode(context, -1);
            }
            
            int preferredSimSlot = targetConfig.getPreferredSimSlot();
            
            if (preferredSimSlot == -1) {
                logDebug("Specific SIM mode: No preferred SIM slot specified, falling back to auto");
                return handleAutoMode(context, -1);
            }
            
            // Get subscription ID for the preferred slot
            int preferredSubscriptionId = SimManager.getSubscriptionIdForSlot(context, preferredSimSlot);
            
            if (preferredSubscriptionId != -1) {
                SimManager.SimInfo preferredSimInfo = SimManager.getSimInfo(context, preferredSubscriptionId);
                if (preferredSimInfo != null && preferredSimInfo.isActive) {
                    logDebug("Specific SIM mode selected preferred SIM: " + preferredSimInfo.displayName + 
                            " (subscription " + preferredSubscriptionId + ", slot " + preferredSimSlot + ")");
                    return new SimSelectionResult(preferredSubscriptionId, preferredSimSlot,
                                                "Specific SIM mode - slot " + preferredSimSlot, true);
                }
            }
            
            // Preferred SIM not available, fallback to auto
            Log.w(TAG, "Specific SIM mode: Preferred SIM slot " + preferredSimSlot + 
                      " is not available, falling back to auto");
            return handleAutoMode(context, -1);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in specific SIM mode selection: " + e.getMessage(), e);
            return handleAutoMode(context, -1);
        }
    }
    
    /**
     * Get global SIM selection preference from settings
     * @param context Application context
     * @return Global SIM selection mode preference
     */
    public static String getGlobalSimSelectionMode(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getString("pref_global_sim_mode", "auto");
        } catch (Exception e) {
            Log.e(TAG, "Error getting global SIM selection mode: " + e.getMessage(), e);
            return "auto";
        }
    }
    
    /**
     * Set global SIM selection preference in settings
     * @param context Application context
     * @param mode SIM selection mode ("auto", "source_sim", "specific_sim")
     */
    public static void setGlobalSimSelectionMode(Context context, String mode) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString("pref_global_sim_mode", mode).apply();
            logDebug("Global SIM selection mode set to: " + mode);
        } catch (Exception e) {
            Log.e(TAG, "Error setting global SIM selection mode: " + e.getMessage(), e);
        }
    }
    
    /**
     * Mask phone number for secure logging
     */
    private static String maskPhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length() < 8) {
            return "***";
        }
        
        String prefix = phoneNumber.substring(0, Math.min(5, phoneNumber.length() - 4));
        String suffix = phoneNumber.substring(phoneNumber.length() - 4);
        return prefix + "***" + suffix;
    }
    
    /**
     * Secure debug logging
     */
    private static void logDebug(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }
}