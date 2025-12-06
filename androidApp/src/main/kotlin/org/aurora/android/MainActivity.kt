package org.aurora.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.aurora.android.auth.model.User
import org.aurora.android.auth.service.AuthService
import org.aurora.android.theme.AuroraTheme
import org.aurora.android.ui.auth.LoginScreen
import org.aurora.android.ui.auth.RegisterScreen
import org.aurora.android.ui.HomeScreen

class MainActivity : ComponentActivity() {
    private lateinit var authService: AuthService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        authService = AuthService(applicationContext)

        setContent {
            AuroraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuroraApp(authService)
                }
            }
        }
    }
}

enum class AuthScreen {
    LOGIN, REGISTER, HOME
}

@Composable
fun AuroraApp(authService: AuthService) {
    var currentScreen by remember { mutableStateOf(AuthScreen.LOGIN) }
    var currentUser by remember { mutableStateOf<User?>(null) }

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
                HomeScreen(
                    user = user,
                    onLogout = {
                        currentUser = null
                        currentScreen = AuthScreen.LOGIN
                    }
                )
            }
        }
    }
}
