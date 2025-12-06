package org.aurora.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import org.aurora.auth.model.User
import org.aurora.database.Database
import org.aurora.ui.auth.LoginScreen
import org.aurora.ui.auth.RegisterScreen
import org.aurora.ui.dashboard.DashboardScreen

enum class AuthScreen {
    LOGIN, REGISTER, DASHBOARD
}

fun main() = application {
    // Initialize database
    DisposableEffect(Unit) {
        Database.getConnection()
        onDispose {
            Database.close()
        }
    }
    
    var currentScreen by remember { mutableStateOf(AuthScreen.LOGIN) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Aurora - AI Traffic Orchestration",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        when (currentScreen) {
            AuthScreen.LOGIN -> {
                LoginScreen(
                    onLoginSuccess = { user ->
                        currentUser = user
                        currentScreen = AuthScreen.DASHBOARD
                    },
                    onNavigateToRegister = {
                        currentScreen = AuthScreen.REGISTER
                    }
                )
            }
            
            AuthScreen.REGISTER -> {
                RegisterScreen(
                    onRegisterSuccess = { user ->
                        currentUser = user
                        currentScreen = AuthScreen.DASHBOARD
                    },
                    onNavigateToLogin = {
                        currentScreen = AuthScreen.LOGIN
                    }
                )
            }
            
            AuthScreen.DASHBOARD -> {
                currentUser?.let { user ->
                    DashboardScreen(
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
}
