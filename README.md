# FundSync

FundSync is an Android app that monitors UPI payments and forwards them to Streamlabs.

## Features

- Monitors UPI notifications from apps like Google Pay, PhonePe, and PayTM
- Sends donation information to Streamlabs
- Dark/Light theme toggle
- Test donation feature

## Setup Instructions

### 1. Adding the Custom Font

To add the custom font (bicubik_regular.otf):

1. Create a `fonts` directory in the `app/src/main/res` folder
2. Place the `bicubik_regular.otf` file in this directory
3. Create a new file `app/src/main/res/font/fonts.xml` with the following content:

```xml
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">
    <font
        android:font="@font/bicubik_regular"
        android:fontStyle="normal"
        android:fontWeight="400"
        app:font="@font/bicubik_regular"
        app:fontStyle="normal"
        app:fontWeight="400" />
</font-family>
```

4. Update the `TextAppearance.FundSync.AppTitle` style in `app/src/main/res/values/themes.xml`:

```xml
<style name="TextAppearance.FundSync.AppTitle" parent="TextAppearance.Material3.TitleLarge">
    <item name="android:fontFamily">@font/bicubik_regular</item>
    <item name="fontFamily">@font/bicubik_regular</item>
    <item name="android:textSize">20sp</item>
    <item name="android:textColor">@color/text_primary</item>
</style>
```

### 2. Streamlabs Authentication

The app uses OAuth to authenticate with Streamlabs:

1. When you first grant notification access, the app will prompt you to connect to Streamlabs
2. Click "Connect" to initiate the OAuth flow
3. After authentication, the app will receive the authorization code via deep linking
4. The app will exchange this code for an access token
5. The access token is stored securely and used for all Streamlabs API requests

### 3. Notification Access

The app requires notification access to monitor UPI payments:

1. Grant notification access when prompted
2. The app will only read notifications from UPI apps (Google Pay, PhonePe, PayTM, etc.)
3. The app will extract payment information and forward it to Streamlabs

## Testing

Use the "Send Test Donation" button to send a test donation of 1 rupee to Streamlabs.

## Support

Join our Discord server for support: [https://discord.gg/Yjrq3wNhAs](https://discord.gg/Yjrq3wNhAs) 