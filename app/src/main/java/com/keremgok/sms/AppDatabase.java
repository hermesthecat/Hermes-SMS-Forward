package com.keremgok.sms;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Main Room Database class for Hermes SMS Forward
 * Contains SMS history tracking and target numbers management
 */
@Database(
    entities = {SmsHistory.class, TargetNumber.class, SmsFilter.class, AnalyticsEvent.class, StatisticsSummary.class},
    version = 4,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "hermes_sms_database";
    private static volatile AppDatabase INSTANCE;
    
    /**
     * Get the SmsHistoryDao for database operations
     * @return SmsHistoryDao instance
     */
    public abstract SmsHistoryDao smsHistoryDao();
    
    /**
     * Get the TargetNumberDao for database operations
     * @return TargetNumberDao instance
     */
    public abstract TargetNumberDao targetNumberDao();
    
    /**
     * Get the SmsFilterDao for database operations
     * @return SmsFilterDao instance
     */
    public abstract SmsFilterDao smsFilterDao();
    
    /**
     * Get the AnalyticsEventDao for database operations
     * @return AnalyticsEventDao instance
     */
    public abstract AnalyticsEventDao analyticsEventDao();
    
    /**
     * Get the StatisticsSummaryDao for database operations
     * @return StatisticsSummaryDao instance
     */
    public abstract StatisticsSummaryDao statisticsSummaryDao();
    
    /**
     * Migration from version 1 to 2: Add target_numbers table and migrate SharedPreferences
     */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create target_numbers table
            database.execSQL("CREATE TABLE IF NOT EXISTS `target_numbers` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`phone_number` TEXT, " +
                "`display_name` TEXT, " +
                "`is_primary` INTEGER NOT NULL, " +
                "`is_enabled` INTEGER NOT NULL, " +
                "`created_timestamp` INTEGER NOT NULL, " +
                "`last_used_timestamp` INTEGER NOT NULL)");
        }
    };
    
    /**
     * Migration from version 2 to 3: Add sms_filters table for SMS filtering system
     */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create sms_filters table
            database.execSQL("CREATE TABLE IF NOT EXISTS `sms_filters` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`filter_name` TEXT, " +
                "`filter_type` TEXT, " +
                "`pattern` TEXT, " +
                "`action` TEXT, " +
                "`is_enabled` INTEGER NOT NULL, " +
                "`is_case_sensitive` INTEGER NOT NULL, " +
                "`is_regex` INTEGER NOT NULL, " +
                "`priority` INTEGER NOT NULL, " +
                "`time_start` TEXT, " +
                "`time_end` TEXT, " +
                "`days_of_week` TEXT, " +
                "`match_count` INTEGER NOT NULL, " +
                "`last_matched` INTEGER NOT NULL, " +
                "`created_timestamp` INTEGER NOT NULL, " +
                "`modified_timestamp` INTEGER NOT NULL)");
        }
    };
    
    /**
     * Migration from version 3 to 4: Add analytics and statistics tables
     */
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create analytics_events table
            database.execSQL("CREATE TABLE IF NOT EXISTS `analytics_events` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`event_type` TEXT, " +
                "`event_category` TEXT, " +
                "`event_action` TEXT, " +
                "`timestamp` INTEGER NOT NULL, " +
                "`duration_ms` INTEGER NOT NULL, " +
                "`error_code` TEXT, " +
                "`metadata` TEXT, " +
                "`app_version` TEXT, " +
                "`session_id` TEXT)");
            
            // Create statistics_summary table
            database.execSQL("CREATE TABLE IF NOT EXISTS `statistics_summary` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` TEXT, " +
                "`summary_type` TEXT, " +
                "`total_sms_received` INTEGER NOT NULL, " +
                "`total_sms_forwarded` INTEGER NOT NULL, " +
                "`successful_forwards` INTEGER NOT NULL, " +
                "`failed_forwards` INTEGER NOT NULL, " +
                "`success_rate` REAL NOT NULL, " +
                "`avg_processing_time_ms` REAL NOT NULL, " +
                "`error_count` INTEGER NOT NULL, " +
                "`most_common_error` TEXT, " +
                "`app_opens` INTEGER NOT NULL, " +
                "`session_duration_total_ms` INTEGER NOT NULL, " +
                "`avg_session_duration_ms` REAL NOT NULL, " +
                "`created_timestamp` INTEGER NOT NULL, " +
                "`last_updated` INTEGER NOT NULL)");
        }
    };
    
    /**
     * Get singleton instance of the database
     * Thread-safe implementation with double-checked locking
     * @param context Application context
     * @return AppDatabase singleton instance
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DATABASE_NAME
                    )
                    .allowMainThreadQueries() // For simple operations only
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(new RoomDatabase.Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                        }
                        
                        @Override
                        public void onOpen(@NonNull SupportSQLiteDatabase db) {
                            super.onOpen(db);
                            // Migrate SharedPreferences data on first open after migration
                            migrateSharedPreferencesData(context);
                        }
                    })
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * Clean up resources when app is destroyed (for testing)
     */
    public static void destroyInstance() {
        INSTANCE = null;
    }
    
    /**
     * Perform automatic cleanup of old records (30+ days old)
     * Should be called periodically in optimized background thread
     * @param context Application context
     */
    public static void performAutoCleanup(Context context) {
        ThreadManager.getInstance().executeBackground(() -> {
            AppDatabase db = getInstance(context);
            long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000); // 30 days in milliseconds
            
            // Clean up SMS history
            int deletedSmsCount = db.smsHistoryDao().deleteOldRecords(thirtyDaysAgo);
            
            // Clean up analytics events (keep 90 days for analytics)
            long ninetyDaysAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000);
            int deletedAnalyticsCount = db.analyticsEventDao().deleteOldEvents(ninetyDaysAgo);
            
            if (deletedSmsCount > 0) {
                android.util.Log.i("AppDatabase", "Auto cleanup: Deleted " + deletedSmsCount + " old SMS history records");
            }
            if (deletedAnalyticsCount > 0) {
                android.util.Log.i("AppDatabase", "Auto cleanup: Deleted " + deletedAnalyticsCount + " old analytics events");
            }
        });
    }
    
    /**
     * Migrate data from SharedPreferences to database
     * Called after database migration to preserve existing target number
     * @param context Application context
     */
    private static void migrateSharedPreferencesData(Context context) {
        ThreadManager.getInstance().executeBackground(() -> {
            try {
                AppDatabase db = getInstance(context);
                TargetNumberDao dao = db.targetNumberDao();
                
                // Check if migration has already been done
                if (dao.getEnabledTargetCount() > 0) {
                    return; // Already migrated
                }
                
                // Get target number from SharedPreferences
                SharedPreferences prefs = context.getSharedPreferences("HermesPrefs", Context.MODE_PRIVATE);
                String existingTargetNumber = prefs.getString("target_number", "");
                
                if (!existingTargetNumber.trim().isEmpty()) {
                    // Create target number record
                    TargetNumber targetNumber = new TargetNumber(
                        existingTargetNumber,
                        "Migrated Target", // Default display name
                        true, // Set as primary
                        true  // Enabled
                    );
                    
                    dao.insert(targetNumber);
                    
                    android.util.Log.i("AppDatabase", "Successfully migrated target number from SharedPreferences");
                    
                    // Clear from SharedPreferences to avoid future migrations
                    prefs.edit().remove("target_number").apply();
                }
                
            } catch (Exception e) {
                android.util.Log.e("AppDatabase", "Error migrating SharedPreferences data: " + e.getMessage(), e);
            }
        });
    }
}