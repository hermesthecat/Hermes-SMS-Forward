package com.keremgok.sms.remote;

import android.util.Log;

import com.keremgok.sms.PhoneNumberValidator;

/**
 * Processor for parsing and validating remote SMS commands
 * Command format: SMS_GONDER [SIM] [NUMBER] [MESSAGE]
 */
public class RemoteCommandProcessor {
    
    private static final String TAG = "RemoteCommandProcessor";
    public static final String COMMAND_PREFIX = "SMS_GONDER";
    public static final String COMMAND_PREFIX_ALT = "SMS_SEND"; // English alternative
    
    /**
     * Parsed command result
     */
    public static class ParsedCommand {
        public String simMode;        // "SIM1", "SIM2", "AUTO"
        public String targetNumber;   // "+905551234567"
        public String messageContent; // "Actual message to send"
        public boolean isValid;
        public String errorMessage;
        
        public ParsedCommand() {
            this.isValid = false;
        }
        
        public static ParsedCommand error(String message) {
            ParsedCommand cmd = new ParsedCommand();
            cmd.isValid = false;
            cmd.errorMessage = message;
            return cmd;
        }
        
        public static ParsedCommand success(String sim, String target, String message) {
            ParsedCommand cmd = new ParsedCommand();
            cmd.isValid = true;
            cmd.simMode = sim;
            cmd.targetNumber = target;
            cmd.messageContent = message;
            return cmd;
        }
        
        @Override
        public String toString() {
            if (!isValid) {
                return "ParsedCommand{error='" + errorMessage + "'}";
            }
            return "ParsedCommand{sim=" + simMode + ", target=" + maskNumber(targetNumber) + ", messageLength=" + 
                   (messageContent != null ? messageContent.length() : 0) + "}";
        }
        
        private String maskNumber(String number) {
            if (number == null || number.length() < 4) return "***";
            return number.substring(0, 4) + "***" + number.substring(number.length() - 3);
        }
    }
    
    /**
     * Check if SMS body contains a remote command
     */
    public static boolean isRemoteCommand(String smsBody) {
        if (smsBody == null || smsBody.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = smsBody.trim().toUpperCase();
        return trimmed.startsWith(COMMAND_PREFIX) || trimmed.startsWith(COMMAND_PREFIX_ALT);
    }
    
    /**
     * Parse a remote SMS command
     * 
     * Format: SMS_GONDER [SIM] [NUMBER] [MESSAGE]
     * 
     * Examples:
     * - SMS_GONDER SIM1 +905551234567 Test message
     * - SMS_GONDER SIM2 +905559876543 Urgent: Please call back
     * - SMS_GONDER AUTO +905551111111 Multi word message content here
     * 
     * @param smsBody The SMS message body
     * @return ParsedCommand with parsed components or error
     */
    public static ParsedCommand parseCommand(String smsBody) {
        try {
            if (smsBody == null || smsBody.trim().isEmpty()) {
                return ParsedCommand.error("Empty command");
            }
            
            String trimmed = smsBody.trim();
            String upper = trimmed.toUpperCase();
            
            // Check if starts with command prefix
            String prefix;
            if (upper.startsWith(COMMAND_PREFIX)) {
                prefix = COMMAND_PREFIX;
            } else if (upper.startsWith(COMMAND_PREFIX_ALT)) {
                prefix = COMMAND_PREFIX_ALT;
            } else {
                return ParsedCommand.error("Invalid command prefix. Use: SMS_GONDER or SMS_SEND");
            }
            
            // Remove prefix
            String commandBody = trimmed.substring(prefix.length()).trim();
            
            if (commandBody.isEmpty()) {
                return ParsedCommand.error("Missing command parameters. Format: SMS_GONDER [SIM] [NUMBER] [MESSAGE]");
            }
            
            // Split by space (max 3 parts: SIM TARGET MESSAGE)
            String[] parts = commandBody.split("\\s+", 3);
            
            if (parts.length < 3) {
                return ParsedCommand.error("Missing required parameters. Format: SMS_GONDER [SIM] [NUMBER] [MESSAGE]");
            }
            
            // Parse SIM selection
            String simPart = parts[0].toUpperCase();
            if (!isValidSimMode(simPart)) {
                return ParsedCommand.error("Invalid SIM selection '" + simPart + "'. Use: SIM1, SIM2, or AUTO");
            }
            
            // Parse target number
            String targetPart = parts[1].trim();
            if (!PhoneNumberValidator.isValid(targetPart)) {
                return ParsedCommand.error("Invalid target phone number");
            }
            
            // Parse message content
            String messagePart = parts[2].trim();
            if (messagePart.isEmpty()) {
                return ParsedCommand.error("Message content cannot be empty");
            }
            
            // Check message length (SMS limit)
            if (messagePart.length() > 1600) { // 10 SMS parts max
                return ParsedCommand.error("Message too long (max 1600 characters)");
            }
            
            Log.i(TAG, "Successfully parsed command: SIM=" + simPart + ", target=" + 
                  maskPhoneNumber(targetPart) + ", msgLength=" + messagePart.length());
            
            return ParsedCommand.success(simPart, targetPart, messagePart);
            
        } catch (Exception e) {
            Log.e(TAG, "Parse error: " + e.getMessage(), e);
            return ParsedCommand.error("Parse error: " + e.getMessage());
        }
    }
    
    /**
     * Check if SIM mode is valid
     */
    private static boolean isValidSimMode(String simMode) {
        return simMode.equals("SIM1") || simMode.equals("SIM2") || simMode.equals("AUTO");
    }
    
    /**
     * Mask phone number for logging (privacy)
     */
    private static String maskPhoneNumber(String number) {
        if (number == null || number.length() < 4) {
            return "***";
        }
        
        String prefix = number.substring(0, Math.min(4, number.length()));
        String suffix = number.length() > 7 ? number.substring(number.length() - 3) : "";
        
        return prefix + "***" + suffix;
    }
    
    /**
     * Convert SIM mode to subscription ID (-1 for auto, 0 for SIM1, 1 for SIM2)
     * Note: Actual mapping should be done by SmsSimSelectionHelper
     */
    public static int simModeToSlot(String simMode) {
        switch (simMode.toUpperCase()) {
            case "SIM1":
                return 0;
            case "SIM2":
                return 1;
            case "AUTO":
            default:
                return -1; // Auto selection
        }
    }
    
    /**
     * Get help text for command format
     */
    public static String getHelpText() {
        return "Remote SMS Command Format:\n\n" +
               "SMS_GONDER [SIM] [NUMBER] [MESSAGE]\n\n" +
               "SIM Options:\n" +
               "  SIM1 - Use SIM card 1\n" +
               "  SIM2 - Use SIM card 2\n" +
               "  AUTO - Automatic selection\n\n" +
               "Examples:\n" +
               "SMS_GONDER SIM1 +905551234567 Test message\n" +
               "SMS_GONDER AUTO +905559876543 Urgent call back\n\n" +
               "Note: Phone number must include country code (+90 for Turkey)";
    }
}
