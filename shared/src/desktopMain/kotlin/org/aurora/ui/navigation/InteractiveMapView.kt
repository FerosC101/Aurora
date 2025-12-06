package org.aurora.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.aurora.navigation.NavigationState
import org.aurora.navigation.model.DetectedHazard
import org.aurora.navigation.model.HazardType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.net.URL
import javax.imageio.ImageIO
import java.awt.Desktop
import java.net.URI
import org.jetbrains.skia.Image as SkiaImage

@Composable
fun InteractiveMapView(
    navState: NavigationState,
    modifier: Modifier = Modifier
) {
    val route = navState.selectedRoute
    var mapImagePainter by remember { mutableStateOf<BitmapPainter?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showHazards by remember { mutableStateOf(true) }
    var mapType by remember { mutableStateOf("hybrid") }
    var zoomLevel by remember { mutableStateOf(route?.zoomLevel ?: 17) }
    
    // Load map image when route is available
    if (route != null && mapImagePainter == null && !isLoading) {
        isLoading = true
        Thread {
            try {
                val mapUrl = generateStaticMapUrl(
                    centerLat = route.centerLat,
                    centerLng = route.centerLng,
                    zoomLevel = zoomLevel,
                    waypoints = route.realLatLngWaypoints,
                    hazards = if (showHazards) route.detectedHazards ?: emptyList() else emptyList(),
                    mapType = mapType,
                    currentPosition = navState.currentPosition?.let {
                        it.y / 100.0 to it.x / 100.0
                    }
                )
                
                val bufferedImage = ImageIO.read(URL(mapUrl))
                val bytes = java.io.ByteArrayOutputStream().use { baos ->
                    ImageIO.write(bufferedImage, "PNG", baos)
                    baos.toByteArray()
                }
                val skiaImage = SkiaImage.makeFromEncoded(bytes)
                mapImagePainter = BitmapPainter(skiaImage.toComposeImageBitmap())
                isLoading = false
            } catch (e: Exception) {
                println("❌ Error loading map: ${e.message}")
                e.printStackTrace()
                isLoading = false
            }
        }.start()
    }
    
    // Function to reload map
    fun reloadMap() {
        if (route != null && !isLoading) {
            mapImagePainter = null  // Trigger reload
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        if (route != null) {
            // Display map image
            if (mapImagePainter != null) {
                Image(
                    painter = mapImagePainter!!,
                    contentDescription = "Navigation Map",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF3B82F6))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading map...", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            
            // "Open in Google Maps" button
            Button(
                onClick = {
                    try {
                        val googleMapsUrl = "https://www.google.com/maps/dir/?api=1" +
                            "&origin=${route.realLatLngWaypoints.firstOrNull()?.let { "${it.first},${it.second}" } ?: ""}" +
                            "&destination=${route.realLatLngWaypoints.lastOrNull()?.let { "${it.first},${it.second}" } ?: ""}" +
                            "&travelmode=driving"
                        
                        Desktop.getDesktop().browse(URI(googleMapsUrl))
                    } catch (e: Exception) {
                        println("❌ Error opening browser: ${e.message}")
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3B82F6)
                )
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open in Google Maps", fontSize = 14.sp)
            }
            
            // Map controls overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Map type selector
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Map, null, modifier = Modifier.size(20.dp))
                            Text("Map Type", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            MapTypeButton("Satellite", mapType == "satellite") { 
                                mapType = "satellite"
                                reloadMap()
                            }
                            MapTypeButton("Hybrid", mapType == "hybrid") { 
                                mapType = "hybrid"
                                reloadMap()
                            }
                            MapTypeButton("Map", mapType == "roadmap") { 
                                mapType = "roadmap"
                                reloadMap()
                            }
                        }
                    }
                }
                
                // Hazard toggle
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (showHazards) Color(0xFFEF4444) else Color.White
                    ),
                    modifier = Modifier.clickable { 
                        showHazards = !showHazards
                        reloadMap()
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Toggle Hazards",
                            tint = if (showHazards) Color.White else Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "${route.detectedHazards?.size ?: 0} Hazards",
                            color = if (showHazards) Color.White else Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Zoom controls
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        IconButton(onClick = { 
                            zoomLevel = (zoomLevel + 1).coerceAtMost(20)
                            reloadMap()
                        }) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                        }
                        HorizontalDivider()
                        IconButton(onClick = { 
                            zoomLevel = (zoomLevel - 1).coerceAtLeast(10)
                            reloadMap()
                        }) {
                            Icon(Icons.Default.Remove, null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            
            // AI Status Banner
            if (route.detectedHazards?.isNotEmpty() == true) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                "🤖 Aurora AI Active",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Detected ${route.detectedHazards.size} hazards • Optimizing route",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else {
            // No route selected
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Select a route to begin navigation",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun MapTypeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF3B82F6) else Color.LightGray
        ),
        modifier = Modifier.height(28.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 10.sp, color = if (selected) Color.White else Color.Black)
    }
}

fun generateStaticMapUrl(
    centerLat: Double,
    centerLng: Double,
    zoomLevel: Int,
    waypoints: List<Pair<Double, Double>>,
    hazards: List<DetectedHazard>,
    mapType: String,
    currentPosition: Pair<Double, Double>?
): String {
    val apiKey = "AIzaSyClM3oua_QM_fSy_9WgnhQK6jkoN50lGTc"
    val baseUrl = "https://maps.googleapis.com/maps/api/staticmap"
    
    val params = mutableListOf(
        "center=$centerLat,$centerLng",
        "zoom=$zoomLevel",
        "size=1200x800",
        "scale=2",
        "maptype=$mapType"
    )
    
    // Add route path
    if (waypoints.isNotEmpty()) {
        val pathPoints = waypoints.joinToString("|") { "${it.first},${it.second}" }
        params.add("path=color:0x3B82F6FF|weight:5|$pathPoints")
        
        // Start marker
        waypoints.firstOrNull()?.let {
            params.add("markers=color:green|label:S|${it.first},${it.second}")
        }
        
        // End marker
        waypoints.lastOrNull()?.let {
            params.add("markers=color:red|label:E|${it.first},${it.second}")
        }
    }
    
    // Add current position marker
    currentPosition?.let {
        params.add("markers=color:blue|label:●|${it.first},${it.second}")
    }
    
    // Add hazard markers
    hazards.forEach { hazard ->
        val label = when (hazard.type) {
            HazardType.CONSTRUCTION -> "C"
            HazardType.POTHOLE -> "P"
            HazardType.FLOOD -> "F"
            HazardType.ACCIDENT -> "A"
        }
        val color = when (hazard.severity.name) {
            "LOW" -> "yellow"
            "MODERATE" -> "orange"
            "HIGH", "CRITICAL" -> "red"
            else -> "yellow"
        }
        params.add("markers=color:$color|label:$label|${hazard.lat},${hazard.lng}")
    }
    
    params.add("key=$apiKey")
    
    return "$baseUrl?${params.joinToString("&")}"
}
