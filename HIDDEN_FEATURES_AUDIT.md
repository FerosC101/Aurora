# Hidden Features Audit Report
**Generated:** January 8, 2026

## Summary
Comprehensive audit of Aurora app features to identify UI visibility issues and missing access points.

---

## ✅ Features with Good Visibility

### 1. **Cost Tracking** - FIXED ✅
- **Location:** Activity Screen → Monthly Costs card
- **Status:** Fixed - now always visible with empty state
- **Access:** Card is clickable to add manual costs
- **Discovery:** Easy - visible on main Activity tab

### 2. **Friend Location Sharing** - VISIBLE ✅
- **Location:** Profile → Friends menu option
- **Status:** Good - clear menu item with description
- **Access:** RealNavigationScreen → Share/Friends buttons during navigation
- **Discovery:** Easy - appears in Profile preferences

### 3. **Weather Display** - VISIBLE ✅
- **Location:** RealNavigationScreen during active navigation
- **Status:** Good - shows weather emoji and alerts automatically
- **Access:** Automatic during navigation (updates every 5 minutes)
- **Discovery:** Easy - visible during navigation

### 4. **Departure Reminders** - VISIBLE ✅
- **Location:** HomeScreen → "Set Departure Reminder" button
- **Status:** Good - button appears when destination is entered
- **Access:** Clear button with notification icon
- **Discovery:** Easy - shown after entering destination

---

## ⚠️ Features with Visibility Issues

### 5. **Driving Behavior Analytics** - HIDDEN ⚠️

**Problem:**
- Service exists: `DrivingBehaviorAnalyzer.kt` (126 lines)
- Features:
  - Harsh braking detection
  - Rapid acceleration tracking
  - Speeding incident counting
  - Smooth driving score (0-100)
- **NOT displayed anywhere in the UI**

**Current Display:**
- ActivityScreen shows "Driving Score: 85" (hardcoded)
- Says "Smooth and safe driving!" but this is mock text
- Score is not actually calculated from real driving data

**What's Missing:**
```kotlin
// ActivityScreen.kt lines 310-335
Text(text = if (tripHistory.isEmpty()) "--" else "85", ...)
// ^ This is hardcoded, not using DrivingBehaviorAnalyzer
```

**Solution Needed:**
1. Integrate `DrivingBehaviorAnalyzer` into `RealNavigationScreen`
2. Save behavior data per trip in `TripRecord`
3. Display real metrics in ActivityScreen:
   - Harsh braking count
   - Rapid acceleration count
   - Speeding incidents
   - Real calculated score

**Impact:** HIGH - Users can't see their actual driving behavior

---

### 6. **Route Change Alerts** - PARTIALLY HIDDEN ⚠️

**Problem:**
- Service exists: `RouteChangeAlertService.kt` (158 lines)
- Features:
  - Monitors planned route vs actual location
  - Detects route deviations
  - Creates WorkManager background task
  - Shows notification when off-route
- **Only visible as notification, no in-app display**

**Current Integration:**
- Service is implemented with WorkManager
- Sends system notifications when off-route
- **No visual indicator in navigation UI**

**What's Missing:**
- No on-screen "Off Route" warning in RealNavigationScreen
- No visual feedback when route recalculation happens
- No history of route deviations in trip summary

**Solution Needed:**
1. Add visual alert overlay in RealNavigationScreen
2. Show "Recalculating route..." message
3. Display deviation history in ActivityScreen trip details

**Impact:** MEDIUM - Feature works but users might miss notifications

---

### 7. **Traffic-Aware Navigation** - BACKGROUND ONLY ⚠️

**Problem:**
- Service exists: `TrafficAwareNavigationService.kt`
- Features:
  - Real-time traffic condition checks
  - Alternative route suggestions when traffic detected
  - Estimated delay calculations
- **No visual traffic indicators on map**
- **No traffic comparison in route options**

**What's Missing:**
- No traffic layer on map (red/yellow/green roads)
- Route comparison doesn't show traffic levels clearly
- No "Current traffic: Heavy" indicator

**Solution Needed:**
1. Add traffic polyline colors to map
2. Show traffic alerts in route preview
3. Display traffic comparison in AlternativeRoutesScreen

**Impact:** MEDIUM - Traffic data exists but not visualized

---

### 8. **Vehicle Profile Settings** - HARD TO FIND ⚠️

**Problem:**
- Service exists: `VehicleProfileService.kt`
- Features:
  - Multiple vehicle types (car, motorcycle, truck)
  - Custom fuel consumption rates
  - Vehicle-specific routing
- **No UI to select or configure vehicles**

**Current Status:**
- ProfileScreen has a "Vehicle Profile" placeholder
- No actual vehicle selection screen exists
- Cost calculations use default values

**What's Missing:**
- Vehicle selection dialog/screen
- Vehicle profile editor
- Visual indication of active vehicle

**Solution Needed:**
1. Create VehicleProfileDialog/Screen
2. Add vehicle selector in ProfileScreen
3. Show active vehicle in HomeScreen

**Impact:** HIGH - Users can't customize for their vehicle

---

### 9. **Parking Finder** - NOT ACCESSIBLE ⚠️

**Problem:**
- Service exists: `ParkingFinderService.kt`
- Features:
  - Finds nearby parking locations
  - Shows parking availability
  - Displays parking fees
- **No UI access point anywhere**

**What's Missing:**
- No button to find parking near destination
- No parking markers on map
- No parking info in destination preview

**Solution Needed:**
1. Add "Find Parking" button in route preview
2. Show parking markers on map near destination
3. Display parking options in bottom sheet

**Impact:** HIGH - Complete feature hidden from users

---

### 10. **Voice Navigation Settings** - NO CONTROLS ⚠️

**Problem:**
- Service exists: `VoiceNavigationService.kt`
- Features:
  - Text-to-speech announcements
  - Turn-by-turn voice guidance
  - Speed warning announcements
- **No UI to enable/disable or configure**

**Current Status:**
- Service is implemented
- Likely auto-enabled during navigation
- No volume control, no mute button

**What's Missing:**
- Voice on/off toggle in RealNavigationScreen
- Volume control
- Voice preview/test button
- Language selection

**Solution Needed:**
1. Add voice toggle button in navigation UI
2. Add voice settings in ProfileScreen
3. Show voice indicator when active

**Impact:** MEDIUM - Feature exists but no user control

---

### 11. **AI Route Assistant** - COMPLETELY HIDDEN ⚠️

**Problem:**
- Service exists: `RouteAssistantService.kt` + `GeminiAIService.kt`
- Features:
  - AI-powered route recommendations
  - Natural language route queries
  - Smart suggestions based on history
- **No UI integration at all**

**What's Missing:**
- No chat interface
- No AI suggestions button
- No "Ask AI" feature anywhere

**Solution Needed:**
1. Add AI assistant button in HomeScreen
2. Create AI chat dialog/screen
3. Show AI suggestions in route planning

**Impact:** CRITICAL - Major feature completely inaccessible

---

### 12. **Hazard Detection** - NO VISUAL FEEDBACK ⚠️

**Problem:**
- Service exists: `HazardDetectionService.kt`
- Features:
  - Detects sharp curves
  - Identifies steep hills
  - Warns about narrow roads
- **Works but shows generic "Hazard Ahead" alert**

**Current Status:**
- ActivityScreen shows "Hazards Avoided: X" count
- Alert dialog appears (line 94 in RealNavigationScreen)
- No specific hazard type shown

**What's Missing:**
- Specific hazard icons (curve, hill, narrow road)
- Hazard markers on map
- Hazard details in alert

**Solution Needed:**
1. Show hazard icons on map ahead of location
2. Display specific hazard type in alert
3. Add hazard legend/guide

**Impact:** LOW - Feature works but could be clearer

---

## 🎯 Priority Recommendations

### CRITICAL Priority (Implement First)
1. **AI Route Assistant** - Complete feature hidden
2. **Parking Finder** - High-value feature not accessible
3. **Vehicle Profile Settings** - Affects cost calculations

### HIGH Priority
4. **Driving Behavior Analytics** - Display real data not mock scores
5. **Voice Navigation Controls** - Let users control announcements

### MEDIUM Priority
6. **Traffic Visualization** - Show traffic on map
7. **Route Change Visual Alerts** - On-screen indicators

### LOW Priority
8. **Hazard Details** - Improve specificity
9. **Better Discovery** - Add feature tour on first launch

---

## 📋 Quick Fixes

### Make Features More Discoverable

**Add to ProfileScreen:**
```kotlin
SettingsItem(
    icon = Icons.Default.Settings,
    title = "Voice Navigation",
    subtitle = "Configure voice announcements",
    onClick = { /* Show voice settings dialog */ }
)

SettingsItem(
    icon = Icons.Default.Place,
    title = "Parking Finder",
    subtitle = "Find parking near destinations",
    onClick = { /* Enable parking feature */ }
)

SettingsItem(
    icon = Icons.Default.Create,
    title = "Vehicle Profile",
    subtitle = "Configure your vehicle settings",
    onClick = { /* Show vehicle editor */ }
)

SettingsItem(
    icon = Icons.Default.Face,
    title = "AI Assistant",
    subtitle = "Get AI-powered route suggestions",
    onClick = { /* Show AI chat */ }
)
```

**Add to RealNavigationScreen during navigation:**
```kotlin
// Voice control button
IconButton(onClick = { toggleVoice() }) {
    Icon(
        if (voiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
        contentDescription = "Toggle voice"
    )
}

// Parking button near destination
if (remainingDistance < 1000) {
    Button(onClick = { showParkingOptions() }) {
        Icon(Icons.Default.LocalParking, ...)
        Text("Find Parking")
    }
}

// Traffic toggle
IconButton(onClick = { showTrafficLayer = !showTrafficLayer }) {
    Icon(Icons.Default.Traffic, ...)
}
```

**Add to HomeScreen:**
```kotlin
// AI Assistant button
FloatingActionButton(
    onClick = { showAIAssistant = true },
    backgroundColor = Color(0xFF9C27B0)
) {
    Icon(Icons.Default.Face, contentDescription = "AI Assistant")
}
```

---

## 📊 Feature Visibility Matrix

| Feature | Service Exists | UI Access | User Visibility | Priority |
|---------|---------------|-----------|-----------------|----------|
| Cost Tracking | ✅ | ✅ | ✅ Fixed | ✅ Done |
| Friends Sharing | ✅ | ✅ | ✅ Visible | ✅ Done |
| Weather | ✅ | ✅ | ✅ Visible | ✅ Done |
| Departure Reminders | ✅ | ✅ | ✅ Visible | ✅ Done |
| Driving Analytics | ✅ | ⚠️ Mock | ❌ Not Real | 🔴 High |
| Route Alerts | ✅ | ⚠️ Notification | ⚠️ Limited | 🟡 Medium |
| Traffic Aware | ✅ | ❌ Background | ❌ Hidden | 🟡 Medium |
| Vehicle Profile | ✅ | ❌ None | ❌ Hidden | 🔴 Critical |
| Parking Finder | ✅ | ❌ None | ❌ Hidden | 🔴 Critical |
| Voice Navigation | ✅ | ❌ No controls | ⚠️ Auto | 🟡 Medium |
| AI Assistant | ✅ | ❌ None | ❌ Hidden | 🔴 Critical |
| Hazard Detection | ✅ | ⚠️ Generic | ⚠️ Limited | 🟢 Low |

---

## 🎁 Hidden Gem Features

These awesome features exist in the codebase but users have NO IDEA they exist:

1. **🤖 AI Route Planning** - Gemini AI integration for smart suggestions
2. **🅿️ Smart Parking Finder** - Find and book parking automatically
3. **🚗 Vehicle Profiles** - Customize for your car, truck, or motorcycle
4. **📊 Real Driving Score** - Get rated on braking, acceleration, speeding
5. **🗣️ Voice Announcements** - Full turn-by-turn voice guidance
6. **🚦 Live Traffic Visualization** - See real-time traffic on map
7. **⚠️ Smart Hazard Warnings** - Specific warnings for curves, hills, etc.

**These features are production-ready but have no UI!** This is like having a Ferrari in your garage but not knowing where the keys are. 🔑

---

## Next Steps

1. **Review this audit** with the development team
2. **Prioritize** which hidden features to expose first
3. **Create UI designs** for critical missing access points
4. **Implement quick fixes** for ProfileScreen additions
5. **Test discovery** - Can new users find all features?
6. **Add onboarding tour** to showcase features on first launch

Would you like me to implement any of these fixes now?
