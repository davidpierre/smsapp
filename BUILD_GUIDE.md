# Build & Deployment Guide

## Quick Build Commands

### Development Build
```bash
# Run on connected device
flutter run

# Run with hot reload
flutter run --hot

# Run in release mode for testing
flutter run --release
```

### Production Build
```bash
# Build release APK
flutter build apk --release

# Build app bundle (for Play Store, if needed)
flutter build appbundle --release

# Build split APKs per ABI (smaller downloads)
flutter build apk --split-per-abi --release
```

### Output Locations
- **Standard APK**: `build/app/outputs/flutter-apk/app-release.apk`
- **Split APKs**: `build/app/outputs/flutter-apk/app-armeabi-v7a-release.apk`, etc.
- **App Bundle**: `build/app/outputs/bundle/release/app-release.aab`

## Pre-Build Checklist

### 1. Update Version Numbers
Edit `pubspec.yaml`:
```yaml
version: 1.0.0+1  # Format: major.minor.patch+buildNumber
```

### 2. Verify Android Configuration
Check `android/app/build.gradle.kts`:
```kotlin
defaultConfig {
    applicationId = "com.example.smsapp"  # Change if needed
    minSdk = 26
    targetSdk = 35
    versionCode = flutter.versionCode
    versionName = flutter.versionName
}
```

### 3. Configure App Signing (Production)

#### Create keystore:
```bash
keytool -genkey -v -keystore ~/smsapp-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias smsapp-key
```

#### Create `android/key.properties`:
```properties
storePassword=YOUR_STORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=smsapp-key
storeFile=/path/to/smsapp-release.jks
```

#### Update `android/app/build.gradle.kts`:
```kotlin
// Add before android block
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    // ... existing config ...
    
    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## MDM Deployment

### Option 1: Direct APK Upload
1. Build release APK
2. Log into MDM console (e.g., Workspace ONE, Intune, etc.)
3. Upload APK to app catalog
4. Configure deployment settings:
   - Target: Managed device group
   - Installation: Required/Available
   - Default SMS: Set policy

### Option 2: Private App Repository
1. Build and sign APK
2. Upload to company private repo
3. Configure MDM to pull from repo
4. Set auto-update policy

### Option 3: Side-loading Script
```bash
#!/bin/bash
# deploy.sh - Install on connected device

# Build
flutter build apk --release

# Install
adb install -r build/app/outputs/flutter-apk/app-release.apk

# Grant default SMS app (requires root or special permissions)
adb shell cmd role add-role-holder android.app.role.SMS com.example.smsapp

# Grant permissions
adb shell pm grant com.example.smsapp android.permission.SEND_SMS
adb shell pm grant com.example.smsapp android.permission.RECEIVE_SMS
adb shell pm grant com.example.smsapp android.permission.READ_SMS
```

## Testing Builds

### Local Testing
```bash
# Install debug build
flutter install

# Check logs
flutter logs

# Or use adb directly
adb logcat | grep -i sms
```

### Test on Real Device
**IMPORTANT**: SMS/MMS testing requires:
- Physical device with SIM card
- Active cellular plan with SMS/MMS
- Another phone to send test messages

Cannot test on:
- Emulators (no SMS hardware)
- Tablets without cellular (no SIM)
- Devices without active plan

### Verification Steps
1. Install APK on test device
2. Open app → Complete setup flow
3. Grant default SMS app role
4. Grant all permissions
5. Send test SMS from app
6. Receive test SMS from another device
7. Verify thread list shows conversations
8. Test MMS with image attachment
9. Verify incoming message notifications

## Build Optimization

### Reduce APK Size
```bash
# Enable R8 optimization (default in release)
flutter build apk --release

# Split by ABI (recommended)
flutter build apk --split-per-abi --release

# Remove unused resources
flutter build apk --release --tree-shake-icons
```

### ProGuard/R8 Rules
If you encounter issues with release builds, add to `android/app/proguard-rules.pro`:
```proguard
-keep class com.example.sms_android.** { *; }
-keep class android.telephony.** { *; }
```

## Troubleshooting Build Issues

### Flutter not found
```bash
export PATH="$PATH:$HOME/flutter/bin"
flutter doctor
```

### Android SDK issues
```bash
flutter doctor --android-licenses
```

### Gradle build fails
```bash
cd android
./gradlew clean
cd ..
flutter clean
flutter pub get
flutter build apk
```

### Kotlin version conflicts
Verify in `android/build.gradle.kts`:
```kotlin
dependencies {
    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0"
}
```

### Plugin not found
```bash
flutter clean
flutter pub get
flutter pub upgrade
```

## CI/CD Integration

### GitHub Actions Example
```yaml
name: Build APK
on:
  push:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: subosito/flutter-action@v2
        with:
          flutter-version: '3.24.0'
      - run: flutter pub get
      - run: flutter build apk --release
      - uses: actions/upload-artifact@v3
        with:
          name: release-apk
          path: build/app/outputs/flutter-apk/app-release.apk
```

### Jenkins Pipeline Example
```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'flutter pub get'
                sh 'flutter build apk --release'
            }
        }
        stage('Archive') {
            steps {
                archiveArtifacts artifacts: 'build/app/outputs/flutter-apk/*.apk'
            }
        }
    }
}
```

## Version Management

### Semantic Versioning
Format: `MAJOR.MINOR.PATCH+BUILD`

- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes
- **BUILD**: Internal build number (must increment)

Example progression:
```
1.0.0+1  → Initial release
1.0.1+2  → Bug fix
1.1.0+3  → New feature
2.0.0+4  → Breaking change
```

### Update Process
1. Edit `pubspec.yaml`
2. Commit version change
3. Tag release: `git tag v1.0.0`
4. Build production APK
5. Upload to MDM
6. Deploy to device group

## Distribution

### Internal Distribution
- **MDM/EMM**: Workspace ONE, Intune, MobileIron
- **Email**: Send APK link to IT staff
- **Intranet**: Host on company server
- **Direct**: Install via USB/ADB

### No Google Play Required
This app does not require Play Store:
- Side-loading enabled
- Enterprise distribution only
- No Play Store review needed
- Full SMS/MMS access available

## Security Considerations

### APK Signing
- Always sign production builds
- Protect keystore file
- Use strong passwords
- Back up keystore securely

### Version Control
- Never commit keystore
- Add to `.gitignore`:
  ```
  *.jks
  *.keystore
  key.properties
  ```

## Support & Updates

### Rolling Out Updates
1. Build new version
2. Test on pilot device group
3. Deploy to wider audience
4. Monitor for issues
5. Provide rollback plan

### Emergency Hotfix
```bash
# Increment patch version
# pubspec.yaml: 1.0.0+1 → 1.0.1+2

flutter build apk --release
# Upload to MDM with high priority
```

## File Checklist Before Release

- [ ] Version number updated in `pubspec.yaml`
- [ ] Changelog documented
- [ ] All tests passing
- [ ] APK built and tested on real device
- [ ] SMS send/receive verified
- [ ] MMS send/receive verified
- [ ] Keystore backed up
- [ ] Release notes prepared
- [ ] MDM deployment configured
- [ ] Rollback plan documented

