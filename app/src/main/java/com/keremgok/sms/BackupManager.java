package com.keremgok.sms;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import androidx.preference.PreferenceManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * BackupManager - Handles backup and restore functionality for Hermes SMS Forward
 * 
 * Features:
 * - Export app settings to JSON format
 * - Backup target numbers and filter rules
 * - Import and restore from JSON backup files
 * - Data validation and integrity checks
 * - Merge vs replace restore options
 * 
 * Task 19: Backup & Restore functionality implementation
 */
public class BackupManager {
    
    private static final String TAG = "BackupManager";
    private static final String BACKUP_VERSION = "1.0";
    private static final String BACKUP_FILE_PREFIX = "hermes_backup_";
    private static final String BACKUP_FILE_EXTENSION = ".json";
    
    private final Context context;
    private final AppDatabase database;
    
    /**
     * Backup validation result
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * Restore options for merge vs replace
     */
    public enum RestoreMode {
        REPLACE, // Replace all existing data
        MERGE    // Merge with existing data
    }
    
    public BackupManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
    }
    
    /**
     * Create a complete backup of app settings and data
     * @param includeHistory Whether to include SMS history in backup
     * @return File path of created backup, or null if failed
     */
    public String createBackup(boolean includeHistory) {
        try {
            JSONObject backup = new JSONObject();
            
            // Backup metadata
            backup.put("backup_version", BACKUP_VERSION);
            
            // Get app version from PackageManager
            try {
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                backup.put("app_version", packageInfo.versionName);
                backup.put("app_version_code", packageInfo.versionCode);
            } catch (PackageManager.NameNotFoundException e) {
                backup.put("app_version", "unknown");
                backup.put("app_version_code", 0);
            }
            
            backup.put("created_timestamp", System.currentTimeMillis());
            backup.put("created_by", "Hermes SMS Forward");
            
            // Backup SharedPreferences settings
            backup.put("settings", exportSettings());
            
            // Backup database tables
            backup.put("target_numbers", exportTargetNumbers());
            backup.put("sms_filters", exportSmsFilters());
            
            // Optionally include SMS history
            backup.put("include_sms_history", includeHistory);
            if (includeHistory) {
                backup.put("sms_history", exportSmsHistory());
            } else {
                backup.put("sms_history", new JSONArray());
            }
            
            // Save to file
            String fileName = generateBackupFileName();
            File backupFile = new File(context.getExternalFilesDir(null), fileName);
            
            FileOutputStream fos = new FileOutputStream(backupFile);
            fos.write(backup.toString(2).getBytes());
            fos.close();
            
            android.util.Log.i(TAG, "Backup created successfully: " + backupFile.getAbsolutePath());
            return backupFile.getAbsolutePath();
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to create backup: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Validate a backup file before restoration
     * @param backupFilePath Path to backup file
     * @return ValidationResult with validation status and error message
     */
    public ValidationResult validateBackup(String backupFilePath) {
        try {
            File backupFile = new File(backupFilePath);
            if (!backupFile.exists()) {
                return new ValidationResult(false, "Backup file does not exist");
            }
            
            // Read file content
            FileInputStream fis = new FileInputStream(backupFile);
            byte[] data = new byte[(int) backupFile.length()];
            fis.read(data);
            fis.close();
            
            String jsonContent = new String(data);
            JSONObject backup = new JSONObject(jsonContent);
            
            // Validate required fields
            if (!backup.has("backup_version")) {
                return new ValidationResult(false, "Invalid backup format: missing backup_version");
            }
            
            if (!backup.has("settings") || !backup.has("target_numbers") || !backup.has("sms_filters")) {
                return new ValidationResult(false, "Invalid backup format: missing required sections");
            }
            
            // Check backup version compatibility
            String backupVersion = backup.getString("backup_version");
            if (!BACKUP_VERSION.equals(backupVersion)) {
                return new ValidationResult(false, "Incompatible backup version: " + backupVersion);
            }
            
            return new ValidationResult(true, null);
            
        } catch (Exception e) {
            return new ValidationResult(false, "Failed to validate backup: " + e.getMessage());
        }
    }
    
    /**
     * Restore data from backup file
     * @param backupFilePath Path to backup file
     * @param mode Restore mode (REPLACE or MERGE)
     * @return true if restore was successful
     */
    public boolean restoreFromBackup(String backupFilePath, RestoreMode mode) {
        try {
            // First validate the backup
            ValidationResult validation = validateBackup(backupFilePath);
            if (!validation.isValid()) {
                android.util.Log.e(TAG, "Backup validation failed: " + validation.getErrorMessage());
                return false;
            }
            
            // Read backup file
            File backupFile = new File(backupFilePath);
            FileInputStream fis = new FileInputStream(backupFile);
            byte[] data = new byte[(int) backupFile.length()];
            fis.read(data);
            fis.close();
            
            String jsonContent = new String(data);
            JSONObject backup = new JSONObject(jsonContent);
            
            // Restore in transaction-like manner
            boolean success = true;
            
            // Restore settings
            success &= importSettings(backup.getJSONObject("settings"));
            
            // Restore target numbers
            success &= importTargetNumbers(backup.getJSONArray("target_numbers"), mode);
            
            // Restore SMS filters
            success &= importSmsFilters(backup.getJSONArray("sms_filters"), mode);
            
            // Restore SMS history if included
            if (backup.getBoolean("include_sms_history")) {
                success &= importSmsHistory(backup.getJSONArray("sms_history"), mode);
            }
            
            if (success) {
                android.util.Log.i(TAG, "Backup restored successfully from: " + backupFilePath);
            } else {
                android.util.Log.e(TAG, "Some errors occurred during restore");
            }
            
            return success;
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to restore backup: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Export app settings from SharedPreferences
     */
    private JSONObject exportSettings() throws JSONException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        JSONObject settings = new JSONObject();
        
        // Export all settings defined in preferences.xml
        settings.put("pref_forwarding_enabled", prefs.getBoolean("pref_forwarding_enabled", true));
        settings.put("pref_sms_format", prefs.getString("pref_sms_format", "standard"));
        settings.put("pref_forwarding_delay", prefs.getInt("pref_forwarding_delay", 0));
        settings.put("pref_show_notifications", prefs.getBoolean("pref_show_notifications", true));
        settings.put("pref_notification_sound", prefs.getBoolean("pref_notification_sound", false));
        settings.put("pref_notification_vibration", prefs.getBoolean("pref_notification_vibration", false));
        settings.put("pref_log_level", prefs.getString("pref_log_level", "error"));
        
        return settings;
    }
    
    /**
     * Export target numbers from database
     */
    private JSONArray exportTargetNumbers() throws JSONException {
        List<TargetNumber> targetNumbers = database.targetNumberDao().getAllTargetNumbers();
        JSONArray jsonArray = new JSONArray();
        
        for (TargetNumber target : targetNumbers) {
            JSONObject json = new JSONObject();
            json.put("id", target.getId());
            json.put("phone_number", target.getPhoneNumber());
            json.put("display_name", target.getDisplayName());
            json.put("is_primary", target.isPrimary());
            json.put("is_enabled", target.isEnabled());
            json.put("created_timestamp", target.getCreatedTimestamp());
            json.put("last_used_timestamp", target.getLastUsedTimestamp());
            jsonArray.put(json);
        }
        
        return jsonArray;
    }
    
    /**
     * Export SMS filters from database
     */
    private JSONArray exportSmsFilters() throws JSONException {
        List<SmsFilter> filters = database.smsFilterDao().getAllFilters();
        JSONArray jsonArray = new JSONArray();
        
        for (SmsFilter filter : filters) {
            JSONObject json = new JSONObject();
            json.put("id", filter.getId());
            json.put("filter_name", filter.getFilterName());
            json.put("filter_type", filter.getFilterType());
            json.put("pattern", filter.getPattern());
            json.put("action", filter.getAction());
            json.put("is_enabled", filter.isEnabled());
            json.put("is_case_sensitive", filter.isCaseSensitive());
            json.put("is_regex", filter.isRegex());
            json.put("priority", filter.getPriority());
            json.put("match_count", filter.getMatchCount());
            json.put("last_matched", filter.getLastMatched());
            json.put("created_timestamp", filter.getCreatedTimestamp());
            json.put("modified_timestamp", filter.getModifiedTimestamp());
            jsonArray.put(json);
        }
        
        return jsonArray;
    }
    
    /**
     * Export SMS history from database (optional)
     */
    private JSONArray exportSmsHistory() throws JSONException {
        List<SmsHistory> historyList = database.smsHistoryDao().getAllHistory();
        JSONArray jsonArray = new JSONArray();
        
        for (SmsHistory history : historyList) {
            JSONObject json = new JSONObject();
            json.put("id", history.getId());
            json.put("sender_number", history.getSenderNumber());
            json.put("original_message", history.getOriginalMessage());
            json.put("target_number", history.getTargetNumber());
            json.put("forwarded_message", history.getForwardedMessage());
            json.put("timestamp", history.getTimestamp());
            json.put("success", history.isSuccess());
            json.put("error_message", history.getErrorMessage());
            jsonArray.put(json);
        }
        
        return jsonArray;
    }
    
    /**
     * Import settings to SharedPreferences
     */
    private boolean importSettings(JSONObject settings) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = prefs.edit();
            
            // Import all settings
            if (settings.has("pref_forwarding_enabled")) {
                editor.putBoolean("pref_forwarding_enabled", settings.getBoolean("pref_forwarding_enabled"));
            }
            if (settings.has("pref_sms_format")) {
                editor.putString("pref_sms_format", settings.getString("pref_sms_format"));
            }
            if (settings.has("pref_forwarding_delay")) {
                editor.putInt("pref_forwarding_delay", settings.getInt("pref_forwarding_delay"));
            }
            if (settings.has("pref_show_notifications")) {
                editor.putBoolean("pref_show_notifications", settings.getBoolean("pref_show_notifications"));
            }
            if (settings.has("pref_notification_sound")) {
                editor.putBoolean("pref_notification_sound", settings.getBoolean("pref_notification_sound"));
            }
            if (settings.has("pref_notification_vibration")) {
                editor.putBoolean("pref_notification_vibration", settings.getBoolean("pref_notification_vibration"));
            }
            if (settings.has("pref_log_level")) {
                editor.putString("pref_log_level", settings.getString("pref_log_level"));
            }
            
            editor.apply();
            return true;
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to import settings: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Import target numbers to database
     */
    private boolean importTargetNumbers(JSONArray targetNumbers, RestoreMode mode) {
        try {
            TargetNumberDao dao = database.targetNumberDao();
            
            // If REPLACE mode, clear existing data
            if (mode == RestoreMode.REPLACE) {
                dao.deleteAll();
            }
            
            // Import target numbers
            for (int i = 0; i < targetNumbers.length(); i++) {
                JSONObject json = targetNumbers.getJSONObject(i);
                
                TargetNumber target = new TargetNumber(
                    json.getString("phone_number"),
                    json.getString("display_name"),
                    json.getBoolean("is_primary"),
                    json.getBoolean("is_enabled")
                );
                
                if (json.has("created_timestamp")) {
                    target.setCreatedTimestamp(json.getLong("created_timestamp"));
                }
                if (json.has("last_used_timestamp")) {
                    target.setLastUsedTimestamp(json.getLong("last_used_timestamp"));
                }
                
                // In MERGE mode, check if phone number already exists
                if (mode == RestoreMode.MERGE) {
                    TargetNumber existing = dao.getTargetNumberByPhone(target.getPhoneNumber());
                    if (existing != null) {
                        // Update existing record
                        existing.setDisplayName(target.getDisplayName());
                        existing.setPrimary(target.isPrimary());
                        existing.setEnabled(target.isEnabled());
                        dao.update(existing);
                        continue;
                    }
                }
                
                dao.insert(target);
            }
            
            return true;
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to import target numbers: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Import SMS filters to database
     */
    private boolean importSmsFilters(JSONArray smsFilters, RestoreMode mode) {
        try {
            SmsFilterDao dao = database.smsFilterDao();
            
            // If REPLACE mode, clear existing data
            if (mode == RestoreMode.REPLACE) {
                dao.deleteAllFilters();
            }
            
            // Import SMS filters
            for (int i = 0; i < smsFilters.length(); i++) {
                JSONObject json = smsFilters.getJSONObject(i);
                
                SmsFilter filter = new SmsFilter(
                    json.getString("filter_name"),
                    json.getString("filter_type"),
                    json.getString("pattern"),
                    json.getString("action"),
                    json.getBoolean("is_enabled")
                );
                
                // Set additional properties
                if (json.has("is_case_sensitive")) {
                    filter.setCaseSensitive(json.getBoolean("is_case_sensitive"));
                }
                if (json.has("is_regex")) {
                    filter.setRegex(json.getBoolean("is_regex"));
                }
                if (json.has("priority")) {
                    filter.setPriority(json.getInt("priority"));
                }
                if (json.has("match_count")) {
                    filter.setMatchCount(json.getInt("match_count"));
                }
                if (json.has("last_matched")) {
                    filter.setLastMatched(json.getLong("last_matched"));
                }
                if (json.has("created_timestamp")) {
                    filter.setCreatedTimestamp(json.getLong("created_timestamp"));
                }
                if (json.has("modified_timestamp")) {
                    filter.setModifiedTimestamp(json.getLong("modified_timestamp"));
                }
                
                // In MERGE mode, check if filter with same name exists
                if (mode == RestoreMode.MERGE) {
                    SmsFilter existing = dao.getFilterByName(filter.getFilterName());
                    if (existing != null) {
                        // Update existing filter
                        existing.setFilterType(filter.getFilterType());
                        existing.setPattern(filter.getPattern());
                        existing.setAction(filter.getAction());
                        existing.setEnabled(filter.isEnabled());
                        existing.setCaseSensitive(filter.isCaseSensitive());
                        existing.setRegex(filter.isRegex());
                        existing.setPriority(filter.getPriority());
                        dao.update(existing);
                        continue;
                    }
                }
                
                dao.insert(filter);
            }
            
            return true;
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to import SMS filters: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Import SMS history to database (optional)
     */
    private boolean importSmsHistory(JSONArray smsHistory, RestoreMode mode) {
        try {
            SmsHistoryDao dao = database.smsHistoryDao();
            
            // If REPLACE mode, clear existing data
            if (mode == RestoreMode.REPLACE) {
                dao.deleteAllHistory();
            }
            
            // Import SMS history
            for (int i = 0; i < smsHistory.length(); i++) {
                JSONObject json = smsHistory.getJSONObject(i);
                
                SmsHistory history = new SmsHistory(
                    json.getString("sender_number"),
                    json.getString("original_message"),
                    json.getString("target_number"),
                    json.getString("forwarded_message"),
                    json.getLong("timestamp"),
                    json.getBoolean("success"),
                    json.optString("error_message", null)
                );
                
                // In MERGE mode, avoid duplicates by checking timestamp and sender
                if (mode == RestoreMode.MERGE) {
                    // For simplicity, we'll skip history duplicates in merge mode
                    // In a real implementation, you might want more sophisticated duplicate detection
                }
                
                dao.insert(history);
            }
            
            return true;
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to import SMS history: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Generate unique backup file name with timestamp
     */
    private String generateBackupFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        return BACKUP_FILE_PREFIX + timestamp + BACKUP_FILE_EXTENSION;
    }
    
    /**
     * Get available backup files in the app's external files directory
     * @return Array of backup file paths
     */
    public String[] getAvailableBackupFiles() {
        File filesDir = context.getExternalFilesDir(null);
        if (filesDir == null || !filesDir.exists()) {
            return new String[0];
        }
        
        File[] files = filesDir.listFiles((dir, name) -> 
            name.startsWith(BACKUP_FILE_PREFIX) && name.endsWith(BACKUP_FILE_EXTENSION));
        
        if (files == null) {
            return new String[0];
        }
        
        String[] filePaths = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            filePaths[i] = files[i].getAbsolutePath();
        }
        
        return filePaths;
    }
}