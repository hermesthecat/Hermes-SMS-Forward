package com.keremgok.sms;

import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.annotation.RequiresApi;
import java.util.ArrayList;
import java.util.List;

/**
 * SimManager - Dual SIM Management Utility
 * Provides comprehensive dual SIM detection, configuration, and management
 * Supports Android 5.1+ (API 22+) dual SIM APIs with graceful degradation
 */
public class SimManager {
    
    private static final String TAG = "SimManager";
    private static final boolean DEBUG = true;
    
    // Cache to prevent infinite loops and improve performance
    private static List<SimInfo> cachedSimList = null;
    private static long lastCacheTime = 0;
    private static final long CACHE_DURATION_MS = 5000; // 5 seconds cache
    
    // Cache for dual SIM support check
    private static Boolean cachedDualSimSupported = null;
    private static long lastDualSimCheckTime = 0;
    
    /**
     * SimInfo data class - Contains detailed information about a SIM card
     */
    public static class SimInfo {
        public int subscriptionId;
        public int slotIndex;
        public String carrierName;
        public String displayName;
        public String phoneNumber;
        public boolean isActive;
        
        public SimInfo(int subscriptionId, int slotIndex, String carrierName, 
                      String displayName, String phoneNumber, boolean isActive) {
            this.subscriptionId = subscriptionId;
            this.slotIndex = slotIndex;
            this.carrierName = carrierName != null ? carrierName : "Unknown";
            this.displayName = displayName != null ? displayName : "SIM " + (slotIndex + 1);
            this.phoneNumber = phoneNumber != null ? phoneNumber : "Unknown";
            this.isActive = isActive;
        }
        
        @Override
        public String toString() {
            return displayName + " (" + carrierName + ")";
        }
    }
    
    /**
     * Check if the device supports dual SIM APIs
     * @return true if Android 5.1+ (API 22+) and dual SIM APIs are available
     */
    public static boolean isDualSimApiSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1;
    }
    
    /**
     * Check if dual SIM is supported on this device
     * @param context Application context
     * @return true if dual SIM is supported and available
     */
    public static boolean isDualSimSupported(Context context) {
        // Use cache to prevent excessive calls
        long currentTime = System.currentTimeMillis();
        if (cachedDualSimSupported != null && (currentTime - lastDualSimCheckTime) < CACHE_DURATION_MS) {
            logDebug("Returning cached dual SIM support: " + cachedDualSimSupported);
            return cachedDualSimSupported;
        }
        
        if (!isDualSimApiSupported()) {
            logDebug("Dual SIM APIs not supported - Android version < 5.1");
            cachedDualSimSupported = false;
            lastDualSimCheckTime = currentTime;
            return false;
        }
        
        try {
            // Check for required permissions first
            if (!hasRequiredPermissions(context)) {
                Log.w(TAG, "Required permissions not granted for dual SIM detection");
                cachedDualSimSupported = false;
                lastDualSimCheckTime = currentTime;
                return false;
            }
            
            List<SimInfo> sims = getActiveSimCards(context);
            boolean isDualSupported = sims.size() >= 2;
            logDebug("Dual SIM supported: " + isDualSupported + " (Found " + sims.size() + " SIMs)");
            SimLogger.logSimDetection(context, sims, isDualSupported);
            
            // Cache the result
            cachedDualSimSupported = isDualSupported;
            lastDualSimCheckTime = currentTime;
            
            return isDualSupported;
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied while checking dual SIM support: " + e.getMessage());
            cachedDualSimSupported = false;
            lastDualSimCheckTime = currentTime;
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking dual SIM support: " + e.getMessage(), e);
            cachedDualSimSupported = false;
            lastDualSimCheckTime = currentTime;
            return false;
        }
    }
    
    /**
     * Get list of active SIM cards on the device
     * @param context Application context
     * @return List of SimInfo objects representing active SIM cards
     */
    public static List<SimInfo> getActiveSimCards(Context context) {
        // Use cache to prevent excessive calls
        long currentTime = System.currentTimeMillis();
        if (cachedSimList != null && (currentTime - lastCacheTime) < CACHE_DURATION_MS) {
            logDebug("Returning cached SIM list (" + cachedSimList.size() + " SIMs)");
            return new ArrayList<>(cachedSimList);
        }
        
        List<SimInfo> simList = new ArrayList<>();
        
        if (!isDualSimApiSupported()) {
            logDebug("Dual SIM APIs not supported - returning single SIM fallback");
            // Fallback for older Android versions - assume single SIM
            simList.add(new SimInfo(-1, 0, "Default", "SIM 1", "Unknown", true));
            cachedSimList = new ArrayList<>(simList);
            lastCacheTime = currentTime;
            return simList;
        }
        
        try {
            SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
            if (subscriptionManager == null) {
                logDebug("SubscriptionManager not available");
                return simList;
            }
            
            List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
            if (subscriptionInfoList == null || subscriptionInfoList.isEmpty()) {
                logDebug("No active subscriptions found");
                return simList;
            }
            
            for (SubscriptionInfo subInfo : subscriptionInfoList) {
                SimInfo simInfo = createSimInfoFromSubscription(subInfo);
                simList.add(simInfo);
                logDebug("Found SIM: " + simInfo.toString() + " (Slot: " + simInfo.slotIndex + 
                        ", SubID: " + simInfo.subscriptionId + ")");
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "Permission denied accessing SIM information: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error getting active SIM cards: " + e.getMessage(), e);
        }
        
        // Update cache
        cachedSimList = new ArrayList<>(simList);
        lastCacheTime = currentTime;
        logDebug("Updated SIM cache with " + simList.size() + " SIMs");
        
        return simList;
    }
    
    /**
     * Create SimInfo object from SubscriptionInfo
     * @param subInfo SubscriptionInfo from Android system
     * @return SimInfo object with extracted information
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private static SimInfo createSimInfoFromSubscription(SubscriptionInfo subInfo) {
        int subscriptionId = subInfo.getSubscriptionId();
        int slotIndex = subInfo.getSimSlotIndex();
        String carrierName = subInfo.getCarrierName() != null ? subInfo.getCarrierName().toString() : null;
        String displayName = subInfo.getDisplayName() != null ? subInfo.getDisplayName().toString() : null;
        String phoneNumber = subInfo.getNumber();
        
        // Android 10+ additional data
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (displayName == null || displayName.trim().isEmpty()) {
                displayName = "SIM " + (slotIndex + 1);
            }
        }
        
        return new SimInfo(subscriptionId, slotIndex, carrierName, displayName, phoneNumber, true);
    }
    
    /**
     * Get SIM information by subscription ID
     * @param context Application context
     * @param subscriptionId The subscription ID to look up
     * @return SimInfo object or null if not found
     */
    public static SimInfo getSimInfo(Context context, int subscriptionId) {
        if (subscriptionId == -1) {
            logDebug("Invalid subscription ID: -1");
            return null;
        }
        
        List<SimInfo> sims = getActiveSimCards(context);
        for (SimInfo sim : sims) {
            if (sim.subscriptionId == subscriptionId) {
                logDebug("Found SIM info for subscription ID " + subscriptionId + ": " + sim.toString());
                return sim;
            }
        }
        
        logDebug("No SIM found for subscription ID: " + subscriptionId);
        return null;
    }
    
    /**
     * Get the default SMS subscription ID
     * @param context Application context
     * @return Default SMS subscription ID, or -1 if not available
     */
    public static int getDefaultSmsSubscriptionId(Context context) {
        if (!isDualSimApiSupported()) {
            logDebug("Dual SIM APIs not supported - returning -1 for default");
            return -1;
        }
        
        try {
            int defaultSubId = SubscriptionManager.getDefaultSmsSubscriptionId();
            logDebug("Default SMS subscription ID: " + defaultSubId);
            return defaultSubId;
        } catch (Exception e) {
            Log.e(TAG, "Error getting default SMS subscription ID: " + e.getMessage(), e);
            return -1;
        }
    }
    
    /**
     * Get subscription ID for a specific SIM slot
     * @param context Application context
     * @param slotIndex SIM slot index (0 for SIM1, 1 for SIM2)
     * @return Subscription ID for the slot, or -1 if not found
     */
    public static int getSubscriptionIdForSlot(Context context, int slotIndex) {
        List<SimInfo> sims = getActiveSimCards(context);
        for (SimInfo sim : sims) {
            if (sim.slotIndex == slotIndex) {
                logDebug("Subscription ID for slot " + slotIndex + ": " + sim.subscriptionId);
                return sim.subscriptionId;
            }
        }
        
        logDebug("No subscription found for slot: " + slotIndex);
        return -1;
    }
    
    /**
     * Get SIM slot index for a subscription ID
     * @param context Application context
     * @param subscriptionId The subscription ID
     * @return SIM slot index (0 for SIM1, 1 for SIM2), or -1 if not found
     */
    public static int getSlotIndexForSubscription(Context context, int subscriptionId) {
        SimInfo sim = getSimInfo(context, subscriptionId);
        if (sim != null) {
            logDebug("Slot index for subscription " + subscriptionId + ": " + sim.slotIndex);
            return sim.slotIndex;
        }
        
        logDebug("No slot found for subscription ID: " + subscriptionId);
        return -1;
    }
    
    /**
     * Check if a specific subscription ID is valid and active
     * @param context Application context
     * @param subscriptionId The subscription ID to validate
     * @return true if the subscription is valid and active
     */
    public static boolean isSubscriptionActive(Context context, int subscriptionId) {
        if (subscriptionId == -1) {
            return false;
        }
        
        SimInfo sim = getSimInfo(context, subscriptionId);
        boolean isActive = sim != null && sim.isActive;
        logDebug("Subscription " + subscriptionId + " active: " + isActive);
        return isActive;
    }
    
    /**
     * Get user-friendly name for a SIM slot
     * @param context Application context
     * @param slotIndex SIM slot index (0 for SIM1, 1 for SIM2)
     * @return User-friendly name like "SIM 1 (Vodafone)" or "SIM 1"
     */
    public static String getSimDisplayName(Context context, int slotIndex) {
        if (slotIndex == -1) {
            return "Auto";
        }
        
        int subscriptionId = getSubscriptionIdForSlot(context, slotIndex);
        if (subscriptionId != -1) {
            SimInfo sim = getSimInfo(context, subscriptionId);
            if (sim != null) {
                String displayName = "SIM " + (slotIndex + 1);
                if (sim.carrierName != null && !sim.carrierName.equals("Unknown")) {
                    displayName += " (" + sim.carrierName + ")";
                }
                return displayName;
            }
        }
        
        return "SIM " + (slotIndex + 1);
    }
    
    /**
     * Get detailed SIM status information for debugging
     * @param context Application context
     * @return Formatted string with SIM status information
     */
    public static String getSimStatusInfo(Context context) {
        StringBuilder status = new StringBuilder();
        status.append("=== SIM Status Information ===\n");
        status.append("Dual SIM API Supported: ").append(isDualSimApiSupported()).append("\n");
        status.append("Dual SIM Supported: ").append(isDualSimSupported(context)).append("\n");
        status.append("Default SMS Subscription: ").append(getDefaultSmsSubscriptionId(context)).append("\n");
        
        List<SimInfo> sims = getActiveSimCards(context);
        status.append("Active SIMs: ").append(sims.size()).append("\n");
        
        for (int i = 0; i < sims.size(); i++) {
            SimInfo sim = sims.get(i);
            status.append("SIM ").append(i + 1).append(":\n");
            status.append("  Slot: ").append(sim.slotIndex).append("\n");
            status.append("  Subscription ID: ").append(sim.subscriptionId).append("\n");
            status.append("  Carrier: ").append(sim.carrierName).append("\n");
            status.append("  Display Name: ").append(sim.displayName).append("\n");
            status.append("  Phone Number: ").append(maskPhoneNumber(sim.phoneNumber)).append("\n");
            status.append("  Active: ").append(sim.isActive).append("\n");
        }
        
        return status.toString();
    }
    
    /**
     * Mask phone number for secure logging
     * @param phoneNumber Phone number to mask
     * @return Masked phone number for logging
     */
    private static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "***";
        }
        return phoneNumber.substring(0, 2) + "***" + phoneNumber.substring(phoneNumber.length() - 2);
    }
    
    /**
     * Check if all required permissions are granted for dual SIM operations
     * @param context Application context
     * @return true if all required permissions are granted
     */
    public static boolean hasRequiredPermissions(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean hasReadPhoneState = context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) 
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
                boolean hasReadSms = context.checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) 
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
                boolean hasSendSms = context.checkSelfPermission(android.Manifest.permission.SEND_SMS) 
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
                
                logDebug("Permission check - READ_PHONE_STATE: " + hasReadPhoneState + 
                        ", RECEIVE_SMS: " + hasReadSms + ", SEND_SMS: " + hasSendSms);
                
                return hasReadPhoneState && hasReadSms && hasSendSms;
            } else {
                // Pre-Marshmallow - permissions granted at install time
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Check if a specific subscription ID is valid and active
     * @param context Application context
     * @param subscriptionId Subscription ID to validate
     * @return true if subscription is valid and active
     */
    public static boolean isSubscriptionValid(Context context, int subscriptionId) {
        if (subscriptionId == -1) {
            // -1 means default/unspecified subscription
            return true;
        }
        
        if (!isDualSimApiSupported()) {
            logDebug("Dual SIM APIs not supported - treating subscription as valid");
            return true;
        }
        
        try {
            if (!hasRequiredPermissions(context)) {
                Log.w(TAG, "Cannot validate subscription - missing permissions");
                return false;
            }
            
            List<SimInfo> activeSims = getActiveSimCards(context);
            for (SimInfo sim : activeSims) {
                if (sim.subscriptionId == subscriptionId && sim.isActive) {
                    logDebug("Subscription " + subscriptionId + " is valid and active");
                    return true;
                }
            }
            
            Log.w(TAG, "Subscription " + subscriptionId + " not found or inactive");
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error validating subscription " + subscriptionId + ": " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get fallback subscription ID when specified subscription is not available
     * @param context Application context
     * @param preferredSubscriptionId Preferred subscription ID
     * @return Valid fallback subscription ID, or -1 for default
     */
    public static int getFallbackSubscriptionId(Context context, int preferredSubscriptionId) {
        try {
            // If preferred subscription is valid, use it
            if (isSubscriptionValid(context, preferredSubscriptionId)) {
                return preferredSubscriptionId;
            }
            
            logDebug("Preferred subscription " + preferredSubscriptionId + " not available, finding fallback");
            
            // Get default SMS subscription as fallback
            int defaultSubscriptionId = getDefaultSmsSubscriptionId(context);
            if (isSubscriptionValid(context, defaultSubscriptionId)) {
                logDebug("Using default SMS subscription " + defaultSubscriptionId + " as fallback");
                return defaultSubscriptionId;
            }
            
            // If default not available, use first active subscription
            List<SimInfo> activeSims = getActiveSimCards(context);
            if (!activeSims.isEmpty()) {
                int fallbackId = activeSims.get(0).subscriptionId;
                logDebug("Using first active subscription " + fallbackId + " as fallback");
                return fallbackId;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error finding fallback subscription: " + e.getMessage(), e);
        }
        
        // Ultimate fallback - use default SIM
        logDebug("No valid subscriptions found, using default SIM (-1)");
        return -1;
    }
    
    /**
     * Handle SIM state changes (e.g., SIM removed/inserted)
     * @param context Application context
     * @param intent Intent containing SIM state change information
     */
    public static void handleSimStateChange(Context context, android.content.Intent intent) {
        try {
            String action = intent.getAction();
            if (action == null) return;
            
            logDebug("Handling SIM state change: " + action);
            
            switch (action) {
                case "android.intent.action.SIM_STATE_CHANGED":
                case "android.intent.action.SIM_APPLICATION_STATE_CHANGED":
                    // Get the affected slot
                    int slotId = intent.getIntExtra("android.telephony.extra.SLOT_INDEX", -1);
                    int simState = intent.getIntExtra("android.telephony.extra.SIM_STATE", -1);
                    
                    logDebug("SIM state change - Slot: " + slotId + ", State: " + simState);
                    
                    // Refresh SIM information
                    List<SimInfo> currentSims = getActiveSimCards(context);
                    logDebug("Current active SIMs after state change: " + currentSims.size());
                    
                    break;
                    
                default:
                    logDebug("Unhandled SIM state change action: " + action);
                    break;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error handling SIM state change: " + e.getMessage(), e);
        }
    }
    
    /**
     * Graceful degradation for single SIM devices
     * @param context Application context
     * @return SimInfo for single SIM device or null if no SIM
     */
    public static SimInfo getSingleSimFallback(Context context) {
        try {
            if (isDualSimSupported(context)) {
                // Not a single SIM device
                return null;
            }
            
            logDebug("Creating single SIM fallback info");
            
            // Get basic telephony manager info for single SIM
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null && hasRequiredPermissions(context)) {
                String carrierName = telephonyManager.getNetworkOperatorName();
                if (carrierName == null || carrierName.trim().isEmpty()) {
                    carrierName = "Unknown Carrier";
                }
                
                return new SimInfo(-1, 0, carrierName, "SIM 1", "Unknown", true);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating single SIM fallback: " + e.getMessage(), e);
        }
        
        // Ultimate fallback
        return new SimInfo(-1, 0, "Unknown", "SIM 1", "Unknown", true);
    }
    
    /**
     * Debug logging helper
     * @param message Message to log
     */
    private static void logDebug(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }
}