package com.keremgok.sms;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Simple unit tests for PhoneNumberValidator without Android dependencies
 */
public class SimplePhoneNumberValidatorTest {

    @Test
    public void testValidTurkeyInternationalNumber() {
        PhoneNumberValidator.ValidationResult result = 
            PhoneNumberValidator.validate("+905551234567");
        
        assertTrue("Turkey international number should be valid", result.isValid());
        assertNull("No error code expected for valid number", result.getErrorCode());
    }

    @Test
    public void testValidTurkeyDomesticNumber() {
        PhoneNumberValidator.ValidationResult result = 
            PhoneNumberValidator.validate("5551234567");
        
        assertTrue("Turkey domestic number should be valid", result.isValid());
        assertNull("No error code expected for valid number", result.getErrorCode());
        assertEquals("Should format to international", "+905551234567", result.getFormattedNumber());
    }

    @Test
    public void testEmptyNumber() {
        PhoneNumberValidator.ValidationResult result = 
            PhoneNumberValidator.validate("");
        
        assertFalse("Empty number should be invalid", result.isValid());
        assertEquals("Should return EMPTY_NUMBER error", "EMPTY_NUMBER", result.getErrorCode());
    }

    @Test
    public void testNullNumber() {
        PhoneNumberValidator.ValidationResult result = 
            PhoneNumberValidator.validate(null);
        
        assertFalse("Null number should be invalid", result.isValid());
        assertEquals("Should return EMPTY_NUMBER error", "EMPTY_NUMBER", result.getErrorCode());
    }

    @Test
    public void testValidUSNumber() {
        PhoneNumberValidator.ValidationResult result = 
            PhoneNumberValidator.validate("+14155552671");
        
        assertTrue("Valid US number should be valid", result.isValid());
        assertNull("No error code expected for valid number", result.getErrorCode());
    }

    @Test
    public void testInvalidFormat() {
        PhoneNumberValidator.ValidationResult result = 
            PhoneNumberValidator.validate("invalid");
        
        assertFalse("Invalid format should be invalid", result.isValid());
        assertEquals("Should return MISSING_COUNTRY_CODE error", "MISSING_COUNTRY_CODE", result.getErrorCode());
    }

    @Test
    public void testTooShortNumber() {
        PhoneNumberValidator.ValidationResult result = 
            PhoneNumberValidator.validate("+90123");
        
        assertFalse("Too short number should be invalid", result.isValid());
        assertEquals("Should return TOO_SHORT error", "TOO_SHORT", result.getErrorCode());
    }

    @Test
    public void testTooLongNumber() {
        PhoneNumberValidator.ValidationResult result = 
            PhoneNumberValidator.validate("+901234567890123456789");
        
        assertFalse("Too long number should be invalid", result.isValid());
        assertEquals("Should return TOO_LONG error", "TOO_LONG", result.getErrorCode());
    }

    @Test
    public void testIsValidMethod() {
        assertTrue("Valid Turkey number should return true", 
                  PhoneNumberValidator.isValid("+905551234567"));
        assertTrue("Valid domestic Turkey number should return true", 
                  PhoneNumberValidator.isValid("5551234567"));
        assertFalse("Invalid number should return false", 
                   PhoneNumberValidator.isValid("invalid"));
        assertFalse("Empty number should return false", 
                   PhoneNumberValidator.isValid(""));
    }

    @Test
    public void testFormatToInternational() {
        assertEquals("Should format domestic Turkey number", 
                    "+905551234567", 
                    PhoneNumberValidator.formatToInternational("5551234567"));
        
        assertEquals("Should return international as-is", 
                    "+905551234567", 
                    PhoneNumberValidator.formatToInternational("+905551234567"));
        
        assertEquals("Should return invalid number as-is", 
                    "invalid", 
                    PhoneNumberValidator.formatToInternational("invalid"));
    }

    @Test
    public void testIsValidTurkeyNumber() {
        assertTrue("Valid Turkey international should be Turkey number", 
                  PhoneNumberValidator.isValidTurkeyNumber("+905551234567"));
        assertTrue("Valid Turkey domestic should be Turkey number", 
                  PhoneNumberValidator.isValidTurkeyNumber("5551234567"));
        assertFalse("US number should not be Turkey number", 
                   PhoneNumberValidator.isValidTurkeyNumber("+14155552671"));
        assertFalse("Empty should not be Turkey number", 
                   PhoneNumberValidator.isValidTurkeyNumber(""));
        assertFalse("Null should not be Turkey number", 
                   PhoneNumberValidator.isValidTurkeyNumber(null));
    }
}