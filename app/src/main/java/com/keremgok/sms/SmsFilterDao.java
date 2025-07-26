package com.keremgok.sms;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import java.util.List;

/**
 * Data Access Object for SMS Filters
 * Provides database operations for SMS filtering rules
 */
@Dao
public interface SmsFilterDao {
    
    /**
     * Insert a new SMS filter
     * @param filter The filter to insert
     * @return The ID of the inserted record
     */
    @Insert
    long insert(SmsFilter filter);
    
    /**
     * Update an existing SMS filter
     * @param filter The filter to update
     */
    @Update
    void update(SmsFilter filter);
    
    /**
     * Delete an SMS filter
     * @param filter The filter to delete
     */
    @Delete
    void delete(SmsFilter filter);
    
    /**
     * Get all SMS filters ordered by priority and creation time
     * @return List of all SMS filters
     */
    @Query("SELECT * FROM sms_filters ORDER BY priority DESC, created_timestamp ASC")
    List<SmsFilter> getAllFilters();
    
    /**
     * Get all enabled SMS filters ordered by priority
     * @return List of enabled SMS filters
     */
    @Query("SELECT * FROM sms_filters WHERE is_enabled = 1 ORDER BY priority DESC, created_timestamp ASC")
    List<SmsFilter> getEnabledFilters();
    
    /**
     * Get filters by type
     * @param filterType The type of filters to retrieve
     * @return List of filters of the specified type
     */
    @Query("SELECT * FROM sms_filters WHERE filter_type = :filterType AND is_enabled = 1 ORDER BY priority DESC")
    List<SmsFilter> getFiltersByType(String filterType);
    
    /**
     * Get filter by ID
     * @param filterId The ID of the filter
     * @return The filter with the specified ID, or null if not found
     */
    @Query("SELECT * FROM sms_filters WHERE id = :filterId LIMIT 1")
    SmsFilter getFilterById(int filterId);
    
    /**
     * Check if a filter with the same name already exists
     * @param filterName The name to check
     * @return True if a filter with the name exists, false otherwise
     */
    @Query("SELECT COUNT(*) > 0 FROM sms_filters WHERE filter_name = :filterName")
    boolean isFilterNameExists(String filterName);
    
    /**
     * Get filter by name
     * @param filterName The name of the filter
     * @return The filter with the specified name, or null if not found
     */
    @Query("SELECT * FROM sms_filters WHERE filter_name = :filterName LIMIT 1")
    SmsFilter getFilterByName(String filterName);
    
    /**
     * Get whitelist filters (ALLOW action)
     * @return List of whitelist filters
     */
    @Query("SELECT * FROM sms_filters WHERE action = 'ALLOW' AND is_enabled = 1 ORDER BY priority DESC")
    List<SmsFilter> getWhitelistFilters();
    
    /**
     * Get blacklist filters (BLOCK action)
     * @return List of blacklist filters
     */
    @Query("SELECT * FROM sms_filters WHERE action = 'BLOCK' AND is_enabled = 1 ORDER BY priority DESC")
    List<SmsFilter> getBlacklistFilters();
    
    /**
     * Get keyword filters
     * @return List of keyword-based filters
     */
    @Query("SELECT * FROM sms_filters WHERE filter_type = 'KEYWORD' AND is_enabled = 1 ORDER BY priority DESC")
    List<SmsFilter> getKeywordFilters();
    
    /**
     * Get sender number filters
     * @return List of sender number filters
     */
    @Query("SELECT * FROM sms_filters WHERE filter_type = 'SENDER_NUMBER' AND is_enabled = 1 ORDER BY priority DESC")
    List<SmsFilter> getSenderFilters();
    
    /**
     * Get time-based filters
     * @return List of time-based filters
     */
    @Query("SELECT * FROM sms_filters WHERE filter_type = 'TIME_BASED' AND is_enabled = 1 ORDER BY priority DESC")
    List<SmsFilter> getTimeBasedFilters();
    
    /**
     * Get spam detection filters
     * @return List of spam detection filters
     */
    @Query("SELECT * FROM sms_filters WHERE filter_type = 'SPAM_DETECTION' AND is_enabled = 1 ORDER BY priority DESC")
    List<SmsFilter> getSpamFilters();
    
    /**
     * Update filter enabled status
     * @param filterId The ID of the filter
     * @param enabled The new enabled status
     */
    @Query("UPDATE sms_filters SET is_enabled = :enabled, modified_timestamp = :timestamp WHERE id = :filterId")
    void setEnabledStatus(int filterId, boolean enabled, long timestamp);
    
    /**
     * Increment match count for a filter
     * @param filterId The ID of the filter
     * @param timestamp The timestamp of the match
     */
    @Query("UPDATE sms_filters SET match_count = match_count + 1, last_matched = :timestamp WHERE id = :filterId")
    void incrementMatchCount(int filterId, long timestamp);
    
    /**
     * Get filter statistics (most used filters)
     * @param limit Number of top filters to return
     * @return List of most frequently matched filters
     */
    @Query("SELECT * FROM sms_filters WHERE match_count > 0 ORDER BY match_count DESC, last_matched DESC LIMIT :limit")
    List<SmsFilter> getTopMatchedFilters(int limit);
    
    /**
     * Get filters created in the last N days
     * @param daysAgo Number of days to look back
     * @return List of recently created filters
     */
    @Query("SELECT * FROM sms_filters WHERE created_timestamp > :timestampThreshold ORDER BY created_timestamp DESC")
    List<SmsFilter> getRecentFilters(long timestampThreshold);
    
    /**
     * Delete all filters (for testing/reset purposes)
     */
    @Query("DELETE FROM sms_filters")
    void deleteAllFilters();
    
    /**
     * Get count of enabled filters
     * @return Number of enabled filters
     */
    @Query("SELECT COUNT(*) FROM sms_filters WHERE is_enabled = 1")
    int getEnabledFilterCount();
    
    /**
     * Get count of filters by type
     * @param filterType The filter type
     * @return Number of filters of the specified type
     */
    @Query("SELECT COUNT(*) FROM sms_filters WHERE filter_type = :filterType AND is_enabled = 1")
    int getFilterCountByType(String filterType);
    
    /**
     * Search filters by name or pattern
     * @param searchQuery The search query
     * @return List of filters matching the search query
     */
    @Query("SELECT * FROM sms_filters WHERE filter_name LIKE '%' || :searchQuery || '%' OR pattern LIKE '%' || :searchQuery || '%' ORDER BY priority DESC")
    List<SmsFilter> searchFilters(String searchQuery);
}