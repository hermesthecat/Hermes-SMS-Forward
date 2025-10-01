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
        public static final String SMS_BLOCKED = "SMS_BLOCKED";
        public static final String APP_OPEN = "APP_OPEN";
        public static final String APP_CLOSE = "APP_CLOSE";
        public static final String PERMISSION_REQUEST = "PERMISSION_REQUEST";
        public static final String SETTINGS_CHANGE = "SETTINGS_CHANGE";
        public static final String FILTER_APPLIED = "FILTER_APPLIED";
        public static final String PERFORMANCE_METRIC = "PERFORMANCE_METRIC";
        public static final String SIM_SELECTION = "SIM_SELECTION";
        public static final String DUAL_SIM_CONFIG = "DUAL_SIM_CONFIG";
        public static final String DAILY_SIM_SUMMARY = "DAILY_SIM_SUMMARY";
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
     * Record SMS forward success with SIM information
     * @param processingTimeMs Time taken to process the SMS forward
     * @param sourceSimSlot Source SIM slot (-1 if unknown)
     * @param forwardingSimSlot Forwarding SIM slot (-1 if unknown)
     * @param sourceSubscriptionId Source subscription ID (-1 if unknown)
     * @param forwardingSubscriptionId Forwarding subscription ID (-1 if unknown)
     */
    public void recordSmsForwardSuccessWithSim(long processingTimeMs, int sourceSimSlot, 
                                              int forwardingSimSlot, int sourceSubscriptionId, 
                                              int forwardingSubscriptionId) {
        String metadata = String.format(
            "{\"source_sim_slot\":%d,\"forwarding_sim_slot\":%d,\"source_subscription_id\":%d,\"forwarding_subscription_id\":%d}",
            sourceSimSlot, forwardingSimSlot, sourceSubscriptionId, forwardingSubscriptionId);
        recordEvent(EventType.SMS_FORWARD, EventCategory.MESSAGING, EventAction.SUCCESS,
                   processingTimeMs, null, metadata, getAppVersion());
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
     * Record SMS forward failure with SIM information
     * @param errorCode Error code describing the failure
     * @param errorMessage Error message for metadata
     * @param sourceSimSlot Source SIM slot (-1 if unknown)
     * @param forwardingSimSlot Forwarding SIM slot (-1 if unknown)
     * @param sourceSubscriptionId Source subscription ID (-1 if unknown)
     * @param forwardingSubscriptionId Forwarding subscription ID (-1 if unknown)
     */
    public void recordSmsForwardFailureWithSim(String errorCode, String errorMessage,
                                              int sourceSimSlot, int forwardingSimSlot,
                                              int sourceSubscriptionId, int forwardingSubscriptionId) {
        String simInfo = String.format(
            "\"source_sim_slot\":%d,\"forwarding_sim_slot\":%d,\"source_subscription_id\":%d,\"forwarding_subscription_id\":%d",
            sourceSimSlot, forwardingSimSlot, sourceSubscriptionId, forwardingSubscriptionId);
        String metadata = errorMessage != null ? 
            String.format("{\"error\":\"%s\",%s}", errorMessage, simInfo) :
            String.format("{%s}", simInfo);
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
     * Record SMS received with SIM information
     * @param sourceSimSlot Source SIM slot (-1 if unknown)
     * @param sourceSubscriptionId Source subscription ID (-1 if unknown)
     * @param senderNumber Sender phone number (hashed for privacy)
     */
    public void recordSmsReceivedWithSim(int sourceSimSlot, int sourceSubscriptionId, String senderNumber) {
        // Hash sender number for privacy (only last 4 digits)
        String hashedSender = senderNumber != null && senderNumber.length() > 4 ?
            "****" + senderNumber.substring(senderNumber.length() - 4) : "unknown";

        String metadata = String.format(
            "{\"source_sim_slot\":%d,\"source_subscription_id\":%d,\"hashed_sender\":\"%s\"}",
            sourceSimSlot, sourceSubscriptionId, hashedSender);
        recordEvent(EventType.SMS_RECEIVED, EventCategory.MESSAGING, EventAction.SUCCESS,
                   0, null, metadata, getAppVersion());
    }

    /**
     * Record SMS blocked by filter rule
     * @param filterName Name of the filter that blocked the message
     * @param filterType Type of filter (e.g., SENDER, CONTENT)
     */
    public void recordSmsBlocked(String filterName, String filterType) {
        String metadata = String.format(
            "{\"filter_name\":\"%s\",\"filter_type\":\"%s\"}",
            filterName != null ? filterName : "unknown",
            filterType != null ? filterType : "unknown");
        recordEvent(EventType.SMS_BLOCKED, EventCategory.MESSAGING, EventAction.FILTERED,
                   0, null, metadata, getAppVersion());
    }

    /**
     * Record SMS blocked with SIM information
     * @param filterName Name of the filter that blocked the message
     * @param filterType Type of filter
     * @param sourceSimSlot Source SIM slot (-1 if unknown)
     * @param senderNumber Sender phone number (hashed for privacy)
     */
    public void recordSmsBlockedWithSim(String filterName, String filterType, int sourceSimSlot, String senderNumber) {
        // Hash sender number for privacy (only last 4 digits)
        String hashedSender = senderNumber != null && senderNumber.length() > 4 ?
            "****" + senderNumber.substring(senderNumber.length() - 4) : "unknown";

        String metadata = String.format(
            "{\"filter_name\":\"%s\",\"filter_type\":\"%s\",\"source_sim_slot\":%d,\"hashed_sender\":\"%s\"}",
            filterName != null ? filterName : "unknown",
            filterType != null ? filterType : "unknown",
            sourceSimSlot, hashedSender);
        recordEvent(EventType.SMS_BLOCKED, EventCategory.MESSAGING, EventAction.FILTERED,
                   0, null, metadata, getAppVersion());
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

                int totalBlockedCount = analyticsDao.getEventCountByTypeAndAction(
                    EventType.SMS_BLOCKED, EventAction.FILTERED, startTime, endTime);

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
                    totalBlockedCount, mostCommonError, appOpens, totalSessionDuration,
                    avgSessionDuration, System.currentTimeMillis(), System.currentTimeMillis()
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
                        existing.setTotalBlockedCount(totalBlockedCount);
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
     * Get SIM usage statistics
     * @param startTime Start timestamp for analysis period
     * @param endTime End timestamp for analysis period
     * @return SIM usage statistics
     */
    public void getSimUsageStatistics(long startTime, long endTime, SimStatsCallback callback) {
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                SmsHistoryDao historyDao = database.smsHistoryDao();
                
                // Get SIM-based counts from SMS history
                int sim1Received = historyDao.getCountBySourceSim(0);
                int sim2Received = historyDao.getCountBySourceSim(1);
                int sim1Forwarded = historyDao.getCountByForwardingSim(0);
                int sim2Forwarded = historyDao.getCountByForwardingSim(1);
                
                // Get success rates
                double sim1SuccessRate = historyDao.getSuccessRateByForwardingSim(0);
                double sim2SuccessRate = historyDao.getSuccessRateByForwardingSim(1);
                
                // Get SIM switch count (received on one SIM, forwarded via another)
                int simSwitchCount = historyDao.getSimSwitchCount();
                
                // Get most used forwarding SIM
                int mostUsedForwardingSim = historyDao.getMostUsedForwardingSim();
                
                // Get dual SIM statistics
                List<SmsHistory> dualSimStats = historyDao.getDualSimStatistics();
                
                SimUsageStats stats = new SimUsageStats(
                    sim1Received, sim2Received, sim1Forwarded, sim2Forwarded,
                    sim1SuccessRate, sim2SuccessRate, simSwitchCount,
                    mostUsedForwardingSim, dualSimStats.size()
                );
                
                ThreadManager.getInstance().executeOnMainThread(() -> {
                    if (callback != null) {
                        callback.onStatsReady(stats);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting SIM usage statistics: " + e.getMessage(), e);
                ThreadManager.getInstance().executeOnMainThread(() -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * Record SIM selection event
     * @param selectionMode Mode used for SIM selection (auto, source_sim, specific_sim)
     * @param selectedSimSlot Selected SIM slot
     * @param targetNumber Target number (hashed for privacy)
     */
    public void recordSimSelection(String selectionMode, int selectedSimSlot, String targetNumber) {
        String hashedTarget = targetNumber != null && targetNumber.length() > 4 ?
            "****" + targetNumber.substring(targetNumber.length() - 4) : "unknown";
        
        String metadata = String.format(
            "{\"selection_mode\":\"%s\",\"selected_sim_slot\":%d,\"hashed_target\":\"%s\"}",
            selectionMode, selectedSimSlot, hashedTarget);
        recordEvent("SIM_SELECTION", EventCategory.CONFIGURATION, EventAction.SUCCESS,
                   0, null, metadata, getAppVersion());
    }
    
    /**
     * Record dual SIM configuration change
     * @param settingName Name of the setting changed
     * @param oldValue Previous value
     * @param newValue New value
     */
    public void recordDualSimConfigChange(String settingName, String oldValue, String newValue) {
        String metadata = String.format(
            "{\"setting\":\"%s\",\"old_value\":\"%s\",\"new_value\":\"%s\"}",
            settingName, oldValue != null ? oldValue : "null", newValue != null ? newValue : "null");
        recordEvent("DUAL_SIM_CONFIG", EventCategory.CONFIGURATION, EventAction.COMPLETED,
                   0, null, metadata, getAppVersion());
    }
    
    /**
     * Generate enhanced daily summary with SIM statistics
     * @param date Date in YYYY-MM-DD format
     */
    public void generateDailySummaryWithSim(String date) {
        ThreadManager.getInstance().executeBackground(() -> {
            try {
                // Calculate date range for the day
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date targetDate = dateFormat.parse(date);
                long startTime = targetDate.getTime();
                long endTime = startTime + (24 * 60 * 60 * 1000) - 1; // End of day
                
                // Get existing daily summary data
                generateDailySummary(date); // Generate base summary first
                
                // Get SIM-specific statistics for the day
                SmsHistoryDao historyDao = database.smsHistoryDao();
                List<SmsHistory> dayHistory = historyDao.getHistoryByDateRange(startTime, endTime);
                
                // Calculate SIM-specific metrics
                int sim1ReceivedToday = 0;
                int sim2ReceivedToday = 0;
                int sim1ForwardedToday = 0;
                int sim2ForwardedToday = 0;
                int simSwitchesToday = 0;
                
                for (SmsHistory history : dayHistory) {
                    // Count received by SIM
                    if (history.getSourceSimSlot() == 0) sim1ReceivedToday++;
                    else if (history.getSourceSimSlot() == 1) sim2ReceivedToday++;
                    
                    // Count forwarded by SIM
                    if (history.getForwardingSimSlot() == 0) sim1ForwardedToday++;
                    else if (history.getForwardingSimSlot() == 1) sim2ForwardedToday++;
                    
                    // Count SIM switches
                    if (history.getSourceSimSlot() != -1 && history.getForwardingSimSlot() != -1 &&
                        history.getSourceSimSlot() != history.getForwardingSimSlot()) {
                        simSwitchesToday++;
                    }
                }
                
                // Create SIM statistics metadata
                String simMetadata = String.format(
                    "{\"sim1_received\":%d,\"sim2_received\":%d,\"sim1_forwarded\":%d,\"sim2_forwarded\":%d,\"sim_switches\":%d}",
                    sim1ReceivedToday, sim2ReceivedToday, sim1ForwardedToday, sim2ForwardedToday, simSwitchesToday);
                
                // Record SIM summary event
                recordEvent("DAILY_SIM_SUMMARY", EventCategory.PERFORMANCE, EventAction.COMPLETED,
                           0, null, simMetadata, getAppVersion());
                
            } catch (Exception e) {
                Log.e(TAG, "Error generating daily SIM summary: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Clean up resources (call on app destruction)
     */
    public void cleanup() {
        endSession();
        INSTANCE = null;
    }
    
    /**
     * SIM usage statistics data class
     */
    public static class SimUsageStats {
        public final int sim1Received;
        public final int sim2Received;
        public final int sim1Forwarded;
        public final int sim2Forwarded;
        public final double sim1SuccessRate;
        public final double sim2SuccessRate;
        public final int simSwitchCount;
        public final int mostUsedForwardingSim;
        public final int dualSimRecordsCount;
        
        public SimUsageStats(int sim1Received, int sim2Received, int sim1Forwarded, int sim2Forwarded,
                           double sim1SuccessRate, double sim2SuccessRate, int simSwitchCount,
                           int mostUsedForwardingSim, int dualSimRecordsCount) {
            this.sim1Received = sim1Received;
            this.sim2Received = sim2Received;
            this.sim1Forwarded = sim1Forwarded;
            this.sim2Forwarded = sim2Forwarded;
            this.sim1SuccessRate = sim1SuccessRate;
            this.sim2SuccessRate = sim2SuccessRate;
            this.simSwitchCount = simSwitchCount;
            this.mostUsedForwardingSim = mostUsedForwardingSim;
            this.dualSimRecordsCount = dualSimRecordsCount;
        }
        
        @Override
        public String toString() {
            return String.format("SimUsageStats{sim1_rx=%d, sim2_rx=%d, sim1_tx=%d, sim2_tx=%d, " +
                               "sim1_success=%.1f%%, sim2_success=%.1f%%, switches=%d, most_used=%d, records=%d}",
                               sim1Received, sim2Received, sim1Forwarded, sim2Forwarded,
                               sim1SuccessRate, sim2SuccessRate, simSwitchCount, mostUsedForwardingSim, dualSimRecordsCount);
        }
    }
    
    /**
     * Callback interface for SIM statistics
     */
    public interface SimStatsCallback {
        void onStatsReady(SimUsageStats stats);
        void onError(String error);
    }
}