package org.aurora.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.aurora.android.ui.navigation.BottomNavItem
import org.aurora.android.ui.screens.*

@Composable
fun MainNavigationApp(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    var showBottomBar by remember { mutableStateOf(true) }
    
    // Hide bottom bar when navigating
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            showBottomBar = backStackEntry.destination.route in listOf(
                BottomNavItem.Home.route,
                BottomNavItem.Explore.route,
                BottomNavItem.Activity.route,
                BottomNavItem.Profile.route
            )
        }
    }
    
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(navController = navController)
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        NavigationGraph(
            navController = navController,
            userName = userName,
            userEmail = userEmail,
            onLogout = onLogout,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Explore,
        BottomNavItem.Activity,
        BottomNavItem.Profile
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF1E88E5),
                    selectedTextColor = Color(0xFF1E88E5),
                    indicatorColor = Color(0xFFE3F2FD),
                    unselectedIconColor = Color(0xFF9E9E9E),
                    unselectedTextColor = Color(0xFF9E9E9E)
                )
            )
        }
    }
}

@Composable
fun NavigationGraph(
    navController: NavHostController,
    userName: String,
    userEmail: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Home.route) {
            HomeScreen(
                onStartNavigation = { origin, destination ->
                    navController.navigate("navigation/$origin/$destination")
                }
            )
        }
        
        composable(BottomNavItem.Explore.route) {
            ExploreScreen(
                onRouteClick = { route ->
                    navController.navigate("navigation/${route.origin}/${route.destination}")
                }
            )
        }
        
        composable(BottomNavItem.Activity.route) {
            ActivityScreen()
        }
        
        composable(BottomNavItem.Profile.route) {
            ProfileScreen(
                userName = userName,
                userEmail = userEmail,
                onLogout = onLogout
            )
        }
        
        composable("navigation/{origin}/{destination}") { backStackEntry ->
            val origin = backStackEntry.arguments?.getString("origin") ?: ""
            val destination = backStackEntry.arguments?.getString("destination") ?: ""
            
            SimpleNavigationScreen(
                origin = origin,
                destination = destination,
                onBack = { navController.navigateUp() }
            )
        }
    }
}
