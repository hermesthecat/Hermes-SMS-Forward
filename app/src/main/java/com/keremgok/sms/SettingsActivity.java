package com.keremgok.sms;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

/**
 * Settings Activity for Hermes SMS Forward
 * Provides comprehensive configuration options for the SMS forwarding app
 */
public class SettingsActivity extends AppCompatActivity {

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
        
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load preferences from XML resource
            setPreferencesFromResource(R.xml.preferences, rootKey);
            
            // Initialize preference listeners and setup
            initializePreferences();
        }
        
        /**
         * Initialize all preference listeners and setup
         */
        private void initializePreferences() {
            // Initialize forwarding toggle
            initializeForwardingToggle();
            
            // Initialize about section
            initializeAboutSection();
            
            // Initialize notification settings
            initializeNotificationSettings();
            
            // Initialize log level settings
            initializeLogLevelSettings();
        }
        
        /**
         * Initialize the main forwarding enable/disable toggle
         */
        private void initializeForwardingToggle() {
            // Implementation will be added when we create the preferences XML
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
    }
}