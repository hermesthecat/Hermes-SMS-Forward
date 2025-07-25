# Data Handling Disclosure - Hermes SMS Forward

**Last Updated:** July 25, 2025  
**App Version:** 2.0.0

This document provides a comprehensive disclosure of how Hermes SMS Forward handles user data in compliance with Google Play Store requirements and international privacy regulations.

## 📊 Data Collection and Processing Summary

### Data Types Processed

| Data Type | Purpose | Storage Location | Retention Period | Third-Party Sharing |
|-----------|---------|------------------|------------------|-------------------|
| SMS Content | Forwarding functionality | Not stored (processed only) | Not retained | No |
| Target Phone Number | SMS forwarding destination | Device only (SharedPreferences) | Until user changes/deletes | No |
| Sender Phone Numbers | SMS forwarding context | Not stored (processed only) | Not retained | No |
| App Usage Logs | Debugging and error handling | Device only | Until app uninstall | No |

## 🔒 Data Security Measures

### Technical Safeguards

- **Local Processing Only:** All data processing occurs locally on the user's device
- **No Server Communication:** The app does not communicate with external servers
- **Encrypted Storage:** Uses Android's secure SharedPreferences for local data storage
- **Code Obfuscation:** ProGuard/R8 implementation for additional security
- **Permission Validation:** Runtime permission checks before accessing sensitive data

### Privacy by Design

- **Data Minimization:** Only processes data necessary for SMS forwarding functionality
- **Purpose Limitation:** Data used solely for intended SMS forwarding purposes
- **No Data Mining:** No analysis, profiling, or data mining of user communications
- **Secure Logging:** Sensitive information masked in debug logs, disabled in production

## 📱 Data Flow Architecture

### SMS Processing Flow

1. **SMS Reception:** Android system broadcasts incoming SMS to the app
2. **Content Extraction:** App extracts sender, message content, and timestamp
3. **Formatting:** Creates forwarded message with original sender information
4. **Transmission:** Sends formatted message to user-specified target number
5. **Cleanup:** All processed data immediately cleared from memory

### Configuration Data Flow

1. **User Input:** User enters target phone number in the app interface
2. **Validation:** Phone number validated using local validation algorithms
3. **Storage:** Valid phone number stored in Android's SharedPreferences
4. **Retrieval:** Number retrieved when SMS forwarding is needed
5. **Security:** Number masked in any log outputs for privacy

## 🌍 International Compliance

### GDPR Compliance (European Union)

- **Lawful Basis:** Processing based on user consent for legitimate purposes
- **Data Minimization:** Only essential data is processed
- **Purpose Limitation:** Data used only for stated SMS forwarding functionality
- **Storage Limitation:** No long-term data retention
- **Transparency:** Full disclosure of data processing activities
- **User Rights:** Users can delete all data by uninstalling the app

### CCPA Compliance (California, USA)

- **No Sale of Personal Information:** We do not sell any personal information
- **No Commercial Use:** Data not used for commercial purposes beyond app functionality
- **Right to Delete:** Users can delete all data by uninstalling the app
- **Right to Know:** This disclosure provides complete transparency
- **Non-Discrimination:** Equal service regardless of privacy choices

### KVKK Compliance (Turkey)

- **Data Controller:** KeremGok Development acts as data controller
- **Processing Basis:** User consent and legitimate interest for service provision
- **Data Subject Rights:** Full rights granted including access, correction, and deletion
- **Cross-Border Transfer:** Only occurs when user initiates SMS forwarding
- **Data Security:** Appropriate technical measures implemented

## 🚫 What We Don't Do

### No Data Collection

- ❌ We do not collect SMS messages for storage or analysis
- ❌ We do not maintain databases of user communications
- ❌ We do not create user profiles or behavioral analytics
- ❌ We do not track user location or device information
- ❌ We do not access contact lists or other personal data

### No Third-Party Sharing

- ❌ We do not share data with advertisers
- ❌ We do not use analytics services that collect personal data
- ❌ We do not integrate with social media platforms
- ❌ We do not sell or rent user information
- ❌ We do not use cloud storage services for user data

### No Commercial Use

- ❌ We do not analyze SMS content for marketing purposes
- ❌ We do not use personal data for targeted advertising
- ❌ We do not monetize user data in any way
- ❌ We do not create advertising profiles
- ❌ We do not share data for commercial gain

## 👶 Children's Privacy

### Age Restrictions

- **Minimum Age:** 18+ (Adult supervision required for minors)
- **Parental Consent:** Required for users under 18 in applicable jurisdictions
- **Educational Use:** Limited educational use permitted with appropriate supervision
- **Data Protection:** Extra safeguards for any minor users

### COPPA Compliance (USA)

- We do not knowingly collect personal information from children under 13
- If we become aware of data collection from children under 13, we immediately delete it
- Parents can contact us to review, delete, or refuse further collection of their child's information

## 🔄 Data Lifecycle Management

### Collection Phase

- **Minimal Collection:** Only essential data for SMS forwarding
- **Consent-Based:** Clear user consent before any data processing
- **Transparent:** Users informed of all data processing activities

### Processing Phase

- **Purpose-Limited:** Data used only for stated SMS forwarding purposes
- **Secure Processing:** All processing occurs in secure, local environment
- **No Retention:** SMS content not retained after processing

### Storage Phase

- **Local Only:** Configuration data stored only on user's device
- **Encrypted:** Uses Android's secure storage mechanisms
- **User-Controlled:** Users can modify or delete stored data at any time

### Deletion Phase

- **Automatic:** SMS content automatically cleared after processing
- **User-Initiated:** Users can delete configuration data through app settings
- **Complete Removal:** Uninstalling app removes all data completely

## 📋 User Rights and Controls

### Access Rights

- Users can view their stored target phone number in app settings
- Users can export their configuration data (single phone number)
- Users can request information about data processing (this document serves this purpose)

### Correction Rights

- Users can modify their target phone number at any time
- Users can update app settings and preferences
- No other personal data is stored that requires correction

### Deletion Rights

- Users can delete target phone number through app settings
- Users can completely remove all data by uninstalling the app
- Users can revoke SMS permissions to stop all data processing

### Portability Rights

- Users can export their configuration (target phone number) for use elsewhere
- Configuration data is stored in standard Android format
- No complex data structures or proprietary formats used

## 📞 Contact Information

### Data Protection Inquiries

- **Email:** <privacy@hermessms.com>
- **Subject Line:** "Data Handling Inquiry - Hermes SMS Forward"
- **Response Time:** Within 72 hours for privacy-related inquiries

### Technical Support

- **Email:** <support@hermessms.com>
- **GitHub Issues:** <https://github.com/hermesthecat/Hermes-SMS-Forward/issues>
- **Documentation:** Comprehensive documentation available in app repository

## 📝 Regular Updates

### Review Schedule

- This disclosure is reviewed quarterly for accuracy and completeness
- Updates made as needed when app functionality changes
- Users notified of material changes through app updates or notifications

### Version History

- **v2.0.0 (July 2025):** Initial comprehensive data handling disclosure
- **v1.9.0 (July 2025):** Added production build security enhancements
- **v1.8.0 (July 2025):** Implemented comprehensive testing framework

---

**Declaration of Accuracy:** This data handling disclosure accurately represents all data processing activities of Hermes SMS Forward as of the last updated date. We commit to maintaining transparency and updating this document as needed to reflect any changes in our data handling practices.

**Contact for Corrections:** If you find any inaccuracies in this disclosure, please contact <privacy@hermessms.com> immediately so we can investigate and correct any issues.
