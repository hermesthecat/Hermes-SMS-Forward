package com.keremgok.sms;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

/**
 * SMS Filter Entity for Room Database
 * Stores SMS filtering rules for advanced message filtering
 */
@Entity(tableName = "sms_filters")
public class SmsFilter {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @ColumnInfo(name = "filter_name")
    private String filterName;
    
    @ColumnInfo(name = "filter_type")
    private String filterType; // KEYWORD, SENDER_NUMBER, WHITELIST, BLACKLIST, SIM_BASED
    
    @ColumnInfo(name = "pattern")
    private String pattern; // The pattern to match (keyword, regex, phone number pattern)
    
    @ColumnInfo(name = "action")
    private String action; // ALLOW, BLOCK
    
    @ColumnInfo(name = "is_enabled")
    private boolean isEnabled;
    
    @ColumnInfo(name = "is_case_sensitive")
    private boolean isCaseSensitive;
    
    @ColumnInfo(name = "is_regex")
    private boolean isRegex;
    
    @ColumnInfo(name = "priority")
    private int priority; // Higher number = higher priority
    
    
    @ColumnInfo(name = "match_count")
    private int matchCount; // Number of times this filter has matched
    
    @ColumnInfo(name = "last_matched")
    private long lastMatched; // Timestamp of last match
    
    @ColumnInfo(name = "created_timestamp")
    private long createdTimestamp;
    
    @ColumnInfo(name = "modified_timestamp")
    private long modifiedTimestamp;
    
    // Filter Type Constants
    public static final String TYPE_KEYWORD = "KEYWORD";
    public static final String TYPE_SENDER_NUMBER = "SENDER_NUMBER";
    public static final String TYPE_WHITELIST = "WHITELIST";
    public static final String TYPE_BLACKLIST = "BLACKLIST";
    public static final String TYPE_SIM_BASED = "SIM_BASED";
    
    // Action Constants
    public static final String ACTION_ALLOW = "ALLOW";
    public static final String ACTION_BLOCK = "BLOCK";
    
    // Constructor
    public SmsFilter(String filterName, String filterType, String pattern, String action, boolean isEnabled) {
        this.filterName = filterName;
        this.filterType = filterType;
        this.pattern = pattern;
        this.action = action;
        this.isEnabled = isEnabled;
        this.isCaseSensitive = false;
        this.isRegex = false;
        this.priority = 0;
        this.matchCount = 0;
        this.lastMatched = 0;
        this.createdTimestamp = System.currentTimeMillis();
        this.modifiedTimestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getFilterName() {
        return filterName;
    }
    
    public void setFilterName(String filterName) {
        this.filterName = filterName;
        this.modifiedTimestamp = System.currentTimeMillis();
    }
    
    public String getFilterType() {
        return filterType;
    }
    
    public void setFilterType(String filterType) {
        this.filterType = filterType;
        this.modifiedTimestamp = System.currentTimeMillis();
    }
    
    public String getPattern() {
        return pattern;
    }
    
    public void setPattern(String pattern) {
        this.pattern = pattern;
        this.modifiedTimestamp = System.currentTimeMillis();
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
        this.modifiedTimestamp = System.currentTimeMillis();
    }
    
    public boolean isEnabled() {
        return isEnabled;
    }
    
    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
        this.modifiedTimestamp = System.currentTimeMillis();
    }
    
    public boolean isCaseSensitive() {
        return isCaseSensitive;
    }
    
    public void setCaseSensitive(boolean caseSensitive) {
        isCaseSensitive = caseSensitive;
        this.modifiedTimestamp = System.currentTimeMillis();
    }
    
    public boolean isRegex() {
        return isRegex;
    }
    
    public void setRegex(boolean regex) {
        isRegex = regex;
        this.modifiedTimestamp = System.currentTimeMillis();
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
        this.modifiedTimestamp = System.currentTimeMillis();
    }
    
    
    public int getMatchCount() {
        return matchCount;
    }
    
    public void setMatchCount(int matchCount) {
        this.matchCount = matchCount;
    }
    
    public long getLastMatched() {
        return lastMatched;
    }
    
    public void setLastMatched(long lastMatched) {
        this.lastMatched = lastMatched;
    }
    
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }
    
    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }
    
    public long getModifiedTimestamp() {
        return modifiedTimestamp;
    }
    
    public void setModifiedTimestamp(long modifiedTimestamp) {
        this.modifiedTimestamp = modifiedTimestamp;
    }
    
    /**
     * Increment match count and update last matched timestamp
     */
    public void incrementMatchCount() {
        this.matchCount++;
        this.lastMatched = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return filterName + " (" + filterType + " - " + action + ")";
    }
}