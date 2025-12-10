package org.aurora.android

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import io.ktor.client.*
import io.ktor.client.engine.android.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aurora.android.auth.model.User
import org.aurora.android.auth.service.AuthService
import org.aurora.android.theme.AuroraTheme
import org.aurora.android.ui.auth.LoginScreen
import org.aurora.android.ui.auth.RegisterScreen
import org.aurora.android.maps.GoogleMapsProvider
import org.aurora.android.ui.AuroraRiderApp

class MainActivity : ComponentActivity() {
    private lateinit var authService: AuthService
    private val httpClient = HttpClient(Android)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authService = AuthService(applicationContext)
        
        // Initialize Google Maps Provider with your API key
        val apiKey = "YOUR_GOOGLE_MAPS_API_KEY" // TODO: Add your API key
        val mapsProvider = GoogleMapsProvider(apiKey, httpClient)

        setContent {
            AuroraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuroraApp(authService, mapsProvider)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        httpClient.close()
    }
}

enum class AuthScreen {
    LOGIN, REGISTER, HOME
}

@Composable
fun AuroraApp(authService: AuthService, mapsProvider: org.aurora.android.maps.MapsProvider) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("aurora_prefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    
    var currentScreen by remember { mutableStateOf(AuthScreen.LOGIN) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    var isCheckingAuth by remember { mutableStateOf(true) }

    // Auto-login check
    LaunchedEffect(Unit) {
        val rememberMe = prefs.getBoolean("remember_me", false)
        if (rememberMe) {
            val savedEmail = prefs.getString("saved_email", null)
            val savedPassword = prefs.getString("saved_password", null)
            
            if (savedEmail != null && savedPassword != null) {
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.IO) {
                            authService.login(savedEmail, savedPassword)
                        }
                        result.onSuccess { user ->
                            currentUser = user
                            currentScreen = AuthScreen.HOME
                        }
                    } catch (e: Exception) {
                        // Failed auto-login, clear saved credentials
                        prefs.edit().clear().apply()
                    } finally {
                        isCheckingAuth = false
                    }
                }
            } else {
                isCheckingAuth = false
            }
        } else {
            isCheckingAuth = false
        }
    }

    if (isCheckingAuth) {
        // Show loading while checking auto-login
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }

    when (currentScreen) {
        AuthScreen.LOGIN -> {
            LoginScreen(
                onLoginSuccess = { user ->
                    currentUser = user
                    currentScreen = AuthScreen.HOME
                },
                onNavigateToRegister = {
                    currentScreen = AuthScreen.REGISTER
                },
                authService = authService
            )
        }
        
        AuthScreen.REGISTER -> {
            RegisterScreen(
                onRegisterSuccess = { user ->
                    currentUser = user
                    currentScreen = AuthScreen.HOME
                },
                onNavigateToLogin = {
                    currentScreen = AuthScreen.LOGIN
                },
                authService = authService
            )
        }
        
        AuthScreen.HOME -> {
            currentUser?.let { user ->
                // Use the new MainNavigationApp with bottom navigation
                org.aurora.android.ui.MainNavigationApp(
                    userName = user.fullName,
                    userEmail = user.email,
                    onLogout = {
                        // Clear saved credentials on logout
                        prefs.edit().clear().apply()
                        currentUser = null
                        currentScreen = AuthScreen.LOGIN
                    }
                )
            }
        }
    }
}
