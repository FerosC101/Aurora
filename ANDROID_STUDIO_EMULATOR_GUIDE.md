# Android Studio Emulator Setup Guide for Aurora

## Prerequisites
- Android Studio installed
- Aurora project building successfully (✅ VERIFIED)
- At least 8GB RAM and 10GB free disk space

## Step 1: Open Project in Android Studio

1. Launch **Android Studio**
2. Click **Open** (or File → Open)
3. Navigate to: `d:\coding\Kotlin\Aurora`
4. Click **OK**
5. Wait for Gradle sync to complete (check bottom-right status bar)

## Step 2: Create an Android Virtual Device (AVD)

### Method A: Using Device Manager (Recommended)
1. Click the **Device Manager** icon in the top toolbar (phone icon)
   - Or go to: **Tools → Device Manager**

2. Click **Create Device** (+ icon)

3. **Select Hardware:**
   - Choose a device profile (Recommended: **Pixel 5** or **Pixel 6**)
   - Click **Next**

4. **Select System Image:**
   - Choose API Level **34** (Android 14.0) - Matches your targetSdk
   - If not downloaded, click **Download** next to the system image
   - Recommended: **x86_64** architecture for better performance
   - Click **Next**

5. **Verify Configuration:**
   - AVD Name: Keep default or name it "Aurora_Emulator"
   - Startup orientation: Portrait
   - Enable **Device Frame** (optional)
   - Graphics: **Hardware - GLES 2.0** (for better performance)
   - Click **Finish**

### Method B: Quick Setup
1. Top toolbar: Click dropdown next to ▶ (Run button)
2. Select **Device Manager**
3. Follow steps 2-5 from Method A

## Step 3: Configure Emulator Settings (Optional but Recommended)

1. In Device Manager, click **⋮** (three dots) next to your emulator
2. Select **Edit**
3. Click **Show Advanced Settings**

### Recommended Settings:
- **Camera:** Webcam0/VirtualScene (for testing location features)
- **Network:** Speed: Full, Latency: None
- **Emulated Performance:**
  - Graphics: Hardware - GLES 2.0
  - Boot option: Cold boot (or Quick boot for faster restarts)
- **Memory and Storage:**
  - RAM: 2048 MB (minimum), 4096 MB (recommended)
  - VM heap: 512 MB
  - Internal Storage: 6000 MB (minimum)
  - SD card: 512 MB (optional)

4. Click **Finish** to save changes

## Step 4: Run Aurora App on Emulator

### First Time Run:
1. Make sure your emulator is created (check Device Manager)
2. In Android Studio toolbar:
   - Select **androidApp** from the configuration dropdown (if not selected)
   - Select your emulator from the device dropdown
3. Click the **▶ Run** button (green play icon) or press **Shift+F10**
4. Wait for:
   - Emulator to boot (first boot takes 2-5 minutes)
   - App to install and launch

### Subsequent Runs:
1. If emulator is not running, start it from Device Manager
2. Click **▶ Run** button or press **Shift+F10**
3. App will reinstall and launch (much faster)

## Step 5: Using the Emulator

### Basic Controls:
- **Power:** Side button (turns screen on/off)
- **Volume:** Volume up/down buttons
- **Rotate:** Ctrl+Arrow keys or toolbar rotate button
- **Back:** Back button or Esc key
- **Home:** Home button
- **Recent Apps:** Square button

### Testing Aurora Features:
1. **Login/Register:**
   - Test user registration with email validation
   - Test login functionality

2. **Navigation:**
   - Use emulator's extended controls for location testing
   - Click **⋮** → **Extended controls** → **Location**

3. **Network Testing:**
   - Extended controls → **Cellular** → Set signal strength
   - Extended controls → **Settings** → Proxy (for network debugging)

### Emulator Extended Controls (Important!):
- Click **⋮** (three dots) on emulator sidebar
- **Location:** Set GPS coordinates for testing maps
- **Cellular:** Simulate network conditions
- **Battery:** Test different battery levels
- **Phone:** Simulate calls/SMS
- **Camera:** Test camera features
- **Settings:** Advanced configurations

## Step 6: Debugging

### View Logs (Logcat):
1. Bottom toolbar: Click **Logcat** tab
2. Filter by:
   - Package: `org.aurora.android`
   - Log level: Info, Debug, Warning, or Error
3. Search for specific messages or errors

### Debug Mode:
1. Click **🐛 Debug** button instead of Run
2. Set breakpoints: Click left gutter next to line numbers
3. Use debug controls: Step Over (F8), Step Into (F7), Resume (F9)

## Step 7: Install APK on Physical Phone

### Generate APK:
1. In VS Code terminal (current location):
   ```powershell
   .\gradlew.bat :androidApp:assembleDebug
   ```
2. APK location: `d:\coding\Kotlin\Aurora\androidApp\build\outputs\apk\debug\androidApp-debug.apk`

### Install on Phone:
**Method 1: USB Connection**
1. Enable **Developer Options** on your phone:
   - Go to Settings → About Phone
   - Tap **Build Number** 7 times
   - Developer Options will appear in Settings

2. Enable **USB Debugging**:
   - Settings → Developer Options → USB Debugging: ON

3. Connect phone via USB cable

4. In VS Code terminal:
   ```powershell
   adb install "d:\coding\Kotlin\Aurora\androidApp\build\outputs\apk\debug\androidApp-debug.apk"
   ```
   Or in Android Studio:
   - Select your phone from device dropdown
   - Click **▶ Run**

**Method 2: Direct Install**
1. Copy APK file to your phone (via USB, Bluetooth, or cloud storage)
2. On your phone, open the APK file
3. Allow "Install from Unknown Sources" if prompted
4. Click **Install**

### Verify Installation:
- Look for "Aurora" app icon in your app drawer
- Launch and test functionality

## Troubleshooting

### Emulator Won't Start:
- Check BIOS: Enable **Intel VT-x** or **AMD-V** (virtualization)
- Windows: Disable Hyper-V if using Intel HAXM
- Restart Android Studio and computer
- Try creating a new AVD with different API level

### App Crashes:
- Check Logcat for error messages
- Verify build.gradle.kts configurations
- Clean and rebuild: `.\gradlew.bat clean :androidApp:assembleDebug`

### Slow Performance:
- Allocate more RAM to emulator (step 3)
- Use x86_64 system images instead of ARM
- Enable Hardware Graphics (GLES 2.0)
- Close other applications

### Build Errors:
- Sync Gradle: File → Sync Project with Gradle Files
- Invalidate caches: File → Invalidate Caches → Invalidate and Restart
- Check for Android SDK updates: Tools → SDK Manager

### Disk Space Issues:
- Your C: drive has limited space - Gradle cache moved to D: drive
- Clean old builds: `.\gradlew.bat clean`
- Delete old AVDs you don't use

## Performance Tips

1. **Use Quick Boot:**
   - AVD Edit → Show Advanced Settings → Boot Option → Quick Boot
   - Saves emulator state for faster restarts

2. **Keep Emulator Running:**
   - Don't close emulator between app tests
   - Just click Run to reinstall app

3. **Use Hardware Acceleration:**
   - Ensure Intel HAXM or AMD Hypervisor is installed
   - Check: SDK Manager → SDK Tools → Intel x86 Emulator Accelerator (HAXM)

4. **Optimize RAM Usage:**
   - Close unnecessary Android Studio plugins
   - Reduce emulator RAM if you have limited system RAM
   - Monitor with Task Manager

## Aurora App Features to Test

1. **Authentication:**
   - Register new user
   - Login with credentials
   - Logout functionality

2. **User Interface:**
   - Theme and styling
   - Navigation between screens
   - Form validation

3. **Navigation Features:**
   - Location services (use extended controls)
   - Map integration
   - Route planning

4. **Data Persistence:**
   - User data storage
   - Session management
   - Database operations

## Next Steps After Successful Testing

1. **Production Build:**
   ```powershell
   .\gradlew.bat :androidApp:assembleRelease
   ```
   - Requires signing key configuration

2. **Optimize Performance:**
   - Enable ProGuard/R8 code shrinking
   - Optimize resource usage
   - Test on multiple Android versions

3. **Prepare for Play Store:**
   - Create signing key
   - Configure release build
   - Generate signed APK/AAB

## Quick Reference Commands

```powershell
# Build debug APK
.\gradlew.bat :androidApp:assembleDebug

# Clean build
.\gradlew.bat clean

# Install on connected device
adb install path\to\app.apk

# View connected devices
adb devices

# Uninstall app
adb uninstall org.aurora.android

# View app logs
adb logcat -s "Aurora"
```

## Important Notes

✅ **Build Status:** Your Aurora project builds successfully!  
✅ **APK Location:** `androidApp\build\outputs\apk\debug\androidApp-debug.apk`  
✅ **Package Name:** `org.aurora.android`  
✅ **Min SDK:** Android 7.0 (API 24)  
✅ **Target SDK:** Android 14 (API 34)  

⚠️ **Disk Space:** C: drive was almost full. Gradle cache moved to D: drive at `D:\.gradle`

## Support & Documentation

- **Android Studio:** https://developer.android.com/studio
- **Emulator Guide:** https://developer.android.com/studio/run/emulator
- **ADB Documentation:** https://developer.android.com/tools/adb
- **Jetpack Compose:** https://developer.android.com/jetpack/compose

---

**Ready to develop!** Your Aurora app is configured and ready for testing in Android Studio. Happy coding! 🚀
