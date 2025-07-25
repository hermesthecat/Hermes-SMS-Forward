package com.keremgok.sms;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    private Context context;
    private SharedPreferences sharedPreferences;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        sharedPreferences = context.getSharedPreferences("HermesPrefs", Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().apply();
    }

    @After
    public void tearDown() {
        sharedPreferences.edit().clear().apply();
    }

    @Test
    public void testInitialUIState() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Espresso.onView(withId(R.id.etTargetNumber))
                    .check(ViewAssertions.matches(isDisplayed()));
            
            Espresso.onView(withId(R.id.btnSave))
                    .check(ViewAssertions.matches(isDisplayed()))
                    .check(ViewAssertions.matches(not(isEnabled())));
            
            Espresso.onView(withId(R.id.tvStatus))
                    .check(ViewAssertions.matches(isDisplayed()));
        }
    }

    @Test
    public void testPhoneNumberValidation_ValidNumber() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Espresso.onView(withId(R.id.etTargetNumber))
                    .perform(ViewActions.typeText("+905551234567"));
            
            Espresso.onView(withId(R.id.btnSave))
                    .check(ViewAssertions.matches(isEnabled()));
            
            Espresso.onView(withId(R.id.tvStatus))
                    .check(ViewAssertions.matches(withText("✓ Geçerli telefon numarası")));
        }
    }

    @Test
    public void testPhoneNumberValidation_InvalidNumber() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Espresso.onView(withId(R.id.etTargetNumber))
                    .perform(ViewActions.typeText("123"));
            
            Espresso.onView(withId(R.id.btnSave))
                    .check(ViewAssertions.matches(not(isEnabled())));
            
            Espresso.onView(withId(R.id.tvStatus))
                    .check(ViewAssertions.matches(withText("✗ Geçersiz telefon numarası formatı")));
        }
    }

    @Test
    public void testSaveButtonFunctionality() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            String validNumber = "+905551234567";
            
            Espresso.onView(withId(R.id.etTargetNumber))
                    .perform(ViewActions.typeText(validNumber));
            
            Espresso.onView(withId(R.id.btnSave))
                    .perform(ViewActions.click());
            
            Espresso.onView(withId(R.id.tvStatus))
                    .check(ViewAssertions.matches(withText("✓ Hedef numara kaydedildi: +90 555 123 4567")));
            
            String savedNumber = sharedPreferences.getString("target_number", "");
            assertEquals(validNumber, savedNumber);
        }
    }

    @Test
    public void testFormPersistence_LoadSavedNumber() {
        String savedNumber = "+905551234567";
        sharedPreferences.edit().putString("target_number", savedNumber).apply();
        
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Espresso.onView(withId(R.id.etTargetNumber))
                    .check(ViewAssertions.matches(withText(savedNumber)));
            
            Espresso.onView(withId(R.id.btnSave))
                    .check(ViewAssertions.matches(isEnabled()));
        }
    }

    @Test
    public void testEmptyInput() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Espresso.onView(withId(R.id.etTargetNumber))
                    .perform(ViewActions.typeText("abc"))
                    .perform(ViewActions.clearText());
            
            Espresso.onView(withId(R.id.btnSave))
                    .check(ViewAssertions.matches(not(isEnabled())));
            
            Espresso.onView(withId(R.id.tvStatus))
                    .check(ViewAssertions.matches(withText("Lütfen hedef telefon numarasını girin")));
        }
    }

    @Test
    public void testTurkishDomesticNumber() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Espresso.onView(withId(R.id.etTargetNumber))
                    .perform(ViewActions.typeText("05551234567"));
            
            Espresso.onView(withId(R.id.btnSave))
                    .check(ViewAssertions.matches(isEnabled()));
            
            Espresso.onView(withId(R.id.tvStatus))
                    .check(ViewAssertions.matches(withText("✓ Geçerli telefon numarası (Türkiye)")));
        }
    }

    @Test
    public void testLongInvalidNumber() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Espresso.onView(withId(R.id.etTargetNumber))
                    .perform(ViewActions.typeText("+905551234567890"));
            
            Espresso.onView(withId(R.id.btnSave))
                    .check(ViewAssertions.matches(not(isEnabled())));
        }
    }
}