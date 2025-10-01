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
     * Check if a phone number exists excluding a specific target number ID
     * Used for edit validation to allow keeping the same number
     * @param phoneNumber The phone number to check
     * @param excludeId The ID to exclude from the check (current target being edited)
     * @return True if the phone number exists in another record, false otherwise
     */
    @Query("SELECT COUNT(*) > 0 FROM target_numbers WHERE phone_number = :phoneNumber AND id != :excludeId")
    boolean isPhoneNumberExistsExcept(String phoneNumber, int excludeId);

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
    
    /**
     * Update SIM selection settings for a target number
     * @param targetNumberId The ID of the target number
     * @param preferredSimSlot The preferred SIM slot (-1 for auto, 0 for SIM1, 1 for SIM2)
     * @param simSelectionMode The SIM selection mode ("auto", "source_sim", "specific_sim")
     */
    @Query("UPDATE target_numbers SET preferred_sim_slot = :preferredSimSlot, sim_selection_mode = :simSelectionMode WHERE id = :targetNumberId")
    void updateSimSettings(int targetNumberId, int preferredSimSlot, String simSelectionMode);
    
    /**
     * Get target numbers filtered by SIM selection mode
     * @param simSelectionMode The SIM selection mode to filter by
     * @return List of target numbers with the specified SIM selection mode
     */
    @Query("SELECT * FROM target_numbers WHERE sim_selection_mode = :simSelectionMode AND is_enabled = 1 ORDER BY is_primary DESC, created_timestamp ASC")
    List<TargetNumber> getTargetNumbersBySimMode(String simSelectionMode);
    
    /**
     * Get target numbers that use a specific SIM slot
     * @param simSlot The SIM slot to filter by (0 for SIM1, 1 for SIM2)
     * @return List of target numbers configured for the specified SIM slot
     */
    @Query("SELECT * FROM target_numbers WHERE preferred_sim_slot = :simSlot AND is_enabled = 1 ORDER BY is_primary DESC, created_timestamp ASC")
    List<TargetNumber> getTargetNumbersBySimSlot(int simSlot);
    
    /**
     * Get target numbers that use auto SIM selection
     * @return List of target numbers configured for auto SIM selection
     */
    @Query("SELECT * FROM target_numbers WHERE preferred_sim_slot = -1 AND is_enabled = 1 ORDER BY is_primary DESC, created_timestamp ASC")
    List<TargetNumber> getAutoSimTargetNumbers();
    
    /**
     * Get count of target numbers by SIM selection mode
     * @param simSelectionMode The SIM selection mode to count
     * @return Number of target numbers with the specified SIM selection mode
     */
    @Query("SELECT COUNT(*) FROM target_numbers WHERE sim_selection_mode = :simSelectionMode AND is_enabled = 1")
    int getTargetCountBySimMode(String simSelectionMode);
    
    /**
     * Get count of target numbers by preferred SIM slot
     * @param simSlot The SIM slot to count (-1 for auto, 0 for SIM1, 1 for SIM2)
     * @return Number of target numbers configured for the specified SIM slot
     */
    @Query("SELECT COUNT(*) FROM target_numbers WHERE preferred_sim_slot = :simSlot AND is_enabled = 1")
    int getTargetCountBySimSlot(int simSlot);
}