package com.keremgok.sms;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import java.util.List;

/**
 * Data Access Object (DAO) for Analytics Events
 * Contains all database operations for AnalyticsEvent entity
 * Privacy-first analytics with local data only
 */
@Dao
public interface AnalyticsEventDao {
    
    /**
     * Insert new analytics event
     * @param event The analytics event to insert
     */
    @Insert
    void insert(AnalyticsEvent event);
    
    /**
     * Insert multiple analytics events
     * @param events List of analytics events to insert
     */
    @Insert
    void insertAll(List<AnalyticsEvent> events);
    
    /**
     * Get all analytics events ordered by timestamp (newest first)
     * @return List of all analytics events
     */
    @Query("SELECT * FROM analytics_events ORDER BY timestamp DESC")
    List<AnalyticsEvent> getAllEvents();
    
    /**
     * Get analytics events by type
     * @param eventType Type of events to retrieve
     * @return List of events of specified type
     */
    @Query("SELECT * FROM analytics_events WHERE event_type = :eventType ORDER BY timestamp DESC")
    List<AnalyticsEvent> getEventsByType(String eventType);
    
    /**
     * Get analytics events by category
     * @param eventCategory Category of events to retrieve
     * @return List of events in specified category
     */
    @Query("SELECT * FROM analytics_events WHERE event_category = :eventCategory ORDER BY timestamp DESC")
    List<AnalyticsEvent> getEventsByCategory(String eventCategory);
    
    /**
     * Get analytics events within date range
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return List of events in the specified date range
     */
    @Query("SELECT * FROM analytics_events WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    List<AnalyticsEvent> getEventsByDateRange(long startTime, long endTime);
    
    /**
     * Get events by session ID
     * @param sessionId Session identifier
     * @return List of events for the specified session
     */
    @Query("SELECT * FROM analytics_events WHERE session_id = :sessionId ORDER BY timestamp ASC")
    List<AnalyticsEvent> getEventsBySession(String sessionId);
    
    /**
     * Get error events only
     * @return List of error events
     */
    @Query("SELECT * FROM analytics_events WHERE event_action = 'FAILURE' OR event_action = 'ERROR' ORDER BY timestamp DESC")
    List<AnalyticsEvent> getErrorEvents();
    
    /**
     * Get successful SMS forward events
     * @return List of successful SMS forward events
     */
    @Query("SELECT * FROM analytics_events WHERE event_type = 'SMS_FORWARD' AND event_action = 'SUCCESS' ORDER BY timestamp DESC")
    List<AnalyticsEvent> getSuccessfulForwards();
    
    /**
     * Get failed SMS forward events
     * @return List of failed SMS forward events
     */
    @Query("SELECT * FROM analytics_events WHERE event_type = 'SMS_FORWARD' AND event_action = 'FAILURE' ORDER BY timestamp DESC")
    List<AnalyticsEvent> getFailedForwards();
    
    /**
     * Count total events
     * @return Total number of events
     */
    @Query("SELECT COUNT(*) FROM analytics_events")
    int getTotalEventCount();
    
    /**
     * Count events by type
     * @param eventType Event type to count
     * @return Number of events of specified type
     */
    @Query("SELECT COUNT(*) FROM analytics_events WHERE event_type = :eventType")
    int getEventCountByType(String eventType);
    
    /**
     * Count events by type and action within date range
     * @param eventType Event type
     * @param eventAction Event action
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return Count of matching events
     */
    @Query("SELECT COUNT(*) FROM analytics_events WHERE event_type = :eventType AND event_action = :eventAction AND timestamp BETWEEN :startTime AND :endTime")
    int getEventCountByTypeAndAction(String eventType, String eventAction, long startTime, long endTime);
    
    /**
     * Get average processing time for SMS forwards
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return Average processing time in milliseconds
     */
    @Query("SELECT AVG(duration_ms) FROM analytics_events WHERE event_type = 'SMS_FORWARD' AND duration_ms > 0 AND timestamp BETWEEN :startTime AND :endTime")
    Double getAvgProcessingTime(long startTime, long endTime);
    
    /**
     * Get most common error codes within date range
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @param limit Maximum number of results
     * @return List of error codes ordered by frequency
     */
    @Query("SELECT error_code, COUNT(*) as count FROM analytics_events WHERE error_code IS NOT NULL AND error_code != '' AND timestamp BETWEEN :startTime AND :endTime GROUP BY error_code ORDER BY count DESC LIMIT :limit")
    List<ErrorCodeCount> getMostCommonErrors(long startTime, long endTime, int limit);
    
    /**
     * Get performance metrics for specified date range
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return List of performance events
     */
    @Query("SELECT * FROM analytics_events WHERE event_category = 'PERFORMANCE' AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    List<AnalyticsEvent> getPerformanceMetrics(long startTime, long endTime);
    
    /**
     * Delete old events older than specified timestamp
     * Used for automatic cleanup
     * @param timestamp Timestamp threshold (events older than this will be deleted)
     * @return Number of deleted events
     */
    @Query("DELETE FROM analytics_events WHERE timestamp < :timestamp")
    int deleteOldEvents(long timestamp);
    
    /**
     * Delete all analytics events
     */
    @Query("DELETE FROM analytics_events")
    void deleteAllEvents();
    
    /**
     * Delete specific analytics event
     * @param event The analytics event to delete
     */
    @Delete
    void delete(AnalyticsEvent event);
    
    /**
     * Helper class for error code counting query results
     */
    class ErrorCodeCount {
        public String error_code;
        public int count;
        
        public ErrorCodeCount(String error_code, int count) {
            this.error_code = error_code;
            this.count = count;
        }
    }
}