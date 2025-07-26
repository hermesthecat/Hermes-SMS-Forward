# 📱 Play Store Publishing Guide - Hermes SMS Forward

Complete guide for publishing Hermes SMS Forward to Google Play Store.

## 📋 Table of Contents

1. [Prerequisites](#-prerequisites)
2. [Pre-Publishing Checklist](#-pre-publishing-checklist)
3. [Play Console Setup](#-play-console-setup)
4. [App Bundle Preparation](#-app-bundle-preparation)
5. [Store Listing Configuration](#-store-listing-configuration)
6. [Release Management](#-release-management)
7. [Compliance & Review](#-compliance--review)
8. [Post-Launch Management](#-post-launch-management)

## 🔧 Prerequisites

### Developer Account
- Google Play Console developer account ($25 one-time fee)
- Valid Google account with 2-factor authentication
- Business/individual identity verification completed

### Technical Requirements
- Android Studio with latest SDK tools
- Valid keystore for app signing
- All project dependencies up to date
- Clean build environment

### Legal Documentation
- Privacy Policy (✅ Available: `PRIVACY_POLICY.md`)
- Data Handling Disclosure (✅ Available: `DATA_HANDLING_DISCLOSURE.md`)
- Target Audience definition (✅ Available: `TARGET_AUDIENCE.md`)

## ✅ Pre-Publishing Checklist

### Build Preparation

```bash
# Clean and rebuild
./gradlew clean
./gradlew build

# Run all tests
./gradlew test
./gradlew connectedAndroidTest

# Build release AAB
./gradlew bundleRelease
```

### Version Management
- [ ] Update `versionCode` in `app/build.gradle` (current: 22)
- [ ] Update `versionName` in `app/build.gradle` (current: "2.11.0")
- [ ] Update `changelog.md` with release notes
- [ ] Create git tag: `git tag v2.11.0`

### Signing Configuration
- [ ] Verify `keystore.properties` exists and is properly configured
- [ ] Test release build signing: `./gradlew assembleRelease`
- [ ] Verify AAB is signed: `bundletool verify --bundle=app/build/outputs/bundle/release/app-release.aab`

### App Assets
- [ ] App icon available in all densities (mdpi to xxxhdpi)
- [ ] Adaptive icon configured (`ic_launcher.xml`)
- [ ] Feature graphic (1024x500px)
- [ ] Screenshots (phone, tablet, TV if applicable)
- [ ] High-res icon (512x512px)

## 🏪 Play Console Setup

### 1. Create New App

1. Go to [Google Play Console](https://play.google.com/console)
2. Click "Create app"
3. Fill app details:
   - **App name**: "Hermes SMS Forward"
   - **Default language**: Turkish (Türkçe)
   - **App or game**: App
   - **Free or paid**: Free

### 2. App Category and Content Rating

**Category**: Communication  
**Tags**: SMS, Forwarding, Communication, Business

**Content Rating Questionnaire**:
- Target age group: 18+ (due to SMS permissions)
- No violence, sexual content, or inappropriate material
- Communication app with SMS functionality

### 3. Privacy and Security

```text
Privacy Policy URL: https://github.com/hermesthecat/Hermes-SMS-Forward/blob/main/PRIVACY_POLICY.md

Data Types Collected:
- SMS messages (processed locally, not stored permanently)
- Phone numbers (stored locally only)
- No personal data transmitted to servers
```

## 📦 App Bundle Preparation

### Build Commands

```bash
# Clean previous builds
./gradlew clean

# Build release AAB
./gradlew bundleRelease

# Verify bundle
bundletool build-apks --bundle=app/build/outputs/bundle/release/app-release.aab --output=app.apks

# Test installation
bundletool install-apks --apks=app.apks
```

### Bundle Optimization

Current optimizations in `app/build.gradle`:
```gradle
android {
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }
}
```

### Size Analysis

```bash
# Analyze bundle size
bundletool get-size total --apks=app.apks

# Expected sizes:
# - Total download size: ~2.3MB
# - Install size: ~5.8MB
```

## 📝 Store Listing Configuration

### App Information

**Short Description (80 characters):**
```
Automatically forward incoming SMS to your designated phone number
```

**Full Description:**
Use content from `play-store-description-en.md` (English) and `play-store-description-tr.md` (Turkish)

### Graphics Requirements

#### App Icon
- [x] **High-res icon**: 512x512px PNG (from `ic_launcher_legacy.xml`)
- [x] **Adaptive icon**: Available in project

#### Screenshots
Required screenshots (upload 4-8 images):
1. **Main Interface** (1080x1920px or 1080x2340px)
2. **Onboarding Flow** - 5 step process
3. **Settings Screen**
4. **Target Numbers Management**
5. **SMS History View**
6. **Permission Request Dialog**

#### Feature Graphic
- **Size**: 1024x500px
- **Content**: App logo + "Secure SMS Forwarding" tagline
- **Format**: PNG or JPG

### Localization

#### Turkish (Primary)
- App name: "Hermes SMS Forward"
- Description: From `play-store-description-tr.md`
- Screenshots with Turkish UI

#### English (Secondary)
- App name: "Hermes SMS Forward"
- Description: From `play-store-description-en.md`
- Screenshots with English UI

## 🚀 Release Management

### 1. Internal Testing (Recommended First)

```bash
# Upload AAB to Internal Testing track
# Test with 2-3 internal users
# Verify all functionality works
```

**Internal Testing Group:**
- Developer account holders
- 1-2 beta testers
- Test duration: 1-2 weeks

### 2. Closed Testing (Alpha/Beta)

**Alpha Track (Limited):**
- 10-20 users
- Core functionality testing
- Performance validation

**Beta Track (Broader):**
- 50-100 users
- Real-world usage scenarios
- Feedback collection

### 3. Production Release

#### Staged Rollout Strategy
```
Day 1:    1% of users   (gradual rollout)
Day 3:    5% of users   (monitor crash rates)
Day 7:    20% of users  (performance validation)
Day 14:   50% of users  (broader testing)
Day 21:   100% of users (full release)
```

#### Monitoring Metrics
- **Crash rate**: < 2%
- **ANR rate**: < 1%
- **User ratings**: > 4.0
- **Install success rate**: > 95%

## 📋 Compliance & Review

### SMS Permissions Justification

**RECEIVE_SMS Permission:**
```
This permission is essential for the app's core functionality. 
Hermes SMS Forward intercepts incoming SMS messages to forward 
them to user-designated phone numbers. Without this permission, 
the app cannot perform its primary function.

Use case: Business users forwarding work SMS to personal phones,
travelers receiving SMS on secondary devices, backup solutions.
```

**SEND_SMS Permission:**
```
Required to forward intercepted SMS messages to target phone numbers.
The app sends SMS containing the original message content, sender
information, and timestamp to provide complete message forwarding.

Privacy: All processing is local, no data transmitted to servers.
```

### Policy Compliance

#### Dangerous Permissions
- [x] Clear explanation provided for SMS permissions
- [x] Permissions used only for declared functionality
- [x] No background SMS interception without user consent
- [x] Clear privacy policy regarding SMS handling

#### Content Policy
- [x] App provides legitimate communication functionality
- [x] No spam or unwanted message generation
- [x] User controls SMS forwarding behavior
- [x] Respects user privacy and local laws

### Review Process Timeline

**Expected Timeline:**
- **Initial Review**: 1-3 days
- **Appeals (if rejected)**: 1-2 days
- **Policy Updates**: Additional 1-2 days

**Common Review Issues:**
1. SMS permissions justification
2. Privacy policy clarity
3. Screenshot quality
4. App description accuracy

## 📊 Post-Launch Management

### Monitoring Dashboard

Track these metrics in Play Console:
- **Install metrics**: Downloads, install success rate
- **Performance**: Crashes, ANRs, app start time
- **User feedback**: Ratings, reviews, user engagement
- **Financial**: Revenue (if applicable), conversion rates

### Update Management

#### Regular Updates
```bash
# Version bump process
1. Update versionCode (increment by 1)
2. Update versionName (semantic versioning)
3. Update changelog.md
4. Build and test AAB
5. Upload to appropriate track
6. Staged rollout for major updates
```

#### Hotfix Process
```bash
# Critical bug fixes
1. Create hotfix branch
2. Minimal code changes
3. Emergency version bump
4. Fast-track testing
5. Direct production release (if urgent)
```

### User Communication

#### Release Notes Template
```
Version X.X.X - What's New:

🎯 New Features:
- [Feature description]

🛠️ Improvements:
- [Enhancement description]

🐛 Bug Fixes:
- [Bug fix description]

🔒 Security:
- [Security improvement]
```

## 🔧 Development Workflow

### Pre-Release Process

```bash
# 1. Feature complete & tested
git checkout main
git pull origin main

# 2. Version bump
# Edit app/build.gradle: versionCode, versionName
# Edit changelog.md

# 3. Build and test
./gradlew clean test connectedAndroidTest
./gradlew bundleRelease

# 4. Commit and tag
git add .
git commit -m "Release v2.11.0"
git tag v2.11.0
git push origin main --tags

# 5. Upload to Play Console
# Manual upload of AAB file
```

### Post-Release Process

```bash
# 1. Monitor for 24-48 hours
# Check Play Console for crash reports
# Monitor user reviews and ratings

# 2. Document lessons learned
# Update this guide if needed
# Plan next release cycle

# 3. Prepare for next version
git checkout -b develop/v2.12.0
# Begin next development cycle
```

## 📞 Support and Resources

### Google Play Console Resources
- [Play Console Help Center](https://support.google.com/googleplay/android-developer/)
- [Android App Bundle Guide](https://developer.android.com/guide/app-bundle)
- [Play Store Policy Center](https://play.google.com/about/developer-content-policy/)

### App-Specific Resources
- **Project Repository**: [GitHub](https://github.com/hermesthecat/Hermes-SMS-Forward)
- **Privacy Policy**: `PRIVACY_POLICY.md`
- **User Manual**: `USER_MANUAL.md`
- **Installation Guide**: `INSTALLATION_GUIDE.md`

### Contact Information
- **Developer Email**: support@hermessms.com
- **Play Console Account**: [Developer Account Email]
- **Emergency Contact**: [Backup Email]

## 🎯 Success Metrics

### Launch Success Criteria
- [ ] **App approved** without policy violations
- [ ] **Crash rate** < 2% in first week
- [ ] **Average rating** > 4.0 stars
- [ ] **Download rate** meets expectations
- [ ] **User reviews** are generally positive

### Long-term Goals
- **Organic growth**: 1000+ downloads in first month
- **User retention**: >70% 7-day retention
- **Rating maintenance**: >4.2 average rating
- **Regular updates**: Monthly feature/bug fix releases

---

**Last Updated**: 2025-07-26  
**Version**: 2.11.0  
**Status**: Ready for Play Store Submission

**Note**: This guide should be updated after each Play Store interaction to capture lessons learned and process improvements.