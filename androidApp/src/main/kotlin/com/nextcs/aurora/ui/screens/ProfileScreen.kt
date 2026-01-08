package com.nextcs.aurora.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextcs.aurora.navigation.VehicleProfileService

@Composable
fun ProfileScreen(
    userName: String,
    userEmail: String,
    onLogout: () -> Unit,
    onNavigateToFriends: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vehicleProfileService = remember { VehicleProfileService(context) }
    
    var isDarkMode by remember { mutableStateOf(false) }
    var selectedVehicle by remember { mutableStateOf(vehicleProfileService.getVehicleType()) }
    var showVehicleDialog by remember { mutableStateOf(false) }
    
    // Vehicle selection dialog
    if (showVehicleDialog) {
        AlertDialog(
            onDismissRequest = { showVehicleDialog = false },
            title = { Text("Select Vehicle Type") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VehicleOption(
                        icon = Icons.Default.Star,
                        label = "Car",
                        vehicleType = VehicleProfileService.TYPE_DRIVING,
                        isSelected = selectedVehicle == VehicleProfileService.TYPE_DRIVING,
                        onClick = {
                            vehicleProfileService.setVehicleType(VehicleProfileService.TYPE_DRIVING)
                            selectedVehicle = VehicleProfileService.TYPE_DRIVING
                            showVehicleDialog = false
                        }
                    )
                    VehicleOption(
                        icon = Icons.Default.ShoppingCart,
                        label = "Bicycle",
                        vehicleType = VehicleProfileService.TYPE_BICYCLING,
                        isSelected = selectedVehicle == VehicleProfileService.TYPE_BICYCLING,
                        onClick = {
                            vehicleProfileService.setVehicleType(VehicleProfileService.TYPE_BICYCLING)
                            selectedVehicle = VehicleProfileService.TYPE_BICYCLING
                            showVehicleDialog = false
                        }
                    )
                    VehicleOption(
                        icon = Icons.Default.Person,
                        label = "Walking",
                        vehicleType = VehicleProfileService.TYPE_WALKING,
                        isSelected = selectedVehicle == VehicleProfileService.TYPE_WALKING,
                        onClick = {
                            vehicleProfileService.setVehicleType(VehicleProfileService.TYPE_WALKING)
                            selectedVehicle = VehicleProfileService.TYPE_WALKING
                            showVehicleDialog = false
                        }
                    )
                    VehicleOption(
                        icon = Icons.Default.Build,
                        label = "Transit",
                        vehicleType = VehicleProfileService.TYPE_TRANSIT,
                        isSelected = selectedVehicle == VehicleProfileService.TYPE_TRANSIT,
                        onClick = {
                            vehicleProfileService.setVehicleType(VehicleProfileService.TYPE_TRANSIT)
                            selectedVehicle = VehicleProfileService.TYPE_TRANSIT
                            showVehicleDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showVehicleDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Header with Profile
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Picture with Logo
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(80.dp),
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.nextcs.aurora.R.mipmap.ic_launcher),
                            contentDescription = "Profile",
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = userName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                
                Text(
                    text = userEmail,
                    fontSize = 14.sp,
                    color = Color(0xFF757575)
                )
            }
        }
        
        // Settings Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Preferences",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF757575),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = "Friends",
                        subtitle = "Manage friends and location sharing",
                        onClick = onNavigateToFriends
                    )
                    Divider(color = Color(0xFFE0E0E0))
                    
                    SettingsItem(
                        icon = Icons.Default.Star,
                        title = "Vehicle Profile",
                        subtitle = vehicleProfileService.getVehicleDisplayName(selectedVehicle),
                        onClick = { showVehicleDialog = true }
                    )
                    Divider(color = Color(0xFFE0E0E0))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = Color(0xFF1E88E5),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Dark Mode",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF212121)
                                )
                                Text(
                                    text = "Reduce eye strain",
                                    fontSize = 13.sp,
                                    color = Color(0xFF757575)
                                )
                            }
                        }
                        
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { isDarkMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF1E88E5)
                            )
                        )
                    }
                    
                    Divider(color = Color(0xFFE0E0E0))
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Manage alerts",
                        onClick = { /* Notifications */ }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Account",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF757575),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Privacy Policy",
                        subtitle = "View our policies",
                        onClick = { /* Privacy */ }
                    )
                    Divider(color = Color(0xFFE0E0E0))
                    SettingsItem(
                        icon = Icons.Default.Email,
                        title = "Help & Support",
                        subtitle = "Get assistance",
                        onClick = { /* Support */ }
                    )
                    Divider(color = Color(0xFFE0E0E0))
                    SettingsItem(
                        icon = Icons.Default.ExitToApp,
                        title = "Logout",
                        subtitle = "Sign out of your account",
                        onClick = onLogout,
                        iconTint = Color(0xFFE53935)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Aurora v1.0.0",
                fontSize = 12.sp,
                color = Color(0xFF9E9E9E),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = Color(0xFF1E88E5)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF212121)
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF757575)
                )
            }
        }
        
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF9E9E9E)
        )
    }
}
@Composable
fun VehicleOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    vehicleType: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) Color(0xFFE3F2FD) else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFF1976D2) else Color(0xFF757575),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFF1976D2) else Color(0xFF212121)
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}