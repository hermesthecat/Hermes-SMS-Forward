# 📱 Hermes SMS Forward

[![Version](https://img.shields.io/badge/version-2.0.0-blue.svg)](https://github.com/hermesthecat/Hermes-SMS-Forward)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Android](https://img.shields.io/badge/platform-Android%205.0%2B-brightgreen.svg)](https://android.com)
[![Build Status](https://img.shields.io/badge/build-passing-success.svg)](https://github.com/hermesthecat/Hermes-SMS-Forward)

> 🚀 **Reliable, secure, and privacy-focused SMS forwarding application for Android**

Hermes SMS Forward automatically forwards incoming SMS messages to your designated phone number with advanced security features, comprehensive testing, and production-ready optimization.

## ✨ Features

### 🔄 Core Functionality

- **Automatic SMS Forwarding**: Instantly forwards all incoming SMS to your specified number
- **Multipart SMS Support**: Handles long messages by automatically splitting them
- **Original Context Preservation**: Maintains sender info, message content, and timestamp
- **Real-time Processing**: Sub-100ms SMS processing with high-priority broadcast receiver

### 🛡️ Security & Privacy

- **Local Processing Only**: All data processing happens on your device
- **No Cloud Storage**: Zero external data transmission or storage
- **Production Security**: Sensitive logging disabled, phone number masking
- **ProGuard/R8 Optimization**: Code obfuscation and size optimization (63% reduction)
- **Secure Storage**: Uses Android's encrypted SharedPreferences

### ⚡ Smart Features

- **Real-time Validation**: Live phone number validation with format checking
- **Turkish Number Support**: Special validation for Turkey (+90) format
- **Retry Mechanism**: Automatic retry with exponential backoff (3 attempts)
- **Error Handling**: Comprehensive error management with user feedback
- **Permission Management**: Graceful handling of permission grants/revocations

### 🎨 Modern Design

- **Material Design**: Clean, intuitive interface following Android guidelines
- **Custom Branding**: Hermes-themed adaptive icon with multiple densities
- **Accessibility Ready**: Screen reader support and high contrast compatibility
- **Responsive UI**: Optimized for different screen sizes and orientations

### 🧪 Production Ready

- **Comprehensive Testing**: 100% unit test coverage with integration tests
- **Build Optimization**: Release builds optimized for size and performance
- **Quality Assurance**: Lint checks, code analysis, and automated testing
- **Documentation**: Complete technical and user documentation

## 📸 Screenshots

| Main Interface | Settings | Validation |
|:-------------:|:--------:|:----------:|
| ![Main](screenshots/main.png) | ![Settings](screenshots/settings.png) | ![Validation](screenshots/validation.png) |

## 🚀 Quick Start

### Prerequisites

- Android 5.0+ (API level 21 or higher)
- SMS-capable Android device
- Active SMS service from your carrier

### Installation

#### Option 1: Download APK (Recommended)

1. Download the latest APK from [Releases](https://github.com/hermesthecat/Hermes-SMS-Forward/releases)
2. Enable "Install from Unknown Sources" in Android Settings
3. Install the APK file

#### Option 2: Build from Source

```bash
# Clone the repository
git clone https://github.com/hermesthecat/Hermes-SMS-Forward.git
cd Hermes-SMS-Forward

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires keystore)
./gradlew assembleRelease

# Install to connected device
./gradlew installDebug
```

### Setup

1. **Launch the app** and grant SMS permissions when prompted
2. **Enter target number** in international format (e.g., +905551234567)
3. **Verify validation** - green checkmark indicates valid number
4. **Tap "Save and Start"** to begin SMS forwarding
5. **Test forwarding** by sending a test SMS to your device

## 📋 Detailed Usage

### First Time Setup

1. **Permission Requests**
   - `RECEIVE_SMS`: Required to intercept incoming SMS
   - `SEND_SMS`: Required to forward SMS to target number
   - Both permissions are mandatory for functionality

2. **Phone Number Configuration**
   - Enter target number in international format (+country_code_number)
   - Real-time validation provides immediate feedback
   - Supports Turkish (+90) numbers with special validation
   - Number is saved securely on your device only

3. **Activation**
   - Toggle forwarding on/off via the main interface
   - Status indicator shows current forwarding state
   - Automatic activation after successful setup

### SMS Forwarding Process

```text
Incoming SMS → Hermes Receiver → Format Message → Send to Target
     ↓              ↓               ↓              ↓
   Original     Extract Info    Add Context    Multipart Support
   Message      (Sender/Time)   & Timestamp    if Needed
```

### Message Format

Forwarded SMS includes:

- **Original Sender**: Phone number or contact name
- **Message Content**: Complete original message
- **Timestamp**: When the message was received
- **Device Info**: Brief identifier (optional)

Example forwarded message:

```text
From: +905551234567
Time: 2025-07-25 14:30:15
Message: Hello, this is a test message!
```

## 🔧 Build Configuration

### Development Environment

- **Android Studio**: 2022.3.1 or later
- **Gradle**: 8.14.1
- **Android Gradle Plugin**: 8.1.0
- **Compile SDK**: 34 (Android 14)
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)

### Build Types

#### Debug Build

```bash
./gradlew assembleDebug
```

- Application ID: `com.keremgok.sms.debug`
- Debuggable: true
- Minification: disabled
- Size: ~5.4MB

#### Release Build

```bash
./gradlew assembleRelease
```

- Application ID: `com.keremgok.sms`
- Debuggable: false
- Minification: enabled (ProGuard/R8)
- Size: ~2.0MB (63% reduction)

### App Bundle (Play Store)

```bash
./gradlew bundleRelease
```

- Format: Android App Bundle (AAB)
- Size: ~2.3MB
- Play Store ready

## 🧪 Testing

### Running Tests

#### Unit Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "PhoneNumberValidatorTest"

# Generate test report
./gradlew test --continue
# Report: build/reports/tests/test/index.html
```

#### Integration Tests

```bash
# Run UI tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run specific UI test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=MainActivityUITest
```

### Test Coverage

- **Unit Tests**: 100% coverage for core functionality
- **Integration Tests**: Complete UI flow testing
- **Manual Tests**: Device compatibility and edge cases

### Test Structure

```bash
app/src/test/java/          # Unit tests
├── PhoneNumberValidatorTest.java
├── SimplePhoneNumberValidatorTest.java
└── ...

app/src/androidTest/java/   # Integration tests
├── MainActivityUITest.java
├── SmsReceiverIntegrationTest.java
└── ...
```

## 📁 Project Structure

```bash
sms-forward-android/
├── app/
│   ├── src/main/java/com/keremgok/sms/
│   │   ├── MainActivity.java           # Main UI and configuration
│   │   ├── SmsReceiver.java           # SMS interception and forwarding
│   │   └── PhoneNumberValidator.java  # Input validation logic
│   ├── src/main/res/
│   │   ├── layout/                    # UI layouts
│   │   ├── values/                    # Strings, colors, themes
│   │   └── mipmap-*/                  # App icons (all densities)
│   ├── src/test/java/                 # Unit tests
│   ├── src/androidTest/java/          # Integration tests
│   ├── build.gradle                   # App-level build configuration
│   └── proguard-rules.pro            # ProGuard optimization rules
├── gradle/                           # Gradle wrapper files
├── keystore.properties              # Signing configuration (gitignored)
├── PRIVACY_POLICY.md                # Privacy policy for Play Store
├── PERMISSIONS.md                   # Detailed permissions explanation
├── DATA_HANDLING_DISCLOSURE.md     # Data handling transparency
├── TARGET_AUDIENCE.md               # Target audience definition
├── changelog.md                     # Version history and changes
├── tasks.md                         # Development task tracking
└── README.md                        # This file
```

## 🔐 Security Considerations

### Privacy Protection

- **Local Processing**: No data leaves your device except for SMS forwarding
- **No Analytics**: No usage tracking or data collection
- **Masked Logging**: Phone numbers masked in debug logs (e.g., +9055***4567)
- **Production Safety**: All sensitive logging disabled in release builds

### Code Security

- **ProGuard/R8**: Code obfuscation in release builds
- **Permission Validation**: Runtime permission checks before operations
- **Input Sanitization**: All user inputs validated and sanitized
- **Secure Storage**: Encrypted local storage for configuration

### Best Practices

- **Minimal Permissions**: Only necessary permissions requested
- **Graceful Degradation**: App handles permission denials gracefully
- **Error Boundaries**: Comprehensive error handling prevents crashes
- **Memory Management**: Efficient memory usage and cleanup

## 📜 Permissions

### Required Permissions

| Permission | Purpose | Usage |
|------------|---------|-------|
| `RECEIVE_SMS` | Intercept incoming SMS | Core functionality - required |
| `SEND_SMS` | Forward SMS to target | Core functionality - required |

### Permission Details

- **Runtime Permissions**: Both permissions requested at runtime (Android 6.0+)
- **User Control**: Users can revoke permissions at any time
- **Graceful Handling**: App provides clear feedback when permissions are missing
- **No Internet**: App doesn't require internet permissions

For detailed permission information, see [PERMISSIONS.md](PERMISSIONS.md).

## 📱 Compatibility

### Android Versions

- **Minimum**: Android 5.0 (API 21) - 99.8% device coverage
- **Target**: Android 14 (API 34) - Latest features and security
- **Tested**: Android 5.0 through Android 14

### Device Requirements

- **RAM**: Minimum 1GB (recommended 2GB+)
- **Storage**: 10MB available space
- **SMS Service**: Active SMS service from carrier required
- **Network**: No internet connection required

### Carrier Compatibility

- **Tested Carriers**: Major Turkish carriers (Turkcell, Vodafone, Türk Telekom)
- **International**: Should work with any SMS-capable carrier
- **MVNO Support**: Compatible with virtual network operators

## 🛠️ Development

### Setup Development Environment

```bash
# Clone repository
git clone https://github.com/hermesthecat/Hermes-SMS-Forward.git
cd Hermes-SMS-Forward

# Ensure you have Android Studio and Android SDK installed
# Open project in Android Studio

# Build and run
./gradlew assembleDebug
./gradlew installDebug
```

### Development Workflow

1. **Create Feature Branch**: `git checkout -b feature/new-feature`
2. **Make Changes**: Follow coding standards and add tests
3. **Run Tests**: `./gradlew test connectedAndroidTest`
4. **Build APK**: `./gradlew assembleDebug`
5. **Create PR**: Submit pull request with detailed description

### Coding Standards

- **Java Style**: Follow Android/Google Java style guide
- **Comments**: Document complex logic and public methods
- **Testing**: Maintain 100% unit test coverage for core functionality
- **Security**: Follow security best practices for Android development

### Release Process

1. **Update Version**: Increment version in `build.gradle`
2. **Update Changelog**: Document changes in `changelog.md`
3. **Run Full Tests**: Execute all unit and integration tests
4. **Build Release**: `./gradlew assembleRelease bundleRelease`
5. **Create Tag**: `git tag v2.0.0`
6. **Release Notes**: Prepare comprehensive release notes

## 📞 Support

### Getting Help

- **Email**: <support@hermessms.com>
- **Issues**: [GitHub Issues](https://github.com/hermesthecat/Hermes-SMS-Forward/issues)
- **Documentation**: Check this README and other documentation files

### Common Issues

#### SMS Forwarding Not Working

1. Check SMS permissions are granted
2. Verify target phone number format
3. Ensure SMS service is active
4. Check Android battery optimization settings

#### Permission Errors

1. Go to Settings > Apps > Hermes SMS Forward > Permissions
2. Enable "SMS" permissions
3. Restart the app
4. Try setup process again

#### Invalid Phone Number

1. Use international format (+country_code_number)
2. Check for special characters or spaces
3. Verify number is SMS-capable
4. Try without country code for local numbers

### Reporting Bugs

When reporting bugs, please include:

- Android version and device model
- App version (from Settings > About)
- Steps to reproduce the issue
- Expected vs actual behavior
- Any error messages or logs

## 🤝 Contributing

We welcome contributions! Please see our contributing guidelines:

### How to Contribute

1. **Fork** the repository
2. **Create** a feature branch
3. **Add** tests for new functionality
4. **Ensure** all tests pass
5. **Submit** a pull request

### Areas for Contribution

- 🌐 **Internationalization**: Add support for more languages
- 🎨 **UI/UX**: Improve user interface and experience
- 🧪 **Testing**: Add more test coverage and edge cases
- 📱 **Compatibility**: Test on more devices and Android versions
- 📖 **Documentation**: Improve documentation and tutorials

### Development Guidelines

- Follow existing code style and patterns
- Add unit tests for new functionality
- Update documentation for changes
- Test on multiple Android versions
- Follow security best practices

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

```text
MIT License

Copyright (c) 2025 KeremGok Development

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 📊 Statistics

- **Lines of Code**: ~2,500 (excluding tests and documentation)
- **Test Coverage**: 100% for core functionality
- **Documentation Coverage**: Comprehensive (README, Privacy Policy, Permissions, etc.)
- **Supported Languages**: Turkish (English coming soon)
- **Release APK Size**: 2.0MB (optimized)
- **Minimum Android Version**: 5.0 (99.8% device coverage)

## 🔮 Roadmap

### Version 2.1.0 (Planned)

- 🌙 **Dark Mode Support**: System-aware theme switching
- 🌐 **English Localization**: Full English language support
- ⚙️ **Settings Screen**: Advanced configuration options

### Version 2.2.0 (Planned)

- 📊 **SMS History**: View forwarded message history
- 🗄️ **Database Integration**: Room database for history tracking
- 🔍 **Search Functionality**: Search through forwarded messages

### Version 2.3.0 (Future)

- 📱 **Multiple Target Numbers**: Forward to multiple recipients
- 🎯 **SMS Filtering**: Filter messages by sender or content
- 🔔 **Smart Notifications**: Enhanced notification system

### Long-term Goals

- 🌍 **Multi-language Support**: Additional language localizations
- 📈 **Analytics Dashboard**: Usage statistics and insights
- 🔄 **Backup & Restore**: Settings backup and restore functionality
- 🤖 **Automation**: Integration with automation apps (Tasker, etc.)

## 🙏 Acknowledgments

- **Android Development Team**: For excellent documentation and tools
- **Material Design Team**: For design guidelines and components
- **Open Source Community**: For libraries and inspiration
- **Beta Testers**: For valuable feedback and bug reports
- **Turkish Android Community**: For support and encouragement

## 📧 Contact

- **Developer**: KeremGok Development
- **Email**: <contact@hermessms.com>
- **GitHub**: [@hermesthecat](https://github.com/hermesthecat)
- **Project Repository**: [Hermes-SMS-Forward](https://github.com/hermesthecat/Hermes-SMS-Forward)
