package org.aurora.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.aurora.navigation.PersonalNavigationEngine
import org.aurora.navigation.model.*

/**
 * Aurora Rider - Personal Navigation App
 * Matching Figma design specifications
 */
@Composable
fun AuroraRiderApp(onLogout: () -> Unit) {
    val navigationEngine = remember { PersonalNavigationEngine() }
    val navState by navigationEngine.navigationState.collectAsState()
    
    var showOnboarding by remember { mutableStateOf(true) }
    var origin by remember { mutableStateOf("Manila") }
    var destination by remember { mutableStateOf("Sunset Hills") }
    var availableRoutes by remember { mutableStateOf<List<NavigationRoute>>(emptyList()) }
    var selectedRouteType by remember { mutableStateOf<RouteType?>(null) }
    var showTripComplete by remember { mutableStateOf(false) }
    
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
                        availableRoutes = navigationEngine.generateRoutes(origin, destination)
                        selectedRouteType = RouteType.SMART // Auto-select Smart route
                    },
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

@Composable
fun HomeScreen(
    origin: String,
    destination: String,
    onOriginChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onFindRoutes: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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
        
        Spacer(modifier = Modifier.height(48.dp))
        
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
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
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
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
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
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Find Routes button
                Button(
                    onClick = onFindRoutes,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Find Routes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
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
