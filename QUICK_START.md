# Aurora - Quick Start Guide

## 🚀 Getting Started

### Prerequisites
- JDK 17 or higher installed
- Gradle 8.9 (included via wrapper)

### Running the Application

1. **Clone and Navigate**
   ```bash
   cd Aurora
   ```

2. **Build the Project**
   ```bash
   ./gradlew build
   ```

3. **Run the Desktop App**
   ```bash
   ./gradlew desktopApp:run
   ```

## 📱 Using the Application

### First Time Setup

1. **Registration**
   - Launch the application
   - Click "Register" button
   - Fill in your details:
     - Full Name: Your name
     - Email: Valid email format
     - Password: At least 8 characters
     - Confirm Password: Must match
   - Click "Create Account"
   - You'll be automatically logged in

2. **Login**
   - Enter your registered email
   - Enter your password
   - Click "Sign In"
   - Access your dashboard

### Dashboard Features

Once logged in, you'll see:
- **Welcome Card**: Personalized greeting
- **Stats Panel**: Active routes, traffic lights, and alerts (coming soon)
- **Feature Cards**: Traffic monitoring and route optimization (coming soon)
- **Account Info**: Your email, user ID, and registration date
- **Logout**: Click the exit icon in the top-right

## 🗄️ Database

The application creates a local SQLite database (`aurora.db`) automatically.

**Location**: Same directory as the application

**Reset Database**:
```bash
# Delete the database file to start fresh
rm aurora.db  # Linux/Mac
del aurora.db # Windows
```

## 🎨 UI Screenshots

### Login Screen
- Modern dark theme
- Email and password fields
- Password visibility toggle
- Demo access info
- Link to registration

### Register Screen
- Full name input
- Email validation
- Password strength requirements
- Password confirmation
- Terms agreement
- Link back to login

### Dashboard
- Top navigation bar with logo
- User profile display
- Stats cards
- Feature cards
- Account information
- Logout button

## 🔐 Security Features

- ✅ BCrypt password hashing
- ✅ Email validation
- ✅ SQL injection prevention
- ✅ Password strength requirements
- ✅ Unique email constraints
- ✅ Input sanitization

## 📊 Project Structure

```
Aurora/
├── shared/              # Shared code (auth, UI, database)
│   ├── auth/           # Authentication logic
│   ├── database/       # SQLite connection
│   └── ui/             # Compose UI screens
├── desktopApp/         # Desktop application
├── app/                # CLI utilities
└── utils/              # Shared utilities
```

## 🛠️ Development

### Build Commands

```bash
# Clean build
./gradlew clean build

# Run desktop app
./gradlew desktopApp:run

# Run tests
./gradlew test

# Stop Gradle daemons
./gradlew --stop
```

### Module Status

✅ **Working**:
- shared (Authentication, Database, UI)
- desktopApp (Desktop application)
- app (CLI utilities)
- utils (Helper functions)

⏸️ **Temporarily Disabled**:
- androidApp (AGP compatibility issues)

## 🐛 Troubleshooting

### Build Fails
```bash
./gradlew clean
./gradlew --stop
./gradlew build
```

### Database Locked
- Close all running instances
- Delete `aurora.db`
- Restart application

### Login Issues
- Verify email format is correct
- Password is case-sensitive
- Try creating a new account

## 📚 Documentation

- [AUTHENTICATION_GUIDE.md](AUTHENTICATION_GUIDE.md) - Detailed auth system docs
- [README.md](README.md) - Project overview

## 🎯 Next Steps

1. **Explore the Dashboard**: Check out the UI components
2. **Test Registration**: Create multiple users
3. **Review the Code**: Examine the authentication flow
4. **Extend Features**: Add new functionality to the dashboard

## ⚡ Quick Test

```bash
# Test credentials (create via registration):
Email: test@example.com
Password: test1234

# Or use demo mode with any valid format:
Email: demo@aurora.com
Password: demopass123
```

## 🔗 Helpful Commands

```bash
# Check Gradle version
./gradlew --version

# List all tasks
./gradlew tasks

# Build specific module
./gradlew :shared:build

# Run with debug output
./gradlew desktopApp:run --info
```

## 💡 Tips

1. **Password Visibility**: Click the eye icon to toggle
2. **Demo Access**: Use the info panel on login screen
3. **Logout**: Always logout before closing for proper cleanup
4. **Fresh Start**: Delete `aurora.db` to reset everything

## 🚧 Coming Soon

- [ ] Password reset functionality
- [ ] Email verification
- [ ] User profile editing
- [ ] Session management
- [ ] Traffic simulation features
- [ ] Route optimization
- [ ] Real-time monitoring

---

**Need Help?** Check the [AUTHENTICATION_GUIDE.md](AUTHENTICATION_GUIDE.md) for more details!
