package com.keremgok.sms.remote;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing an authorized phone number for remote SMS control
 * Only numbers in this table can send remote SMS commands
 */
@Entity(
    tableName = "authorized_numbers",
    indices = {@Index(value = "phone_number", unique = true)}
)
public class AuthorizedNumber {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @ColumnInfo(name = "phone_number")
    private String phoneNumber;
    
    @ColumnInfo(name = "display_name")
    private String displayName;
    
    @ColumnInfo(name = "is_primary")
    private boolean isPrimary;
    
    @ColumnInfo(name = "is_enabled")
    private boolean isEnabled;
    
    @ColumnInfo(name = "added_timestamp")
    private long addedTimestamp;
    
    @ColumnInfo(name = "last_used_timestamp")
    private Long lastUsedTimestamp;
    
    @ColumnInfo(name = "total_commands_sent")
    private int totalCommandsSent;
    
    /**
     * Constructor for Room
     */
    public AuthorizedNumber(String phoneNumber, String displayName, boolean isPrimary, 
                           boolean isEnabled, long addedTimestamp) {
        this.phoneNumber = phoneNumber;
        this.displayName = displayName;
        this.isPrimary = isPrimary;
        this.isEnabled = isEnabled;
        this.addedTimestamp = addedTimestamp;
        this.lastUsedTimestamp = null;
        this.totalCommandsSent = 0;
    }
    
    /**
     * Create a new authorized number (default enabled, not primary)
     */
    public static AuthorizedNumber create(String phoneNumber, String displayName) {
        return new AuthorizedNumber(
            phoneNumber,
            displayName != null ? displayName : phoneNumber,
            false,
            true,
            System.currentTimeMillis()
        );
    }
    
    /**
     * Create a new primary authorized number (owner)
     */
    public static AuthorizedNumber createPrimary(String phoneNumber, String displayName) {
        return new AuthorizedNumber(
            phoneNumber,
            displayName != null ? displayName : "Owner",
            true,
            true,
            System.currentTimeMillis()
        );
    }
    
    // Getters and Setters
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public boolean isPrimary() {
        return isPrimary;
    }
    
    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }
    
    public boolean isEnabled() {
        return isEnabled;
    }
    
    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }
    
    public long getAddedTimestamp() {
        return addedTimestamp;
    }
    
    public void setAddedTimestamp(long addedTimestamp) {
        this.addedTimestamp = addedTimestamp;
    }
    
    public Long getLastUsedTimestamp() {
        return lastUsedTimestamp;
    }
    
    public void setLastUsedTimestamp(Long lastUsedTimestamp) {
        this.lastUsedTimestamp = lastUsedTimestamp;
    }
    
    public int getTotalCommandsSent() {
        return totalCommandsSent;
    }
    
    public void setTotalCommandsSent(int totalCommandsSent) {
        this.totalCommandsSent = totalCommandsSent;
    }
    
    /**
     * Increment command counter and update last used timestamp
     */
    public void incrementCommandCount() {
        this.totalCommandsSent++;
        this.lastUsedTimestamp = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "AuthorizedNumber{" +
                "id=" + id +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", displayName='" + displayName + '\'' +
                ", isPrimary=" + isPrimary +
                ", isEnabled=" + isEnabled +
                ", totalCommandsSent=" + totalCommandsSent +
                '}';
    }
}
