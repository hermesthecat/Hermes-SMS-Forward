package com.keremgok.sms;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

/**
 * BroadcastReceiver for handling SMS sent and delivered confirmations
 * Updates the SMS history database with success/failure status
 */
public class SmsCallbackReceiver extends BroadcastReceiver {
    
    private static final String TAG = "HermesSmsCallback";
    private static final String ACTION_SMS_SENT = "SMS_SENT";
    private static final String ACTION_SMS_DELIVERED = "SMS_DELIVERED";
    private static final boolean DEBUG = true;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (ACTION_SMS_SENT.equals(action)) {
            handleSmsSent(context, intent);
        } else if (ACTION_SMS_DELIVERED.equals(action)) {
            handleSmsDelivered(context, intent);
        }
    }
    
    /**
     * Handle SMS sent confirmation
     * Updates database with success/failure status based on result code
     */
    private void handleSmsSent(Context context, Intent intent) {
        // Extract data from intent
        String originalSender = intent.getStringExtra("originalSender");
        String originalMessage = intent.getStringExtra("originalMessage");
        String forwardedMessage = intent.getStringExtra("forwardedMessage");
        String targetNumber = intent.getStringExtra("targetNumber");
        long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());
        int retryCount = intent.getIntExtra("retryCount", 0);
        boolean isMultipart = intent.getBooleanExtra("isMultipart", false);
        
        int resultCode = getResultCode();
        boolean success = (resultCode == Activity.RESULT_OK);
        String errorMessage = "";
        
        // Determine error message based on result code
        switch (resultCode) {
            case Activity.RESULT_OK:
                if (DEBUG) {
                    Log.d(TAG, "SMS sent successfully to " + maskPhoneNumber(targetNumber));
                }
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                errorMessage = "Generic SMS failure";
                Log.e(TAG, "SMS generic failure to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                errorMessage = "No SMS service available";
                Log.e(TAG, "SMS no service to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                errorMessage = "SMS PDU null error";
                Log.e(TAG, "SMS null PDU to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                errorMessage = "Radio turned off";
                Log.e(TAG, "SMS radio off to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_ERROR_LIMIT_EXCEEDED:
                errorMessage = "SMS limit exceeded";
                Log.e(TAG, "SMS limit exceeded to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE:
                errorMessage = "FDN check failure";
                Log.e(TAG, "SMS FDN failure to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED:
                errorMessage = "Short code not allowed";
                Log.e(TAG, "SMS short code error to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED:
                errorMessage = "Short code never allowed";
                Log.e(TAG, "SMS short code never allowed to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RADIO_NOT_AVAILABLE:
                errorMessage = "Radio not available";
                Log.e(TAG, "SMS radio not available to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_NETWORK_REJECT:
                errorMessage = "Network rejected SMS";
                Log.e(TAG, "SMS network reject to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_INVALID_ARGUMENTS:
                errorMessage = "Invalid SMS arguments";
                Log.e(TAG, "SMS invalid arguments to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_INVALID_STATE:
                errorMessage = "Invalid SMS state";
                Log.e(TAG, "SMS invalid state to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_NO_MEMORY:
                errorMessage = "No memory for SMS";
                Log.e(TAG, "SMS no memory to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_REQUEST_NOT_SUPPORTED:
                errorMessage = "SMS request not supported";
                Log.e(TAG, "SMS not supported to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_OPERATION_NOT_ALLOWED:
                errorMessage = "SMS operation not allowed";
                Log.e(TAG, "SMS operation not allowed to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_INTERNAL_ERROR:
                errorMessage = "Internal SMS error";
                Log.e(TAG, "SMS internal error to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_SYSTEM_ERROR:
                errorMessage = "System SMS error";
                Log.e(TAG, "SMS system error to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_ENCODING_ERROR:
                errorMessage = "SMS encoding error";
                Log.e(TAG, "SMS encoding error to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_INVALID_SMSC_ADDRESS:
                errorMessage = "Invalid SMSC address";
                Log.e(TAG, "SMS invalid SMSC to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_INVALID_SMS_FORMAT:
                errorMessage = "Invalid SMS format";
                Log.e(TAG, "SMS invalid format to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_UNEXPECTED_EVENT_STOP_SENDING:
                errorMessage = "Unexpected event, stopped sending";
                Log.e(TAG, "SMS unexpected stop to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_SMS_BLOCKED_DURING_EMERGENCY:
                errorMessage = "SMS blocked during emergency";
                Log.e(TAG, "SMS blocked emergency to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_SMS_SEND_RETRY_FAILED:
                errorMessage = "SMS retry failed";
                Log.e(TAG, "SMS retry failed to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_REMOTE_EXCEPTION:
                errorMessage = "Remote exception";
                Log.e(TAG, "SMS remote exception to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_NO_DEFAULT_SMS_APP:
                errorMessage = "No default SMS app";
                Log.e(TAG, "SMS no default app to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RIL_RADIO_NOT_AVAILABLE:
                errorMessage = "RIL radio not available";
                Log.e(TAG, "SMS RIL not available to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RIL_SMS_SEND_FAIL_RETRY:
                errorMessage = "RIL SMS send failed, retry";
                Log.e(TAG, "SMS RIL retry to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RIL_NETWORK_REJECT:
                errorMessage = "RIL network rejected";
                Log.e(TAG, "SMS RIL network reject to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RIL_INVALID_STATE:
                errorMessage = "RIL invalid state";
                Log.e(TAG, "SMS RIL invalid state to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RIL_INVALID_ARGUMENTS:
                errorMessage = "RIL invalid arguments";
                Log.e(TAG, "SMS RIL invalid args to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RIL_NO_MEMORY:
                errorMessage = "RIL no memory";
                Log.e(TAG, "SMS RIL no memory to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RIL_REQUEST_NOT_SUPPORTED:
                errorMessage = "RIL request not supported";
                Log.e(TAG, "SMS RIL not supported to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RIL_OPERATION_NOT_ALLOWED:
                errorMessage = "RIL operation not allowed";
                Log.e(TAG, "SMS RIL not allowed to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RIL_ENCODING_ERR:
                errorMessage = "RIL encoding error";
                Log.e(TAG, "SMS RIL encoding error to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RIL_INVALID_SMSC_ADDRESS:
                errorMessage = "RIL invalid SMSC address";
                Log.e(TAG, "SMS RIL invalid SMSC to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RIL_MODEM_ERR:
                errorMessage = "RIL modem error";
                Log.e(TAG, "SMS RIL modem error to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RIL_NETWORK_ERR:
                errorMessage = "RIL network error";
                Log.e(TAG, "SMS RIL network error to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RIL_INTERNAL_ERR:
                errorMessage = "RIL internal error";
                Log.e(TAG, "SMS RIL internal error to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RIL_REQUEST_RATE_LIMITED:
                errorMessage = "RIL request rate limited";
                Log.e(TAG, "SMS RIL rate limited to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RIL_INVALID_SMS_FORMAT:
                errorMessage = "RIL invalid SMS format";
                Log.e(TAG, "SMS RIL invalid format to " + maskPhoneNumber(targetNumber));
                break;
            case SmsManager.RESULT_RIL_SYSTEM_ERR:
                errorMessage = "RIL system error";
                Log.e(TAG, "SMS RIL system error to " + maskPhoneNumber(targetNumber));
                break;
            default:
                errorMessage = "Unknown SMS error (code: " + resultCode + ")";
                Log.e(TAG, "SMS unknown error " + resultCode + " to " + maskPhoneNumber(targetNumber));
                break;
        }
        
        // Log to database in background thread
        logSmsHistory(context, originalSender, originalMessage, targetNumber, forwardedMessage, timestamp, success, errorMessage);
        
        // Handle retry if SMS failed and retries available
        if (!success && retryCount < 3) { // MAX_RETRY_COUNT = 3
            if (DEBUG) {
                Log.d(TAG, "SMS failed, will be retried by SmsReceiver. Retry: " + (retryCount + 1) + "/3");
            }
        }
    }
    
    /**
     * Handle SMS delivered confirmation
     * Currently not used for database updates, but can be extended
     */
    private void handleSmsDelivered(Context context, Intent intent) {
        int resultCode = getResultCode();
        String targetNumber = intent.getStringExtra("targetNumber");
        
        if (resultCode == Activity.RESULT_OK) {
            if (DEBUG) {
                Log.d(TAG, "SMS delivered successfully to " + maskPhoneNumber(targetNumber));
            }
        } else {
            Log.w(TAG, "SMS delivery failed to " + maskPhoneNumber(targetNumber) + " (code: " + resultCode + ")");
        }
    }
    
    /**
     * Log SMS forwarding history to database
     * Runs in optimized background thread to avoid blocking main thread
     */
    private void logSmsHistory(Context context, String senderNumber, String originalMessage, String targetNumber, String forwardedMessage, long timestamp, boolean success, String errorMessage) {
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(context);
                SmsHistory history = new SmsHistory(
                    senderNumber,
                    originalMessage,
                    targetNumber,
                    forwardedMessage,
                    timestamp,
                    success,
                    errorMessage
                );
                database.smsHistoryDao().insert(history);
                
                if (DEBUG) {
                    String status = success ? "SUCCESS" : "FAILED";
                    Log.d(TAG, "SMS history logged: " + status + " from " + maskPhoneNumber(senderNumber) + 
                            " to " + maskPhoneNumber(targetNumber));
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to log SMS history: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Masks phone number for secure logging
     * Example: +905551234567 -> +9055***4567
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "***";
        }
        
        String prefix = phoneNumber.substring(0, Math.min(5, phoneNumber.length() - 4));
        String suffix = phoneNumber.substring(phoneNumber.length() - 4);
        return prefix + "***" + suffix;
    }
}