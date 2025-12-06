package org.aurora.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import org.aurora.auth.model.User
import org.aurora.database.Database
import org.aurora.rider.model.RiderCityMap
import org.aurora.rider.simulation.RiderSimulationEngine
import org.aurora.ui.auth.LoginScreen
import org.aurora.ui.auth.RegisterScreen
import org.aurora.ui.rider.RiderOSDashboard

enum class AuthScreen {
    LOGIN, REGISTER, SIMULATION
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
    var riderCityMap by remember { mutableStateOf<RiderCityMap?>(null) }
    var simulationEngine by remember { mutableStateOf<RiderSimulationEngine?>(null) }
    
    Window(
        onCloseRequest = ::exitApplication,
        title = "Aurora 2.0 - RiderOS | AI-Powered Rider Optimization",
        state = rememberWindowState(width = 1600.dp, height = 900.dp)
    ) {
        when (currentScreen) {
            AuthScreen.LOGIN -> {
                LoginScreen(
                    onLoginSuccess = { user ->
                        currentUser = user
                        // Initialize RiderOS simulation
                        riderCityMap = RiderCityMap()
                        simulationEngine = RiderSimulationEngine(riderCityMap!!)
                        riderCityMap!!.spawnRiders(30)
                        currentScreen = AuthScreen.SIMULATION
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
                        // Initialize RiderOS simulation
                        riderCityMap = RiderCityMap()
                        simulationEngine = RiderSimulationEngine(riderCityMap!!)
                        riderCityMap!!.spawnRiders(30)
                        currentScreen = AuthScreen.SIMULATION
                    },
                    onNavigateToLogin = {
                        currentScreen = AuthScreen.LOGIN
                    }
                )
            }
            
            AuthScreen.SIMULATION -> {
                currentUser?.let { _ ->
                    riderCityMap?.let { _ ->
                        simulationEngine?.let { engine ->
                            RiderOSDashboard(
                                simulationEngine = engine,
                                onLogout = {
                                    currentUser = null
                                    riderCityMap = null
                                    simulationEngine = null
                                    currentScreen = AuthScreen.LOGIN
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
