# 📱 Hermes SMS Forward

[![Version](https://img.shields.io/badge/version-2.12.0-blue.svg)](https://github.com/hermesthecat/Hermes-SMS-Forward)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Android](https://img.shields.io/badge/platform-Android%205.0%2B-brightgreen.svg)](https://android.com)

> **Secure and privacy-focused SMS forwarding application for Android**

Automatically forwards incoming SMS messages to your configured phone numbers with advanced features and complete privacy protection.

## ✨ Features

- 📱 **Multiple Target Numbers**: Forward SMS to unlimited destination numbers
- 🔍 **Smart Filtering**: Content-based SMS filtering system with include/exclude rules
- 📊 **Message History**: Complete log of all forwarded messages with analytics
- 🌐 **Multi-language**: Full Turkish and English language support
- ♿ **Accessibility**: Complete TalkBack and screen reader support
- 🔐 **Privacy First**: All data stays on your device, no cloud storage

## 🚀 Quick Start

### Requirements
- Android 5.0+ (API 21)
- SMS send/receive permissions

### Installation
1. Download APK from [Releases](https://github.com/hermesthecat/Hermes-SMS-Forward/releases)
2. Enable "Install from Unknown Sources" in Android settings
3. Install the APK file
4. Open the app and follow the 5-step guided setup

## 📱 Usage

### First Time Setup
1. **Welcome**: Learn about app features and benefits
2. **Permissions**: Grant SMS permissions with detailed explanations
3. **Target Number**: Add your first forwarding destination
4. **Filters**: Introduction to the smart filtering system
5. **Completion**: Finish setup and start forwarding

### Main Features
- **Main Screen**: Quick status check and basic settings
- **Target Numbers**: Manage multiple forwarding destinations
- **Filter Rules**: Configure which messages to forward
- **History**: View all forwarded messages with detailed logs
- **Analytics**: Local usage statistics and insights
- **Settings**: Advanced configuration options

## 🔧 Development

### Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Install to device
./gradlew installDebug

# Clean and rebuild
./gradlew clean assembleDebug
```

### Project Structure
```
app/src/main/java/com/keremgok/sms/
├── MainActivity.java              # Main screen and configuration
├── OnboardingActivity.java        # 5-step guided setup
├── SmsReceiver.java              # SMS interception and forwarding
├── TargetNumbersActivity.java    # Multiple target number management
├── FilterRulesActivity.java      # SMS filtering rules
├── HistoryActivity.java          # Message history and logs
├── AnalyticsActivity.java        # Local usage statistics
├── SettingsActivity.java         # Advanced settings
├── AppDatabase.java              # Room database configuration
├── ThreadManager.java            # Centralized thread management
└── SmsQueueManager.java          # WorkManager-based SMS queue
```

## 🔐 Security & Privacy

- **Local Processing**: All data processing happens on your device
- **No Cloud Storage**: Zero external data transmission
- **Encrypted Storage**: Sensitive data encrypted using Android secure storage
- **Minimal Permissions**: Only necessary SMS permissions requested
- **Open Source**: Fully transparent codebase

## ⚡ How It Works

```
Incoming SMS → Filter Engine → Queue Manager → Multiple Targets
     ↓              ↓              ↓              ↓
   Original     Apply Rules    WorkManager    Reliable Delivery
   Message      (Include/      Background     with Retry Logic
               Exclude)       Processing
```

## 📊 Statistics

- **Lines of Code**: 4,500+ lines (excluding tests)
- **Test Coverage**: 100% for core functionality
- **Supported Languages**: Turkish, English
- **APK Size**: ~2MB (optimized with ProGuard/R8)
- **Minimum Android**: 5.0 (99.8% device coverage)
- **Architecture**: Room + WorkManager + Material Design 3

## 🔮 Version History

### 2.12.0 (Current)
- 🔧 Complete Room database integration
- 📱 Multiple target number support
- 🎯 Advanced filtering system with include/exclude patterns
- 📊 Local analytics and comprehensive history tracking
- 🌐 Full Turkish/English internationalization
- ♿ Complete accessibility support with TalkBack
- 🛠️ Reliable WorkManager-based background processing
- 📈 Performance monitoring and optimization

### 2.11.0
- 🎯 5-step guided onboarding flow with ViewPager2
- 📱 Modern Material Design 3 UI components
- 🎨 Interactive setup with visual guides
- 🛡️ Detailed permission explanations

## 🛠️ Technical Details

### Architecture Components
- **Room Database**: Local data persistence
- **WorkManager**: Reliable background SMS processing
- **ThreadManager**: Centralized thread pool management
- **Material Design 3**: Modern UI components
- **BroadcastReceiver**: SMS interception (priority 1000)

### Build Configuration
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- **ProGuard/R8**: Enabled for release builds (63% size reduction)

## 📄 License

MIT License - see [LICENSE](LICENSE) file for details.

## 📞 Support

- **Issues**: [Report bugs](https://github.com/hermesthecat/Hermes-SMS-Forward/issues)
- **Documentation**: Check this README and other .md files

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

### Development Guidelines
- Follow existing code style and patterns
- Add unit tests for new features
- Update documentation
- Test on multiple Android versions

---

**⚡ Fast, secure, privacy-focused SMS forwarding**