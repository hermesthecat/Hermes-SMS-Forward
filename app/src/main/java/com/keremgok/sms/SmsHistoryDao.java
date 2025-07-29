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
    
    /**
     * Get SMS history records filtered by source SIM slot
     * @param sourceSimSlot The source SIM slot to filter by (-1 for unknown, 0 for SIM1, 1 for SIM2) 
     * @return List of SMS history records from the specified source SIM
     */
    @Query("SELECT * FROM sms_history WHERE source_sim_slot = :sourceSimSlot ORDER BY timestamp DESC")
    List<SmsHistory> getHistoryBySourceSim(int sourceSimSlot);
    
    /**
     * Get SMS history records filtered by forwarding SIM slot
     * @param forwardingSimSlot The forwarding SIM slot to filter by (-1 for unknown, 0 for SIM1, 1 for SIM2)
     * @return List of SMS history records forwarded via the specified SIM
     */
    @Query("SELECT * FROM sms_history WHERE forwarding_sim_slot = :forwardingSimSlot ORDER BY timestamp DESC")
    List<SmsHistory> getHistoryByForwardingSim(int forwardingSimSlot);
    
    /**
     * Get SMS history records by source subscription ID
     * @param sourceSubscriptionId The source subscription ID to filter by
     * @return List of SMS history records from the specified source subscription
     */
    @Query("SELECT * FROM sms_history WHERE source_subscription_id = :sourceSubscriptionId ORDER BY timestamp DESC")
    List<SmsHistory> getHistoryBySourceSubscription(int sourceSubscriptionId);
    
    /**
     * Get SMS history records by forwarding subscription ID
     * @param forwardingSubscriptionId The forwarding subscription ID to filter by
     * @return List of SMS history records forwarded via the specified subscription
     */
    @Query("SELECT * FROM sms_history WHERE forwarding_subscription_id = :forwardingSubscriptionId ORDER BY timestamp DESC")
    List<SmsHistory> getHistoryByForwardingSubscription(int forwardingSubscriptionId);
    
    /**
     * Get count of SMS by source SIM slot
     * @param sourceSimSlot The source SIM slot to count
     * @return Number of SMS received from the specified source SIM
     */
    @Query("SELECT COUNT(*) FROM sms_history WHERE source_sim_slot = :sourceSimSlot")
    int getCountBySourceSim(int sourceSimSlot);
    
    /**
     * Get count of SMS by forwarding SIM slot
     * @param forwardingSimSlot The forwarding SIM slot to count
     * @return Number of SMS forwarded via the specified SIM
     */
    @Query("SELECT COUNT(*) FROM sms_history WHERE forwarding_sim_slot = :forwardingSimSlot")
    int getCountByForwardingSim(int forwardingSimSlot);
    
    /**
     * Get success rate by forwarding SIM slot
     * @param forwardingSimSlot The forwarding SIM slot to analyze
     * @return Success rate as percentage (0.0 to 100.0)
     */
    @Query("SELECT (CAST(SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) AS REAL) / CAST(COUNT(*) AS REAL)) * 100 FROM sms_history WHERE forwarding_sim_slot = :forwardingSimSlot")
    double getSuccessRateByForwardingSim(int forwardingSimSlot);
    
    /**
     * Get SMS history with SIM information for analytics
     * Includes both source and forwarding SIM details
     * @param startTime Start timestamp for the period
     * @param endTime End timestamp for the period
     * @return List of SMS history records with SIM information
     */
    @Query("SELECT * FROM sms_history WHERE timestamp BETWEEN :startTime AND :endTime AND (source_sim_slot != -1 OR forwarding_sim_slot != -1) ORDER BY timestamp DESC")
    List<SmsHistory> getSimAwareHistoryByDateRange(long startTime, long endTime);
    
    /**
     * Get dual SIM usage statistics
     * Returns records where both source and forwarding SIM information is available
     * @return List of SMS history records with complete dual SIM information
     */
    @Query("SELECT * FROM sms_history WHERE source_sim_slot != -1 AND forwarding_sim_slot != -1 AND source_subscription_id != -1 AND forwarding_subscription_id != -1 ORDER BY timestamp DESC")
    List<SmsHistory> getDualSimStatistics();
    
    /**
     * Get count of SMS that switched SIMs (received on one SIM, forwarded via another)
     * @return Number of SMS that were forwarded using a different SIM than the source
     */
    @Query("SELECT COUNT(*) FROM sms_history WHERE source_sim_slot != -1 AND forwarding_sim_slot != -1 AND source_sim_slot != forwarding_sim_slot")
    int getSimSwitchCount();
    
    /**
     * Get most used SIM for forwarding
     * @return The SIM slot that has been used most for forwarding (-1 if no data)
     */
    @Query("SELECT forwarding_sim_slot FROM sms_history WHERE forwarding_sim_slot != -1 GROUP BY forwarding_sim_slot ORDER BY COUNT(*) DESC LIMIT 1")
    int getMostUsedForwardingSim();
}