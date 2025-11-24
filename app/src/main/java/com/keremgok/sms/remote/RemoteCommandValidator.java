package com.keremgok.sms.remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.keremgok.sms.AppDatabase;

/**
 * Validator for remote SMS commands
 * Handles authorization, rate limiting, and security checks
 */
public class RemoteCommandValidator {
    
    private static final String TAG = "RemoteCommandValidator";
    
    // Rate limits (configurable via SharedPreferences)
    public static final int DEFAULT_MAX_COMMANDS_PER_HOUR = 10;
    public static final int DEFAULT_MAX_COMMANDS_PER_DAY = 50;
    
    // SharedPreferences keys
    private static final String PREFS_NAME = "remote_control_settings";
    public static final String KEY_ENABLED = "remote_control_enabled";
    public static final String KEY_MAX_COMMANDS_HOUR = "max_commands_hour";
    public static final String KEY_MAX_COMMANDS_DAY = "max_commands_day";
    public static final String KEY_SECURITY_MODE = "security_mode";
    public static final String KEY_COMMAND_PREFIX = "command_prefix";
    public static final String KEY_SEND_RESPONSE_SMS = "send_response_sms";
    
    // Security modes
    public static final String SECURITY_MODE_IMMEDIATE = "immediate";
    public static final String SECURITY_MODE_CONFIRM = "confirm";
    public static final String SECURITY_MODE_PIN = "pin";
    
    /**
     * Validation result
     */
    public static class ValidationResult {
        public boolean isAuthorized;
        public String reason;
        public AuthorizedNumber authorizedNumber;
        
        public static ValidationResult authorized(AuthorizedNumber number) {
            ValidationResult result = new ValidationResult();
            result.isAuthorized = true;
            result.authorizedNumber = number;
            result.reason = "Authorized";
            return result;
        }
        
        public static ValidationResult unauthorized(String reason) {
            ValidationResult result = new ValidationResult();
            result.isAuthorized = false;
            result.reason = reason;
            return result;
        }
        
        @Override
        public String toString() {
            return "ValidationResult{authorized=" + isAuthorized + ", reason='" + reason + "'}";
        }
    }
    
    /**
     * Check if remote control feature is enabled
     */
    public static boolean isEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ENABLED, false); // Disabled by default for security
    }
    
    /**
     * Enable or disable remote control feature
     */
    public static void setEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
        Log.i(TAG, "Remote control " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Get security mode
     */
    public static String getSecurityMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_SECURITY_MODE, SECURITY_MODE_CONFIRM); // Default: require confirmation
    }
    
    /**
     * Set security mode
     */
    public static void setSecurityMode(Context context, String mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SECURITY_MODE, mode).apply();
        Log.i(TAG, "Security mode set to: " + mode);
    }
    
    /**
     * Check if response SMS should be sent
     */
    public static boolean shouldSendResponseSms(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SEND_RESPONSE_SMS, true); // Default: send responses
    }
    
    /**
     * Validate sender authorization and rate limits
     */
    public static ValidationResult validateSender(Context context, String senderNumber) {
        try {
            // Check if feature is enabled
            if (!isEnabled(context)) {
                Log.w(TAG, "Remote control is disabled");
                return ValidationResult.unauthorized("Remote SMS control is disabled");
            }
            
            // Get database
            AppDatabase db = AppDatabase.getInstance(context);
            if (db == null) {
                Log.e(TAG, "Database is null");
                return ValidationResult.unauthorized("Internal error: database unavailable");
            }
            
            AuthorizedNumberDao dao = db.authorizedNumberDao();
            if (dao == null) {
                Log.e(TAG, "AuthorizedNumberDao is null");
                return ValidationResult.unauthorized("Internal error: database unavailable");
            }
            
            // Check if sender is authorized
            AuthorizedNumber authNumber = dao.getByPhoneNumber(senderNumber);
            if (authNumber == null) {
                Log.w(TAG, "Unauthorized sender: " + maskPhoneNumber(senderNumber));
                return ValidationResult.unauthorized("Your number is not authorized for remote SMS control");
            }
            
            // Check if authorized number is enabled
            if (!authNumber.isEnabled()) {
                Log.w(TAG, "Disabled authorized number: " + maskPhoneNumber(senderNumber));
                return ValidationResult.unauthorized("Your authorized number is currently disabled");
            }
            
            // Check rate limits
            ValidationResult rateLimitCheck = checkRateLimits(context, senderNumber);
            if (!rateLimitCheck.isAuthorized) {
                return rateLimitCheck;
            }
            
            Log.i(TAG, "Sender validated: " + maskPhoneNumber(senderNumber) + 
                  " (" + authNumber.getDisplayName() + ")");
            
            return ValidationResult.authorized(authNumber);
            
        } catch (Exception e) {
            Log.e(TAG, "Validation error: " + e.getMessage(), e);
            return ValidationResult.unauthorized("Validation error: " + e.getMessage());
        }
    }
    
    /**
     * Check rate limits for sender
     */
    private static ValidationResult checkRateLimits(Context context, String senderNumber) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            int maxPerHour = prefs.getInt(KEY_MAX_COMMANDS_HOUR, DEFAULT_MAX_COMMANDS_PER_HOUR);
            int maxPerDay = prefs.getInt(KEY_MAX_COMMANDS_DAY, DEFAULT_MAX_COMMANDS_PER_DAY);
            
            AppDatabase db = AppDatabase.getInstance(context);
            if (db == null) {
                Log.w(TAG, "Database unavailable, skipping rate limit check");
                return ValidationResult.authorized(null); // Allow if we can't check
            }
            
            RemoteCommandHistoryDao historyDao = db.remoteCommandHistoryDao();
            if (historyDao == null) {
                Log.w(TAG, "HistoryDao unavailable, skipping rate limit check");
                return ValidationResult.authorized(null);
            }
            
            long now = System.currentTimeMillis();
            long oneHourAgo = now - (60 * 60 * 1000);
            long oneDayAgo = now - (24 * 60 * 60 * 1000);
            
            // Check hourly limit
            int commandsLastHour = historyDao.countCommandsSince(senderNumber, oneHourAgo);
            if (commandsLastHour >= maxPerHour) {
                Log.w(TAG, "Rate limit exceeded (hourly): " + commandsLastHour + "/" + maxPerHour + 
                      " for " + maskPhoneNumber(senderNumber));
                return ValidationResult.unauthorized(
                    "Rate limit exceeded: maximum " + maxPerHour + " commands per hour. " +
                    "Please try again later."
                );
            }
            
            // Check daily limit
            int commandsLastDay = historyDao.countCommandsSince(senderNumber, oneDayAgo);
            if (commandsLastDay >= maxPerDay) {
                Log.w(TAG, "Rate limit exceeded (daily): " + commandsLastDay + "/" + maxPerDay + 
                      " for " + maskPhoneNumber(senderNumber));
                return ValidationResult.unauthorized(
                    "Rate limit exceeded: maximum " + maxPerDay + " commands per day. " +
                    "Please try again tomorrow."
                );
            }
            
            Log.d(TAG, "Rate limit check passed: " + commandsLastHour + "/" + maxPerHour + 
                  " per hour, " + commandsLastDay + "/" + maxPerDay + " per day");
            
            return ValidationResult.authorized(null);
            
        } catch (Exception e) {
            Log.e(TAG, "Rate limit check error: " + e.getMessage(), e);
            // Allow on error (fail open) but log it
            return ValidationResult.authorized(null);
        }
    }
    
    /**
     * Update authorized number's last used timestamp
     */
    public static void updateLastUsed(Context context, String phoneNumber) {
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            if (db != null && db.authorizedNumberDao() != null) {
                db.authorizedNumberDao().updateLastUsed(phoneNumber, System.currentTimeMillis());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating last used: " + e.getMessage(), e);
        }
    }
    
    /**
     * Mask phone number for logging (privacy)
     */
    private static String maskPhoneNumber(String number) {
        if (number == null || number.length() < 4) {
            return "***";
        }
        
        String prefix = number.substring(0, Math.min(4, number.length()));
        String suffix = number.length() > 7 ? number.substring(number.length() - 3) : "";
        
        return prefix + "***" + suffix;
    }
    
    /**
     * Get rate limit settings
     */
    public static int getMaxCommandsPerHour(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_MAX_COMMANDS_HOUR, DEFAULT_MAX_COMMANDS_PER_HOUR);
    }
    
    public static int getMaxCommandsPerDay(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_MAX_COMMANDS_DAY, DEFAULT_MAX_COMMANDS_PER_DAY);
    }
    
    /**
     * Set rate limit settings
     */
    public static void setRateLimits(Context context, int perHour, int perDay) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putInt(KEY_MAX_COMMANDS_HOUR, perHour)
            .putInt(KEY_MAX_COMMANDS_DAY, perDay)
            .apply();
        Log.i(TAG, "Rate limits updated: " + perHour + "/hour, " + perDay + "/day");
    }
}
