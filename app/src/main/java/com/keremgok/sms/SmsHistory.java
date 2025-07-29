package com.keremgok.sms;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;

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
    
    @ColumnInfo(name = "source_sim_slot")
    private int sourceSimSlot = -1;
    
    @ColumnInfo(name = "forwarding_sim_slot")
    private int forwardingSimSlot = -1;
    
    @ColumnInfo(name = "source_subscription_id")
    private int sourceSubscriptionId = -1;
    
    @ColumnInfo(name = "forwarding_subscription_id")
    private int forwardingSubscriptionId = -1;
    
    // Constructor for backward compatibility
    @Ignore
    public SmsHistory(String senderNumber, String originalMessage, String targetNumber, 
                     String forwardedMessage, long timestamp, boolean success, String errorMessage) {
        this.senderNumber = senderNumber;
        this.originalMessage = originalMessage;
        this.targetNumber = targetNumber;
        this.forwardedMessage = forwardedMessage;
        this.timestamp = timestamp;
        this.success = success;
        this.errorMessage = errorMessage;
        this.sourceSimSlot = -1;
        this.forwardingSimSlot = -1;
        this.sourceSubscriptionId = -1;
        this.forwardingSubscriptionId = -1;
    }
    
    // Constructor with dual SIM support
    public SmsHistory(String senderNumber, String originalMessage, String targetNumber, 
                     String forwardedMessage, long timestamp, boolean success, String errorMessage,
                     int sourceSimSlot, int forwardingSimSlot, int sourceSubscriptionId, int forwardingSubscriptionId) {
        this.senderNumber = senderNumber;
        this.originalMessage = originalMessage;
        this.targetNumber = targetNumber;
        this.forwardedMessage = forwardedMessage;
        this.timestamp = timestamp;
        this.success = success;
        this.errorMessage = errorMessage;
        this.sourceSimSlot = sourceSimSlot;
        this.forwardingSimSlot = forwardingSimSlot;
        this.sourceSubscriptionId = sourceSubscriptionId;
        this.forwardingSubscriptionId = forwardingSubscriptionId;
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
    
    public int getSourceSimSlot() {
        return sourceSimSlot;
    }
    
    public void setSourceSimSlot(int sourceSimSlot) {
        this.sourceSimSlot = sourceSimSlot;
    }
    
    public int getForwardingSimSlot() {
        return forwardingSimSlot;
    }
    
    public void setForwardingSimSlot(int forwardingSimSlot) {
        this.forwardingSimSlot = forwardingSimSlot;
    }
    
    public int getSourceSubscriptionId() {
        return sourceSubscriptionId;
    }
    
    public void setSourceSubscriptionId(int sourceSubscriptionId) {
        this.sourceSubscriptionId = sourceSubscriptionId;
    }
    
    public int getForwardingSubscriptionId() {
        return forwardingSubscriptionId;
    }
    
    public void setForwardingSubscriptionId(int forwardingSubscriptionId) {
        this.forwardingSubscriptionId = forwardingSubscriptionId;
    }
}