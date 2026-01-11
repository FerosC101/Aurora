# Aurora iOS Module

> Part of the Aurora Kotlin Multiplatform Contest 2026 submission

## Status: Structure Ready 🚧

The iOS module structure has been created with SwiftUI components and Firebase configuration in place. However, iOS compilation requires macOS with Xcode 15+.

## What's Included

- ✅ SwiftUI app structure (`AuroraApp.swift`, `ContentView.swift`)
- ✅ Firebase configuration (`GoogleService-Info.plist`)
- ✅ CocoaPods setup with dependencies (`Podfile`)
- ✅ iOS targets in shared module (iosX64, iosArm64, iosSimulatorArm64)
- ✅ Complete platform abstractions (`Platform.swift`)

## Requirements
- **Xcode**: 15.0 or later
- **iOS Deployment Target**: 15.0+
- **Swift**: 5.0+
- **CocoaPods**: For dependency management
- **macOS**: Required for iOS development

## Setup Instructions

### 1. Install CocoaPods
```bash
sudo gem install cocoapods
```

### 2. Build Shared Framework
From the project root directory:
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### 3. Install Dependencies
```bash
cd iosApp
pod install
```

### 4. Open in Xcode
```bash
open Aurora.xcworkspace
```

### 5. Build and Run
Select a simulator or device in Xcode and press **Cmd + R**

## Project Structure
```
iosApp/
├── iosApp/
│   ├── AuroraApp.swift              # Main app entry point
│   ├── ContentView.swift            # Main UI view (placeholder)
│   ├── Platform.swift               # Platform-specific stubs
│   ├── Info.plist                   # App configuration
│   ├── GoogleService-Info.plist     # Firebase config (ready)
│   └── Assets.xcassets/             # App icons and images
├── Podfile                          # CocoaPods dependencies
├── SETUP.md                         # Detailed setup guide
└── README.md                        # This file
```

## Features (When Fully Implemented)

The iOS app will include all Android features:
- ✅ Smart navigation with AI assistant
- ✅ Social carpool features  
- ✅ Real-time chat and notifications
- ✅ Trip history and analytics
- ✅ Firebase integration
- ✅ Google Maps integration

## Dependencies (Podfile)

```ruby
# Firebase SDK
pod 'Firebase/Analytics'
pod 'Firebase/Auth'
pod 'Firebase/Firestore'
pod 'Firebase/Messaging'

# Google Maps
pod 'GoogleMaps'
pod 'GooglePlaces'

# Networking
pod 'Alamofire'
```

## Development Timeline

**Estimated**: 6-8 weeks for full iOS implementation  
**Requires**: Mac hardware with Xcode

### Implementation Phases
1. **Phase 1**: Core navigation and maps (2 weeks)
2. **Phase 2**: Firebase integration and auth (2 weeks)
3. **Phase 3**: Social features and chat (2 weeks)
4. **Phase 4**: Polish and testing (2 weeks)

## Troubleshooting

### Framework not found: 'shared'
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

### CocoaPods issues
```bash
pod repo update
pod install --repo-update
```

### Code signing
1. Open Xcode
2. Select iosApp target
3. Go to **Signing & Capabilities**
4. Select your development team

---

For full project documentation, see the [main README](../README.md).

