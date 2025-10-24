# SMS/MMS Flutter App for Android

A complete SMS/MMS messaging application for Android-only deployment on company-managed devices. This app can be set as the default SMS handler on Android 13-15 devices.

## 🏗️ Project Structure

```
smsapp/
├── packages/
│   ├── sms_platform/          # Platform interface (pure Dart)
│   │   ├── lib/
│   │   │   ├── src/
│   │   │   │   ├── sms_platform_interface.dart
│   │   │   │   └── models/
│   │   │   │       ├── sms_message.dart
│   │   │   │       ├── sms_thread.dart
│   │   │   │       ├── incoming_sms.dart
│   │   │   │       └── mms_attachment.dart
│   │   │   └── sms_platform.dart
│   │   └── pubspec.yaml
│   │
│   └── sms_android/           # Android implementation
│       ├── lib/
│       │   └── sms_android.dart
│       ├── android/
│       │   ├── src/main/kotlin/com/example/sms_android/
│       │   │   ├── SmsAndroidPlugin.kt
│       │   │   ├── SmsReceiver.kt
│       │   │   └── MmsReceiver.kt
│       │   ├── build.gradle
│       │   └── AndroidManifest.xml
│       └── pubspec.yaml
│
├── lib/
│   ├── main.dart
│   ├── providers/
│   │   └── sms_provider.dart
│   ├── screens/
│   │   ├── setup_screen.dart
│   │   ├── home_screen.dart
│   │   ├── thread_screen.dart
│   │   └── new_message_screen.dart
│   └── utils/
│       └── date_formatter.dart
│
└── android/
    └── app/
        ├── build.gradle.kts
        └── AndroidManifest.xml
```

## 🎯 Features

- ✅ **Default SMS App**: Uses RoleManager API to become the default SMS handler
- ✅ **Send/Receive SMS**: Full SMS messaging support
- ✅ **Send/Receive MMS**: Picture messaging with attachment support
- ✅ **Thread List**: Conversation view with contact names and snippets
- ✅ **Real-time Updates**: Live incoming message notifications via EventChannel
- ✅ **Material 3 UI**: Modern, clean interface following Material Design 3
- ✅ **Permission Handling**: Proper runtime permission flow
- ✅ **Group Messaging**: Support for multiple recipients

## 🔧 Technical Specifications

### Android Configuration
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 35
- **Min SDK**: 26 (Android 8.0)
- **Build Tools**: Android Gradle Plugin 8.7.3
- **Kotlin**: 2.1.0
- **Java**: 17

### Required Permissions
```xml
- SEND_SMS
- RECEIVE_SMS
- READ_SMS
- RECEIVE_MMS
- READ_PHONE_STATE
- READ_MEDIA_IMAGES (Android 13+)
- INTERNET
```

### Broadcast Receivers
- **SMS_DELIVER**: Handles incoming SMS messages
- **WAP_PUSH_DELIVER**: Handles incoming MMS messages

## 🚀 Setup & Installation

### Prerequisites
- Flutter 3.3.0 or higher
- Android SDK 35
- Android Studio or VS Code with Flutter extensions

### Installation Steps

1. **Clone or extract the project**
   ```bash
   cd /path/to/smsapp
   ```

2. **Install Flutter dependencies**
   ```bash
   flutter pub get
   ```

3. **Verify Android SDK**
   ```bash
   flutter doctor
   ```

4. **Build the APK**
   ```bash
   flutter build apk --release
   ```

5. **Install on device**
   ```bash
   flutter install
   ```

   Or manually install the APK from:
   ```
   build/app/outputs/flutter-apk/app-release.apk
   ```

## 📱 Usage Flow

### First Launch
1. App opens to **Setup Screen**
2. User taps "Set as Default SMS App"
3. System dialog appears to grant SMS role
4. App requests SMS permissions
5. Setup complete → Navigate to message list

### Sending Messages
1. Tap **New Message** FAB on home screen
2. Enter recipient phone number(s)
3. Type message text
4. (Optional) Add image attachments for MMS
5. Tap **SEND**

### Receiving Messages
- Incoming SMS/MMS automatically appear in thread list
- Red dot indicator for unread messages
- Real-time updates without manual refresh

## 🏢 Enterprise Deployment

### MDM/EMM Configuration

This app is designed for side-loading via Mobile Device Management:

1. **Build Release APK**
   ```bash
   flutter build apk --release
   ```

2. **Sign with Company Certificate** (Optional)
   - Configure signing in `android/app/build.gradle.kts`
   - Or use MDM signing capabilities

3. **Deploy via MDM**
   - Upload APK to MDM console
   - Push to managed device group
   - Configure as default SMS app via device policy

### Required Device Permissions
```json
{
  "defaultSmsApp": "com.example.smsapp",
  "permissions": [
    "android.permission.SEND_SMS",
    "android.permission.RECEIVE_SMS",
    "android.permission.READ_SMS"
  ]
}
```

## 🔑 Key Components

### Platform Interface (`sms_platform`)
Pure Dart package defining the contract for SMS operations:
- `SmsPlatform`: Abstract base class
- Data models: `SmsMessage`, `SmsThread`, `IncomingSms`, `MmsAttachment`

### Android Plugin (`sms_android`)
Implements platform interface using Android APIs:
- `SmsAndroidPlugin`: Main plugin with MethodChannel
- `SmsReceiver`: Handles SMS_DELIVER broadcasts
- `MmsReceiver`: Handles WAP_PUSH_DELIVER broadcasts

### State Management (Riverpod)
- `smsPlatformProvider`: Singleton SMS platform instance
- `threadListProvider`: Conversation list
- `threadMessagesProvider`: Messages for specific thread
- `incomingSmsStreamProvider`: Real-time message stream
- `smsActionsProvider`: Actions like send, request permissions

## 📋 API Methods

### Platform Methods

```dart
// Check if default SMS app
await platform.isDefaultSmsApp();

// Request default SMS role
await platform.requestDefaultSmsRole();

// Send SMS
await platform.sendText(address: '+1234567890', body: 'Hello');

// Send MMS
await platform.sendMms(
  addresses: ['+1234567890'],
  text: 'Check this out',
  attachments: [attachment],
);

// List conversations
final threads = await platform.listThreads(limit: 50);

// List messages in thread
final messages = await platform.listMessages(threadId: 123, limit: 100);

// Watch incoming messages
platform.watchIncoming().listen((sms) {
  print('New message from ${sms.address}');
});

// Request permissions
await platform.requestPermissions();

// Check permissions
final hasPerms = await platform.hasPermissions();
```

## 🧪 Testing Checklist

- [ ] App becomes default SMS handler via role dialog
- [ ] SMS permissions granted after role grant
- [ ] Send SMS to another device
- [ ] Receive SMS from another device
- [ ] Send MMS with image attachment
- [ ] Receive MMS with image
- [ ] Thread list loads existing conversations
- [ ] Messages display in chronological order
- [ ] Incoming messages update UI in real-time
- [ ] Unread indicator appears for new messages
- [ ] Date/time formatting displays correctly

## 🔍 Troubleshooting

### SMS Not Sending
- Verify device has active SIM card
- Check SMS permissions granted
- Confirm app is default SMS handler
- Check Logcat for error messages

### MMS Not Working
- Verify device APN settings configured
- Confirm internet connectivity
- Check MMS permissions granted
- MMS requires data connection

### Not Receiving Messages
- Verify SMS_DELIVER receiver in manifest
- Check receiver has `android:exported="true"`
- Confirm app is default SMS handler
- Check Logcat for broadcast events

### Build Errors
```bash
# Clean and rebuild
flutter clean
flutter pub get
flutter build apk
```

## 📦 Dependencies

### Main App
- `flutter_riverpod: ^2.6.1` - State management
- `intl: ^0.19.0` - Date/time formatting
- `image_picker: ^1.1.2` - MMS image selection
- `permission_handler: ^11.3.1` - Permission utilities

### Platform Interface
- `plugin_platform_interface: ^2.1.8` - Platform interface pattern

## 🔐 Security & Privacy

- App does not store messages externally
- All data remains in Android Telephony provider
- No cloud sync or backup
- No analytics or tracking
- Suitable for corporate security requirements

## 📝 License

Internal company use only. Not for public distribution.

## 👥 Support

For internal company support:
- Contact IT Help Desk
- Submit ticket via company portal
- Email: support@company.com

## 🔄 Version History

### Version 1.0.0 (2025)
- Initial release
- SMS/MMS send and receive
- Material 3 UI
- Android 13-15 support
- Default SMS app capability
