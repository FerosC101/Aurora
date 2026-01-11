package com.nextcs.aurora.ui

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.android.gms.maps.model.LatLng
import com.nextcs.aurora.location.LocationService
import com.nextcs.aurora.location.PlacesAutocompleteService
import com.nextcs.aurora.ui.navigation.BottomNavItem
import com.nextcs.aurora.ui.screens.*
import kotlinx.coroutines.launch
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
                BottomNavItem.Social.route,
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
        BottomNavItem.Social,
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
                    selectedIconColor = Color(0xFF1976D2),
                    selectedTextColor = Color(0xFF1976D2),
                    indicatorColor = Color(0xFFE3F2FD),
                    unselectedIconColor = Color(0xFF757575),
                    unselectedTextColor = Color(0xFF757575)
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    val placesService = remember { PlacesAutocompleteService(context) }
    
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
        modifier = modifier,
        enterTransition = { slideInHorizontally(animationSpec = tween(200), initialOffsetX = { it }) },
        exitTransition = { slideOutHorizontally(animationSpec = tween(200), targetOffsetX = { -it }) },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(200), initialOffsetX = { -it }) },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(200), targetOffsetX = { it }) }
    ) {
        composable(BottomNavItem.Home.route) {
            HomeScreen(
                initialOrigin = selectedOrigin,
                initialDestination = selectedDestination,
                initialOriginLocation = selectedOriginLocation,
                initialDestinationLocation = selectedDestinationLocation,
                onStateChange = { newOrigin, newDest, newOrigLoc, newDestLoc ->
                    selectedOrigin = newOrigin
                    selectedDestination = newDest
                    selectedOriginLocation = newOrigLoc
                    selectedDestinationLocation = newDestLoc
                },
                onStartNavigation = { orig, dest, origLoc, destLoc ->
                    // Clear selected route before starting new navigation
                    selectedRoute = null
                    selectedWaypoints = emptyList()
                    
                    // Update shared state
                    selectedOrigin = orig
                    selectedDestination = dest
                    selectedOriginLocation = origLoc
                    selectedDestinationLocation = destLoc
                    
                    // Encode text addresses
                    val encodedOrigin = URLEncoder.encode(orig, StandardCharsets.UTF_8.toString())
                    val encodedDest = URLEncoder.encode(dest, StandardCharsets.UTF_8.toString())
                    
                    // Get coordinates - use passed coordinates directly
                    val origLat = origLoc?.latitude?.toString() ?: ""
                    val origLng = origLoc?.longitude?.toString() ?: ""
                    val destLat = destLoc?.latitude?.toString() ?: ""
                    val destLng = destLoc?.longitude?.toString() ?: ""
                    
                    Log.d("MainNavApp", "Starting navigation: $orig -> $dest, Coords: ($origLat,$origLng) -> ($destLat,$destLng)")
                    
                    navController.navigate(
                        "navigation?origLat=$origLat&origLng=$origLng&destLat=$destLat&destLng=$destLng&origin=$encodedOrigin&dest=$encodedDest&waypoints="
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
                        // Update shared state immediately based on picker type
                        if (title.contains("Origin", ignoreCase = true)) {
                            selectedOrigin = address
                            selectedOriginLocation = location
                        } else if (title.contains("Destination", ignoreCase = true)) {
                            selectedDestination = address
                            selectedDestinationLocation = location
                        }
                    }
                    navController.navigate("mappicker")
                }
            )
        }
        
        composable(BottomNavItem.Social.route) {
            SocialScreen(
                userName = userName,
                userEmail = userEmail,
                onNavigateToFriend = { friendLocation ->
                    // Navigate to friend's location
                    val encodedOrigin = URLEncoder.encode("Current Location", StandardCharsets.UTF_8.toString())
                    val encodedDest = URLEncoder.encode(friendLocation.displayName, StandardCharsets.UTF_8.toString())
                    
                    scope.launch {
                        val currentLoc = locationService.getLastKnownLocation()
                        if (currentLoc != null) {
                            val origLat = currentLoc.latitude.toString()
                            val origLng = currentLoc.longitude.toString()
                            val destLat = friendLocation.latitude.toString()
                            val destLng = friendLocation.longitude.toString()
                            
                            navController.navigate(
                                "navigation?origLat=$origLat&origLng=$origLng&destLat=$destLat&destLng=$destLng&origin=$encodedOrigin&dest=$encodedDest&waypoints="
                            )
                        }
                    }
                },
                onNavigateToChat = {
                    navController.navigate("chats")
                },
                onNavigateToNotifications = {
                    navController.navigate("notifications")
                }
            )
        }
        
        // Chat Screen - New two-panel Facebook Messenger style
        composable("chats") {
            com.nextcs.aurora.ui.screens.ChatScreen(
                onBack = { navController.navigateUp() }
            )
        }
        
        // Individual Chat Screen (legacy route for backward compatibility)
        composable("chat/{chatId}/{otherUserName}") { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: ""
            com.nextcs.aurora.ui.screens.ChatScreenLegacy(
                chatId = chatId,
                otherUserName = otherUserName,
                onBack = { navController.navigateUp() }
            )
        }
        
        // Notifications Screen
        composable("notifications") {
            com.nextcs.aurora.ui.screens.NotificationsScreen(
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
        
        // Friends Screen - deprecated, now in Social tab
        composable("friends") {
            FriendsScreen()
        }
        
        // Navigation route using query parameters to avoid URL encoding issues
        composable(
            route = "navigation?origLat={origLat}&origLng={origLng}&destLat={destLat}&destLng={destLng}&origin={origin}&dest={dest}&waypoints={waypoints}",
            arguments = listOf(
                navArgument("origLat") { type = NavType.StringType },
                navArgument("origLng") { type = NavType.StringType },
                navArgument("destLat") { type = NavType.StringType },
                navArgument("destLng") { type = NavType.StringType },
                navArgument("origin") { type = NavType.StringType; defaultValue = "" },
                navArgument("dest") { type = NavType.StringType; defaultValue = "" },
                navArgument("waypoints") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val encodedOrigin = backStackEntry.arguments?.getString("origin") ?: ""
            val encodedDest = backStackEntry.arguments?.getString("dest") ?: ""
            val origin = URLDecoder.decode(encodedOrigin, StandardCharsets.UTF_8.toString())
            val destination = URLDecoder.decode(encodedDest, StandardCharsets.UTF_8.toString())
            
            // Parse coordinates
            val origLat = backStackEntry.arguments?.getString("origLat")?.toDoubleOrNull()
            val origLng = backStackEntry.arguments?.getString("origLng")?.toDoubleOrNull()
            val destLat = backStackEntry.arguments?.getString("destLat")?.toDoubleOrNull()
            val destLng = backStackEntry.arguments?.getString("destLng")?.toDoubleOrNull()
            
            // Parse waypoints if present (format: lat1,lng1|lat2,lng2|...)
            val waypointsString = backStackEntry.arguments?.getString("waypoints") ?: ""
            val waypointsList = if (waypointsString.isNotEmpty()) {
                waypointsString.split("|").mapNotNull { waypointStr ->
                    val parts = waypointStr.split(",")
                    if (parts.size == 2) {
                        val lat = parts[0].toDoubleOrNull()
                        val lng = parts[1].toDoubleOrNull()
                        if (lat != null && lng != null) LatLng(lat, lng) else null
                    } else null
                }
            } else emptyList()
            
            Log.d("MainNavApp", "Navigation route with ${waypointsList.size} waypoints")
            
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
                waypoints = waypointsList,  // Use parsed waypoints from URL
                selectedRoute = selectedRoute,
                onBack = {
                    // Clear navigation state when going back
                    selectedWaypoints = emptyList()
                    selectedRoute = null
                    navController.navigateUp()
                },
                onViewAlternativeRoutes = {
                    // Pass coordinates to alternative routes for accurate route calculation
                    val origLatStr = (originLocation?.latitude ?: 14.5995).toString()
                    val origLngStr = (originLocation?.longitude ?: 120.9842).toString()
                    val destLatStr = (destinationLocation?.latitude ?: 14.6000).toString()
                    val destLngStr = (destinationLocation?.longitude ?: 120.9593).toString()
                    navController.navigate("alternatives/${URLEncoder.encode(origin, StandardCharsets.UTF_8.toString())}/${URLEncoder.encode(destination, StandardCharsets.UTF_8.toString())}/$origLatStr/$origLngStr/$destLatStr/$destLngStr")
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
                    // Store the selected route and clear waypoints for this route
                    selectedRoute = route
                    selectedWaypoints = emptyList() // Clear any previous waypoints
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
                        val encodedDest = URLEncoder.encode(destination, StandardCharsets.UTF_8.toString())
                        
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
                        
                        // Encode waypoints as "lat,lng|lat,lng|..." format
                        val waypointsParam = if (selectedWaypoints.isNotEmpty()) {
                            URLEncoder.encode(
                                selectedWaypoints.joinToString("|") { "${it.latitude},${it.longitude}" },
                                StandardCharsets.UTF_8.toString()
                            )
                        } else ""
                        
                        navController.navigate("navigation?origLat=$origLat&origLng=$origLng&destLat=$destLat&destLng=$destLng&origin=$encodedOrigin&dest=$encodedDest&waypoints=$waypointsParam") {
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
