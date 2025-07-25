package com.keremgok.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {
    
    private static final String TAG = "HermesSmsReceiver";
    private static final String PREFS_NAME = "HermesPrefs";
    private static final String KEY_TARGET_NUMBER = "target_number";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "SMS received");
        
        if (intent.getAction() == null || !intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            return;
        }
        
        // Get target number from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String targetNumber = prefs.getString(KEY_TARGET_NUMBER, "");
        
        if (TextUtils.isEmpty(targetNumber)) {
            Log.w(TAG, "No target number configured, SMS forwarding disabled");
            return;
        }
        
        // Extract SMS messages from the intent
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Log.e(TAG, "No bundle data in SMS intent");
            return;
        }
        
        try {
            Object[] pdus = (Object[]) bundle.get("pdus");
            String format = bundle.getString("format");
            
            if (pdus == null || pdus.length == 0) {
                Log.e(TAG, "No PDU data found in SMS intent");
                return;
            }
            
            StringBuilder messageBody = new StringBuilder();
            String senderNumber = "";
            long timestamp = 0;
            
            // Parse all SMS parts
            for (Object pdu : pdus) {
                SmsMessage smsMessage;
                if (format != null) {
                    smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                } else {
                    smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                }
                
                if (smsMessage != null) {
                    if (TextUtils.isEmpty(senderNumber)) {
                        senderNumber = smsMessage.getOriginatingAddress();
                        timestamp = smsMessage.getTimestampMillis();
                    }
                    messageBody.append(smsMessage.getMessageBody());
                }
            }
            
            String finalMessage = messageBody.toString();
            
            if (!TextUtils.isEmpty(finalMessage) && !TextUtils.isEmpty(senderNumber)) {
                Log.d(TAG, "SMS from: " + senderNumber + ", forwarding to: " + targetNumber);
                forwardSms(context, senderNumber, finalMessage, targetNumber, timestamp);
            } else {
                Log.e(TAG, "Invalid SMS data - sender: " + senderNumber + ", message: " + finalMessage);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing SMS: " + e.getMessage(), e);
        }
    }
    
    private void forwardSms(Context context, String originalSender, String message, String targetNumber, long timestamp) {
        try {
            // Format the forwarded message with original sender info
            String forwardedMessage = String.format(
                "[Hermes SMS Forward]\nGönderen: %s\nMesaj: %s\nZaman: %s",
                originalSender,
                message,
                new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
                    .format(new java.util.Date(timestamp))
            );
            
            // Check if message is too long and split if necessary
            if (forwardedMessage.length() > 160) {
                sendLongSms(forwardedMessage, targetNumber);
            } else {
                sendSingleSms(forwardedMessage, targetNumber);
            }
            
            Log.i(TAG, "SMS successfully forwarded to: " + targetNumber);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to forward SMS: " + e.getMessage(), e);
        }
    }
    
    private void sendSingleSms(String message, String targetNumber) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(targetNumber, null, message, null, null);
            Log.d(TAG, "Single SMS sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send single SMS: " + e.getMessage(), e);
        }
    }
    
    private void sendLongSms(String message, String targetNumber) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            java.util.ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(targetNumber, null, parts, null, null);
            Log.d(TAG, "Multipart SMS sent successfully (" + parts.size() + " parts)");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send multipart SMS: " + e.getMessage(), e);
        }
    }
}