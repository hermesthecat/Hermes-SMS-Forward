package com.keremgok.sms;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages call state detection and missed call processing
 * Implements singleton pattern for centralized call state management
 */
public class CallStateManager {
    
    private static final String TAG = "CallStateManager";
    private static final String PREFS_NAME = "CallStatePrefs";
    private static final String KEY_LAST_CALL_TIME = "last_call_time_";
    
    private static CallStateManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    
    // Track call states per phone number
    private final Map<String, CallInfo> activeCall = new HashMap<>();
    
    private CallStateManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized CallStateManager getInstance(Context context) {
        if (instance == null) {
            instance = new CallStateManager(context);
        }
        return instance;
    }
    
    /**
     * Handle phone state changes from the BroadcastReceiver
     */
    public void handlePhoneStateChange(String state, String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            handleIncomingCall(phoneNumber, currentTime);
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            handleCallAnswered(phoneNumber, currentTime);
        } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            handleCallEnded(phoneNumber, currentTime);
        }
    }
    
    /**
     * Handle incoming call (phone is ringing)
     */
    private void handleIncomingCall(String phoneNumber, long timestamp) {
        Log.d(TAG, "Incoming call from: " + phoneNumber);
        activeCall.put(phoneNumber, new CallInfo(phoneNumber, timestamp, false));
    }
    
    /**
     * Handle call answered (went off-hook)
     */
    private void handleCallAnswered(String phoneNumber, long timestamp) {
        Log.d(TAG, "Call answered: " + phoneNumber);
        CallInfo call = activeCall.get(phoneNumber);
        if (call != null) {
            call.answered = true;
        }
    }
    
    /**
     * Handle call ended (back to idle)
     */
    private void handleCallEnded(String phoneNumber, long timestamp) {
        Log.d(TAG, "Call ended: " + phoneNumber);
        
        CallInfo call = activeCall.remove(phoneNumber);
        if (call != null && !call.answered) {
            // This was a missed call
            processMissedCall(call.phoneNumber, call.startTime, timestamp);
        }
    }
    
    /**
     * Process a detected missed call
     */
    private void processMissedCall(String phoneNumber, long callStartTime, long callEndTime) {
        Log.i(TAG, "Processing missed call from: " + phoneNumber);
        
        try {
            // Check if we've already processed this missed call recently (avoid duplicates)
            String lastCallKey = KEY_LAST_CALL_TIME + phoneNumber;
            long lastProcessedTime = prefs.getLong(lastCallKey, 0);
            
            // If we processed a call from this number in the last 60 seconds, skip
            if (callStartTime - lastProcessedTime < 60000) {
                Log.d(TAG, "Skipping duplicate missed call from: " + phoneNumber);
                return;
            }
            
            // Verify this is actually a missed call by checking call log
            if (verifyMissedCallInLog(phoneNumber, callStartTime)) {
                // Send missed call notification via SMS
                sendMissedCallNotification(phoneNumber, callStartTime);
                
                // Update last processed time
                prefs.edit().putLong(lastCallKey, callStartTime).apply();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing missed call: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verify the missed call exists in the call log
     */
    private boolean verifyMissedCallInLog(String phoneNumber, long timestamp) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "No permission to read call log");
            return true; // Assume it's a missed call if we can't verify
        }
        
        try {
            Uri uri = CallLog.Calls.CONTENT_URI;
            String[] projection = {CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE};
            String selection = CallLog.Calls.NUMBER + " = ? AND " + 
                             CallLog.Calls.TYPE + " = ? AND " +
                             CallLog.Calls.DATE + " > ?";
            String[] selectionArgs = {
                phoneNumber, 
                String.valueOf(CallLog.Calls.MISSED_TYPE),
                String.valueOf(timestamp - 10000) // 10 second tolerance
            };
            String sortOrder = CallLog.Calls.DATE + " DESC";
            
            Cursor cursor = context.getContentResolver().query(
                uri, projection, selection, selectionArgs, sortOrder);
            
            if (cursor != null) {
                boolean found = cursor.getCount() > 0;
                cursor.close();
                return found;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error verifying missed call in log: " + e.getMessage(), e);
        }
        
        return true; // Assume it's a missed call if verification fails
    }
    
    /**
     * Send missed call notification via SMS
     */
    private void sendMissedCallNotification(String phoneNumber, long timestamp) {
        try {
            // Get SmsFormatter for message formatting
            SmsFormatter formatter = new SmsFormatter(context);
            String missedCallMessage = formatter.formatMissedCall(phoneNumber, timestamp);
            
            // Use existing SMS queue system to send notification
            SmsQueueManager queueManager = SmsQueueManager.getInstance(context);
            queueManager.queueMissedCallNotification(phoneNumber, missedCallMessage, timestamp);
            
            Log.i(TAG, "Queued missed call notification for: " + phoneNumber);
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending missed call notification: " + e.getMessage(), e);
        }
    }
    
    /**
     * Inner class to track call information
     */
    private static class CallInfo {
        final String phoneNumber;
        final long startTime;
        boolean answered;
        
        CallInfo(String phoneNumber, long startTime, boolean answered) {
            this.phoneNumber = phoneNumber;
            this.startTime = startTime;
            this.answered = answered;
        }
    }
}