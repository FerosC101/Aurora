# Installing Aurora on Your Phone

Due to Kotlin Multiplatform and Android Gradle Plugin compatibility issues in the build system, the easiest way to run Aurora on your phone is using Android Studio.

## Option 1: Install via Android Studio (Recommended)

### Prerequisites
- Install [Android Studio](https://developer.android.com/studio)
- Enable Developer Options on your phone:
  1. Go to **Settings** > **About Phone**
  2. Tap **Build Number** 7 times
  3. Go back to **Settings** > **Developer Options**
  4. Enable **USB Debugging**

### Steps
1. Open Android Studio
2. Select **Open an Existing Project**
3. Navigate to `D:\coding\Kotlin\Aurora`
4. Wait for Gradle sync to complete
5. Connect your phone via USB
6. Click the green **Run** button (▶️) or press `Shift+F10`
7. Select your phone from the device list
8. Android Studio will automatically build and install the app

## Option 2: Manual APK Installation (if Gradle build works)

If you manage to fix the build issues:

```powershell
# Build the APK
./gradlew androidApp:assembleDebug

# The APK will be created at:
# androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

Then transfer to your phone and install:
1. Copy `androidApp-debug.apk` to your phone
2. Open the file on your phone
3. Allow installation from unknown sources if prompted
4. Install and run

## Current Build Issue

The project has a Kotlin Multiplatform + Android Gradle Plugin compatibility issue:
- Error: `NoClassDefFoundError: com/android/build/gradle/api/BaseVariant`
- Cause: AGP 8.x removed the BaseVariant API, but Kotlin plugins still reference it

### Workaround
Use Android Studio which handles these compatibility issues automatically through its own build system integration.

## Authentication Features

Once installed, Aurora includes:
- ✅ **Login Screen** with email/password
- ✅ **Registration** with full name, email, password confirmation
- ✅ **SQLite Database** for user storage
- ✅ **BCrypt Password Hashing** for security
- ✅ **Tab-based UI** matching the design specs
- ✅ **User Dashboard** after successful login

### Test Credentials
Create a new account using the registration screen. The app uses a local SQLite database, so your data stays on your device.
