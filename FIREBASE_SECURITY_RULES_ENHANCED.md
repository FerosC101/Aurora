# Firebase Security Rules for Aurora App (sfse database)

Copy these rules to Firebase Console → Firestore Database → Rules tab

```javascript
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
    
    function isParticipant(participants) {
      return isAuthenticated() && request.auth.uid in participants;
    }
    
    // Users collection
    match /users/{userId} {
      allow read: if isAuthenticated();
      allow write: if isOwner(userId);
    }
    
    // Friends collection
    match /friends/{userId}/userFriends/{friendId} {
      allow read, write: if isAuthenticated() && request.auth.uid == userId;
    }
    
    // Friend requests
    match /friendRequests/{requestId} {
      allow create: if isAuthenticated();
      allow read: if isAuthenticated() && 
        (resource.data.fromUserId == request.auth.uid || resource.data.toUserId == request.auth.uid);
      allow update: if isAuthenticated() && resource.data.toUserId == request.auth.uid;
      allow delete: if isAuthenticated() && 
        (resource.data.fromUserId == request.auth.uid || resource.data.toUserId == request.auth.uid);
    }
    
    // Chats
    match /chats/{chatId} {
      allow create: if isAuthenticated();
      allow read, update: if isAuthenticated() && request.auth.uid in resource.data.participants;
      allow delete: if isAuthenticated() && request.auth.uid in resource.data.participants;
    }
    
    // Messages
    match /messages/{messageId} {
      allow create: if isAuthenticated();
      allow read: if isAuthenticated();
      allow delete: if isAuthenticated() && resource.data.senderId == request.auth.uid;
    }
    
    // Notifications
    match /notifications/{notificationId} {
      allow create: if isAuthenticated();
      allow read, update, delete: if isAuthenticated() && resource.data.userId == request.auth.uid;
    }
    
    // User locations - allow friends to read each other's locations
    match /user_locations/{userId} {
      allow read: if isAuthenticated();
      allow write: if isOwner(userId);
    }
    
    // Carpools
    match /carpools/{carpool} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated();
      allow update, delete: if isAuthenticated() && resource.data.driverId == request.auth.uid;
    }
    
    // Ride requests
    match /rideRequests/{request} {
      allow read: if isAuthenticated();
      allow create: if isAuthenticated();
      allow update, delete: if isAuthenticated() && resource.data.requesterId == request.auth.uid;
    }
    
    // Trip history
    match /trips/{tripId} {
      allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
      allow read, update, delete: if isAuthenticated() && resource.data.userId == request.auth.uid;
    }
    
    // Costs
    match /costs/{costId} {
      allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
      allow read, update, delete: if isAuthenticated() && resource.data.userId == request.auth.uid;
    }
    
    // Saved routes
    match /savedRoutes/{routeId} {
      allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
      allow read, update, delete: if isAuthenticated() && resource.data.userId == request.auth.uid;
    }
    
    // Chat history
    match /chatHistory/{messageId} {
      allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
      allow read, update, delete: if isAuthenticated() && resource.data.userId == request.auth.uid;
    }
    
    // Vehicle profiles
    match /vehicleProfiles/{profileId} {
      allow create: if isAuthenticated() && request.resource.data.userId == request.auth.uid;
      allow read, update, delete: if isAuthenticated() && resource.data.userId == request.auth.uid;
    }
    
    // User settings
    match /userSettings/{userId} {
      allow read, write: if isOwner(userId);
    }
  }
}
```

## Collections Overview

### New Collections Added:
- **friendRequests**: Stores pending, accepted, rejected friend requests
- **chats**: Stores chat conversations between users
- **messages**: Stores individual chat messages
- **notifications**: Stores user notifications

### Updated Collections:
- **carpools**: Enhanced with more fields (phone, rating, vehicle details, amenities)
- **rideRequests**: Enhanced with date/time, pricing, preferences

## Testing the Rules

After updating, test by:
1. Sending a friend request
2. Accepting/rejecting requests
3. Sending chat messages
4. Creating carpool listings
5. Creating ride requests

All operations should work without PERMISSION_DENIED errors.
