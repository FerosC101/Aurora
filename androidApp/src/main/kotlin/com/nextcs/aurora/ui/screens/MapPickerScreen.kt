package com.nextcs.aurora.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import com.nextcs.aurora.location.LocationService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    title: String = "Select Location",
    initialLocation: LatLng? = null,
    onLocationSelected: (LatLng, String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val locationService = remember { LocationService(context) }
    val scope = rememberCoroutineScope()
    
    // Get current location or use default (Manila)
    val defaultLocation = initialLocation 
        ?: locationService.getLastKnownLocation() 
        ?: LatLng(14.5995, 120.9842)
    
    var selectedLocation by remember { mutableStateOf(defaultLocation) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }
    
    // Update selected location when camera moves
    LaunchedEffect(cameraPositionState.position) {
        selectedLocation = cameraPositionState.position.target
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E88E5),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current Location Button
                FloatingActionButton(
                    onClick = {
                        val currentLoc = locationService.getLastKnownLocation()
                        if (currentLoc != null) {
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(currentLoc, 15f),
                                    durationMs = 1000
                                )
                            }
                        }
                    },
                    containerColor = Color.White,
                    contentColor = Color(0xFF1E88E5)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "My Location")
                }
                
                // Confirm Selection Button
                ExtendedFloatingActionButton(
                    onClick = {
                        val address = locationService.formatLocationToAddress(selectedLocation)
                        Log.d("MapPicker", "Confirming location: $selectedLocation, address: $address")
                        onLocationSelected(selectedLocation, address)
                    },
                    containerColor = Color(0xFF1E88E5),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Confirm")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Location")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Google Map
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = locationService.hasLocationPermission()
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false
                )
            )
            
            // Center Pin Marker (fixed in screen center) - better positioning
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-32).dp), // Offset so pin bottom points to center
                contentAlignment = Alignment.BottomCenter
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location Pin",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFE53935)
                )
            }
            
            // Center crosshair (optional - helps with precision)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(4.dp)
                    .background(Color(0xFF1E88E5), shape = androidx.compose.foundation.shape.CircleShape)
            )
            
            // Location Info Card
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 80.dp), // Space for FAB
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Selected Location",
                        fontSize = 14.sp,
                        color = Color(0xFF757575),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = locationService.formatLocationToAddress(selectedLocation),
                        fontSize = 16.sp,
                        color = Color(0xFF212121),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Move the map to adjust the pin location",
                        fontSize = 12.sp,
                        color = Color(0xFF9E9E9E)
                    )
                }
            }
        }
    }
}
