package com.keremgok.sms;

import android.content.Context;
import android.util.Log;

/**
 * SimLogger - Specialized logging utility for dual SIM operations
 * Provides secure, structured logging with debug/production awareness
 * Automatically masks sensitive information in production builds
 */
public class SimLogger {
    
    private static final String TAG_PREFIX = "SimLogger";
    
    // Log levels
    public static final int LEVEL_DEBUG = 0;
    public static final int LEVEL_INFO = 1;
    public static final int LEVEL_WARN = 2;
    public static final int LEVEL_ERROR = 3;
    
    // Enable debug mode based on build variant
    
    /**
     * Log SIM operation with detailed information
     * @param operation The operation being performed (e.g., "SEND_SMS", "SIM_DETECTION")
     * @param sourceSubscriptionId Source SIM subscription ID
     * @param targetSubscriptionId Target SIM subscription ID  
     * @param message Additional message
     * @param level Log level
     */
    public static void logSimOperation(String operation, int sourceSubscriptionId, 
                                     int targetSubscriptionId, String message, int level) {
        String tag = TAG_PREFIX + "_" + operation;
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[SIM_OP] ").append(operation).append(" | ");
        
        if (BuildConfig.ENABLE_DEBUG_LOGS) {
            // Debug build - show detailed SIM information
            logMessage.append("SRC_SUB:").append(sourceSubscriptionId).append(" | ");
            logMessage.append("TGT_SUB:").append(targetSubscriptionId).append(" | ");
        } else {
            // Production build - mask SIM IDs but show slot info
            logMessage.append("SRC_SLOT:").append(subscriptionToSlotInfo(sourceSubscriptionId)).append(" | ");
            logMessage.append("TGT_SLOT:").append(subscriptionToSlotInfo(targetSubscriptionId)).append(" | ");
        }
        
        logMessage.append("MSG:").append(message);
        
        writeLog(tag, logMessage.toString(), level);
    }
    
    /**
     * Log SIM detection and configuration
     * @param context Application context
     * @param sims List of detected SIMs
     * @param isDualSupported Whether dual SIM is supported
     */
    public static void logSimDetection(Context context, java.util.List<SimManager.SimInfo> sims, boolean isDualSupported) {
        String tag = TAG_PREFIX + "_DETECTION";
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[SIM_DETECT] Found ").append(sims.size()).append(" SIM(s) | ");
        logMessage.append("DUAL_SUPPORT:").append(isDualSupported).append(" | ");
        logMessage.append("DEFAULT_SUB:").append(maskSubscriptionId(SimManager.getDefaultSmsSubscriptionId(context)));
        
        writeLog(tag, logMessage.toString(), LEVEL_INFO);
        
        // Log each SIM with appropriate masking
        for (int i = 0; i < sims.size(); i++) {
            SimManager.SimInfo sim = sims.get(i);
            StringBuilder simDetails = new StringBuilder();
            simDetails.append("[SIM_").append(i + 1).append("] ");
            simDetails.append("SLOT:").append(sim.slotIndex).append(" | ");
            
            if (BuildConfig.ENABLE_DEBUG_LOGS) {
                simDetails.append("SUB_ID:").append(sim.subscriptionId).append(" | ");
                simDetails.append("CARRIER:").append(sim.carrierName).append(" | ");
                simDetails.append("NUMBER:").append(maskPhoneNumber(sim.phoneNumber)).append(" | ");
            } else {
                simDetails.append("CARRIER:").append(sim.carrierName).append(" | ");
            }
            
            simDetails.append("ACTIVE:").append(sim.isActive);
            
            writeLog(tag, simDetails.toString(), LEVEL_DEBUG);
        }
    }
    
    /**
     * Log SIM selection process
     * @param targetNumber Target phone number
     * @param selectionMode SIM selection mode (auto, source_sim, specific_sim)
     * @param sourceSubscriptionId Source SIM subscription ID
     * @param selectedSubscriptionId Selected SIM subscription ID
     * @param reason Selection reason
     */
    public static void logSimSelection(String targetNumber, String selectionMode, 
                                     int sourceSubscriptionId, int selectedSubscriptionId, String reason) {
        String tag = TAG_PREFIX + "_SELECTION";
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[SIM_SELECT] TARGET:").append(maskPhoneNumber(targetNumber)).append(" | ");
        logMessage.append("MODE:").append(selectionMode).append(" | ");
        
        if (BuildConfig.ENABLE_DEBUG_LOGS) {
            logMessage.append("SRC_SUB:").append(sourceSubscriptionId).append(" | ");
            logMessage.append("SEL_SUB:").append(selectedSubscriptionId).append(" | ");
        } else {
            logMessage.append("SRC_SLOT:").append(subscriptionToSlotInfo(sourceSubscriptionId)).append(" | ");
            logMessage.append("SEL_SLOT:").append(subscriptionToSlotInfo(selectedSubscriptionId)).append(" | ");
        }
        
        logMessage.append("REASON:").append(reason);
        
        writeLog(tag, logMessage.toString(), LEVEL_INFO);
    }
    
    /**
     * Log SIM error conditions
     * @param operation The operation that failed
     * @param subscriptionId Subscription ID involved
     * @param errorMessage Error message
     * @param exception Exception if available
     */
    public static void logSimError(String operation, int subscriptionId, String errorMessage, Throwable exception) {
        String tag = TAG_PREFIX + "_ERROR";
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[SIM_ERROR] OP:").append(operation).append(" | ");
        
        if (BuildConfig.ENABLE_DEBUG_LOGS) {
            logMessage.append("SUB_ID:").append(subscriptionId).append(" | ");
        } else {
            logMessage.append("SLOT:").append(subscriptionToSlotInfo(subscriptionId)).append(" | ");
        }
        
        logMessage.append("ERROR:").append(errorMessage);
        
        if (exception != null) {
            if (BuildConfig.ENABLE_DEBUG_LOGS) {
                Log.e(tag, logMessage.toString(), exception);
            } else {
                logMessage.append(" | EXCEPTION:").append(exception.getClass().getSimpleName());
                writeLog(tag, logMessage.toString(), LEVEL_ERROR);
            }
        } else {
            writeLog(tag, logMessage.toString(), LEVEL_ERROR);
        }
    }
    
    /**
     * Log SMS forwarding with SIM information
     * @param originalSender Original SMS sender
     * @param targetNumber Forward target number
     * @param sourceSimSlot Source SIM slot
     * @param forwardingSimSlot Forwarding SIM slot
     * @param success Whether forwarding was successful
     * @param processingTimeMs Processing time in milliseconds
     */
    public static void logSmsForwarding(String originalSender, String targetNumber, 
                                      int sourceSimSlot, int forwardingSimSlot, 
                                      boolean success, long processingTimeMs) {
        String tag = TAG_PREFIX + "_FORWARD";
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[SMS_FWD] FROM:").append(maskPhoneNumber(originalSender)).append(" | ");
        logMessage.append("TO:").append(maskPhoneNumber(targetNumber)).append(" | ");
        logMessage.append("SRC_SLOT:").append(sourceSimSlot).append(" | ");
        logMessage.append("FWD_SLOT:").append(forwardingSimSlot).append(" | ");
        logMessage.append("SUCCESS:").append(success).append(" | ");
        logMessage.append("TIME:").append(processingTimeMs).append("ms");
        
        int level = success ? LEVEL_INFO : LEVEL_WARN;
        writeLog(tag, logMessage.toString(), level);
    }
    
    /**
     * Log SIM state changes
     * @param action Intent action
     * @param slotId Affected SIM slot
     * @param newState New SIM state
     * @param subscriptionId Affected subscription ID
     */
    public static void logSimStateChange(String action, int slotId, int newState, int subscriptionId) {
        String tag = TAG_PREFIX + "_STATE";
        
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("[SIM_STATE] ACTION:").append(action).append(" | ");
        logMessage.append("SLOT:").append(slotId).append(" | ");
        logMessage.append("STATE:").append(simStateToString(newState)).append(" | ");
        
        if (BuildConfig.ENABLE_DEBUG_LOGS) {
            logMessage.append("SUB_ID:").append(subscriptionId);
        } else {
            logMessage.append("SUB_MASKED:").append(subscriptionId != -1 ? "***" : "-1");
        }
        
        writeLog(tag, logMessage.toString(), LEVEL_INFO);
    }
    
    /**
     * Log comprehensive SIM system status (debug builds only)
     * @param context Application context
     */
    public static void logSystemStatus(Context context) {
        if (!BuildConfig.ENABLE_DEBUG_LOGS) {
            return; // Only log detailed system status in debug builds
        }
        
        String tag = TAG_PREFIX + "_SYSTEM";
        
        try {
            writeLog(tag, "[SYSTEM_STATUS] === SIM System Status ===", LEVEL_DEBUG);
            writeLog(tag, "[SYSTEM_STATUS] API_SUPPORT:" + SimManager.isDualSimApiSupported(), LEVEL_DEBUG);
            writeLog(tag, "[SYSTEM_STATUS] DUAL_SIM:" + SimManager.isDualSimSupported(context), LEVEL_DEBUG);
            writeLog(tag, "[SYSTEM_STATUS] PERMISSIONS:" + SimManager.hasRequiredPermissions(context), LEVEL_DEBUG);
            writeLog(tag, "[SYSTEM_STATUS] DEFAULT_SUB:" + SimManager.getDefaultSmsSubscriptionId(context), LEVEL_DEBUG);
            
            java.util.List<SimManager.SimInfo> sims = SimManager.getActiveSimCards(context);
            writeLog(tag, "[SYSTEM_STATUS] ACTIVE_SIMS:" + sims.size(), LEVEL_DEBUG);
            
            for (int i = 0; i < sims.size(); i++) {
                SimManager.SimInfo sim = sims.get(i);
                String simStatus = String.format("[SIM_%d] SLOT:%d SUB:%d CARRIER:%s ACTIVE:%s", 
                    i + 1, sim.slotIndex, sim.subscriptionId, sim.carrierName, sim.isActive);
                writeLog(tag, "[SYSTEM_STATUS] " + simStatus, LEVEL_DEBUG);
            }
            
        } catch (Exception e) {
            writeLog(tag, "[SYSTEM_STATUS] ERROR:" + e.getMessage(), LEVEL_ERROR);
        }
    }
    
    /**
     * Write log message with appropriate level
     * @param tag Log tag
     * @param message Log message
     * @param level Log level
     */
    private static void writeLog(String tag, String message, int level) {
        switch (level) {
            case LEVEL_DEBUG:
                if (BuildConfig.ENABLE_DEBUG_LOGS) {
                    Log.d(tag, message);
                }
                break;
            case LEVEL_INFO:
                Log.i(tag, message);
                break;
            case LEVEL_WARN:
                Log.w(tag, message);
                break;
            case LEVEL_ERROR:
                Log.e(tag, message);
                break;
        }
    }
    
    /**
     * Mask phone number for secure logging
     * @param phoneNumber Phone number to mask
     * @return Masked phone number
     */
    private static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "***";
        }
        
        if (BuildConfig.ENABLE_DEBUG_LOGS) {
            // Debug build - show first 3 and last 2 digits
            if (phoneNumber.length() > 5) {
                return phoneNumber.substring(0, 3) + "***" + phoneNumber.substring(phoneNumber.length() - 2);
            }
        }
        
        // Production build - minimal information
        return "***" + phoneNumber.substring(phoneNumber.length() - 2);
    }
    
    /**
     * Mask subscription ID for production logging
     * @param subscriptionId Subscription ID to mask
     * @return Masked or original subscription ID based on build type
     */
    private static String maskSubscriptionId(int subscriptionId) {
        if (BuildConfig.ENABLE_DEBUG_LOGS) {
            return String.valueOf(subscriptionId);
        } else {
            return subscriptionId == -1 ? "-1" : "***";
        }
    }
    
    /**
     * Convert subscription ID to slot information for production logging
     * @param subscriptionId Subscription ID
     * @return Slot information string
     */
    private static String subscriptionToSlotInfo(int subscriptionId) {
        if (subscriptionId == -1) {
            return "DEFAULT";
        } else {
            return "SLOT_" + (subscriptionId % 2); // Simple slot estimation
        }
    }
    
    /**
     * Convert SIM state integer to readable string
     * @param simState SIM state integer
     * @return Human-readable SIM state
     */
    private static String simStateToString(int simState) {
        switch (simState) {
            case 0: return "UNKNOWN";
            case 1: return "ABSENT";
            case 2: return "PIN_REQUIRED";
            case 3: return "PUK_REQUIRED";
            case 4: return "NETWORK_LOCKED";
            case 5: return "READY";
            case 6: return "NOT_READY";
            case 7: return "PERM_DISABLED";
            case 8: return "CARD_IO_ERROR";
            case 9: return "CARD_RESTRICTED";
            case 10: return "LOADED";
            case 11: return "PRESENT";
            default: return "STATE_" + simState;
        }
    }
}