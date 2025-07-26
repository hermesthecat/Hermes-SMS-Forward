package com.keremgok.sms;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Statistics Manager for Hermes SMS Forward
 * Handles privacy-first local analytics and performance monitoring
 * No sensitive user data is stored or transmitted
 */
public class StatisticsManager {
    
    private static final String TAG = "StatisticsManager";
    private static final String PREFS_NAME = "HermesAnalytics";
    private static final String KEY_SESSION_ID = "current_session_id";
    private static final String KEY_SESSION_START = "session_start_time";
    private static final String KEY_APP_OPENS = "total_app_opens";
    
    private static volatile StatisticsManager INSTANCE;
    private final Context context;
    private final AppDatabase database;
    private final SharedPreferences prefs;
    private final AtomicReference<String> currentSessionId = new AtomicReference<>();
    private final AtomicReference<Long> sessionStartTime = new AtomicReference<>();
    
    // Event Types
    public static class EventType {
        public static final String SMS_RECEIVED = "SMS_RECEIVED";
        public static final String SMS_FORWARD = "SMS_FORWARD";
        public static final String SMS_ERROR = "SMS_ERROR";
        public static final String APP_OPEN = "APP_OPEN";
        public static final String APP_CLOSE = "APP_CLOSE";
        public static final String PERMISSION_REQUEST = "PERMISSION_REQUEST";
        public static final String SETTINGS_CHANGE = "SETTINGS_CHANGE";
        public static final String FILTER_APPLIED = "FILTER_APPLIED";
        public static final String PERFORMANCE_METRIC = "PERFORMANCE_METRIC";
    }
    
    // Event Categories
    public static class EventCategory {
        public static final String MESSAGING = "MESSAGING";
        public static final String UI = "UI";
        public static final String PERMISSION = "PERMISSION";
        public static final String PERFORMANCE = "PERFORMANCE";
        public static final String ERROR = "ERROR";
        public static final String CONFIGURATION = "CONFIGURATION";
    }
    
    // Event Actions
    public static class EventAction {
        public static final String SUCCESS = "SUCCESS";
        public static final String FAILURE = "FAILURE";
        public static final String TIMEOUT = "TIMEOUT";
        public static final String RETRY = "RETRY";
        public static final String GRANTED = "GRANTED";
        public static final String DENIED = "DENIED";
        public static final String FILTERED = "FILTERED";
        public static final String STARTED = "STARTED";
        public static final String COMPLETED = "COMPLETED";
    }
    
    // Error Codes
    public static class ErrorCode {
        public static final String SMS_SEND_FAILED = "SMS_SEND_FAILED";
        public static final String SMS_SERVICE_UNAVAILABLE = "SMS_SERVICE_UNAVAILABLE";
        public static final String NO_TARGET_NUMBERS = "NO_TARGET_NUMBERS";
        public static final String PERMISSION_MISSING = "PERMISSION_MISSING";
        public static final String NETWORK_ERROR = "NETWORK_ERROR";
        public static final String UNKNOWN_ERROR = "UNKNOWN_ERROR";
    }
    
    private StatisticsManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = AppDatabase.getInstance(context);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        initializeSession();
    }
    
    /**
     * Get singleton instance of StatisticsManager
     * @param context Application context
     * @return StatisticsManager instance
     */
    public static StatisticsManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (StatisticsManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new StatisticsManager(context);
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Initialize or resume analytics session
     */
    private void initializeSession() {
        String sessionId = generateSessionId();
        long startTime = System.currentTimeMillis();
        
        currentSessionId.set(sessionId);
        sessionStartTime.set(startTime);
        
        prefs.edit()
            .putString(KEY_SESSION_ID, sessionId)
            .putLong(KEY_SESSION_START, startTime)
            .apply();
        
        // Increment app opens counter
        int appOpens = prefs.getInt(KEY_APP_OPENS, 0) + 1;
        prefs.edit().putInt(KEY_APP_OPENS, appOpens).apply();
        
        // Log app open event
        recordEvent(EventType.APP_OPEN, EventCategory.UI, EventAction.STARTED, 
                   0, null, null, getAppVersion());
    }
    
    /**
     * Generate unique session identifier
     * @return Anonymous session ID
     */
    private String generateSessionId() {
        return "session_" + UUID.randomUUID().toString().substring(0, 8) + 
               "_" + System.currentTimeMillis();
    }
    
    /**
     * Get current app version
     * @return App version string
     */
    private String getAppVersion() {
        try {
            return context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "unknown";
        }
    }
    
    /**
     * Record analytics event
     * @param eventType Type of event
     * @param eventCategory Category of event
     * @param eventAction Action performed
     * @param durationMs Duration in milliseconds (0 if not applicable)
     * @param errorCode Error code if applicable
     * @param metadata Additional metadata as JSON string
     * @param appVersion App version when event occurred
     */
    public void recordEvent(String eventType, String eventCategory, String eventAction,
                          long durationMs, String errorCode, String metadata, String appVersion) {
        ThreadManager.getInstance().executeBackground(() -> {
            try {
                AnalyticsEvent event = new AnalyticsEvent(
                    eventType,
                    eventCategory, 
                    eventAction,
                    System.currentTimeMillis(),
                    durationMs,
                    errorCode,
                    metadata,
                    appVersion != null ? appVersion : getAppVersion(),
                    currentSessionId.get()
                );
                
                database.analyticsEventDao().insert(event);
                
                // Debug logging removed for compatibility
                
            } catch (Exception e) {
                Log.e(TAG, "Error recording analytics event: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Record SMS forward success
     * @param processingTimeMs Time taken to process the SMS forward
     */
    public void recordSmsForwardSuccess(long processingTimeMs) {
        recordEvent(EventType.SMS_FORWARD, EventCategory.MESSAGING, EventAction.SUCCESS,
                   processingTimeMs, null, null, getAppVersion());
    }
    
    /**
     * Record SMS forward failure
     * @param errorCode Error code describing the failure
     * @param errorMessage Error message for metadata
     */
    public void recordSmsForwardFailure(String errorCode, String errorMessage) {
        String metadata = errorMessage != null ? "{\"error\":\"" + errorMessage + "\"}" : null;
        recordEvent(EventType.SMS_FORWARD, EventCategory.MESSAGING, EventAction.FAILURE,
                   0, errorCode, metadata, getAppVersion());
    }
    
    /**
     * Record SMS received (for statistics)
     */
    public void recordSmsReceived() {
        recordEvent(EventType.SMS_RECEIVED, EventCategory.MESSAGING, EventAction.SUCCESS,
                   0, null, null, getAppVersion());
    }
    
    /**
     * Record permission request result
     * @param permission Permission name
     * @param granted Whether permission was granted
     */
    public void recordPermissionRequest(String permission, boolean granted) {
        String action = granted ? EventAction.GRANTED : EventAction.DENIED;
        String metadata = "{\"permission\":\"" + permission + "\"}";
        recordEvent(EventType.PERMISSION_REQUEST, EventCategory.PERMISSION, action,
                   0, null, metadata, getAppVersion());
    }
    
    /**
     * Record performance metric
     * @param metricName Name of the performance metric
     * @param value Metric value
     * @param unit Unit of measurement
     */
    public void recordPerformanceMetric(String metricName, double value, String unit) {
        String metadata = String.format("{\"metric\":\"%s\",\"value\":%f,\"unit\":\"%s\"}", 
                                       metricName, value, unit);
        recordEvent(EventType.PERFORMANCE_METRIC, EventCategory.PERFORMANCE, EventAction.COMPLETED,
                   0, null, metadata, getAppVersion());
    }
    
    /**
     * Record filter application
     * @param filterType Type of filter applied
     * @param action Action taken (FILTERED, ALLOWED)
     */
    public void recordFilterApplication(String filterType, String action) {
        String metadata = "{\"filter_type\":\"" + filterType + "\"}";
        recordEvent(EventType.FILTER_APPLIED, EventCategory.MESSAGING, action,
                   0, null, metadata, getAppVersion());
    }
    
    /**
     * End current session and record session duration
     */
    public void endSession() {
        Long startTime = sessionStartTime.get();
        if (startTime != null) {
            long sessionDuration = System.currentTimeMillis() - startTime;
            recordEvent(EventType.APP_CLOSE, EventCategory.UI, EventAction.COMPLETED,
                       sessionDuration, null, null, getAppVersion());
        }
    }
    
    /**
     * Generate daily statistics summary
     * @param date Date in YYYY-MM-DD format
     */
    public void generateDailySummary(String date) {
        ThreadManager.getInstance().executeBackground(() -> {
            try {
                // Calculate date range for the day
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date targetDate = dateFormat.parse(date);
                long startTime = targetDate.getTime();
                long endTime = startTime + (24 * 60 * 60 * 1000) - 1; // End of day
                
                // Get analytics data for the day
                AnalyticsEventDao analyticsDao = database.analyticsEventDao();
                
                int totalSmsReceived = analyticsDao.getEventCountByTypeAndAction(
                    EventType.SMS_RECEIVED, EventAction.SUCCESS, startTime, endTime);
                
                int successfulForwards = analyticsDao.getEventCountByTypeAndAction(
                    EventType.SMS_FORWARD, EventAction.SUCCESS, startTime, endTime);
                
                int failedForwards = analyticsDao.getEventCountByTypeAndAction(
                    EventType.SMS_FORWARD, EventAction.FAILURE, startTime, endTime);
                
                int totalForwards = successfulForwards + failedForwards;
                double successRate = totalForwards > 0 ? (successfulForwards * 100.0 / totalForwards) : 0.0;
                
                Double avgProcessingTime = analyticsDao.getAvgProcessingTime(startTime, endTime);
                if (avgProcessingTime == null) avgProcessingTime = 0.0;
                
                int errorCount = analyticsDao.getEventCountByTypeAndAction(
                    EventType.SMS_ERROR, EventAction.FAILURE, startTime, endTime);
                
                int appOpens = analyticsDao.getEventCountByTypeAndAction(
                    EventType.APP_OPEN, EventAction.STARTED, startTime, endTime);
                
                // Get most common error
                String mostCommonError = "";
                try {
                    List<AnalyticsEventDao.ErrorCodeCount> errorCounts = analyticsDao.getMostCommonErrors(startTime, endTime, 1);
                    if (!errorCounts.isEmpty()) {
                        mostCommonError = errorCounts.get(0).error_code;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Could not get most common error: " + e.getMessage());
                }
                
                // Calculate session metrics
                long totalSessionDuration = 0;
                double avgSessionDuration = 0.0;
                // TODO: Calculate from APP_CLOSE events with duration
                
                // Create or update daily summary
                StatisticsSummary summary = new StatisticsSummary(
                    date, "DAILY", totalSmsReceived, totalForwards, successfulForwards,
                    failedForwards, successRate, avgProcessingTime, errorCount,
                    mostCommonError, appOpens, totalSessionDuration, avgSessionDuration,
                    System.currentTimeMillis(), System.currentTimeMillis()
                );
                
                // Check if summary already exists
                StatisticsSummaryDao summaryDao = database.statisticsSummaryDao();
                if (summaryDao.summaryExists(date, "DAILY") > 0) {
                    // Update existing summary
                    StatisticsSummary existing = summaryDao.getSummaryByDateAndType(date, "DAILY");
                    if (existing != null) {
                        existing.setTotalSmsReceived(totalSmsReceived);
                        existing.setTotalSmsForwarded(totalForwards);
                        existing.setSuccessfulForwards(successfulForwards);
                        existing.setFailedForwards(failedForwards);
                        existing.setSuccessRate(successRate);
                        existing.setAvgProcessingTimeMs(avgProcessingTime);
                        existing.setErrorCount(errorCount);
                        existing.setMostCommonError(mostCommonError);
                        existing.setAppOpens(appOpens);
                        existing.setLastUpdated(System.currentTimeMillis());
                        summaryDao.update(existing);
                    }
                } else {
                    // Insert new summary
                    summaryDao.insert(summary);
                }
                
                // Debug logging removed for compatibility
                
            } catch (Exception e) {
                Log.e(TAG, "Error generating daily summary: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Generate daily summary for today
     */
    public void generateTodaysSummary() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String today = dateFormat.format(new Date());
        generateDailySummary(today);
    }
    
    /**
     * Get current session statistics
     * @return Session statistics as metadata string
     */
    public String getCurrentSessionStats() {
        Long startTime = sessionStartTime.get();
        if (startTime == null) return "{}";
        
        long sessionDuration = System.currentTimeMillis() - startTime;
        return String.format("{\"session_id\":\"%s\",\"duration_ms\":%d}", 
                           currentSessionId.get(), sessionDuration);
    }
    
    /**
     * Clean up resources (call on app destruction)
     */
    public void cleanup() {
        endSession();
        INSTANCE = null;
    }
}