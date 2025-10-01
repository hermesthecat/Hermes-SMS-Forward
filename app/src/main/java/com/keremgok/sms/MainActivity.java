package com.keremgok.sms;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import java.util.concurrent.TimeUnit;

/**
 * Main Activity - Dashboard for SMS Forwarding App
 * Displays app status and provides quick access to all features
 */
public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_REQUEST_CODE = 123;

    private TextView tvReceiveSmsStatus;
    private TextView tvSendSmsStatus;
    private TextView tvReadPhoneStateStatus;

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
        StatisticsManager.getInstance(this);

        // Schedule periodic database cleanup
        schedulePeriodicCleanup();

        initViews();
        checkPermissions();
        updateUI();
    }

    private void initViews() {
        tvReceiveSmsStatus = findViewById(R.id.tvReceiveSmsStatus);
        tvSendSmsStatus = findViewById(R.id.tvSendSmsStatus);
        tvReadPhoneStateStatus = findViewById(R.id.tvReadPhoneStateStatus);
    }

    private boolean hasRequiredPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
               ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS,
                           Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG},
                SMS_PERMISSION_REQUEST_CODE);
    }

    private void checkPermissions() {
        if (!hasRequiredPermissions()) {
            requestPermissions();
        }
    }

    private void updateUI() {
        boolean hasReceiveSmsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean hasSendSmsPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean hasReadPhoneStatePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;

        // Update individual permission status indicators
        if (hasReceiveSmsPermission) {
            tvReceiveSmsStatus.setText(getString(R.string.permission_granted_symbol));
            tvReceiveSmsStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            tvReceiveSmsStatus.setText(getString(R.string.permission_denied_symbol));
            tvReceiveSmsStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }

        if (hasSendSmsPermission) {
            tvSendSmsStatus.setText(getString(R.string.permission_granted_symbol));
            tvSendSmsStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            tvSendSmsStatus.setText(getString(R.string.permission_denied_symbol));
            tvSendSmsStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        }

        if (hasReadPhoneStatePermission) {
            tvReadPhoneStateStatus.setText(getString(R.string.permission_granted_symbol));
            tvReadPhoneStateStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
        } else {
            tvReadPhoneStateStatus.setText(getString(R.string.permission_denied_symbol));
            tvReadPhoneStateStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
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
            startActivity(new Intent(this, TargetNumbersActivity.class));
            return true;
        } else if (itemId == R.id.action_filter_rules) {
            startActivity(new Intent(this, FilterRulesActivity.class));
            return true;
        } else if (itemId == R.id.action_history) {
            startActivity(new Intent(this, HistoryActivity.class));
            return true;
        } else if (itemId == R.id.action_analytics) {
            startActivity(new Intent(this, AnalyticsActivity.class));
            return true;
        } else if (itemId == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (itemId == R.id.action_sim_debug) {
            SimDebugActivity.launch(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StatisticsManager.getInstance(this).endSession();
    }

    /**
     * Schedule periodic database cleanup using WorkManager
     * Runs once per day to delete old SMS history and analytics events
     * based on retention period configured in Settings
     */
    private void schedulePeriodicCleanup() {
        // Create constraints: don't run on low battery
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build();

        // Create periodic work request (runs once per day)
        PeriodicWorkRequest cleanupWork = new PeriodicWorkRequest.Builder(
                CleanupWorker.class,
                1, TimeUnit.DAYS  // Repeat every 24 hours
        )
        .setConstraints(constraints)
        .build();

        // Schedule the work (KEEP policy means don't restart if already scheduled)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "database_cleanup",
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupWork
        );

        android.util.Log.i("MainActivity", "Periodic database cleanup scheduled (runs daily)");
    }
}
