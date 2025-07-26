package com.keremgok.sms;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

/**
 * Statistics Summary Entity for Room Database
 * Stores aggregated daily/weekly/monthly statistics for dashboard
 * Privacy-first approach - no sensitive user data stored
 */
@Entity(tableName = "statistics_summary")
public class StatisticsSummary {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @ColumnInfo(name = "date")
    private String date; // Format: YYYY-MM-DD for daily, YYYY-MM-WXX for weekly, YYYY-MM for monthly
    
    @ColumnInfo(name = "summary_type")
    private String summaryType; // DAILY, WEEKLY, MONTHLY
    
    @ColumnInfo(name = "total_sms_received")
    private int totalSmsReceived;
    
    @ColumnInfo(name = "total_sms_forwarded")
    private int totalSmsForwarded;
    
    @ColumnInfo(name = "successful_forwards")
    private int successfulForwards;
    
    @ColumnInfo(name = "failed_forwards")
    private int failedForwards;
    
    @ColumnInfo(name = "success_rate")
    private double successRate; // Percentage
    
    @ColumnInfo(name = "avg_processing_time_ms")
    private double avgProcessingTimeMs;
    
    @ColumnInfo(name = "error_count")
    private int errorCount;
    
    @ColumnInfo(name = "most_common_error")
    private String mostCommonError;
    
    @ColumnInfo(name = "app_opens")
    private int appOpens;
    
    @ColumnInfo(name = "session_duration_total_ms")
    private long sessionDurationTotalMs;
    
    @ColumnInfo(name = "avg_session_duration_ms")
    private double avgSessionDurationMs;
    
    @ColumnInfo(name = "created_timestamp")
    private long createdTimestamp;
    
    @ColumnInfo(name = "last_updated")
    private long lastUpdated;
    
    // Constructor
    public StatisticsSummary(String date, String summaryType, int totalSmsReceived, 
                           int totalSmsForwarded, int successfulForwards, int failedForwards,
                           double successRate, double avgProcessingTimeMs, int errorCount, 
                           String mostCommonError, int appOpens, long sessionDurationTotalMs,
                           double avgSessionDurationMs, long createdTimestamp, long lastUpdated) {
        this.date = date;
        this.summaryType = summaryType;
        this.totalSmsReceived = totalSmsReceived;
        this.totalSmsForwarded = totalSmsForwarded;
        this.successfulForwards = successfulForwards;
        this.failedForwards = failedForwards;
        this.successRate = successRate;
        this.avgProcessingTimeMs = avgProcessingTimeMs;
        this.errorCount = errorCount;
        this.mostCommonError = mostCommonError;
        this.appOpens = appOpens;
        this.sessionDurationTotalMs = sessionDurationTotalMs;
        this.avgSessionDurationMs = avgSessionDurationMs;
        this.createdTimestamp = createdTimestamp;
        this.lastUpdated = lastUpdated;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getSummaryType() {
        return summaryType;
    }
    
    public void setSummaryType(String summaryType) {
        this.summaryType = summaryType;
    }
    
    public int getTotalSmsReceived() {
        return totalSmsReceived;
    }
    
    public void setTotalSmsReceived(int totalSmsReceived) {
        this.totalSmsReceived = totalSmsReceived;
    }
    
    public int getTotalSmsForwarded() {
        return totalSmsForwarded;
    }
    
    public void setTotalSmsForwarded(int totalSmsForwarded) {
        this.totalSmsForwarded = totalSmsForwarded;
    }
    
    public int getSuccessfulForwards() {
        return successfulForwards;
    }
    
    public void setSuccessfulForwards(int successfulForwards) {
        this.successfulForwards = successfulForwards;
    }
    
    public int getFailedForwards() {
        return failedForwards;
    }
    
    public void setFailedForwards(int failedForwards) {
        this.failedForwards = failedForwards;
    }
    
    public double getSuccessRate() {
        return successRate;
    }
    
    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }
    
    public double getAvgProcessingTimeMs() {
        return avgProcessingTimeMs;
    }
    
    public void setAvgProcessingTimeMs(double avgProcessingTimeMs) {
        this.avgProcessingTimeMs = avgProcessingTimeMs;
    }
    
    public int getErrorCount() {
        return errorCount;
    }
    
    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }
    
    public String getMostCommonError() {
        return mostCommonError;
    }
    
    public void setMostCommonError(String mostCommonError) {
        this.mostCommonError = mostCommonError;
    }
    
    public int getAppOpens() {
        return appOpens;
    }
    
    public void setAppOpens(int appOpens) {
        this.appOpens = appOpens;
    }
    
    public long getSessionDurationTotalMs() {
        return sessionDurationTotalMs;
    }
    
    public void setSessionDurationTotalMs(long sessionDurationTotalMs) {
        this.sessionDurationTotalMs = sessionDurationTotalMs;
    }
    
    public double getAvgSessionDurationMs() {
        return avgSessionDurationMs;
    }
    
    public void setAvgSessionDurationMs(double avgSessionDurationMs) {
        this.avgSessionDurationMs = avgSessionDurationMs;
    }
    
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }
    
    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
    
    public long getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}