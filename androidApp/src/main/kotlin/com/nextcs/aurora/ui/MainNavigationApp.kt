package com.nextcs.aurora.ui

import android.util.Log
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
import com.google.android.gms.maps.model.LatLng
import com.nextcs.aurora.ui.navigation.BottomNavItem
import com.nextcs.aurora.ui.screens.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
                BottomNavItem.Assistant.route,
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
        BottomNavItem.Assistant,
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
    // Shared state for location selection across navigation
    var selectedOrigin by remember { mutableStateOf("") }
    var selectedDestination by remember { mutableStateOf("") }
    var selectedOriginLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedDestinationLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedRoute by remember { mutableStateOf<com.nextcs.aurora.navigation.RouteAlternative?>(null) }
    var selectedWaypoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    
    var mapPickerCallback by remember { mutableStateOf<((LatLng, String) -> Unit)?>(null) }
    var mapPickerTitle by remember { mutableStateOf("Select Location") }
    var mapPickerInitialLocation by remember { mutableStateOf<LatLng?>(null) }
    
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Home.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Home.route) {
            // Use shared state from NavigationGraph scope
            var origin by remember { mutableStateOf(selectedOrigin) }
            var destination by remember { mutableStateOf(selectedDestination) }
            var originLocation by remember { mutableStateOf(selectedOriginLocation) }
            var destinationLocation by remember { mutableStateOf(selectedDestinationLocation) }
            
            // Sync back to shared state when changed
            LaunchedEffect(origin, destination, originLocation, destinationLocation) {
                selectedOrigin = origin
                selectedDestination = destination
                selectedOriginLocation = originLocation
                selectedDestinationLocation = destinationLocation
            }
            
            HomeScreen(
                initialOrigin = origin,
                initialDestination = destination,
                initialOriginLocation = originLocation,
                initialDestinationLocation = destinationLocation,
                onStateChange = { newOrigin, newDest, newOrigLoc, newDestLoc ->
                    origin = newOrigin
                    destination = newDest
                    originLocation = newOrigLoc
                    destinationLocation = newDestLoc
                },
                onStartNavigation = { orig, dest ->
                    // Encode text addresses
                    val encodedOrigin = URLEncoder.encode(orig, StandardCharsets.UTF_8.toString())
                    val encodedDestination = URLEncoder.encode(dest, StandardCharsets.UTF_8.toString())
                    
                    // Encode coordinates if available
                    val origLat = originLocation?.latitude?.toString() ?: ""
                    val origLng = originLocation?.longitude?.toString() ?: ""
                    val destLat = destinationLocation?.latitude?.toString() ?: ""
                    val destLng = destinationLocation?.longitude?.toString() ?: ""
                    
                    navController.navigate(
                        "navigation/$encodedOrigin/$encodedDestination/$origLat/$origLng/$destLat/$destLng"
                    )
                },
                onMultiStopNavigation = {
                    navController.navigate("multistop")
                },
                onMapPicker = { title, initialLocation, callback ->
                    mapPickerTitle = title
                    mapPickerInitialLocation = initialLocation
                    mapPickerCallback = { location, address ->
                        Log.d("MainNavApp", "Invoking callback with: $location, $address")
                        callback(location, address)
                        // Update shared state immediately
                        if (title.contains("Origin")) {
                            selectedOrigin = address
                            selectedOriginLocation = location
                        } else {
                            selectedDestination = address
                            selectedDestinationLocation = location
                        }
                    }
                    navController.navigate("mappicker")
                }
            )
        }
        
        composable(BottomNavItem.Explore.route) {
            ExploreScreen(
                onRouteClick = { route ->
                    val encodedOrigin = URLEncoder.encode(route.origin, StandardCharsets.UTF_8.toString())
                    val encodedDestination = URLEncoder.encode(route.destination, StandardCharsets.UTF_8.toString())
                    navController.navigate("navigation/$encodedOrigin/$encodedDestination")
                }
            )
        }
        
        composable(BottomNavItem.Assistant.route) {
            AIAssistantScreen(
                onNavigateToRoute = { origin, destination, waypoints ->
                    // TODO: Resolve addresses to coordinates using PlacesAutocompleteService
                    // For now, navigate to multi-stop planning screen
                    navController.navigate("multistop")
                },
                onBack = { navController.navigateUp() }
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
        
        composable("navigation/{origin}/{destination}/{origLat}/{origLng}/{destLat}/{destLng}") { backStackEntry ->
            val encodedOrigin = backStackEntry.arguments?.getString("origin") ?: ""
            val encodedDestination = backStackEntry.arguments?.getString("destination") ?: ""
            val origin = URLDecoder.decode(encodedOrigin, StandardCharsets.UTF_8.toString())
            val destination = URLDecoder.decode(encodedDestination, StandardCharsets.UTF_8.toString())
            
            // Parse coordinates
            val origLat = backStackEntry.arguments?.getString("origLat")?.toDoubleOrNull()
            val origLng = backStackEntry.arguments?.getString("origLng")?.toDoubleOrNull()
            val destLat = backStackEntry.arguments?.getString("destLat")?.toDoubleOrNull()
            val destLng = backStackEntry.arguments?.getString("destLng")?.toDoubleOrNull()
            
            val originLocation = if (origLat != null && origLng != null) {
                LatLng(origLat, origLng)
            } else null
            
            val destinationLocation = if (destLat != null && destLng != null) {
                LatLng(destLat, destLng)
            } else null
            
            RealNavigationScreen(
                origin = origin,
                destination = destination,
                originLocation = originLocation,
                destinationLocation = destinationLocation,
                waypoints = selectedWaypoints,
                selectedRoute = selectedRoute,
                onBack = {
                    selectedWaypoints = emptyList() // Clear waypoints when leaving
                    navController.navigateUp()
                },
                onViewAlternativeRoutes = {
                    // Pass coordinates to alternative routes for accurate route calculation
                    val origLatStr = (originLocation?.latitude ?: 14.5995).toString()
                    val origLngStr = (originLocation?.longitude ?: 120.9842).toString()
                    val destLatStr = (destinationLocation?.latitude ?: 14.6000).toString()
                    val destLngStr = (destinationLocation?.longitude ?: 120.9593).toString()
                    navController.navigate("alternatives/$encodedOrigin/$encodedDestination/$origLatStr/$origLngStr/$destLatStr/$destLngStr")
                }
            )
        }
        
        composable("alternatives/{origin}/{destination}/{origLat}/{origLng}/{destLat}/{destLng}") { backStackEntry ->
            val encodedOrigin = backStackEntry.arguments?.getString("origin") ?: ""
            val encodedDestination = backStackEntry.arguments?.getString("destination") ?: ""
            val origin = URLDecoder.decode(encodedOrigin, StandardCharsets.UTF_8.toString())
            val destination = URLDecoder.decode(encodedDestination, StandardCharsets.UTF_8.toString())
            
            val origLat = backStackEntry.arguments?.getString("origLat")?.toDoubleOrNull()
            val origLng = backStackEntry.arguments?.getString("origLng")?.toDoubleOrNull()
            val destLat = backStackEntry.arguments?.getString("destLat")?.toDoubleOrNull()
            val destLng = backStackEntry.arguments?.getString("destLng")?.toDoubleOrNull()
            
            val originLocation = if (origLat != null && origLng != null) {
                LatLng(origLat, origLng)
            } else null
            
            val destinationLocation = if (destLat != null && destLng != null) {
                LatLng(destLat, destLng)
            } else null
            
            AlternativeRoutesScreen(
                origin = origin,
                destination = destination,
                originLocation = originLocation,
                destinationLocation = destinationLocation,
                onRouteSelected = { route ->
                    // Store the selected route and return to navigation
                    selectedRoute = route
                    navController.navigateUp()
                },
                onBack = { navController.navigateUp() }
            )
        }
        
        composable("multistop") {
            MultiStopPlanningScreen(
                onStartNavigation = { waypoints ->
                    // Extract origin and destination with their coordinates
                    val firstWaypoint = waypoints.firstOrNull()
                    val lastWaypoint = waypoints.lastOrNull()
                    
                    if (firstWaypoint != null && lastWaypoint != null) {
                        val origin = firstWaypoint.name
                        val destination = lastWaypoint.name
                        val encodedOrigin = URLEncoder.encode(origin, StandardCharsets.UTF_8.toString())
                        val encodedDestination = URLEncoder.encode(destination, StandardCharsets.UTF_8.toString())
                        
                        // Extract coordinates from Position object (x=lat, y=lng)
                        val origLat = firstWaypoint.position.x.toString()
                        val origLng = firstWaypoint.position.y.toString()
                        val destLat = lastWaypoint.position.x.toString()
                        val destLng = lastWaypoint.position.y.toString()
                        
                        // Extract intermediate waypoints (exclude first and last)
                        selectedWaypoints = waypoints
                            .drop(1) // Skip origin
                            .dropLast(1) // Skip destination
                            .map { LatLng(it.position.x.toDouble(), it.position.y.toDouble()) }
                        
                        navController.navigate("navigation/$encodedOrigin/$encodedDestination/$origLat/$origLng/$destLat/$destLng") {
                            popUpTo("multistop") { inclusive = true }
                        }
                    }
                },
                onBack = { navController.navigateUp() },
                onMapPicker = { title, initialLocation, callback ->
                    mapPickerTitle = title
                    mapPickerInitialLocation = initialLocation
                    mapPickerCallback = callback
                    navController.navigate("mappicker")
                }
            )
        }
        
        composable("mappicker") {
            MapPickerScreen(
                title = mapPickerTitle,
                initialLocation = mapPickerInitialLocation,
                onLocationSelected = { location, address ->
                    Log.d("MainNavApp", "MapPicker callback invoked: $location, $address")
                    mapPickerCallback?.invoke(location, address)
                    navController.navigateUp()
                },
                onBack = { navController.navigateUp() }
            )
        }
    }
}
