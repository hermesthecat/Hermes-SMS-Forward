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
    
    private AppDatabase database;
    private StatisticsManager statsManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);
        
        // Set up toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Analytics Dashboard");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // Initialize database and statistics manager
        database = AppDatabase.getInstance(this);
        statsManager = StatisticsManager.getInstance(this);
        
        initViews();
        setupClickListeners();
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
        tvAvgProcessingTime = findViewById(R.id.tvAvgProcessingTime);
        tvAppOpens = findViewById(R.id.tvAppOpens);
        tvMostCommonError = findViewById(R.id.tvMostCommonError);
        tvLastUpdateTime = findViewById(R.id.tvLastUpdateTime);
        progressSuccessRate = findViewById(R.id.progressSuccessRate);
        
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
            Toast.makeText(this, "Statistics refreshed", Toast.LENGTH_SHORT).show();
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
                final int appOpens = analyticsDao.getEventCountByType("APP_OPEN");
                
                // Get average processing time from analytics events
                long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                Double avgProcessingTime = analyticsDao.getAvgProcessingTime(thirtyDaysAgo, System.currentTimeMillis());
                if (avgProcessingTime == null) avgProcessingTime = 0.0;
                final Double finalAvgProcessingTime = avgProcessingTime;
                
                // Get most common error
                String commonError = "None";
                try {
                    List<AnalyticsEventDao.ErrorCodeCount> errors = analyticsDao.getMostCommonErrors(
                        thirtyDaysAgo, System.currentTimeMillis(), 1);
                    if (!errors.isEmpty()) {
                        commonError = errors.get(0).error_code;
                    }
                } catch (Exception e) {
                    commonError = "Unknown";
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
                    totalErrors, appOpens, finalAvgProcessingTime, mostCommonError,
                    todayStats, weekStats, monthStats
                ));
                
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error loading statistics: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Error loading statistics", Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    /**
     * Update UI with loaded statistics
     */
    private void updateUI(int totalReceived, int successCount, int failedCount, double overallSuccessRate,
                         int totalErrors, int appOpens, double avgProcessingTime, String mostCommonError,
                         StatisticsSummary todayStats, List<StatisticsSummary> weekStats, 
                         List<StatisticsSummary> monthStats) {
        
        // Overall statistics
        tvTotalReceived.setText(String.valueOf(totalReceived));
        tvTotalForwarded.setText(String.valueOf(successCount + failedCount));
        tvSuccessRate.setText(String.format(Locale.US, "%.1f%%", overallSuccessRate));
        tvTotalErrors.setText(String.valueOf(totalErrors));
        tvAvgProcessingTime.setText(String.format(Locale.US, "%.0f ms", avgProcessingTime));
        tvAppOpens.setText(String.valueOf(appOpens));
        tvMostCommonError.setText(mostCommonError);
        
        // Update progress bar
        progressSuccessRate.setProgress((int) overallSuccessRate);
        
        // Update last refresh time
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
        tvLastUpdateTime.setText("Last updated: " + timeFormat.format(new Date()));
        
        // Today's statistics
        if (todayStats != null) {
            tvTodayReceived.setText(String.valueOf(todayStats.getTotalSmsReceived()));
            tvTodayForwarded.setText(String.valueOf(todayStats.getTotalSmsForwarded()));
            tvTodaySuccessRate.setText(String.format(Locale.US, "%.1f%%", todayStats.getSuccessRate()));
            tvTodayErrors.setText(String.valueOf(todayStats.getErrorCount()));
        } else {
            tvTodayReceived.setText("0");
            tvTodayForwarded.setText("0");
            tvTodaySuccessRate.setText("0%");
            tvTodayErrors.setText("0");
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
    }
    
    /**
     * Export statistics to external file
     */
    private void exportStatistics() {
        ThreadManager.getInstance().executeBackground(() -> {
            try {
                // Generate CSV content
                StringBuilder csvContent = new StringBuilder();
                csvContent.append("Hermes SMS Forward - Analytics Export\n");
                csvContent.append("Generated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date())).append("\n\n");
                
                // Overall statistics
                csvContent.append("OVERALL STATISTICS\n");
                csvContent.append("Metric,Value\n");
                
                SmsHistoryDao historyDao = database.smsHistoryDao();
                int totalReceived = historyDao.getTotalCount();
                int successCount = historyDao.getSuccessCount();
                int failedCount = historyDao.getFailedCount();
                double overallSuccessRate = totalReceived > 0 ? (successCount * 100.0 / totalReceived) : 0.0;
                
                csvContent.append("Total SMS Received,").append(totalReceived).append("\n");
                csvContent.append("Total SMS Forwarded,").append(successCount + failedCount).append("\n");
                csvContent.append("Successful Forwards,").append(successCount).append("\n");
                csvContent.append("Failed Forwards,").append(failedCount).append("\n");
                csvContent.append("Success Rate,").append(String.format(Locale.US, "%.2f%%", overallSuccessRate)).append("\n");
                
                // Get analytics data
                AnalyticsEventDao analyticsDao = database.analyticsEventDao();
                int totalErrors = analyticsDao.getEventCountByType("SMS_ERROR");
                int appOpens = analyticsDao.getEventCountByType("APP_OPEN");
                
                csvContent.append("Total Errors,").append(totalErrors).append("\n");
                csvContent.append("App Opens,").append(appOpens).append("\n");
                
                // Daily statistics for last 30 days
                csvContent.append("\nDAILY STATISTICS (Last 30 Days)\n");
                csvContent.append("Date,Received,Forwarded,Successful,Failed,Success Rate,Errors\n");
                
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
                csvContent.append("\nERROR ANALYSIS (Last 30 Days)\n");
                csvContent.append("Error Code,Count\n");
                
                long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
                try {
                    List<AnalyticsEventDao.ErrorCodeCount> errors = analyticsDao.getMostCommonErrors(
                        thirtyDaysAgo, System.currentTimeMillis(), 10);
                    for (AnalyticsEventDao.ErrorCodeCount error : errors) {
                        csvContent.append(error.error_code).append(",").append(error.count).append("\n");
                    }
                } catch (Exception e) {
                    csvContent.append("No error data available\n");
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
                runOnUiThread(() -> Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
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
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Hermes SMS Forward - Analytics Export");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Analytics data exported from Hermes SMS Forward app.");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Share Analytics Export"));
            
            Toast.makeText(this, "Statistics exported successfully", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error sharing export file: " + e.getMessage(), e);
            Toast.makeText(this, "Export completed but sharing failed", Toast.LENGTH_SHORT).show();
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
            .setTitle("Clear Analytics Data")
            .setMessage("Are you sure you want to clear all analytics data? This action cannot be undone.")
            .setPositiveButton("Clear", (dialog, which) -> clearAnalyticsData())
            .setNegativeButton("Cancel", null)
            .show();
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
                    Toast.makeText(this, "Analytics data cleared", Toast.LENGTH_SHORT).show();
                    loadStatistics(); // Refresh the display
                });
                
            } catch (Exception e) {
                android.util.Log.e(TAG, "Error clearing analytics data: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(this, "Error clearing data", Toast.LENGTH_SHORT).show());
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