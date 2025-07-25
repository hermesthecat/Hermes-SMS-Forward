package com.keremgok.sms;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Main Room Database class for Hermes SMS Forward
 * Contains SMS history tracking and provides singleton access
 */
@Database(
    entities = {SmsHistory.class},
    version = 1,
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
                    .fallbackToDestructiveMigration() // For development phase
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
     * Should be called periodically in background
     * @param context Application context
     */
    public static void performAutoCleanup(Context context) {
        new Thread(() -> {
            AppDatabase db = getInstance(context);
            long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000); // 30 days in milliseconds
            int deletedCount = db.smsHistoryDao().deleteOldRecords(thirtyDaysAgo);
            
            if (deletedCount > 0) {
                android.util.Log.i("AppDatabase", "Auto cleanup: Deleted " + deletedCount + " old SMS history records");
            }
        }).start();
    }
}