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
    entities = {SmsHistory.class, TargetNumber.class},
    version = 2,
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
                    .addMigrations(MIGRATION_1_2)
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
            int deletedCount = db.smsHistoryDao().deleteOldRecords(thirtyDaysAgo);
            
            if (deletedCount > 0) {
                android.util.Log.i("AppDatabase", "Auto cleanup: Deleted " + deletedCount + " old SMS history records");
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