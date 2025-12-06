package org.aurora.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.aurora.maps.GoogleMapsProvider
import org.aurora.maps.MapsProvider
import org.aurora.navigation.PersonalNavigationEngine
import org.aurora.navigation.model.*

/**
 * Aurora Rider - Personal Navigation App
 * Matching Figma design specifications
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuroraRiderApp(
    userId: Int,
    mapsProvider: MapsProvider,
    onLogout: () -> Unit
) {
    val navigationEngine = remember { PersonalNavigationEngine() }
    val navState by navigationEngine.navigationState.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showOnboarding by remember { mutableStateOf(true) }
    var origin by remember { mutableStateOf("Manila") }
    var destination by remember { mutableStateOf("Makati") }
    var availableRoutes by remember { mutableStateOf<List<NavigationRoute>>(emptyList()) }
    var selectedRouteType by remember { mutableStateOf<RouteType?>(null) }
    var showTripComplete by remember { mutableStateOf(false) }
    var isLoadingRoutes by remember { mutableStateOf(false) }
    var useDemoMode by remember { mutableStateOf(false) }
    var vehicleMode by remember { mutableStateOf("bicycling") } // bicycling, walking, driving, transit
    
    // Update loop
    LaunchedEffect(Unit) {
        while (true) {
            navigationEngine.update(0.016f) // ~60 FPS
            delay(16)
        }
    }
    
    // Check for trip completion
    LaunchedEffect(navState.progress) {
        if (!navState.isNavigating && navState.progress >= 1f && navState.selectedRoute != null) {
            showTripComplete = true
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        when {
            showOnboarding -> {
                OnboardingScreen(
                    onComplete = { showOnboarding = false }
                )
            }
            
            navState.isNavigating -> {
                LiveNavigationScreen(
                    navState = navState,
                    onEndTrip = {
                        navigationEngine.endNavigation()
                        selectedRouteType = null
                        availableRoutes = emptyList()
                    }
                )
            }
            
            availableRoutes.isNotEmpty() -> {
                RouteSelectionScreen(
                    origin = origin,
                    destination = destination,
                    routes = availableRoutes,
                    selectedType = selectedRouteType,
                    onRouteSelected = { selectedRouteType = it },
                    onStartNavigation = {
                        val route = availableRoutes.find { it.type == selectedRouteType }
                        route?.let { navigationEngine.startNavigation(it) }
                    },
                    onBack = {
                        availableRoutes = emptyList()
                        selectedRouteType = null
                    }
                )
            }
            
            else -> {
                HomeScreen(
                    origin = origin,
                    destination = destination,
                    onOriginChange = { origin = it },
                    onDestinationChange = { destination = it },
                    onFindRoutes = {
                        isLoadingRoutes = true
                        scope.launch {
                            try {
                                availableRoutes = if (useDemoMode) {
                                    // Demo mode: use simulation
                                    println("🎮 Demo Mode: Using simulated routes")
                                    navigationEngine.generateRoutes(origin, destination)
                                } else {
                                    // Real mode: try Google Maps API first
                                    println("🗺️ Real Mode: Fetching routes from Google Maps (mode=$vehicleMode)...")
                                    val realRoutes = if (mapsProvider is GoogleMapsProvider) {
                                        (mapsProvider as GoogleMapsProvider).generateRoutesWithMode(origin, destination, vehicleMode)
                                    } else {
                                        mapsProvider.generateRoutes(origin, destination)
                                    }
                                    
                                    if (realRoutes.isNotEmpty()) {
                                        println("✅ Using real routes from Google Maps (${realRoutes.size} routes)")
                                        realRoutes
                                    } else {
                                        // Fallback to simulation
                                        println("⚠️ Google Maps returned no routes, falling back to simulation")
                                        navigationEngine.generateRoutes(origin, destination)
                                    }
                                }
                                
                                selectedRouteType = RouteType.SMART
                            } catch (e: Exception) {
                                println("❌ Error loading routes: ${e.message}")
                                e.printStackTrace()
                                availableRoutes = navigationEngine.generateRoutes(origin, destination)
                                selectedRouteType = RouteType.SMART
                            } finally {
                                isLoadingRoutes = false
                            }
                        }
                    },
                    useDemoMode = useDemoMode,
                    onDemoModeChange = { useDemoMode = it },
                    isLoadingRoutes = isLoadingRoutes,
                    vehicleMode = vehicleMode,
                    onVehicleModeChange = { vehicleMode = it },
                    onLogout = onLogout
                )
            }
        }
        
        // Trip Complete Modal
        if (showTripComplete) {
            TripCompleteModal(
                route = navState.selectedRoute!!,
                hazardsAvoided = navState.hazardsAvoided,
                timeSaved = navState.timeSaved,
                onDismiss = {
                    showTripComplete = false
                    navigationEngine.endNavigation()
                    selectedRouteType = null
                    availableRoutes = emptyList()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    origin: String,
    destination: String,
    onOriginChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onFindRoutes: () -> Unit,
    useDemoMode: Boolean,
    onDemoModeChange: (Boolean) -> Unit,
    isLoadingRoutes: Boolean,
    vehicleMode: String,
    onVehicleModeChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo and title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Aurora Rider",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Smart & safe routes for riders",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                }
            }
            
            IconButton(
                onClick = onLogout,
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFEF4444), CircleShape)
            ) {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Plan Your Route section
        Card(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(0.6f),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Plan Your Route",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    "Enter your destination to see intelligent route options\noptimized for riders",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Origin input
                OutlinedTextField(
                    value = origin,
                    onValueChange = onOriginChange,
                    label = { Text("From", color = Color.White.copy(alpha = 0.6f)) },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF10B981))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Vehicle Mode Selector
                var expandedVehicle by remember { mutableStateOf(false) }
                val vehicleOptions = mapOf(
                    "bicycling" to "🚴 Bicycling",
                    "walking" to "🚶 Walking",
                    "driving" to "🚗 Driving",
                    "transit" to "🚌 Transit"
                )
                
                ExposedDropdownMenuBox(
                    expanded = expandedVehicle,
                    onExpandedChange = { expandedVehicle = !expandedVehicle }
                ) {
                    OutlinedTextField(
                        value = vehicleOptions[vehicleMode] ?: "🚴 Bicycling",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Vehicle Mode", color = Color.White.copy(alpha = 0.6f)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicle)
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedVehicle,
                        onDismissRequest = { expandedVehicle = false }
                    ) {
                        vehicleOptions.forEach { (mode, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = Color.White) },
                                onClick = {
                                    onVehicleModeChange(mode)
                                    expandedVehicle = false
                                },
                                modifier = Modifier.background(Color(0xFF1E293B))
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Destination input
                OutlinedTextField(
                    value = destination,
                    onValueChange = onDestinationChange,
                    label = { Text("To", color = Color.White.copy(alpha = 0.6f)) },
                    leadingIcon = {
                        Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFFEF4444))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Demo Mode Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (useDemoMode) "🎮 Demo Mode" else "🗺️ Real Maps",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (useDemoMode) "Using simulated routes" else "Using Google Maps API",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp
                            )
                        }
                        
                        Switch(
                            checked = useDemoMode,
                            onCheckedChange = onDemoModeChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFEAB308),
                                checkedTrackColor = Color(0xFFEAB308).copy(alpha = 0.5f),
                                uncheckedThumbColor = Color(0xFF3B82F6),
                                uncheckedTrackColor = Color(0xFF3B82F6).copy(alpha = 0.5f)
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Find Routes button
                Button(
                    onClick = onFindRoutes,
                    enabled = !isLoadingRoutes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoadingRoutes) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Loading routes...",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            "Find Routes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Features list
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureItem("AI-powered route optimization", Color(0xFF3B82F6))
                    FeatureItem("Real-time hazard detection", Color(0xFF10B981))
                    FeatureItem("Stoplight timing predictions", Color(0xFFF59E0B))
                }
            }
        }
    }
}

@Composable
fun FeatureItem(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
    }
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(600.dp)
                .height(400.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(80.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Welcome to Aurora Rider",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "AI-powered navigation designed exclusively for riders.\nExperience Smart routes, hazard avoidance, and real-time traffic predictions.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .width(200.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Get Started",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
