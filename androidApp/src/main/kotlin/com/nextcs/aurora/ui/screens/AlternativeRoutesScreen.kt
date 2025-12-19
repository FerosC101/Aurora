package com.nextcs.aurora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.nextcs.aurora.ai.GeminiAIService
import com.nextcs.aurora.navigation.DirectionsService
import com.nextcs.aurora.navigation.RouteAlternative
import com.google.android.gms.maps.model.LatLng
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AlternativeRoutesScreen(
    origin: String,
    destination: String,
    originLocation: LatLng? = null,
    destinationLocation: LatLng? = null,
    onRouteSelected: (RouteAlternative) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val directionsService = remember { DirectionsService(context) }
    val geminiAIService = remember { GeminiAIService(context) }
    val scope = rememberCoroutineScope()
    
    var routes by remember { mutableStateOf<List<RouteAlternative>?>(null) }
    var aiAnalysis by remember { mutableStateOf<GeminiAIService.RouteAnalysis?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isAnalyzing by remember { mutableStateOf(false) }
    
    // Use provided coordinates or default fallback
    val originCoords = originLocation ?: LatLng(14.5995, 120.9842)
    val destCoords = destinationLocation ?: LatLng(14.6091, 121.0159)
    
    LaunchedEffect(origin, destination) {
        scope.launch {
            try {
                val result = directionsService.getAlternativeRoutes(originCoords, destCoords)
                result.onSuccess { alternativeRoutes ->
                    routes = alternativeRoutes
                    isLoading = false
                    
                    // Analyze routes with Gemini AI
                    isAnalyzing = true
                    val currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                    try {
                        val analysis = geminiAIService.analyzeRoutes(
                            routes = alternativeRoutes,
                            currentTime = currentTime,
                            trafficConditions = "normal"
                        )
                        aiAnalysis = analysis
                    } catch (e: Exception) {
                        // Continue without AI analysis if it fails
                    }
                    isAnalyzing = false
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
                Text(
                    error!!,
                    color = Color(0xFFD32F2F),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(onClick = onBack) {
                    Text("Go Back")
                }
            }
        }
    } else if (routes != null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
        ) {
            // Header
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Choose Route",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    Spacer(modifier = Modifier.width(40.dp))
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // AI Recommendation Card
                if (aiAnalysis != null && !isAnalyzing) {
                    AiRecommendationCard(analysis = aiAnalysis!!)
                } else if (isAnalyzing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(Color(0xFFF3E5F5), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF7C3AED)
                            )
                            Text(
                                "AI analyzing routes...",
                                color = Color(0xFF7C3AED),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Route Cards directly here (not using RouteComparisonScreen)
                routes!!.forEach { route ->
                    RouteCard(
                        route = route,
                        isRecommended = route.name == aiAnalysis?.recommendedRoute,
                        onSelect = { onRouteSelected(route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AiRecommendationCard(analysis: GeminiAIService.RouteAnalysis) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color(0xFFF3E5F5),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "AI",
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI Recommendation",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C3AED)
                    )
                }
                Surface(
                    color = Color(0xFF7C3AED),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = analysis.recommendedRoute,
                        modifier = Modifier.padding(6.dp, 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Text(
                text = analysis.reasoning,
                fontSize = 13.sp,
                color = Color(0xFF5E35B1),
                lineHeight = 18.sp
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScoreIndicator("Safety", analysis.safetyScore)
                ScoreIndicator("Efficiency", analysis.efficiencyScore)
                ScoreIndicator("Comfort", analysis.comfortScore)
            }
            
            if (analysis.warning != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = analysis.warning,
                            fontSize = 12.sp,
                            color = Color(0xFFC62828)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreIndicator(label: String, score: Int) {
    Column(
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = when {
                        score >= 85 -> Color(0xFFC8E6C9)
                        score >= 70 -> Color(0xFFFFF9C4)
                        score >= 50 -> Color(0xFFFFCC80)
                        else -> Color(0xFFFFCDD2)
                    },
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$score%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF424242)
        )
    }
}

@Composable
private fun RouteCard(
    route: RouteAlternative,
    isRecommended: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onSelect)
            .then(
                if (isRecommended) {
                    Modifier.border(3.dp, Color(0xFF7C3AED), RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            ),
        color = if (isRecommended) Color(0xFFF3E5F5) else Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Route Name and Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = route.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                
                if (isRecommended) {
                    Surface(
                        color = Color(0xFF7C3AED),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "AI Recommended",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    // Route Type Badge
                    val badgeText = when (route.name) {
                        "Smart Route" -> "FASTEST"
                        "Chill Route" -> "SCENIC"
                        else -> "STANDARD"
                    }
                    Surface(
                        color = when (badgeText) {
                            "FASTEST" -> Color(0xFF1976D2)
                            "SCENIC" -> Color(0xFF388E3C)
                            else -> Color(0xFF757575)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            // Characteristics
            Text(
                text = route.characteristics,
                fontSize = 14.sp,
                color = Color(0xFF757575)
            )
            
            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RouteStatItem(
                    icon = Icons.Default.Info,
                    label = "ETA",
                    value = "${route.routeInfo.duration / 60} min"
                )
                RouteStatItem(
                    icon = Icons.Default.Place,
                    label = "Distance",
                    value = String.format("%.1f km", route.routeInfo.distance / 1000.0)
                )
                RouteStatItem(
                    icon = Icons.Default.Star,
                    label = "Safety",
                    value = "${route.safetyScore}%"
                )
                RouteStatItem(
                    icon = Icons.Default.Warning,
                    label = "Hazards",
                    value = "${route.hazards.size}"
                )
            }
            
            // Continue Button
            Button(
                onClick = onSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecommended) Color(0xFF7C3AED) else Color(0xFF1E88E5)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Continue with ${route.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RouteStatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFF1E88E5),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF9E9E9E)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121)
        )
    }
}
