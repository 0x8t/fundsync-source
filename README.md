# FundSync Documentation

## Overview
FundSync is an Android application that bridges UPI payments with Streamlabs alerts. It monitors UPI payment notifications from popular apps (Google Pay, PhonePe, Paytm, Amazon) and automatically forwards them as donations to Streamlabs, making it perfect for streamers who accept UPI payments during streams.

## Features
- Real-time UPI payment monitoring
- Automatic Streamlabs donation forwarding
- Dark/Light theme support
- Persistent notification history
- Test donation functionality
- Error handling with detailed feedback
- Material Design 3 UI

## Technical Architecture

### Core Components

1. **MainActivity**
   - Main UI controller
   - Handles user interactions
   - Manages notification display
   - Coordinates authentication flow

2. **StreamlabsService**
   - Manages Streamlabs API communication
   - Handles OAuth authentication
   - Processes donation requests
   - Provides callback mechanism for UI updates

3. **FundSyncNotificationListener**
   - Android NotificationListenerService implementation
   - Monitors UPI payment notifications
   - Extracts payment details using regex patterns
   - Forwards payments to StreamlabsService

4. **StorageManager**
   - Handles persistent storage of notifications
   - Uses JSON format for data storage
   - Implements thread-safe file operations

### Data Models

1. **UpiNotification**
```kotlin
data class UpiNotification(
    val amount: String,
    val sender: String,
    val timestamp: Long,
    val donationId: String?,
    val isSuccess: Boolean,
    val errorMessage: String?
)
```

## Setup Instructions

### 1. Streamlabs API Configuration

1. Visit [Streamlabs Developer Portal](https://dev.streamlabs.com/)
2. Log in with your Streamlabs account
3. Create a new application:
   - Click "Create New Application"
   - Fill in application details:
     - Name: "FundSync" (or your preferred name)
     - Description: "UPI payment to Streamlabs bridge"
     - Category: "Donations"
   - Set OAuth Redirect URI: `fundsync://com.zero.fundsync`
   - Note down the Client ID and Client Secret

### 2. Project Configuration

1. Clone the repository
2. Find `secrets.xml` in `app/src/main/res/values/`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="streamlabs_client_id">YOUR_CLIENT_ID</string>
    <string name="streamlabs_client_secret">YOUR_CLIENT_SECRET</string>
    <string name="streamlabs_redirect_uri">fundsync://com.zero.fundsync</string>
</resources>
```
3. Replace `YOUR_CLIENT_ID` and `YOUR_CLIENT_SECRET` with values from Streamlabs

### 3. Building the Project

1. Open the project in Android Studio
2. Sync project with Gradle files
3. Build the project (⌘F9 or Ctrl+F9)

## Usage Guide

### First Launch

1. Grant Notification Access
   - App will prompt for notification access
   - Click "Grant Access"
   - Enable FundSync in system settings

2. Connect Streamlabs Account
   - Click "Connect" when prompted
   - Log in to Streamlabs
   - Authorize the application

### Regular Usage

1. **Receiving Donations**
   - UPI payments are automatically detected
   - Notifications appear in the app
   - Donations are forwarded to Streamlabs

2. **Testing**
   - Use the debug button (₹) to send test donations
   - Test donations appear in both app and Streamlabs

3. **Viewing History**
   - Scroll through notification history
   - Green status indicates successful donations
   - Red status indicates failed donations
   - Click info button on failed donations for details

### Theme Customization

- Click sun/moon icon to toggle theme
- Theme preference is saved automatically

## Troubleshooting

### Common Issues

1. **Notifications Not Detected**
   - Verify notification access is enabled
   - Ensure UPI app is supported (GPay/PhonePe/Paytm/Amazon)
   - Check if notifications are enabled for UPI apps

2. **Streamlabs Connection Failed**
   - Verify internet connection
   - Check Client ID and Secret
   - Try reconnecting account

3. **Donations Not Appearing in OBS**
   - Verify Streamlabs connection in OBS
   - Check alert box settings
   - Test with debug donation button

## Security Considerations

1. **API Keys**
   - Store Client ID and Secret in `secrets.xml`
   - Never commit `secrets.xml` to version control
   - Add to `.gitignore`: `/app/src/main/res/values/secrets.xml`

2. **OAuth Tokens**
   - Tokens stored securely using Android's SharedPreferences
   - Automatically refreshed when expired
   - Cleared on authentication errors

## Performance Optimizations

1. **Network**
   - OkHttp caching enabled (10MB)
   - Connection/read/write timeouts configured
   - Efficient token management

2. **Storage**
   - JSON-based notification storage
   - Thread-safe file operations
   - Maximum 1000 notifications stored

3. **UI**
   - RecyclerView for efficient list rendering
   - Smooth scrolling and animations
   - Proper configuration change handling

## Contributing

1. Fork the repository
2. Create feature branch
3. Implement changes
4. Submit pull request

## License
[Insert your chosen license here]

## Support
Join our [Discord server](https://discord.gg/Yjrq3wNhAs) for support and updates.

---

This documentation provides a comprehensive overview of the FundSync project. For specific implementation details, refer to the inline code comments and the respective class documentation.

Remember to keep your Client ID and Secret secure and never share them publicly. If you suspect your credentials have been compromised, regenerate them immediately in the Streamlabs Developer Portal.
