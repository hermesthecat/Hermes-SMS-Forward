package com.keremgok.sms;

import android.text.TextUtils;
import java.util.regex.Pattern;

/**
 * Utility class for phone number validation
 * Supports international formats with special handling for Turkey (+90)
 */
public class PhoneNumberValidator {
    
    // Turkey country code pattern (+90 followed by 10 digits)
    private static final Pattern TURKEY_PATTERN = Pattern.compile("^\\+90[1-9][0-9]{9}$");
    
    // International pattern (+ followed by 1-3 digits country code, then 4-15 digits)
    private static final Pattern INTERNATIONAL_PATTERN = Pattern.compile("^\\+[1-9][0-9]{1,3}[0-9]{4,14}$");
    
    // Domestic Turkey pattern (5xx xxx xx xx format)
    private static final Pattern DOMESTIC_TURKEY_PATTERN = Pattern.compile("^5[0-9]{2}[0-9]{3}[0-9]{2}[0-9]{2}$");
    
    /**
     * Validates if the phone number is in correct format
     * @param phoneNumber The phone number to validate
     * @return ValidationResult containing validation status and error message
     */
    public static ValidationResult validate(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return new ValidationResult(false, "EMPTY_NUMBER");
        }
        
        // Remove all spaces and special characters except +
        String cleanNumber = phoneNumber.replaceAll("[\\s\\-\\(\\)\\.]", "");
        
        // Check if it's a valid Turkey number
        if (TURKEY_PATTERN.matcher(cleanNumber).matches()) {
            return new ValidationResult(true, null);
        }
        
        // Check if it's a domestic Turkey number (convert to international)
        if (DOMESTIC_TURKEY_PATTERN.matcher(cleanNumber).matches()) {
            return new ValidationResult(true, null, "+90" + cleanNumber);
        }
        
        // Check if it's a valid international number
        if (INTERNATIONAL_PATTERN.matcher(cleanNumber).matches()) {
            return new ValidationResult(true, null);
        }
        
        // Invalid format
        if (!cleanNumber.startsWith("+")) {
            return new ValidationResult(false, "MISSING_COUNTRY_CODE");
        }
        
        if (cleanNumber.length() < 8) {
            return new ValidationResult(false, "TOO_SHORT");
        }
        
        if (cleanNumber.length() > 18) {
            return new ValidationResult(false, "TOO_LONG");
        }
        
        return new ValidationResult(false, "INVALID_FORMAT");
    }
    
    /**
     * Quick validation check - returns only boolean
     * @param phoneNumber The phone number to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String phoneNumber) {
        return validate(phoneNumber).isValid();
    }
    
    /**
     * Formats phone number to international format
     * @param phoneNumber The phone number to format
     * @return Formatted phone number or original if already formatted
     */
    public static String formatToInternational(String phoneNumber) {
        ValidationResult result = validate(phoneNumber);
        if (result.isValid() && result.getFormattedNumber() != null) {
            return result.getFormattedNumber();
        }
        return phoneNumber;
    }
    
    /**
     * Validates Turkey-specific phone numbers
     * @param phoneNumber The phone number to validate
     * @return true if valid Turkey number
     */
    public static boolean isValidTurkeyNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return false;
        }
        
        String cleanNumber = phoneNumber.replaceAll("[\\s\\-\\(\\)\\.]", "");
        return TURKEY_PATTERN.matcher(cleanNumber).matches() || 
               DOMESTIC_TURKEY_PATTERN.matcher(cleanNumber).matches();
    }
    
    /**
     * Result class for validation operations
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorCode;
        private final String formattedNumber;
        
        public ValidationResult(boolean valid, String errorCode) {
            this(valid, errorCode, null);
        }
        
        public ValidationResult(boolean valid, String errorCode, String formattedNumber) {
            this.valid = valid;
            this.errorCode = errorCode;
            this.formattedNumber = formattedNumber;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
        
        public String getFormattedNumber() {
            return formattedNumber;
        }
    }
}