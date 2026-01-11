# iOS App Setup Guide

## Prerequisites
You **must** have a Mac with Xcode to build and run the iOS app.

## Quick Start

### 1. Enable Shared Module
First, enable the shared module in `settings.gradle.kts`:
```kotlin
include(":shared")
```

### 2. Build Kotlin Framework
From the project root on your Mac:
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### 3. Install CocoaPods
```bash
sudo gem install cocoapods
cd iosApp
pod install
```

### 4. Open in Xcode
```bash
open iosApp.xcworkspace
```

### 5. Configure Firebase
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Add iOS app to your Aurora project
3. Download `GoogleService-Info.plist`
4. Drag it into Xcode (iosApp target)

### 6. Add Google Maps API Key
1. Get iOS API key from [Google Cloud Console](https://console.cloud.google.com)
2. Add to `Info.plist`:
```xml
<key>GMSApiKey</key>
<string>YOUR_IOS_API_KEY_HERE</string>
```

### 7. Build & Run
- Select a simulator or device
- Press **Cmd + R**

## Current Status
✅ Basic project structure created  
✅ iOS targets added to shared module  
✅ CocoaPods configuration  
⏳ Awaiting full implementation

## Next Steps
See `iosApp/README.md` for detailed implementation roadmap.

## Need Help?
- iOS development requires a Mac
- Xcode 15.0+ required
- Apple Developer account needed for device testing
