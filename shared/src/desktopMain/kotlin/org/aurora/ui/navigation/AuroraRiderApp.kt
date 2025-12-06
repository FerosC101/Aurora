package org.aurora.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
    var animatedProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        animate(0f, 1f, animationSpec = tween(800, easing = EaseOutCubic)) { value, _ ->
            animatedProgress = value
        }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Animated gradient background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val time = System.currentTimeMillis() / 2000f
            
            // Flowing gradient circles
            for (i in 0..3) {
                val offset = i * 90f
                val radius = 400f + kotlin.math.sin(time + offset) * 60f
                val alpha = 0.04f + kotlin.math.sin(time + offset) * 0.02f
                
                drawCircle(
                    color = if (i % 2 == 0) Color(0xFF3B82F6) else Color(0xFF8B5CF6),
                    radius = radius,
                    center = Offset(
                        size.width * (0.2f + i * 0.25f),
                        size.height * (0.3f + kotlin.math.cos(time + offset) * 0.15f)
                    ),
                    alpha = alpha
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Modern header with glassmorphism
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Column {
                            Text(
                                "Aurora Rider",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "AI-Powered Smart Navigation",
                                color = Color(0xFF3B82F6),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
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
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Dashboard statistics
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                DashboardStatCard(
                    title = "Total Trips",
                    value = "248",
                    subtitle = "+12 this week",
                    icon = Icons.Default.Route,
                    iconColor = Color(0xFF3B82F6),
                    progress = animatedProgress,
                    modifier = Modifier.weight(1f)
                )
                DashboardStatCard(
                    title = "Hazards Avoided",
                    value = "1,432",
                    subtitle = "97% safety rate",
                    icon = Icons.Default.Shield,
                    iconColor = Color(0xFF10B981),
                    progress = animatedProgress,
                    modifier = Modifier.weight(1f)
                )
                DashboardStatCard(
                    title = "Time Saved",
                    value = "18.5h",
                    subtitle = "This month",
                    icon = Icons.Default.Speed,
                    iconColor = Color(0xFF8B5CF6),
                    progress = animatedProgress,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        
            // Modern route planning card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(40.dp)
                        )
                        Column {
                            Text(
                                "Plan Your Journey",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Find the smartest route for your ride",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    // Origin input with modern styling
                    OutlinedTextField(
                        value = origin,
                        onValueChange = onOriginChange,
                        label = { Text("Starting Point", color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF10B981))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF334155),
                            cursorColor = Color(0xFF3B82F6)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Destination input
                    OutlinedTextField(
                        value = destination,
                        onValueChange = onDestinationChange,
                        label = { Text("Destination", color = Color.White.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFEF4444))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF3B82F6),
                            unfocusedBorderColor = Color(0xFF334155),
                            cursorColor = Color(0xFF3B82F6)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Vehicle mode selector with modern cards
                    Text(
                        "Transportation Mode",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        VehicleModeCard(
                            icon = "🚴",
                            label = "Bike",
                            isSelected = vehicleMode == "bicycling",
                            onClick = { onVehicleModeChange("bicycling") },
                            modifier = Modifier.weight(1f)
                        )
                        VehicleModeCard(
                            icon = "🚶",
                            label = "Walk",
                            isSelected = vehicleMode == "walking",
                            onClick = { onVehicleModeChange("walking") },
                            modifier = Modifier.weight(1f)
                        )
                        VehicleModeCard(
                            icon = "🚗",
                            label = "Drive",
                            isSelected = vehicleMode == "driving",
                            onClick = { onVehicleModeChange("driving") },
                            modifier = Modifier.weight(1f)
                        )
                        VehicleModeCard(
                            icon = "🚌",
                            label = "Transit",
                            isSelected = vehicleMode == "transit",
                            onClick = { onVehicleModeChange("transit") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Demo mode toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF334155).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Science,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    if (useDemoMode) "🎮 Demo Mode" else "🗺️ Real Maps",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    if (useDemoMode) "Simulated routes" else "Google Maps API",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = useDemoMode,
                            onCheckedChange = onDemoModeChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF3B82F6),
                                uncheckedThumbColor = Color(0xFF64748B),
                                uncheckedTrackColor = Color(0xFF334155)
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Find routes button
                    Button(
                        onClick = onFindRoutes,
                        enabled = !isLoadingRoutes && origin.isNotBlank() && destination.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6),
                            disabledContainerColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoadingRoutes) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Finding Best Routes...", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        } else {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Find Smart Routes", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var currentSlide by remember { mutableStateOf(0) }
    val slideProgress by animateFloatAsState(
        targetValue = currentSlide.toFloat(),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
    )
    
    val slides = remember {
        listOf(
            OnboardingSlide(
                title = "Welcome to Aurora Rider",
                subtitle = "AI-Powered Smart Navigation",
                description = "Navigate smarter with real-time hazard detection, intelligent route optimization, and adaptive traffic predictions.",
                icon = Icons.Default.ElectricBolt,
                color = Color(0xFF3B82F6)
            ),
            OnboardingSlide(
                title = "Aurora SHIELD",
                subtitle = "Advanced Hazard Detection",
                description = "Our AI identifies construction zones, potholes, floods, and accidents in real-time to keep you safe on every ride.",
                icon = Icons.Default.Shield,
                color = Color(0xFF10B981)
            ),
            OnboardingSlide(
                title = "Smart Route Selection",
                subtitle = "3 Intelligent Options",
                description = "Choose between Smart (fastest), Chill (scenic), or Regular routes. Each optimized for your riding style and safety.",
                icon = Icons.Default.Route,
                color = Color(0xFF8B5CF6)
            ),
            OnboardingSlide(
                title = "Real-Time Insights",
                subtitle = "Live Traffic & Stoplights",
                description = "Get stoplight countdown timers, live traffic updates, and time-saving suggestions throughout your journey.",
                icon = Icons.Default.Speed,
                color = Color(0xFFF59E0B)
            )
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Animated background particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val time = System.currentTimeMillis() / 3000f
            for (i in 0..15) {
                val offset = i * 40f
                val x = size.width * (0.1f + (i % 5) * 0.2f)
                val y = size.height * ((kotlin.math.sin(time + offset) + 1f) / 2f)
                val alpha = (kotlin.math.sin(time + offset) + 1f) / 4f
                
                drawCircle(
                    color = slides[currentSlide].color.copy(alpha = alpha * 0.15f),
                    radius = 60f + kotlin.math.sin(time + offset) * 20f,
                    center = Offset(x, y)
                )
            }
        }
        
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .width(700.dp)
                .height(500.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated slide content
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Animated icon with pulse
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                        colors = listOf(
                                            slides[currentSlide].color.copy(alpha = 0.3f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                slides[currentSlide].icon,
                                contentDescription = null,
                                tint = slides[currentSlide].color,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            slides[currentSlide].title,
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            slides[currentSlide].subtitle,
                            color = slides[currentSlide].color,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            slides[currentSlide].description,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                            lineHeight = 24.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Progress indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    slides.forEachIndexed { index, _ ->
                        val indicatorColor by animateColorAsState(
                            targetValue = if (index == currentSlide) 
                                slides[currentSlide].color 
                            else 
                                Color.White.copy(alpha = 0.3f),
                            animationSpec = tween(300)
                        )
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(if (index == currentSlide) 40.dp else 24.dp)
                                .background(indicatorColor, RoundedCornerShape(2.dp))
                        )
                    }
                }
                
                // Navigation buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (currentSlide > 0) {
                        TextButton(
                            onClick = { currentSlide-- },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.White.copy(alpha = 0.7f)
                            )
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Back", fontSize = 16.sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    
                    Button(
                        onClick = {
                            if (currentSlide < slides.size - 1) {
                                currentSlide++
                            } else {
                                onComplete()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = slides[currentSlide].color
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            if (currentSlide < slides.size - 1) "Next" else "Get Started",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            if (currentSlide < slides.size - 1) Icons.Default.ArrowForward else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

data class OnboardingSlide(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

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

