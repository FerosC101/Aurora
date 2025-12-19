package com.nextcs.aurora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.delay
import com.nextcs.aurora.audio.VoiceNavigationService
import com.nextcs.aurora.sensors.SpeedMonitor
import com.nextcs.aurora.ui.components.SpeedDisplay
import com.nextcs.aurora.ui.components.CompactSpeedDisplay

@Composable
fun SimpleNavigationScreen(
    origin: String,
    destination: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val voiceService = remember { VoiceNavigationService(context) }
    val speedMonitor = remember { SpeedMonitor(context) }
    
    val speedData by speedMonitor.speedData.collectAsState()
    val isVoiceEnabled by voiceService.isEnabled.collectAsState()
    val isVoiceReady by voiceService.isReady.collectAsState()
    
    var currentInstruction by remember { mutableStateOf("Head north on Main Street") }
    var distanceToTurn by remember { mutableStateOf(450) }
    var eta by remember { mutableStateOf("12 min") }
    var remainingDistance by remember { mutableStateOf("5.2 km") }
    
    // Demo: Simulate speed limit changes
    LaunchedEffect(Unit) {
        speedMonitor.setSpeedLimit(60)
        speedMonitor.startMonitoring()
        
        // Announce start
        delay(1000)
        voiceService.announce("Navigation started. Head north on Main Street.")
        
        // Demo instructions every 10 seconds
        var instructionIndex = 0
        val instructions = listOf(
            "Turn right" to "Elm Street",
            "Turn left" to "Oak Avenue",
            "Continue straight" to "Highway 101",
            "Take the exit" to "Downtown"
        )
        
        while (true) {
            delay(10000)
            if (instructionIndex < instructions.size) {
                val (direction, road) = instructions[instructionIndex]
                currentInstruction = "$direction onto $road"
                distanceToTurn = 500
                voiceService.announceTurn(direction, distanceToTurn, road)
                instructionIndex++
            }
        }
    }
    
    // Monitor speed violations
    LaunchedEffect(speedData.isExceeding) {
        if (speedData.isExceeding) {
            speedData.speedLimit?.let { limit ->
                voiceService.announceSpeedWarning(speedData.currentSpeed.toInt(), limit)
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            speedMonitor.stopMonitoring()
            voiceService.shutdown()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
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
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = eta,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E88E5)
                    )
                    Text(
                        text = remainingDistance,
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { voiceService.setEnabled(!isVoiceEnabled) }
                    ) {
                        Icon(
                            if (isVoiceEnabled && isVoiceReady) Icons.Default.Notifications 
                            else Icons.Default.Clear,
                            contentDescription = "Toggle voice",
                            tint = if (isVoiceEnabled && isVoiceReady) Color(0xFF4CAF50) 
                                   else Color(0xFF9E9E9E)
                        )
                    }
                }
            }
        }
        
        // Map Area (Placeholder)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF1E88E5)
                )
                Text(
                    text = "Map View",
                    fontSize = 16.sp,
                    color = Color(0xFF757575)
                )
                Text(
                    text = "$origin → $destination",
                    fontSize = 14.sp,
                    color = Color(0xFF9E9E9E)
                )
            }
            
            // Speed Display Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                SpeedDisplay(
                    currentSpeed = speedData.currentSpeed,
                    speedLimit = speedData.speedLimit,
                    isExceeding = speedData.isExceeding
                )
            }
        }
        
        // Bottom Instruction Panel
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Direction Icon
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E88E5),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    // Instruction Text
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentInstruction,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF212121)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "in $distanceToTurn m",
                            fontSize = 14.sp,
                            color = Color(0xFF757575)
                        )
                    }
                }
                
                // Speed Warning Banner
                if (speedData.isExceeding) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Exceeding speed limit!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }
            }
        }
    }
}
