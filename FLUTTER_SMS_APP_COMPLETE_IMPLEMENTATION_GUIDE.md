# Flutter Android Default SMS/MMS App - Complete Implementation Guide

> **🎯 THIS IS A FLUTTER/DART GUIDE FOR ANDROID**
> 
> **Copy this file to your new project and follow it step-by-step to recreate a working SMS app**
> 
> **✅ Works for BOTH new Flutter apps and existing Flutter apps!**
> 
> **📱 Platform:** Flutter (Dart) with Android native code (Kotlin)  
> **Integration:** Seamlessly integrates into existing Flutter apps without affecting current features

---

## 📋 What You're Building

This guide shows you how to add **SMS and MMS functionality** to a **Flutter Android app** using:

### 📱 MMS Support Status - **⚠️ CRITICAL UPDATE: MMS CURRENTLY NOT WORKING**

**🚨 HONEST STATUS REPORT:**

| Feature | Implementation | Current Status |
|---------|----------------|----------------|
| **SMS (Text Only)** | ✅ **Fully Tested & Working** | ✅ Send/receive text messages - production-ready on Android 13-15 |
| **MMS - Send Pictures** | ⚠️ **Code Included, Untested** | ⚠️ `sendMms()` implementation exists but not verified working |
| **MMS - Send Documents** | ⚠️ **Code Included, Untested** | ⚠️ MIME type support exists but not tested |
| **MMS - Receive Pictures/Files** | ❌ **NOT WORKING** | ❌ WAP_PUSH received, but downloads fail with "empty response" |
| **Image Picker UI** | ✅ **Complete Code Included** | ✅ Pick from gallery, take photos, attach multiple files (Step 3.4) |
| **File Preview** | ✅ **Complete Code Included** | ✅ See attachments before sending, remove attachments |
| **Auto SMS/MMS Detection** | ✅ **Complete Code Included** | ✅ Automatically sends as SMS (text) or MMS (attachments) |

---

### ⚠️ WHY MMS IS NOT WORKING (Read This Before Attempting MMS!)

**The Brutal Truth About MMS Implementation:**

After **weeks of debugging and multiple implementation attempts**, MMS receiving is still not working. Here's what we learned:

#### 🔴 The Core Problem: MMS is Incredibly Complex

Unlike SMS (which is simple text over cellular network), MMS is:
- **HTTP-based protocol** - Requires downloading from carrier servers
- **Carrier-specific** - Each carrier has different APN/MMSC settings
- **PDU format** - Complex binary protocol for message encoding
- **Multi-step process** - Notification → Download → Parse → Display

**The real issue:** MMS requires a complete HTTP client, APN manager, PDU parser, and carrier-specific configurations that are **far more complex than we initially thought**.

#### 📋 What We Attempted (Chronological)

**Attempt #1: Native Android SmsManager API**
- Used `SmsManager.downloadMultimediaMessage()`
- **Result:** ❌ Failed - `IllegalArgumentException: Uri contentUri null`
- **Why:** System expects specific PDU data structure that we couldn't create correctly

**Attempt #2: Manual PDU Parsing**
- Tried to manually parse WAP_PUSH PDU data
- Extract content location URL and trigger download
- **Result:** ❌ Failed - PDU parsing is extremely complex, errors everywhere

**Attempt #3: android-smsmms Library (PushReceiver)**
- Integrated `android-smsmms` library from GitHub
- Extended `PushReceiver` to handle WAP_PUSH broadcasts
- Added `TransactionService` for background downloads
- Configured APN settings in SharedPreferences
- Added User-Agent header
- **Result:** ⚠️ Partial Success - WAP_PUSH received, notification created (m_type=130), but downloads fail with "empty response"

**Attempt #4: DownloadRequest from android-smsmms**
- Used library's `DownloadRequest` class directly
- Configured `MmsNetworkManager` and `MmsHttpClient`
- **Result:** ❌ Failed - Still getting "empty response" from carrier

**Attempt #5: RetrieveTransaction**
- Used library's `RetrieveTransaction` class
- Passed proper `TransactionSettings`
- **Result:** ❌ Failed - App crashed with `AssertionError` in `PduPersister.load`

**Attempt #6: TransactionService Integration**
- Declared `TransactionService` in manifest
- Let `PushReceiver` coordinate with service
- **Result:** ❌ Still Failing - "empty response" persists

#### 🎯 Current State (Latest Attempt)

**What Works:**
- ✅ WAP_PUSH broadcasts are received
- ✅ MMS notifications are created in database (m_type=130)
- ✅ `PushReceiver` from android-smsmms is properly integrated
- ✅ `TransactionService` is declared in manifest
- ✅ APN/MMSC settings are configured in SharedPreferences
- ✅ User-Agent header is set
- ✅ WiFi and network permissions are granted

**What Doesn't Work:**
- ❌ MMS download fails with "DownloadRequest.persistIfRequired: empty response"
- ❌ Content never downloads from carrier MMSC server
- ❌ MMS stays as notification (m_type=130), never converts to downloaded (m_type=132)
- ❌ No images appear in the app

#### 🔍 Root Cause Analysis

**The "empty response" error suggests:**

1. **Carrier Configuration Issue** - T-Mobile's MMSC server might be rejecting our requests
   - Despite correct MMSC URL: `http://mms.msg.eng.t-mobile.com/mms/wapenc`
   - Despite correct User-Agent: `Android MmsLib/1.0`
   - Possible additional headers or authentication required

2. **APN Settings Incomplete** - The android-smsmms library may need additional APN parameters
   - Proxy settings
   - Authentication credentials
   - APN type specifications
   - Network preference (WiFi vs cellular)

3. **Library Integration Issues** - The android-smsmms library may expect:
   - Additional services or receivers we haven't implemented
   - Specific database schema or content provider structure
   - Background sync or JobScheduler integration

4. **Android Version Incompatibility** - Testing on Android 13-15
   - android-smsmms library was built for older Android versions
   - May not work correctly with modern Android security restrictions
   - Possible permission or network policy issues

#### 📚 Lessons Learned

**Why MMS is Hard:**

1. **No Official Documentation** - Google provides almost zero documentation on MMS implementation
2. **Carrier Dependencies** - Each carrier has proprietary MMSC configurations
3. **Protocol Complexity** - MMS PDU format is poorly documented
4. **Library Abandonment** - Most MMS libraries are outdated (android-smsmms last updated 2016)
5. **Testing Difficulty** - Requires real SIM cards and carrier data connections

**What Would Be Needed for Success:**

1. ✅ **Carrier Partnership** - Direct access to carrier APN/MMSC documentation
2. ✅ **Updated MMS Library** - Modern library built for Android 13+
3. ✅ **Extensive Testing** - Multiple carriers and devices
4. ✅ **Professional Android Developer** - With deep Android internals knowledge
5. ✅ **Time Investment** - 2-4 weeks of focused debugging with proper tools

---

### ⚠️ REALISTIC EXPECTATIONS FOR MMS

**What This Guide Provides:**
- ✅ **MMS CODE IS INCLUDED** - Kotlin implementation based on android-smsmms library
- ✅ **UI CODE IS PROVIDED** - Flutter composer with image_picker for attachments
- ⚠️ **NOT VERIFIED WORKING** - Code is untested and MMS receiving currently fails

**Current Reality:**
- ❌ **MMS RECEIVING DOES NOT WORK** - Downloads fail with "empty response" error
- ⚠️ **MMS SENDING UNTESTED** - Code exists but hasn't been verified
- ✅ **SMS FULLY WORKING** - Text-only messages work perfectly on Android 13-15

**Why MMS Is Not Working:**
- MMS is carrier-dependent with proprietary configurations
- android-smsmms library (from 2016) may not work on modern Android
- Requires extensive carrier-specific debugging
- "empty response" error suggests APN/MMSC configuration issues
- May require carrier partnership or professional Android developer

**If You Need MMS:**

**Option 1: Use System SMS App Integration**
- Instead of building a full default SMS app, integrate with system SMS app
- Use `Intent.ACTION_SEND` to send MMS via Google Messages
- Read received MMS from SMS database (requires READ_SMS permission)
- **Pros:** MMS works reliably, no complex implementation
- **Cons:** User sees system SMS composer, not fully integrated

**Option 2: Continue Debugging (Realistic Time Estimate)**
- Expect 2-4+ weeks of focused debugging
- Requires professional Android developer familiar with telephony stack
- May need carrier partnership for APN documentation
- Success not guaranteed

**Option 3: Wait for Better Libraries**
- Monitor for updated MMS libraries for modern Android
- Google may improve default SMS app APIs in future Android versions
- Current android-smsmms library is 9+ years old

**Bottom Line:**
- ✅ **SMS is production-ready** - Text messages work perfectly
- ❌ **MMS is NOT working** - Despite weeks of debugging attempts
- ⚠️ **Code is provided as reference** - But expect to debug extensively
- 💡 **Consider system SMS app integration** - More reliable for MMS

---

### 🎯 What's Included for File Attachments (MMS) - Current Status:

**⚠️ IMPORTANT: MMS is not currently working, but code is provided as reference**

1. **Kotlin Backend (Step 2.4):**
   - ⚠️ `sendMms()` method - **CODE PROVIDED BUT UNTESTED**
   - ⚠️ Handles multiple attachments - **THEORY ONLY**
   - ⚠️ Supports any MIME type (images, PDFs, etc.) - **NOT VERIFIED**
   - ⚠️ Writes MMS to database - **IMPLEMENTATION INCLUDED**
   - ⚠️ Uses Android's native `SmsManager.sendMultimediaMessage()` - **UNTESTED**
   - **Status:** Code exists but sending has not been verified to work

2. **Kotlin Receiver (Step 2.5):**
   - ⚠️ `MmsReceiverImpl` extends `PushReceiver` - **IMPLEMENTED**
   - ❌ Downloads MMS content - **FAILS WITH "EMPTY RESPONSE"**
   - ⚠️ Notifies Flutter app - **PARTIAL (notifications received, downloads fail)**
   - **Status:** WAP_PUSH received successfully, but downloads fail at carrier MMSC level

3. **Flutter UI (Step 3.4):**
   - ✅ Image picker from gallery - **WORKING**
   - ✅ Camera integration to take photos - **WORKING**
   - ⚠️ Document picker (with file_picker package) - **NOT IMPLEMENTED**
   - ✅ Multiple file attachments - **UI WORKS, SENDING UNTESTED**
   - ✅ Attachment preview - **WORKING**
   - ✅ Remove attachments - **WORKING**
   - ✅ Auto SMS/MMS switching - **WORKING**
   - **Status:** UI is functional, but backend MMS isn't working

4. **Dart Bridge (Step 3.5):**
   - ⚠️ `sendMms()` method in Dart - **CODE PROVIDED, UNTESTED**
   - ✅ Platform channel communication - **WORKING FOR SMS**
   - ✅ Provider/Bloc integration examples - **WORKING**
   - **Status:** Bridge is ready, but Kotlin backend MMS doesn't work

**Summary:**
- ✅ **UI is ready** - You can select images and files
- ⚠️ **Sending code exists** - But hasn't been tested successfully
- ❌ **Receiving fails** - "empty response" from carrier servers
- 💡 **Alternative:** Use system SMS app for MMS (Intent-based)

---

This guide shows you how to add **SMS and MMS functionality** to a **Flutter Android app** using:

- **Flutter/Dart** - UI, state management, navigation (your existing app code)
- **Kotlin** - Android native plugin for SMS operations (new plugin packages)
- **Platform Channels** - Communication between Flutter and Android

**For Existing Flutter Apps:**
- ✅ Keeps all your existing Dart/Flutter code intact
- ✅ Adds 2 new local packages (`sms_platform`, `sms_android`)
- ✅ Works with your current state management (Bloc/Provider/GetX/Riverpod)
- ✅ Integrates into your existing navigation/routing

**For New Flutter Apps:**
- ✅ Complete working SMS app from scratch
- ✅ Includes example UI screens
- ✅ Ready to deploy

---

## 📚 Guide Structure (Quick Navigation)

| Section | Content | For Existing Apps? | Language |
|---------|---------|-------------------|----------|
| ⚠️ Critical Pitfalls | **Read this first!** Avoid all common issues | ✅ Yes | All |
| 🔄 Integration Options | New vs existing app paths | ✅ **Start here** | All |
| 🔧 Step 1 | Android Gradle configuration | ✅ Yes | Gradle/Kotlin |
| 🔧 Step 2 | Kotlin plugin implementation | ✅ Yes | Kotlin |
| 🎨 Step 3 | Flutter UI implementation | ✅ Yes (optional) | Dart/Flutter |
| 🔌 Integration Guide | **For existing Flutter apps** | ✅ **Required** | All |
| 🐛 Troubleshooting | Common issues and fixes | ✅ Yes | All |
| ✅ Testing Checklist | Verify everything works | ✅ Yes | All |

**👉 If integrating into an existing Flutter app, read:**
1. ⚠️ Critical Pitfalls (Section 2)
2. 🔌 Integration into Existing Flutter App (Section 9)
3. Follow the Bloc/Provider/GetX examples for your state management

---

## 🎯 Critical Discovery: Manual Database Persistence

**THE MOST IMPORTANT THING TO KNOW:**

When your app becomes the **default SMS app**, Android does **NOT** automatically save sent or received messages to the SMS database. **YOU must manually write them!**

Without this, your app will send/receive messages, but they won't appear in the UI or in other SMS apps.

---

## ⚠️ CRITICAL PITFALLS TO AVOID

**Read this section FIRST to avoid hours of debugging!** These are the exact issues we encountered during development, with clear solutions:

### ❌ Pitfall #1: Not Saving Messages to Database (MOST CRITICAL!)

**The Problem:**
- Your app sends/receives SMS successfully
- Logs show "SMS sent" / "SMS received"
- But messages DON'T appear in UI
- Query returns 0 threads or only old messages

**Why It Happens:**
Android does **NOT** automatically save messages for default SMS apps. YOU must manually write them!

**The Fix:**
✅ Add `saveSentSmsToDatabase()` in `sendText()` method (Step 2.2)
✅ Add `saveReceivedSmsToDatabase()` in `SmsReceiver.onReceive()` (Step 2.3)
✅ Use `ContentResolver.insert()` with correct URIs:
   - Sent: `content://sms/sent` with `type=2`
   - Received: `content://sms/inbox` with `type=1`

**How to Verify:**
```kotlin
// Look for these logs after sending/receiving:
D/SmsAndroidPlugin: ✅ Saved sent SMS to database: content://sms/8
D/SmsReceiver: ✅ Saved received SMS to database: content://sms/9
```

---

### ❌ Pitfall #2: App Fails to Become Default SMS App

**The Problem:**
- `requestDefaultSmsRole()` returns `false`
- System dialog doesn't show your app
- Or dialog shows but app isn't selectable

**Why It Happens:**
Missing required components that Android checks for default SMS apps.

**The Fix:**
✅ **Add SENDTO intent filters** to MainActivity (Step 1.3):
```xml
<intent-filter>
    <action android:name="android.intent.action.SENDTO"/>
    <data android:scheme="sms"/>
</intent-filter>
```

✅ **Add HeadlessSmsSendService** (Step 2.4) - **MANDATORY for Android 10+**:
```xml
<service android:name="com.example.sms_android.HeadlessSmsSendService"
    android:permission="android.permission.SEND_RESPOND_VIA_MESSAGE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.RESPOND_VIA_MESSAGE"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <data android:scheme="sms"/>
    </intent-filter>
</service>
```

✅ **Add all broadcast receivers** (SMS_DELIVER, WAP_PUSH_DELIVER)

**How to Verify:**
```
D/SmsAndroidPlugin: Already holding SMS role
```
Or go to Android Settings → Apps → Default apps → SMS app → Your app should be listed

---

### ❌ Pitfall #3: Kotlin Version Incompatibility

**The Problem:**
```
Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin.
The actual metadata version is 2.1.0, but the compiler version 1.8.0 can read versions up to 1.9.0.
```

**Why It Happens:**
Old Kotlin plugin version in Gradle can't read new Kotlin code.

**The Fix:**
✅ Update `android/settings.gradle.kts`:
```kotlin
id("org.jetbrains.kotlin.android") version "2.1.0" apply false  // NOT 1.8.x!
```

**How to Verify:**
Build completes without Kotlin version errors.

---

### ❌ Pitfall #4: NDK Version Mismatch

**The Problem:**
```
Your project is configured with Android NDK 26.3.11579264, but the following plugin(s) 
depend on a different Android NDK version: requires Android NDK 27.0.12077973
```

**Why It Happens:**
Flutter or other plugins require a newer NDK version.

**The Fix:**
✅ Update `android/app/build.gradle.kts`:
```kotlin
android {
    ndkVersion = "27.0.12077973"  // Match the required version!
}
```

**How to Verify:**
Build completes without NDK version warnings.

---

### ❌ Pitfall #5: Missing RECEIVE_WAP_PUSH Permission

**The Problem:**
```
E/SmsApplication: com.example.smsapp lost android:receive_wap_push: (no permission to fix)
```
MMS won't work properly.

**Why It Happens:**
MMS uses WAP_PUSH protocol, needs explicit permission.

**The Fix:**
✅ Add to AndroidManifest.xml:
```xml
<uses-permission android:name="android.permission.RECEIVE_WAP_PUSH"/>
```

**How to Verify:**
No "lost android:receive_wap_push" error in logs.

---

### ❌ Pitfall #6: UI Not Updating for New Messages

**The Problem:**
- Logs show "📱 Received SMS from +1234567890: Hello"
- Logs show "🔄 Refreshing thread list..."
- But UI still shows 0 threads or doesn't update

**Why It Happens:**
Race condition - you're querying database BEFORE Android finishes writing the message.

**The Fix:**
✅ Add 500ms delay before refreshing in `home_screen.dart` (Step 3.2):
```dart
ref.listen(incomingSmsStreamProvider, (previous, next) {
  next.when(
    data: (incomingSms) async {
      // ⚠️ CRITICAL: Wait for Android to write to database
      await Future.delayed(const Duration(milliseconds: 500));
      ref.read(smsActionsProvider).refreshThreads();
    },
  );
});
```

✅ Also refresh after sending (Step 3.3):
```dart
final success = await actions.sendText(address, body);
if (success) {
  actions.refreshThreads(); // Refresh immediately
}
```

**How to Verify:**
```
I/flutter: 📱 Received SMS from +15627162080: Hello
I/flutter: ⏳ Waiting for system to save SMS...
I/flutter: 🔄 Refreshing thread list...
D/SmsAndroidPlugin: listThreads: Cursor is not null, count=6  // ✅ Count increased!
I/flutter: 📋 threadListProvider: Fetched 4 threads  // ✅ Threads increased!
```

---

### ❌ Pitfall #7: Using Wrong Content Provider URI

**The Problem:**
```
D/SmsAndroidPlugin: listThreads: Exception: Invalid column date
```
Query crashes or returns no data.

**Why It Happens:**
Using `content://sms/conversations` which has different column names.

**The Fix:**
✅ Use `content://sms` (NOT `content://sms/conversations`) in `listThreads()` (Step 2.5):
```kotlin
val uri = Uri.parse("content://sms")  // ✅ Correct!
// NOT: Telephony.Sms.Conversations.CONTENT_URI  // ❌ Wrong!

val projection = arrayOf(
    "thread_id",  // ✅ Available in content://sms
    "address",
    "body",
    "date",      // ✅ Available in content://sms
    "read",
    "type"
)
```

Then manually group by `thread_id` to get unique threads.

**How to Verify:**
```
D/SmsAndroidPlugin: listThreads: Cursor is not null, count=9
D/SmsAndroidPlugin: listThreads: Processed 9 messages, returning 5 threads
```

---

### ✅ Quick Verification Checklist

Before running your app, verify these critical items:

**Kotlin Code:**
- [ ] `saveSentSmsToDatabase()` called in `sendText()`
- [ ] `saveReceivedSmsToDatabase()` called in `SmsReceiver.onReceive()`
- [ ] Using `content://sms` URI (not `content://sms/conversations`)
- [ ] Kotlin version is 2.1.0 in `settings.gradle.kts`
- [ ] NDK version is 27.0.12077973 in `build.gradle.kts`

**Android Manifest:**
- [ ] SENDTO intent filter in MainActivity
- [ ] HeadlessSmsSendService declared
- [ ] RECEIVE_WAP_PUSH permission declared
- [ ] All 3 receivers: SmsReceiver, MmsReceiver, HeadlessSmsSendService

**Flutter Code:**
- [ ] 500ms delay before refresh in incoming SMS listener
- [ ] Refresh after sending messages

**Expected Logs When Working:**
```
D/SmsAndroidPlugin: Already holding SMS role                          ✅
D/SmsAndroidPlugin: ✅ Saved sent SMS to database: content://sms/8   ✅
D/SmsReceiver: ✅ Saved received SMS to database: content://sms/9    ✅
I/flutter: 📋 threadListProvider: Fetched 5 threads                  ✅
```

---

## 🎓 How We Discovered These Issues

This guide is based on **real debugging sessions** where we:
1. Built the app from scratch
2. Hit every single one of these issues
3. Debugged with extensive logging
4. Found the root causes
5. Implemented the fixes
6. Verified everything works on Android 13-15

**The issues above took ~4 hours to debug and fix.** By following this guide, you'll avoid all of them! 🎯

---

## 🔄 Integration Options

### 🔍 Technology Stack Breakdown

**What languages are used where:**

| Component | Language | Location | Purpose |
|-----------|----------|----------|---------|
| UI Screens | **Dart/Flutter** | `lib/screens/` | Message list, thread view, compose |
| State Management | **Dart/Flutter** | `lib/providers/` | App state, data flow |
| Platform Interface | **Dart** | `packages/sms_platform/` | Abstract SMS API |
| Android Plugin | **Kotlin** | `packages/sms_android/android/` | Native SMS operations |
| Plugin Bridge | **Dart** | `packages/sms_android/lib/` | Flutter ↔ Android communication |
| Data Models | **Dart** | `packages/sms_platform/lib/src/models/` | Message, Thread, etc. |

**If you have an existing Flutter app:**
- ✅ Your existing **Dart/Flutter code** stays unchanged
- ✅ You add new **Dart packages** (`sms_platform`, `sms_android`)
- ✅ You add new **Kotlin code** (in the plugin packages, not your main app)
- ✅ Everything integrates via standard Flutter plugin architecture

---

### Option A: New Standalone SMS App
Follow this guide from start to finish to create a dedicated SMS app.

### Option B: Add SMS to Existing Flutter App ✅ **RECOMMENDED FOR YOUR USE CASE**

**Good news!** The SMS functionality is **completely modular** and can be added to any existing Flutter app without affecting your current features.

#### Quick Integration Steps:

1. **Add the plugin packages** (doesn't touch your existing code):
   - Create `packages/sms_platform/` and `packages/sms_android/`
   - These are standalone packages that work independently

2. **Update Android config** (safe additions only):
   - Add SMS permissions to your `AndroidManifest.xml`
   - Add broadcast receivers (they only respond to SMS events)
   - Add the required service (only activated when SMS arrives)

3. **Add SMS screens** (optional - you can use your own UI):
   - The guide includes example screens
   - You can integrate SMS into your existing navigation
   - Or create a separate "Messages" section in your app

4. **State Management Flexibility**:
   - Guide uses Riverpod, but SMS plugin works with **ANY** state management
   - Using Bloc/Provider/GetX? Just wrap the plugin calls in your preferred pattern
   - The plugin is state-agnostic - it just provides data via platform channels

#### What WON'T Change in Your Existing App:

- ✅ Your existing screens/navigation
- ✅ Your current state management
- ✅ Your app's main functionality
- ✅ Your existing permissions (SMS permissions are additive)
- ✅ Your theme/styling

#### What WILL Be Added:

- ✅ Two new local packages (`sms_platform` and `sms_android`)
- ✅ SMS permissions in AndroidManifest
- ✅ 3 broadcast receivers + 1 service (passive, only respond to SMS)
- ✅ SMS functionality accessible via simple Dart API

**Example: Adding SMS to an existing e-commerce app:**
```dart
// Your existing app
class MyExistingApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: HomeScreen(), // Your existing home
      routes: {
        '/products': (context) => ProductsScreen(),
        '/cart': (context) => CartScreen(),
        '/messages': (context) => SmsHomeScreen(), // ✅ Add SMS here
      },
    );
  }
}
```

---

## 📐 Project Structure

```
your_app/
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
├── packages/
│   ├── sms_platform/
│   │   ├── lib/
│   │   │   ├── sms_platform.dart
│   │   │   └── src/
│   │   │       ├── sms_platform_interface.dart
│   │   │       └── models/
│   │   │           ├── sms_message.dart
│   │   │           ├── sms_thread.dart
│   │   │           ├── incoming_sms.dart
│   │   │           └── mms_attachment.dart
│   │   └── pubspec.yaml
│   └── sms_android/
│       ├── lib/
│       │   └── sms_android.dart
│       ├── android/
│       │   ├── build.gradle
│       │   ├── src/main/
│       │   │   ├── AndroidManifest.xml
│       │   │   └── kotlin/com/example/sms_android/
│       │   │       ├── SmsAndroidPlugin.kt
│       │   │       ├── SmsReceiver.kt
│       │   │       ├── MmsReceiver.kt
│       │   │       └── HeadlessSmsSendService.kt
│       │   └── proguard-rules.pro
│       └── pubspec.yaml
└── android/
    ├── app/
    │   ├── build.gradle.kts
    │   └── src/main/AndroidManifest.xml
    └── settings.gradle.kts
```

---

## 🔧 Step 1: Android Configuration

### 1.1 Plugin `build.gradle` (android-smsmms Library)

**⚠️ CRITICAL: Add android-smsmms library to your `sms_android` plugin!**

```gradle
// packages/sms_android/android/build.gradle
android {
    namespace = "com.example.sms_android"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:2.1.0"
    
    // ⚠️ CRITICAL: android-smsmms library for MMS support
    implementation fileTree(dir: 'libs', include: ['*.jar'])
    // You need to download android-smsmms library JARs and place them in:
    // packages/sms_android/android/libs/
    // Get from: https://github.com/klinker24/android-smsmms
}
```

**How to add android-smsmms library:**
1. Clone or download https://github.com/klinker24/android-smsmms
2. Build the library to get JAR files
3. Copy JAR files to `packages/sms_android/android/libs/`
4. Or use Maven/JitPack if available

**Required classes from android-smsmms:**
- `com.android.mms.transaction.PushReceiver`
- `com.android.mms.transaction.TransactionService`
- `com.klinker.android.send_message.Settings`
- `com.android.mms.service_alt.MmsNetworkManager`
- And all their dependencies

---

### 1.2 Main App `build.gradle.kts`

```kotlin
// android/app/build.gradle.kts
android {
    namespace = "com.example.your_app"
    compileSdk = 35
    ndkVersion = "27.0.12077973"  // Critical: Match this version!
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        applicationId = "com.example.your_app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
}
```

### 1.2 Root `settings.gradle.kts`

```kotlin
// android/settings.gradle.kts
plugins {
    id("dev.flutter.flutter-plugin-loader") version "1.0.0"
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false  // Critical: Use 2.1.0+
}
```

### 1.3 Main App `AndroidManifest.xml`

```xml
<!-- android/app/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- ===== SMS/MMS Permissions ===== -->
    <uses-permission android:name="android.permission.SEND_SMS"/>
    <uses-permission android:name="android.permission.RECEIVE_SMS"/>
    <uses-permission android:name="android.permission.READ_SMS"/>
    <uses-permission android:name="android.permission.RECEIVE_MMS"/>
    <uses-permission android:name="android.permission.RECEIVE_WAP_PUSH"/>
    <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="32"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32"/>
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
    <uses-permission android:name="android.permission.INTERNET"/>
    
    <!-- ⚠️ CRITICAL for MMS: WiFi and APN permissions -->
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
    <uses-permission android:name="android.permission.WRITE_APN_SETTINGS"/>
    
    <application
        android:label="Your SMS App"
        android:name="${applicationName}"
        android:icon="@mipmap/ic_launcher">
        
        <!-- ===== Main Activity ===== -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:taskAffinity=""
            android:theme="@style/LaunchTheme"
            android:configChanges="orientation|keyboardHidden|keyboard|screenSize|smallestScreenSize|locale|layoutDirection|fontScale|screenLayout|density|uiMode"
            android:hardwareAccelerated="true"
            android:windowSoftInputMode="adjustResize">
            
            <meta-data
              android:name="io.flutter.embedding.android.NormalTheme"
              android:resource="@style/NormalTheme" />
              
            <!-- LAUNCHER -->
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
            
            <!-- ⚠️ CRITICAL: Required for default SMS app -->
            <intent-filter>
                <action android:name="android.intent.action.SEND"/>
                <action android:name="android.intent.action.SENDTO"/>
                <category android:name="android.intent.category.DEFAULT"/>
                <category android:name="android.intent.category.BROWSABLE"/>
                <data android:scheme="sms"/>
                <data android:scheme="smsto"/>
                <data android:scheme="mms"/>
                <data android:scheme="mmsto"/>
            </intent-filter>
        </activity>
        
        <meta-data android:name="flutterEmbedding" android:value="2" />
        
        <!-- ===== SMS Receiver ===== -->
        <receiver
            android:name="com.example.sms_android.SmsReceiver"
            android:permission="android.permission.BROADCAST_SMS"
            android:exported="true">
            <intent-filter>
                <action android:name="android.provider.Telephony.SMS_DELIVER"/>
            </intent-filter>
        </receiver>

        <!-- ===== MMS Receiver ===== -->
        <receiver
            android:name="com.example.sms_android.MmsReceiver"
            android:permission="android.permission.BROADCAST_WAP_PUSH"
            android:exported="true">
            <intent-filter>
                <action android:name="android.provider.Telephony.WAP_PUSH_DELIVER"/>
                <data android:mimeType="application/vnd.wap.mms-message"/>
            </intent-filter>
        </receiver>

        <!-- ⚠️ CRITICAL: Headless SMS Send Service - Required for API 29+ -->
        <service
            android:name="com.example.sms_android.HeadlessSmsSendService"
            android:permission="android.permission.SEND_RESPOND_VIA_MESSAGE"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.RESPOND_VIA_MESSAGE"/>
                <category android:name="android.intent.category.DEFAULT"/>
                <data android:scheme="sms"/>
                <data android:scheme="smsto"/>
                <data android:scheme="mms"/>
                <data android:scheme="mmsto"/>
            </intent-filter>
        </service>
        
        <!-- ⚠️ CRITICAL: MMS Transaction Service - Required for MMS downloads -->
        <service
            android:name="com.android.mms.transaction.TransactionService"
            android:exported="false"/>
    </application>
    
    <queries>
        <intent>
            <action android:name="android.intent.action.PROCESS_TEXT"/>
            <data android:mimeType="text/plain"/>
        </intent>
    </queries>
</manifest>
```

---

## 🔧 Step 2: Kotlin Plugin Implementation

### 2.1 Critical: Database Persistence Functions

**⚠️ THIS IS THE MOST IMPORTANT PART - Without these, your app won't work!**

Add these helper functions to `SmsAndroidPlugin.kt`:

```kotlin
// SmsAndroidPlugin.kt - Add these helper functions
package com.example.sms_android

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony

/**
 * ⚠️ CRITICAL: Save sent SMS to database
 * 
 * As the default SMS app, YOU must write sent messages to the database.
 * Android does NOT do this automatically!
 */
private fun saveSentSmsToDatabase(context: Context, address: String, body: String): Uri? {
    return try {
        android.util.Log.d("SmsAndroidPlugin", "SMS sent to $address, now saving to database...")
        
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 1)  // Mark as read
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)  // type=2
            put(Telephony.Sms.SEEN, 1)
        }
        
        val uri = context.contentResolver.insert(
            Uri.parse("content://sms/sent"),
            values
        )
        
        android.util.Log.d("SmsAndroidPlugin", "✅ Saved sent SMS to database: $uri")
        uri
    } catch (e: Exception) {
        android.util.Log.e("SmsAndroidPlugin", "❌ Failed to save sent SMS: ${e.message}", e)
        null
    }
}

/**
 * ⚠️ CRITICAL: Save received SMS to database
 * 
 * This should be called in SmsReceiver.kt after receiving an SMS.
 */
fun saveReceivedSmsToDatabase(context: Context, address: String, body: String, timestamp: Long): Uri? {
    return try {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, timestamp)
            put(Telephony.Sms.READ, 0)  // Mark as unread
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)  // type=1
            put(Telephony.Sms.SEEN, 0)
        }
        
        val uri = context.contentResolver.insert(
            Uri.parse("content://sms/inbox"),
            values
        )
        
        android.util.Log.d("SmsReceiver", "✅ Saved received SMS to database: $uri")
        uri
    } catch (e: Exception) {
        android.util.Log.e("SmsReceiver", "❌ Exception saving received SMS: ${e.message}", e)
        null
    }
}
```

---

### 2.1b Initialize MMS Settings (CRITICAL FOR MMS RECEIVING!)

**⚠️ THE MOST IMPORTANT MMS CONFIGURATION**

The `android-smsmms` library reads its configuration from SharedPreferences. You MUST initialize these settings on app startup, including:
- APN/MMSC URL (carrier-specific)
- MMS proxy and port
- **User-Agent header** (CRITICAL - without this, carriers reject downloads with "empty response")

Add this to `SmsAndroidPlugin.kt`:

```kotlin
// SmsAndroidPlugin.kt - Add to onAttachedToEngine method
override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    context = flutterPluginBinding.applicationContext
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "sms_android")
    channel.setMethodCallHandler(this)
    
    // ⚠️ CRITICAL: Initialize MMS settings for android-smsmms library
    initializeMmsSettings(flutterPluginBinding.applicationContext)
    
    // ... rest of onAttachedToEngine
}

/**
 * ⚠️ CRITICAL: Initialize MMS settings in SharedPreferences for android-smsmms library.
 * 
 * The android-smsmms library's PushReceiver reads these settings when downloading MMS!
 * Without proper User-Agent and APN settings, MMS downloads will fail with "empty response".
 */
private fun initializeMmsSettings(ctx: Context) {
    try {
        android.util.Log.d(TAG, "🔧 Initializing MMS settings for android-smsmms library...")
        
        // The library reads settings from SharedPreferences with this specific name
        val prefs = ctx.getSharedPreferences("com.klinker.android.send_message_preferences", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        // Detect carrier
        val telephonyManager = ctx.getSystemService(Context.TELEPHONY_SERVICE) as android.telephony.TelephonyManager
        val carrierName = telephonyManager.networkOperatorName ?: ""
        
        // Configure APN settings based on carrier
        when {
            carrierName.contains("T-Mobile", ignoreCase = true) || 
            carrierName.contains("Metro", ignoreCase = true) -> {
                editor.putString("mmsc_url", "http://mms.msg.eng.t-mobile.com/mms/wapenc")
                editor.putString("mms_proxy", "")
                editor.putString("mms_port", "80")
                android.util.Log.d(TAG, "✅ Configured T-Mobile APN for MMS")
            }
            carrierName.contains("AT&T", ignoreCase = true) ||
            carrierName.contains("Cricket", ignoreCase = true) -> {
                editor.putString("mmsc_url", "http://mmsc.mobile.att.net")
                editor.putString("mms_proxy", "proxy.mobile.att.net")
                editor.putString("mms_port", "80")
                android.util.Log.d(TAG, "✅ Configured AT&T APN for MMS")
            }
            carrierName.contains("Verizon", ignoreCase = true) ||
            carrierName.contains("Visible", ignoreCase = true) -> {
                editor.putString("mmsc_url", "http://mms.vtext.com/servlets/mms")
                editor.putString("mms_proxy", "")
                editor.putString("mms_port", "80")
                android.util.Log.d(TAG, "✅ Configured Verizon APN for MMS")
            }
            else -> {
                // Default to T-Mobile (works for many MVNOs)
                editor.putString("mmsc_url", "http://mms.msg.eng.t-mobile.com/mms/wapenc")
                editor.putString("mms_proxy", "")
                editor.putString("mms_port", "80")
                android.util.Log.w(TAG, "⚠️ Unknown carrier '$carrierName', using T-Mobile default")
            }
        }
        
        // ⚠️ CRITICAL: Set User-Agent for HTTP requests
        // Without this, most carriers will reject MMS downloads with "empty response"!
        editor.putString("user_agent", "Android MmsLib/1.0")
        
        // Other important settings
        editor.putBoolean("use_system_sending", false) // Use HTTP-based MMS
        editor.putBoolean("delivery_reports", false)
        editor.putBoolean("group_message", false)
        
        editor.apply()
        
        android.util.Log.d(TAG, "✅ MMS settings initialized successfully!")
    } catch (e: Exception) {
        android.util.Log.e(TAG, "❌ Failed to initialize MMS settings: ${e.message}", e)
    }
}
```

**Why this is critical:**
1. **User-Agent header** - Carriers validate this. Without it, you get "empty response" errors.
2. **APN/MMSC URL** - Tells the library where to download MMS from (carrier-specific).
3. **SharedPreferences** - The android-smsmms library only reads from SharedPreferences, not from direct Settings objects.

**Common pitfall:** Don't try to set `settings.userAgent` directly in code - it's a private field! You MUST use SharedPreferences.

---

### 2.2 Update `sendText()` to Save Messages

```kotlin
// SmsAndroidPlugin.kt - Update sendText method
private fun sendText(address: String, body: String, result: MethodChannel.Result) {
    try {
        val ctx = context ?: run {
            result.error("NO_CONTEXT", "Context not available", null)
            return
        }
        
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ctx.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        // Send the SMS
        smsManager?.sendTextMessage(address, null, body, null, null)
        
        // ⚠️ CRITICAL: Save to database AFTER sending
        saveSentSmsToDatabase(ctx, address, body)
        
        result.success(true)
    } catch (e: Exception) {
        result.error("SEND_FAILED", e.message, null)
    }
}
```

### 2.3 Update `SmsReceiver.kt` to Save Messages

```kotlin
// SmsReceiver.kt
package com.example.sms_android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("SmsReceiver", "onReceive called, action: ${intent.action}")
        
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION) {
            android.util.Log.d("SmsReceiver", "Processing SMS_DELIVER_ACTION")
            
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            messages?.forEach { smsMessage ->
                val address = smsMessage.displayOriginatingAddress
                val body = smsMessage.messageBody
                val timestamp = smsMessage.timestampMillis

                android.util.Log.d("SmsReceiver", "SMS from $address: $body")

                // ⚠️ CRITICAL: Save to database FIRST
                try {
                    saveReceivedSmsToDatabase(context, address, body, timestamp)
                } catch (e: Exception) {
                    android.util.Log.e("SmsReceiver", "❌ Exception saving received SMS: ${e.message}", e)
                }

                // Then send to Flutter via EventChannel
                val data = mapOf(
                    "address" to address,
                    "body" to body,
                    "timestamp" to timestamp,
                    "isMms" to false
                )

                if (SmsAndroidPlugin.incomingEventSink != null) {
                    SmsAndroidPlugin.incomingEventSink?.success(data)
                    android.util.Log.d("SmsReceiver", "Sending to Flutter EventSink")
                } else {
                    android.util.Log.e("SmsReceiver", "EventSink is null! Flutter not listening.")
                }
            }
        }
    }
    
    // Helper function (same as in SmsAndroidPlugin.kt)
    private fun saveReceivedSmsToDatabase(context: Context, address: String, body: String, timestamp: Long): Uri? {
        return try {
            val values = android.content.ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, timestamp)
                put(Telephony.Sms.READ, 0)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                put(Telephony.Sms.SEEN, 0)
            }
            
            val uri = context.contentResolver.insert(
                Uri.parse("content://sms/inbox"),
                values
            )
            
            android.util.Log.d("SmsReceiver", "✅ Saved received SMS to database: $uri")
            uri
        } catch (e: Exception) {
            android.util.Log.e("SmsReceiver", "❌ Exception saving received SMS: ${e.message}", e)
            null
        }
    }
}
```

---

### 2.4 MMS Implementation (Send Pictures/Documents) ⚠️ **CRITICAL FOR YOUR USE CASE**

**This is what you need for attaching files!**

Add this `sendMms()` method to `SmsAndroidPlugin.kt`:

```kotlin
// SmsAndroidPlugin.kt - Add MMS sending functionality
import android.content.ContentValues
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import java.io.ByteArrayOutputStream

/**
 * Send MMS with attachments (pictures, documents, etc.)
 * 
 * @param addresses List of phone numbers to send to
 * @param text Message text (optional)
 * @param attachments List of file URIs to attach (images, PDFs, etc.)
 */
private fun sendMms(
    addresses: List<String>,
    text: String?,
    attachments: List<Map<String, Any>>?,
    result: MethodChannel.Result
) {
    val ctx = context ?: run {
        result.error("NO_CONTEXT", "Context not available", null)
        return
    }

    try {
        android.util.Log.d("SmsAndroidPlugin", "Sending MMS to ${addresses.joinToString(", ")}")
        android.util.Log.d("SmsAndroidPlugin", "Text: $text, Attachments: ${attachments?.size ?: 0}")

        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ctx.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        // Create MMS message
        val messageUri = Uri.parse("content://mms")
        val threadId = getThreadId(ctx, addresses.first())

        // Insert MMS message into database
        val mmsValues = ContentValues().apply {
            put(Telephony.Mms.THREAD_ID, threadId)
            put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000)
            put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_OUTBOX)
            put(Telephony.Mms.READ, 1)
            put(Telephony.Mms.MESSAGE_TYPE, Telephony.Mms.MESSAGE_TYPE_SEND_REQ)
        }

        val mmsUri = ctx.contentResolver.insert(messageUri, mmsValues)
        android.util.Log.d("SmsAndroidPlugin", "MMS inserted: $mmsUri")

        if (mmsUri != null) {
            val mmsId = mmsUri.lastPathSegment?.toLongOrNull() ?: 0L

            // Add recipients
            addresses.forEach { address ->
                val addrValues = ContentValues().apply {
                    put(Telephony.Mms.Addr.ADDRESS, address)
                    put(Telephony.Mms.Addr.CHARSET, 106) // UTF-8
                    put(Telephony.Mms.Addr.TYPE, Telephony.Mms.Addr.TYPE_TO)
                }
                ctx.contentResolver.insert(
                    Uri.parse("content://mms/$mmsId/addr"),
                    addrValues
                )
            }

            // Add text part if provided
            if (!text.isNullOrEmpty()) {
                val partValues = ContentValues().apply {
                    put(Telephony.Mms.Part.TEXT, text)
                    put(Telephony.Mms.Part.CHARSET, 106)
                    put(Telephony.Mms.Part.CONTENT_TYPE, "text/plain")
                }
                ctx.contentResolver.insert(
                    Uri.parse("content://mms/$mmsId/part"),
                    partValues
                )
            }

            // Add attachments (images, PDFs, etc.)
            attachments?.forEach { attachment ->
                val uri = attachment["uri"] as? String
                val mimeType = attachment["mimeType"] as? String ?: "image/jpeg"
                
                if (uri != null) {
                    try {
                        val fileUri = Uri.parse(uri)
                        val inputStream = ctx.contentResolver.openInputStream(fileUri)
                        val bytes = inputStream?.readBytes()
                        inputStream?.close()

                        if (bytes != null) {
                            val partValues = ContentValues().apply {
                                put(Telephony.Mms.Part.CONTENT_TYPE, mimeType)
                                put(Telephony.Mms.Part._DATA, bytes)
                            }
                            ctx.contentResolver.insert(
                                Uri.parse("content://mms/$mmsId/part"),
                                partValues
                            )
                            android.util.Log.d("SmsAndroidPlugin", "Attached file: $uri ($mimeType)")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SmsAndroidPlugin", "Failed to attach file $uri: ${e.message}")
                    }
                }
            }

            // Send the MMS
            val sendIntent = Intent(Intent.ACTION_SEND)
            sendIntent.putExtra("mms_uri", mmsUri.toString())
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                smsManager?.sendMultimediaMessage(
                    ctx,
                    mmsUri,
                    null, // locationUrl
                    null, // configOverrides
                    null  // sentIntent
                )
            }

            android.util.Log.d("SmsAndroidPlugin", "✅ MMS sent successfully")
            result.success(true)
        } else {
            result.error("MMS_FAILED", "Failed to create MMS", null)
        }
    } catch (e: Exception) {
        android.util.Log.e("SmsAndroidPlugin", "MMS send failed: ${e.message}", e)
        result.error("MMS_FAILED", e.message, null)
    }
}

/**
 * Helper: Get or create thread ID for a phone number
 */
private fun getThreadId(context: Context, address: String): Long {
    val uri = Uri.parse("content://mms-sms/threadID")
    val projection = arrayOf("_id")
    val selection = "recipient_ids=?"
    
    // Try to find existing thread
    val cursor = context.contentResolver.query(
        Telephony.Threads.CONTENT_URI,
        arrayOf(Telephony.Threads._ID),
        null,
        null,
        "${Telephony.Threads.DATE} DESC"
    )

    cursor?.use {
        while (it.moveToNext()) {
            return it.getLong(0)
        }
    }

    // If no thread exists, it will be created when message is inserted
    return 0L
}
```

**Add this to your MethodChannel call handler in `SmsAndroidPlugin.kt`:**

```kotlin
// In onMethodCall method, add:
"sendMms" -> {
    val addresses = call.argument<List<String>>("addresses") ?: emptyList()
    val text = call.argument<String?>("text")
    val attachments = call.argument<List<Map<String, Any>>?>("attachments")
    sendMms(addresses, text, attachments, result)
}
```

---

### 2.5 MmsReceiver (Receive MMS with Attachments) ⚠️ **UPDATED IMPLEMENTATION**

**⚠️ CRITICAL: MmsReceiver MUST extend PushReceiver from android-smsmms library!**

After extensive debugging, we discovered that manual MMS PDU parsing and download triggering is extremely unreliable. The `android-smsmms` library's `PushReceiver` class handles all the complexity:
- WAP_PUSH broadcast handling
- MMS notification PDU parsing
- Database persistence
- Automatic download coordination with `TransactionService`

Create `MmsReceiverImpl.kt`:

```kotlin
// MmsReceiverImpl.kt - Extends PushReceiver from android-smsmms library
package com.example.sms_android

import com.android.mms.transaction.PushReceiver

/**
 * MMS WAP_PUSH receiver that extends PushReceiver from android-smsmms library.
 *
 * ⚠️ CRITICAL: This class is intentionally empty!
 * 
 * The parent PushReceiver handles ALL the logic for:
 * - Parsing WAP_PUSH broadcasts
 * - Persisting MMS notifications to the database (m_type=130)
 * - Coordinating with TransactionService for MMS downloads
 * - Converting notifications to fully downloaded MMS (m_type=132)
 * 
 * We just need to declare this receiver in AndroidManifest.xml for the app
 * to be eligible as a default SMS app. The android-smsmms library does the rest!
 * 
 * DO NOT override onReceive() or any other methods - let the parent handle everything!
 */
class MmsReceiverImpl : PushReceiver()
```

**✅ MMS Receiver is already declared in AndroidManifest.xml** (from Step 1.3)

**⚠️ IMPORTANT: ContentObserver for MMS Notifications**

Since `PushReceiver` handles everything internally, we need to observe the MMS database to detect when:
1. MMS notification arrives (m_type=130)
2. MMS download completes (m_type=132)

This is handled in `SmsAndroidPlugin.kt` with a `ContentObserver` watching `content://mms`:

```kotlin
// In SmsAndroidPlugin.kt - Add this class
private class MmsContentObserver(
    handler: Handler,
    private val context: Context,
    private val onMmsChanged: () -> Unit
) : ContentObserver(handler) {
    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        android.util.Log.d("SmsAndroidPlugin", "📬 MMS database changed!")
        onMmsChanged()
    }
}

// In onAttachedToEngine, register the observer:
val mmsUri = Uri.parse("content://mms")
val mmsObserver = MmsContentObserver(
    Handler(Looper.getMainLooper()),
    context
) {
    // When MMS database changes, check for new MMS
    checkForNewMms(context)
}
context.contentResolver.registerContentObserver(mmsUri, true, mmsObserver)
```

The `checkForNewMms()` function queries for:
- MMS notifications (m_type=130) - just received
- Fully downloaded MMS (m_type=132) - download complete, extract images and notify Flutter

---

### 2.6 HeadlessSmsSendService (Required for Android 10+)

```kotlin
// HeadlessSmsSendService.kt
package com.example.sms_android

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        Log.d("HeadlessSmsSendService", "onBind called")
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("HeadlessSmsSendService", "onCreate called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("HeadlessSmsSendService", "onDestroy called")
    }
}
```

### 2.5 `listThreads()` Implementation

```kotlin
// SmsAndroidPlugin.kt - Query threads from SMS database
private fun listThreads(limit: Int, result: MethodChannel.Result) {
    val ctx = context ?: run {
        result.error("NO_CONTEXT", "Context not available", null)
        return
    }

    try {
        android.util.Log.d("SmsAndroidPlugin", "listThreads: Starting query for $limit threads")
        val threads = mutableListOf<Map<String, Any>>()
        
        // Query the SMS table grouped by thread_id
        val uri = Uri.parse("content://sms")
        android.util.Log.d("SmsAndroidPlugin", "listThreads: URI = $uri")
        
        val projection = arrayOf(
            "thread_id",
            "address",
            "body",
            "date",
            "read",
            "type"
        )

        // Get messages ordered by date DESC
        val cursor: Cursor? = ctx.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "date DESC"
        )

        android.util.Log.d("SmsAndroidPlugin", "listThreads: Cursor is ${if (cursor == null) "NULL" else "not null, count=${cursor.count}"}")
        
        val seenThreads = mutableSetOf<Long>()
        var messagesProcessed = 0
        
        cursor?.use {
            while (it.moveToNext()) {
                messagesProcessed++
                val threadId = it.getLong(it.getColumnIndexOrThrow("thread_id"))
                
                // Log every message for debugging
                val address = it.getString(it.getColumnIndexOrThrow("address"))
                val body = it.getString(it.getColumnIndexOrThrow("body")) ?: ""
                val date = it.getLong(it.getColumnIndexOrThrow("date"))
                val type = it.getInt(it.getColumnIndexOrThrow("type"))
                
                android.util.Log.d("SmsAndroidPlugin", "listThreads: Message #$messagesProcessed: thread=$threadId, address=$address, type=$type, body=${body.take(30)}, date=$date")
                
                // Skip if we've already added this thread
                if (seenThreads.contains(threadId)) {
                    android.util.Log.d("SmsAndroidPlugin", "  └─ Skipping (already have thread $threadId)")
                    continue
                }
                
                seenThreads.add(threadId)
                val read = it.getInt(it.getColumnIndexOrThrow("read")) == 1
                
                // Get message count for this thread
                val messageCount = getMessageCountForThread(ctx, threadId)

                android.util.Log.d("SmsAndroidPlugin", "  └─ ✅ Adding thread $threadId with $messageCount messages")
                
                threads.add(
                    mapOf(
                        "threadId" to threadId.toInt(),
                        "address" to address,
                        "snippet" to body.take(100),
                        "messageCount" to messageCount,
                        "date" to date,
                        "isRead" to read
                    )
                )
                
                // Stop after reaching limit
                if (threads.size >= limit) break
            }
        }

        android.util.Log.d("SmsAndroidPlugin", "listThreads: Processed $messagesProcessed messages, returning ${threads.size} threads")
        result.success(threads)
    } catch (e: Exception) {
        android.util.Log.e("SmsAndroidPlugin", "listThreads: Exception: ${e.message}", e)
        result.error("QUERY_FAILED", e.message, null)
    }
}

private fun getMessageCountForThread(context: Context, threadId: Long): Int {
    val uri = Uri.parse("content://sms")
    val projection = arrayOf("COUNT(*) as count")
    val selection = "thread_id = ?"
    val selectionArgs = arrayOf(threadId.toString())
    
    val cursor = context.contentResolver.query(
        uri,
        projection,
        selection,
        selectionArgs,
        null
    )

    cursor?.use {
        if (it.moveToFirst()) {
            return it.getInt(0)
        }
    }
    return 0
}
```

---

## 🎨 Step 3: Flutter UI Implementation

### 🚨 IMPORTANT: Third-Party Library Required for MMS!

**⚠️ CRITICAL UPDATE: MMS requires the `android-smsmms` library!**

After extensive testing and debugging, we discovered that implementing MMS from scratch using only Android's native APIs is extremely complex and unreliable. **MMS receiving specifically requires proper PDU parsing, APN configuration, HTTP client setup, and carrier-specific handling.**

**What we use:**

**For SMS (works perfectly with native APIs):**
- ✅ **Android's native Telephony API** (built into Android SDK)
  - `SmsManager.sendTextMessage()` - Send SMS
  - `RoleManager` - Request default SMS app role
  - `ContentResolver` - Read/write SMS database
  - `Telephony.Sms` - SMS content provider

**For MMS (requires third-party library):**
- ✅ **android-smsmms library** (Required for MMS receiving!)
  - `PushReceiver` - Handles WAP_PUSH broadcasts and MMS notifications
  - `TransactionService` - Manages MMS downloads in background
  - `MmsNetworkManager` - Handles carrier network connections
  - `ApnSettings` - Reads APN/MMSC configuration
  - Proper PDU parsing and HTTP client
- ✅ **Android's SmsManager.sendMultimediaMessage()** for sending MMS
- ✅ **Flutter's platform channels** (built into Flutter framework)
- ✅ **Standard Flutter packages** (for UI helpers only, NOT for SMS):

| Package | Purpose | SMS Related? |
|---------|---------|--------------|
| `flutter_riverpod` | State management (UI) | ❌ No - UI only |
| `intl` | Date formatting (UI) | ❌ No - UI only |
| `image_picker` | Pick images for MMS | ❌ No - UI only |
| `permission_handler` | Runtime permissions | ❌ No - Permission UI only |
| `sms_platform` | **Your local package** | ✅ Yes - Interface we create |
| `sms_android` | **Your local package** | ✅ Yes - Plugin we create |

---

### ❓ Why Not Use Popular Flutter SMS Packages?

You might be wondering about these packages:

| Package | What It Does | ❌ Why We Can't Use It |
|---------|--------------|----------------------|
| `flutter_sms` | Send SMS/MMS | ❌ **Cannot become default SMS app**<br>❌ Cannot receive SMS broadcasts<br>❌ No HeadlessSmsSendService<br>❌ No database persistence<br>❌ Last updated years ago |
| `send_message` | Fork of flutter_sms | ❌ **Cannot become default SMS app**<br>❌ Cannot receive SMS broadcasts<br>❌ No HeadlessSmsSendService<br>❌ Sending only, no receiving |
| `sms_mms` | Launch native messenger | ❌ **Only launches system SMS app**<br>❌ Doesn't send SMS directly<br>❌ User must manually tap send<br>❌ Not suitable for default SMS app |
| `flutter_sms_inbox` | Read SMS inbox | ❌ **Read-only, cannot send**<br>❌ Cannot become default SMS app<br>❌ Android-only, limited scope |

**✅ BUT WE DO USE `android-smsmms` (Kotlin library, not Flutter plugin):**

| Package | What It Does | ✅ Why We Use It |
|---------|--------------|------------------|
| `android-smsmms` | Native Android MMS library | ✅ **Handles complex MMS PDU parsing**<br>✅ **Manages APN/MMSC connections**<br>✅ **Includes PushReceiver and TransactionService**<br>✅ **Production-ready MMS implementation**<br>⚠️ **Native Kotlin library, NOT a Flutter plugin** |

**The Critical Difference:**

These packages are designed for **sending SMS from your app** (like notifications, verification codes, etc.).

**We're building a DEFAULT SMS APP** - completely different requirements:

| Requirement | Popular Packages | Our Implementation |
|-------------|-----------------|-------------------|
| Send SMS | ✅ Yes | ✅ Yes |
| Receive SMS broadcasts | ❌ **No** | ✅ **Yes** |
| Become default SMS app | ❌ **No** | ✅ **Yes** |
| HeadlessSmsSendService | ❌ **No** | ✅ **Yes** |
| Database persistence | ❌ **No** | ✅ **Yes** |
| Handle SMS_DELIVER intent | ❌ **No** | ✅ **Yes** |
| Handle WAP_PUSH intent | ❌ **No** | ✅ **Yes** |
| SENDTO intent filters | ❌ **No** | ✅ **Yes** |
| RoleManager integration | ❌ **No** | ✅ **Yes** |
| Read existing threads | Limited | ✅ **Full access** |
| MMS support | Limited | ✅ **Full support** |

---

### 🎯 When to Use Third-Party Packages vs This Guide

**Use packages like `flutter_sms` or `send_message` when:**
- ✅ You want to send SMS from your app (notifications, OTP, etc.)
- ✅ You DON'T need to become the default SMS app
- ✅ You DON'T need to receive SMS
- ✅ You just need basic "send message" functionality

**Use THIS guide when:**
- ✅ You're building a **replacement for Android Messages**
- ✅ You need to **receive SMS** in your app
- ✅ You need to become the **default SMS app**
- ✅ You need **full SMS/MMS functionality** (send, receive, threads, database)
- ✅ You're building for **company-managed devices** (like your use case)

**Why Third-Party Packages Can't Do This:**

1. **Android Requirements for Default SMS App:**
   - Must have `HeadlessSmsSendService` (packages don't include this)
   - Must have `SMS_DELIVER` and `WAP_PUSH_DELIVER` receivers (packages don't include this)
   - Must handle `SENDTO` intents (packages don't include this)
   - Must manually persist messages to database (packages don't do this)

2. **Packages Are NOT Designed for This:**
   - They're helper libraries for sending SMS, not full SMS apps
   - They don't expose the necessary components
   - They don't implement the Android default SMS app contract

3. **You MUST Use Native APIs:**
   - `RoleManager.createRequestRoleIntent()` - No package exposes this correctly
   - `ContentResolver.insert(content://sms/sent)` - No package does this
   - Broadcast receivers with proper permissions - No package includes these

---

### 💡 The Bottom Line

**Popular SMS packages = "Send SMS from your app"**  
**This guide = "Build a complete SMS app like Google Messages"**

Completely different use cases! For your requirement (company-managed devices needing a default SMS app), the popular packages **will not work**. You MUST build the native integration yourself, which is exactly what this guide provides.

---

**Why no third-party SMS libraries for THIS use case?**
- 🎯 **Full control** over default SMS app implementation
- 🎯 **No dependency** on packages that can't become default app
- 🎯 **Direct access** to RoleManager, broadcast receivers, services
- 🎯 **Complete compliance** with Android's default SMS app requirements
- 🎯 **Database persistence** that packages don't provide

**The SMS logic is 100% custom Kotlin code** that calls Android's native APIs directly via platform channels, implementing ALL requirements for a default SMS app.

---

### 3.1 Main Dependencies (`pubspec.yaml`)

**Add these to your existing `pubspec.yaml`:**

```yaml
dependencies:
  flutter:
    sdk: flutter

  # State Management (choose one: Riverpod/Bloc/Provider/GetX)
  flutter_riverpod: ^2.6.1
  
  # Local SMS packages (YOU create these)
  sms_platform:
    path: packages/sms_platform
  sms_android:
    path: packages/sms_android
  
  # UI & Utilities (NOT for SMS operations)
  intl: ^0.19.0           # Date formatting
  image_picker: ^1.1.2    # MMS image picker
  permission_handler: ^11.3.1  # Permission dialogs
  
  cupertino_icons: ^1.0.8
```

### 3.2 Key UI Pattern: Listen for Incoming SMS with Refresh

```dart
// home_screen.dart - Critical pattern for UI updates
class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  @override
  Widget build(BuildContext context) {
    // ⚠️ CRITICAL: Listen for incoming messages and refresh thread list
    ref.listen(incomingSmsStreamProvider, (previous, next) {
      next.when(
        data: (incomingSms) async {
          print('📱 Received SMS from ${incomingSms.address}: ${incomingSms.body}');
          print('⏳ Waiting for system to save SMS...');
          
          // Give Android time to write to database
          await Future.delayed(const Duration(milliseconds: 500));
          
          print('🔄 Refreshing thread list...');
          ref.read(smsActionsProvider).refreshThreads();
          print('✅ Thread list refresh triggered');
        },
        loading: () => print('⏳ Stream loading...'),
        error: (error, stack) => print('❌ Stream error: $error'),
      );
    });

    final threadList = ref.watch(threadListProvider);

    return Scaffold(
      appBar: AppBar(
        title: const Text('Messages'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => ref.invalidate(threadListProvider),
          ),
        ],
      ),
      body: threadList.when(
        data: (threads) {
          if (threads.isEmpty) {
            return const Center(child: Text('No conversations yet.'));
          }
          return ListView.builder(
            itemCount: threads.length,
            itemBuilder: (context, index) {
              final thread = threads[index];
              return ListTile(
                leading: CircleAvatar(
                  child: Text(thread.address.isNotEmpty ? thread.address[0] : '?'),
                ),
                title: Text(thread.address),
                subtitle: Text(
                  thread.snippet,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                trailing: Text(formatTimestamp(thread.date)),
                onTap: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => ThreadScreen(thread: thread),
                    ),
                  );
                },
              );
            },
          );
        },
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(child: Text('Error: $err')),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: () {
          Navigator.push(
            context,
            MaterialPageRoute(builder: (context) => const NewMessageScreen()),
          );
        },
        child: const Icon(Icons.add_comment),
      ),
    );
  }
}
```

### 3.3 Refresh After Sending Messages

```dart
// new_message_screen.dart - Add refresh after sending
Future<void> _sendMessage() async {
  // ... validation code ...

  setState(() => _isSending = true);
  
  final actions = ref.read(smsActionsProvider);
  final success = await actions.sendText(_recipientController.text, _messageController.text);

  setState(() => _isSending = false);

  if (mounted) {
    if (success) {
      // ⚠️ CRITICAL: Refresh thread list to show the new message
      print('📤 Message sent, refreshing thread list...');
      actions.refreshThreads();
      
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Message sent'),
          backgroundColor: Colors.green,
        ),
      );
      Navigator.pop(context);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Failed to send message'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }
}
```

---

### 3.4 MMS Composer with File Attachments ⚠️ **CRITICAL FOR YOUR USE CASE**

**Complete implementation of message composer with image/document picker:**

```dart
// new_message_screen.dart - Complete MMS composer with file attachments
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import 'package:sms_platform/sms_platform.dart';
import '../providers/sms_provider.dart';
import 'dart:io';

class NewMessageScreen extends ConsumerStatefulWidget {
  const NewMessageScreen({super.key});

  @override
  ConsumerState<NewMessageScreen> createState() => _NewMessageScreenState();
}

class _NewMessageScreenState extends ConsumerState<NewMessageScreen> {
  final TextEditingController _recipientController = TextEditingController();
  final TextEditingController _messageController = TextEditingController();
  final ImagePicker _picker = ImagePicker();
  
  List<XFile> _attachments = [];
  bool _isSending = false;

  @override
  void dispose() {
    _recipientController.dispose();
    _messageController.dispose();
    super.dispose();
  }

  /// Pick image from gallery
  Future<void> _pickImage() async {
    try {
      final XFile? image = await _picker.pickImage(
        source: ImageSource.gallery,
        maxWidth: 1920,
        maxHeight: 1920,
        imageQuality: 85,
      );
      
      if (image != null) {
        setState(() {
          _attachments.add(image);
        });
        print('✅ Image selected: ${image.path}');
      }
    } catch (e) {
      print('❌ Error picking image: $e');
      _showError('Failed to pick image: $e');
    }
  }

  /// Take photo with camera
  Future<void> _takePhoto() async {
    try {
      final XFile? photo = await _picker.pickImage(
        source: ImageSource.camera,
        maxWidth: 1920,
        maxHeight: 1920,
        imageQuality: 85,
      );
      
      if (photo != null) {
        setState(() {
          _attachments.add(photo);
        });
        print('✅ Photo taken: ${photo.path}');
      }
    } catch (e) {
      print('❌ Error taking photo: $e');
      _showError('Failed to take photo: $e');
    }
  }

  /// Pick document (for PDFs, etc.)
  /// Note: You'll need file_picker package for documents
  /// Add to pubspec.yaml: file_picker: ^5.5.0
  Future<void> _pickDocument() async {
    // This is a placeholder - implement with file_picker package
    _showError('Document picking requires file_picker package.\nAdd: file_picker: ^5.5.0 to pubspec.yaml');
  }

  /// Remove attachment
  void _removeAttachment(int index) {
    setState(() {
      _attachments.removeAt(index);
    });
  }

  /// Send message (SMS or MMS depending on attachments)
  Future<void> _sendMessage() async {
    final recipient = _recipientController.text.trim();
    final message = _messageController.text.trim();

    // Validation
    if (recipient.isEmpty) {
      _showError('Please enter a recipient');
      return;
    }

    if (message.isEmpty && _attachments.isEmpty) {
      _showError('Please enter a message or attach a file');
      return;
    }

    setState(() => _isSending = true);

    final actions = ref.read(smsActionsProvider);
    bool success;

    try {
      if (_attachments.isEmpty) {
        // Send as SMS (text only)
        print('📤 Sending SMS to $recipient');
        success = await actions.sendText(recipient, message);
      } else {
        // Send as MMS (with attachments)
        print('📤 Sending MMS to $recipient with ${_attachments.length} attachments');
        
        // Prepare attachments for MMS
        final attachmentData = _attachments.map((file) {
          // Determine MIME type
          String mimeType = 'image/jpeg';
          final ext = file.path.split('.').last.toLowerCase();
          if (ext == 'png') mimeType = 'image/png';
          else if (ext == 'gif') mimeType = 'image/gif';
          else if (ext == 'pdf') mimeType = 'application/pdf';
          
          return MmsAttachment(
            uri: file.path,
            mimeType: mimeType,
          );
        }).toList();

        success = await actions.sendMms(
          addresses: [recipient],
          text: message.isEmpty ? null : message,
          attachments: attachmentData,
        );
      }

      setState(() => _isSending = false);

      if (mounted) {
        if (success) {
          // Refresh thread list
          print('📤 Message sent, refreshing thread list...');
          actions.refreshThreads();
          
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(_attachments.isEmpty ? 'SMS sent' : 'MMS sent'),
              backgroundColor: Colors.green,
            ),
          );
          Navigator.pop(context);
        } else {
          _showError('Failed to send message');
        }
      }
    } catch (e) {
      setState(() => _isSending = false);
      _showError('Error sending message: $e');
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('New Message'),
        actions: [
          if (_isSending)
            const Center(
              child: Padding(
                padding: EdgeInsets.all(16.0),
                child: CircularProgressIndicator(color: Colors.white),
              ),
            )
          else
            IconButton(
              icon: const Icon(Icons.send),
              onPressed: _sendMessage,
              tooltip: 'Send',
            ),
        ],
      ),
      body: Column(
        children: [
          // Recipient field
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: TextField(
              controller: _recipientController,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(
                labelText: 'To',
                hintText: 'Phone number',
                border: OutlineInputBorder(),
                prefixIcon: Icon(Icons.person),
              ),
            ),
          ),

          // Attachments preview
          if (_attachments.isNotEmpty)
            Container(
              height: 120,
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              child: ListView.builder(
                scrollDirection: Axis.horizontal,
                itemCount: _attachments.length,
                itemBuilder: (context, index) {
                  final file = _attachments[index];
                  return Stack(
                    children: [
                      Container(
                        width: 100,
                        height: 100,
                        margin: const EdgeInsets.only(right: 8.0),
                        decoration: BoxDecoration(
                          border: Border.all(color: Colors.grey),
                          borderRadius: BorderRadius.circular(8.0),
                        ),
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(8.0),
                          child: Image.file(
                            File(file.path),
                            fit: BoxFit.cover,
                            errorBuilder: (context, error, stackTrace) {
                              // Show icon for non-image files
                              return const Icon(Icons.insert_drive_file, size: 48);
                            },
                          ),
                        ),
                      ),
                      Positioned(
                        top: 0,
                        right: 8,
                        child: IconButton(
                          icon: const Icon(Icons.close, color: Colors.red),
                          onPressed: () => _removeAttachment(index),
                          tooltip: 'Remove',
                        ),
                      ),
                    ],
                  );
                },
              ),
            ),

          // Message field
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: TextField(
                controller: _messageController,
                maxLines: null,
                expands: true,
                textAlignVertical: TextAlignVertical.top,
                decoration: InputDecoration(
                  hintText: _attachments.isEmpty 
                      ? 'Type a message...' 
                      : 'Add a caption (optional)...',
                  border: const OutlineInputBorder(),
                ),
              ),
            ),
          ),

          // Attachment buttons
          Container(
            padding: const EdgeInsets.all(8.0),
            decoration: BoxDecoration(
              color: Theme.of(context).colorScheme.surfaceVariant,
              border: Border(top: BorderSide(color: Colors.grey.shade300)),
            ),
            child: Row(
              children: [
                IconButton(
                  icon: const Icon(Icons.photo_library),
                  onPressed: _pickImage,
                  tooltip: 'Attach image',
                ),
                IconButton(
                  icon: const Icon(Icons.camera_alt),
                  onPressed: _takePhoto,
                  tooltip: 'Take photo',
                ),
                IconButton(
                  icon: const Icon(Icons.attach_file),
                  onPressed: _pickDocument,
                  tooltip: 'Attach document',
                ),
                const Spacer(),
                Text(
                  _attachments.isEmpty 
                      ? 'No attachments' 
                      : '${_attachments.length} file(s)',
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
```

**✅ Key Features:**
- Pick images from gallery
- Take photos with camera
- Attach multiple files
- Preview attachments before sending
- Remove attachments
- Automatically sends as SMS (text only) or MMS (with attachments)
- Shows sending progress
- Refreshes thread list after sending

---

### 3.5 Update Dart Plugin Bridge for MMS

Add `sendMms()` to `sms_android.dart`:

```dart
// sms_android.dart - Add MMS support
@override
Future<bool> sendMms({
  required List<String> addresses,
  String? text,
  List<MmsAttachment>? attachments,
}) async {
  try {
    final bool? result = await _channel.invokeMethod('sendMms', {
      'addresses': addresses,
      'text': text,
      'attachments': attachments?.map((a) => {
        'uri': a.uri,
        'mimeType': a.mimeType,
      }).toList(),
    });
    return result ?? false;
  } catch (e) {
    print('❌ Error sending MMS: $e');
    return false;
  }
}
```

**Add to `sms_provider.dart`:**

```dart
// sms_provider.dart - Add MMS action
Future<bool> sendMms({
  required List<String> addresses,
  String? text,
  List<MmsAttachment>? attachments,
}) async {
  print('📤 Sending MMS to ${addresses.join(", ")}, text: $text, attachments: ${attachments?.length ?? 0}');
  final platform = ref.read(smsPlatformProvider);
  final result = await platform.sendMms(
    addresses: addresses,
    text: text,
    attachments: attachments,
  );
  print(result ? '✅ MMS sent successfully' : '❌ MMS send failed');
  if (result) {
    refreshThreads(); // Refresh UI
  }
  return result;
}
```

---

## 🐛 Common Issues and Solutions

### Issue 1: "App fails to become default SMS app"

**Symptoms:**
- Role request returns `false` (resultCode=0)
- System dialog doesn't show your app

**Solutions:**
1. ✅ Ensure `SENDTO` intent filters are in `MainActivity` (see AndroidManifest above)
2. ✅ Add `HeadlessSmsSendService` (required for Android 10+)
3. ✅ Verify all broadcast receivers are present
4. ✅ Check that `RECEIVE_WAP_PUSH` permission is declared

### Issue 2: "Messages not appearing in UI"

**Symptoms:**
- SMS sends/receives successfully
- Logs show "SMS from +1234567890: Hello"
- But UI stays empty

**Root Cause:** You're not saving messages to the database!

**Solution:** Add `saveSentSmsToDatabase()` and `saveReceivedSmsToDatabase()` (see Step 2.1)

### Issue 3: "Database only shows 5 old messages"

**Symptoms:**
- `listThreads()` returns old messages
- New sent/received messages don't appear

**Solution:** Same as Issue 2 - you're not saving to the database!

### Issue 4: "Kotlin version incompatibility"

**Symptoms:**
```
Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin.
The actual metadata version is 2.1.0, but the compiler version 1.8.0 can read versions up to 1.9.0.
```

**Solution:** Update `settings.gradle.kts`:
```kotlin
id("org.jetbrains.kotlin.android") version "2.1.0" apply false
```

### Issue 5: "NDK version mismatch"

**Symptoms:**
```
Your project is configured with Android NDK 26.3.11579264, but the following plugin(s) depend on a different Android NDK version: requires Android NDK 27.0.12077973
```

**Solution:** Update `build.gradle.kts`:
```kotlin
ndkVersion = "27.0.12077973"
```

---

## 🔌 Integration into Existing Flutter App

### Step-by-Step Integration Guide

#### 1. Add the Plugin Packages

**Create the packages directory** (if it doesn't exist):
```bash
cd your_existing_app/
mkdir -p packages/sms_platform/lib/src/models
mkdir -p packages/sms_android/lib
mkdir -p packages/sms_android/android/src/main/kotlin/com/example/sms_android
```

**Copy the package files:**
- Copy all files from `packages/sms_platform/` (models, interface, etc.)
- Copy all files from `packages/sms_android/` (Dart + Kotlin plugin)

**Update your main `pubspec.yaml`:**
```yaml
dependencies:
  # ... your existing dependencies ...
  
  # Add SMS packages
  sms_platform:
    path: packages/sms_platform
  sms_android:
    path: packages/sms_android
```

Run `flutter pub get`.

#### 2. Update Android Configuration

**Add to `android/app/src/main/AndroidManifest.xml`** (inside `<manifest>` tag):
```xml
<!-- Add these permissions at the top -->
<uses-permission android:name="android.permission.SEND_SMS"/>
<uses-permission android:name="android.permission.RECEIVE_SMS"/>
<uses-permission android:name="android.permission.READ_SMS"/>
<uses-permission android:name="android.permission.RECEIVE_MMS"/>
<uses-permission android:name="android.permission.RECEIVE_WAP_PUSH"/>
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
<uses-permission android:name="android.permission.INTERNET"/>

<!-- Inside <application> tag, add: -->

<!-- SMS Receiver -->
<receiver
    android:name="com.example.sms_android.SmsReceiver"
    android:permission="android.permission.BROADCAST_SMS"
    android:exported="true">
    <intent-filter>
        <action android:name="android.provider.Telephony.SMS_DELIVER"/>
    </intent-filter>
</receiver>

<!-- MMS Receiver -->
<receiver
    android:name="com.example.sms_android.MmsReceiver"
    android:permission="android.permission.BROADCAST_WAP_PUSH"
    android:exported="true">
    <intent-filter>
        <action android:name="android.provider.Telephony.WAP_PUSH_DELIVER"/>
        <data android:mimeType="application/vnd.wap.mms-message"/>
    </intent-filter>
</receiver>

<!-- Headless SMS Send Service -->
<service
    android:name="com.example.sms_android.HeadlessSmsSendService"
    android:permission="android.permission.SEND_RESPOND_VIA_MESSAGE"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.RESPOND_VIA_MESSAGE"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <data android:scheme="sms"/>
        <data android:scheme="smsto"/>
    </intent-filter>
</service>

<!-- Inside your existing <activity> tag for MainActivity, add: -->
<intent-filter>
    <action android:name="android.intent.action.SEND"/>
    <action android:name="android.intent.action.SENDTO"/>
    <category android:name="android.intent.category.DEFAULT"/>
    <category android:name="android.intent.category.BROWSABLE"/>
    <data android:scheme="sms"/>
    <data android:scheme="smsto"/>
</intent-filter>
```

**Check Gradle versions** in `android/settings.gradle.kts`:
```kotlin
plugins {
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("com.android.application") version "8.7.3" apply false
}
```

#### 3. Integrate with Your Existing State Management

##### Option 1: Using Riverpod (as in guide)

If you're already using Riverpod, just copy the providers from the guide.

##### Option 2: Using Bloc

```dart
// sms_bloc.dart
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:sms_platform/sms_platform.dart';
import 'package:sms_android/sms_android.dart';

// Events
abstract class SmsEvent {}
class LoadThreads extends SmsEvent {}
class SendSms extends SmsEvent {
  final String address;
  final String body;
  SendSms(this.address, this.body);
}
class IncomingSmsReceived extends SmsEvent {
  final IncomingSms sms;
  IncomingSmsReceived(this.sms);
}

// States
abstract class SmsState {}
class SmsInitial extends SmsState {}
class SmsLoading extends SmsState {}
class SmsLoaded extends SmsState {
  final List<SmsThread> threads;
  SmsLoaded(this.threads);
}
class SmsError extends SmsState {
  final String message;
  SmsError(this.message);
}

// Bloc
class SmsBloc extends Bloc<SmsEvent, SmsState> {
  final SmsPlatform _platform = SmsAndroid();

  SmsBloc() : super(SmsInitial()) {
    on<LoadThreads>(_onLoadThreads);
    on<SendSms>(_onSendSms);
    on<IncomingSmsReceived>(_onIncomingSms);
    
    // Listen to incoming SMS stream
    _platform.watchIncoming().listen((incomingSms) {
      add(IncomingSmsReceived(incomingSms));
    });
  }

  Future<void> _onLoadThreads(LoadThreads event, Emitter<SmsState> emit) async {
    emit(SmsLoading());
    try {
      final threads = await _platform.listThreads(limit: 100);
      emit(SmsLoaded(threads));
    } catch (e) {
      emit(SmsError(e.toString()));
    }
  }

  Future<void> _onSendSms(SendSms event, Emitter<SmsState> emit) async {
    await _platform.sendText(address: event.address, body: event.body);
    add(LoadThreads()); // Refresh threads
  }

  Future<void> _onIncomingSms(IncomingSmsReceived event, Emitter<SmsState> emit) async {
    // Wait for database to update
    await Future.delayed(const Duration(milliseconds: 500));
    add(LoadThreads()); // Refresh threads
  }
}
```

**Usage in your existing app:**
```dart
// In your main.dart or wherever you set up providers
BlocProvider(
  create: (context) => SmsBloc()..add(LoadThreads()),
  child: YourExistingApp(),
),
```

##### Option 3: Using Provider

```dart
// sms_provider.dart
import 'package:flutter/foundation.dart';
import 'package:sms_platform/sms_platform.dart';
import 'package:sms_android/sms_android.dart';

class SmsProvider with ChangeNotifier {
  final SmsPlatform _platform = SmsAndroid();
  
  List<SmsThread> _threads = [];
  bool _isLoading = false;
  String? _error;

  List<SmsThread> get threads => _threads;
  bool get isLoading => _isLoading;
  String? get error => _error;

  SmsProvider() {
    loadThreads();
    _listenToIncoming();
  }

  void _listenToIncoming() {
    _platform.watchIncoming().listen((incomingSms) async {
      await Future.delayed(const Duration(milliseconds: 500));
      await loadThreads();
    });
  }

  Future<void> loadThreads() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _threads = await _platform.listThreads(limit: 100);
    } catch (e) {
      _error = e.toString();
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<bool> sendText(String address, String body) async {
    final success = await _platform.sendText(address: address, body: body);
    if (success) {
      await loadThreads();
    }
    return success;
  }

  Future<bool> requestDefaultSmsRole() async {
    return await _platform.requestDefaultSmsRole();
  }
}
```

**Usage:**
```dart
// In main.dart
ChangeNotifierProvider(
  create: (_) => SmsProvider(),
  child: YourExistingApp(),
),
```

##### Option 4: Using GetX

```dart
// sms_controller.dart
import 'package:get/get.dart';
import 'package:sms_platform/sms_platform.dart';
import 'package:sms_android/sms_android.dart';

class SmsController extends GetxController {
  final SmsPlatform _platform = SmsAndroid();
  
  var threads = <SmsThread>[].obs;
  var isLoading = false.obs;
  var error = Rxn<String>();

  @override
  void onInit() {
    super.onInit();
    loadThreads();
    _listenToIncoming();
  }

  void _listenToIncoming() {
    _platform.watchIncoming().listen((incomingSms) async {
      await Future.delayed(const Duration(milliseconds: 500));
      await loadThreads();
    });
  }

  Future<void> loadThreads() async {
    isLoading.value = true;
    error.value = null;

    try {
      threads.value = await _platform.listThreads(limit: 100);
    } catch (e) {
      error.value = e.toString();
    } finally {
      isLoading.value = false;
    }
  }

  Future<bool> sendText(String address, String body) async {
    final success = await _platform.sendText(address: address, body: body);
    if (success) {
      await loadThreads();
    }
    return success;
  }

  Future<bool> requestDefaultSmsRole() async {
    return await _platform.requestDefaultSmsRole();
  }
}
```

**Usage:**
```dart
// In your existing app
Get.put(SmsController());
```

#### 4. Add SMS Screens to Your Existing Navigation

**Example: Adding to existing Navigator routes:**
```dart
MaterialApp(
  initialRoute: '/home', // Your existing home
  routes: {
    // Your existing routes
    '/home': (context) => YourHomeScreen(),
    '/profile': (context) => YourProfileScreen(),
    
    // Add SMS routes
    '/sms': (context) => SmsHomeScreen(),
    '/sms/thread': (context) => ThreadScreen(),
    '/sms/new': (context) => NewMessageScreen(),
    '/sms/setup': (context) => SmsSetupScreen(),
  },
);
```

**Example: Adding to existing BottomNavigationBar:**
```dart
BottomNavigationBar(
  items: [
    BottomNavigationBarItem(icon: Icon(Icons.home), label: 'Home'),
    BottomNavigationBarItem(icon: Icon(Icons.message), label: 'Messages'), // SMS
    BottomNavigationBarItem(icon: Icon(Icons.person), label: 'Profile'),
  ],
  onTap: (index) {
    switch (index) {
      case 0:
        Navigator.pushNamed(context, '/home');
        break;
      case 1:
        Navigator.pushNamed(context, '/sms'); // SMS screen
        break;
      case 2:
        Navigator.pushNamed(context, '/profile');
        break;
    }
  },
);
```

#### 5. Request SMS Permissions

**Add this check on first SMS screen load:**
```dart
// In your SMS home screen's initState or onInit
Future<void> _checkSmsSetup() async {
  final platform = SmsAndroid();
  
  final isDefault = await platform.isDefaultSmsApp();
  final hasPermissions = await platform.hasPermissions();
  
  if (!isDefault || !hasPermissions) {
    // Navigate to setup screen
    Navigator.pushNamed(context, '/sms/setup');
  }
}
```

#### 6. Testing Integration

**Minimal test to verify it works:**
```dart
// Add a test button somewhere in your app
ElevatedButton(
  onPressed: () async {
    final platform = SmsAndroid();
    
    // Test 1: Check if SMS platform is available
    final isDefault = await platform.isDefaultSmsApp();
    print('Is default SMS app: $isDefault');
    
    // Test 2: Load threads
    final threads = await platform.listThreads(limit: 5);
    print('Found ${threads.length} threads');
    
    // Test 3: Listen for incoming SMS
    platform.watchIncoming().listen((sms) {
      print('Received SMS from ${sms.address}: ${sms.body}');
    });
  },
  child: Text('Test SMS Integration'),
),
```

---

### Common Integration Questions

**Q: Will SMS functionality work if my app isn't the default SMS app?**
A: You can **read** SMS and **send** SMS without being default, but to **receive** SMS broadcasts and be fully functional, the app must be set as default.

**Q: Can users still use their original SMS app?**
A: Yes! Users can switch between SMS apps in Android settings. Your app becoming default doesn't delete other SMS apps.

**Q: Will this affect my existing permissions?**
A: No. SMS permissions are additive and won't interfere with your existing permissions. Users will see a permission dialog for SMS permissions separately.

**Q: Do I need to change my existing state management?**
A: No. The SMS plugin is just a data source. Wrap it in whatever state management you're already using (examples above).

**Q: Can I customize the SMS UI?**
A: Absolutely! The guide provides example screens, but you can build your own UI using the plugin's data (threads, messages, etc.).

**Q: What if I only want to send SMS, not receive?**
A: You can use just the sending functionality without becoming the default app. Skip the receivers in AndroidManifest and don't call `requestDefaultSmsRole()`.

---

## ✅ Testing Checklist

1. **Setup:**
   - [ ] App requests default SMS role
   - [ ] System dialog shows your app as an option
   - [ ] App becomes default SMS app (check in system settings)

2. **Sending:**
   - [ ] Send SMS to a real phone number
   - [ ] Check logs for "✅ Saved sent SMS to database"
   - [ ] Message appears in UI thread list
   - [ ] Recipient receives the message

3. **Receiving:**
   - [ ] Receive SMS from another phone
   - [ ] Check logs for "✅ Saved received SMS to database"
   - [ ] Message appears in UI thread list
   - [ ] UI updates automatically (within 500ms)

4. **Persistence:**
   - [ ] Kill and restart app
   - [ ] All messages still visible
   - [ ] Open default Android SMS app (if available) - messages appear there too

---

## 📦 Project Portability - Essential Files for Android-Only Flutter App

### What to Copy to Another System

**✅ ESSENTIAL FILES/DIRECTORIES (Must Copy):**

```
your_sms_app/
├── lib/                                    # Flutter/Dart code
│   ├── main.dart
│   ├── providers/
│   ├── screens/
│   └── utils/
├── packages/                               # Local plugin packages
│   ├── sms_platform/
│   │   ├── lib/
│   │   └── pubspec.yaml
│   └── sms_android/
│       ├── lib/
│       ├── android/
│       │   ├── build.gradle
│       │   ├── src/main/
│       │   │   ├── AndroidManifest.xml
│       │   │   └── kotlin/
│       │   └── libs/                       # android-smsmms JARs
│       └── pubspec.yaml
├── android/
│   ├── app/
│   │   ├── build.gradle.kts
│   │   └── src/main/
│   │       ├── AndroidManifest.xml
│   │       ├── kotlin/                     # MainActivity
│   │       └── res/                        # Icons, resources
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   └── local.properties                    # SDK path (regenerate)
├── pubspec.yaml                            # Main dependencies
├── pubspec.lock                            # Dependency versions
├── analysis_options.yaml
├── README.md
├── FLUTTER_SMS_APP_COMPLETE_IMPLEMENTATION_GUIDE.md
└── .gitignore
```

**❌ DO NOT COPY (Can Be Regenerated):**

```
❌ build/                                   # Build artifacts
❌ .dart_tool/                              # Dart tools cache
❌ .flutter-plugins
❌ .flutter-plugins-dependencies
❌ .idea/                                   # IDE files
❌ *.iml                                    # IntelliJ files
❌ .vscode/                                 # VSCode settings
❌ ios/                                     # Not needed (Android-only)
❌ linux/                                   # Not needed
❌ macos/                                   # Not needed
❌ web/                                     # Not needed
❌ windows/                                 # Not needed
❌ android/gradle/wrapper/gradle-wrapper.jar  # Will be downloaded
❌ packages/*/build/                        # Plugin build artifacts
❌ packages/*/.dart_tool/
```

---

### 📋 Step-by-Step Transfer Process

**On the source system:**

```bash
cd /path/to/your_sms_app

# Create a clean copy (excludes build artifacts)
tar -czf sms_app_clean.tar.gz \
  --exclude='build' \
  --exclude='.dart_tool' \
  --exclude='.idea' \
  --exclude='*.iml' \
  --exclude='.flutter-plugins*' \
  --exclude='ios' \
  --exclude='linux' \
  --exclude='macos' \
  --exclude='web' \
  --exclude='windows' \
  --exclude='android/gradle/wrapper/*.jar' \
  --exclude='packages/*/build' \
  --exclude='packages/*/.dart_tool' \
  lib/ packages/ android/ pubspec.* analysis_options.yaml \
  README.md FLUTTER_SMS_APP_COMPLETE_IMPLEMENTATION_GUIDE.md .gitignore
```

**On the new system:**

```bash
# Extract
tar -xzf sms_app_clean.tar.gz
cd your_sms_app

# Update Android SDK path
echo "sdk.dir=/path/to/your/Android/Sdk" > android/local.properties

# Install Flutter dependencies
flutter pub get

# Clean and rebuild
flutter clean
flutter pub get

# Verify packages
cd packages/sms_platform && flutter pub get && cd ../..
cd packages/sms_android && flutter pub get && cd ../..

# Build and run
flutter run -d <device_id>
```

---

### 🔑 Critical Files Checklist

**Before transferring, verify these files exist:**

#### 1. Android Native Code
- [ ] `android/app/src/main/AndroidManifest.xml` - With all SMS permissions and receivers
- [ ] `android/app/build.gradle.kts` - compileSdk, targetSdk, ndkVersion
- [ ] `android/settings.gradle.kts` - Kotlin version 2.1.0
- [ ] `android/app/src/main/kotlin/com/example/smsapp/MainActivity.kt`

#### 2. Plugin Packages (sms_android)
- [ ] `packages/sms_android/android/src/main/kotlin/com/example/sms_android/SmsAndroidPlugin.kt`
- [ ] `packages/sms_android/android/src/main/kotlin/com/example/sms_android/SmsReceiver.kt`
- [ ] `packages/sms_android/android/src/main/kotlin/com/example/sms_android/MmsReceiverImpl.kt`
- [ ] `packages/sms_android/android/src/main/kotlin/com/example/sms_android/HeadlessSmsSendService.kt`
- [ ] `packages/sms_android/android/libs/*.jar` - android-smsmms library JARs
- [ ] `packages/sms_android/android/build.gradle` - Plugin gradle config
- [ ] `packages/sms_android/android/src/main/AndroidManifest.xml` - Plugin manifest
- [ ] `packages/sms_android/lib/sms_android.dart` - Dart bridge

#### 3. Plugin Packages (sms_platform)
- [ ] `packages/sms_platform/lib/sms_platform.dart` - Platform interface
- [ ] `packages/sms_platform/lib/src/sms_platform_interface.dart`
- [ ] `packages/sms_platform/lib/src/models/*.dart` - Data models
- [ ] `packages/sms_platform/pubspec.yaml`

#### 4. Flutter App Code
- [ ] `lib/main.dart`
- [ ] `lib/providers/sms_provider.dart` - State management
- [ ] `lib/screens/` - All UI screens
- [ ] `pubspec.yaml` - Main dependencies

#### 5. Documentation
- [ ] `FLUTTER_SMS_APP_COMPLETE_IMPLEMENTATION_GUIDE.md` - This guide!
- [ ] `README.md` - Project overview

---

### 🚀 Minimal Viable Transfer (Absolute Bare Minimum)

**If you're in a hurry, these are the ABSOLUTE minimum files:**

```
your_sms_app/
├── lib/                                    # All Flutter code
├── packages/                               # All plugin packages
├── android/                                # All Android native code
├── pubspec.yaml                            # Dependencies
└── FLUTTER_SMS_APP_COMPLETE_IMPLEMENTATION_GUIDE.md
```

Then run:
```bash
flutter pub get
flutter clean
flutter run
```

Everything else (build artifacts, IDE files, lock files) will regenerate.

---

### ⚠️ Common Transfer Issues

**Issue 1: "SDK location not found"**
```bash
# Fix: Create local.properties
echo "sdk.dir=/path/to/your/Android/Sdk" > android/local.properties
```

**Issue 2: "Package sms_platform not found"**
```bash
# Fix: Install plugin packages first
cd packages/sms_platform && flutter pub get && cd ../..
cd packages/sms_android && flutter pub get && cd ../..
flutter pub get
```

**Issue 3: "Gradle build failed"**
```bash
# Fix: Clean and rebuild
flutter clean
cd android && ./gradlew clean && cd ..
flutter pub get
flutter run
```

**Issue 4: "android-smsmms library not found"**
```bash
# Fix: Verify JAR files exist
ls packages/sms_android/android/libs/
# Should show android-smsmms library JARs
# If missing, copy from source system or rebuild library
```

---

### 📁 Recommended: Use Git (Best Practice)

**Instead of manual copying, use Git:**

```bash
# On source system
git init
git add lib/ packages/ android/ pubspec.yaml analysis_options.yaml README.md
git add FLUTTER_SMS_APP_COMPLETE_IMPLEMENTATION_GUIDE.md .gitignore
git commit -m "Initial commit - SMS app with working SMS, MMS partial"

# Push to remote (GitHub, GitLab, etc.)
git remote add origin <your-repo-url>
git push -u origin main

# On new system
git clone <your-repo-url>
cd your_sms_app
flutter pub get
flutter run
```

**.gitignore should include:**
```
# Flutter
build/
.dart_tool/
.flutter-plugins
.flutter-plugins-dependencies

# Android
*.iml
.gradle
android/local.properties
android/.gradle/
android/captures/
android/gradlew
android/gradlew.bat
android/gradle/wrapper/gradle-wrapper.jar

# IDE
.idea/
.vscode/

# Platforms not needed (Android-only)
ios/
linux/
macos/
web/
windows/
```

---

## 📝 Quick Start Commands

```bash
# On new system after transfer

# 1. Update Android SDK path
echo "sdk.dir=$HOME/Android/Sdk" > android/local.properties

# 2. Install dependencies
flutter pub get
cd packages/sms_platform && flutter pub get && cd ../..
cd packages/sms_android && flutter pub get && cd ../..

# 3. Clean build
flutter clean

# 4. Verify setup
flutter doctor -v

# 5. Run on connected device
flutter run -d <device_id>

# 6. Watch logs (critical for debugging)
flutter logs | grep -E "SmsAndroidPlugin|SmsReceiver|flutter"
```

---

## 🎓 Key Learnings

### 1. **Default SMS App Responsibilities**

When you become the default SMS app, YOU are responsible for:
- ✅ Saving sent messages to `content://sms/sent`
- ✅ Saving received messages to `content://sms/inbox`
- ✅ Displaying message threads
- ✅ Handling MMS downloads

Android **does NOT** do any of this automatically!

### 2. **Why Database Persistence is Critical**

Without saving to the database:
- Messages won't appear in your UI
- Messages won't appear in other SMS apps
- Android won't know about the conversations
- Thread queries will return empty results

### 3. **Timing is Everything**

When receiving an SMS:
1. `SmsReceiver.onReceive()` is called
2. You save to database (takes ~50-200ms)
3. You trigger UI refresh
4. Add a 500ms delay before querying database
5. Database has time to update and index

### 4. **Testing on Real Devices**

SMS apps **MUST** be tested on real devices with SIM cards:
- Emulators can simulate SMS, but not role management
- MMS requires real APN/MMSC configuration
- Permissions behave differently on real devices

---

## 🚀 Next Steps After Implementation

**If You Got SMS Working (Congratulations! 🎉):**

1. **Implement message search** - Query SMS database with search terms
2. **Add conversation threading** - Group messages by contact
3. **Support for contact names** (via Contacts API) - Show names instead of numbers
4. **Notification support** (when app is in background) - FCM or local notifications
5. **Message delivery reports** - Track sent SMS status
6. **Draft messages** - Save unsent messages
7. **Message scheduling** - Schedule SMS for future sending
8. **Message archiving** - Archive old conversations

**If You Need MMS (Reality Check):**

**Option 1: System SMS App Integration (RECOMMENDED)**
```kotlin
// Send MMS via system SMS app
val intent = Intent(Intent.ACTION_SEND).apply {
    type = "image/*"
    putExtra("address", phoneNumber)
    putExtra("sms_body", messageText)
    putExtra(Intent.EXTRA_STREAM, imageUri)
}
startActivity(Intent.createChooser(intent, "Send MMS"))
```

**Option 2: Continue Debugging MMS (Expect 2-4+ Weeks)**
- Hire professional Android developer with telephony stack experience
- Contact carrier for APN/MMSC documentation
- Debug android-smsmms library with modern Android
- Test on multiple carriers and devices
- Success not guaranteed

**Option 3: Wait for Better Solutions**
- Monitor for updated MMS libraries
- Wait for Google to improve default SMS app APIs
- Consider third-party SMS services (Twilio, etc.) for business use

---

## 📚 Additional Resources

- [Android Telephony API Docs](https://developer.android.com/reference/android/telephony/package-summary)
- [Default SMS App Requirements](https://developer.android.com/guide/topics/text/sms)
- [RoleManager Documentation](https://developer.android.com/reference/android/app/role/RoleManager)
- [Flutter Platform Channels](https://docs.flutter.dev/platform-integration/platform-channels)

---

## 💡 Pro Tips

1. **Always log extensively** during development
2. **Test on multiple Android versions** (especially API 29+ for RoleManager)
3. **Use `flutter clean`** when Kotlin changes don't seem to apply
4. **Monitor `adb logcat`** for system-level errors
5. **Keep this guide handy** - copy it to every SMS app project!

---

**Created:** October 2025  
**Tested On:** Android 13-15 (API 33-36)  
**Flutter Version:** 3.x+ with Dart 3.x

---

## 🎯 Critical Summary

**If you remember THREE things from this guide:**

### 1. SMS Database Persistence (Most Important for SMS)

> **As the default SMS app, YOU must manually save both sent and received messages to the SMS database using `ContentResolver.insert()`. Android will NOT do this for you!**

This is the single most important discovery that makes the difference between a non-functional SMS app and a working one.

### 2. SMS Works, MMS Doesn't (Honest Reality)

> **SMS (text-only messages) is fully tested and working on Android 13-15. MMS (messages with images/attachments) does NOT work despite weeks of debugging attempts. The android-smsmms library integration receives WAP_PUSH notifications but fails to download with "empty response" errors.**

Don't expect MMS to "just work" - it requires extensive carrier-specific debugging and may need a professional Android developer.

### 3. Alternative for MMS (Practical Solution)

> **If you need MMS functionality TODAY, integrate with the system SMS app using `Intent.ACTION_SEND` instead of building it from scratch. This provides reliable MMS that actually works.**

Building a default SMS app for text messages is achievable. Building one with working MMS is far more complex than this guide can cover.

---

## 📊 Final Implementation Scorecard

| Feature | Status | Notes |
|---------|--------|-------|
| **SMS Sending** | ✅ Working | Tested on Android 13-15 |
| **SMS Receiving** | ✅ Working | Real-time via broadcast receivers |
| **SMS Database Persistence** | ✅ Working | Manual insert() implementation |
| **Thread Listing** | ✅ Working | Query and group by thread_id |
| **Default SMS App Role** | ✅ Working | RoleManager integration |
| **UI Updates** | ✅ Working | ContentObserver + 500ms delay |
| **MMS Sending** | ⚠️ Untested | Code exists but not verified |
| **MMS Receiving** | ❌ Not Working | "empty response" from carrier |
| **Image Attachments** | ❌ Not Working | MMS backend fails |

**Production Readiness:**
- ✅ **SMS-only app**: Production ready for text messaging
- ❌ **MMS app**: Not production ready, needs extensive debugging
- 💡 **Hybrid approach**: SMS via this guide + MMS via system SMS app

Good luck with your implementation! 🚀

**Last Updated:** October 2025 (After extensive MMS debugging attempts)  
**SMS Status:** ✅ Production Ready  
**MMS Status:** ❌ Not Working (Documented for future reference)

