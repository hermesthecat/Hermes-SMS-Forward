package com.keremgok.sms;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

/**
 * Analytics Event Entity for Room Database
 * Tracks individual events for privacy-first local analytics
 * No sensitive data is stored - only anonymized metrics
 */
@Entity(tableName = "analytics_events")
public class AnalyticsEvent {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @ColumnInfo(name = "event_type")
    private String eventType; // SMS_FORWARD, SMS_ERROR, APP_OPEN, PERMISSION_GRANTED, etc.
    
    @ColumnInfo(name = "event_category")
    private String eventCategory; // MESSAGING, UI, PERMISSION, PERFORMANCE, ERROR
    
    @ColumnInfo(name = "event_action")
    private String eventAction; // SUCCESS, FAILURE, TIMEOUT, RETRY, etc.
    
    @ColumnInfo(name = "timestamp")
    private long timestamp;
    
    @ColumnInfo(name = "duration_ms")
    private long durationMs; // For performance tracking (0 if not applicable)
    
    @ColumnInfo(name = "error_code")
    private String errorCode; // Generic error codes, no sensitive info
    
    @ColumnInfo(name = "metadata")
    private String metadata; // Additional non-sensitive context (JSON format)
    
    @ColumnInfo(name = "app_version")
    private String appVersion; // Version when event occurred
    
    @ColumnInfo(name = "session_id")
    private String sessionId; // Anonymous session identifier
    
    // Constructor
    public AnalyticsEvent(String eventType, String eventCategory, String eventAction, 
                         long timestamp, long durationMs, String errorCode, String metadata, 
                         String appVersion, String sessionId) {
        this.eventType = eventType;
        this.eventCategory = eventCategory;
        this.eventAction = eventAction;
        this.timestamp = timestamp;
        this.durationMs = durationMs;
        this.errorCode = errorCode;
        this.metadata = metadata;
        this.appVersion = appVersion;
        this.sessionId = sessionId;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getEventCategory() {
        return eventCategory;
    }
    
    public void setEventCategory(String eventCategory) {
        this.eventCategory = eventCategory;
    }
    
    public String getEventAction() {
        return eventAction;
    }
    
    public void setEventAction(String eventAction) {
        this.eventAction = eventAction;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getMetadata() {
        return metadata;
    }
    
    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
    
    public String getAppVersion() {
        return appVersion;
    }
    
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}