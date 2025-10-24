# Quick Start Guide

## For Developers

### Prerequisites Check
```bash
flutter doctor
# Ensure Android toolchain is ready
# Ensure Android SDK 35 is installed
```

### Clone & Setup (5 minutes)
```bash
cd /path/to/smsapp
flutter pub get
```

### Run on Device (Development)
```bash
# Connect Android device via USB
# Enable USB debugging on device

flutter devices  # Verify device connected
flutter run      # Launch app

# Hot reload: Press 'r' in terminal
# Hot restart: Press 'R' in terminal
```

### Build Release APK
```bash
flutter build apk --release

# Output: build/app/outputs/flutter-apk/app-release.apk
```

### Install on Device
```bash
# Via Flutter CLI
flutter install

# Or via ADB
adb install -r build/app/outputs/flutter-apk/app-release.apk
```

---

## For IT/MDM Admins

### Deploy to Managed Devices (10 minutes)

1. **Get APK**
   - Receive from development team
   - Or build yourself: `flutter build apk --release`

2. **Upload to MDM**
   - Login to MDM console (Workspace ONE, Intune, etc.)
   - Navigate to Apps section
   - Upload APK file
   - Fill in app details:
     - Name: SMS App
     - Package: com.example.smsapp
     - Category: Business

3. **Configure Deployment**
   - Assignment: Select device group
   - Installation: Required
   - Default SMS app: Enable policy

4. **Deploy**
   - Push to devices
   - Monitor installation status
   - Verify app appears on devices

5. **User Setup (on device)**
   - User opens app
   - Taps "Set as Default SMS App"
   - Accepts system dialog
   - Grants SMS permissions
   - Done!

### Troubleshooting Common Issues

**App won't install**
- Check device Android version (must be 8.0+)
- Verify "Unknown sources" allowed
- Check device storage space

**Can't set as default SMS**
- Verify all permissions in manifest
- Check device policy allows SMS apps
- Try uninstall/reinstall

**SMS not sending**
- Verify device has SIM card
- Check cellular signal
- Verify SMS permissions granted

**MMS not working**
- Check data connection enabled
- Verify APN settings configured
- Check MMS permissions granted

---

## For End Users

### First Time Setup (2 minutes)

1. **Open App**
   - Tap SMS App icon

2. **Set Default**
   - Tap "Set as Default SMS App"
   - Tap "Yes" on system dialog

3. **Grant Permissions**
   - Tap "Allow" for SMS permissions
   - Tap "Allow" for phone permissions

4. **Done!**
   - You'll see your message list
   - Can now send/receive SMS/MMS

### Daily Use

**Send Message**
1. Tap ➕ (New Message) button
2. Enter phone number
3. Tap ➕ to add recipient
4. Type message
5. Tap SEND

**Send MMS with Image**
1. Tap ➕ (New Message) button
2. Enter recipient(s)
3. Tap 🖼️ (Image) icon
4. Select photo
5. Add text (optional)
6. Tap SEND

**Read Messages**
- New messages appear automatically
- Tap conversation to open
- Messages show in chat view
- Pull down to refresh

**Reply to Message**
- Tap conversation in list
- Type reply at bottom
- Tap 📤 (Send) button

---

## File Structure Overview

```
smsapp/
├── lib/                    # Flutter UI code
│   ├── main.dart          # App entry point
│   ├── providers/         # State management
│   ├── screens/           # UI screens
│   └── utils/             # Helper functions
│
├── packages/
│   ├── sms_platform/      # Platform interface
│   └── sms_android/       # Android implementation
│       └── android/       # Kotlin code
│
├── android/               # Android app config
│   ├── app/
│   │   ├── build.gradle.kts
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
│
├── pubspec.yaml           # Dependencies
├── README.md              # Main documentation
├── BUILD_GUIDE.md         # Build instructions
└── ARCHITECTURE.md        # Technical details
```

---

## Key Commands

### Development
```bash
flutter run                    # Run debug build
flutter run --release          # Run release build
flutter logs                   # View logs
flutter clean                  # Clean build cache
```

### Building
```bash
flutter build apk              # Build debug APK
flutter build apk --release    # Build release APK
flutter build appbundle        # Build app bundle
```

### Device Management
```bash
flutter devices                # List connected devices
flutter install                # Install app on device
adb devices                    # List devices (ADB)
adb install app-release.apk   # Install via ADB
```

### Debugging
```bash
flutter logs                   # Stream logs
adb logcat | grep SMS          # Filter SMS logs
flutter doctor                 # Check setup
flutter analyze                # Run static analysis
```

---

## Important Paths

### Source Code
- **Main UI**: `lib/screens/`
- **Providers**: `lib/providers/sms_provider.dart`
- **Android Plugin**: `packages/sms_android/android/src/main/kotlin/`
- **Models**: `packages/sms_platform/lib/src/models/`

### Build Outputs
- **APK**: `build/app/outputs/flutter-apk/app-release.apk`
- **Bundle**: `build/app/outputs/bundle/release/app-release.aab`

### Configuration
- **Dependencies**: `pubspec.yaml`
- **Android Build**: `android/app/build.gradle.kts`
- **Permissions**: `android/app/src/main/AndroidManifest.xml`

---

## Support Resources

### Documentation
- `README.md` - User guide and overview
- `ARCHITECTURE.md` - Technical architecture
- `BUILD_GUIDE.md` - Detailed build instructions
- `CHANGELOG.md` - Version history

### Internal Support
- IT Help Desk: support@company.com
- Developer: [Your contact info]
- Wiki: [Company wiki link]

### External Resources
- Flutter Docs: https://docs.flutter.dev
- Android Telephony: https://developer.android.com/reference/android/telephony
- Riverpod Docs: https://riverpod.dev

---

## Testing Checklist

Before deployment, verify:
- [ ] App installs without errors
- [ ] Default SMS dialog appears
- [ ] All permissions granted
- [ ] Thread list loads existing messages
- [ ] Can send SMS successfully
- [ ] Can receive SMS successfully
- [ ] Can send MMS with image
- [ ] Can receive MMS
- [ ] New messages appear in real-time
- [ ] UI is responsive and smooth

---

## Common Customizations

### Change App Name
Edit `android/app/src/main/AndroidManifest.xml`:
```xml
android:label="Your App Name"
```

### Change App Icon
Replace files in:
```
android/app/src/main/res/mipmap-*/ic_launcher.png
```

### Change Package Name
1. Edit `android/app/build.gradle.kts`: `applicationId`
2. Update `AndroidManifest.xml`: `package` attribute
3. Rename Kotlin package directories
4. Update imports in Kotlin files

### Change Theme Colors
Edit `lib/main.dart`:
```dart
colorScheme: ColorScheme.fromSeed(
  seedColor: Colors.blue,  // Change this
),
```

---

## Version Updating

Quick version bump:
1. Edit `pubspec.yaml`: `version: 1.0.1+2`
2. Build: `flutter build apk --release`
3. Distribute new APK

Version format: `MAJOR.MINOR.PATCH+BUILD`
- Increment BUILD for each release
- Increment PATCH for bug fixes
- Increment MINOR for new features
- Increment MAJOR for breaking changes

---

## Emergency Procedures

### App Crashes on Launch
```bash
flutter clean
flutter pub get
flutter build apk --release
```

### Can't Send Messages
- Check device has SIM
- Verify app is default SMS app
- Check permissions granted
- View logs: `adb logcat | grep SMS`

### Rollback to Previous Version
1. Uninstall current version
2. Install previous APK
3. Set as default SMS app
4. All messages remain intact

---

## Performance Tips

### For Developers
- Use `const` widgets where possible
- Implement lazy loading for long lists
- Optimize image loading for MMS
- Profile with DevTools

### For Users
- Clear old conversations regularly
- Limit message history (system setting)
- Use Wi-Fi for MMS when possible

---

## Security Best Practices

### Developers
- Never commit keystores
- Use strong signing passwords
- Review permissions regularly
- Keep dependencies updated

### IT Admins
- Deploy via secure channels
- Verify APK signatures
- Use MDM policies
- Monitor installation logs

### End Users
- Don't share sensitive info via SMS
- Be cautious of unknown senders
- Report suspicious messages
- Use device lock screen

---

## FAQ

**Q: Does this work on iOS?**
A: No, Android only.

**Q: Can I use this as my personal SMS app?**
A: Yes, but it's designed for business use.

**Q: Will I lose my messages?**
A: No, all messages stay in Android's system database.

**Q: Can I switch back to another SMS app?**
A: Yes, anytime via Android Settings.

**Q: Does it support RCS?**
A: No, SMS/MMS only.

**Q: Can I backup my messages?**
A: Not in v1.0, messages are stored by Android system.

**Q: Does it work offline?**
A: SMS works always. MMS requires data connection.

**Q: What about group messages?**
A: Supported, but shown as individual threads.

---

## Next Steps

1. ✅ Read README.md for overview
2. ✅ Follow this quick start
3. ✅ Build and test on device
4. ✅ Deploy via MDM
5. ✅ Monitor user feedback
6. ✅ Refer to BUILD_GUIDE.md for advanced topics
7. ✅ Refer to ARCHITECTURE.md for deep dive

Good luck! 🚀

