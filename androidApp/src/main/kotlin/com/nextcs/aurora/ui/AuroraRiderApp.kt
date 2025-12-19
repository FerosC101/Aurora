package com.nextcs.aurora.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.nextcs.aurora.maps.GoogleMapsProvider
import com.nextcs.aurora.maps.MapsProvider
import com.nextcs.aurora.navigation.PersonalNavigationEngine
import com.nextcs.aurora.navigation.model.*

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
    
    var showOnboarding by remember { mutableStateOf(false) } // Disabled onboarding
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

// GCash-style components with white and blue theme
@Composable
fun QuickAccessIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(color.copy(alpha = 0.12f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF424242)
        )
    }
}

@Composable
fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(110.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column {
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF757575)
                )
            }
        }
    }
}

@Composable
fun StatBadge(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF757575)
            )
        }
    }
}

// Modern components for winning design
@Composable
fun ModernStatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Column {
                Text(
                    value,
                    color = Color(0xFF1F2937),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    label,
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ActivityMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Column {
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Text(
                label,
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
fun ModernModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(70.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF6366F1) else Color(0xFFF9FAFB)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF6B7280),
                modifier = Modifier.size(24.dp)
            )
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF1F2937)
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
    var animatedProgress by remember { mutableStateOf(0f) }
    var pulseScale by remember { mutableStateOf(1f) }
    
    LaunchedEffect(Unit) {
        animate(0f, 1f, animationSpec = tween(800, easing = EaseOutCubic)) { value, _ ->
            animatedProgress = value
        }
        // Pulsating animation for main button
        while (true) {
            animate(1f, 1.1f, animationSpec = tween(1000, easing = EaseInOut)) { value, _ ->
                pulseScale = value
            }
            animate(1.1f, 1f, animationSpec = tween(1000, easing = EaseInOut)) { value, _ ->
                pulseScale = value
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Blue header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E88E5))
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Hello, Rider!",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Aurora Travel",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .clickable(onClick = onLogout),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Quick Access Icons (GCash style - top center)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickAccessIcon(
                    icon = Icons.Default.Place,
                    label = "Routes",
                    color = Color(0xFF1E88E5),
                    onClick = { }
                )
                QuickAccessIcon(
                    icon = Icons.Default.Info,
                    label = "History",
                    color = Color(0xFF42A5F5),
                    onClick = { }
                )
                QuickAccessIcon(
                    icon = Icons.Default.Star,
                    label = "Favorites",
                    color = Color(0xFF2196F3),
                    onClick = { }
                )
                QuickAccessIcon(
                    icon = Icons.Default.Lock,
                    label = "Safety",
                    color = Color(0xFF1976D2),
                    onClick = { }
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Large Pulsating Travel Button (GCash style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(pulseScale)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF2196F3),
                                    Color(0xFF1976D2)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clickable { /* Optional: scroll to route planning form */ },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = "Optimize Travel",
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Optimize",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Travel",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Route Planning Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF1E88E5).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = Color(0xFF1E88E5),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            "Plan Your Route",
                            color = Color(0xFF1F2937),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    OutlinedTextField(
                        value = origin,
                        onValueChange = onOriginChange,
                        label = { Text("From", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF1E88E5)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1F2937),
                            unfocusedTextColor = Color(0xFF1F2937),
                            focusedBorderColor = Color(0xFF1E88E5),
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            focusedLabelColor = Color(0xFF1E88E5),
                            unfocusedLabelColor = Color(0xFF6B7280),
                            cursorColor = Color(0xFF1E88E5),
                            focusedContainerColor = Color(0xFFF9FAFB),
                            unfocusedContainerColor = Color(0xFFF9FAFB)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    OutlinedTextField(
                        value = destination,
                        onValueChange = onDestinationChange,
                        label = { Text("To", fontSize = 14.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = Color(0xFFEF4444)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1F2937),
                            unfocusedTextColor = Color(0xFF1F2937),
                            focusedBorderColor = Color(0xFF1E88E5),
                            unfocusedBorderColor = Color(0xFFE5E7EB),
                            focusedLabelColor = Color(0xFF1E88E5),
                            unfocusedLabelColor = Color(0xFF6B7280),
                            cursorColor = Color(0xFF1E88E5),
                            focusedContainerColor = Color(0xFFF9FAFB),
                            unfocusedContainerColor = Color(0xFFF9FAFB)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    Text(
                        "Travel Mode",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1F2937)
                    )
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("bicycling", "Bike", Icons.Default.Person),
                            Triple("walking", "Walk", Icons.Default.Person),
                            Triple("driving", "Car", Icons.Default.Star),
                            Triple("transit", "Bus", Icons.Default.Star)
                        ).forEach { (mode, label, icon) ->
                            ModernModeChip(
                                label = label,
                                icon = icon,
                                isSelected = vehicleMode == mode,
                                onClick = { onVehicleModeChange(mode) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    Button(
                        onClick = onFindRoutes,
                        enabled = !isLoadingRoutes && origin.isNotBlank() && destination.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E88E5),
                            disabledContainerColor = Color(0xFFE5E7EB),
                            disabledContentColor = Color(0xFF9CA3AF)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        if (isLoadingRoutes) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Find Routes",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Feature Cards at Bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureCard(
                    icon = Icons.Default.Person,
                    title = "Smart Routes",
                    subtitle = "AI-powered route optimization",
                    color = Color(0xFF1E88E5),
                    onClick = { }
                )
                FeatureCard(
                    icon = Icons.Default.Star,
                    title = "Quick Planning",
                    subtitle = "Save time with instant routes",
                    color = Color(0xFF2196F3),
                    onClick = { }
                )
                FeatureCard(
                    icon = Icons.Default.Lock,
                    title = "Safe Travel",
                    subtitle = "Priority on safety and comfort",
                    color = Color(0xFF42A5F5),
                    onClick = { }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats Row at Bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBadge(
                    value = "248",
                    label = "Trips",
                    color = Color(0xFF1E88E5)
                )
                StatBadge(
                    value = "95%",
                    label = "Safety",
                    color = Color(0xFF2196F3)
                )
                StatBadge(
                    value = "18h",
                    label = "Saved",
                    color = Color(0xFF42A5F5)
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Animated background gradient
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = iconColor.copy(alpha = 0.1f * progress),
                    radius = 120f,
                    center = Offset(size.width - 40f, 40f)
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        title,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column {
                    Text(
                        value,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        subtitle,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MinimalStatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            
            Column {
                Text(
                    value,
                    color = Color(0xFF212121),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    label,
                    color = Color(0xFF757575),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun CleanModeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(48.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                Color(0xFF1E88E5) 
            else 
                Color.White
        ),
        shape = RoundedCornerShape(8.dp),
        onClick = onClick,
        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE0E0E0)) else null,
        elevation = if (isSelected) CardDefaults.cardElevation(defaultElevation = 2.dp) 
                    else CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = if (isSelected) Color.White else Color(0xFF757575),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
fun VehicleModeCard(
    icon: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                Color(0xFF3B82F6) 
            else 
                Color(0xFF334155).copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                icon,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
