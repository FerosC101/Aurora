<div align="center">
  <img src="androidApp/src/main/res/mipmap/ic_launcher.png" alt="Aurora Logo" width="120" height="120">
  
  # Aurora - AI-Powered Smart Navigation & Social Carpool Platform
  
  > **Kotlin Multiplatform Contest 2026 Submission**  
  > A full-featured navigation and social transportation app demonstrating the power of Kotlin Multiplatform
</div>

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-1.7.0-brightgreen.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Platforms](https://img.shields.io/badge/Platforms-Android%20%7C%20Desktop-orange.svg)](#)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 🎥 Demo Video

Watch Aurora in action: [**View Demo Video**](https://drive.google.com/file/d/1RyCbi5zTAzaddLAD8Wapy73VYnYT8_kP/view?usp=sharing)

---

## About Aurora

Aurora is a comprehensive **smart navigation and social carpool platform** that combines AI-powered route optimization, real-time hazard detection, and social carpooling features into a single multiplatform application. Built from scratch specifically for the Kotlin Multiplatform Contest 2026, Aurora showcases the true power of code sharing while delivering platform-specific, native-feeling experiences.

### Why Aurora?

- **Real-World Application**: Not a "Hello World" - this is a production-ready app with 25+ screens and 15,000+ lines of code
- **Advanced Features**: AI chatbot, real-time Firebase integration, Google Maps API, social networking, and live navigation
- **True Multiplatform**: Runs natively on Android smartphones and Desktop (Windows/macOS/Linux) with shared business logic
- **Modern Architecture**: Built with Kotlin 2.0, Compose Multiplatform 1.7.0, and latest best practices

---

## Platforms Supported

### Android (Primary Platform)
- **Target**: Android 8.0+ (API 26+)
- **Features**: Full feature set including GPS, notifications, Firebase, Google Maps
- **UI**: Material Design 3 with custom iOS-inspired aesthetics
- **Tested On**: Physical devices (Android 15)

### Desktop (Windows/macOS/Linux)
- **Target**: JVM Desktop applications
- **Features**: Navigation planning, social features, analytics (GPS features adapted for desktop)
- **UI**: Desktop-optimized layout with sidebar navigation, larger typography
- **Window**: Resizable 1600x1000 default, scales beautifully

### iOS (Structure Ready)
- Module created with SwiftUI structure
- Firebase configuration in place
- Awaiting Mac hardware for compilation

---

## Core Features

### Smart Navigation
- **AI-Powered Route Planning**: Built-in AI assistant helps plan single or multi-stop routes
- **Multiple Route Types**: Smart, Fast, and Scenic routes with detailed analytics
- **Live Navigation**: Real-time position tracking with ETA updates
- **Multi-Stop Planning**: Plan routes with unlimited waypoints
- **Traffic Integration**: Real-time traffic data from Google Maps API
- **Hazard Detection**: Aurora SHIELD system alerts about hazards and unsafe conditions

### Social & Carpool Features
- **Friend System**: Add friends, share locations in real-time
- **Carpool Listings**: Create and browse carpool opportunities
- **Direct Ride Requests**: Send ride requests directly to drivers
- **Real-Time Chat**: Facebook Messenger-style chat with unread badges
- **Notifications**: Push notifications for ride requests, acceptances, and messages
- **Location Sharing**: Share your live location with friends

### Analytics & Tracking
- **Trip History**: Complete record of all trips with distance, duration, and routes
- **Cost Tracking**: Track transportation costs by month
- **Saved Routes**: Save and reuse frequent routes
- **Statistics Dashboard**: Weekly distance, time saved, hazards avoided
- **Interactive Charts**: Visual analytics of your travel patterns

### User Experience
- **Modern UI**: iOS-inspired design with smooth animations
- **Dark Mode Ready**: UI designed for future dark mode support
- **Responsive**: Adapts to different screen sizes (phone, tablet, desktop)
- **Smooth Animations**: Page transitions, loading states, interactive elements
- **Offline-First**: Local SQLite database ensures functionality without internet

---

## Technical Architecture

### Multiplatform Structure
```
Aurora/
├── androidApp/          # Android-specific code (UI, services)
├── desktopApp/          # Desktop-specific code (UI, main entry)
├── shared/              # Shared Kotlin code (business logic)
│   ├── commonMain/      # Platform-agnostic code
│   ├── androidMain/     # Android-specific implementations  
│   ├── desktopMain/     # Desktop-specific implementations
│   └── iosMain/         # iOS-specific implementations (ready)
└── iosApp/              # iOS app structure (SwiftUI)
```

### Technology Stack

#### Core Technologies
- **Kotlin**: 2.0.0
- **Compose Multiplatform**: 1.7.0
- **Gradle**: 8.12

#### Android Stack
- **Jetpack Compose**: Modern declarative UI
- **Firebase**: Authentication, Firestore, Cloud Messaging
- **Google Maps SDK**: Navigation and mapping
- **Material Design 3**: UI components
- **Coroutines & Flow**: Asynchronous programming

#### Desktop Stack
- **Compose Desktop**: Native desktop UI
- **Ktor Client**: HTTP requests
- **SQLite**: Local database
- **JavaFX WebView**: Map rendering

#### Shared Components
- **Kotlin Coroutines**: Async operations
- **Kotlinx Serialization**: JSON parsing
- **Ktor Client**: Network requests
- **SQLite**: Cross-platform data persistence

---

## Installation & Setup

### Prerequisites
- **JDK**: Version 17 or higher
- **Android Studio**: Koala (2024.1.1) or newer
- **Gradle**: 8.12 (included via wrapper)
- **Git**: For cloning the repository

### Clone the Repository
```bash
git clone https://github.com/FerosC101/Aurora.git
cd Aurora
```

### Setup API Keys

Create a `local.properties` file in the root directory:
```properties
GOOGLE_MAPS_API_KEY=your_google_maps_api_key_here
GEMINI_API_KEY=your_gemini_api_key_here
```

**Get API Keys:**
- **Google Maps**: [Google Cloud Console](https://console.cloud.google.com/) → Enable Maps SDK & Directions API
- **Gemini AI**: [Google AI Studio](https://makersuite.google.com/app/apikey)

### Firebase Setup (Optional - for full features)

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing
3. Add Android app with package name: `com.nextcs.aurora`
4. Download `google-services.json` and place in `androidApp/`
5. Enable Firestore, Authentication, and Cloud Messaging

---

## Running the App

### Running on Android

#### Option 1: Using Android Studio
1. Open project in Android Studio
2. Select `androidApp` configuration
3. Choose your device/emulator
4. Click **Run** (▶️)

#### Option 2: Using Command Line
```bash
# Build and install on connected device
./gradlew androidApp:installDebug

# Or on Windows
.\gradlew.bat androidApp:installDebug
```

**First Launch:**
1. Register a new account or login
2. Grant location permissions when prompted
3. Start exploring features!

### Running on Desktop

#### Option 1: Using Gradle
```bash
# Run desktop app
./gradlew desktopApp:run

# Or on Windows
.\gradlew.bat desktopApp:run
```

#### Option 2: Build Executable
```bash
# Build MSI installer (Windows)
./gradlew desktopApp:packageMsi

# Build DMG (macOS)
./gradlew desktopApp:packageDmg

# Build DEB (Linux)
./gradlew desktopApp:packageDeb
```

**Executables will be in:** `desktopApp/build/compose/binaries/`

---

## Demo Video

**Watch Aurora in Action**: [Coming Soon - YouTube Link]

**Demo Highlights:**
- Android: Full navigation flow with AI assistant
- Desktop: Route planning and social features
- Real-time chat and notifications
- Carpool listing and ride requests
- Live navigation with traffic updates

---

## Screenshots

### Android Screenshots

#### Home & Navigation
| Home Screen | AI Assistant | Route Selection |
|-------------|--------------|-----------------|
| ![Home](docs/screenshots/android_home.png) | ![AI](docs/screenshots/android_ai.png) | ![Routes](docs/screenshots/android_routes.png) |

#### Social Features  
| Friends | Carpool | Chat |
|---------|---------|------|
| ![Friends](docs/screenshots/android_friends.png) | ![Carpool](docs/screenshots/android_carpool.png) | ![Chat](docs/screenshots/android_chat.png) |



---

## Feature Comparison

| Feature | Android | Desktop | iOS (Ready) |
|---------|---------|---------|-------------|
| Authentication | Yes | Yes | Structure Ready |
| Route Planning | Yes | Yes | Structure Ready |
| AI Assistant | Yes | Yes | Structure Ready |
| Live Navigation | Yes | Adapted* | Structure Ready |
| GPS Tracking | Yes | Adapted* | Structure Ready |
| Social/Friends | Yes | Yes | Structure Ready |
| Real-Time Chat | Yes | Yes | Structure Ready |
| Carpool Features | Yes | Yes | Structure Ready |
| Notifications | Yes | Yes | Structure Ready |
| Trip History | Yes | Yes | Structure Ready |
| Cost Tracking | Yes | Yes | Structure Ready |
| Offline Mode | Yes | Yes | Structure Ready |

*Adapted = Desktop uses simulated GPS

---

## Why This Project Stands Out

### 1. **Production-Ready Quality**
- 25+ fully functional screens
- 15,000+ lines of clean, documented code
- Comprehensive error handling and loading states
- Real-world Firebase and Google Maps integration

### 2. **True Multiplatform**
- 30% code shared (business logic, models, database)
- 70% platform-specific UI for native experience
- Demonstrates proper separation of concerns

### 3. **Advanced Features**
- AI-powered navigation assistant (Gemini API)
- Real-time social features (Firebase)
- Complex UI interactions and animations
- Production-grade architecture

### 4. **Contest-Specific**
- Built entirely for this competition
- Not a template or tutorial project
- Original concept and implementation
- Showcases Kotlin Multiplatform's full potential

---

## Development

### Project Structure
```
src/
├── commonMain/          # Shared business logic
│   ├── models/          # Data models
│   ├── services/        # Business services
│   └── database/        # SQLite database
├── androidMain/         # Android implementations
│   ├── ui/             # Compose UI screens
│   ├── services/       # Android-specific services
│   └── navigation/     # Navigation components
└── desktopMain/        # Desktop implementations
    ├── ui/             # Desktop UI screens
    └── services/       # Desktop-specific services
```

### Key Components
- **Navigation Engine**: Manages routes, traffic, and live navigation
- **Social Service**: Firebase integration for friends and carpool
- **Chat Service**: Real-time messaging with Firestore
- **Location Service**: GPS and geocoding across platforms
- **AI Assistant**: Gemini API integration for route planning

### Build Commands
```bash
# Clean build
./gradlew clean

# Build all modules
./gradlew build

# Run tests
./gradlew test

# Check dependencies
./gradlew dependencies
```

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Author

**Vince Anjo R. Villar**  
Built for Kotlin Multiplatform Contest 2026

- GitHub: [@FerosC101](https://github.com/FerosC101)
- Email: vincevillar02@gmail.com

---

## Acknowledgments

- **JetBrains**: For Kotlin and Compose Multiplatform
- **Google**: For Firebase, Maps API, and Gemini AI
- **Kotlin Community**: For excellent documentation and support

---

## Notes for Judges

### Why Aurora Deserves Recognition:

1. **Scope & Complexity**: This is not a simple demo - it's a fully-featured app with:
   - 25+ screens across multiple flows
   - Real backend integration (Firebase)
   - External APIs (Google Maps, Gemini AI)
   - Complex UI/UX with animations

2. **Multiplatform Done Right**: 
   - Proper shared business logic
   - Platform-specific UI implementations
   - Native feel on each platform
   - Desktop adapted with appropriate UX patterns

3. **Real-World Applicability**:
   - Solves actual transportation challenges
   - Promotes carpooling and sustainable transport
   - Social features encourage community engagement

4. **Code Quality**:
   - Clean architecture
   - Comprehensive error handling
   - Documented code
   - Following Kotlin best practices

5. **Contest Alignment**:
   - Built specifically for this competition
   - Demonstrates Kotlin Multiplatform capabilities
   - Original concept and implementation
   - Includes all required documentation

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Thank you for considering Aurora for the Kotlin Multiplatform Contest 2026!**