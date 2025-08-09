package com.keremgok.sms;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import androidx.preference.PreferenceManager;
import java.util.Locale;

/**
 * Helper class to manage app language settings
 * Handles switching between Turkish, English, German, and automatic (system) language
 */
public class LanguageManager {
    
    private static final String PREF_LANGUAGE = "pref_app_language";
    private static final String LANGUAGE_AUTO = "auto";
    private static final String LANGUAGE_TURKISH = "tr";
    private static final String LANGUAGE_ENGLISH = "en";
    private static final String LANGUAGE_GERMAN = "de";
    
    /**
     * Apply the selected language to the context
     * @param context The context to apply language to
     * @return Updated context with applied language
     */
    public static Context applyLanguage(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String languageCode = prefs.getString(PREF_LANGUAGE, LANGUAGE_AUTO);
        
        if (LANGUAGE_AUTO.equals(languageCode)) {
            // Use system language - no need to override
            return context;
        }
        
        // Apply selected language
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        
        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale);
        } else {
            config.locale = locale;
        }
        
        return context.createConfigurationContext(config);
    }
    
    /**
     * Get the currently selected language code
     * @param context The context to get preferences from
     * @return Language code (auto, tr, en, de)
     */
    public static String getCurrentLanguage(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_LANGUAGE, LANGUAGE_AUTO);
    }
    
    /**
     * Set the app language
     * @param context The context to save preferences to
     * @param languageCode The language code to set (auto, tr, en, de)
     */
    public static void setLanguage(Context context, String languageCode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREF_LANGUAGE, languageCode).apply();
    }
    
    /**
     * Check if language override is needed (not auto)
     * @param context The context to check preferences
     * @return true if manual language is selected
     */
    public static boolean isLanguageOverrideEnabled(Context context) {
        return !LANGUAGE_AUTO.equals(getCurrentLanguage(context));
    }
}