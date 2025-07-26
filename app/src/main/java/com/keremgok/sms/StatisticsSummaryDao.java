package com.keremgok.sms;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;
import androidx.room.Delete;
import java.util.List;

/**
 * Data Access Object (DAO) for Statistics Summary
 * Contains all database operations for StatisticsSummary entity
 * Handles aggregated statistics for dashboard display
 */
@Dao
public interface StatisticsSummaryDao {
    
    /**
     * Insert new statistics summary
     * @param summary The statistics summary to insert
     */
    @Insert
    void insert(StatisticsSummary summary);
    
    /**
     * Update existing statistics summary
     * @param summary The statistics summary to update
     */
    @Update
    void update(StatisticsSummary summary);
    
    /**
     * Insert or update statistics summary (upsert functionality)
     * @param summary The statistics summary to insert or update
     */
    @Query("INSERT OR REPLACE INTO statistics_summary (date, summary_type, total_sms_received, total_sms_forwarded, successful_forwards, failed_forwards, success_rate, avg_processing_time_ms, error_count, most_common_error, app_opens, session_duration_total_ms, avg_session_duration_ms, created_timestamp, last_updated) VALUES (:date, :summaryType, :totalSmsReceived, :totalSmsForwarded, :successfulForwards, :failedForwards, :successRate, :avgProcessingTimeMs, :errorCount, :mostCommonError, :appOpens, :sessionDurationTotalMs, :avgSessionDurationMs, :createdTimestamp, :lastUpdated)")
    void insertOrUpdate(String date, String summaryType, int totalSmsReceived, int totalSmsForwarded, 
                       int successfulForwards, int failedForwards, double successRate, 
                       double avgProcessingTimeMs, int errorCount, String mostCommonError, 
                       int appOpens, long sessionDurationTotalMs, double avgSessionDurationMs, 
                       long createdTimestamp, long lastUpdated);
    
    /**
     * Get all statistics summaries ordered by date (newest first)
     * @return List of all statistics summaries
     */
    @Query("SELECT * FROM statistics_summary ORDER BY date DESC")
    List<StatisticsSummary> getAllSummaries();
    
    /**
     * Get statistics summaries by type
     * @param summaryType Type of summary (DAILY, WEEKLY, MONTHLY)
     * @return List of summaries of specified type
     */
    @Query("SELECT * FROM statistics_summary WHERE summary_type = :summaryType ORDER BY date DESC")
    List<StatisticsSummary> getSummariesByType(String summaryType);
    
    /**
     * Get statistics summary for specific date and type
     * @param date The date
     * @param summaryType The summary type
     * @return Statistics summary for the specified date and type
     */
    @Query("SELECT * FROM statistics_summary WHERE date = :date AND summary_type = :summaryType LIMIT 1")
    StatisticsSummary getSummaryByDateAndType(String date, String summaryType);
    
    /**
     * Get daily summaries for last N days
     * @param days Number of days to retrieve
     * @return List of daily summaries for the last N days
     */
    @Query("SELECT * FROM statistics_summary WHERE summary_type = 'DAILY' ORDER BY date DESC LIMIT :days")
    List<StatisticsSummary> getLastNDays(int days);
    
    /**
     * Get weekly summaries for last N weeks
     * @param weeks Number of weeks to retrieve
     * @return List of weekly summaries for the last N weeks
     */
    @Query("SELECT * FROM statistics_summary WHERE summary_type = 'WEEKLY' ORDER BY date DESC LIMIT :weeks")
    List<StatisticsSummary> getLastNWeeks(int weeks);
    
    /**
     * Get monthly summaries for last N months
     * @param months Number of months to retrieve
     * @return List of monthly summaries for the last N months
     */
    @Query("SELECT * FROM statistics_summary WHERE summary_type = 'MONTHLY' ORDER BY date DESC LIMIT :months")
    List<StatisticsSummary> getLastNMonths(int months);
    
    /**
     * Get summaries within date range
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param summaryType Type of summary
     * @return List of summaries in the specified date range
     */
    @Query("SELECT * FROM statistics_summary WHERE date BETWEEN :startDate AND :endDate AND summary_type = :summaryType ORDER BY date ASC")
    List<StatisticsSummary> getSummariesByDateRange(String startDate, String endDate, String summaryType);
    
    /**
     * Get total counts across all summaries of specified type
     * @param summaryType Type of summary
     * @return Total statistics across all summaries
     */
    @Query("SELECT SUM(total_sms_received) as total_received, SUM(total_sms_forwarded) as total_forwarded, SUM(successful_forwards) as total_successful, SUM(failed_forwards) as total_failed, AVG(success_rate) as avg_success_rate, SUM(error_count) as total_errors FROM statistics_summary WHERE summary_type = :summaryType")
    TotalStatistics getTotalStatistics(String summaryType);
    
    /**
     * Get latest statistics summary by type
     * @param summaryType Type of summary
     * @return Latest statistics summary of specified type
     */
    @Query("SELECT * FROM statistics_summary WHERE summary_type = :summaryType ORDER BY date DESC LIMIT 1")
    StatisticsSummary getLatestSummary(String summaryType);
    
    /**
     * Get highest success rate summary by type
     * @param summaryType Type of summary
     * @return Summary with highest success rate
     */
    @Query("SELECT * FROM statistics_summary WHERE summary_type = :summaryType ORDER BY success_rate DESC LIMIT 1")
    StatisticsSummary getBestPerformanceSummary(String summaryType);
    
    /**
     * Get lowest success rate summary by type
     * @param summaryType Type of summary
     * @return Summary with lowest success rate
     */
    @Query("SELECT * FROM statistics_summary WHERE summary_type = :summaryType ORDER BY success_rate ASC LIMIT 1")
    StatisticsSummary getWorstPerformanceSummary(String summaryType);
    
    /**
     * Check if summary exists for date and type
     * @param date The date
     * @param summaryType The summary type
     * @return Count of matching summaries (should be 0 or 1)
     */
    @Query("SELECT COUNT(*) FROM statistics_summary WHERE date = :date AND summary_type = :summaryType")
    int summaryExists(String date, String summaryType);
    
    /**
     * Delete old summaries older than specified date
     * Used for automatic cleanup
     * @param date Date threshold (summaries older than this will be deleted)
     * @param summaryType Type of summary to clean up
     * @return Number of deleted summaries
     */
    @Query("DELETE FROM statistics_summary WHERE date < :date AND summary_type = :summaryType")
    int deleteOldSummaries(String date, String summaryType);
    
    /**
     * Delete all statistics summaries
     */
    @Query("DELETE FROM statistics_summary")
    void deleteAllSummaries();
    
    /**
     * Delete summaries by type
     * @param summaryType Type of summaries to delete
     */
    @Query("DELETE FROM statistics_summary WHERE summary_type = :summaryType")
    void deleteSummariesByType(String summaryType);
    
    /**
     * Delete specific statistics summary
     * @param summary The statistics summary to delete
     */
    @Delete
    void delete(StatisticsSummary summary);
    
    /**
     * Helper class for total statistics query results
     */
    class TotalStatistics {
        public int total_received;
        public int total_forwarded;
        public int total_successful;
        public int total_failed;
        public double avg_success_rate;
        public int total_errors;
        
        public TotalStatistics(int total_received, int total_forwarded, int total_successful, 
                             int total_failed, double avg_success_rate, int total_errors) {
            this.total_received = total_received;
            this.total_forwarded = total_forwarded;
            this.total_successful = total_successful;
            this.total_failed = total_failed;
            this.avg_success_rate = avg_success_rate;
            this.total_errors = total_errors;
        }
    }
}