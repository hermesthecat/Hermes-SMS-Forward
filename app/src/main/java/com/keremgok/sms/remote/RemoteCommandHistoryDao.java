package com.keremgok.sms.remote;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * DAO for remote command history
 */
@Dao
public interface RemoteCommandHistoryDao {
    
    /**
     * Insert a new command history entry
     */
    @Insert
    long insert(RemoteCommandHistory history);
    
    /**
     * Update an existing command history entry
     */
    @Update
    void update(RemoteCommandHistory history);
    
    /**
     * Delete a command history entry
     */
    @Delete
    void delete(RemoteCommandHistory history);
    
    /**
     * Get all command history ordered by most recent first
     */
    @Query("SELECT * FROM remote_command_history ORDER BY received_timestamp DESC")
    List<RemoteCommandHistory> getAll();
    
    /**
     * Get command history by ID
     */
    @Query("SELECT * FROM remote_command_history WHERE id = :id LIMIT 1")
    RemoteCommandHistory getById(int id);
    
    /**
     * Get recent command history (last N entries)
     */
    @Query("SELECT * FROM remote_command_history ORDER BY received_timestamp DESC LIMIT :limit")
    List<RemoteCommandHistory> getRecent(int limit);
    
    /**
     * Get command history by sender
     */
    @Query("SELECT * FROM remote_command_history WHERE sender_number = :senderNumber ORDER BY received_timestamp DESC")
    List<RemoteCommandHistory> getBySender(String senderNumber);
    
    /**
     * Get command history by status
     */
    @Query("SELECT * FROM remote_command_history WHERE execution_status = :status ORDER BY received_timestamp DESC")
    List<RemoteCommandHistory> getByStatus(String status);
    
    /**
     * Get successful commands
     */
    @Query("SELECT * FROM remote_command_history WHERE execution_status = 'SUCCESS' ORDER BY received_timestamp DESC LIMIT :limit")
    List<RemoteCommandHistory> getSuccessful(int limit);
    
    /**
     * Get failed commands
     */
    @Query("SELECT * FROM remote_command_history WHERE execution_status = 'FAILED' ORDER BY received_timestamp DESC LIMIT :limit")
    List<RemoteCommandHistory> getFailed(int limit);
    
    /**
     * Get unauthorized attempts
     */
    @Query("SELECT * FROM remote_command_history WHERE execution_status = 'UNAUTHORIZED' ORDER BY received_timestamp DESC LIMIT :limit")
    List<RemoteCommandHistory> getUnauthorized(int limit);
    
    /**
     * Get commands since timestamp
     */
    @Query("SELECT * FROM remote_command_history WHERE received_timestamp >= :timestamp ORDER BY received_timestamp DESC")
    List<RemoteCommandHistory> getSince(long timestamp);
    
    /**
     * Get commands between timestamps
     */
    @Query("SELECT * FROM remote_command_history WHERE received_timestamp BETWEEN :startTime AND :endTime ORDER BY received_timestamp DESC")
    List<RemoteCommandHistory> getBetween(long startTime, long endTime);
    
    /**
     * Count commands by sender since timestamp (for rate limiting)
     */
    @Query("SELECT COUNT(*) FROM remote_command_history WHERE sender_number = :senderNumber AND received_timestamp >= :since")
    int countCommandsSince(String senderNumber, long since);
    
    /**
     * Count total commands
     */
    @Query("SELECT COUNT(*) FROM remote_command_history")
    int count();
    
    /**
     * Count commands by status
     */
    @Query("SELECT COUNT(*) FROM remote_command_history WHERE execution_status = :status")
    int countByStatus(String status);
    
    /**
     * Count successful commands
     */
    @Query("SELECT COUNT(*) FROM remote_command_history WHERE execution_status = 'SUCCESS'")
    int countSuccessful();
    
    /**
     * Count failed commands
     */
    @Query("SELECT COUNT(*) FROM remote_command_history WHERE execution_status = 'FAILED'")
    int countFailed();
    
    /**
     * Count unauthorized attempts
     */
    @Query("SELECT COUNT(*) FROM remote_command_history WHERE execution_status = 'UNAUTHORIZED'")
    int countUnauthorized();
    
    /**
     * Get success rate (percentage)
     */
    @Query("SELECT CAST(COUNT(CASE WHEN execution_status = 'SUCCESS' THEN 1 END) AS REAL) / COUNT(*) * 100 FROM remote_command_history WHERE execution_status IN ('SUCCESS', 'FAILED')")
    float getSuccessRate();
    
    /**
     * Delete old command history (older than timestamp)
     */
    @Query("DELETE FROM remote_command_history WHERE received_timestamp < :timestamp")
    int deleteOlderThan(long timestamp);
    
    /**
     * Delete all command history
     */
    @Query("DELETE FROM remote_command_history")
    void deleteAll();
    
    /**
     * Get daily command statistics
     */
    @Query("SELECT DATE(received_timestamp / 1000, 'unixepoch') as date, COUNT(*) as count FROM remote_command_history WHERE received_timestamp >= :since GROUP BY date ORDER BY date DESC")
    List<DailyCommandStats> getDailyStats(long since);
    
    /**
     * Inner class for daily statistics
     */
    class DailyCommandStats {
        public String date;
        public int count;
    }
}
