package com.keremgok.sms;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import java.util.List;

/**
 * Data Access Object (DAO) for SMS History
 * Contains all database operations for SmsHistory entity
 */
@Dao
public interface SmsHistoryDao {
    
    /**
     * Insert new SMS history record
     * @param smsHistory The SMS history record to insert
     */
    @Insert
    void insert(SmsHistory smsHistory);
    
    /**
     * Get all SMS history records ordered by timestamp (newest first)
     * @return List of all SMS history records
     */
    @Query("SELECT * FROM sms_history ORDER BY timestamp DESC")
    List<SmsHistory> getAllHistory();
    
    /**
     * Get SMS history records within date range
     * @param startTime Start timestamp
     * @param endTime End timestamp
     * @return List of SMS history records in the specified date range
     */
    @Query("SELECT * FROM sms_history WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    List<SmsHistory> getHistoryByDateRange(long startTime, long endTime);
    
    /**
     * Search SMS history by sender number or message content
     * @param query Search query
     * @return List of matching SMS history records
     */
    @Query("SELECT * FROM sms_history WHERE sender_number LIKE '%' || :query || '%' OR original_message LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    List<SmsHistory> searchHistory(String query);
    
    /**
     * Get only successful forwarding records
     * @return List of successful SMS history records
     */
    @Query("SELECT * FROM sms_history WHERE success = 1 ORDER BY timestamp DESC")
    List<SmsHistory> getSuccessfulHistory();
    
    /**
     * Get only failed forwarding records
     * @return List of failed SMS history records
     */
    @Query("SELECT * FROM sms_history WHERE success = 0 ORDER BY timestamp DESC")
    List<SmsHistory> getFailedHistory();
    
    /**
     * Get count of total forwarded SMS
     * @return Total count of SMS history records
     */
    @Query("SELECT COUNT(*) FROM sms_history")
    int getTotalCount();
    
    /**
     * Get count of successful forwarded SMS
     * @return Count of successful SMS history records
     */
    @Query("SELECT COUNT(*) FROM sms_history WHERE success = 1")
    int getSuccessCount();
    
    /**
     * Get count of failed forwarded SMS
     * @return Count of failed SMS history records
     */
    @Query("SELECT COUNT(*) FROM sms_history WHERE success = 0")
    int getFailedCount();
    
    /**
     * Delete old records older than specified timestamp
     * Used for automatic cleanup (30 days old records)
     * @param timestamp Timestamp threshold (records older than this will be deleted)
     * @return Number of deleted records
     */
    @Query("DELETE FROM sms_history WHERE timestamp < :timestamp")
    int deleteOldRecords(long timestamp);
    
    /**
     * Delete all SMS history records
     */
    @Query("DELETE FROM sms_history")
    void deleteAllHistory();
    
    /**
     * Delete specific SMS history record
     * @param smsHistory The SMS history record to delete
     */
    @Delete
    void delete(SmsHistory smsHistory);
    
    /**
     * Get SMS history for a specific sender
     * @param senderNumber The sender's phone number
     * @return List of SMS history records from the specified sender
     */
    @Query("SELECT * FROM sms_history WHERE sender_number = :senderNumber ORDER BY timestamp DESC")
    List<SmsHistory> getHistoryBySender(String senderNumber);
    
    /**
     * Get latest SMS history records (limit to specified count)
     * @param limit Maximum number of records to return
     * @return List of latest SMS history records
     */
    @Query("SELECT * FROM sms_history ORDER BY timestamp DESC LIMIT :limit")
    List<SmsHistory> getLatestHistory(int limit);
    
    /**
     * Delete test data by sender number (used for cleanup)
     * @param senderNumber The test sender number to remove
     * @return Number of deleted records
     */
    @Query("DELETE FROM sms_history WHERE sender_number = :senderNumber OR target_number = :senderNumber")
    int deleteTestData(String senderNumber);
}