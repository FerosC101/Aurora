package org.aurora.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.aurora.auth.model.User
import org.aurora.config.AppConfig
import org.aurora.database.Database
import org.aurora.maps.MapsProviderFactory
import org.aurora.ui.auth.LoginScreen
import org.aurora.ui.auth.RegisterScreen
import org.aurora.ui.navigation.AuroraRiderApp

enum class AuthScreen {
    LOGIN, REGISTER, NAVIGATION
}

fun main() = application {
    // Initialize App Configuration with Google Maps API key
    AppConfig.initialize(
        googleMapsKey = "AIzaSyClM3oua_QM_fSy_9WgnhQK6jkoN50lGTc",
        useRealGPS = false,
        useLiveTraffic = true
    )
    
    // Create HTTP client for API requests
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
    
    // Initialize Maps Provider
    val mapsProvider = remember { MapsProviderFactory.create(httpClient) }
    // Initialize database
    DisposableEffect(Unit) {
        Database.getConnection()
        onDispose {
            httpClient.close()
            Database.close()
        }
    }
    
    var currentScreen by remember { mutableStateOf(AuthScreen.LOGIN) }
    var currentUser by remember { mutableStateOf<User?>(null) }
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Aurora Rider - AI-Powered Navigation for Riders",
        state = rememberWindowState(width = 1600.dp, height = 1000.dp)
    ) {
        when (currentScreen) {
            AuthScreen.LOGIN -> {
                LoginScreen(
                    onLoginSuccess = { user ->
                        currentUser = user
                        currentScreen = AuthScreen.NAVIGATION
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
                        currentScreen = AuthScreen.NAVIGATION
                    },
                    onNavigateToLogin = {
                        currentScreen = AuthScreen.LOGIN
                    }
                )
            }
            
            AuthScreen.NAVIGATION -> {
                currentUser?.let { user ->
                    AuroraRiderApp(
                        userId = user.id.toInt(),
                        mapsProvider = mapsProvider,
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
