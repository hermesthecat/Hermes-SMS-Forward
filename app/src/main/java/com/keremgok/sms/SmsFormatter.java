package com.keremgok.sms;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * SMS formatting system with multilingual support and customizable templates
 * Supports multiple format types and user customization
 */
public class SmsFormatter {
    
    private static final String TAG = "SmsFormatter";
    private static final String PREFS_NAME = "HermesPrefs";
    
    // Format types
    public static final String FORMAT_STANDARD = "standard";
    public static final String FORMAT_COMPACT = "compact";
    public static final String FORMAT_DETAILED = "detailed";
    public static final String FORMAT_CUSTOM = "custom";
    
    // Default settings
    private static final String DEFAULT_FORMAT_TYPE = FORMAT_STANDARD;
    private static final boolean DEFAULT_INCLUDE_SIM_INFO = true;
    private static final boolean DEFAULT_INCLUDE_TIMESTAMP = true;
    private static final String DEFAULT_CUSTOM_HEADER = "Hermes SMS Forward";
    
    private final Context context;
    private final SharedPreferences prefs;
    private final boolean isTurkish;
    
    public SmsFormatter(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Detect language
        String currentLanguage = context.getResources().getConfiguration().locale.getLanguage();
        this.isTurkish = "tr".equals(currentLanguage);
    }
    
    /**
     * Format SMS message with all available information
     */
    public String formatMessage(String originalSender, String originalMessage, long timestamp,
                               int sourceSimSlot, int forwardingSimSlot, 
                               int sourceSubscriptionId, int forwardingSubscriptionId) {
        
        String formatType = prefs.getString("sms_format_type", DEFAULT_FORMAT_TYPE);
        
        switch (formatType) {
            case FORMAT_COMPACT:
                return formatCompact(originalSender, originalMessage, timestamp, sourceSimSlot, forwardingSimSlot);
            case FORMAT_DETAILED:
                return formatDetailed(originalSender, originalMessage, timestamp, 
                                    sourceSimSlot, forwardingSimSlot, sourceSubscriptionId, forwardingSubscriptionId);
            case FORMAT_CUSTOM:
                return formatCustom(originalSender, originalMessage, timestamp, 
                                  sourceSimSlot, forwardingSimSlot, sourceSubscriptionId, forwardingSubscriptionId);
            case FORMAT_STANDARD:
            default:
                return formatStandard(originalSender, originalMessage, timestamp, sourceSimSlot, forwardingSimSlot);
        }
    }
    
    /**
     * Format SMS message (backward compatibility)
     */
    public String formatMessage(String originalSender, String originalMessage, long timestamp) {
        return formatMessage(originalSender, originalMessage, timestamp, -1, -1, -1, -1);
    }
    
    /**
     * Standard format - similar to current format but with multilingual support
     */
    private String formatStandard(String originalSender, String originalMessage, long timestamp, 
                                int sourceSimSlot, int forwardingSimSlot) {
        StringBuilder sb = new StringBuilder();
        
        // Header
        String header = getCustomHeader();
        sb.append("[").append(header).append("]\n");
        
        // Sender
        if (isTurkish) {
            sb.append("Gönderen: ").append(originalSender).append("\n");
        } else {
            sb.append("From: ").append(originalSender).append("\n");
        }
        
        // Message
        if (isTurkish) {
            sb.append("Mesaj: ").append(originalMessage).append("\n");
        } else {
            sb.append("Message: ").append(originalMessage).append("\n");
        }
        
        // Timestamp
        if (shouldIncludeTimestamp()) {
            String timeLabel = isTurkish ? "Zaman: " : "Time: ";
            sb.append(timeLabel).append(formatTimestamp(timestamp));
        }
        
        // SIM info
        if (shouldIncludeSimInfo() && (sourceSimSlot != -1 || forwardingSimSlot != -1)) {
            sb.append("\n").append(formatSimInfo(sourceSimSlot, forwardingSimSlot));
        }
        
        return sb.toString();
    }
    
    /**
     * Compact format - minimal information
     */
    private String formatCompact(String originalSender, String originalMessage, long timestamp,
                               int sourceSimSlot, int forwardingSimSlot) {
        StringBuilder sb = new StringBuilder();
        
        // Just sender and message
        sb.append(originalSender).append(": ").append(originalMessage);
        
        // Add SIM info if available and enabled
        if (shouldIncludeSimInfo() && sourceSimSlot != -1) {
            sb.append(" (SIM").append(sourceSimSlot + 1).append(")");
        }
        
        return sb.toString();
    }
    
    /**
     * Detailed format - all available information
     */
    private String formatDetailed(String originalSender, String originalMessage, long timestamp,
                                int sourceSimSlot, int forwardingSimSlot,
                                int sourceSubscriptionId, int forwardingSubscriptionId) {
        StringBuilder sb = new StringBuilder();
        
        // Header with app version
        String header = getCustomHeader();
        sb.append("═══════════════════════\n");
        sb.append("  ").append(header).append("\n");
        sb.append("═══════════════════════\n");
        
        // Message details
        String senderLabel = isTurkish ? "📱 Gönderen" : "📱 Sender";
        String messageLabel = isTurkish ? "💬 Mesaj" : "💬 Message";
        String timeLabel = isTurkish ? "🕐 Zaman" : "🕐 Time";
        
        sb.append("\n").append(senderLabel).append(": ").append(originalSender).append("\n");
        sb.append(messageLabel).append(": ").append(originalMessage).append("\n");
        
        if (shouldIncludeTimestamp()) {
            sb.append(timeLabel).append(": ").append(formatTimestamp(timestamp)).append("\n");
        }
        
        // SIM information
        if (shouldIncludeSimInfo()) {
            sb.append(formatDetailedSimInfo(sourceSimSlot, forwardingSimSlot, 
                                          sourceSubscriptionId, forwardingSubscriptionId));
        }
        
        // Footer
        sb.append("\n").append("━━━━━━━━━━━━━━━━━━━━━━━");
        
        return sb.toString();
    }
    
    /**
     * Custom format based on user template
     */
    private String formatCustom(String originalSender, String originalMessage, long timestamp,
                              int sourceSimSlot, int forwardingSimSlot,
                              int sourceSubscriptionId, int forwardingSubscriptionId) {
        
        String customTemplate = prefs.getString("custom_sms_template", getDefaultCustomTemplate());
        
        // Replace placeholders
        String formatted = customTemplate
            .replace("{HEADER}", getCustomHeader())
            .replace("{SENDER}", originalSender != null ? originalSender : "")
            .replace("{MESSAGE}", originalMessage != null ? originalMessage : "")
            .replace("{TIME}", formatTimestamp(timestamp))
            .replace("{SIM_INFO}", formatSimInfo(sourceSimSlot, forwardingSimSlot))
            .replace("{APP_NAME}", context.getString(R.string.app_name));
        
        return formatted;
    }
    
    /**
     * Format SIM information for standard/compact formats
     */
    private String formatSimInfo(int sourceSimSlot, int forwardingSimSlot) {
        if (sourceSimSlot == -1 && forwardingSimSlot == -1) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        String simLabel = isTurkish ? "SIM: " : "SIM: ";
        
        if (sourceSimSlot != -1 && forwardingSimSlot != -1 && sourceSimSlot != forwardingSimSlot) {
            // Different SIMs
            String fromLabel = isTurkish ? "dan " : " to ";
            sb.append(simLabel).append("SIM").append(sourceSimSlot + 1).append(fromLabel).append("SIM").append(forwardingSimSlot + 1);
        } else if (sourceSimSlot != -1) {
            sb.append(simLabel).append("SIM").append(sourceSimSlot + 1);
        } else if (forwardingSimSlot != -1) {
            sb.append(simLabel).append("SIM").append(forwardingSimSlot + 1);
        }
        
        return sb.toString();
    }
    
    /**
     * Format detailed SIM information
     */
    private String formatDetailedSimInfo(int sourceSimSlot, int forwardingSimSlot,
                                       int sourceSubscriptionId, int forwardingSubscriptionId) {
        if (sourceSimSlot == -1 && forwardingSimSlot == -1 && 
            sourceSubscriptionId == -1 && forwardingSubscriptionId == -1) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        String simInfoLabel = isTurkish ? "\n📡 SIM Bilgisi:" : "\n📡 SIM Information:";
        sb.append(simInfoLabel).append("\n");
        
        // Source SIM info
        if (sourceSimSlot != -1 || sourceSubscriptionId != -1) {
            String receivingLabel = isTurkish ? "  📥 Alınan SIM: " : "  📥 Received via: ";
            sb.append(receivingLabel);
            
            if (sourceSimSlot != -1) {
                sb.append("SIM ").append(sourceSimSlot + 1);
                
                // Add carrier name if available
                String carrierName = getCarrierName(sourceSubscriptionId);
                if (!TextUtils.isEmpty(carrierName)) {
                    sb.append(" (").append(carrierName).append(")");
                }
            } else if (sourceSubscriptionId != -1) {
                sb.append("Subscription ").append(sourceSubscriptionId);
            }
            sb.append("\n");
        }
        
        // Forwarding SIM info
        if (forwardingSimSlot != -1 || forwardingSubscriptionId != -1) {
            String forwardingLabel = isTurkish ? "  📤 İletilen SIM: " : "  📤 Forwarded via: ";
            sb.append(forwardingLabel);
            
            if (forwardingSimSlot != -1) {
                sb.append("SIM ").append(forwardingSimSlot + 1);
                
                // Add carrier name if available
                String carrierName = getCarrierName(forwardingSubscriptionId);
                if (!TextUtils.isEmpty(carrierName)) {
                    sb.append(" (").append(carrierName).append(")");
                }
            } else if (forwardingSubscriptionId != -1) {
                sb.append("Subscription ").append(forwardingSubscriptionId);
            }
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Get carrier name for subscription ID
     */
    private String getCarrierName(int subscriptionId) {
        try {
            if (subscriptionId != -1) {
                SimManager.SimInfo simInfo = SimManager.getSimInfo(context, subscriptionId);
                return simInfo != null ? simInfo.carrierName : null;
            }
        } catch (Exception e) {
            // Ignore errors
        }
        return null;
    }
    
    /**
     * Format timestamp based on user preferences
     */
    private String formatTimestamp(long timestamp) {
        String dateFormat = prefs.getString("date_format", "dd/MM/yyyy HH:mm:ss");
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    
    /**
     * Get custom header from preferences
     */
    private String getCustomHeader() {
        return prefs.getString("custom_header", DEFAULT_CUSTOM_HEADER);
    }
    
    /**
     * Check if timestamp should be included
     */
    private boolean shouldIncludeTimestamp() {
        return prefs.getBoolean("include_timestamp", DEFAULT_INCLUDE_TIMESTAMP);
    }
    
    /**
     * Check if SIM info should be included
     */
    private boolean shouldIncludeSimInfo() {
        return prefs.getBoolean("include_sim_info", DEFAULT_INCLUDE_SIM_INFO);
    }
    
    /**
     * Get default custom template
     */
    private String getDefaultCustomTemplate() {
        if (isTurkish) {
            return "[{HEADER}]\nGönderen: {SENDER}\nMesaj: {MESSAGE}\nZaman: {TIME}\n{SIM_INFO}";
        } else {
            return "[{HEADER}]\nFrom: {SENDER}\nMessage: {MESSAGE}\nTime: {TIME}\n{SIM_INFO}";
        }
    }
    
    /**
     * Save format preferences
     */
    public void saveFormatPreferences(String formatType, boolean includeTimestamp, 
                                    boolean includeSimInfo, String customHeader, String customTemplate) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("sms_format_type", formatType);
        editor.putBoolean("include_timestamp", includeTimestamp);
        editor.putBoolean("include_sim_info", includeSimInfo);
        editor.putString("custom_header", customHeader);
        if (!TextUtils.isEmpty(customTemplate)) {
            editor.putString("custom_sms_template", customTemplate);
        }
        editor.apply();
    }
    
    /**
     * Get current format type
     */
    public String getCurrentFormatType() {
        return prefs.getString("sms_format_type", DEFAULT_FORMAT_TYPE);
    }
    
    /**
     * Get format preview for settings
     */
    public String getFormatPreview(String formatType) {
        String originalSender = "+905551234567";
        String originalMessage = isTurkish ? "Örnek SMS mesajı" : "Sample SMS message";
        long timestamp = System.currentTimeMillis();
        
        // Temporarily change format type for preview
        String currentFormat = getCurrentFormatType();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("sms_format_type", formatType);
        editor.apply();
        
        // Generate preview
        String preview = formatMessage(originalSender, originalMessage, timestamp, 0, 1, 1, 2);
        
        // Restore original format
        editor.putString("sms_format_type", currentFormat);
        editor.apply();
        
        return preview;
    }
}