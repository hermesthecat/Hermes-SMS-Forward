# 📲 Installation Guide - Hermes SMS Forward

Complete step-by-step installation and setup guide for Hermes SMS Forward.

## 📋 Table of Contents

1. [System Requirements](#-system-requirements)
2. [Installation Methods](#-installation-methods)
3. [First-Time Setup](#-first-time-setup)
4. [Troubleshooting](#-troubleshooting)
5. [Advanced Installation](#-advanced-installation)

## 🔧 System Requirements

### Minimum Requirements

- **Android Version**: 5.0+ (API level 21)
- **RAM**: 1GB or more
- **Storage**: 10MB available space
- **SMS Service**: Active SMS service from carrier
- **Device Type**: Smartphone with SMS capabilities

### Recommended Specifications

- **Android Version**: 8.0+ for optimal experience
- **RAM**: 2GB or more
- **Storage**: 50MB available space (for logs and cache)
- **Network**: No internet required (all processing local)

### Compatibility Check

Before installation, verify your device compatibility:

1. **Check Android Version**:
   - Go to Settings > About Phone > Android Version
   - Ensure version is 5.0 or higher

2. **Check Available Storage**:
   - Go to Settings > Storage
   - Ensure at least 10MB free space

3. **Verify SMS Functionality**:
   - Send and receive a test SMS
   - Confirm SMS app is working properly

## 🚀 Installation Methods

### Method 1: APK Download (Recommended)

#### Step 1: Download APK

1. Visit the [GitHub Releases page](https://github.com/hermesthecat/Hermes-SMS-Forward/releases)
2. Download the latest `app-release.apk` file
3. Note the file location (usually Downloads folder)

#### Step 2: Enable Unknown Sources

For Android 8.0+:

1. Go to **Settings** > **Apps & notifications**
2. Tap **Advanced** > **Special app access**
3. Tap **Install unknown apps**
4. Select your browser or file manager
5. Toggle **Allow from this source**

For Android 7.1 and below:

1. Go to **Settings** > **Security**
2. Enable **Unknown sources**
3. Confirm by tapping **OK**

#### Step 3: Install APK

1. Open your file manager or browser downloads
2. Locate the downloaded `app-release.apv` file
3. Tap the file to begin installation
4. Tap **Install** when prompted
5. Wait for installation to complete
6. Tap **Open** to launch the app

### Method 2: Build from Source

#### Prerequisites

- Android Studio 2022.3.1 or later
- Android SDK with API 21+ and API 34
- Git for cloning the repository
- Java 8+ installed

#### Step 1: Clone Repository

```bash
git clone https://github.com/hermesthecat/Hermes-SMS-Forward.git
cd Hermes-SMS-Forward
```

#### Step 2: Open in Android Studio

1. Launch Android Studio
2. Select **Open an existing project**
3. Navigate to the cloned repository folder
4. Click **OK** to open the project
5. Wait for Gradle sync to complete

#### Step 3: Build APK

```bash
# For debug version (testing)
./gradlew assembleDebug

# For release version (production)
./gradlew assembleRelease
```

#### Step 4: Install to Device

```bash
# Connect your device via USB with USB debugging enabled
./gradlew installDebug

# Or manually install the APK from app/build/outputs/apk/
```

### Method 3: Google Play Store (Coming Soon)

The app will be available on Google Play Store after review process completion.

## 🎯 First-Time Setup

### Step 1: Launch Application

1. Locate **Hermes SMS Forward** in your app drawer
2. Tap the app icon to launch
3. Wait for the main interface to load

### Step 2: Grant Permissions

The app will request two critical permissions:

#### SMS Permissions Dialog

1. **"Allow Hermes SMS Forward to send and view SMS messages?"**
2. Tap **Allow** to grant both permissions
3. If you tap **Deny**, the app will explain why permissions are needed

#### Manual Permission Grant (if needed)

1. Go to **Settings** > **Apps** > **Hermes SMS Forward**
2. Tap **Permissions**
3. Enable **SMS** permission
4. Return to the app and try again

### Step 3: Configure Target Number

#### Enter Phone Number

1. In the **Target Phone Number** field, enter the number where SMS should be forwarded
2. Use international format: `+90555123456` (Turkey example)
3. Or local format: `05551234567` (if in Turkey)

#### Validation Feedback

- **Green checkmark**: Number is valid and properly formatted
- **Red X**: Number format is invalid
- **Orange warning**: Number format is questionable but might work

#### Supported Formats

- **International**: `+90555123456`, `+1555123456`
- **Turkey Local**: `05551234567`, `555 123 45 67`
- **Other Countries**: Follow local conventions with country code

### Step 4: Activate Forwarding

1. Verify your target number shows a green checkmark
2. Tap **"Save and Start"** button
3. Confirmation message will appear
4. The app is now ready to forward SMS

### Step 5: Test Functionality

1. Send a test SMS to your device from another phone
2. Check if the SMS is forwarded to your target number
3. Verify the forwarded message includes original sender info

## 🔍 Troubleshooting

### Common Installation Issues

#### Issue: "App not installed"

**Cause**: Insufficient storage or corrupted APK
**Solution**:

1. Free up at least 50MB storage space
2. Re-download the APK file
3. Clear cache: Settings > Storage > Cached data > Clear

#### Issue: "Installation blocked"

**Cause**: Security settings preventing installation
**Solution**:

1. Enable "Unknown sources" in security settings
2. Temporarily disable antivirus software
3. Use a different file manager to install

#### Issue: "Parse error"

**Cause**: Incompatible Android version or corrupted file
**Solution**:

1. Verify Android version is 5.0+
2. Re-download APK from official source
3. Check available storage space

### Common Setup Issues

#### Issue: "Permission denied"

**Cause**: SMS permissions not granted properly
**Solution**:

1. Go to Settings > Apps > Hermes SMS Forward > Permissions
2. Ensure SMS permission is enabled
3. Restart the app completely
4. Try setup process again

#### Issue: "Invalid phone number"

**Cause**: Incorrect number format
**Solution**:

1. Use international format (+country_code_number)
2. Remove spaces, dashes, or special characters
3. Verify the number can receive SMS
4. Try with country code even for local numbers

#### Issue: "SMS not forwarding"

**Cause**: Multiple possible causes
**Solution**:

1. **Check permissions**: Ensure both RECEIVE_SMS and SEND_SMS are enabled
2. **Verify target number**: Send a test SMS to target number manually
3. **Check carrier settings**: Ensure SMS service is active
4. **Restart device**: Sometimes a restart resolves SMS issues
5. **Battery optimization**: Disable battery optimization for the app

### Battery Optimization Issues

#### Samsung Devices

1. Go to **Settings** > **Device care** > **Battery**
2. Tap **App power management**
3. Find **Hermes SMS Forward**
4. Disable **Put app to sleep**

#### Huawei Devices

1. Go to **Settings** > **Battery** > **App launch**
2. Find **Hermes SMS Forward**
3. Turn off **Manage automatically**
4. Enable **Auto-launch**, **Secondary launch**, and **Run in background**

#### General Android

1. Go to **Settings** > **Battery** > **Battery optimization**
2. Select **All apps** from dropdown
3. Find **Hermes SMS Forward**
4. Select **Don't optimize**

## 🔧 Advanced Installation

### Enterprise/Bulk Installation

#### Using ADB (Android Debug Bridge)

```bash
# Install APK via ADB
adb install app-release.apk

# Install to specific device
adb -s DEVICE_ID install app-release.apk

# Force install (overwrite existing)
adb install -r app-release.apk
```

#### MDM (Mobile Device Management)

For enterprise deployments:

1. Upload APK to your MDM solution
2. Configure app permissions policy
3. Deploy to target devices
4. Use silent configuration if supported

### Custom Keystore Installation

For organizations wanting to sign with their own keystore:

```bash
# Generate your keystore
keytool -genkey -v -keystore my-release-key.keystore -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000

# Update keystore.properties with your details
# Build with your keystore
./gradlew assembleRelease
```

### Debug Installation for Developers

```bash
# Install debug version with different package name
./gradlew installDebug

# This allows running both debug and release versions simultaneously
```

## 🗑️ Uninstallation

### Method 1: Standard Uninstall

1. Go to **Settings** > **Apps** (or **Application Manager**)
2. Find **Hermes SMS Forward**
3. Tap the app name
4. Tap **Uninstall**
5. Confirm by tapping **OK**

### Method 2: From Home Screen

1. Find the app icon on your home screen or app drawer
2. Long-press the app icon
3. Drag to **Uninstall** or tap the uninstall option
4. Confirm uninstallation

### Method 3: Using ADB

```bash
# Uninstall via ADB
adb uninstall com.keremgok.sms

# For debug version
adb uninstall com.keremgok.sms.debug
```

### Data Cleanup

After uninstallation:

- All app data is automatically removed
- SMS forwarding stops immediately
- No residual data remains on the device
- App permissions are automatically revoked

## 🔄 Updating the App

### Update Process

1. **Download** the latest APK from GitHub releases
2. **Install** over the existing app (no need to uninstall)
3. **Grant permissions** if prompted (usually not needed)
4. **Verify settings** are preserved from previous version

### Version Compatibility

- Settings are preserved across updates
- Database migrations handled automatically
- No data loss during updates
- Rollback to previous version possible

## 📞 Installation Support

### Getting Help

If you encounter issues during installation:

- **Email**: <support@hermessms.com>
- **GitHub Issues**: [Report Installation Problems](https://github.com/hermesthecat/Hermes-SMS-Forward/issues)
- **Documentation**: Check README.md for additional information

### Information to Include in Support Requests

- Android version and device model
- Installation method used
- Exact error message (if any)
- Steps taken before the error occurred
- Screenshots of error messages (if applicable)

## ✅ Installation Checklist

Before contacting support, verify:

- [ ] Android version is 5.0 or higher
- [ ] At least 10MB storage available
- [ ] Unknown sources enabled (for APK installation)
- [ ] SMS service is working on device
- [ ] Downloaded APK from official GitHub releases
- [ ] All permissions granted to the app
- [ ] Target phone number in correct format
- [ ] Battery optimization disabled for the app

---

**Note**: This installation guide is updated regularly. For the latest version, visit the [GitHub repository](https://github.com/hermesthecat/Hermes-SMS-Forward).

**Support**: If you encounter any issues not covered in this guide, please open an issue on GitHub or contact our support team.
