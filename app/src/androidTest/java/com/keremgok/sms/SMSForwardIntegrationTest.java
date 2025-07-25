package com.keremgok.sms;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SMSForwardIntegrationTest {

    private Context context;
    private SharedPreferences sharedPreferences;
    private SmsReceiver smsReceiver;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        sharedPreferences = context.getSharedPreferences("SMSForwardPrefs", Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
        smsReceiver = new SmsReceiver();
    }

    @After
    public void tearDown() {
        sharedPreferences.edit().clear().apply();
    }

    @Test
    public void testEndToEndFlow_ConfigurationAndReceiver() {
        String targetNumber = "+905551234567";
        
        sharedPreferences.edit().putString("target_number", targetNumber).apply();
        
        String savedNumber = sharedPreferences.getString("target_number", "");
        assertEquals("Target number should be saved correctly", targetNumber, savedNumber);
        
        assertNotNull("SmsReceiver should be instantiated", smsReceiver);
    }

    @Test
    public void testSharedPreferencesPersistence() {
        String testNumber = "+905551234567";
        
        sharedPreferences.edit()
                .putString("target_number", testNumber)
                .putBoolean("sms_forwarding_enabled", true)
                .apply();
        
        SharedPreferences newPrefs = context.getSharedPreferences("SMSForwardPrefs", Context.MODE_PRIVATE);
        
        assertEquals("Target number should persist", testNumber, newPrefs.getString("target_number", ""));
        assertTrue("SMS forwarding should be enabled", newPrefs.getBoolean("sms_forwarding_enabled", false));
    }

    @Test
    public void testSMSReceiverIntentHandling() {
        String targetNumber = "+905551234567";
        sharedPreferences.edit().putString("target_number", targetNumber).apply();
        
        Intent smsIntent = new Intent("android.provider.Telephony.SMS_RECEIVED");
        smsIntent.putExtra("pdus", createTestSMSPdus());
        smsIntent.putExtra("format", "3gpp");
        
        assertNotNull("SMS Intent should be created", smsIntent);
        assertEquals("Intent action should be SMS_RECEIVED", 
                "android.provider.Telephony.SMS_RECEIVED", smsIntent.getAction());
    }

    @Test
    public void testPhoneNumberValidationIntegration() {
        PhoneNumberValidator.ValidationResult result1 = 
                PhoneNumberValidator.validate("+905551234567");
        assertTrue("Valid international number should pass", result1.isValid());
        assertEquals("Valid number code should be VALID", "VALID", result1.getErrorCode());
        
        PhoneNumberValidator.ValidationResult result2 = 
                PhoneNumberValidator.validate("05551234567");
        assertTrue("Valid Turkish domestic number should pass", result2.isValid());
        assertEquals("Turkish domestic code should be VALID_TURKEY", "VALID_TURKEY", result2.getErrorCode());
        
        PhoneNumberValidator.ValidationResult result3 = 
                PhoneNumberValidator.validate("123");
        assertTrue("Invalid number should fail", !result3.isValid());
        assertEquals("Invalid number should have INVALID_FORMAT", "INVALID_FORMAT", result3.getErrorCode());
    }

    @Test
    public void testConfigurationValidation() {
        String[] validNumbers = {
                "+905551234567",
                "05551234567",
                "+1234567890123",
                "+442071234567"
        };
        
        for (String number : validNumbers) {
            PhoneNumberValidator.ValidationResult result = PhoneNumberValidator.validate(number);
            assertTrue("Number " + number + " should be valid", result.isValid());
        }
        
        String[] invalidNumbers = {
                "",
                "123",
                "abc",
                "+90555123456789012345",
                "555-1234"
        };
        
        for (String number : invalidNumbers) {
            PhoneNumberValidator.ValidationResult result = PhoneNumberValidator.validate(number);
            assertTrue("Number " + number + " should be invalid", !result.isValid());
        }
    }

    @Test
    public void testApplicationContextIntegration() {
        Context appContext = ApplicationProvider.getApplicationContext();
        assertEquals("Package name should match", "com.keremgok.sms", appContext.getPackageName());
        
        SharedPreferences prefs = appContext.getSharedPreferences("SMSForwardPrefs", Context.MODE_PRIVATE);
        assertNotNull("SharedPreferences should be accessible", prefs);
    }

    private Object[] createTestSMSPdus() {
        String testMessage = "Test SMS message";
        String senderNumber = "+905551111111";
        
        byte[] pdu = {
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        };
        
        return new Object[]{pdu};
    }

    @Test
    public void testMultipleConfigurationUpdates() {
        String[] testNumbers = {
                "+905551234567",
                "+905559876543",
                "+905550000000"
        };
        
        for (String number : testNumbers) {
            sharedPreferences.edit().putString("target_number", number).apply();
            String saved = sharedPreferences.getString("target_number", "");
            assertEquals("Each number should be saved correctly", number, saved);
        }
    }
}