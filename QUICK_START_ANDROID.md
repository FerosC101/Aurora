# Quick Start - Running Aurora on Android Emulator

## Summary of Changes Made

I've configured the Android app to work as a standalone application (not using the shared multiplatform module yet due to Gradle compatibility issues). The Android app has its own UI implementation.

## Prerequisites

1. **Android Studio** - Download from https://developer.android.com/studio
2. **JDK 17** - Usually bundled with Android Studio  
3. **Android SDK Platform 34** - Install via Android Studio SDK Manager

## Step-by-Step Instructions

### 1. Open Project in Android Studio

1. Launch Android Studio
2. Click **Open** → Navigate to `D:\coding\Kotlin\Aurora`
3. Click **OK**
4. Wait for Gradle sync (may take 5-10 minutes first time)

### 2. Create Android Virtual Device (AVD)

1. Click **Device Manager** icon (phone icon in toolbar)
2. Click **Create Device**
3. Select **Pixel 5** or **Pixel 6**
4. Choose **API 34** (Android 14) system image
   - If not installed, click **Download**
5. Click **Next** → **Finish**

### 3. Sync and Build

1. Click **File → Sync Project with Gradle Files**
2. Wait for sync to complete
3. Build: **Build → Rebuild Project** (Ctrl+F9)

### 4. Run on Emulator

1. Select **androidApp** from the run configuration dropdown (top toolbar)
2. Select your emulator device from the device dropdown  
3. Click the green **Run** button (▶️) or press Shift+F10
4. Wait for emulator to boot (1-2 minutes first time)
5. App will install and launch automatically

## Alternative: Build from Terminal

```powershell
# Build the APK
.\gradlew :androidApp:assembleDebug

# Install on running emulator
.\gradlew :androidApp:installDebug

# APK location:
# androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

## Running on Physical Phone

### Enable Developer Mode:
1. Settings → About Phone
2. Tap **Build Number** 7 times
3. Settings → Developer Options
4. Enable **USB Debugging**

### Install:
1. Connect phone via USB
2. Allow USB debugging on phone
3. In Android Studio: Select your phone from device dropdown
4. Click Run

## App Features

The Android app includes:
- ✅ Login/Registration system
- ✅ SQLite local database
- ✅ Home screen with simulation
- ✅ Mini map view
- ✅ Congestion alerts
- ✅ Strategy cards

## Troubleshooting

### "SDK location not found"
Create `local.properties` in project root:
```
sdk.dir=C\:\\Users\\[YourUsername]\\AppData\\Local\\Android\\Sdk
```

### Emulator won't start
- Enable VT-x/AMD-V in BIOS
- Windows: Disable Hyper-V (`bcdedit /set hypervisorlaunchtype off`)

### Build fails
```powershell
.\gradlew clean
.\gradlew :androidApp:assembleDebug --refresh-dependencies
```

### App crashes
- Check Logcat (bottom panel in Android Studio)
- Wipe emulator data: Device Manager → Actions → Wipe Data

## Project Structure

```
androidApp/
├── src/main/
│   ├── kotlin/org/aurora/android/
│   │   ├── MainActivity.kt          # Entry point
│   │   ├── ui/                      # UI screens
│   │   ├── auth/                    # Authentication
│   │   ├── viewmodel/               # ViewModels
│   │   └── theme/                   # Material3 theme
│   ├── res/                         # Resources
│   └── AndroidManifest.xml          # App configuration
└── build.gradle.kts                 # Dependencies
```

## Configuration

### Versions:
- Kotlin: 1.9.21
- Android Gradle Plugin: 8.2.0
- Compose: 1.5.7
- Min SDK: 24 (Android 7.0)
- Target SDK: 34 (Android 14)

### Permissions (AndroidManifest.xml):
- INTERNET - For API calls
- ACCESS_FINE_LOCATION - For GPS navigation
- ACCESS_COARSE_LOCATION - For approximate location
- ACCESS_NETWORK_STATE - For connectivity checks

## Next Steps After Successful Build

1. **Test the app** - Try login, registration, and navigation
2. **Customize UI** - Edit colors in `theme/Color.kt`
3. **Add features** - Extend the HomeScreen or add new screens
4. **Debug** - Use Logcat and breakpoints

## Known Issues

- Shared multiplatform module currently disabled for Android due to Gradle compatibility
- Working on integrating the full desktop UI features into Android
- Some desktop-specific features (like JavaFX WebView) not available on Android

## Support Resources

- Android Developer Guide: https://developer.android.com/guide
- Jetpack Compose: https://developer.android.com/jetpack/compose
- Kotlin Multiplatform: https://kotlinlang.org/docs/multiplatform.html

---

**Last Updated:** December 9, 2025

For detailed instructions, see `ANDROID_STUDIO_SETUP.md`
