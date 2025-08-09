package com.keremgok.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * BroadcastReceiver for detecting missed calls
 * Monitors phone state changes to identify when calls are missed
 */
public class MissedCallReceiver extends BroadcastReceiver {
    
    private static final String TAG = "MissedCallReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            // Check if missed call notifications are enabled
            if (!isMissedCallNotificationEnabled(context)) {
                return;
            }
            
            if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                
                Log.d(TAG, "Phone state changed: " + state + ", number: " + phoneNumber);
                
                // Delegate to CallStateManager for processing
                CallStateManager.getInstance(context).handlePhoneStateChange(state, phoneNumber);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing phone state change: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if missed call notifications are enabled in settings
     */
    private boolean isMissedCallNotificationEnabled(Context context) {
        try {
            android.content.SharedPreferences prefs = 
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.getBoolean("missed_call_notifications_enabled", false);
        } catch (Exception e) {
            Log.e(TAG, "Error checking missed call notification setting: " + e.getMessage());
            return false;
        }
    }
}