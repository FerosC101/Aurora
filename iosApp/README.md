# Aurora iOS App

## Overview
iOS application for Aurora - AI-Powered Smart Navigation system.

## Requirements
- **Xcode**: 15.0 or later
- **iOS Deployment Target**: 15.0+
- **Swift**: 5.0+
- **CocoaPods**: For Firebase integration

## Setup Instructions

### 1. Install Xcode
Download and install Xcode from the Mac App Store.

### 2. Install CocoaPods
```bash
sudo gem install cocoapods
```

### 3. Setup Kotlin Multiplatform Framework
From the project root directory:
```bash
./gradlew :shared:linkDebugFrameworkIosArm64
./gradlew :shared:linkDebugFrameworkIosX64
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### 4. Install Dependencies
```bash
cd iosApp
pod install
```

### 5. Open in Xcode
```bash
open iosApp.xcworkspace
```

## Project Structure
```
iosApp/
├── iosApp/
│   ├── AuroraApp.swift        # Main app entry point
│   ├── ContentView.swift      # Main UI view
│   ├── Info.plist            # App configuration
│   └── Assets.xcassets/      # App icons and images
├── Configuration/
│   └── Config.xcconfig       # Build configuration
├── Podfile                   # CocoaPods dependencies
└── README.md                 # This file
```

## Current Status
🚧 **Under Development**

The iOS app is currently a placeholder showing the feature list. Full implementation requires:

### Phase 1: Core Integration
- [ ] Link Kotlin Multiplatform shared module
- [ ] Setup Firebase iOS SDK
- [ ] Configure Google Maps iOS SDK
- [ ] Implement location services

### Phase 2: UI Implementation
- [ ] Home screen with route input
- [ ] Navigation screen with map
- [ ] Profile and settings
- [ ] Activity tracking

### Phase 3: Features
- [ ] AI route planning
- [ ] Real-time navigation
- [ ] Social features (chat, carpool)
- [ ] Voice guidance

### Phase 4: Polish
- [ ] Notifications
- [ ] Background location
- [ ] Widget support
- [ ] App Store preparation

## Building the App

### Debug Build
1. Select a simulator or device in Xcode
2. Press **Cmd + R** to build and run

### Release Build
1. Configure signing in Xcode
2. Select **Product > Archive**
3. Follow App Store submission process

## Dependencies

### CocoaPods (To be added)
```ruby
# Firebase
pod 'Firebase/Auth'
pod 'Firebase/Firestore'
pod 'Firebase/Storage'
pod 'Firebase/Messaging'

# Google Maps
pod 'GoogleMaps'
pod 'GooglePlaces'

# Other
pod 'Alamofire'  # Networking
```

## Configuration

### API Keys
Add the following to `Config.xcconfig`:
```
GOOGLE_MAPS_API_KEY = your_google_maps_ios_key
FIREBASE_OPTIONS_PATH = GoogleService-Info.plist
```

### Firebase Setup
1. Create iOS app in Firebase Console
2. Download `GoogleService-Info.plist`
3. Add to iosApp target in Xcode

## Troubleshooting

### Framework not found: 'shared'
Run the gradle tasks to build the shared framework:
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### CocoaPods not working
```bash
pod repo update
pod install --repo-update
```

### Code signing issues
1. Open Xcode
2. Select iosApp target
3. Go to **Signing & Capabilities**
4. Select your development team

## Contributing
This is the iOS counterpart to the Android app. Features should match the Android implementation as closely as possible.

## License
Educational/Portfolio Project

## Contact
For questions about the iOS implementation, refer to the main project README.
