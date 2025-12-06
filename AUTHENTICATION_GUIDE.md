# Aurora Authentication System

## Overview

Aurora now includes a complete authentication system with user registration, login, and a dashboard. The system uses SQLite for data persistence and BCrypt for secure password hashing.

## Features

### ✅ User Registration
- Full name, email, and password validation
- Email format validation
- Password strength requirements (minimum 8 characters)
- Duplicate email detection
- Password confirmation matching
- Secure password hashing with BCrypt

### ✅ User Login
- Email and password authentication
- Secure password verification
- Error handling with user-friendly messages
- Password visibility toggle

### ✅ Database Integration
- SQLite database for persistent storage
- Users table with proper schema
- Sessions table for future session management
- Automatic database initialization
- Foreign key constraints

### ✅ Dashboard
- Welcome message with user information
- User account details display
- Stats cards for future features
- Feature cards for upcoming functionality
- Logout capability with confirmation dialog

## Architecture

### Components

```
shared/
├── src/
│   └── desktopMain/
│       └── kotlin/
│           └── org/
│               └── aurora/
│                   ├── auth/
│                   │   ├── model/
│                   │   │   └── User.kt           # User data model
│                   │   ├── repository/
│                   │   │   └── UserRepository.kt # Database operations
│                   │   └── service/
│                   │       └── AuthService.kt    # Business logic
│                   ├── database/
│                   │   └── Database.kt           # SQLite connection
│                   └── ui/
│                       ├── auth/
│                       │   ├── LoginScreen.kt    # Login UI
│                       │   └── RegisterScreen.kt # Registration UI
│                       └── dashboard/
│                           └── DashboardScreen.kt # Main dashboard
```

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at INTEGER NOT NULL
)
```

### Sessions Table (Future Use)
```sql
CREATE TABLE sessions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    token TEXT UNIQUE NOT NULL,
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
)
```

## Security Features

1. **Password Hashing**: Uses BCrypt with salt for secure password storage
2. **Email Validation**: Regex-based email format validation
3. **Input Validation**: Comprehensive validation for all user inputs
4. **SQL Injection Prevention**: Parameterized queries throughout
5. **Unique Constraints**: Email uniqueness enforced at database level

## Usage

### Running the Application

```bash
# Build the project
./gradlew build

# Run the desktop application
./gradlew desktopApp:run
```

### User Flow

1. **First Time Users**:
   - Click "Register" on the login screen
   - Fill in full name, email, and password
   - Confirm password
   - Click "Create Account"
   - Automatically logged in to dashboard

2. **Returning Users**:
   - Enter email and password
   - Click "Sign In"
   - Access dashboard

3. **Dashboard**:
   - View welcome message and account info
   - Access traffic orchestration features (coming soon)
   - Logout when done

## Demo Access

For testing purposes, the system accepts any valid:
- Email: Must be in proper email format
- Password: Must be at least 6 characters (8+ recommended)

## Dependencies

```kotlin
// BCrypt for password hashing
implementation("org.mindrot:jbcrypt:0.4")

// SQLite JDBC driver
implementation("org.xerial:sqlite-jdbc:3.45.0.0")

// Compose UI components
implementation(compose.material3)
implementation(compose.materialIconsExtended)
```

## Future Enhancements

- [ ] Session management with tokens
- [ ] Remember me functionality
- [ ] Password reset flow
- [ ] Email verification
- [ ] Two-factor authentication
- [ ] User profile editing
- [ ] Account deletion
- [ ] OAuth integration (Google, GitHub, etc.)
- [ ] Role-based access control
- [ ] Activity logging

## Technical Notes

### Password Security
- BCrypt is used with automatic salt generation
- Cost factor: 10 (default, provides good security/performance balance)
- Passwords are never stored in plain text
- Password comparison uses constant-time algorithm

### Database Location
- SQLite database file: `aurora.db` in the application directory
- Automatically created on first run
- Tables initialized on application startup

### Error Handling
- User-friendly error messages
- SQL exceptions caught and translated
- Validation errors displayed inline
- Network/database errors handled gracefully

## Troubleshooting

### Database Issues
If you encounter database errors:
1. Delete `aurora.db` file
2. Restart the application
3. Database will be recreated automatically

### Build Issues
```bash
# Clean and rebuild
./gradlew clean build

# Stop Gradle daemons
./gradlew --stop
```

## API Reference

### AuthService

```kotlin
class AuthService {
    fun register(fullName: String, email: String, password: String): Result<User>
    fun login(email: String, password: String): Result<User>
}
```

### UserRepository

```kotlin
class UserRepository {
    fun createUser(fullName: String, email: String, passwordHash: String): Result<User>
    fun findByEmail(email: String): User?
    fun findById(id: Long): User?
    fun deleteUser(id: Long): Boolean
}
```

## Contributing

When extending the authentication system:

1. Follow existing code patterns
2. Add proper error handling
3. Write validation for all inputs
4. Use parameterized queries
5. Update this documentation

## License

Part of the Aurora project - See main LICENSE file
