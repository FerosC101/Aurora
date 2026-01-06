package com.nextcs.aurora.ui

import android.util.Log
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
        modifier = modifier
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
                onStartNavigation = { orig, dest ->
                    // Clear selected route before starting new navigation
                    selectedRoute = null
                    selectedWaypoints = emptyList()
                    
                    // Encode text addresses
                    val encodedOrigin = URLEncoder.encode(orig, StandardCharsets.UTF_8.toString())
                    val encodedDestination = URLEncoder.encode(dest, StandardCharsets.UTF_8.toString())
                    
                    // Encode coordinates if available
                    val origLat = selectedOriginLocation?.latitude?.toString() ?: ""
                    val origLng = selectedOriginLocation?.longitude?.toString() ?: ""
                    val destLat = selectedDestinationLocation?.latitude?.toString() ?: ""
                    val destLng = selectedDestinationLocation?.longitude?.toString() ?: ""
                    
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
        
        composable(BottomNavItem.Assistant.route) {
            AIAssistantScreen(
                onNavigateToRoute = { origin, destination, waypoints ->
                    scope.launch {
                        try {
                            Log.d("MainNavApp", "AI Navigation request - Origin: $origin, Destination: $destination, Waypoints: $waypoints")
                            
                            // Resolve origin location
                            val originResult = if (origin.equals("current location", ignoreCase = true) || 
                                                    origin.equals("current", ignoreCase = true) ||
                                                    origin.equals("my location", ignoreCase = true)) {
                                // Get current location
                                val currentLoc = locationService.getLastKnownLocation()
                                if (currentLoc != null) {
                                    // Reverse geocode to get address name
                                    val addressName = placesService.reverseGeocode(currentLoc) ?: "Current Location"
                                    Pair(currentLoc, addressName)
                                } else null
                            } else {
                                // Geocode origin address
                                placesService.searchAndGetCoordinates(origin)
                            }
                            
                            // Geocode destination address
                            val destinationResult = placesService.searchAndGetCoordinates(destination)
                            
                            if (originResult == null || destinationResult == null) {
                                Log.e("MainNavApp", "Failed to geocode locations - Origin: $originResult, Dest: $destinationResult")
                                return@launch
                            }
                            
                            val (originLocation, originAddress) = originResult
                            val (destinationLocation, destinationAddress) = destinationResult
                            
                            // Handle multi-stop routes with waypoints
                            if (waypoints.isNotEmpty()) {
                                Log.d("MainNavApp", "Multi-stop route with ${waypoints.size} waypoints")
                                
                                // Geocode all waypoints
                                val geocodedWaypoints = mutableListOf<LatLng>()
                                for (waypointName in waypoints) {
                                    val waypointResult = placesService.searchAndGetCoordinates(waypointName)
                                    if (waypointResult != null) {
                                        geocodedWaypoints.add(waypointResult.first)
                                        Log.d("MainNavApp", "Geocoded waypoint: $waypointName -> ${waypointResult.first}")
                                    } else {
                                        Log.w("MainNavApp", "Failed to geocode waypoint: $waypointName")
                                    }
                                }
                                
                                // Navigate with all coordinates and waypoints
                                val encodedOrigin = URLEncoder.encode(originAddress, StandardCharsets.UTF_8.toString())
                                val encodedDestination = URLEncoder.encode(destinationAddress, StandardCharsets.UTF_8.toString())
                                val origLat = originLocation.latitude.toString()
                                val origLng = originLocation.longitude.toString()
                                val destLat = destinationLocation.latitude.toString()
                                val destLng = destinationLocation.longitude.toString()
                                
                                // Build waypoints string: lat1,lng1|lat2,lng2|...
                                val waypointsString = geocodedWaypoints.joinToString("|") { "${it.latitude},${it.longitude}" }
                                
                                navController.navigate(
                                    "navigation/$encodedOrigin/$encodedDestination/$origLat/$origLng/$destLat/$destLng/$waypointsString"
                                )
                                return@launch
                            }
                            
                            Log.d("MainNavApp", "Single route without waypoints")
                            
                            // Navigate with coordinates for single route
                            val encodedOrigin = URLEncoder.encode(originAddress, StandardCharsets.UTF_8.toString())
                            val encodedDestination = URLEncoder.encode(destinationAddress, StandardCharsets.UTF_8.toString())
                            val origLat = originLocation.latitude.toString()
                            val origLng = originLocation.longitude.toString()
                            val destLat = destinationLocation.latitude.toString()
                            val destLng = destinationLocation.longitude.toString()
                            
                            navController.navigate(
                                "navigation/$encodedOrigin/$encodedDestination/$origLat/$origLng/$destLat/$destLng"
                            )
                        } catch (e: Exception) {
                            Log.e("MainNavApp", "Error resolving AI navigation", e)
                        }
                    }
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
        
        // Navigation route with optional waypoints (for multi-stop routes)
        composable("navigation/{origin}/{destination}/{origLat}/{origLng}/{destLat}/{destLng}/{waypoints?}") { backStackEntry ->
            val encodedOrigin = backStackEntry.arguments?.getString("origin") ?: ""
            val encodedDestination = backStackEntry.arguments?.getString("destination") ?: ""
            val origin = URLDecoder.decode(encodedOrigin, StandardCharsets.UTF_8.toString())
            val destination = URLDecoder.decode(encodedDestination, StandardCharsets.UTF_8.toString())
            
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
