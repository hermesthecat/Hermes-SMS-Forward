# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Development Environment

- OS: Windows 10.0.26100
- Shell: Git Bash
- Path format: Windows (use forward slashes in Git Bash)
- File system: Case-insensitive
- Line endings: CRLF (configure Git autocrlf)

# Project Overview

This is an Android SMS forwarding application that automatically forwards incoming SMS messages to a configured target phone number. The app uses a BroadcastReceiver to intercept SMS messages and forwards them using Android's SmsManager.

# Development Process

- Git version control system is used
- Complete one task at a time
- Never move to the next task without user saying "okay" or "continue"

- After each task completion:
  1. Build APK if APK build fail, fix error and warning
  2. Each completed task gets a green tick (✅)
  3. Update version (pubspec.yaml)
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

- **MainActivity** (`app/src/main/java/com/keremgok/sms/MainActivity.java`): Main UI for configuration, handles permission requests and target number storage
- **SmsReceiver** (`app/src/main/java/com/keremgok/sms/SmsReceiver.java`): BroadcastReceiver that intercepts incoming SMS and forwards them
- **AndroidManifest.xml**: Declares components and required SMS permissions (RECEIVE_SMS, SEND_SMS)

## Data Flow

1. User configures target phone number in MainActivity
2. MainActivity requests SMS permissions and saves target to SharedPreferences
3. SmsReceiver intercepts incoming SMS via SMS_RECEIVED broadcast (priority 1000)
4. SmsReceiver reads target from SharedPreferences and forwards SMS using SmsManager
5. Long messages are automatically split into multipart SMS

## Key Configuration

- **App ID**: `com.keremgok.sms`
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)
- **Current Version**: 1.3.0 (versionCode 4)
- **Permissions**: RECEIVE_SMS, SEND_SMS (both require runtime permission requests)

# Testing

The project includes test dependencies but no test files are currently implemented:

- Unit tests: `junit:junit:4.13.2`
- Instrumentation tests: `androidx.test.ext:junit` and `androidx.test.espresso:espresso-core`

# Important Files

- `app/build.gradle`: App-level build configuration with dependencies
- `app/src/main/AndroidManifest.xml`: Component declarations and permissions
- `app/src/main/res/layout/activity_main.xml`: Main UI layout
- `app/src/main/res/values/strings.xml`: String resources
