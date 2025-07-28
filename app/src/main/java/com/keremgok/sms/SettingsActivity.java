package com.keremgok.sms;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

/**
 * Settings Activity for Hermes SMS Forward
 * Provides comprehensive configuration options for the SMS forwarding app
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.settings_title);
        }
        
        // Load settings fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button press
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Settings Fragment that handles all preference interactions
     */
    public static class SettingsFragment extends PreferenceFragmentCompat {
        
        private BackupManager backupManager;
        
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load preferences from XML resource
            setPreferencesFromResource(R.xml.preferences, rootKey);
            
            // Initialize backup manager
            backupManager = new BackupManager(requireContext());
            
            // Initialize preference listeners and setup
            initializePreferences();
        }
        
        /**
         * Initialize all preference listeners and setup
         */
        private void initializePreferences() {
            // Initialize forwarding toggle
            initializeForwardingToggle();
            
            // Initialize forwarding delay
            initializeForwardingDelay();
            
            // Initialize about section
            initializeAboutSection();
            
            // Initialize notification settings
            initializeNotificationSettings();
            
            // Initialize log level settings
            initializeLogLevelSettings();
            
            // Initialize language settings
            initializeLanguageSettings();
            
            // Initialize backup and restore preferences
            initializeBackupRestore();
        }
        
        /**
         * Initialize the main forwarding enable/disable toggle
         */
        private void initializeForwardingToggle() {
            // Implementation will be added when we create the preferences XML
        }
        
        /**
         * Initialize forwarding delay SeekBar with dynamic summary
         */
        private void initializeForwardingDelay() {
            androidx.preference.SeekBarPreference delayPref = findPreference("pref_forwarding_delay");
            if (delayPref != null) {
                // Set initial summary
                updateDelayPreferenceSummary(delayPref, delayPref.getValue());
                
                // Add listener for value changes
                delayPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    int delaySeconds = (Integer) newValue;
                    updateDelayPreferenceSummary((androidx.preference.SeekBarPreference) preference, delaySeconds);
                    return true;
                });
            }
        }
        
        /**
         * Update the summary text for forwarding delay preference
         */
        private void updateDelayPreferenceSummary(androidx.preference.SeekBarPreference preference, int delaySeconds) {
            String summary;
            if (delaySeconds == 0) {
                summary = getString(R.string.settings_forwarding_delay_instant);
            } else {
                summary = getString(R.string.settings_forwarding_delay_format, delaySeconds);
            }
            preference.setSummary(summary);
        }
        
        /**
         * Initialize about section with version and developer info
         */
        private void initializeAboutSection() {
            // Implementation will be added when we create the preferences XML
        }
        
        /**
         * Initialize notification settings
         */
        private void initializeNotificationSettings() {
            // Implementation will be added when we create the preferences XML
        }
        
        /**
         * Initialize log level settings
         */
        private void initializeLogLevelSettings() {
            // Implementation will be added when we create the preferences XML
        }
        
        /**
         * Initialize language settings
         */
        private void initializeLanguageSettings() {
            androidx.preference.ListPreference languagePref = findPreference("pref_app_language");
            if (languagePref != null) {
                languagePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    String selectedLanguage = (String) newValue;
                    
                    // Show restart dialog to apply language change
                    new AlertDialog.Builder(requireContext())
                        .setTitle(getString(R.string.settings_language_title))
                        .setMessage("Dil değişikliğinin uygulanması için uygulama yeniden başlatılacak.\n\nLanguage change will be applied after app restart.")
                        .setPositiveButton("Tamam / OK", (dialog, which) -> {
                            // Save the language preference
                            getPreferenceManager().getSharedPreferences()
                                .edit()
                                .putString("pref_app_language", selectedLanguage)
                                .apply();
                            
                            // Restart the app
                            android.content.Intent restartIntent = requireActivity().getPackageManager()
                                .getLaunchIntentForPackage(requireActivity().getPackageName());
                            if (restartIntent != null) {
                                restartIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(restartIntent);
                                requireActivity().finish();
                            }
                        })
                        .setNegativeButton("İptal / Cancel", null)
                        .show();
                    
                    return false; // Don't update preference immediately
                });
            }
        }
        
        /**
         * Initialize backup and restore preferences
         */
        private void initializeBackupRestore() {
            // Backup settings preference
            Preference backupPref = findPreference("pref_backup_settings");
            if (backupPref != null) {
                backupPref.setOnPreferenceClickListener(preference -> {
                    showBackupDialog();
                    return true;
                });
            }
            
            // Restore settings preference
            Preference restorePref = findPreference("pref_restore_settings");
            if (restorePref != null) {
                restorePref.setOnPreferenceClickListener(preference -> {
                    showRestoreDialog();
                    return true;
                });
            }
        }
        
        /**
         * Show backup creation dialog
         */
        private void showBackupDialog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.backup_create_title);
            builder.setMessage(R.string.backup_create_message);
            
            // Add checkbox for including SMS history
            final boolean[] includeHistory = {false};
            builder.setMultiChoiceItems(
                new CharSequence[]{getString(R.string.backup_include_history)},
                new boolean[]{false},
                (dialog, which, isChecked) -> includeHistory[0] = isChecked
            );
            
            builder.setPositiveButton(R.string.backup_create_confirm, (dialog, which) -> {
                createBackup(includeHistory[0]);
            });
            
            builder.setNegativeButton(R.string.cancel, null);
            builder.show();
        }
        
        /**
         * Show restore selection dialog
         */
        private void showRestoreDialog() {
            String[] availableBackups = backupManager.getAvailableBackupFiles();
            
            if (availableBackups.length == 0) {
                new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.restore_select_title)
                    .setMessage(R.string.restore_no_backups)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
                return;
            }
            
            // Create user-friendly backup names with timestamps
            String[] backupNames = new String[availableBackups.length];
            for (int i = 0; i < availableBackups.length; i++) {
                String fileName = availableBackups[i].substring(availableBackups[i].lastIndexOf('/') + 1);
                // Extract timestamp from filename
                if (fileName.startsWith("hermes_backup_") && fileName.endsWith(".json")) {
                    String timestamp = fileName.substring(14, fileName.length() - 5);
                    try {
                        // Parse timestamp: YYYYMMDD_HHMMSS
                        String date = timestamp.substring(0, 8);
                        String time = timestamp.substring(9);
                        String formattedDate = date.substring(6, 8) + "/" + 
                                              date.substring(4, 6) + "/" + 
                                              date.substring(0, 4);
                        String formattedTime = time.substring(0, 2) + ":" + 
                                              time.substring(2, 4) + ":" + 
                                              time.substring(4, 6);
                        backupNames[i] = formattedDate + " " + formattedTime;
                    } catch (Exception e) {
                        backupNames[i] = fileName;
                    }
                } else {
                    backupNames[i] = fileName;
                }
            }
            
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.restore_select_title);
            builder.setItems(backupNames, (dialog, which) -> {
                showRestoreModeDialog(availableBackups[which]);
            });
            builder.setNegativeButton(R.string.cancel, null);
            builder.show();
        }
        
        /**
         * Show restore mode selection dialog (replace vs merge)
         */
        private void showRestoreModeDialog(String backupFilePath) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.restore_mode_title);
            builder.setMessage(R.string.restore_mode_message);
            
            String[] modes = {
                getString(R.string.restore_mode_replace),
                getString(R.string.restore_mode_merge)
            };
            
            builder.setItems(modes, (dialog, which) -> {
                BackupManager.RestoreMode mode = (which == 0) ? 
                    BackupManager.RestoreMode.REPLACE : 
                    BackupManager.RestoreMode.MERGE;
                showRestoreConfirmDialog(backupFilePath, mode);
            });
            
            builder.setNegativeButton(R.string.cancel, null);
            builder.show();
        }
        
        /**
         * Show final restore confirmation dialog
         */
        private void showRestoreConfirmDialog(String backupFilePath, BackupManager.RestoreMode mode) {
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle(R.string.restore_confirm_title);
            builder.setMessage(R.string.restore_confirm_message);
            
            builder.setPositiveButton(R.string.restore_confirm, (dialog, which) -> {
                restoreFromBackup(backupFilePath, mode);
            });
            
            builder.setNegativeButton(R.string.cancel, null);
            builder.show();
        }
        
        /**
         * Create backup in background thread
         */
        private void createBackup(boolean includeHistory) {
            ProgressDialog progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage(getString(R.string.backup_creating));
            progressDialog.setCancelable(false);
            progressDialog.show();
            
            ThreadManager.getInstance().executeBackground(() -> {
                String backupPath = backupManager.createBackup(includeHistory);
                
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    
                    if (backupPath != null) {
                        // Show success message with file path
                        String message = getString(R.string.backup_success) + "\n\n" + 
                                       getString(R.string.backup_success_path, backupPath);
                        
                        new AlertDialog.Builder(requireContext())
                            .setTitle(R.string.backup_success)
                            .setMessage(message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    } else {
                        Toast.makeText(requireContext(), R.string.backup_error, Toast.LENGTH_LONG).show();
                    }
                });
            });
        }
        
        /**
         * Restore from backup in background thread
         */
        private void restoreFromBackup(String backupFilePath, BackupManager.RestoreMode mode) {
            ProgressDialog progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage(getString(R.string.restore_restoring));
            progressDialog.setCancelable(false);
            progressDialog.show();
            
            ThreadManager.getInstance().executeBackground(() -> {
                // First validate the backup
                BackupManager.ValidationResult validation = backupManager.validateBackup(backupFilePath);
                
                if (!validation.isValid()) {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();
                        String errorMessage = getString(R.string.restore_validation_error, validation.getErrorMessage());
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                
                // Perform restore
                boolean success = backupManager.restoreFromBackup(backupFilePath, mode);
                
                requireActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    
                    if (success) {
                        Toast.makeText(requireContext(), R.string.restore_success, Toast.LENGTH_LONG).show();
                        
                        // Refresh the preferences screen to show updated values
                        getPreferenceScreen().removeAll();
                        addPreferencesFromResource(R.xml.preferences);
                        initializePreferences();
                        
                    } else {
                        Toast.makeText(requireContext(), R.string.restore_error, Toast.LENGTH_LONG).show();
                    }
                });
            });
        }
    }
}