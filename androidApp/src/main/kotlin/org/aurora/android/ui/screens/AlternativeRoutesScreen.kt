package org.aurora.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.aurora.android.navigation.DirectionsService
import org.aurora.android.navigation.RouteAlternative
import com.google.android.gms.maps.model.LatLng

@Composable
fun AlternativeRoutesScreen(
    origin: String,
    destination: String,
    onRouteSelected: (RouteAlternative) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val directionsService = remember { DirectionsService(context) }
    val scope = rememberCoroutineScope()
    
    var routes by remember { mutableStateOf<List<RouteAlternative>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Parse origin and destination addresses to coordinates
    // For now, using default Manila coordinates
    val originCoords = LatLng(14.5995, 120.9842)
    val destCoords = LatLng(14.6091, 121.0159)
    
    LaunchedEffect(origin, destination) {
        scope.launch {
            try {
                val result = directionsService.getAlternativeRoutes(originCoords, destCoords)
                result.onSuccess { alternativeRoutes ->
                    routes = alternativeRoutes
                    isLoading = false
                }.onFailure { exception ->
                    error = "Failed to fetch routes: ${exception.message}"
                    isLoading = false
                }
            } catch (e: Exception) {
                error = "Error: ${e.message}"
                isLoading = false
            }
        }
    }
    
    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = Color(0xFF1E88E5))
                Text("Calculating routes...", color = Color(0xFF757575))
            }
        }
    } else if (error != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(error!!, color = Color(0xFFD32F2F), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Button(onClick = onBack) {
                    Text("Go Back")
                }
            }
        }
    } else if (routes != null) {
        RouteComparisonScreen(
            routes = routes!!,
            onRouteSelected = {
                onRouteSelected(it)
                onBack()
            },
            onBack = onBack,
            modifier = modifier
        )
    }
}

@Deprecated("Use new RouteComparisonScreen instead")
@Composable
fun RouteOptionCard(
    route: Any, // Changed from RouteOption to Any
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Deprecated - kept for compatibility
}

fun getTrafficColor(level: Any): Color { // Changed from TrafficLevel to Any
    return Color(0xFF1E88E5)
}
