package org.aurora.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.aurora.navigation.model.NavigationRoute
import org.aurora.navigation.model.RouteType
import org.aurora.navigation.model.TrafficLevel
import org.jetbrains.skia.Image as SkiaImage
import java.net.URL

@Composable
fun RouteSelectionScreen(
    origin: String,
    destination: String,
    routes: List<NavigationRoute>,
    selectedType: RouteType?,
    onRouteSelected: (RouteType) -> Unit,
    onStartNavigation: () -> Unit,
    onBack: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left sidebar with route cards
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.35f)
                .fillMaxHeight(),
            color = Color(0xFF1E293B)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Aurora Rider",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Smart & safe routes for riders",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Change destination link
                TextButton(onClick = onBack) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Change destination",
                        color = Color(0xFF3B82F6),
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Route cards
                routes.forEach { route ->
                    RouteCard(
                        route = route,
                        isSelected = route.type == selectedType,
                        onClick = { onRouteSelected(route.type) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Start Navigation button
                Button(
                    onClick = onStartNavigation,
                    enabled = selectedType != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6),
                        disabledContainerColor = Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Start Navigation",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Smart Route Benefits (if selected)
                if (selectedType == RouteType.SMART) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Smart Route Benefits:",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "• Clear road ahead - optimal conditions\n• Traffic prediction: Light for next 5 minutes\n• Route efficiency: 97%",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
        
        // Right side - Map preview
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
        ) {
            // Show Google Maps satellite view if available
            val selectedRoute = routes.find { it.type == selectedType }
            
            if (selectedRoute?.staticMapUrl != null) {
                // Display actual Google Maps satellite image
                NetworkImage(
                    url = selectedRoute.staticMapUrl,
                    contentDescription = "Satellite Map View",
                    modifier = Modifier.fillMaxSize()
                )
                
                // Overlay with route info
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        "${selectedRoute.name} - Satellite View",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                // Fallback to text if no map available
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Map Preview",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light
                    )
                    Text(
                        "$origin → $destination",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 14.sp
                    )
                }
            }
            
            // Route selector pills
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                routes.forEach { route ->
                    val isSelected = route.type == selectedType
                    val bgColor = when (route.type) {
                        RouteType.SMART -> if (isSelected) Color(0xFF3B82F6) else Color(0xFF3B82F6).copy(alpha = 0.3f)
                        RouteType.REGULAR -> if (isSelected) Color(0xFF64748B) else Color(0xFF64748B).copy(alpha = 0.3f)
                        RouteType.CHILL -> if (isSelected) Color(0xFFA855F7) else Color(0xFFA855F7).copy(alpha = 0.3f)
                    }
                    
                    Card(
                        modifier = Modifier.clickable { onRouteSelected(route.type) },
                        colors = CardDefaults.cardColors(containerColor = bgColor)
                    ) {
                        Text(
                            route.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RouteCard(
    route: NavigationRoute,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (route.type) {
        RouteType.SMART -> if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E3A5F)
        RouteType.CHILL -> if (isSelected) Color(0xFFA855F7) else Color(0xFF4C1D95)
        RouteType.REGULAR -> if (isSelected) Color(0xFF64748B) else Color(0xFF334155)
    }
    
    val borderColor = when (route.type) {
        RouteType.SMART -> Color(0xFF3B82F6)
        RouteType.CHILL -> Color(0xFFA855F7)
        RouteType.REGULAR -> Color(0xFF64748B)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(2.dp, borderColor, RoundedCornerShape(16.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with icon and badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (route.type) {
                            RouteType.SMART -> Icons.Default.AutoAwesome
                            RouteType.CHILL -> Icons.Default.Park
                            RouteType.REGULAR -> Icons.Default.Route
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        route.name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (route.type == RouteType.SMART) {
                    Surface(
                        color = Color(0xFF10B981),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "Recommended",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                route.description,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${route.estimatedTime}",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "min",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        "ETA",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                
                if (route.timeSavedVsBaseline != 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (route.timeSavedVsBaseline > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (route.timeSavedVsBaseline > 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "${if (route.timeSavedVsBaseline > 0) "+" else ""}${route.timeSavedVsBaseline}",
                                color = if (route.timeSavedVsBaseline > 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Min saved",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Details grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RouteDetail("Safety", "${route.safetyScore}%")
                RouteDetail("Traffic", route.trafficLevel.name.replace("_", " "))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RouteDetail("Hazards", "${route.hazardCount}")
                if (route.scenicPoints > 0) {
                    RouteDetail("View points", "${route.scenicPoints}")
                }
            }
        }
    }
}

@Composable
fun RouteDetail(label: String, value: String) {
    Column {
        Text(
            label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp
        )
        Text(
            value,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Custom composable for loading and displaying images from URLs
 * Uses SkiaImage for Compose Desktop compatibility
 */
@Composable
fun NetworkImage(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    
    LaunchedEffect(url) {
        isLoading = true
        hasError = false
        try {
            bitmap = loadImageFromUrl(url)
            isLoading = false
        } catch (e: Exception) {
            println("Failed to load image from $url: ${e.message}")
            hasError = true
            isLoading = false
        }
    }
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    color = Color(0xFF3B82F6),
                    modifier = Modifier.size(48.dp)
                )
            }
            hasError -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = "Error loading map",
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Map unavailable",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
            bitmap != null -> {
                Image(
                    bitmap = bitmap!!,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

/**
 * Loads an image from a URL and converts it to ImageBitmap
 * Uses SkiaImage for Compose Desktop compatibility
 */
suspend fun loadImageFromUrl(url: String): ImageBitmap = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection()
    connection.connectTimeout = 10000
    connection.readTimeout = 10000
    val bytes = connection.getInputStream().readBytes()
    SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
}
