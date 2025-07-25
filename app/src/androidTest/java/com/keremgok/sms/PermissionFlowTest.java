package com.keremgok.sms;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class PermissionFlowTest {

    @Test
    public void testSMSPermissionsGranted() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        
        int receiveSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS);
        int sendSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS);
        
        assertNotNull("Context should not be null", context);
    }

    @Test
    public void testApplicationPermissionDeclaration() {
        Context context = ApplicationProvider.getApplicationContext();
        
        try {
            String[] requestedPermissions = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
            
            boolean hasReceiveSms = false;
            boolean hasSendSms = false;
            
            if (requestedPermissions != null) {
                for (String permission : requestedPermissions) {
                    if (Manifest.permission.RECEIVE_SMS.equals(permission)) {
                        hasReceiveSms = true;
                    }
                    if (Manifest.permission.SEND_SMS.equals(permission)) {
                        hasSendSms = true;
                    }
                }
            }
            
            assertEquals("RECEIVE_SMS permission should be declared", true, hasReceiveSms);
            assertEquals("SEND_SMS permission should be declared", true, hasSendSms);
            
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Package not found", e);
        }
    }

    @Test
    public void testMainActivityLaunchWithPermissions() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                assertNotNull("MainActivity should launch successfully", activity);
                assertNotNull("Activity context should be available", activity.getApplicationContext());
            });
        }
    }

    @Test
    public void testPermissionConstants() {
        assertEquals("RECEIVE_SMS constant should match", 
                "android.permission.RECEIVE_SMS", Manifest.permission.RECEIVE_SMS);
        assertEquals("SEND_SMS constant should match", 
                "android.permission.SEND_SMS", Manifest.permission.SEND_SMS);
    }

    @Test
    public void testPackageManagerAccess() {
        Context context = ApplicationProvider.getApplicationContext();
        PackageManager packageManager = context.getPackageManager();
        
        assertNotNull("PackageManager should be accessible", packageManager);
        assertEquals("Package name should be correct", 
                "com.keremgok.sms", context.getPackageName());
    }
}