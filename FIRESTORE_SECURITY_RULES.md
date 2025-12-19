# Firestore Security Rules for Aurora

Copy and paste these rules into Firebase Console → Firestore Database → Rules

## Production-Ready Security Rules

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // ============================================
    // HELPER FUNCTIONS
    // ============================================
    
    // Check if user is authenticated
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Check if user owns the resource
    function isOwner(userId) {
      return request.auth.uid == userId;
    }
    
    // Check if resource exists
    function resourceExists() {
      return resource != null;
    }
    
    // Validate required fields for SavedRoute
    function isValidRoute() {
      let data = request.resource.data;
      return data.keys().hasAll(['userId', 'routeName', 'origin', 'destination', 'distance', 'duration']) &&
             data.userId is string &&
             data.routeName is string &&
             data.distance is number &&
             data.duration is number &&
             data.distance >= 0 &&
             data.duration >= 0;
    }
    
    // Validate route location object
    function isValidLocation(location) {
      return location.keys().hasAll(['latitude', 'longitude', 'address']) &&
             location.latitude is number &&
             location.longitude is number &&
             location.latitude >= -90 && location.latitude <= 90 &&
             location.longitude >= -180 && location.longitude <= 180;
    }
    
    // ============================================
    // ROUTES COLLECTION
    // ============================================
    
    match /routes/{routeId} {
      // Allow read if authenticated and user owns the route
      allow read: if isAuthenticated() && 
                     resourceExists() && 
                     isOwner(resource.data.userId);
      
      // Allow create if:
      // - User is authenticated
      // - userId matches auth user
      // - Route data is valid
      // - Origin and destination are valid locations
      allow create: if isAuthenticated() && 
                       isOwner(request.resource.data.userId) &&
                       isValidRoute() &&
                       isValidLocation(request.resource.data.origin) &&
                       isValidLocation(request.resource.data.destination);
      
      // Allow update if:
      // - User is authenticated
      // - User owns the route
      // - userId cannot be changed
      allow update: if isAuthenticated() && 
                       resourceExists() &&
                       isOwner(resource.data.userId) &&
                       request.resource.data.userId == resource.data.userId;
      
      // Allow delete if user owns the route
      allow delete: if isAuthenticated() && 
                       resourceExists() &&
                       isOwner(resource.data.userId);
    }
    
    // ============================================
    // USER PROFILES COLLECTION
    // ============================================
    
    match /users/{userId} {
      // Validate user profile fields
      function isValidProfile() {
        let data = request.resource.data;
        return data.keys().hasAll(['userId', 'fullName', 'email', 'phoneNumber']) &&
               data.userId is string &&
               data.fullName is string &&
               data.email is string &&
               data.phoneNumber is string &&
               data.userId == userId;
      }
      
      // User can read their own profile
      allow read: if isAuthenticated() && isOwner(userId);
      
      // User can create their own profile with valid data
      allow create: if isAuthenticated() && 
                       isOwner(userId) &&
                       isValidProfile();
      
      // User can update their own profile
      allow update: if isAuthenticated() && 
                       isOwner(userId) &&
                       request.resource.data.userId == resource.data.userId; // userId cannot change
      
      // User can delete their own profile
      allow delete: if isAuthenticated() && isOwner(userId);
    }
    
    // ============================================
    // TRIP HISTORY COLLECTION (Future)
    // ============================================
    
    match /trips/{tripId} {
      // User can read/write their own trips
      allow read, write: if isAuthenticated() && 
                            resourceExists() &&
                            isOwner(resource.data.userId);
    }
    
    // ============================================
    // SHARED LOCATIONS COLLECTION (Future)
    // ============================================
    
    match /sharedLocations/{locationId} {
      // Anyone authenticated can read shared locations
      allow read: if isAuthenticated();
      
      // Only owner can write/delete
      allow create: if isAuthenticated() && 
                       isOwner(request.resource.data.userId);
      allow update, delete: if isAuthenticated() && 
                               resourceExists() &&
                               isOwner(resource.data.userId);
    }
    
    // ============================================
    // DENY ALL OTHER ACCESS
    // ============================================
    
    // Explicitly deny all other access
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

## Test Mode Rules (For Development Only)

⚠️ **WARNING**: Only use these during initial development. Replace with production rules before launch!

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      // Allow read/write for authenticated users only
      allow read, write: if request.auth != null;
    }
  }
}
```

## Storage Security Rules (If Using Cloud Storage)

For Firebase Storage, use these rules to secure user-uploaded files:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    
    // User profile pictures
    match /users/{userId}/profile/{fileName} {
      // Allow read by anyone authenticated
      allow read: if request.auth != null;
      
      // Allow write only by owner, max 5MB
      allow write: if request.auth != null && 
                      request.auth.uid == userId &&
                      request.resource.size < 5 * 1024 * 1024 &&
                      request.resource.contentType.matches('image/.*');
    }
    
    // Route photos
    match /routes/{routeId}/photos/{fileName} {
      // Allow read by anyone authenticated
      allow read: if request.auth != null;
      
      // Allow write by authenticated users, max 10MB
      allow write: if request.auth != null &&
                      request.resource.size < 10 * 1024 * 1024 &&
                      request.resource.contentType.matches('image/.*');
    }
    
    // Trip recordings or logs
    match /trips/{tripId}/data/{fileName} {
      // Allow read/write only by owner
      allow read, write: if request.auth != null &&
                            request.auth.uid == request.path[2]; // matches tripId owner
    }
    
    // Deny all other access
    match /{allPaths=**} {
      allow read, write: if false;
    }
  }
}
```

## How to Apply Rules

### 1. **Firestore Rules**
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **fleet-rite-470802-h8**
3. Click **Firestore Database** in left menu
4. Click **Rules** tab at top
5. Replace existing rules with production rules above
6. Click **Publish**

### 2. **Storage Rules** (If using Cloud Storage)
1. In Firebase Console, select your project
2. Click **Storage** in left menu
3. Click **Rules** tab
4. Replace existing rules with storage rules above
5. Click **Publish**

## Testing Your Rules

### Test in Firebase Console
1. Go to Firestore Database → Rules
2. Click **Rules playground** button
3. Test scenarios:

**Test 1: Authenticated user reads own route**
```
Operation: get
Location: /databases/(default)/documents/routes/route123
Auth: user_id_123
```
✅ Should allow (if userId in document matches)

**Test 2: Unauthenticated read**
```
Operation: get
Location: /databases/(default)/documents/routes/route123
Auth: none
```
❌ Should deny

**Test 3: User creates route with correct userId**
```
Operation: create
Location: /databases/(default)/documents/routes/newroute
Auth: user_id_123
Data: { userId: "user_id_123", routeName: "Test", ... }
```
✅ Should allow

**Test 4: User creates route with different userId**
```
Operation: create
Location: /databases/(default)/documents/routes/newroute
Auth: user_id_123
Data: { userId: "user_id_456", routeName: "Test", ... }
```
❌ Should deny

### Test in Your App

Use these functions to test:

```kotlin
// Test 1: Save a route (should succeed)
val route = SavedRoute(
    userId = FirebaseAuth.getInstance().currentUser!!.uid,
    routeName = "Test Route",
    origin = RouteLocation(40.7128, -74.0060, "NYC"),
    destination = RouteLocation(40.7589, -73.9851, "Times Square"),
    distance = 5200.0,
    duration = 720
)

firestoreRepo.saveRoute(route).onSuccess {
    println("✅ Route saved successfully")
}.onFailure {
    println("❌ Error: ${it.message}")
}

// Test 2: Try to save route with wrong userId (should fail)
val badRoute = route.copy(userId = "wrong_user_id")
firestoreRepo.saveRoute(badRoute).onFailure {
    println("✅ Correctly denied: ${it.message}")
}

// Test 3: Read your own routes (should succeed)
firestoreRepo.getUserRoutes(currentUserId).onSuccess { routes ->
    println("✅ Retrieved ${routes.size} routes")
}

// Test 4: Try to read without auth (should fail)
// Sign out first, then try to read
FirebaseAuth.getInstance().signOut()
firestoreRepo.getUserRoutes(currentUserId).onFailure {
    println("✅ Correctly denied when not authenticated")
}
```

## Rule Validation Checklist

Before going to production, verify:

- ✅ Only authenticated users can access data
- ✅ Users can only access their own routes
- ✅ userId field cannot be modified after creation
- ✅ Route data validation prevents invalid entries
- ✅ Latitude/longitude are within valid ranges
- ✅ Distance and duration are non-negative
- ✅ All required fields are present
- ✅ Unauthenticated requests are denied
- ✅ Cross-user data access is blocked
- ✅ File size limits enforced in Storage rules

## Common Issues

### Issue 1: "Missing or insufficient permissions"
**Cause**: User not authenticated or accessing another user's data
**Solution**: Ensure user is signed in and accessing their own userId

### Issue 2: "Document does not match required fields"
**Cause**: Missing required fields in SavedRoute
**Solution**: Ensure all required fields (userId, routeName, origin, destination, distance, duration) are present

### Issue 3: "Invalid location coordinates"
**Cause**: Latitude/longitude out of range
**Solution**: Validate coordinates before saving (lat: -90 to 90, lng: -180 to 180)

### Issue 4: "Cannot modify userId"
**Cause**: Attempting to change userId on update
**Solution**: Don't include userId in update operations, or ensure it matches existing value

## Monitoring Rules

### View rule usage in Firebase Console:
1. Go to Firestore Database → Usage tab
2. Monitor read/write operations
3. Check for denied requests (indicates potential security issues or app bugs)

### Enable audit logging:
1. Go to IAM & Admin → Audit Logs
2. Enable "Data Read" and "Data Write" for Firestore
3. View logs in Cloud Logging

## Best Practices

1. **Never use test mode in production** - Always use proper authentication rules
2. **Validate all user input** - Check data types, ranges, and required fields
3. **Use helper functions** - Makes rules more readable and maintainable
4. **Test thoroughly** - Use Rules Playground and app testing
5. **Monitor regularly** - Check for denied requests indicating bugs or attacks
6. **Limit data exposure** - Only allow access to necessary data
7. **Version control your rules** - Keep rules in git alongside your code
8. **Document rule changes** - Explain why rules were modified

## Migration Plan

**Phase 1: Development (Current)**
- Use test mode with authentication requirement
- Focus on app development

**Phase 2: Internal Testing**
- Apply production rules
- Test all CRUD operations
- Verify security with multiple test accounts

**Phase 3: Beta Launch**
- Enable audit logging
- Monitor for rule violations
- Adjust rules based on real usage

**Phase 4: Production**
- Final rule review
- Enable alerts for denied requests
- Regular security audits

---

**Current Status**: Rules ready to apply!
**Next Step**: Copy production rules into Firebase Console → Firestore → Rules → Publish

Need help? Check [Firebase Security Rules Documentation](https://firebase.google.com/docs/firestore/security/get-started)
