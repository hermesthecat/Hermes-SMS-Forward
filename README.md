# Hermes SMS Forward

[![Version](https://img.shields.io/badge/version-2.44.0-blue.svg)](https://github.com/hermesthecat/sms-forward-android)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Android](https://img.shields.io/badge/platform-Android%205.0%2B-brightgreen.svg)](https://android.com)

**Professional SMS forwarding solution for Android with enterprise-grade reliability and privacy protection.**

Automatically forwards incoming SMS messages to multiple configured phone numbers with advanced filtering, dual SIM support, comprehensive notifications, and local analytics - all processed locally on your device.

## Features

- **Multiple Target Numbers**: Forward SMS to unlimited destination phone numbers
- **Advanced Filtering**: Content-based SMS filtering with include/exclude patterns
- **Dual SIM Support**: Full dual SIM compatibility with SIM selection controls
- **Smart Notifications**: Comprehensive notification system with success/error/missed call channels (Android 13+ POST_NOTIFICATIONS support)
- **Custom Templates**: Fully customizable SMS and missed call message formatting with placeholders
- **Message History**: Complete audit trail of all forwarded messages
- **Local Analytics**: Privacy-first usage statistics and insights
- **Multi-language Support**: Complete localization in 6 languages (Turkish, English, German, Italian, French, Spanish)
- **Accessibility**: Full TalkBack and screen reader compatibility
- **Privacy Protection**: Zero external data transmission - all processing local
- **Background Processing**: Reliable WorkManager-based SMS forwarding with retry logic
- **Backup/Restore**: Export and import settings, target numbers, and filter rules

## Installation

### System Requirements

- Android 5.0+ (API Level 21)
- SMS send/receive permissions
- Dual SIM support (optional)

### Setup Process

1. Download APK from [Releases](https://github.com/hermesthecat/sms-forward-android/releases)
2. Enable "Install from Unknown Sources" in Android settings
3. Install the APK file
4. Complete the guided onboarding process (5 steps)
5. Configure target numbers and filtering rules

## Application Components

### Core Activities

- **MainActivity**: Primary interface with status overview and navigation
- **OnboardingActivity**: Guided 5-step setup for new users
- **TargetNumbersActivity**: Multiple destination number management
- **FilterRulesActivity**: SMS filtering rule configuration
- **HistoryActivity**: Complete message forwarding audit trail
- **AnalyticsActivity**: Local usage statistics dashboard
- **SettingsActivity**: Advanced configuration and preferences
- **SimDebugActivity**: Dual SIM diagnostics and debugging interface

### Background Services

- **SmsReceiver**: High-priority broadcast receiver for SMS interception
- **SmsQueueManager**: WorkManager-based reliable message forwarding
- **FilterEngine**: Core message filtering logic with pattern matching

## Architecture

### Technical Stack

- **Database**: Room persistence library with SQLite backend
- **Background Processing**: WorkManager for reliable task execution
- **UI Framework**: Material Design 3 with ViewPager2
- **Threading**: Custom ThreadManager for optimized resource usage
- **Testing**: Comprehensive unit and instrumentation test coverage

### Data Flow

```text
Incoming SMS → SmsReceiver → FilterEngine → SmsQueueManager → Multiple Targets
     ↓              ↓              ↓              ↓              ↓
  Priority 1000   Rule Engine   WorkManager   Background      Retry Logic
  Interception    Processing     Queuing       Execution      & Analytics
```

### Build System

```bash
# Development builds
./gradlew assembleDebug
./gradlew installDebug

# Production builds  
./gradlew assembleRelease
./gradlew bundleRelease

# Quality assurance
./gradlew test
./gradlew connectedAndroidTest
./gradlew lint
```

## Security & Privacy

### Privacy-First Design

- **Local Processing**: All data processing occurs exclusively on device
- **Zero External Transmission**: No cloud storage or external data sharing
- **Encrypted Storage**: Sensitive data secured using Android Keystore
- **Minimal Permissions**: Only essential SMS permissions requested
- **Audit Trail**: Complete local logging for transparency

### Security Features

- **Code Obfuscation**: ProGuard/R8 enabled for release builds
- **Input Validation**: Comprehensive phone number and SMS content validation
- **Thread Safety**: Concurrent operations managed through ThreadManager
- **Error Handling**: Robust exception handling with graceful degradation

## Technical Specifications

### Build Configuration

- **Application ID**: `com.keremgok.sms`
- **Version**: 2.44.0 (Build 65)
- **Min SDK**: 21 (Android 5.0+)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

### Dependencies

- **Room Database**: 2.5.0 - Type-safe SQLite abstraction
- **WorkManager**: 2.8.1 - Reliable background task execution
- **Material Components**: 1.9.0 - Modern UI framework
- **ViewPager2**: 1.0.0 - Fragment-based navigation
- **Testing**: JUnit, Mockito, Robolectric, Espresso

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Support & Development

- **Issue Tracking**: [GitHub Issues](https://github.com/hermesthecat/sms-forward-android/issues)
- **Documentation**: Comprehensive inline documentation and CLAUDE.md
- **Build Scripts**: Windows batch files included for streamlined development

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-functionality`)
3. Implement changes with comprehensive test coverage
4. Ensure all quality checks pass (`./gradlew lint test`)
5. Submit a pull request with detailed description

### Development Standards

- Maintain existing architectural patterns
- Add unit and instrumentation tests for new features
- Follow Material Design 3 guidelines
- Test across multiple Android versions and device configurations

---

**Professional SMS forwarding solution with enterprise-grade reliability and privacy protection.**
