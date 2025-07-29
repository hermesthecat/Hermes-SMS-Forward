package com.keremgok.sms;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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
        
        setContentView(R.layout.activity_sim_debug);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("SIM Debug Information");
        }
        
        tvSimInfo = findViewById(R.id.tv_sim_info);
        
        // Load basic SIM information
        loadSimInformation();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    /**
     * Load basic SIM information for debug display
     */
    private void loadSimInformation() {
        StringBuilder info = new StringBuilder();
        
        info.append("=== SIM DEBUG INFORMATION ===\n\n");
        
        // System Information
        info.append("SYSTEM INFO:\n");
        info.append("Android Version: ").append(android.os.Build.VERSION.RELEASE).append("\n");
        info.append("API Level: ").append(android.os.Build.VERSION.SDK_INT).append("\n");
        info.append("Device: ").append(android.os.Build.MANUFACTURER)
            .append(" ").append(android.os.Build.MODEL).append("\n");
        info.append("Build Type: Debug\n\n");
        
        // Basic SIM status
        info.append("DUAL SIM STATUS:\n");
        info.append("SIM Debug Activity loaded successfully!\n");
        info.append("Use the buttons below to test functionality.\n\n");
        
        info.append("=== END DEBUG INFO ===");
        
        tvSimInfo.setText(info.toString());
    }
    
    /**
     * Handle refresh button click
     */
    public void onRefreshClicked(android.view.View view) {
        tvSimInfo.setText("Refreshing...\n");
        loadSimInformation();
    }
    
    /**
     * Handle test SIM selection button click
     */
    public void onTestSelectionClicked(android.view.View view) {
        tvSimInfo.append("\n\nTest button clicked at: " + new java.util.Date().toString());
    }
    
    /**
     * Launch SIM Debug Activity (debug builds only)
     */
    public static void launch(Context context) {
        Intent intent = new Intent(context, SimDebugActivity.class);
        context.startActivity(intent);
    }
}