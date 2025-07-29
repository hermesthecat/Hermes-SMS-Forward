package com.keremgok.sms;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * SMS Filter Engine
 * Handles SMS filtering logic based on configured filter rules
 */
public class FilterEngine {
    
    private static final String TAG = "FilterEngine";
    private static final boolean DEBUG = true;
    
    private Context context;
    private AppDatabase database;
    private SmsFilterDao filterDao;
    
    // Filter Result Class
    public static class FilterResult {
        private boolean shouldForward;
        private String reason;
        private SmsFilter matchedFilter;
        
        public FilterResult(boolean shouldForward, String reason, SmsFilter matchedFilter) {
            this.shouldForward = shouldForward;
            this.reason = reason;
            this.matchedFilter = matchedFilter;
        }
        
        public boolean shouldForward() {
            return shouldForward;
        }
        
        public String getReason() {
            return reason;
        }
        
        public SmsFilter getMatchedFilter() {
            return matchedFilter;
        }
        
        public String getFilterType() {
            return matchedFilter != null ? matchedFilter.getFilterType() : "UNKNOWN";
        }
    }
    
    public FilterEngine(Context context) {
        this.context = context;
        this.database = AppDatabase.getInstance(context);
        this.filterDao = database.smsFilterDao();
    }
    
    /**
     * Apply all enabled filters to an SMS and determine if it should be forwarded
     * @param senderNumber The sender's phone number
     * @param messageContent The SMS message content
     * @param timestamp The SMS timestamp
     * @return FilterResult indicating whether to forward and why
     */
    public FilterResult applyFilters(String senderNumber, String messageContent, long timestamp) {
        return applyFilters(senderNumber, messageContent, timestamp, -1, -1);
    }
    
    /**
     * Apply all enabled filters to an SMS and determine if it should be forwarded (with SIM support)
     * @param senderNumber The sender's phone number
     * @param messageContent The SMS message content
     * @param timestamp The SMS timestamp
     * @param sourceSubscriptionId The subscription ID of the source SIM (-1 if not available)
     * @param sourceSimSlot The slot index of the source SIM (-1 if not available)
     * @return FilterResult indicating whether to forward and why
     */
    public FilterResult applyFilters(String senderNumber, String messageContent, long timestamp, int sourceSubscriptionId, int sourceSimSlot) {
        try {
            // Get all enabled filters ordered by priority
            List<SmsFilter> enabledFilters = filterDao.getEnabledFilters();
            
            if (enabledFilters == null || enabledFilters.isEmpty()) {
                logDebug("No enabled filters found, allowing SMS forwarding");
                return new FilterResult(true, "No filters configured", null);
            }
            
            logDebug("Applying " + enabledFilters.size() + " filters to SMS from: " + maskPhoneNumber(senderNumber));
            
            // Log SIM information if available (for debugging)
            if (sourceSubscriptionId != -1 || sourceSimSlot != -1) {
                logDebug("Applying filters with SIM info - Subscription ID: " + sourceSubscriptionId + ", Slot: " + sourceSimSlot);
            }
            
            // Apply filters in priority order
            for (SmsFilter filter : enabledFilters) {
                FilterResult result = applyFilter(filter, senderNumber, messageContent, timestamp, sourceSubscriptionId, sourceSimSlot);
                
                if (result != null) {
                    // Filter matched, update match count and return result
                    updateFilterMatchCount(filter.getId());
                    logDebug("Filter matched: " + filter.getFilterName() + " -> " + result.getReason());
                    return result;
                }
            }
            
            // No filters matched, allow forwarding by default
            logDebug("No filters matched, allowing SMS forwarding");
            return new FilterResult(true, "No filters matched", null);
            
        } catch (Exception e) {
            Log.e(TAG, "Error applying filters: " + e.getMessage(), e);
            // On error, default to allowing forwarding
            return new FilterResult(true, "Filter error: " + e.getMessage(), null);
        }
    }
    
    /**
     * Apply a single filter to an SMS
     * @param filter The filter to apply
     * @param senderNumber The sender's phone number
     * @param messageContent The SMS message content
     * @param timestamp The SMS timestamp
     * @param sourceSubscriptionId The subscription ID of the source SIM (-1 if not available)
     * @param sourceSimSlot The slot index of the source SIM (-1 if not available)
     * @return FilterResult if filter matches, null if no match
     */
    private FilterResult applyFilter(SmsFilter filter, String senderNumber, String messageContent, long timestamp, int sourceSubscriptionId, int sourceSimSlot) {
        try {
            boolean matches = false;
            String filterType = filter.getFilterType();
            
            switch (filterType) {
                case SmsFilter.TYPE_KEYWORD:
                    matches = applyKeywordFilter(filter, messageContent);
                    break;
                    
                case SmsFilter.TYPE_SENDER_NUMBER:
                    matches = applySenderFilter(filter, senderNumber);
                    break;
                    
                case SmsFilter.TYPE_TIME_BASED:
                    matches = applyTimeBasedFilter(filter, timestamp);
                    break;
                    
                case SmsFilter.TYPE_WHITELIST:
                    matches = applyWhitelistFilter(filter, senderNumber, messageContent);
                    break;
                    
                case SmsFilter.TYPE_BLACKLIST:
                    matches = applyBlacklistFilter(filter, senderNumber, messageContent);
                    break;
                    
                case SmsFilter.TYPE_SPAM_DETECTION:
                    matches = applySpamDetectionFilter(filter, senderNumber, messageContent);
                    break;
                    
                case "SIM_BASED":
                    matches = applySimBasedFilter(filter, sourceSubscriptionId, sourceSimSlot);
                    break;
                    
                default:
                    logDebug("Unknown filter type: " + filterType);
                    return null;
            }
            
            if (matches) {
                boolean shouldForward = SmsFilter.ACTION_ALLOW.equals(filter.getAction());
                String reason = filter.getFilterName() + " (" + filterType + " - " + filter.getAction() + ")";
                return new FilterResult(shouldForward, reason, filter);
            }
            
            return null; // No match
            
        } catch (Exception e) {
            Log.e(TAG, "Error applying filter " + filter.getFilterName() + ": " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Apply keyword-based filtering
     */
    private boolean applyKeywordFilter(SmsFilter filter, String messageContent) {
        if (TextUtils.isEmpty(messageContent) || TextUtils.isEmpty(filter.getPattern())) {
            return false;
        }
        
        String content = filter.isCaseSensitive() ? messageContent : messageContent.toLowerCase();
        String pattern = filter.isCaseSensitive() ? filter.getPattern() : filter.getPattern().toLowerCase();
        
        if (filter.isRegex()) {
            try {
                Pattern regexPattern = Pattern.compile(pattern);
                return regexPattern.matcher(content).find();
            } catch (PatternSyntaxException e) {
                Log.e(TAG, "Invalid regex pattern in filter " + filter.getFilterName() + ": " + e.getMessage());
                return false;
            }
        } else {
            return content.contains(pattern);
        }
    }
    
    /**
     * Apply sender number filtering
     */
    private boolean applySenderFilter(SmsFilter filter, String senderNumber) {
        if (TextUtils.isEmpty(senderNumber) || TextUtils.isEmpty(filter.getPattern())) {
            return false;
        }
        
        String pattern = filter.getPattern();
        
        if (filter.isRegex()) {
            try {
                Pattern regexPattern = Pattern.compile(pattern);
                return regexPattern.matcher(senderNumber).find();
            } catch (PatternSyntaxException e) {
                Log.e(TAG, "Invalid regex pattern in sender filter " + filter.getFilterName() + ": " + e.getMessage());
                return false;
            }
        } else {
            // Simple pattern matching (exact match or contains)
            return senderNumber.equals(pattern) || senderNumber.contains(pattern);
        }
    }
    
    /**
     * Apply time-based filtering (work hours, etc.)
     */
    private boolean applyTimeBasedFilter(SmsFilter filter, long timestamp) {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestamp);
            
            // Check day of week if specified
            String daysOfWeek = filter.getDaysOfWeek();
            if (!TextUtils.isEmpty(daysOfWeek)) {
                int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                // Convert to Monday=1 format (Calendar uses Sunday=1)
                int dayOfWeek = currentDayOfWeek == Calendar.SUNDAY ? 7 : currentDayOfWeek - 1;
                
                if (!daysOfWeek.contains(String.valueOf(dayOfWeek))) {
                    return false; // Not in allowed days
                }
            }
            
            // Check time range if specified
            String timeStart = filter.getTimeStart();
            String timeEnd = filter.getTimeEnd();
            
            if (!TextUtils.isEmpty(timeStart) && !TextUtils.isEmpty(timeEnd)) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                int currentHour = calendar.get(Calendar.HOUR_OF_DAY);
                int currentMinute = calendar.get(Calendar.MINUTE);
                int currentTimeMinutes = currentHour * 60 + currentMinute;
                
                try {
                    Date startTime = timeFormat.parse(timeStart);
                    Date endTime = timeFormat.parse(timeEnd);
                    
                    if (startTime != null && endTime != null) {
                        Calendar startCal = Calendar.getInstance();
                        Calendar endCal = Calendar.getInstance();
                        startCal.setTime(startTime);
                        endCal.setTime(endTime);
                        
                        int startMinutes = startCal.get(Calendar.HOUR_OF_DAY) * 60 + startCal.get(Calendar.MINUTE);
                        int endMinutes = endCal.get(Calendar.HOUR_OF_DAY) * 60 + endCal.get(Calendar.MINUTE);
                        
                        // Handle time range spanning midnight
                        if (startMinutes <= endMinutes) {
                            return currentTimeMinutes >= startMinutes && currentTimeMinutes <= endMinutes;
                        } else {
                            return currentTimeMinutes >= startMinutes || currentTimeMinutes <= endMinutes;
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing time in filter " + filter.getFilterName() + ": " + e.getMessage());
                    return false;
                }
            }
            
            return true; // Time-based filter passed
            
        } catch (Exception e) {
            Log.e(TAG, "Error in time-based filter " + filter.getFilterName() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Apply whitelist filtering (always allow)
     */
    private boolean applyWhitelistFilter(SmsFilter filter, String senderNumber, String messageContent) {
        // Whitelist can be based on sender or keyword
        return applySenderFilter(filter, senderNumber) || applyKeywordFilter(filter, messageContent);
    }
    
    /**
     * Apply blacklist filtering (always block)
     */
    private boolean applyBlacklistFilter(SmsFilter filter, String senderNumber, String messageContent) {
        // Blacklist can be based on sender or keyword
        return applySenderFilter(filter, senderNumber) || applyKeywordFilter(filter, messageContent);
    }
    
    /**
     * Apply spam detection filtering
     */
    private boolean applySpamDetectionFilter(SmsFilter filter, String senderNumber, String messageContent) {
        if (TextUtils.isEmpty(messageContent)) {
            return false;
        }
        
        // Basic spam detection heuristics
        String content = messageContent.toLowerCase();
        
        // Check for common spam keywords
        String[] spamKeywords = {
            "congratulations", "winner", "prize", "free", "click here", "urgent", 
            "limited time", "act now", "call now", "teklif", "hediye", "ücretsiz",
            "kazandınız", "ödül", "acele edin", "son fırsat", "kredi", "loan"
        };
        
        for (String keyword : spamKeywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }
        
        // Check for suspicious sender patterns
        if (!TextUtils.isEmpty(senderNumber)) {
            // Short codes (typically 4-6 digits) can be spam
            if (senderNumber.matches("\\d{4,6}")) {
                return true;
            }
            
            // Numbers with many repeated digits
            if (senderNumber.matches(".*([0-9])\\1{3,}.*")) {
                return true;
            }
        }
        
        // Check for excessive caps or special characters
        long capsCount = content.chars().filter(Character::isUpperCase).count();
        long totalLetters = content.chars().filter(Character::isLetter).count();
        
        if (totalLetters > 0 && (capsCount * 100 / totalLetters) > 50) {
            return true; // More than 50% caps
        }
        
        return false;
    }
    
    /**
     * Apply SIM-based filtering (for dual SIM support)
     * Allows filtering based on which SIM received the SMS
     * @param filter The SIM-based filter to apply
     * @param sourceSubscriptionId The subscription ID of the source SIM
     * @param sourceSimSlot The slot index of the source SIM
     * @return true if filter matches, false otherwise
     */
    private boolean applySimBasedFilter(SmsFilter filter, int sourceSubscriptionId, int sourceSimSlot) {
        try {
            // If SIM information is not available, skip SIM-based filtering
            if (sourceSubscriptionId == -1 && sourceSimSlot == -1) {
                logDebug("SIM information not available, skipping SIM-based filter: " + filter.getFilterName());
                return false;
            }
            
            String pattern = filter.getPattern();
            if (TextUtils.isEmpty(pattern)) {
                return false;
            }
            
            // Pattern can be:
            // "slot:0" or "slot:1" for specific SIM slots
            // "subscription:12345" for specific subscription IDs
            // "sim:SIM1" or "sim:SIM2" for named SIMs
            
            if (pattern.startsWith("slot:")) {
                try {
                    int targetSlot = Integer.parseInt(pattern.substring(5));
                    boolean matches = sourceSimSlot == targetSlot;
                    if (matches) {
                        logDebug("SIM slot filter matched: " + sourceSimSlot + " == " + targetSlot);
                    }
                    return matches;
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid slot pattern in SIM filter " + filter.getFilterName() + ": " + pattern);
                    return false;
                }
            }
            
            if (pattern.startsWith("subscription:")) {
                try {
                    int targetSubscriptionId = Integer.parseInt(pattern.substring(13));
                    boolean matches = sourceSubscriptionId == targetSubscriptionId;
                    if (matches) {
                        logDebug("SIM subscription filter matched: " + sourceSubscriptionId + " == " + targetSubscriptionId);
                    }
                    return matches;
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid subscription pattern in SIM filter " + filter.getFilterName() + ": " + pattern);
                    return false;
                }
            }
            
            if (pattern.startsWith("sim:")) {
                String simName = pattern.substring(4).toLowerCase();
                // Map SIM names to slots
                boolean matches = false;
                if ("sim1".equals(simName) && sourceSimSlot == 0) {
                    matches = true;
                } else if ("sim2".equals(simName) && sourceSimSlot == 1) {
                    matches = true;
                }
                
                if (matches) {
                    logDebug("SIM name filter matched: slot " + sourceSimSlot + " for " + simName);
                }
                return matches;
            }
            
            // Enhanced SIM filtering - get actual SIM information using SimManager
            if (sourceSubscriptionId != -1) {
                SimManager.SimInfo simInfo = SimManager.getSimInfo(context, sourceSubscriptionId);
                if (simInfo != null) {
                    // Check if pattern matches carrier name
                    if (!TextUtils.isEmpty(simInfo.carrierName) && 
                        simInfo.carrierName.toLowerCase().contains(pattern.toLowerCase())) {
                        logDebug("SIM carrier filter matched: " + simInfo.carrierName + " contains " + pattern);
                        return true;
                    }
                    
                    // Check if pattern matches display name
                    if (!TextUtils.isEmpty(simInfo.displayName) && 
                        simInfo.displayName.toLowerCase().contains(pattern.toLowerCase())) {
                        logDebug("SIM display name filter matched: " + simInfo.displayName + " contains " + pattern);
                        return true;
                    }
                }
            }
            
            logDebug("SIM-based filter did not match: " + pattern + " for subscription " + sourceSubscriptionId + ", slot " + sourceSimSlot);
            return false;
            
        } catch (Exception e) {
            Log.e(TAG, "Error in SIM-based filter " + filter.getFilterName() + ": " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Update filter match count in database
     */
    private void updateFilterMatchCount(int filterId) {
        ThreadManager.getInstance().executeDatabase(() -> {
            try {
                filterDao.incrementMatchCount(filterId, System.currentTimeMillis());
            } catch (Exception e) {
                Log.e(TAG, "Error updating filter match count: " + e.getMessage());
            }
        });
    }
    
    /**
     * Mask phone number for secure logging
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber) || phoneNumber.length() < 8) {
            return "***";
        }
        
        String prefix = phoneNumber.substring(0, Math.min(5, phoneNumber.length() - 4));
        String suffix = phoneNumber.substring(phoneNumber.length() - 4);
        return prefix + "***" + suffix;
    }
    
    /**
     * Secure debug logging
     */
    private void logDebug(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }
}