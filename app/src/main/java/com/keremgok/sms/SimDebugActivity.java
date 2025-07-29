package com.keremgok.sms;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.util.List;

/**
 * SimDebugActivity - Debug-only activity for comprehensive SIM information display
 * Only available in debug builds, provides detailed SIM status and diagnostics
 */
public class SimDebugActivity extends AppCompatActivity {
    
    private static final String TAG = "SimDebugActivity";
    
    private TextView tvSimInfo;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Only show in debug builds
        // Only show in debug builds - for now always show
        if (false) {
            finish();
            return;
        }
        
        setContentView(R.layout.activity_sim_debug);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("SIM Debug Information");
        }
        
        tvSimInfo = findViewById(R.id.tv_sim_info);
        
        // Load and display SIM information
        loadSimInformation();
        
        // Log system status for debugging
        SimLogger.logSystemStatus(this);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    /**
     * Load comprehensive SIM information for debug display
     */
    private void loadSimInformation() {
        ThreadManager.getInstance().executeBackground(() -> {
            StringBuilder info = new StringBuilder();
            
            try {
                info.append("=== SIM DEBUG INFORMATION ===\n\n");
                
                // System Information
                info.append("SYSTEM INFO:\n");
                info.append("Android Version: ").append(android.os.Build.VERSION.RELEASE).append("\n");
                info.append("API Level: ").append(android.os.Build.VERSION.SDK_INT).append("\n");
                info.append("Device: ").append(android.os.Build.MANUFACTURER)
                    .append(" ").append(android.os.Build.MODEL).append("\n");
                try {
                    String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                    int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                    info.append("App Version: ").append(versionName)
                        .append(" (").append(versionCode).append(")\n");
                    info.append("Build Type: Debug\n\n");
                } catch (Exception e) {
                    info.append("App Version: Unknown\n");
                    info.append("Build Type: Debug\n\n");
                }
                
                // Dual SIM API Support
                info.append("DUAL SIM API SUPPORT:\n");
                info.append("API Supported: ").append(SimManager.isDualSimApiSupported()).append("\n");
                info.append("Dual SIM Supported: ").append(SimManager.isDualSimSupported(this)).append("\n");
                info.append("Permissions Granted: ").append(SimManager.hasRequiredPermissions(this)).append("\n");
                info.append("Default SMS Sub ID: ").append(SimManager.getDefaultSmsSubscriptionId(this)).append("\n\n");
                
                // Active SIM Cards
                List<SimManager.SimInfo> sims = SimManager.getActiveSimCards(this);
                info.append("ACTIVE SIM CARDS (").append(sims.size()).append("):\n");
                
                if (sims.isEmpty()) {
                    info.append("No active SIM cards detected\n");
                } else {
                    for (int i = 0; i < sims.size(); i++) {
                        SimManager.SimInfo sim = sims.get(i);
                        info.append("SIM ").append(i + 1).append(":\n");
                        info.append("  Slot Index: ").append(sim.slotIndex).append("\n");
                        info.append("  Subscription ID: ").append(sim.subscriptionId).append("\n");
                        info.append("  Display Name: ").append(sim.displayName).append("\n");
                        info.append("  Carrier Name: ").append(sim.carrierName).append("\n");
                        info.append("  Phone Number: ").append(maskPhoneNumber(sim.phoneNumber)).append("\n");
                        info.append("  Active: ").append(sim.isActive).append("\n");
                        info.append("  Valid: ").append(SimManager.isSubscriptionValid(this, sim.subscriptionId)).append("\n");
                        info.append("  Display String: ").append(SimManager.getSimDisplayName(this, sim.slotIndex)).append("\n");
                        info.append("\n");
                    }
                }
                
                // SIM Selection Testing
                info.append("SIM SELECTION TESTING:\n");
                if (sims.size() >= 2) {
                    // Test different selection modes
                    testSimSelection(info, sims);
                } else {
                    info.append("Dual SIM not available for selection testing\n");
                }
                info.append("\n");
                
                // Database Information
                info.append("DATABASE INFORMATION:\n");
                AppDatabase db = AppDatabase.getInstance(this);
                int targetCount = db.targetNumberDao().getEnabledTargetCount();
                info.append("Enabled Target Numbers: ").append(targetCount).append("\n");
                int historyCount = db.smsHistoryDao().getTotalCount();
                info.append("SMS History Records: ").append(historyCount).append("\n");
                
                // Recent SIM-related history
                List<SmsHistory> recentHistory = db.smsHistoryDao().getLatestHistory(10);
                info.append("Recent SMS History (SIM Info):\n");
                for (SmsHistory history : recentHistory) {
                    info.append("  ").append(new java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())
                        .format(new java.util.Date(history.getTimestamp())));
                    info.append(" | SRC_SLOT:").append(history.getSourceSimSlot());
                    info.append(" | FWD_SLOT:").append(history.getForwardingSimSlot());
                    info.append(" | SUCCESS:").append(history.isSuccess()).append("\n");
                }
                
                info.append("\n=== END DEBUG INFO ===");
                
            } catch (Exception e) {
                info.append("ERROR LOADING SIM INFO: ").append(e.getMessage()).append("\n");
                info.append("Stack trace: ").append(android.util.Log.getStackTraceString(e));
            }
            
            // Update UI on main thread
            runOnUiThread(() -> {
                if (tvSimInfo != null) {
                    tvSimInfo.setText(info.toString());
                }
            });
        });
    }
    
    /**
     * Test SIM selection logic with different modes
     */
    private void testSimSelection(StringBuilder info, List<SimManager.SimInfo> sims) {
        try {
            // Create a test target number configuration
            TargetNumber testTarget = new TargetNumber("+1234567890", "Test Target", true, true);
            
            // Test auto mode
            testTarget.setSimSelectionMode("auto");
            SmsSimSelectionHelper.SimSelectionResult autoResult = 
                SmsSimSelectionHelper.determineForwardingSim(this, testTarget.getPhoneNumber(), 
                    sims.get(0).subscriptionId, testTarget);
            info.append("Auto Mode Test: ").append(autoResult.toString()).append("\n");
            
            // Test source_sim mode
            testTarget.setSimSelectionMode("source_sim");
            SmsSimSelectionHelper.SimSelectionResult sourceResult = 
                SmsSimSelectionHelper.determineForwardingSim(this, testTarget.getPhoneNumber(), 
                    sims.get(0).subscriptionId, testTarget);
            info.append("Source SIM Mode Test: ").append(sourceResult.toString()).append("\n");
            
            // Test specific_sim mode
            testTarget.setSimSelectionMode("specific_sim");
            testTarget.setPreferredSimSlot(1);
            SmsSimSelectionHelper.SimSelectionResult specificResult = 
                SmsSimSelectionHelper.determineForwardingSim(this, testTarget.getPhoneNumber(), 
                    sims.get(0).subscriptionId, testTarget);
            info.append("Specific SIM Mode Test: ").append(specificResult.toString()).append("\n");
            
        } catch (Exception e) {
            info.append("Error testing SIM selection: ").append(e.getMessage()).append("\n");
        }
    }
    
    /**
     * Mask phone number for secure display
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "***";
        }
        return phoneNumber.substring(0, 2) + "***" + phoneNumber.substring(phoneNumber.length() - 2);
    }
    
    /**
     * Refresh button click handler
     */
    public void onRefreshClicked(android.view.View view) {
        loadSimInformation();
        SimLogger.logSystemStatus(this);
    }
    
    /**
     * Test SIM selection button click handler
     */
    public void onTestSelectionClicked(android.view.View view) {
        ThreadManager.getInstance().executeBackground(() -> {
            StringBuilder testResults = new StringBuilder();
            testResults.append("\n=== SIM SELECTION TEST RESULTS ===\n\n");
            
            try {
                List<SimManager.SimInfo> sims = SimManager.getActiveSimCards(this);
                if (sims.size() >= 2) {
                    testSimSelection(testResults, sims);
                } else {
                    testResults.append("Dual SIM not available for testing\n");
                }
            } catch (Exception e) {
                testResults.append("Error during testing: ").append(e.getMessage()).append("\n");
            }
            
            runOnUiThread(() -> {
                if (tvSimInfo != null) {
                    tvSimInfo.append(testResults.toString());
                }
            });
        });
    }
    
    /**
     * Launch SIM Debug Activity (debug builds only)
     */
    public static void launch(Context context) {
        // Always show in debug builds
        if (true) {
            Intent intent = new Intent(context, SimDebugActivity.class);
            context.startActivity(intent);
        }
    }
}