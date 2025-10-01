package com.keremgok.sms;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Analytics Activity for Hermes SMS Forward
 * Displays comprehensive statistics dashboard with privacy-first local analytics
 * No sensitive user data is shown - only anonymized metrics and aggregated statistics
 */
public class AnalyticsActivity extends AppCompatActivity {
    
    private static final String TAG = "AnalyticsActivity";
    
    // UI Components
    private TextView tvTotalReceived;
    private TextView tvTotalForwarded;
    private TextView tvSuccessRate;
    private TextView tvTotalErrors;
    private TextView tvTotalBlocked;
    private TextView tvAvgProcessingTime;
    private TextView tvAppOpens;
    private TextView tvMostCommonError;
    private TextView tvLastUpdateTime;
    private ProgressBar progressSuccessRate;
    private Button btnExport;
    private Button btnRefresh;
    private CardView cardToday;
    private CardView cardWeek;
    private CardView cardMonth;
    
    // Today's stats TextViews
    private TextView tvTodayReceived;
    private TextView tvTodayForwarded;
    private TextView tvTodaySuccessRate;
    private TextView tvTodayErrors;
    
    // Week's stats TextViews
    private TextView tvWeekReceived;
    private TextView tvWeekForwarded;
    private TextView tvWeekSuccessRate;
    private TextView tvWeekErrors;
    
    // Month's stats TextViews
    private TextView tvMonthReceived;
    private TextView tvMonthForwarded;
    private TextView tvMonthSuccessRate;
    private TextView tvMonthErrors;
    
    // SIM statistics TextViews
    private TextView tvSim1Received;
    private TextView tvSim1Forwarded;
    private TextView tvSim1SuccessRate;
    private TextView tvSim2Received;
    private TextView tvSim2Forwarded;
    private TextView tvSim2SuccessRate;
    private TextView tvSimSwitchCount;
    private TextView tvMostUsedSim;
    private CardView cardSimStats;
    private ProgressBar progressSim1Success;
    private ProgressBar progressSim2Success;
    
    private AppDatabase database;
    private StatisticsManager statsManager;
    private boolean isDualSimDevice = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);
        
        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.analytics_dashboard_title));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Initialize database and statistics manager
        database = AppDatabase.getInstance(this);
        statsManager = StatisticsManager.getInstance(this);
        
        initViews();
        setupClickListeners();
        checkDualSimSupport();
        loadStatistics();
    }
    
    /**
     * Initialize all UI views
     */
    private void initViews() {
        // Overall statistics
        tvTotalReceived = findViewById(R.id.tvTotalReceived);
        tvTotalForwarded = findViewById(R.id.tvTotalForwarded);
        tvSuccessRate = findViewById(R.id.tvSuccessRate);
        tvTotalErrors = findViewById(R.id.tvTotalErrors);
        tvTotalBlocked = findViewById(R.id.tvTotalBlocked);
        tvAvgProcessingTime = findViewById(R.id.tvAvgProcessingTime);
        tvAppOpens = findViewById(R.id.tvAppOpens);
        tvMostCommonError = findViewById(R.id.tvMostCommonError);
        tvLastUpdateTime = findViewById(R.id.tvLastUpdateTime);
        progressSuccessRate = findViewById(R.id.progressSuccessRate);
        
        // SIM statistics views - Initialize to null for now (UI layout not implemented yet)
        cardSimStats = null; // findViewById(R.id.cardSimStats);
        tvSim1Received = null; // findViewById(R.id.tvSim1Received);
        tvSim1Forwarded = null; // findViewById(R.id.tvSim1Forwarded);
        tvSim1SuccessRate = null; // findViewById(R.id.tvSim1SuccessRate);
        tvSim2Received = null; // findViewById(R.id.tvSim2Received);
        tvSim2Forwarded = null; // findViewById(R.id.tvSim2Forwarded);
        tvSim2SuccessRate = null; // findViewById(R.id.tvSim2SuccessRate);
        tvSimSwitchCount = null; // findViewById(R.id.tvSimSwitchCount);
        tvMostUsedSim = null; // findViewById(R.id.tvMostUsedSim);
        progressSim1Success = null; // findViewById(R.id.progressSim1Success);
        progressSim2Success = null; // findViewById(R.id.progressSim2Success);
        
        // Action buttons
        btnExport = findViewById(R.id.btnExport);
        btnRefresh = findViewById(R.id.btnRefresh);
        
        // Period cards
        cardToday = findViewById(R.id.cardToday);
        cardWeek = findViewById(R.id.cardWeek);
        cardMonth = findViewById(R.id.cardMonth);
        
        // Today's statistics
        tvTodayReceived = findViewById(R.id.tvTodayReceived);
        tvTodayForwarded = findViewById(R.id.tvTodayForwarded);
        tvTodaySuccessRate = findViewById(R.id.tvTodaySuccessRate);
        tvTodayErrors = findViewById(R.id.tvTodayErrors);
        
        // Week's statistics  
        tvWeekReceived = findViewById(R.id.tvWeekReceived);
        tvWeekForwarded = findViewById(R.id.tvWeekForwarded);
        tvWeekSuccessRate = findViewById(R.id.tvWeekSuccessRate);
        tvWeekErrors = findViewById(R.id.tvWeekErrors);
        
        // Month's statistics
        tvMonthReceived = findViewById(R.id.tvMonthReceived);
        tvMonthForwarded = findViewById(R.id.tvMonthForwarded);
        tvMonthSuccessRate = findViewById(R.id.tvMonthSuccessRate);
        tvMonthErrors = findViewById(R.id.tvMonthErrors);
    }
    
    /**
     * Setup click listeners for interactive elements
     */
    private void setupClickListeners() {
        btnRefresh.setOnClickListener(v -> {
            loadStatistics();
            Toast.makeText(this, getString(R.string.statistics_refreshed), Toast.LENGTH_SHORT).show();
        });
        
        btnExport.setOnClickListener(v -> exportStatistics());
        
        // Cards are display-only for now
    }
    
    /**
     * Load statistics from database and update UI
     */
    private void loadStatistics() {
        ThreadManager.getInstance().executeBackground(() -> {
            try {
                // Generate today's summary to ensure data is up to date
                statsManager.generateTodaysSummary();
                
                // Load overall statistics from SMS history
                SmsHistoryDao historyDao = database.smsHistoryDao();
                int totalReceived = historyDao.getTotalCount();
                int successCount = historyDao.getSuccessCount();
                int failedCount = historyDao.getFailedCount();
                double overallSuccessRate = totalReceived > 0 ? (successCount * 100.0 / totalReceived) : 0.0;
                
                // Load analytics events for additional metrics
                AnalyticsEventDao analyticsDao = database.analyticsEventDao();
                int totalErrors = analyticsDao.getEventCountByType("SMS_ERROR");
                int totalBlocked = analyticsDao.getEventCountByType("SMS_BLOCKED");
                final int appOpens = analyticsDao.getEventCountByType("APP_OPEN");
                
                // Get average processing time from analytics events
                long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                Double avgProcessingTime = analyticsDao.getAvgProcessingTime(thirtyDaysAgo, System.currentTimeMillis());
                if (avgProcessingTime == null) avgProcessingTime = 0.0;
                final Double finalAvgProcessingTime = avgProcessingTime;
                
                // Get most common error
                String commonError = getString(R.string.error_none);
                try {
                    List<AnalyticsEventDao.ErrorCodeCount> errors = analyticsDao.getMostCommonErrors(
                        thirtyDaysAgo, System.currentTimeMillis(), 1);
                    if (!errors.isEmpty()) {
                        commonError = errors.get(0).error_code;
                    }
                } catch (Exception e) {
                    commonError = getString(R.string.error_unknown);
                }
                final String mostCommonError = commonError;
                
                // Load period statistics
                StatisticsSummaryDao summaryDao = database.statisticsSummaryDao();
                
                // Today's stats
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                String today = dateFormat.format(new Date());
                StatisticsSummary todayStats = summaryDao.getSummaryByDateAndType(today, "DAILY");
                
                // Week's stats (last 7 days)
                List<StatisticsSummary> weekStats = summaryDao.getLastNDays(7);
                
                // Month's stats (last 30 days)
                List<StatisticsSummary> monthStats = summaryDao.getLastNDays(30);
                
                // Update UI on main thread
                runOnUiThread(() -> updateUI(
                    totalReceived, successCount, failedCount, overallSuccessRate,
                    totalErrors, totalBlocked, appOpens, finalAvgProcessingTime, mostCommonError,
                    todayStats, weekStats, monthStats
                ));
                
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error loading statistics: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.error_loading_statistics), Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    /**
     * Update UI with loaded statistics
     */
    private void updateUI(int totalReceived, int successCount, int failedCount, double overallSuccessRate,
                         int totalErrors, int totalBlocked, int appOpens, double avgProcessingTime,
                         String mostCommonError, StatisticsSummary todayStats,
                         List<StatisticsSummary> weekStats, List<StatisticsSummary> monthStats) {
        
        // Overall statistics
        tvTotalReceived.setText(String.valueOf(totalReceived));
        tvTotalForwarded.setText(String.valueOf(successCount + failedCount));
        tvSuccessRate.setText(String.format(Locale.US, "%.1f%%", overallSuccessRate));
        tvTotalErrors.setText(String.valueOf(totalErrors));
        tvTotalBlocked.setText(String.valueOf(totalBlocked));
        tvAvgProcessingTime.setText(String.format(Locale.US, "%.0f ms", avgProcessingTime));
        tvAppOpens.setText(String.valueOf(appOpens));
        tvMostCommonError.setText(mostCommonError);
        
        // Update progress bar
        progressSuccessRate.setProgress((int) overallSuccessRate);
        
        // Update last refresh time
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
        tvLastUpdateTime.setText(getString(R.string.last_updated_prefix) + timeFormat.format(new Date()));
        
        // Today's statistics
        if (todayStats != null) {
            tvTodayReceived.setText(String.valueOf(todayStats.getTotalSmsReceived()));
            tvTodayForwarded.setText(String.valueOf(todayStats.getTotalSmsForwarded()));
            tvTodaySuccessRate.setText(String.format(Locale.US, "%.1f%%", todayStats.getSuccessRate()));
            tvTodayErrors.setText(String.valueOf(todayStats.getErrorCount()));
        } else {
            tvTodayReceived.setText(getString(R.string.default_zero));
            tvTodayForwarded.setText(getString(R.string.default_zero));
            tvTodaySuccessRate.setText(getString(R.string.default_zero_percent));
            tvTodayErrors.setText(getString(R.string.default_zero));
        }
        
        // Week's statistics (aggregate from daily summaries)
        int weekReceived = 0, weekForwarded = 0, weekErrors = 0;
        double weekSuccessRate = 0.0;
        for (StatisticsSummary stat : weekStats) {
            weekReceived += stat.getTotalSmsReceived();
            weekForwarded += stat.getTotalSmsForwarded();
            weekErrors += stat.getErrorCount();
        }
        if (weekForwarded > 0) {
            int weekSuccessful = 0;
            for (StatisticsSummary stat : weekStats) {
                weekSuccessful += stat.getSuccessfulForwards();
            }
            weekSuccessRate = weekSuccessful * 100.0 / weekForwarded;
        }
        
        tvWeekReceived.setText(String.valueOf(weekReceived));
        tvWeekForwarded.setText(String.valueOf(weekForwarded));
        tvWeekSuccessRate.setText(String.format(Locale.US, "%.1f%%", weekSuccessRate));
        tvWeekErrors.setText(String.valueOf(weekErrors));
        
        // Month's statistics (aggregate from daily summaries)
        int monthReceived = 0, monthForwarded = 0, monthErrors = 0;
        double monthSuccessRate = 0.0;
        for (StatisticsSummary stat : monthStats) {
            monthReceived += stat.getTotalSmsReceived();
            monthForwarded += stat.getTotalSmsForwarded();
            monthErrors += stat.getErrorCount();
        }
        if (monthForwarded > 0) {
            int monthSuccessful = 0;
            for (StatisticsSummary stat : monthStats) {
                monthSuccessful += stat.getSuccessfulForwards();
            }
            monthSuccessRate = monthSuccessful * 100.0 / monthForwarded;
        }
        
        tvMonthReceived.setText(String.valueOf(monthReceived));
        tvMonthForwarded.setText(String.valueOf(monthForwarded));
        tvMonthSuccessRate.setText(String.format(Locale.US, "%.1f%%", monthSuccessRate));
        tvMonthErrors.setText(String.valueOf(monthErrors));
        
        // Load SIM statistics if dual SIM is supported
        if (isDualSimDevice) {
            loadSimStatistics();
        }
    }
    
    /**
     * Export statistics to external file
     */
    private void exportStatistics() {
        ThreadManager.getInstance().executeBackground(() -> {
            try {
                // Generate CSV content
                StringBuilder csvContent = new StringBuilder();
                csvContent.append(getString(R.string.csv_export_header)).append("\n");
                csvContent.append(getString(R.string.csv_generated_prefix)).append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())).append("\n\n");
                
                // Overall statistics
                csvContent.append(getString(R.string.csv_overall_statistics)).append("\n");
                csvContent.append(getString(R.string.csv_metric_value_header)).append("\n");
                
                SmsHistoryDao historyDao = database.smsHistoryDao();
                int totalReceived = historyDao.getTotalCount();
                int successCount = historyDao.getSuccessCount();
                int failedCount = historyDao.getFailedCount();
                double overallSuccessRate = totalReceived > 0 ? (successCount * 100.0 / totalReceived) : 0.0;
                
                csvContent.append(getString(R.string.csv_total_received)).append(totalReceived).append("\n");
                csvContent.append(getString(R.string.csv_total_forwarded)).append(successCount + failedCount).append("\n");
                csvContent.append(getString(R.string.csv_successful_forwards)).append(successCount).append("\n");
                csvContent.append(getString(R.string.csv_failed_forwards)).append(failedCount).append("\n");
                csvContent.append(getString(R.string.csv_success_rate)).append(String.format(Locale.US, "%.2f%%", overallSuccessRate)).append("\n");
                
                // Get analytics data
                AnalyticsEventDao analyticsDao = database.analyticsEventDao();
                int totalErrors = analyticsDao.getEventCountByType("SMS_ERROR");
                int appOpens = analyticsDao.getEventCountByType("APP_OPEN");
                
                csvContent.append(getString(R.string.csv_total_errors)).append(totalErrors).append("\n");
                csvContent.append(getString(R.string.csv_app_opens)).append(appOpens).append("\n");
                
                // Daily statistics for last 30 days
                csvContent.append("\n").append(getString(R.string.csv_daily_stats_header)).append("\n");
                csvContent.append(getString(R.string.csv_daily_columns)).append("\n");
                
                StatisticsSummaryDao summaryDao = database.statisticsSummaryDao();
                List<StatisticsSummary> dailyStats = summaryDao.getLastNDays(30);
                
                for (StatisticsSummary stat : dailyStats) {
                    csvContent.append(stat.getDate()).append(",");
                    csvContent.append(stat.getTotalSmsReceived()).append(",");
                    csvContent.append(stat.getTotalSmsForwarded()).append(",");
                    csvContent.append(stat.getSuccessfulForwards()).append(",");
                    csvContent.append(stat.getFailedForwards()).append(",");
                    csvContent.append(String.format(Locale.US, "%.2f%%", stat.getSuccessRate())).append(",");
                    csvContent.append(stat.getErrorCount()).append("\n");
                }
                
                // Error analysis
                csvContent.append("\n").append(getString(R.string.csv_error_analysis_header)).append("\n");
                csvContent.append(getString(R.string.csv_error_columns)).append("\n");
                
                long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                try {
                    List<AnalyticsEventDao.ErrorCodeCount> errors = analyticsDao.getMostCommonErrors(
                        thirtyDaysAgo, System.currentTimeMillis(), 10);
                    for (AnalyticsEventDao.ErrorCodeCount error : errors) {
                        csvContent.append(error.error_code).append(",").append(error.count).append("\n");
                    }
                } catch (Exception e) {
                    csvContent.append(getString(R.string.csv_no_error_data)).append("\n");
                }
                
                // Create file in app-specific external storage (no permissions needed)
                java.io.File exportDir = new java.io.File(getExternalFilesDir(null), "exports");
                if (!exportDir.exists()) {
                    exportDir.mkdirs();
                }
                
                String filename = "hermes_analytics_" + 
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".csv";
                java.io.File exportFile = new java.io.File(exportDir, filename);
                
                // Write CSV content to file
                java.io.FileWriter writer = new java.io.FileWriter(exportFile);
                writer.write(csvContent.toString());
                writer.close();
                
                // Share the file
                runOnUiThread(() -> shareExportFile(exportFile));
                
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error exporting statistics: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.export_failed) + ": " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }
    
    /**
     * Share the exported statistics file
     * @param file The CSV file to share
     */
    private void shareExportFile(java.io.File file) {
        try {
            android.net.Uri fileUri = androidx.core.content.FileProvider.getUriForFile(
                this, getPackageName() + ".fileprovider", file);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/csv");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.analytics_email_subject));
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.analytics_email_body));
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_analytics_title)));
            
            Toast.makeText(this, getString(R.string.statistics_exported_successfully), Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error sharing export file: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.export_completed_sharing_failed), Toast.LENGTH_SHORT).show();
        }
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.analytics_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_clear_data) {
            showClearDataConfirmation();
            return true;
        } else if (itemId == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Show confirmation dialog for clearing analytics data
     */
    private void showClearDataConfirmation() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_analytics_data_title))
            .setMessage(getString(R.string.clear_analytics_confirmation_message))
            .setPositiveButton(getString(R.string.clear_button), (dialog, which) -> clearAnalyticsData())
            .setNegativeButton(getString(R.string.cancel_button), null)
            .show();
    }
    
    /**
     * Check if device supports dual SIM
     */
    private void checkDualSimSupport() {
        // Default to hiding SIM stats card
        if (cardSimStats != null) {
            cardSimStats.setVisibility(View.GONE);
        }
        
        // Check dual SIM support in background thread to avoid ANR
        ThreadManager.getInstance().executeBackground(() -> {
            try {
                boolean isDualSim = SimManager.isDualSimSupported(this);
                
                runOnUiThread(() -> {
                    isDualSimDevice = isDualSim;
                    
                    // Show/hide SIM statistics card based on dual SIM support
                    if (cardSimStats != null) {
                        cardSimStats.setVisibility(isDualSimDevice ? View.VISIBLE : View.GONE);
                    }
                });
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error checking dual SIM support: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    isDualSimDevice = false;
                    if (cardSimStats != null) {
                        cardSimStats.setVisibility(View.GONE);
                    }
                });
            }
        });
    }
    
    /**
     * Load SIM-specific statistics
     */
    private void loadSimStatistics() {
        if (!isDualSimDevice) {
            return;
        }
        
        long startTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000); // 30 days ago
        long endTime = System.currentTimeMillis();
        
        statsManager.getSimUsageStatistics(startTime, endTime, new StatisticsManager.SimStatsCallback() {
            @Override
            public void onStatsReady(StatisticsManager.SimUsageStats stats) {
                updateSimStatisticsUI(stats);
            }
            
            @Override
            public void onError(String error) {
                android.util.Log.e(TAG, "Error loading SIM statistics: " + error);
                // Hide SIM stats card on error
                if (cardSimStats != null) {
                    cardSimStats.setVisibility(View.GONE);
                }
            }
        });
    }
    
    /**
     * Update SIM statistics UI
     */
    private void updateSimStatisticsUI(StatisticsManager.SimUsageStats stats) {
        if (!isDualSimDevice || cardSimStats == null) {
            return;
        }
        
        // SIM 1 statistics
        if (tvSim1Received != null) {
            tvSim1Received.setText(String.valueOf(stats.sim1Received));
        }
        if (tvSim1Forwarded != null) {
            tvSim1Forwarded.setText(String.valueOf(stats.sim1Forwarded));
        }
        if (tvSim1SuccessRate != null) {
            tvSim1SuccessRate.setText(String.format(Locale.US, "%.1f%%", stats.sim1SuccessRate));
        }
        if (progressSim1Success != null) {
            progressSim1Success.setProgress((int) stats.sim1SuccessRate);
        }
        
        // SIM 2 statistics
        if (tvSim2Received != null) {
            tvSim2Received.setText(String.valueOf(stats.sim2Received));
        }
        if (tvSim2Forwarded != null) {
            tvSim2Forwarded.setText(String.valueOf(stats.sim2Forwarded));
        }
        if (tvSim2SuccessRate != null) {
            tvSim2SuccessRate.setText(String.format(Locale.US, "%.1f%%", stats.sim2SuccessRate));
        }
        if (progressSim2Success != null) {
            progressSim2Success.setProgress((int) stats.sim2SuccessRate);
        }
        
        // Additional SIM statistics
        if (tvSimSwitchCount != null) {
            tvSimSwitchCount.setText(String.valueOf(stats.simSwitchCount));
        }
        if (tvMostUsedSim != null) {
            String mostUsedText;
            if (stats.mostUsedForwardingSim == 0) {
                mostUsedText = getString(R.string.sim_1);
            } else if (stats.mostUsedForwardingSim == 1) {
                mostUsedText = getString(R.string.sim_2);
            } else {
                mostUsedText = getString(R.string.sim_not_available);
            }
            tvMostUsedSim.setText(mostUsedText);
        }
        
        // Make SIM stats card visible
        cardSimStats.setVisibility(View.VISIBLE);
    }
    
    /**
     * Clear all analytics data from database
     */
    private void clearAnalyticsData() {
        ThreadManager.getInstance().executeBackground(() -> {
            try {
                database.analyticsEventDao().deleteAllEvents();
                database.statisticsSummaryDao().deleteAllSummaries();
                
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.analytics_data_cleared), Toast.LENGTH_SHORT).show();
                    loadStatistics(); // Refresh the display
                });
                
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error clearing analytics data: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, getString(R.string.error_clearing_data), Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh statistics when returning to the activity
        loadStatistics();
    }
}