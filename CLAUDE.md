# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Development Environment

- OS: Windows 10.0.26100
- Shell: Git Bash
- Path format: Windows (use forward slashes in Git Bash)
- File system: Case-insensitive
- Line endings: CRLF (configure Git autocrlf)

## Project Overview

This is a comprehensive Android SMS forwarding application that automatically forwards incoming SMS messages to multiple configured target phone numbers. The app features a sophisticated architecture with Room database, WorkManager for reliable background processing, privacy-first local analytics, advanced filtering rules, comprehensive logging, backup/restore functionality, and customizable SMS formatting. It uses a BroadcastReceiver to intercept SMS messages and forwards them using Android's SmsManager with retry logic and queue management.

## Development Process

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

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Build App Bundle for Play Store
./gradlew bundleRelease

# Install debug APK to connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "PhoneNumberValidatorTest"

# Run instrumentation tests
./gradlew connectedAndroidTest

# Clean build artifacts
./gradlew clean

# Clean and build (full rebuild)
./gradlew clean assembleDebug

# Check for dependency updates
./gradlew dependencyUpdates

# Lint check
./gradlew lint

# Generate lint report
./gradlew lint --continue
```

### Windows Batch Scripts (Alternative)

```bat
# Clean and build debug APK
clean-build.bat

# Create signed release APK with automatic versioning
create-signed-apk.bat
```

**Note**: `create-signed-apk.bat` automatically versions output files (e.g., `sms-forward-v2.35.0-signed-YYYYMMDD-1.apk`) and archives them in the `apk_archive/` directory for release management.

### APK Output Locations

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`
- **App Bundle**: `app/build/outputs/bundle/release/app-release.aab`

## Architecture

The application follows a **layered, component-based architecture** with clear separation of concerns:

- **UI Layer**: Activities and Fragments with Material Design 3 components
- **Business Logic Layer**: FilterEngine, SmsQueueManager, and various Manager classes
- **Data Layer**: Room database with DAOs providing abstraction over SQLite
- **Background Processing Layer**: WorkManager for reliable task execution
- **System Integration Layer**: BroadcastReceiver for SMS interception

### SMS Processing Pipeline

1. **SMS_RECEIVED** broadcast intercepted by `SmsReceiver` (priority 1000)
2. **FilterEngine** evaluates message against user-defined rules from database
3. **SmsQueueManager** queues qualifying messages for background processing
4. **SmsQueueWorker** (WorkManager) handles actual forwarding with retry logic
5. **SmsSimSelectionHelper** manages dual-SIM routing with configurable selection modes
6. **SmsFormatter** applies user-selected formatting (standard/compact/detailed/custom template)
7. **SmsCallbackReceiver** handles SMS_SENT/SMS_DELIVERED status updates
8. **StatisticsManager** logs events locally for privacy-first analytics

## Core Components

- **MainActivity** (`app/src/main/java/com/keremgok/sms/MainActivity.java`): Main UI for configuration, handles permission requests and navigation
- **SmsReceiver** (`app/src/main/java/com/keremgok/sms/SmsReceiver.java`): BroadcastReceiver that intercepts incoming SMS and forwards them
- **AppDatabase** (`app/src/main/java/com/keremgok/sms/AppDatabase.java`): Room database managing all persistent data
- **ThreadManager** (`app/src/main/java/com/keremgok/sms/ThreadManager.java`): Centralized thread pool management with purpose-specific executors (database, network, background) to prevent blocking
- **SmsQueueManager** & **SmsQueueWorker**: WorkManager-based reliable background SMS forwarding with retry logic
- **StatisticsManager**: Privacy-first local analytics system with no external data transmission
- **FilterEngine** (`app/src/main/java/com/keremgok/sms/FilterEngine.java`): Core message filtering logic with include/exclude patterns
- **SimManager** (`app/src/main/java/com/keremgok/sms/SimManager.java`): Dual SIM support and SIM card management
- **LanguageManager** (`app/src/main/java/com/keremgok/sms/LanguageManager.java`): Runtime language switching and localization
- **BackupManager** (`app/src/main/java/com/keremgok/sms/BackupManager.java`): Backup and restore system for settings, target numbers, and filter rules to JSON format
- **SmsFormatter** (`app/src/main/java/com/keremgok/sms/SmsFormatter.java`): Customizable SMS formatting with templates and placeholders
- **SmsCallbackReceiver**: Handles SMS delivery status callbacks (SMS_SENT/SMS_DELIVERED) for message status tracking

## Feature Activities

- **OnboardingActivity**: ViewPager2-based guided setup flow for new users (5-step process)
- **TargetNumbersActivity**: Manages multiple destination phone numbers
- **FilterRulesActivity**: Manages SMS filtering rules (include/exclude patterns)
- **HistoryActivity**: Displays comprehensive log of forwarded messages
- **SettingsActivity**: Advanced configuration options
- **AnalyticsActivity**: Local usage statistics dashboard
- **SimDebugActivity**: Dual SIM debugging and diagnostics interface (debug builds only via `@bool/is_debug_build`)

## Database Entities (Room)

- **SmsHistory**: Logs of all forwarded messages
- **TargetNumber**: Multiple destination phone numbers
- **SmsFilter**: Rules for message filtering
- **AnalyticsEvent** & **StatisticsSummary**: Local analytics data

## Onboarding System Architecture

- **OnboardingActivity**: Manages ViewPager2 with 5 fragments
- **Fragment Flow**: Welcome → Permissions → Target Setup → Filter Intro → Completion
- **State Management**: SharedPreferences tracks onboarding completion (`onboarding_completed`)
- **Navigation Controls**: Progress indicator, skip/back/next buttons with adaptive UI
- **Auto-Launch**: MainActivity checks completion status and redirects new users

## Data Flow

1. **First Launch**: OnboardingActivity detects new users and guides through 5-step setup
2. User configures multiple target numbers and filtering rules
3. SmsReceiver intercepts incoming SMS via SMS_RECEIVED broadcast (priority 1000)
4. Messages are filtered based on configured rules
5. Qualifying messages are queued for forwarding via SmsQueueManager
6. WorkManager ensures reliable delivery with retry logic
7. All events are logged locally for analytics and history

## Key Configuration

- **App ID**: `com.keremgok.sms`
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)
- **Current Version**: 2.35.1 (versionCode 47) - Update these values when releasing new versions
- **Permissions**: RECEIVE_SMS, SEND_SMS (both require runtime permission requests)
- **Build Types**: Debug (with `.debug` suffix) and Release (with ProGuard/R8 obfuscation)
- **Threading**: Use ThreadManager for all background operations instead of creating raw threads
- **Kotlin Version**: Forced to 1.8.10 in build.gradle to resolve dependency conflicts
- **File Sharing**: Uses FileProvider for secure analytics file exports

## Testing

Comprehensive testing framework with **two-pronged strategy**:

### Unit Tests (`app/src/test/`)

- **Frameworks**: JUnit, Mockito, Robolectric, Truth
- **Focus**: Testing individual components in isolation
- **Key Test**: `SimplePhoneNumberValidatorTest.java` for validation logic
- **Command**: `./gradlew test --tests "PhoneNumberValidatorTest"`

### Instrumentation Tests (`app/src/androidTest/`)

- **Frameworks**: AndroidX Test (JUnit4), Espresso
- **Focus**: UI interactions and component integrations on real device/emulator
- **Key Tests**:
  - `MainActivityTest.java`: Main screen UI and behavior
  - `PermissionFlowTest.java`: Permission declarations and handling
  - `SMSForwardIntegrationTest.java`: End-to-end flow testing
- **Command**: `./gradlew connectedAndroidTest`

## Development Patterns

### Essential Practices

- **Database Access**: Always use Room DAOs instead of direct database operations
- **Background Work**: Use ThreadManager's executor services (database, network, background) instead of creating threads
- **Privacy-First**: All analytics and data processing must remain local - no external data transmission
- **Input Validation**: Use PhoneNumberValidator for all phone number inputs
- **Error Handling**: Leverage WorkManager's retry logic for reliable SMS forwarding
- **First-Time Users**: OnboardingActivity automatically launches for new users; use `OnboardingActivity.isOnboardingCompleted()` to check status
- **Fragment-Based UI**: Onboarding uses ViewPager2 with fragments; follow established patterns for new multi-step flows
- **Dual SIM Support**: Use SimManager for SIM card detection and selection; SmsSimSelectionHelper for SMS sending with selection modes (auto/source SIM/specific SIM)
- **SMS Formatting**: Use SmsFormatter for customizable message templates; supports standard/compact/detailed formats plus custom templates with placeholders
- **Backup/Restore**: Use BackupManager for data portability; exports/imports settings, target numbers, and filters as JSON
- **Internationalization**: Use LanguageManager for runtime language switching; support Turkish and English
- **Debug Components**: Use `@bool/is_debug_build` pattern for conditional debug-only activities and features
- **Secure File Access**: Use FileProvider for sharing exported data files; avoid direct file URIs

### Modern Android Patterns Used

- **Room Database**: Type-safe database access with automatic SQLite generation
- **WorkManager**: Deferrable, guaranteed background work execution
- **Material Design 3**: Latest design system with adaptive theming
- **ViewPager2**: Modern fragment-based UI flows with better performance
- **BroadcastReceiver**: System-level SMS interception with high priority
- **Singleton Pattern**: StatisticsManager and ThreadManager for centralized resource management

## Key Dependencies

- **Room**: `androidx.room` for database operations and type-safe SQL generation
- **WorkManager**: `androidx.work` for background task reliability and retry logic
- **Material Design**: `com.google.android.material` for UI components and theming
- **ViewPager2**: `androidx.viewpager2` for onboarding flow and fragment navigation
- **Preferences**: `androidx.preference` for settings management with PreferenceFragmentCompat
- **Testing**: JUnit, Mockito, Robolectric (unit) + AndroidX Test, Espresso (instrumentation)

## Important Files

- `app/build.gradle`: Build configuration with release obfuscation settings
- `proguard-rules.pro`: Code obfuscation rules for release builds
- `keystore.properties`: Release signing configuration (gitignored)
- `app/src/main/AndroidManifest.xml`: Component declarations and permissions
- `app/src/main/res/xml/preferences.xml`: Settings screen configuration
- `changelog.md`: Version history and release notes
- `clean-build.bat` / `create-signed-apk.bat`: Windows build scripts for easier development

## Code Quality

```bash
# Run all quality checks
./gradlew lint test

# Generate comprehensive reports
./gradlew lint test --continue

# Check ProGuard/R8 rules
./gradlew assembleRelease --info
```

Reports are generated in:

- **Lint Report**: `app/build/reports/lint-results.html`
- **Test Report**: `app/build/reports/tests/test/index.html`
