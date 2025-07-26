package com.keremgok.sms;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

/**
 * Target Number Entity for Room Database
 * Stores multiple target phone numbers for SMS forwarding
 */
@Entity(tableName = "target_numbers")
public class TargetNumber {
    
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
    
    @ColumnInfo(name = "created_timestamp")
    private long createdTimestamp;
    
    @ColumnInfo(name = "last_used_timestamp")
    private long lastUsedTimestamp;
    
    // Constructor
    public TargetNumber(String phoneNumber, String displayName, boolean isPrimary, boolean isEnabled) {
        this.phoneNumber = phoneNumber;
        this.displayName = displayName;
        this.isPrimary = isPrimary;
        this.isEnabled = isEnabled;
        this.createdTimestamp = System.currentTimeMillis();
        this.lastUsedTimestamp = 0;
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
    
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }
    
    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
    
    public long getLastUsedTimestamp() {
        return lastUsedTimestamp;
    }
    
    public void setLastUsedTimestamp(long lastUsedTimestamp) {
        this.lastUsedTimestamp = lastUsedTimestamp;
    }
    
    @Override
    public String toString() {
        return displayName != null && !displayName.trim().isEmpty() ? 
               displayName + " (" + phoneNumber + ")" : phoneNumber;
    }
}