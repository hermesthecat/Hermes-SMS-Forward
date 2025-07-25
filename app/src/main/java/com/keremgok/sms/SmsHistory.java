package com.keremgok.sms;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

/**
 * SMS History Entity for Room Database
 * Stores forwarded SMS information with success/failure tracking
 */
@Entity(tableName = "sms_history")
public class SmsHistory {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @ColumnInfo(name = "sender_number")
    private String senderNumber;
    
    @ColumnInfo(name = "original_message")
    private String originalMessage;
    
    @ColumnInfo(name = "target_number")
    private String targetNumber;
    
    @ColumnInfo(name = "forwarded_message")
    private String forwardedMessage;
    
    @ColumnInfo(name = "timestamp")
    private long timestamp;
    
    @ColumnInfo(name = "success")
    private boolean success;
    
    @ColumnInfo(name = "error_message")
    private String errorMessage;
    
    // Constructor
    public SmsHistory(String senderNumber, String originalMessage, String targetNumber, 
                     String forwardedMessage, long timestamp, boolean success, String errorMessage) {
        this.senderNumber = senderNumber;
        this.originalMessage = originalMessage;
        this.targetNumber = targetNumber;
        this.forwardedMessage = forwardedMessage;
        this.timestamp = timestamp;
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getSenderNumber() {
        return senderNumber;
    }
    
    public void setSenderNumber(String senderNumber) {
        this.senderNumber = senderNumber;
    }
    
    public String getOriginalMessage() {
        return originalMessage;
    }
    
    public void setOriginalMessage(String originalMessage) {
        this.originalMessage = originalMessage;
    }
    
    public String getTargetNumber() {
        return targetNumber;
    }
    
    public void setTargetNumber(String targetNumber) {
        this.targetNumber = targetNumber;
    }
    
    public String getForwardedMessage() {
        return forwardedMessage;
    }
    
    public void setForwardedMessage(String forwardedMessage) {
        this.forwardedMessage = forwardedMessage;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}