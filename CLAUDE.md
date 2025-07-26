# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Development Environment

- OS: Windows 10.0.26100
- Shell: Git Bash
- Path format: Windows (use forward slashes in Git Bash)
- File system: Case-insensitive
- Line endings: CRLF (configure Git autocrlf)

# Project Overview

This is a comprehensive Android SMS forwarding application that automatically forwards incoming SMS messages to multiple configured target phone numbers. The app features a sophisticated architecture with Room database, WorkManager for reliable background processing, privacy-first local analytics, advanced filtering rules, and comprehensive logging. It uses a BroadcastReceiver to intercept SMS messages and forwards them using Android's SmsManager with retry logic and queue management.

# Development Process

- Git version control system is used
- Complete one task at a time
- Never move to the next task without user saying "okay" or "continue"

- After each task completion:
  1. Build APK - if APK build fail, fix errors and warnings
  2. Each completed task gets a green tick (✅) (AFTER APK build succeeds)
  3. Update version in app/build.gradle (versionCode and versionName)
  4. Write changes to changelog.md file
  5. Git commit
  6. Git push

# Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK  
./gradlew assembleRelease

# Install debug APK to connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedAndroidTest

# Clean build artifacts
./gradlew clean

# Check for dependency updates
./gradlew dependencyUpdates
```

# Architecture

## Core Components

- **MainActivity** (`app/src/main/java/com/keremgok/sms/MainActivity.java`): Main UI for configuration, handles permission requests and navigation
- **SmsReceiver** (`app/src/main/java/com/keremgok/sms/SmsReceiver.java`): BroadcastReceiver that intercepts incoming SMS and forwards them
- **AppDatabase** (`app/src/main/java/com/keremgok/sms/AppDatabase.java`): Room database managing all persistent data
- **ThreadManager** (`app/src/main/java/com/keremgok/sms/ThreadManager.java`): Centralized thread pool management for database, network, and background tasks
- **SmsQueueManager** & **SmsQueueWorker**: WorkManager-based reliable background SMS forwarding with retry logic
- **StatisticsManager**: Privacy-first local analytics system with no external data transmission

## Feature Activities

- **TargetNumbersActivity**: Manages multiple destination phone numbers
- **FilterRulesActivity**: Manages SMS filtering rules (include/exclude patterns)
- **HistoryActivity**: Displays comprehensive log of forwarded messages
- **SettingsActivity**: Advanced configuration options
- **AnalyticsActivity**: Local usage statistics dashboard

## Database Entities (Room)

- **SmsHistory**: Logs of all forwarded messages
- **TargetNumber**: Multiple destination phone numbers
- **SmsFilter**: Rules for message filtering
- **AnalyticsEvent** & **StatisticsSummary**: Local analytics data

## Data Flow

1. User configures multiple target numbers and filtering rules
2. SmsReceiver intercepts incoming SMS via SMS_RECEIVED broadcast (priority 1000)
3. Messages are filtered based on configured rules
4. Qualifying messages are queued for forwarding via SmsQueueManager
5. WorkManager ensures reliable delivery with retry logic
6. All events are logged locally for analytics and history

## Key Configuration

- **App ID**: `com.keremgok.sms`
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)
- **Current Version**: 2.3.0 (versionCode 14)
- **Permissions**: RECEIVE_SMS, SEND_SMS (both require runtime permission requests)
- **Build Types**: Debug (with `.debug` suffix) and Release (with ProGuard/R8 obfuscation)
- **Threading**: Use ThreadManager for all background operations instead of creating raw threads

# Testing

Comprehensive testing framework configured with:

- **Unit tests**: `junit`, `mockito`, `robolectric` for isolated component testing
- **Integration tests**: `androidx.test.ext:junit`, `espresso-core`, and `truth` for UI and database testing
- **Test directories**: `app/src/test/` (unit) and `app/src/androidTest/` (instrumentation)

# Development Patterns

## Essential Practices

- **Database Access**: Always use Room DAOs instead of direct database operations
- **Background Work**: Use ThreadManager's executor services (database, network, background) instead of creating threads
- **Privacy-First**: All analytics and data processing must remain local - no external data transmission
- **Input Validation**: Use PhoneNumberValidator for all phone number inputs
- **Error Handling**: Leverage WorkManager's retry logic for reliable SMS forwarding

## Key Dependencies

- **Room**: `androidx.room` for database operations
- **WorkManager**: `androidx.work` for background task reliability
- **Material Design**: `com.google.android.material` for UI components
- **Preferences**: `androidx.preference` for settings management

# Important Files

- `app/build.gradle`: Build configuration with release obfuscation settings
- `proguard-rules.pro`: Code obfuscation rules for release builds
- `keystore.properties`: Release signing configuration
- `app/src/main/AndroidManifest.xml`: Component declarations and permissions
- `app/src/main/res/xml/preferences.xml`: Settings screen configuration
