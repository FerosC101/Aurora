# Quick Phone Installation Guide - No Android Studio Needed!

## Fastest Way to Test Aurora on Your Phone

### Prerequisites
- USB cable
- Android phone with Developer Mode enabled

## Step 1: Enable Developer Mode on Your Phone

1. Go to **Settings** → **About Phone**
2. Find **Build Number** (might be under Software Information)
3. Tap **Build Number** 7 times rapidly
4. You'll see "You are now a developer!"
5. Go back to **Settings** → **Developer Options** (or System → Developer Options)
6. Enable **USB Debugging**

## Step 2: Connect Phone to Computer

1. Connect phone via USB cable
2. On your phone, you'll see "Allow USB Debugging?" popup
3. Check "Always allow from this computer"
4. Tap **Allow**

## Step 3: Verify Connection (VS Code Terminal)

```powershell
# Check if phone is detected
adb devices
```

**Expected output:**
```
List of devices attached
ABC123XYZ    device
```

If you see "unauthorized", unplug and replug the cable, then allow on phone.

## Step 4: Build and Install APK

### Option A: Build and Install in One Command
```powershell
# Build fresh APK and install
.\gradlew.bat :androidApp:assembleDebug ; adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

### Option B: Quick Install (if already built)
```powershell
# Just install existing APK
adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

The `-r` flag reinstalls the app (keeps data).

## Step 5: Launch App on Phone

**Manual:** Find "Aurora" app in your app drawer and launch

**Via ADB (auto-launch):**
```powershell
adb shell am start -n org.aurora.android/.MainActivity
```

## Development Workflow (No Android Studio!)

### 1. Make Code Changes in VS Code
Edit your Kotlin files as needed.

### 2. Build & Deploy
```powershell
# Quick rebuild and install
.\gradlew.bat :androidApp:assembleDebug ; adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

### 3. View Live Logs
```powershell
# See all logs from Aurora app
adb logcat -s "AndroidRuntime:E" "*:W" | Select-String "aurora"

# Or cleaner output
adb logcat | Select-String "org.aurora"
```

### 4. Clear App Data (if needed)
```powershell
# Clear app data and cache
adb shell pm clear org.aurora.android
```

### 5. Uninstall (if needed)
```powershell
adb uninstall org.aurora.android
```

## Fast Iteration Tips

### Keep ADB Connection Active
Once phone is connected, keep it plugged in. You can:
1. Edit code in VS Code
2. Run build command
3. App auto-installs
4. Manually reopen on phone (takes 10 seconds total)

### Use PowerShell Aliases (Optional)
Add to your PowerShell profile for faster commands:

```powershell
# Create aliases
function Build-Aurora { .\gradlew.bat :androidApp:assembleDebug }
function Install-Aurora { adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk }
function Deploy-Aurora { Build-Aurora; Install-Aurora }
function Logs-Aurora { adb logcat | Select-String "org.aurora" }

# Usage:
Deploy-Aurora  # Builds and installs
Logs-Aurora    # Shows logs
```

## Debugging Without Android Studio

### View Crash Logs
```powershell
# See crash reports
adb logcat -b crash
```

### Monitor in Real-Time
```powershell
# Clear old logs and watch new ones
adb logcat -c ; adb logcat -s "AndroidRuntime" | Select-String "aurora|Exception|Error"
```

### Screen Recording (for bug reports)
```powershell
# Record screen
adb shell screenrecord /sdcard/aurora_test.mp4

# Stop recording (Ctrl+C)
# Pull video to computer
adb pull /sdcard/aurora_test.mp4 .
```

### Take Screenshots
```powershell
# Capture screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png .
```

## Wireless Debugging (Optional - Android 11+)

### Enable Wireless ADB:
1. Phone and PC on same WiFi network
2. Phone: Settings → Developer Options → Wireless Debugging → ON
3. Tap "Pair device with pairing code"
4. Note the IP and port (e.g., 192.168.1.5:37171)

```powershell
# Pair (one-time)
adb pair 192.168.1.5:37171
# Enter pairing code from phone

# Connect
adb connect 192.168.1.5:37171

# Now you can unplug USB!
```

## Troubleshooting

### "adb: command not found"
ADB is located in Android SDK:
```powershell
# Add to PATH or use full path
$env:PATH += ";D:\Android\Sdk\platform-tools"
```

### "device unauthorized"
1. Unplug USB cable
2. Phone: Revoke USB debugging authorizations
3. Replug cable
4. Allow when prompted

### "INSTALL_FAILED_UPDATE_INCOMPATIBLE"
Previous version with different signature:
```powershell
# Uninstall first
adb uninstall org.aurora.android
# Then install
adb install androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

### Phone Not Detected
1. Try different USB cable (data cable, not just charging)
2. Try different USB port
3. Install phone drivers (usually automatic)
4. Windows: Check Device Manager for driver issues

## Performance Comparison

| Method | First Time | Subsequent | Complexity |
|--------|-----------|------------|------------|
| **Android Studio** | 10-15 min | 2-3 min | High |
| **IntelliJ IDEA** | 8-12 min | 1-2 min | Medium |
| **VS Code + ADB** | 3-5 min | 30-60 sec | Low |
| **Physical Phone Only** | 2-3 min | 20-40 sec | Lowest |

## Why This is Faster

✅ No emulator overhead  
✅ No IDE indexing (just VS Code)  
✅ Direct APK installation  
✅ Real device performance  
✅ Actual hardware testing  
✅ No virtualization layer  

## Recommended Setup for You

**Development:**
1. Code in **VS Code** (lightweight, fast)
2. Build with Gradle in terminal
3. Test on **physical phone** (real performance)
4. Use **adb logcat** for debugging

**Only use Android Studio/IntelliJ when you need:**
- Complex debugging with breakpoints
- Layout preview/editor
- Profiling tools
- Managing multiple AVDs

## Quick Command Reference

```powershell
# Build
.\gradlew.bat :androidApp:assembleDebug

# Install
adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk

# Build + Install
.\gradlew.bat :androidApp:assembleDebug ; adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk

# Launch
adb shell am start -n org.aurora.android/.MainActivity

# Logs
adb logcat | Select-String "org.aurora"

# Clear data
adb shell pm clear org.aurora.android

# Uninstall
adb uninstall org.aurora.android

# Check devices
adb devices

# Restart ADB (if issues)
adb kill-server ; adb start-server
```

---

**This is the fastest way to develop and test Aurora!** No waiting for Android Studio sync. Just code, build, install, test! 🚀
