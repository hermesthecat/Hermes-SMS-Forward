package com.keremgok.sms.remote;

import android.content.Context;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.util.Log;

import com.keremgok.sms.AppDatabase;
import com.keremgok.sms.SimManager;
import com.keremgok.sms.ThreadManager;

import java.util.List;

/**
 * Executor for remote SMS commands
 * Handles actual SMS sending with SIM selection
 */
public class RemoteCommandExecutor {
    
    private static final String TAG = "RemoteCommandExecutor";
    
    /**
     * Execution result
     */
    public static class ExecutionResult {
        public boolean success;
        public String message;
        public int simSlotUsed;
        
        public static ExecutionResult success(int simSlot) {
            ExecutionResult result = new ExecutionResult();
            result.success = true;
            result.simSlotUsed = simSlot;
            result.message = "SMS sent successfully via SIM" + (simSlot + 1);
            return result;
        }
        
        public static ExecutionResult failure(String error) {
            ExecutionResult result = new ExecutionResult();
            result.success = false;
            result.simSlotUsed = -1;
            result.message = error;
            return result;
        }
    }
    
    /**
     * Execute a parsed command
     */
    public static void executeCommand(Context context, RemoteCommandHistory history, 
                                     RemoteCommandProcessor.ParsedCommand parsed) {
        ThreadManager.getInstance().executeBackground(() -> {
            try {
                Log.i(TAG, "Executing command ID: " + history.getId());
                
                // Mark as executing
                history.markExecuting();
                updateHistory(context, history);
                
                // Select SIM
                int simSlot = selectSim(context, parsed.simMode);
                if (simSlot < 0) {
                    history.markFailed("No suitable SIM card found");
                    updateHistory(context, history);
                    sendResponseSms(context, history.getSenderNumber(), false, history.getResultMessage());
                    return;
                }
                
                // Send SMS
                ExecutionResult result = sendSms(context, parsed.targetNumber, parsed.messageContent, simSlot);
                
                if (result.success) {
                    history.markSuccess("SMS sent successfully via SIM" + (simSlot + 1));
                    Log.i(TAG, "Command executed successfully: " + history.getId());
                } else {
                    history.markFailed(result.message);
                    Log.e(TAG, "Command execution failed: " + result.message);
                }
                
                updateHistory(context, history);
                
                // Send response SMS
                sendResponseSms(context, history.getSenderNumber(), result.success, history.getResultMessage());
                
                // Update authorized number stats
                if (result.success) {
                    RemoteCommandValidator.updateLastUsed(context, history.getSenderNumber());
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Execution error: " + e.getMessage(), e);
                history.markFailed("Execution error: " + e.getMessage());
                updateHistory(context, history);
                sendResponseSms(context, history.getSenderNumber(), false, "Execution error");
            }
        });
    }
    
    /**
     * Select SIM card based on mode
     * Returns slot index (0 for SIM1, 1 for SIM2)
     */
    private static int selectSim(Context context, String simMode) {
        if ("SIM2".equals(simMode)) {
            return 1;
        }
        // Default to slot 0 for SIM1 and AUTO
        return 0;
    }
    
    /**
     * Send SMS using SmsManager
     */
    private static ExecutionResult sendSms(Context context, String targetNumber, String message, int simSlot) {
        try {
            // Use default SmsManager (will use default SIM)
            SmsManager smsManager = SmsManager.getDefault();
            
            // Split message if needed
            if (message.length() > 160) {
                smsManager.sendMultipartTextMessage(
                    targetNumber,
                    null,
                    smsManager.divideMessage(message),
                    null,
                    null
                );
            } else {
                smsManager.sendTextMessage(targetNumber, null, message, null, null);
            }
            
            Log.i(TAG, "SMS sent successfully to " + maskNumber(targetNumber) + " via slot " + simSlot);
            return ExecutionResult.success(simSlot);
            
        } catch (Exception e) {
            Log.e(TAG, "SMS sending failed: " + e.getMessage(), e);
            return ExecutionResult.failure("SMS sending failed: " + e.getMessage());
        }
    }
    
    /**
     * Send response SMS to command sender
     */
    private static void sendResponseSms(Context context, String senderNumber, boolean success, String message) {
        try {
            if (!RemoteCommandValidator.shouldSendResponseSms(context)) {
                return; // Response SMS disabled
            }
            
            String responseText;
            if (success) {
                responseText = "✅ SMS sent successfully\n" + message;
            } else {
                responseText = "❌ SMS sending failed\n" + message;
            }
            
            SmsManager.getDefault().sendTextMessage(senderNumber, null, responseText, null, null);
            Log.i(TAG, "Response SMS sent to " + maskNumber(senderNumber));
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to send response SMS: " + e.getMessage(), e);
        }
    }
    
    /**
     * Update command history in database
     */
    private static void updateHistory(Context context, RemoteCommandHistory history) {
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            if (db != null && db.remoteCommandHistoryDao() != null) {
                db.remoteCommandHistoryDao().update(history);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to update history: " + e.getMessage(), e);
        }
    }
    
    /**
     * Mask phone number for logging
     */
    private static String maskNumber(String number) {
        if (number == null || number.length() < 4) return "***";
        return number.substring(0, 4) + "***" + number.substring(number.length() - 3);
    }
}
