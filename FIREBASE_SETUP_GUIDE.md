# Firebase Setup and Integration Guide for Aurora

Complete step-by-step guide to integrate Firebase into your Aurora navigation app.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Firebase Console Setup](#firebase-console-setup)
3. [Add Firebase to Android Project](#add-firebase-to-android-project)
4. [Configure Gradle](#configure-gradle)
5. [Initialize Firebase](#initialize-firebase)
6. [Implement Firebase Services](#implement-firebase-services)
7. [Testing and Verification](#testing-and-verification)
8. [Security Rules](#security-rules)

---

## Prerequisites

Before starting, ensure you have:
- ✅ Google account
- ✅ Android Studio with Aurora project open
- ✅ Internet connection
- ✅ Google Play Services on your test device
- ✅ Package name: `com.aurora.navigation` (or your actual package name)

---

## Firebase Console Setup

### Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"** or **"Create a project"**
3. Enter project name: `Aurora Navigation`
4. Enable/Disable Google Analytics (recommended: **Enable**)
5. Select or create Analytics account
6. Click **"Create project"** and wait for setup to complete
7. Click **"Continue"** when ready

### Step 2: Add Android App to Firebase

1. In Firebase Console, click the **Android icon** (or "Add app")
2. Fill in the registration form:
   - **Android package name**: `com.aurora.navigation` ⚠️ **MUST match your AndroidManifest.xml**
   - **App nickname** (optional): `Aurora Android`
   - **Debug signing certificate SHA-1**: 
     ```bash
     # Get SHA-1 from your debug keystore:
     cd %USERPROFILE%\.android
     keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```
     Copy the SHA-1 fingerprint (format: `AA:BB:CC:...`)
3. Click **"Register app"**

### Step 3: Download google-services.json

1. Click **"Download google-services.json"**
2. Save the file
3. Move it to your project:
   ```
   Aurora/
   └── androidApp/
       ├── build.gradle.kts
       ├── google-services.json  ← Place here
       └── src/
   ```
4. ⚠️ **IMPORTANT**: Add to `.gitignore`:
   ```
   # In your .gitignore file
   google-services.json
   ```
5. Click **"Next"** in Firebase Console

---

## Add Firebase to Android Project

### Step 4: Add Google Services Plugin

**File: `build.gradle.kts` (Project root)**

```kotlin
plugins {
    // existing plugins...
    id("com.google.gms.google-services") version "4.4.0" apply false
}
```

**File: `androidApp/build.gradle.kts`**

Add at the **bottom** of the file:

```kotlin
plugins {
    // existing plugins...
    id("com.google.gms.google-services")
}
```

### Step 5: Add Firebase Dependencies

**File: `gradle/libs.versions.toml`**

Add Firebase versions to `[versions]` section:
```toml
[versions]
# ... existing versions ...
firebase-bom = "32.7.0"
```

Add Firebase libraries to `[libraries]` section:
```toml
[libraries]
# ... existing libraries ...

# Firebase
firebase-bom = { module = "com.google.firebase:firebase-bom", version.ref = "firebase-bom" }
firebase-analytics = { module = "com.google.firebase:firebase-analytics-ktx" }
firebase-auth = { module = "com.google.firebase:firebase-auth-ktx" }
firebase-firestore = { module = "com.google.firebase:firebase-firestore-ktx" }
firebase-messaging = { module = "com.google.firebase:firebase-messaging-ktx" }
firebase-crashlytics = { module = "com.google.firebase:firebase-crashlytics-ktx" }
firebase-storage = { module = "com.google.firebase:firebase-storage-ktx" }
```

**File: `androidApp/build.gradle.kts`**

Add dependencies:
```kotlin
dependencies {
    // ... existing dependencies ...
    
    // Firebase BOM - manages all Firebase versions
    implementation(platform(libs.firebase.bom))
    
    // Firebase services (versions managed by BOM)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.storage)
}
```

### Step 6: Sync Gradle

1. Click **"Sync Now"** in Android Studio
2. Wait for Gradle sync to complete
3. Verify no errors in Build output

---

## Initialize Firebase

### Step 7: Verify Package Name

**File: `androidApp/src/main/AndroidManifest.xml`**

Ensure package name matches Firebase registration:
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.aurora.navigation">
```

### Step 8: Create Firebase Initializer (Optional but Recommended)

Firebase auto-initializes, but you can customize initialization:

**File: `androidApp/src/main/kotlin/com/aurora/navigation/AuroraApplication.kt`**

```kotlin
package com.aurora.navigation

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class AuroraApplication : Application() {
    
    private lateinit var analytics: FirebaseAnalytics
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Analytics
        analytics = Firebase.analytics
        
        // Enable analytics collection
        analytics.setAnalyticsCollectionEnabled(true)
        
        println("Firebase initialized successfully")
    }
}
```

### Step 9: Register Application Class

**File: `androidApp/src/main/AndroidManifest.xml`**

Add `android:name` to `<application>` tag:
```xml
<application
    android:name=".AuroraApplication"
    android:allowBackup="true"
    android:icon="@mipmap/ic_launcher"
    ...>
```

---

## Implement Firebase Services

### 🔐 Firebase Authentication

**File: `androidApp/src/main/kotlin/com/aurora/navigation/auth/FirebaseAuthManager.kt`**

```kotlin
package com.aurora.navigation.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class FirebaseAuthManager {
    
    private val auth: FirebaseAuth = Firebase.auth
    
    val currentUser: FirebaseUser?
        get() = auth.currentUser
    
    val isUserSignedIn: Boolean
        get() = currentUser != null
    
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { 
                Result.success(it) 
            } ?: Result.failure(Exception("User is null"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create new user with email and password
     */
    suspend fun createUserWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { 
                Result.success(it) 
            } ?: Result.failure(Exception("User is null"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
    }
    
    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 🗄️ Cloud Firestore

**File: `androidApp/src/main/kotlin/com/aurora/navigation/data/models/SavedRoute.kt`**

```kotlin
package com.aurora.navigation.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class SavedRoute(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val routeName: String = "",
    val origin: RouteLocation = RouteLocation(),
    val destination: RouteLocation = RouteLocation(),
    val waypoints: List<RouteLocation> = emptyList(),
    val distance: Double = 0.0,
    val duration: Long = 0,
    val isFavorite: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

data class RouteLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = ""
)
```

**File: `androidApp/src/main/kotlin/com/aurora/navigation/data/FirestoreRepository.kt`**

```kotlin
package com.aurora.navigation.data

import com.aurora.navigation.data.models.SavedRoute
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    
    private val db: FirebaseFirestore = Firebase.firestore
    private val routesCollection = db.collection("routes")
    
    /**
     * Save a new route to Firestore
     */
    suspend fun saveRoute(route: SavedRoute): Result<String> {
        return try {
            val docRef = routesCollection.add(route).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all routes for a user
     */
    suspend fun getUserRoutes(userId: String): Result<List<SavedRoute>> {
        return try {
            val snapshot = routesCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val routes = snapshot.documents.mapNotNull { it.toObject<SavedRoute>() }
            Result.success(routes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user routes as a real-time Flow
     */
    fun getUserRoutesFlow(userId: String): Flow<List<SavedRoute>> = callbackFlow {
        val listener = routesCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val routes = snapshot?.documents?.mapNotNull { 
                    it.toObject<SavedRoute>() 
                } ?: emptyList()
                
                trySend(routes)
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Delete a route
     */
    suspend fun deleteRoute(routeId: String): Result<Unit> {
        return try {
            routesCollection.document(routeId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update route favorite status
     */
    suspend fun toggleFavorite(routeId: String, isFavorite: Boolean): Result<Unit> {
        return try {
            routesCollection.document(routeId)
                .update("isFavorite", isFavorite)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get favorite routes
     */
    suspend fun getFavoriteRoutes(userId: String): Result<List<SavedRoute>> {
        return try {
            val snapshot = routesCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isFavorite", true)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val routes = snapshot.documents.mapNotNull { it.toObject<SavedRoute>() }
            Result.success(routes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 📊 Firebase Analytics

**File: `androidApp/src/main/kotlin/com/aurora/navigation/analytics/AnalyticsTracker.kt`**

```kotlin
package com.aurora.navigation.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

class AnalyticsTracker {
    
    private val analytics: FirebaseAnalytics = Firebase.analytics
    
    /**
     * Track navigation started
     */
    fun trackNavigationStarted(
        origin: String,
        destination: String,
        distance: Double,
        estimatedDuration: Long
    ) {
        val params = Bundle().apply {
            putString("origin", origin)
            putString("destination", destination)
            putDouble("distance_km", distance / 1000.0)
            putLong("estimated_duration_min", estimatedDuration / 60)
        }
        analytics.logEvent("navigation_started", params)
    }
    
    /**
     * Track navigation completed
     */
    fun trackNavigationCompleted(
        distance: Double,
        actualDuration: Long,
        hazardsEncountered: Int
    ) {
        val params = Bundle().apply {
            putDouble("distance_km", distance / 1000.0)
            putLong("actual_duration_min", actualDuration / 60)
            putInt("hazards_encountered", hazardsEncountered)
        }
        analytics.logEvent("navigation_completed", params)
    }
    
    /**
     * Track route saved
     */
    fun trackRouteSaved(routeName: String, isFavorite: Boolean) {
        val params = Bundle().apply {
            putString("route_name", routeName)
            putBoolean("is_favorite", isFavorite)
        }
        analytics.logEvent("route_saved", params)
    }
    
    /**
     * Track AI recommendation used
     */
    fun trackAIRecommendationUsed(recommendationType: String) {
        val params = Bundle().apply {
            putString("recommendation_type", recommendationType)
        }
        analytics.logEvent("ai_recommendation_used", params)
    }
    
    /**
     * Track hazard detected
     */
    fun trackHazardDetected(hazardType: String, severity: String) {
        val params = Bundle().apply {
            putString("hazard_type", hazardType)
            putString("severity", severity)
        }
        analytics.logEvent("hazard_detected", params)
    }
    
    /**
     * Set user properties
     */
    fun setUserProperties(userId: String, isPremium: Boolean) {
        analytics.setUserId(userId)
        analytics.setUserProperty("premium_user", isPremium.toString())
    }
}
```

### 🔔 Cloud Messaging (Push Notifications)

**File: `androidApp/src/main/kotlin/com/aurora/navigation/notifications/AuroraFirebaseMessagingService.kt`**

```kotlin
package com.aurora.navigation.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aurora.navigation.MainActivity
import com.aurora.navigation.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AuroraFirebaseMessagingService : FirebaseMessagingService() {
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // Handle data payload
        remoteMessage.data.isNotEmpty().let {
            handleDataMessage(remoteMessage.data)
        }
        
        // Handle notification payload
        remoteMessage.notification?.let {
            sendNotification(it.title ?: "Aurora", it.body ?: "")
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        println("New FCM token: $token")
        // Send token to your server or save to Firestore
    }
    
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        when (type) {
            "traffic_alert" -> {
                val title = "Traffic Alert"
                val message = data["message"] ?: "Heavy traffic detected on your route"
                sendNotification(title, message)
            }
            "hazard_warning" -> {
                val title = "Hazard Warning"
                val message = data["message"] ?: "Hazard detected on your route"
                sendNotification(title, message)
            }
            "weather_alert" -> {
                val title = "Weather Alert"
                val message = data["message"] ?: "Weather conditions changed"
                sendNotification(title, message)
            }
        }
    }
    
    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val channelId = "aurora_navigation_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_navigation)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Aurora Navigation",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Aurora navigation alerts and updates"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        notificationManager.notify(0, notificationBuilder.build())
    }
}
```

**Add to AndroidManifest.xml:**

```xml
<manifest>
    <application>
        <!-- ... existing content ... -->
        
        <!-- Firebase Messaging Service -->
        <service
            android:name=".notifications.AuroraFirebaseMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
        
        <!-- Default notification icon -->
        <meta-data
            android:name="com.google.firebase.messaging.default_notification_icon"
            android:resource="@drawable/ic_navigation" />
        
        <!-- Default notification color -->
        <meta-data
            android:name="com.google.firebase.messaging.default_notification_color"
            android:resource="@color/primary" />
    </application>
</manifest>
```

---

## Testing and Verification

### Step 10: Test Firebase Connection

**Create a test screen:**

**File: `androidApp/src/main/kotlin/com/aurora/navigation/screens/FirebaseTestScreen.kt`**

```kotlin
package com.aurora.navigation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aurora.navigation.auth.FirebaseAuthManager
import com.aurora.navigation.data.FirestoreRepository
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import kotlinx.coroutines.launch

@Composable
fun FirebaseTestScreen() {
    val scope = rememberCoroutineScope()
    val authManager = remember { FirebaseAuthManager() }
    val firestoreRepo = remember { FirestoreRepository() }
    
    var status by remember { mutableStateOf("Firebase Status: Checking...") }
    var testEmail by remember { mutableStateOf("test@aurora.com") }
    var testPassword by remember { mutableStateOf("testpassword123") }
    
    LaunchedEffect(Unit) {
        try {
            val app = Firebase.app
            status = "✅ Firebase initialized: ${app.name}"
        } catch (e: Exception) {
            status = "❌ Firebase error: ${e.message}"
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = status, style = MaterialTheme.typography.titleMedium)
        
        Divider()
        
        // Auth Test
        Text("Authentication Test", style = MaterialTheme.typography.titleSmall)
        
        OutlinedTextField(
            value = testEmail,
            onValueChange = { testEmail = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        
        OutlinedTextField(
            value = testPassword,
            onValueChange = { testPassword = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val result = authManager.createUserWithEmail(testEmail, testPassword)
                    status = result.fold(
                        onSuccess = { "✅ User created: ${it.email}" },
                        onFailure = { "❌ Create failed: ${it.message}" }
                    )
                }
            }) {
                Text("Create User")
            }
            
            Button(onClick = {
                scope.launch {
                    val result = authManager.signInWithEmail(testEmail, testPassword)
                    status = result.fold(
                        onSuccess = { "✅ Signed in: ${it.email}" },
                        onFailure = { "❌ Sign in failed: ${it.message}" }
                    )
                }
            }) {
                Text("Sign In")
            }
            
            Button(onClick = {
                authManager.signOut()
                status = "✅ Signed out"
            }) {
                Text("Sign Out")
            }
        }
        
        Text("Current user: ${authManager.currentUser?.email ?: "None"}")
    }
}
```

### Step 11: Run the App

1. Build and run on a **real device** (Firebase requires Google Play Services)
2. Check Logcat for Firebase initialization messages
3. Go to Firebase Console → Project Overview → check for app activity
4. Test authentication and Firestore operations
5. Monitor real-time database activity in Firebase Console

### Step 12: Verify in Firebase Console

**Authentication:**
- Go to Authentication → Users
- Should see test users you created

**Firestore:**
- Go to Firestore Database → Data
- Should see documents you created

**Analytics:**
- Go to Analytics → Events
- Should see events within 24 hours (not real-time)

---

## Security Rules

### Step 13: Configure Firestore Security Rules

In Firebase Console → Firestore Database → Rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper function to check if user is authenticated
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Helper function to check if user owns the resource
    function isOwner(userId) {
      return request.auth.uid == userId;
    }
    
    // Routes collection
    match /routes/{routeId} {
      // Allow read if authenticated and owner
      allow read: if isAuthenticated() && isOwner(resource.data.userId);
      
      // Allow create if authenticated and setting correct userId
      allow create: if isAuthenticated() && 
                       request.resource.data.userId == request.auth.uid;
      
      // Allow update/delete if owner
      allow update, delete: if isAuthenticated() && 
                               isOwner(resource.data.userId);
    }
    
    // User profiles (if you add them later)
    match /users/{userId} {
      allow read, write: if isAuthenticated() && request.auth.uid == userId;
    }
  }
}
```

### Step 14: Configure Storage Security Rules (if using Cloud Storage)

In Firebase Console → Storage → Rules:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    
    // User uploads
    match /users/{userId}/{allPaths=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Route images
    match /routes/{routeId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

---

## Common Issues and Solutions

### Issue 1: "google-services.json not found"
**Solution:** Ensure file is in `androidApp/` folder at same level as `build.gradle.kts`

### Issue 2: "Default FirebaseApp is not initialized"
**Solution:** 
- Verify `google-services.json` is correct
- Check package name matches AndroidManifest.xml
- Clean and rebuild project

### Issue 3: "FirebaseAuth is not initialized"
**Solution:** Make sure `firebase-auth-ktx` dependency is added

### Issue 4: Google Play Services not available
**Solution:** 
- Test on real device, not all emulators have Play Services
- Update Google Play Services on device

### Issue 5: Firestore permission denied
**Solution:** Update security rules to allow authenticated users

---

## Next Steps

After basic setup, you can implement:

1. **Social Login** - Add Google Sign-In, Facebook, etc.
2. **Offline Support** - Enable Firestore offline persistence
3. **Cloud Functions** - Backend logic for complex operations
4. **Performance Monitoring** - Track app performance metrics
5. **Remote Config** - Feature flags and dynamic configuration
6. **App Distribution** - Beta testing with Firebase

---

## Useful Firebase Console URLs

- **Console Home**: https://console.firebase.google.com/
- **Authentication**: `[Your Project]` → Authentication
- **Firestore**: `[Your Project]` → Firestore Database
- **Analytics**: `[Your Project]` → Analytics → Events
- **Cloud Messaging**: `[Your Project]` → Cloud Messaging
- **Project Settings**: Gear icon → Project settings

---

## Additional Resources

- [Firebase Android Documentation](https://firebase.google.com/docs/android/setup)
- [Firestore Data Modeling](https://firebase.google.com/docs/firestore/data-model)
- [Firebase Auth Best Practices](https://firebase.google.com/docs/auth/android/start)
- [Cloud Messaging Setup](https://firebase.google.com/docs/cloud-messaging/android/client)

---

## Estimated Implementation Time

| Feature | Time |
|---------|------|
| Basic Setup | 30-45 min |
| Authentication | 1-2 hours |
| Firestore (routes) | 2-3 hours |
| Analytics | 30 min |
| Cloud Messaging | 1-2 hours |
| **Total** | **5-8 hours** |

---

## Support

If you encounter issues:
1. Check Firebase Console logs
2. Review Logcat output
3. Verify all dependencies are synced
4. Ensure internet connection is active
5. Check Firebase status page: https://status.firebase.google.com/

Happy coding! 🚀
