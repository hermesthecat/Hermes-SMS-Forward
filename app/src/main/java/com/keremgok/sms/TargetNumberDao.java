package com.keremgok.sms;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import java.util.List;

/**
 * Data Access Object for Target Numbers
 * Provides database operations for target phone numbers
 */
@Dao
public interface TargetNumberDao {
    
    /**
     * Insert a new target number
     * @param targetNumber The target number to insert
     * @return The ID of the inserted record
     */
    @Insert
    long insert(TargetNumber targetNumber);
    
    /**
     * Update an existing target number
     * @param targetNumber The target number to update
     */
    @Update
    void update(TargetNumber targetNumber);
    
    /**
     * Delete a target number
     * @param targetNumber The target number to delete
     */
    @Delete
    void delete(TargetNumber targetNumber);
    
    /**
     * Get all target numbers ordered by primary status and creation time
     * @return List of all target numbers
     */
    @Query("SELECT * FROM target_numbers ORDER BY is_primary DESC, created_timestamp ASC")
    List<TargetNumber> getAllTargetNumbers();
    
    /**
     * Get all enabled target numbers ordered by primary status
     * @return List of enabled target numbers
     */
    @Query("SELECT * FROM target_numbers WHERE is_enabled = 1 ORDER BY is_primary DESC, created_timestamp ASC")
    List<TargetNumber> getEnabledTargetNumbers();
    
    /**
     * Get the primary target number
     * @return The primary target number, or null if none is set
     */
    @Query("SELECT * FROM target_numbers WHERE is_primary = 1 AND is_enabled = 1 LIMIT 1")
    TargetNumber getPrimaryTargetNumber();
    
    /**
     * Get target number by phone number
     * @param phoneNumber The phone number to search for
     * @return The target number with the specified phone number, or null if not found
     */
    @Query("SELECT * FROM target_numbers WHERE phone_number = :phoneNumber LIMIT 1")
    TargetNumber getTargetNumberByPhone(String phoneNumber);
    
    /**
     * Check if a phone number already exists
     * @param phoneNumber The phone number to check
     * @return True if the phone number exists, false otherwise
     */
    @Query("SELECT COUNT(*) > 0 FROM target_numbers WHERE phone_number = :phoneNumber")
    boolean isPhoneNumberExists(String phoneNumber);
    
    /**
     * Set a target number as primary (and unset others)
     * @param targetNumberId The ID of the target number to set as primary
     */
    @Query("UPDATE target_numbers SET is_primary = CASE WHEN id = :targetNumberId THEN 1 ELSE 0 END")
    void setPrimaryTargetNumber(int targetNumberId);
    
    /**
     * Update the last used timestamp for a target number
     * @param targetNumberId The ID of the target number
     * @param timestamp The timestamp to set
     */
    @Query("UPDATE target_numbers SET last_used_timestamp = :timestamp WHERE id = :targetNumberId")
    void updateLastUsedTimestamp(int targetNumberId, long timestamp);
    
    /**
     * Get count of enabled target numbers
     * @return Number of enabled target numbers
     */
    @Query("SELECT COUNT(*) FROM target_numbers WHERE is_enabled = 1")
    int getEnabledTargetCount();
    
    /**
     * Delete all target numbers (for migration purposes)
     */
    @Query("DELETE FROM target_numbers")
    void deleteAll();
    
    /**
     * Toggle enabled status of a target number
     * @param targetNumberId The ID of the target number
     * @param enabled The new enabled status
     */
    @Query("UPDATE target_numbers SET is_enabled = :enabled WHERE id = :targetNumberId")
    void setEnabledStatus(int targetNumberId, boolean enabled);
}