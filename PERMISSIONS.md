# App Permissions - Hermes SMS Forward

This document explains why Hermes SMS Forward requires specific Android permissions and how they are used.

## Required Permissions

### 1. RECEIVE_SMS (Receive Text Messages)

**Why we need this permission:**

- To intercept incoming SMS messages in real-time
- This is the core functionality of the SMS forwarding app
- Without this permission, the app cannot detect new SMS messages

**How we use this permission:**

- A BroadcastReceiver listens for incoming SMS messages
- When a new SMS arrives, the app extracts the sender and message content
- The SMS is immediately processed for forwarding
- No SMS messages are stored permanently on the device

**Technical Details:**

- Uses `android.provider.Telephony.SMS_RECEIVED` broadcast intent
- BroadcastReceiver is registered in AndroidManifest.xml
- Priority set to 1000 to ensure timely processing
- Only processes SMS messages when forwarding is active

### 2. SEND_SMS (Send Text Messages)

**Why we need this permission:**

- To send the forwarded SMS to your designated target number
- This enables the app to actually perform the SMS forwarding functionality
- Without this permission, the app cannot send SMS messages

**How we use this permission:**

- Uses Android's SmsManager to send SMS messages
- Automatically handles long messages by splitting them into multiple parts
- Sends SMS with the original sender information and timestamp
- Includes delivery confirmation and failure handling

**Technical Details:**

- Uses `android.telephony.SmsManager.sendTextMessage()` API
- Handles multipart SMS with `sendMultipartTextMessage()` for long messages
- Implements PendingIntent for delivery status tracking
- Includes retry mechanism for failed sends (up to 3 attempts)

### 3. READ_PHONE_STATE (Read Phone Information)

**Why we need this permission:**

- To support dual SIM functionality on compatible devices
- Enables automatic SIM card detection and selection
- Required for advanced SIM routing features

**How we use this permission:**

- Detects available SIM cards and their properties
- Enables user to choose which SIM to use for forwarding
- Supports automatic SIM selection based on received SMS source
- No personal information or call logs are accessed

**Technical Details:**

- Uses `SubscriptionManager` API for SIM card information
- Only accesses SIM slot information and carrier details
- No access to call history, contacts, or phone numbers
- Permission gracefully degrades on single SIM devices

## Permission Security & Privacy

### Runtime Permission Requests

- All three permissions are classified as "dangerous" by Android
- The app explicitly requests these permissions at runtime
- Users can grant or deny permissions individually
- Clear explanations are provided for why each permission is needed

### Minimal Permission Usage

- Permissions are used ONLY for their intended SMS forwarding functionality
- No additional data collection or unauthorized access
- No access to other apps' data or system functions
- No internet permissions required (all processing is local)

### Permission Revocation

- Users can revoke permissions at any time through Android Settings
- The app gracefully handles permission revocation
- Clear error messages guide users if permissions are needed but not granted
- App provides direct links to system settings for easy permission management

## Security Measures

### Data Protection

- SMS content is processed but not permanently stored
- Phone numbers are masked in debug logs (e.g., +9055***4567)
- Production builds have sensitive logging disabled
- All data processing happens locally on the device

### Permission Validation

- App checks for permissions before attempting SMS operations
- Graceful error handling when permissions are not available
- User-friendly prompts explain why permissions are necessary
- No unauthorized permission requests or excessive permission usage

## Android Version Compatibility

### Modern Android (6.0+)

- Runtime permission model is fully supported
- Users can grant/revoke permissions individually
- Permission explanations are shown in-context
- Follows Android's best practices for permission requests

### Legacy Android (5.0-5.1)

- Permissions are granted at install time
- All required permissions are clearly listed in Play Store
- No runtime permission dialogs on older versions
- Maintains full functionality across all supported versions

## Permissions We DON'T Request

### Internet/Network Access

- **INTERNET** - Not required as all processing is local
- **ACCESS_NETWORK_STATE** - Not needed for SMS forwarding
- **ACCESS_WIFI_STATE** - Not used by the application

### Device Access

- **CAMERA** - Not used by the application
- **MICROPHONE** - Not used by the application
- **LOCATION** - Not required for SMS forwarding
- **BLUETOOTH** - Not required for SMS forwarding

### Storage Access

- **READ_EXTERNAL_STORAGE** - Not needed for core functionality
- **WRITE_EXTERNAL_STORAGE** - All data stored in app-private storage
- **MANAGE_EXTERNAL_STORAGE** - Not required

### System Access

- **SYSTEM_ALERT_WINDOW** - Not used for overlays
- **DEVICE_ADMIN** - Not required for SMS forwarding
- **ACCESSIBILITY_SERVICE** - Not used by the application

## User Guidance

### First Launch

1. The app will request SMS and phone permissions on first use
2. Clear explanations are provided for each permission
3. Users can choose to grant or deny permissions
4. Dual SIM features require phone permission

### Permission Management

1. Access Android Settings > Apps > Hermes SMS Forward > Permissions
2. Toggle SMS and Phone permissions on/off as needed
3. App will request permissions again when needed
4. No persistent permission requests or nagging

### Troubleshooting

- If SMS forwarding stops working, check permissions
- Ensure RECEIVE_SMS, SEND_SMS, and READ_PHONE_STATE are enabled
- Restart the app after changing permissions
- Check device SMS settings for any restrictions

## Transparency Report

### Permission Usage Statistics

- RECEIVE_SMS: Used continuously when app is active
- SEND_SMS: Used only when forwarding SMS messages
- READ_PHONE_STATE: Used for dual SIM detection and configuration
- Average SMS processing time: <100ms per message
- No background permission usage when app is inactive

### Privacy Commitment

- We do not request unnecessary permissions
- All permission usage is documented and transparent
- Regular security audits ensure proper permission handling
- Open source code available for verification

## Support

If you have questions about app permissions:

- **GitHub:** <https://github.com/hermesthecat/sms-forward-android/issues>
- **Documentation:** Check README.md for additional information

---

**Note:** This app requires SMS permissions to function properly. If you're uncomfortable granting these permissions, the app will not be able to provide SMS forwarding functionality. All permission usage is strictly limited to the documented functionality.
