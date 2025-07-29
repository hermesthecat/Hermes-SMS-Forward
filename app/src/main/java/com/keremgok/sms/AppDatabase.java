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
    version = 7,
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
     * Migration from version 4 to 5: Add dual SIM support fields
     */
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            try {
                android.util.Log.i("AppDatabase", "Starting migration from version 4 to 5 (dual SIM support)");
                
                // Add dual SIM fields to target_numbers table with error handling
                try {
                    // Check if columns already exist first
                    android.database.Cursor cursor = database.query("PRAGMA table_info(target_numbers)");
                    boolean hasPreferredSimSlot = false;
                    boolean hasSimSelectionMode = false;
                    
                    while (cursor.moveToNext()) {
                        String columnName = cursor.getString(1); // Column name is at index 1
                        if ("preferred_sim_slot".equals(columnName)) {
                            hasPreferredSimSlot = true;
                        } else if ("sim_selection_mode".equals(columnName)) {
                            hasSimSelectionMode = true;
                        }
                    }
                    cursor.close();
                    
                    if (!hasPreferredSimSlot) {
                        database.execSQL("ALTER TABLE target_numbers ADD COLUMN preferred_sim_slot INTEGER DEFAULT -1");
                        android.util.Log.d("AppDatabase", "Added preferred_sim_slot column to target_numbers");
                    }
                    
                    if (!hasSimSelectionMode) {
                        database.execSQL("ALTER TABLE target_numbers ADD COLUMN sim_selection_mode TEXT DEFAULT 'auto'");
                        android.util.Log.d("AppDatabase", "Added sim_selection_mode column to target_numbers");
                    }
                    
                } catch (Exception e) {
                    android.util.Log.e("AppDatabase", "Error migrating target_numbers table: " + e.getMessage(), e);
                    throw e;
                }
                
                // Add dual SIM fields to sms_history table with error handling
                try {
                    // Check if columns already exist first
                    android.database.Cursor cursor = database.query("PRAGMA table_info(sms_history)");
                    boolean hasSourceSimSlot = false;
                    boolean hasForwardingSimSlot = false;
                    boolean hasSourceSubscriptionId = false;
                    boolean hasForwardingSubscriptionId = false;
                    
                    while (cursor.moveToNext()) {
                        String columnName = cursor.getString(1); // Column name is at index 1
                        switch (columnName) {
                            case "source_sim_slot":
                                hasSourceSimSlot = true;
                                break;
                            case "forwarding_sim_slot":
                                hasForwardingSimSlot = true;
                                break;
                            case "source_subscription_id":
                                hasSourceSubscriptionId = true;
                                break;
                            case "forwarding_subscription_id":
                                hasForwardingSubscriptionId = true;
                                break;
                        }
                    }
                    cursor.close();
                    
                    if (!hasSourceSimSlot) {
                        database.execSQL("ALTER TABLE sms_history ADD COLUMN source_sim_slot INTEGER DEFAULT -1");
                        android.util.Log.d("AppDatabase", "Added source_sim_slot column to sms_history");
                    }
                    
                    if (!hasForwardingSimSlot) {
                        database.execSQL("ALTER TABLE sms_history ADD COLUMN forwarding_sim_slot INTEGER DEFAULT -1");
                        android.util.Log.d("AppDatabase", "Added forwarding_sim_slot column to sms_history");
                    }
                    
                    if (!hasSourceSubscriptionId) {
                        database.execSQL("ALTER TABLE sms_history ADD COLUMN source_subscription_id INTEGER DEFAULT -1");
                        android.util.Log.d("AppDatabase", "Added source_subscription_id column to sms_history");
                    }
                    
                    if (!hasForwardingSubscriptionId) {
                        database.execSQL("ALTER TABLE sms_history ADD COLUMN forwarding_subscription_id INTEGER DEFAULT -1");
                        android.util.Log.d("AppDatabase", "Added forwarding_subscription_id column to sms_history");
                    }
                    
                } catch (Exception e) {
                    android.util.Log.e("AppDatabase", "Error migrating sms_history table: " + e.getMessage(), e);
                    throw e;
                }
                
                android.util.Log.i("AppDatabase", "Successfully completed migration from version 4 to 5");
                
            } catch (Exception e) {
                android.util.Log.e("AppDatabase", "Migration 4->5 failed: " + e.getMessage(), e);
                throw e; // Re-throw to trigger fallback
            }
        }
    };
    
    /**
     * Migration from version 5 to 6: Remove time-based filter columns
     */
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            try {
                android.util.Log.i("AppDatabase", "Starting migration from version 5 to 6 (removing time-based filter columns)");
                
                // Create new sms_filters table without time-based columns
                database.execSQL("CREATE TABLE IF NOT EXISTS `sms_filters_new` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`filter_name` TEXT, " +
                    "`filter_type` TEXT, " +
                    "`pattern` TEXT, " +
                    "`action` TEXT, " +
                    "`is_enabled` INTEGER NOT NULL, " +
                    "`is_case_sensitive` INTEGER NOT NULL, " +
                    "`is_regex` INTEGER NOT NULL, " +
                    "`priority` INTEGER NOT NULL, " +
                    "`match_count` INTEGER NOT NULL, " +
                    "`last_matched` INTEGER NOT NULL, " +
                    "`created_timestamp` INTEGER NOT NULL, " +
                    "`modified_timestamp` INTEGER NOT NULL)");
                
                // Copy data from old table to new table (excluding time-based columns)
                database.execSQL("INSERT INTO sms_filters_new (" +
                    "id, filter_name, filter_type, pattern, action, is_enabled, " +
                    "is_case_sensitive, is_regex, priority, match_count, last_matched, " +
                    "created_timestamp, modified_timestamp) " +
                    "SELECT id, filter_name, filter_type, pattern, action, is_enabled, " +
                    "is_case_sensitive, is_regex, priority, match_count, last_matched, " +
                    "created_timestamp, modified_timestamp FROM sms_filters " +
                    "WHERE filter_type != 'TIME_BASED'"); // Exclude TIME_BASED filters
                
                // Drop old table
                database.execSQL("DROP TABLE sms_filters");
                
                // Rename new table to original name
                database.execSQL("ALTER TABLE sms_filters_new RENAME TO sms_filters");
                
                android.util.Log.i("AppDatabase", "Successfully completed migration from version 5 to 6");
                
            } catch (Exception e) {
                android.util.Log.e("AppDatabase", "Migration 5->6 failed: " + e.getMessage(), e);
                throw e; // Re-throw to trigger fallback
            }
        }
    };
    
    /**
     * Migration from version 6 to 7: Remove SPAM_DETECTION filters
     */
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            try {
                android.util.Log.i("AppDatabase", "Starting migration from version 6 to 7 (removing SPAM_DETECTION filters)");
                
                // Remove all SPAM_DETECTION filters from the database
                database.execSQL("DELETE FROM sms_filters WHERE filter_type = 'SPAM_DETECTION'");
                
                android.util.Log.i("AppDatabase", "Successfully completed migration from version 6 to 7");
                
            } catch (Exception e) {
                android.util.Log.e("AppDatabase", "Migration 6->7 failed: " + e.getMessage(), e);
                throw e; // Re-throw to trigger fallback
            }
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
                    try {
                        INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                        )
                        .allowMainThreadQueries() // For simple operations only
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                        .addCallback(new RoomDatabase.Callback() {
                            @Override
                            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                super.onCreate(db);
                                android.util.Log.i("AppDatabase", "Database created successfully");
                            }
                            
                            @Override
                            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                                super.onOpen(db);
                                android.util.Log.i("AppDatabase", "Database opened successfully");
                                // Migrate SharedPreferences data on first open after migration
                                migrateSharedPreferencesData(context);
                            }
                        })
                        .fallbackToDestructiveMigration() // Fallback if migration fails
                        .build();
                        
                        android.util.Log.i("AppDatabase", "Database instance created successfully");
                        
                    } catch (Exception e) {
                        android.util.Log.e("AppDatabase", "Failed to create database instance: " + e.getMessage(), e);
                        
                        // Try fallback database creation without migrations
                        try {
                            android.util.Log.w("AppDatabase", "Attempting emergency database creation");
                            INSTANCE = Room.databaseBuilder(
                                context.getApplicationContext(),
                                AppDatabase.class,
                                DATABASE_NAME + "_emergency"
                            )
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build();
                            
                            android.util.Log.i("AppDatabase", "Emergency database created successfully");
                            
                        } catch (Exception emergencyException) {
                            android.util.Log.e("AppDatabase", "Emergency database creation failed: " + emergencyException.getMessage(), emergencyException);
                            throw new RuntimeException("Cannot create database", emergencyException);
                        }
                    }
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