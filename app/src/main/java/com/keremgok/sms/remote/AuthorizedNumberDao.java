package com.keremgok.sms.remote;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * DAO for authorized phone numbers
 */
@Dao
public interface AuthorizedNumberDao {
    
    /**
     * Insert a new authorized number
     */
    @Insert
    long insert(AuthorizedNumber authorizedNumber);
    
    /**
     * Update an existing authorized number
     */
    @Update
    void update(AuthorizedNumber authorizedNumber);
    
    /**
     * Delete an authorized number
     */
    @Delete
    void delete(AuthorizedNumber authorizedNumber);
    
    /**
     * Get all authorized numbers
     */
    @Query("SELECT * FROM authorized_numbers ORDER BY is_primary DESC, added_timestamp DESC")
    List<AuthorizedNumber> getAll();
    
    /**
     * Get all enabled authorized numbers
     */
    @Query("SELECT * FROM authorized_numbers WHERE is_enabled = 1 ORDER BY is_primary DESC, added_timestamp DESC")
    List<AuthorizedNumber> getAllEnabled();
    
    /**
     * Get authorized number by phone number
     */
    @Query("SELECT * FROM authorized_numbers WHERE phone_number = :phoneNumber LIMIT 1")
    AuthorizedNumber getByPhoneNumber(String phoneNumber);
    
    /**
     * Get authorized number by ID
     */
    @Query("SELECT * FROM authorized_numbers WHERE id = :id LIMIT 1")
    AuthorizedNumber getById(int id);
    
    /**
     * Get primary authorized number
     */
    @Query("SELECT * FROM authorized_numbers WHERE is_primary = 1 LIMIT 1")
    AuthorizedNumber getPrimary();
    
    /**
     * Check if phone number is authorized and enabled
     */
    @Query("SELECT COUNT(*) > 0 FROM authorized_numbers WHERE phone_number = :phoneNumber AND is_enabled = 1")
    boolean isAuthorized(String phoneNumber);
    
    /**
     * Count total authorized numbers
     */
    @Query("SELECT COUNT(*) FROM authorized_numbers")
    int count();
    
    /**
     * Count enabled authorized numbers
     */
    @Query("SELECT COUNT(*) FROM authorized_numbers WHERE is_enabled = 1")
    int countEnabled();
    
    /**
     * Update last used timestamp and increment command count
     */
    @Query("UPDATE authorized_numbers SET last_used_timestamp = :timestamp, total_commands_sent = total_commands_sent + 1 WHERE phone_number = :phoneNumber")
    void updateLastUsed(String phoneNumber, long timestamp);
    
    /**
     * Set primary status (ensure only one primary)
     */
    @Query("UPDATE authorized_numbers SET is_primary = CASE WHEN id = :id THEN 1 ELSE 0 END")
    void setPrimary(int id);
    
    /**
     * Enable or disable an authorized number
     */
    @Query("UPDATE authorized_numbers SET is_enabled = :enabled WHERE id = :id")
    void setEnabled(int id, boolean enabled);
    
    /**
     * Delete all authorized numbers
     */
    @Query("DELETE FROM authorized_numbers")
    void deleteAll();
    
    /**
     * Get top senders by command count
     */
    @Query("SELECT * FROM authorized_numbers ORDER BY total_commands_sent DESC LIMIT :limit")
    List<AuthorizedNumber> getTopSenders(int limit);
}
