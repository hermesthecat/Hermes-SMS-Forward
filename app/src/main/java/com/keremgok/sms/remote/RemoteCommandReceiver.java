package com.keremgok.sms.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.keremgok.sms.AppDatabase;
import com.keremgok.sms.StatisticsManager;
import com.keremgok.sms.ThreadManager;

/**
 * Broadcast receiver for remote SMS commands
 * Listens for SMS messages containing remote control commands
 */
public class RemoteCommandReceiver extends BroadcastReceiver {
    
    private static final String TAG = "RemoteCommandReceiver";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            return;
        }
        
        if (!SMS_RECEIVED.equals(intent.getAction())) {
            return;
        }
        
        try {
            // Check if remote control is enabled
            if (!RemoteCommandValidator.isEnabled(context)) {
                return; // Feature disabled, ignore
            }
            
            // Extract SMS messages
            Bundle bundle = intent.getExtras();
            if (bundle == null) {
                return;
            }
            
            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null || pdus.length == 0) {
                return;
            }
            
            // Parse SMS messages
            StringBuilder fullMessage = new StringBuilder();
            String senderNumber = null;
            
            for (Object pdu : pdus) {
                SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                if (smsMessage != null) {
                    if (senderNumber == null) {
                        senderNumber = smsMessage.getOriginatingAddress();
                    }
                    String body = smsMessage.getMessageBody();
                    if (body != null) {
                        fullMessage.append(body);
                    }
                }
            }
            
            String messageBody = fullMessage.toString();
            
            // Check if this is a remote command
            if (!RemoteCommandProcessor.isRemoteCommand(messageBody)) {
                return; // Not a command, let other receivers handle it
            }
            
            Log.i(TAG, "Remote command received from: " + maskNumber(senderNumber));
            
            // Process command in background
            String finalSender = senderNumber;
            ThreadManager.getInstance().executeBackground(() -> {
                processCommand(context, finalSender, messageBody);
            });
            
            // Track in statistics
            StatisticsManager.getInstance(context).recordEvent(
                "REMOTE_COMMAND_RECEIVED",
                "REMOTE_CONTROL",
                "COMMAND_RECEIVED",
                0,
                null,
                null,
                null
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing SMS: " + e.getMessage(), e);
        }
    }
    
    /**
     * Process a remote command
     */
    private void processCommand(Context context, String senderNumber, String messageBody) {
        try {
            // Create history entry
            RemoteCommandHistory history = RemoteCommandHistory.create(senderNumber, messageBody);
            
            AppDatabase db = AppDatabase.getInstance(context);
            if (db == null || db.remoteCommandHistoryDao() == null) {
                Log.e(TAG, "Database unavailable");
                return;
            }
            
            long historyId = db.remoteCommandHistoryDao().insert(history);
            history.setId((int) historyId);
            
            // Validate sender
            RemoteCommandValidator.ValidationResult validation = 
                RemoteCommandValidator.validateSender(context, senderNumber);
            
            if (!validation.isAuthorized) {
                Log.w(TAG, "Unauthorized sender: " + validation.reason);
                history.markUnauthorized(validation.reason);
                db.remoteCommandHistoryDao().update(history);
                
                // Send response SMS
                if (RemoteCommandValidator.shouldSendResponseSms(context)) {
                    sendUnauthorizedResponse(context, senderNumber, validation.reason);
                }
                
                // Track in statistics
                StatisticsManager.getInstance(context).recordEvent(
                    "REMOTE_COMMAND_UNAUTHORIZED",
                    "REMOTE_CONTROL",
                    "UNAUTHORIZED",
                    0,
                    null,
                    null,
                    null
                );
                
                return;
            }
            
            // Parse command
            RemoteCommandProcessor.ParsedCommand parsed = 
                RemoteCommandProcessor.parseCommand(messageBody);
            
            if (!parsed.isValid) {
                Log.w(TAG, "Invalid command format: " + parsed.errorMessage);
                history.markInvalidFormat(parsed.errorMessage);
                db.remoteCommandHistoryDao().update(history);
                
                // Send response SMS
                if (RemoteCommandValidator.shouldSendResponseSms(context)) {
                    sendInvalidFormatResponse(context, senderNumber, parsed.errorMessage);
                }
                
                return;
            }
            
            // Update history with parsed data
            history.markAuthorized(parsed.simMode, parsed.targetNumber, parsed.messageContent);
            db.remoteCommandHistoryDao().update(history);
            
            Log.i(TAG, "Command authorized and parsed: " + parsed);
            
            // Check security mode
            String securityMode = RemoteCommandValidator.getSecurityMode(context);
            
            if (RemoteCommandValidator.SECURITY_MODE_IMMEDIATE.equals(securityMode)) {
                // Execute immediately
                RemoteCommandExecutor.executeCommand(context, history, parsed);
            } else {
                // TODO: Show confirmation notification (Phase 5)
                // For now, execute immediately
                Log.i(TAG, "Confirmation mode not yet implemented, executing immediately");
                RemoteCommandExecutor.executeCommand(context, history, parsed);
            }
            
            // Track in statistics
            StatisticsManager.getInstance(context).recordEvent(
                "REMOTE_COMMAND_AUTHORIZED",
                "REMOTE_CONTROL",
                "AUTHORIZED",
                0,
                null,
                null,
                null
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Command processing error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send unauthorized response SMS
     */
    private void sendUnauthorizedResponse(Context context, String senderNumber, String reason) {
        try {
            android.telephony.SmsManager.getDefault().sendTextMessage(
                senderNumber,
                null,
                "⚠️ Unauthorized\n" + reason,
                null,
                null
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to send unauthorized response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send invalid format response SMS
     */
    private void sendInvalidFormatResponse(Context context, String senderNumber, String errorMessage) {
        try {
            String helpText = RemoteCommandProcessor.getHelpText();
            android.telephony.SmsManager.getDefault().sendTextMessage(
                senderNumber,
                null,
                "❌ Invalid Command\n" + errorMessage + "\n\n" + helpText,
                null,
                null
            );
        } catch (Exception e) {
            Log.e(TAG, "Failed to send invalid format response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Mask phone number for logging
     */
    private String maskNumber(String number) {
        if (number == null || number.length() < 4) return "***";
        return number.substring(0, 4) + "***" + (number.length() > 7 ? number.substring(number.length() - 3) : "");
    }
}
