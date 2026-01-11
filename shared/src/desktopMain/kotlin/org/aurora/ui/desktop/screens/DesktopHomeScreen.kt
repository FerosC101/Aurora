package org.aurora.ui.desktop.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.aurora.maps.MapsProvider

/**
 * Desktop Home Screen - 1:1 port of Android HomeScreen
 * Adapted for desktop typography and layout
 */
@Composable
fun DesktopHomeScreen(
    mapsProvider: MapsProvider,
    onStartNavigation: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var showAIAssistant by remember { mutableStateOf(false) }
    var showQuickActions by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F7))
                .verticalScroll(scrollState)
        ) {
            // App Bar - adapted for desktop (larger font)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                shadowElevation = 0.dp
            ) {
                Text(
                    text = "Where to?",
                    fontSize = 36.sp, // Desktop: larger font
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1C1C1E),
                    letterSpacing = (-0.5).sp,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp)
                )
            }
            
            // Input Section Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp) // Desktop: more padding
                ) {
                    // Origin Field
                    OutlinedTextField(
                        value = origin,
                        onValueChange = { origin = it },
                        placeholder = { Text("Start location", fontSize = 16.sp) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(0xFF4CAF50), shape = CircleShape)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color(0xFFD1D1D6),
                            focusedTextColor = Color(0xFF1C1C1E),
                            unfocusedTextColor = Color(0xFF1C1C1E)
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Destination Field
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        placeholder = { Text("End location", fontSize = 16.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color(0xFFD1D1D6),
                            focusedTextColor = Color(0xFF1C1C1E),
                            unfocusedTextColor = Color(0xFF1C1C1E)
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
                    )
                    
                    if (destination.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Button(
                            onClick = { onStartNavigation(origin.ifEmpty { "Current location" }, destination) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp), // Desktop: taller button
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF007AFF)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                "Start Navigation",
                                fontSize = 18.sp, // Desktop: larger font
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.3).sp
                            )
                        }
                    }
                    
                    // Multi-stop button
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF007AFF)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1D1D6)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Plan Multi-Stop Route",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Quick Actions
            if (showQuickActions) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                ) {
                    Text(
                        text = "Quick Actions",
                        fontSize = 17.sp, // Desktop: slightly larger
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DesktopQuickActionCard(
                            icon = Icons.Default.Home,
                            label = "Home",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                destination = "Home"
                            }
                        )
                        
                        DesktopQuickActionCard(
                            icon = Icons.Default.Star,
                            label = "Work",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                destination = "Work"
                            }
                        )
                        
                        DesktopQuickActionCard(
                            icon = Icons.Default.LocationOn,
                            label = "Nearby",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                origin = "Current Location"
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Recent Trips Section (placeholder for now)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Text(
                    text = "Recent Trips",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFFD1D1D6)
                            )
                            Text(
                                "No recent trips",
                                fontSize = 16.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
        
        // AI Assistant FAB - Desktop positioned bottom-right
        FloatingActionButton(
            onClick = { showAIAssistant = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp),
            containerColor = Color(0xFF007AFF),
            shape = RoundedCornerShape(18.dp),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 8.dp
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "AI Assistant",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    "AI Assistant",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun DesktopQuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
                .height(120.dp) // Desktop: taller cards
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(32.dp) // Desktop: larger icon
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                color = Color(0xFF1C1C1E),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
