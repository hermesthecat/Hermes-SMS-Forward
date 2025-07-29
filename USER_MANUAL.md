# User Manual - Hermes SMS Forward

Complete user guide for using Hermes SMS Forward effectively and safely.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Main Interface](#main-interface)
3. [Setup and Configuration](#setup-and-configuration)
4. [Daily Usage](#daily-usage)
5. [Advanced Features](#advanced-features)
6. [Troubleshooting](#troubleshooting)
7. [Best Practices](#best-practices)
8. [FAQ](#faq)

## Getting Started

### What is Hermes SMS Forward?

Hermes SMS Forward is a secure Android application that automatically forwards incoming SMS messages to a phone number you specify. It's perfect for:

- **Business Users**: Forward work SMS to personal phone
- **Travelers**: Receive SMS while abroad on your primary device
- **Family Management**: Monitor family member SMS (with consent)
- **Backup**: Ensure important SMS reach multiple devices

### Key Benefits

- **Complete Privacy**: All processing happens on your device
- **Real-time Forwarding**: Instant SMS forwarding
- **Dual SIM Support**: Advanced dual SIM routing capabilities
- **Smart Filtering**: Content-based message filtering
- **Multiple Targets**: Forward to unlimited phone numbers
- **Secure**: No data sent to external servers
- **Simple Setup**: Easy guided onboarding process
- **Reliable**: Production-tested and optimized

## 📱 Main Interface

### Application Layout

```text
┌─────────────────────────────────────┐
│ 🏛️ Hermes SMS Forward              │
├─────────────────────────────────────┤
│                                     │
│  📞 Target Phone Number             │
│  ┌─────────────────────────────────┐ │
│  │ +90555123456                    │ │
│  └─────────────────────────────────┘ │
│                                     │
│  Status: ● Active / ○ Inactive      │
│                                     │
│  ┌─────────────────────────────────┐ │
│  │      💾 Save and Start          │ │
│  └─────────────────────────────────┘ │
│                                     │
│  Last Activity: 2025-07-25 14:30    │
│                                     │
└─────────────────────────────────────┘
```

### Interface Elements

#### 1. Target Phone Number Field

- **Purpose**: Enter the phone number where SMS will be forwarded
- **Format**: Supports international (+90555123456) and local (05551234567) formats
- **Validation**: Real-time validation with visual feedback
- **Status Indicators**:
  - ✅ Green checkmark: Valid number format
  - ❌ Red X: Invalid format
  - ⚠️ Orange warning: Questionable format

#### 2. Status Indicator

- **Active (Green Dot)**: SMS forwarding is enabled and working
- **Inactive (Gray Dot)**: SMS forwarding is disabled
- **Text Description**: Shows current forwarding status

#### 3. Save and Start Button

- **Enabled**: When valid phone number is entered
- **Disabled**: When number format is invalid
- **Function**: Saves configuration and starts SMS forwarding

#### 4. Last Activity

- **Purpose**: Shows when the last SMS was forwarded
- **Format**: Date and time in local timezone
- **Updates**: Automatically refreshes when SMS is forwarded

## ⚙️ Setup and Configuration

### Initial Setup Process

#### Step 1: Permission Grant

When you first launch the app:

1. **SMS Permission Dialog** appears
2. **Tap "Allow"** to grant SMS permissions
3. **Both permissions required**:
   - RECEIVE_SMS: To intercept incoming messages
   - SEND_SMS: To forward messages to target number

**Important**: Both permissions are mandatory for the app to function.

#### Step 2: Phone Number Entry

1. **Tap the phone number field**
2. **Enter target number** where SMS should be forwarded
3. **Use proper format**:
   - International: `+90555123456`
   - Local (Turkey): `05551234567` or `555 123 45 67`
   - Other countries: Include country code

#### Step 3: Number Validation

The app validates your number in real-time:

- **Green Checkmark**: Number is properly formatted
- **Red X**: Invalid format, please correct
- **Orange Warning**: Format might work but is non-standard

#### Step 4: Activation

1. **Ensure green checkmark** is visible
2. **Tap "Save and Start"** button
3. **Confirmation message** appears
4. **Status changes to "Active"**

### Number Format Guide

#### Turkish Numbers

```text
✅ Correct Formats:
+905551234567    (International format)
05551234567      (Local format)
555 123 45 67    (Spaced format)
0555-123-45-67   (Dashed format)

❌ Incorrect Formats:
5551234567       (Missing leading 0)
905551234567     (Missing + for international)
+90 555 123 4567 (Spaces in wrong places)
```

#### International Numbers

```text
✅ Correct Formats:
+1555123456      (US number)
+442071234567    (UK number)
+4915123456789   (German number)

❌ Incorrect Formats:
15551234567      (Missing + for international)
+1 555 123 456   (Spaces in number)
001555123456     (Using exit code instead of +)
```

## 📲 Daily Usage

### Normal Operation

Once configured, the app works automatically:

1. **Incoming SMS** arrives on your device
2. **App intercepts** the message immediately
3. **Forwards SMS** to your target number with:
   - Original sender information
   - Complete message content
   - Timestamp of reception
   - Optional device identifier

### Forwarded Message Format

Your forwarded SMS will look like this:

```text
From: +905551234567
Time: 2025-07-25 14:30:15
Message: Hello, this is the original message content!
```

### Monitoring Activity

- **Status Indicator**: Shows active/inactive state
- **Last Activity**: Displays when last SMS was forwarded
- **No Notifications**: App works silently in background

### Starting and Stopping

#### To Stop SMS Forwarding

1. Open the app
2. Clear the target phone number field
3. Tap "Save and Start" (button will change to "Save")

#### To Resume SMS Forwarding

1. Open the app
2. Enter your target phone number
3. Wait for green checkmark validation
4. Tap "Save and Start"

## 🔧 Advanced Features

### Battery Optimization

For reliable operation, disable battery optimization:

#### Samsung Devices

1. Settings > Device care > Battery
2. App power management > Hermes SMS Forward
3. Disable "Put app to sleep"

#### Huawei Devices

1. Settings > Battery > App launch
2. Find Hermes SMS Forward
3. Turn off "Manage automatically"
4. Enable all launch options

#### Stock Android

1. Settings > Battery > Battery optimization
2. Select "All apps"
3. Find Hermes SMS Forward
4. Select "Don't optimize"

### Multiple Phone Management

If you have multiple phones:

1. **Install on each device** you want to forward FROM
2. **Use same target number** for all devices
3. **Different target numbers** for different sources
4. **Identify source** by checking forwarded message format

### Long Message Handling

The app automatically handles long SMS messages:

- **Multipart SMS**: Long messages split automatically
- **Preserves Order**: Message parts sent in correct sequence
- **Complete Content**: No message truncation
- **Error Recovery**: Resends failed parts

## 🔍 Troubleshooting

### Common Issues and Solutions

#### Issue: SMS Not Forwarding

**Possible Causes and Solutions:**

1. **Permissions Not Granted**
   - Go to Settings > Apps > Hermes SMS Forward > Permissions
   - Ensure SMS permission is enabled
   - Restart the app

2. **Invalid Target Number**
   - Check number format (use international format)
   - Verify the number can receive SMS
   - Try sending test SMS to target manually

3. **Battery Optimization**
   - Disable battery optimization for the app
   - Check manufacturer-specific power management settings

4. **Carrier Restrictions**
   - Some carriers block automatic SMS
   - Contact carrier to verify SMS forwarding is allowed
   - Try with different carrier SIM card

#### Issue: Delayed Forwarding

**Causes and Solutions:**

1. **Network Congestion**
   - SMS forwarding depends on carrier network
   - Delays are usually temporary
   - No action needed, system will catch up

2. **Device Performance**
   - Close unnecessary apps
   - Restart device if very slow
   - Check available RAM

#### Issue: Partial Message Forwarding

**Causes and Solutions:**

1. **Multipart SMS Issues**
   - Long messages might have delivery issues
   - Check if all parts were received
   - Retry sending if incomplete

2. **Character Encoding**
   - Some special characters might cause issues
   - Mostly affects emojis or non-Latin scripts
   - Usually resolves automatically

### Error Messages

#### "Permission Denied"

- **Meaning**: SMS permissions not granted
- **Solution**: Go to app permissions and enable SMS

#### "Invalid Phone Number"  

- **Meaning**: Target number format is incorrect
- **Solution**: Use international format (+country_code_number)

#### "SMS Send Failed"

- **Meaning**: Unable to send forwarded SMS
- **Solution**: Check carrier service, SIM card, and network

#### "App Not Responding"

- **Meaning**: App is overloaded or has crashed
- **Solution**: Force close and restart app

## 🛡️ Best Practices

### Security Guidelines

#### 1. Choose Target Numbers Carefully

- Only forward to numbers you control
- Avoid forwarding to shared or public numbers
- Regularly review your target number

#### 2. Respect Privacy Laws

- Obtain consent before forwarding others' SMS
- Comply with local privacy regulations
- Use responsibly for legitimate purposes only

#### 3. Regular Monitoring

- Check forwarding status periodically
- Verify forwarded messages are being received
- Update target number if needed

### Performance Optimization

#### 1. Keep App Updated

- Install updates promptly for bug fixes
- New versions improve performance and security
- Check GitHub releases for latest version

#### 2. Device Maintenance

- Keep device storage clean (at least 1GB free)
- Restart device weekly for optimal performance
- Update Android OS when available

#### 3. Network Considerations

- SMS forwarding uses cellular network
- Roaming charges may apply when traveling
- Wi-Fi doesn't affect SMS forwarding

### Usage Recommendations

#### 1. Business Use

- Set up during business hours first
- Test with non-critical messages initially
- Have backup communication method ready

#### 2. Travel Use

- Set up before traveling
- Verify international SMS charges
- Consider time zone differences for message timing

#### 3. Family Use

- Explain system to family members
- Ensure everyone consents to forwarding
- Respect privacy and only monitor when necessary

## ❓ FAQ

### General Questions

**Q: Does the app require internet connection?**
A: No, Hermes SMS Forward works completely offline using only SMS functionality.

**Q: Will forwarding work if my phone is turned off?**
A: No, the phone must be on and have cellular service for SMS forwarding to work.

**Q: Can I forward to multiple numbers?**
A: Currently, you can only forward to one target number. Multiple target support is planned for future versions.

**Q: Does forwarding work with WhatsApp or other messaging apps?**
A: No, only traditional SMS messages are forwarded. App-based messages are not supported.

### Technical Questions

**Q: How much battery does the app use?**
A: Very minimal. The app only activates when SMS arrives and uses optimized code for efficiency.

**Q: Will it slow down my phone?**
A: No, the app is designed to be lightweight and only processes SMS when they arrive.

**Q: Can I see a history of forwarded messages?**
A: Currently no, but SMS history feature is planned for version 2.2.0.

**Q: What happens to SMS if forwarding fails?**
A: The app will retry up to 3 times with increasing delays. Original SMS remains on your device.

### Privacy Questions

**Q: Are my SMS messages stored anywhere?**
A: No, SMS content is never stored. Only the target phone number is saved locally on your device.

**Q: Can the developer see my messages?**
A: No, all processing happens locally on your device. No data is sent to external servers.

**Q: Is the app secure?**
A: Yes, the app uses Android's security features and includes additional privacy protections like phone number masking in logs.

**Q: Can I delete all my data?**
A: Yes, uninstalling the app removes all data completely from your device.

### Setup Questions

**Q: Why does the app need SMS permissions?**
A: RECEIVE_SMS is needed to intercept incoming messages, and SEND_SMS is needed to forward them to your target number.

**Q: Can I use the app without granting permissions?**
A: No, both SMS permissions are required for the app to function.

**Q: What phone number formats are supported?**
A: International format (+country_code_number) is recommended, but local formats are also supported for Turkish numbers.

### Troubleshooting Questions

**Q: What if SMS forwarding suddenly stops working?**
A: Check permissions, verify target number, ensure battery optimization is disabled, and restart the app.

**Q: Why am I getting "invalid number" error?**
A: Use international format (+90555123456) instead of local format, and ensure no extra characters or spaces.

**Q: The app crashes when I try to enter a phone number. What should I do?**
A: Try restarting the app, clearing app cache, or reinstalling the app. Contact support if the issue persists.

## 📞 Getting Help

### Support Channels

- **Email**: <support@hermessms.com>
- **GitHub Issues**: [Report Problems](https://github.com/hermesthecat/Hermes-SMS-Forward/issues)
- **Documentation**: README.md and other guides in the repository

### Before Contacting Support

Please have ready:

- Android version and device model
- App version (check About section)
- Target phone number format (masked for privacy)
- Description of the issue
- Steps you've already tried

### Response Times

- **Email Support**: Within 24-48 hours
- **GitHub Issues**: Usually within 24 hours
- **Critical Security Issues**: Within 24 hours

---

**Note**: This user manual is regularly updated. For the latest version and additional resources, visit our [GitHub repository](https://github.com/hermesthecat/Hermes-SMS-Forward).

**Happy SMS Forwarding!** 📱➡️📱
