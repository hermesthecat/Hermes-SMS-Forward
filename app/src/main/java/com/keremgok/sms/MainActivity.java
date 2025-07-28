package com.keremgok.sms;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    
    private static final int SMS_PERMISSION_REQUEST_CODE = 123;
    private static final String PREFS_NAME = "HermesPrefs";
    private static final String KEY_TARGET_NUMBER = "target_number";
    
    private EditText etTargetNumber;
    private Button btnSave;
    private TextView tvStatus;
    private TextView tvReceiveSmsStatus;
    private TextView tvSendSmsStatus;
    private TextView tvValidation;
    private SharedPreferences prefs;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if onboarding is completed
        if (!OnboardingActivity.isOnboardingCompleted(this)) {
            Intent onboardingIntent = new Intent(this, OnboardingActivity.class);
            startActivity(onboardingIntent);
            finish();
            return;
        }
        
        setContentView(R.layout.activity_main);
        
        // Initialize StatisticsManager for analytics tracking
        StatisticsManager statsManager = StatisticsManager.getInstance(this);
        
        initViews();
        setupPreferences();
        setupValidation();
        checkPermissions();
        loadSavedNumber();
        updateUI();
        
        // Initialize performance monitoring for Task 15 optimization testing
        initializePerformanceMonitoring();
        
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTargetNumber();
            }
        });
    }
    
    private void initViews() {
        etTargetNumber = findViewById(R.id.etTargetNumber);
        btnSave = findViewById(R.id.btnSave);
        tvStatus = findViewById(R.id.tvStatus);
        tvReceiveSmsStatus = findViewById(R.id.tvReceiveSmsStatus);
        tvSendSmsStatus = findViewById(R.id.tvSendSmsStatus);
        tvValidation = findViewById(R.id.tvValidation);
    }
    
    private void setupPreferences() {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
    }
    
    private void setupValidation() {
        etTargetNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                validatePhoneNumber();
            }
        });
    }
    
    private void validatePhoneNumber() {
        String phoneNumber = etTargetNumber.getText().toString().trim();
        PhoneNumberValidator.ValidationResult result = PhoneNumberValidator.validate(phoneNumber);
        
        updateValidationUI(result);
        updateSaveButtonState(result);
    }
    
    private void updateValidationUI(PhoneNumberValidator.ValidationResult result) {
        if (TextUtils.isEmpty(etTargetNumber.getText().toString().trim())) {
            // Don't show validation message for empty input
            tvValidation.setVisibility(View.GONE);
            return;
        }
        
        if (result.isValid()) {
            tvValidation.setText(R.string.validation_success);
            tvValidation.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            tvValidation.setVisibility(View.VISIBLE);
        } else {
            String errorMessage = getValidationMessage(result.getErrorCode());
            tvValidation.setText(errorMessage);
            tvValidation.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            tvValidation.setVisibility(View.VISIBLE);
        }
    }
    
    private void updateSaveButtonState(PhoneNumberValidator.ValidationResult result) {
        btnSave.setEnabled(result.isValid() && hasRequiredPermissions());
    }
    
    private String getValidationMessage(String errorCode) {
        if (errorCode == null) return "";
        
        switch (errorCode) {
            case "EMPTY_NUMBER":
                return getString(R.string.validation_empty_number);
            case "MISSING_COUNTRY_CODE":
                return getString(R.string.validation_missing_country_code);
            case "TOO_SHORT":
                return getString(R.string.validation_too_short);
            case "TOO_LONG":
                return getString(R.string.validation_too_long);
            case "INVALID_FORMAT":
                return getString(R.string.validation_invalid_format);
            default:
                return getString(R.string.validation_invalid_format);
        }
    }
    
    private void loadSavedNumber() {
        String savedNumber = prefs.getString(KEY_TARGET_NUMBER, "");
        etTargetNumber.setText(savedNumber);
    }
    
    private void saveTargetNumber() {
        String targetNumber = etTargetNumber.getText().toString().trim();
        
        // Validate phone number using PhoneNumberValidator
        PhoneNumberValidator.ValidationResult result = PhoneNumberValidator.validate(targetNumber);
        
        if (!result.isValid()) {
            String errorMessage = getValidationMessage(result.getErrorCode());
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!hasRequiredPermissions()) {
            requestPermissions();
            return;
        }
        
        // Use formatted number if available (for domestic Turkey numbers)
        String numberToSave = result.getFormattedNumber() != null ? 
                              result.getFormattedNumber() : targetNumber;
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_TARGET_NUMBER, numberToSave);
        editor.apply();
        
        Toast.makeText(this, R.string.success_saved, Toast.LENGTH_SHORT).show();
        updateUI();
    }
    
    private boolean hasRequiredPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS},
                SMS_PERMISSION_REQUEST_CODE);
    }
    
    private void checkPermissions() {
        if (!hasRequiredPermissions()) {
            requestPermissions();
        }
    }
    
    private void updateUI() {
        String savedNumber = prefs.getString(KEY_TARGET_NUMBER, "");
        boolean hasReceiveSmsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean hasSendSmsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean hasAllPermissions = hasReceiveSmsPermission && hasSendSmsPermission;
        
        // Update individual permission status indicators
        if (hasReceiveSmsPermission) {
            tvReceiveSmsStatus.setText("✅");
            tvReceiveSmsStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvReceiveSmsStatus.setText("❌");
            tvReceiveSmsStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        if (hasSendSmsPermission) {
            tvSendSmsStatus.setText("✅");
            tvSendSmsStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvSendSmsStatus.setText("❌");
            tvSendSmsStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
        
        if (!TextUtils.isEmpty(savedNumber) && hasAllPermissions) {
            tvStatus.setText(R.string.status_configured);
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            tvStatus.setText(R.string.status_not_configured);
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            StatisticsManager statsManager = StatisticsManager.getInstance(this);
            
            boolean allPermissionsGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                boolean granted = i < grantResults.length && grantResults[i] == PackageManager.PERMISSION_GRANTED;
                
                // Record each permission result
                statsManager.recordPermissionRequest(permissions[i], granted);
                
                if (!granted) {
                    allPermissionsGranted = false;
                }
            }
            
            if (allPermissionsGranted) {
                Toast.makeText(this, getString(R.string.permissions_granted_message), Toast.LENGTH_LONG).show();
                saveTargetNumber();
            } else {
                Toast.makeText(this, getString(R.string.sms_permissions_required), Toast.LENGTH_LONG).show();
            }
            
            updateUI();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.action_target_numbers) {
            // Launch Target Numbers Activity
            Intent targetNumbersIntent = new Intent(this, TargetNumbersActivity.class);
            startActivity(targetNumbersIntent);
            return true;
        } else if (itemId == R.id.action_filter_rules) {
            // Launch Filter Rules Activity
            Intent filterRulesIntent = new Intent(this, FilterRulesActivity.class);
            startActivity(filterRulesIntent);
            return true;
        } else if (itemId == R.id.action_history) {
            // Launch History Activity
            Intent historyIntent = new Intent(this, HistoryActivity.class);
            startActivity(historyIntent);
            return true;
        } else if (itemId == R.id.action_analytics) {
            // Launch Analytics Activity
            Intent analyticsIntent = new Intent(this, AnalyticsActivity.class);
            startActivity(analyticsIntent);
            return true;
        } else if (itemId == R.id.action_settings) {
            // Launch Settings Activity
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Clean up StatisticsManager when app is destroyed
        StatisticsManager statsManager = StatisticsManager.getInstance(this);
        statsManager.endSession();
    }
    
    /**
     * Initialize performance monitoring for Task 15 optimization testing
     * This method runs performance tests to validate our optimizations
     */
    private void initializePerformanceMonitoring() {
        if (!PerformanceMonitor.isMonitoringEnabled()) {
            return;
        }
        
        PerformanceMonitor monitor = PerformanceMonitor.getInstance();
        monitor.reset();
        monitor.logCurrentStatus(this, "App Start");
        
        // Run performance test in background thread to avoid blocking UI
        ThreadManager.getInstance().executeBackground(() -> {
            try {
                // Wait a bit for app to fully initialize
                Thread.sleep(2000);
                
                // Run comprehensive performance test
                monitor.runPerformanceTest(this);
                
                // Check for memory leaks
                monitor.checkMemoryLeaks(this);
                
                // Log final status
                monitor.logCurrentStatus(this, "Performance Test Complete");
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Performance monitoring error: " + e.getMessage());
            }
        });
    }
}