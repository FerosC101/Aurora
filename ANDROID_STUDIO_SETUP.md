# Aurora - Android Studio Setup Guide

## Prerequisites

Before you begin, make sure you have:

1. **Android Studio** (Latest version - Arctic Fox or newer recommended)
   - Download from: https://developer.android.com/studio
   
2. **JDK 17** (Required for Kotlin 2.0 and AGP 8.5)
   - Android Studio usually comes with a bundled JDK
   - Verify: File → Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK
   
3. **Android SDK** with the following components:
   - Android SDK Platform 34 (Android 14)
   - Android SDK Build-Tools 34.0.0+
   - Android Emulator
   - Intel x86 Emulator Accelerator (HAXM) or Android Emulator Hypervisor Driver for AMD

## Step-by-Step Setup Instructions

### Step 1: Open Project in Android Studio

1. Launch **Android Studio**
2. Click **Open** (or File → Open)
3. Navigate to `D:\coding\Kotlin\Aurora`
4. Click **OK**
5. Wait for Android Studio to index the project and sync Gradle (this may take a few minutes on first load)

### Step 2: Configure Android SDK

1. Go to **File → Project Structure** (or press `Ctrl+Alt+Shift+S`)
2. Under **Project**, verify:
   - **SDK:** Android API 34 (or higher)
   - **Language level:** 17 - Sealed types, always-strict floating-point semantics
3. Under **Modules → androidApp**, verify:
   - **Compile SDK Version:** 34
   - **Min SDK Version:** 24 (Android 7.0+)
   - **Target SDK Version:** 34

### Step 3: Create Android Emulator (Virtual Device)

1. Click the **Device Manager** icon in the top right toolbar (phone icon)
   - Or go to **Tools → Device Manager**

2. Click **Create Device** button

3. **Select Hardware:**
   - Choose a device definition (recommended: **Pixel 5** or **Pixel 6**)
   - Click **Next**

4. **Select System Image:**
   - Choose a system image (recommended: **API 34** with **x86_64** ABI)
   - If not downloaded, click **Download** next to the image
   - **Recommended images:**
     - **Tiramisu** (API 33) - Android 13
     - **UpsideDownCake** (API 34) - Android 14
   - Choose one with **Google APIs** for full functionality
   - Click **Next**

5. **Configure AVD:**
   - Give it a name (e.g., "Pixel 5 API 34")
   - Verify settings:
     - **Startup orientation:** Portrait
     - **Graphics:** Automatic (or Hardware - GLES 2.0 if available)
   - Click **Finish**

### Step 4: Sync Gradle and Build Project

1. Click **File → Sync Project with Gradle Files**
   - Or click the elephant icon with a blue arrow in the toolbar
   - Wait for sync to complete (first time may take 5-10 minutes)

2. **If you see any errors:**
   - Check the "Build" tab at the bottom
   - Most common fixes:
     - Update Gradle: Click the suggestion to update if prompted
     - Accept SDK licenses: Run `./gradlew --stop` then `./gradlew :androidApp:assembleDebug`

3. Once sync is successful, build the project:
   - Go to **Build → Rebuild Project**
   - Or press `Ctrl+F9`
   - Wait for build to complete

### Step 5: Run on Emulator

1. **Select the androidApp configuration:**
   - In the top toolbar, click the dropdown next to the green play button
   - Select **androidApp** (if not already selected)

2. **Select your emulator:**
   - Click the device dropdown next to the configuration
   - Select the emulator you created (e.g., "Pixel 5 API 34")

3. **Run the app:**
   - Click the green **Run** button (▶️) or press `Shift+F10`
   - The emulator will start (first launch takes 1-2 minutes)
   - The app will automatically install and launch

4. **Wait for the app to appear:**
   - Once the emulator boots and shows the home screen
   - Your Aurora app should launch automatically
   - You'll see the login screen with the white/blue design

### Step 6: Testing the App

1. **Login Screen:**
   - The app starts at the login screen
   - You can use test credentials or register a new account

2. **Home Screen Features:**
   - Large pulsating circle button for "Optimize Travel"
   - Route planning form with origin/destination inputs
   - Travel mode selector (Bike/Walk/Car/Bus)
   - Quick access icons for Routes, History, Favorites, Safety

3. **Navigation Features:**
   - Enter origin (e.g., "Manila")
   - Enter destination (e.g., "Makati")
   - Select travel mode
   - Click "Find Routes"
   - View route options with interactive maps
   - Select a route and start navigation

## Troubleshooting

### Build Fails with "SDK location not found"

**Solution:**
1. Create a `local.properties` file in the project root
2. Add: `sdk.dir=C\:\\Users\\[YourUsername]\\AppData\\Local\\Android\\Sdk`
   (Replace with your actual SDK path)

### Emulator Won't Start

**Solution:**
1. Check BIOS settings - Ensure virtualization (VT-x/AMD-V) is enabled
2. For Windows: Disable Hyper-V if using Intel HAXM
   - Run: `bcdedit /set hypervisorlaunchtype off` (requires admin)
   - Restart computer
3. Try different system image (x86 vs x86_64)

### App Crashes on Launch

**Solution:**
1. Check Logcat (bottom panel → Logcat tab)
2. Filter by "Error" or "Aurora"
3. Common fixes:
   - Clear app data: Long press app icon → App info → Storage → Clear data
   - Wipe emulator data: Device Manager → Actions (⋮) → Wipe Data
   - Rebuild: Build → Clean Project, then Build → Rebuild Project

### Gradle Sync Fails

**Solution:**
1. Check internet connection (Gradle downloads dependencies)
2. Invalidate caches: File → Invalidate Caches → Invalidate and Restart
3. Delete `.gradle` and `.idea` folders, then reopen project
4. Run in terminal: `.\gradlew clean build --refresh-dependencies`

### "Could not resolve org.aurora:shared" Error

**Solution:**
1. Ensure the `shared` module is built: `.\gradlew :shared:build`
2. Sync Gradle again
3. Check that `settings.gradle.kts` includes `include(":shared")`

## Running from Terminal (Alternative Method)

If Android Studio has issues, you can build and install from terminal:

```powershell
# Build the APK
.\gradlew :androidApp:assembleDebug

# Install on connected device/emulator
.\gradlew :androidApp:installDebug

# Or combine both
.\gradlew :androidApp:installDebug
```

The APK will be located at:
`androidApp\build\outputs\apk\debug\androidApp-debug.apk`

## Running on Physical Android Phone

### Prerequisites:
1. Android phone with Android 7.0 (API 24) or higher
2. USB cable
3. Developer mode enabled on phone

### Steps:

1. **Enable Developer Options on Phone:**
   - Go to **Settings → About Phone**
   - Tap **Build Number** 7 times
   - Go back to Settings → **Developer Options**
   - Enable **USB Debugging**

2. **Connect Phone to Computer:**
   - Connect via USB cable
   - On phone, tap **Allow** when prompted for USB debugging
   - Check "Always allow from this computer"

3. **Verify Connection:**
   - In Android Studio, click the device dropdown
   - Your phone should appear (e.g., "Samsung SM-G991B")
   - Or run in terminal: `adb devices`

4. **Run on Phone:**
   - Select your phone from the device dropdown
   - Click the green Run button
   - App will install and launch on your phone

### Permissions on Phone:
When the app first launches, it will request:
- **Location** - Allow "While using the app" for navigation features
- **Internet** - Automatically granted

## Project Structure

```
Aurora/
├── androidApp/          # Android-specific app code
│   ├── src/main/
│   │   ├── kotlin/     # MainActivity and Android UI
│   │   ├── res/        # Android resources (strings, themes, icons)
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── shared/             # Shared Kotlin Multiplatform code
│   ├── src/
│   │   ├── commonMain/  # Platform-independent code
│   │   ├── androidMain/ # Android-specific implementations
│   │   └── desktopMain/ # Desktop-specific implementations
│   └── build.gradle.kts
├── desktopApp/         # Desktop application
└── settings.gradle.kts # Project configuration
```

## Features Available on Android

✅ **Available:**
- Login/Registration
- Home screen with route planning
- Google Maps integration for real routes
- Interactive route selection with maps
- Travel mode selection (bike/walk/car/bus)
- Route visualization
- Navigation UI

⚠️ **Limited:**
- Some desktop-specific features may not be available
- Map interactions adapted for touch input

## Configuration Files

### Key Files to Know:

1. **`local.properties`** - SDK location (git-ignored)
2. **`gradle.properties`** - Gradle configuration
3. **`settings.gradle.kts`** - Project modules and plugin versions
4. **`androidApp/build.gradle.kts`** - Android app dependencies
5. **`shared/build.gradle.kts`** - Shared module configuration

## Next Steps

After successfully running the app:

1. **Explore the Features:**
   - Try different travel modes
   - Test route planning with real locations
   - Check the interactive maps

2. **Customize:**
   - Modify colors in `androidApp/src/main/kotlin/org/aurora/android/theme/Color.kt`
   - Update app name in `androidApp/src/main/res/values/strings.xml`
   - Change app icon in `androidApp/src/main/res/mipmap-*/`

3. **Debug:**
   - Use Logcat for runtime logs
   - Set breakpoints in code and use debugger
   - Use Layout Inspector (Tools → Layout Inspector) to inspect UI

## Useful Android Studio Shortcuts

- **Run app:** `Shift + F10`
- **Debug app:** `Shift + F9`
- **Build project:** `Ctrl + F9`
- **Sync Gradle:** `Ctrl + Shift + O` (or toolbar elephant icon)
- **Find in project:** `Ctrl + Shift + F`
- **Go to class:** `Ctrl + N`
- **Recent files:** `Ctrl + E`

## Support

If you encounter issues:

1. Check the "Build" and "Logcat" panels in Android Studio
2. Search error messages on Stack Overflow
3. Review Android Studio documentation: https://developer.android.com/studio/intro
4. Check Kotlin Multiplatform documentation: https://www.jetbrains.com/kotlin-multiplatform/

## Version Information

- **Kotlin:** 2.0.0
- **Compose Multiplatform:** 1.6.11
- **Android Gradle Plugin:** 8.5.2
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 34 (Android 14)
- **Gradle:** 8.9

---

**Last Updated:** December 9, 2025
