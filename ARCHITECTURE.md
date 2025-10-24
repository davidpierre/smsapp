# Technical Architecture

## Overview

This SMS/MMS app is built using Flutter for the UI layer and native Kotlin for Android platform integration. It follows a plugin architecture pattern with clear separation between the platform interface and implementation.

## Architecture Layers

```
┌─────────────────────────────────────────────┐
│           Flutter UI Layer                  │
│  (Material 3, Riverpod State Management)    │
└─────────────────┬───────────────────────────┘
                  │
                  ↓
┌─────────────────────────────────────────────┐
│        Platform Interface Layer             │
│         (sms_platform package)              │
│      Pure Dart - No platform code           │
└─────────────────┬───────────────────────────┘
                  │
                  ↓
┌─────────────────────────────────────────────┐
│     Android Implementation Layer            │
│        (sms_android package)                │
│   Kotlin + Android Telephony APIs           │
└─────────────────┬───────────────────────────┘
                  │
                  ↓
┌─────────────────────────────────────────────┐
│         Android System Layer                │
│  (Telephony Provider, SmsManager, etc.)     │
└─────────────────────────────────────────────┘
```

## Package Structure

### 1. Main App (`lib/`)

**Purpose**: User interface and application logic

**Key Components**:
- `main.dart`: App entry point, Riverpod setup, theme configuration
- `providers/sms_provider.dart`: State management, data providers
- `screens/`: UI screens (home, thread, composer, setup)
- `utils/`: Helper functions (date formatting)

**Dependencies**:
- flutter_riverpod: State management
- sms_platform: Platform interface
- sms_android: Android implementation
- image_picker: Image selection for MMS
- intl: Date/time formatting

**State Flow**:
```
User Action → Provider Action → Platform Method → Android API
                    ↓
            Provider State Update
                    ↓
            UI Rebuild (Riverpod)
```

### 2. Platform Interface (`packages/sms_platform/`)

**Purpose**: Define platform-agnostic SMS operations contract

**Key Components**:
- `SmsPlatform`: Abstract base class with platform methods
- Models: `SmsMessage`, `SmsThread`, `IncomingSms`, `MmsAttachment`

**Design Pattern**: Platform Interface Pattern
- Uses `plugin_platform_interface` package
- Token-based instance verification
- Type-safe data models
- No platform-specific code

**Methods**:
```dart
- isDefaultSmsApp() → Future<bool>
- requestDefaultSmsRole() → Future<bool>
- sendText(address, body) → Future<bool>
- sendMms(addresses, text, attachments) → Future<bool>
- listThreads(limit) → Future<List<SmsThread>>
- listMessages(threadId, limit) → Future<List<SmsMessage>>
- watchIncoming() → Stream<IncomingSms>
- requestPermissions() → Future<bool>
- hasPermissions() → Future<bool>
```

### 3. Android Implementation (`packages/sms_android/`)

**Purpose**: Implement SMS/MMS operations using Android APIs

**Architecture**:

```
Flutter (Dart)
      ↕ MethodChannel & EventChannel
Kotlin Plugin
      ↕ Android APIs
System Telephony Provider
```

**Key Components**:

#### SmsAndroidPlugin.kt
- Main plugin class
- Implements `FlutterPlugin`, `ActivityAware`, `MethodChannel.MethodCallHandler`
- Handles method calls from Flutter
- Manages activity lifecycle
- Handles permission requests
- Handles role requests (default SMS app)

**Key Methods**:
```kotlin
- isDefaultSmsApp(): Check SMS role status
- requestDefaultSmsRole(): Request RoleManager.ROLE_SMS
- sendText(): Use SmsManager.sendTextMessage()
- sendMms(): Use SmsManager.sendMultimediaMessage()
- listThreads(): Query Telephony.Sms.Conversations.CONTENT_URI
- listMessages(): Query Telephony.Sms.CONTENT_URI
- requestPermissions(): ActivityCompat.requestPermissions()
- hasPermissions(): Check permission status
```

#### SmsReceiver.kt
- BroadcastReceiver for incoming SMS
- Listens for `SMS_DELIVER_ACTION`
- Parses SMS PDU using `Telephony.Sms.Intents.getMessagesFromIntent()`
- Forwards to Flutter via EventChannel

#### MmsReceiver.kt
- BroadcastReceiver for incoming MMS
- Listens for `WAP_PUSH_DELIVER_ACTION`
- Triggers MMS download (simplified in v1.0)
- Forwards notification to Flutter via EventChannel

## Communication Channels

### MethodChannel (`sms_android`)
**Purpose**: Dart ↔ Kotlin method calls

**Pattern**: Request/Response
```dart
// Dart side
final result = await _channel.invokeMethod('sendText', {
  'address': '+1234567890',
  'body': 'Hello'
});
```

```kotlin
// Kotlin side
when (call.method) {
  "sendText" -> {
    val address = call.argument<String>("address")
    val body = call.argument<String>("body")
    sendText(address, body, result)
  }
}
```

### EventChannel (`sms_android/incoming`)
**Purpose**: Stream incoming SMS/MMS to Flutter

**Pattern**: Stream/Subscribe
```dart
// Dart side
platform.watchIncoming().listen((sms) {
  print('New message: ${sms.body}');
});
```

```kotlin
// Kotlin side
companion object {
    var incomingEventSink: EventChannel.EventSink? = null
}

// In receiver
SmsAndroidPlugin.incomingEventSink?.success(messageData)
```

## Data Flow

### Sending SMS
```
User types message → ThreadScreen._sendMessage()
       ↓
SmsActions.sendText(address, body)
       ↓
SmsPlatform.sendText() via MethodChannel
       ↓
SmsAndroidPlugin.sendText()
       ↓
SmsManager.sendTextMessage()
       ↓
Android System sends SMS
       ↓
Result returns to Flutter
       ↓
UI updated (message sent confirmation)
```

### Receiving SMS
```
SMS arrives at device
       ↓
Android System broadcasts SMS_DELIVER intent
       ↓
SmsReceiver.onReceive() catches broadcast
       ↓
Parse SMS PDU → Extract address, body, timestamp
       ↓
Send to EventChannel.EventSink
       ↓
Stream emits IncomingSms event in Flutter
       ↓
Riverpod provider listens to stream
       ↓
UI updated (new message appears)
```

### Loading Threads
```
HomeScreen builds → ref.watch(threadListProvider)
       ↓
threadListProvider fetches data
       ↓
SmsPlatform.listThreads() via MethodChannel
       ↓
SmsAndroidPlugin.listThreads()
       ↓
ContentResolver.query(Telephony.Sms.Conversations.CONTENT_URI)
       ↓
Map cursor results to List<SmsThread>
       ↓
Return to Flutter via MethodChannel result
       ↓
Riverpod caches and provides to UI
       ↓
ListView.builder renders thread list
```

## State Management (Riverpod)

### Provider Hierarchy
```
ProviderScope (root)
    │
    ├─ smsPlatformProvider (singleton)
    │       │
    │       ├─ isDefaultSmsAppProvider (FutureProvider)
    │       ├─ hasPermissionsProvider (FutureProvider)
    │       ├─ threadListProvider (FutureProvider.autoDispose)
    │       ├─ threadMessagesProvider (FutureProvider.family)
    │       ├─ incomingSmsStreamProvider (StreamProvider)
    │       └─ smsActionsProvider (actions)
    │
    └─ UI Widgets (Consumers)
```

### Provider Types

**Provider** (smsPlatformProvider):
- Singleton SMS platform instance
- Never disposed
- Shared across entire app

**FutureProvider** (threadListProvider):
- Async data fetching
- Auto-caches results
- Auto-disposes when not watched
- Supports refresh via `ref.invalidate()`

**FutureProvider.family** (threadMessagesProvider):
- Parameterized provider (threadId)
- Separate cache per thread
- Auto-disposes unused threads

**StreamProvider** (incomingSmsStreamProvider):
- Continuous event stream
- Listens to EventChannel
- Updates UI in real-time

**Actions** (smsActionsProvider):
- Encapsulates write operations
- No state, just methods
- Access via `ref.read()`

## Android Manifest Configuration

### Permissions (Runtime + Install)
```xml
<uses-permission android:name="android.permission.SEND_SMS"/>
<uses-permission android:name="android.permission.RECEIVE_SMS"/>
<uses-permission android:name="android.permission.READ_SMS"/>
<uses-permission android:name="android.permission.RECEIVE_MMS"/>
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
<uses-permission android:name="android.permission.INTERNET"/>
```

### Broadcast Receivers
```xml
<receiver
    android:name="com.example.sms_android.SmsReceiver"
    android:permission="android.permission.BROADCAST_SMS"
    android:exported="true">
    <intent-filter>
        <action android:name="android.provider.Telephony.SMS_DELIVER"/>
    </intent-filter>
</receiver>

<receiver
    android:name="com.example.sms_android.MmsReceiver"
    android:permission="android.permission.BROADCAST_WAP_PUSH"
    android:exported="true">
    <intent-filter>
        <action android:name="android.provider.Telephony.WAP_PUSH_DELIVER"/>
        <data android:mimeType="application/vnd.wap.mms-message"/>
    </intent-filter>
</receiver>
```

**Critical Requirements**:
- `android:exported="true"` - Required for system broadcasts
- Permissions on receivers - Security requirement
- Proper intent filters - Routes messages to app

## Default SMS App Role

### API < 29 (Android 9 and below)
```kotlin
val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
startActivityForResult(intent, ROLE_REQUEST_CODE)
```

### API >= 29 (Android 10+)
```kotlin
val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
startActivityForResult(intent, ROLE_REQUEST_CODE)
```

**Behavior**:
1. System dialog appears to user
2. User accepts/rejects
3. Result returned via `onActivityResult()`
4. App becomes default SMS handler
5. Receives SMS broadcasts
6. Has write access to SMS database

## Database Access

### Content Provider URIs
```kotlin
// Conversations/Threads
Telephony.Sms.Conversations.CONTENT_URI
// content://mms-sms/conversations

// SMS Messages
Telephony.Sms.CONTENT_URI
// content://sms

// MMS Messages
Telephony.Mms.CONTENT_URI
// content://mms
```

### Query Pattern
```kotlin
val cursor = contentResolver.query(
    uri,              // Content URI
    projection,       // Columns to retrieve
    selection,        // WHERE clause
    selectionArgs,    // WHERE clause args
    sortOrder         // ORDER BY clause
)

cursor?.use {
    while (it.moveToNext()) {
        // Extract data from cursor
    }
}
```

## Security Considerations

### Permission Model
- Runtime permissions (Android 6+)
- Request only when needed
- Handle denial gracefully
- Explain permission usage

### Data Privacy
- No external storage
- No network transmission
- No logging of message content
- Rely on Android system security

### App Signing
- Release builds must be signed
- Protect keystore
- Use strong passwords
- Enterprise certificates supported

## Performance Optimizations

### UI Layer
- `autoDispose` on providers (memory management)
- ListView lazy loading
- Async image loading
- Minimal rebuilds (Riverpod)

### Platform Layer
- Query limits on large datasets
- Cursor pagination support
- Background thread for queries
- Efficient JSON serialization

### Native Layer
- Reuse SmsManager instance
- Efficient cursor iteration
- Query optimization (projections, indexes)
- Minimal object allocation

## Error Handling

### Pattern: Try-Catch with Fallback
```dart
try {
  final result = await platform.sendText(...);
  return result;
} catch (e) {
  // Log error
  // Show user-friendly message
  return false;
}
```

### Error Categories
1. **Permission Denied**: Request permissions
2. **Not Default SMS App**: Prompt user to set
3. **Network Error (MMS)**: Check connectivity
4. **Invalid Phone Number**: Validate input
5. **Platform Error**: Log and retry

## Testing Strategy

### Unit Tests
- Model serialization/deserialization
- Date formatting utilities
- Provider logic

### Integration Tests
- Platform interface contracts
- Method channel communication
- Event channel streams

### Manual Testing (Required)
- SMS send/receive on real device
- MMS send/receive on real device
- Default SMS app flow
- Permission grant flow
- Thread list display
- Message display
- UI navigation

### Test Coverage
```
lib/models/          → Unit tests
lib/utils/           → Unit tests
lib/providers/       → Integration tests
lib/screens/         → Widget tests
packages/sms_android → Android instrumented tests
```

## Build Configuration

### Development
```kotlin
buildTypes {
    debug {
        applicationIdSuffix = ".debug"
        debuggable = true
        minifyEnabled = false
    }
}
```

### Production
```kotlin
buildTypes {
    release {
        minifyEnabled = true
        shrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        signingConfig = signingConfigs.getByName("release")
    }
}
```

## Deployment Architecture

```
Developer Machine
      ↓ (Build APK)
CI/CD Pipeline (Optional)
      ↓ (Sign & Upload)
MDM/EMM Server
      ↓ (Push Policy)
Managed Android Devices
```

## Future Architecture Considerations

### Scalability
- Add iOS support (separate plugin)
- Multi-SIM support
- RCS messaging integration

### Features
- Local message database (SQLite)
- Full-text search (FTS5)
- Contact integration
- Message encryption

### Performance
- Message pagination
- Image caching
- Background sync
- Notification channels

## Dependencies

### Runtime
- Flutter SDK 3.3.0+
- Riverpod 2.6.1
- Android SDK 26-35

### Build
- Kotlin 2.1.0
- Android Gradle Plugin 8.7.3
- Java 17

### Development
- Android Studio / VS Code
- Flutter DevTools
- ADB (Android Debug Bridge)

## Monitoring & Debugging

### Logging
```kotlin
// Native side
android.util.Log.d("SmsAndroid", "Message sent: $address")
```

```dart
// Dart side
debugPrint('Message sent: $address');
```

### Flutter DevTools
- Widget inspector
- Provider state inspection
- Performance profiling
- Network monitoring

### ADB Commands
```bash
# View logs
adb logcat | grep -i sms

# Check permissions
adb shell dumpsys package com.example.smsapp | grep permission

# Check default SMS app
adb shell cmd role get-role-holders android.app.role.SMS
```

## Documentation

- README.md: User guide
- BUILD_GUIDE.md: Build & deployment
- ARCHITECTURE.md: This document
- CHANGELOG.md: Version history
- Inline code comments: Implementation details

## Conclusion

This architecture provides:
- ✅ Clear separation of concerns
- ✅ Platform independence (via interface)
- ✅ Type safety (Dart + Kotlin)
- ✅ Reactive UI (Riverpod)
- ✅ Real-time updates (EventChannel)
- ✅ Enterprise-ready deployment
- ✅ Maintainable codebase
- ✅ Testable components

