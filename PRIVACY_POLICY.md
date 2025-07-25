# Privacy Policy - Hermes SMS Forward

**Effective Date:** July 25, 2025  
**Last Updated:** July 25, 2025

## 1. Introduction

This Privacy Policy explains how Hermes SMS Forward ("we," "our," or "the app") collects, uses, and protects your information when you use our SMS forwarding application.

## 2. Information We Collect

### 2.1 SMS Messages

- **What we collect:** The app accesses incoming SMS messages to forward them to your designated number
- **Why we collect it:** To provide the core SMS forwarding functionality
- **How we use it:** SMS content is only used for forwarding purposes and is not stored permanently

### 2.2 Phone Numbers

- **What we collect:** Your target phone number for SMS forwarding
- **Why we collect it:** To know where to forward incoming SMS messages
- **How we store it:** Stored locally on your device using Android's SharedPreferences

### 2.3 App Usage Data

- **What we collect:** Basic app functionality logs for debugging purposes
- **Why we collect it:** To improve app performance and fix issues
- **How we use it:** Only used locally on your device, not transmitted to external servers

## 3. Data Storage and Processing

### 3.1 Local Storage Only

- All data processing happens locally on your Android device
- We do NOT collect, store, or transmit any personal data to external servers
- Your SMS content and phone numbers never leave your device except for the forwarding functionality

### 3.2 No Cloud Storage

- We do not use any cloud storage services
- We do not maintain any databases or servers that store your personal information
- All app settings and data are stored locally using Android's secure storage mechanisms

## 4. Data Sharing and Disclosure

### 4.1 No Third-Party Sharing

- We do NOT share your SMS content, phone numbers, or any personal data with third parties
- We do NOT sell, rent, or lease your personal information to anyone
- We do NOT use analytics services that collect personal data

### 4.2 Legal Requirements

We may disclose information if required by law, but since we don't collect or store personal data on our servers, there is minimal data available for such disclosures.

## 5. Permissions and Their Use

### 5.1 SMS Permissions

- **RECEIVE_SMS:** Required to intercept incoming SMS messages for forwarding
- **SEND_SMS:** Required to forward SMS messages to your designated number

### 5.2 Permission Usage

- These permissions are used ONLY for the core SMS forwarding functionality
- We do not access SMS messages for any other purpose
- We do not read or analyze historical SMS messages stored on your device

## 6. Data Security

### 6.1 Security Measures

- Phone numbers are masked in debug logs for security
- Production builds have sensitive logging disabled
- All local data storage uses Android's secure SharedPreferences
- The app uses ProGuard/R8 code obfuscation for additional security

### 6.2 Data Retention

- Target phone numbers are stored locally until you change or delete them
- SMS content is not retained after forwarding
- App logs are automatically cleared when the app is uninstalled

## 7. Children's Privacy

This app is not intended for use by children under 18 years of age. We do not knowingly collect personal information from children under 18. If you are a parent or guardian and believe your child has provided us with personal information, please contact us.

## 8. Your Rights and Choices

### 8.1 Data Control

- You can delete the target phone number at any time through the app settings
- You can revoke SMS permissions through Android's app settings
- Uninstalling the app removes all locally stored data

### 8.2 Opt-Out

- You can stop SMS forwarding by removing the target phone number or revoking permissions
- You can uninstall the app at any time to completely stop all data processing

## 9. Changes to This Privacy Policy

We may update this Privacy Policy from time to time. When we do:

- We will update the "Last Updated" date at the top of this policy
- We will notify users through the app or other appropriate means
- Continued use of the app after changes constitutes acceptance of the new policy

## 10. International Data Transfers

Since all data processing happens locally on your device, there are no international data transfers involved in the app's operation.

## 11. Contact Information

If you have any questions, concerns, or requests regarding this Privacy Policy or our data practices, please contact us:

- **Email:** <privacy@hermessms.com>
- **GitHub Issues:** <https://github.com/hermesthecat/Hermes-SMS-Forward/issues>
- **Developer:** KeremGok Development

## 12. Legal Compliance

### 12.1 GDPR Compliance (EU Users)

- We process data only for legitimate purposes (SMS forwarding)
- All processing happens locally on your device
- You have full control over your data
- You can request data deletion by uninstalling the app

### 12.2 CCPA Compliance (California Users)

- We do not sell personal information
- We do not collect personal information for commercial purposes beyond app functionality
- You have the right to delete personal information (by uninstalling the app)

### 12.3 Turkish Data Protection Law (KVKK)

- Data processing is limited to app functionality
- Users are informed about data processing through this policy
- No data is transferred outside Turkey except for SMS forwarding as instructed by the user

## 13. Technical Implementation

### 13.1 Data Minimization

- We collect only the minimum data necessary for SMS forwarding
- SMS content is processed but not stored
- Only the target phone number is persistently stored

### 13.2 Security by Design

- The app is designed with privacy and security as primary considerations
- Debug information is sanitized in production builds
- All sensitive operations are logged securely

---

**Disclaimer:** This Privacy Policy applies only to the Hermes SMS Forward application. We are not responsible for the privacy practices of third-party services or applications that may be used in conjunction with our app.

By using Hermes SMS Forward, you acknowledge that you have read and understood this Privacy Policy and agree to its terms.
