package com.nextcs.aurora.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nextcs.aurora.navigation.Analytics

@Composable
fun AnalyticsDashboard(
    analytics: Analytics,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Your Journey Statistics",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                Text(
                    text = "Track your navigation progress",
                    fontSize = 13.sp,
                    color = Color(0xFF757575)
                )
            }
        }
        
        // Key Metrics Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Place,
                    iconColor = Color(0xFF1976D2),
                    label = "Total Trips",
                    value = analytics.totalTrips.toString(),
                    backgroundColor = Color(0xFFE3F2FD)
                )
                
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocationOn,
                    iconColor = Color(0xFF4CAF50),
                    label = "Distance",
                    value = String.format("%.1f km", analytics.totalDistance),
                    backgroundColor = Color(0xFFE8F5E9)
                )
            }
            
            // Second Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Star,
                    iconColor = Color(0xFFFF9800),
                    label = "Travel Time",
                    value = String.format("%.1f h", analytics.totalDuration),
                    backgroundColor = Color(0xFFFFF3E0)
                )
                
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Info,
                    iconColor = Color(0xFFF44336),
                    label = "Avg. Safety",
                    value = "${analytics.averageSafetyScore}%",
                    backgroundColor = Color(0xFFFFEBEE)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Highlights Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎯 Highlights",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    HighlightRow(
                        icon = Icons.Default.Warning,
                        title = "Hazards Avoided",
                        value = analytics.hazardsAvoided.toString(),
                        color = Color(0xFFF44336)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    HighlightRow(
                        icon = Icons.Default.Star,
                        title = "Time Saved (This Month)",
                        value = String.format("%.1f h", analytics.timeSavedThisMonth),
                        color = Color(0xFF4CAF50)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    HighlightRow(
                        icon = Icons.Default.Info,
                        title = "Total Time Saved",
                        value = String.format("%.1f h", analytics.totalTimeSaved),
                        color = Color(0xFF1976D2)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Safety Score Breakdown
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Safety Performance",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF212121)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Safety Score Ring
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        CircularProgressIndicator(
                            progress = { analytics.averageSafetyScore / 100f },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            color = getSafetyColor(analytics.averageSafetyScore),
                            strokeWidth = 8.dp
                        )
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${analytics.averageSafetyScore}%",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF212121)
                            )
                            Text(
                                text = getSafetyLabel(analytics.averageSafetyScore),
                                fontSize = 11.sp,
                                color = Color(0xFF757575)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Description
                    Text(
                        text = getSafetyDescription(analytics.averageSafetyScore),
                        fontSize = 12.sp,
                        color = Color(0xFF757575),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick Stats
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFE3F2FD),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "💡 Pro Tips",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Using Smart Routes consistently can help you save time and avoid hazards. Continue exploring new routes to improve your navigation experience!",
                        fontSize = 12.sp,
                        color = Color(0xFF1565C0),
                        lineHeight = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    backgroundColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF212121)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF757575)
            )
        }
    }
}

@Composable
fun HighlightRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF212121)
                )
                Text(
                    text = "Performance metric",
                    fontSize = 11.sp,
                    color = Color(0xFF757575)
                )
            }
        }
        
        Surface(
            color = color.copy(alpha = 0.1f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

private fun getSafetyColor(score: Int): Color {
    return when {
        score >= 85 -> Color(0xFF4CAF50)
        score >= 70 -> Color(0xFF8BC34A)
        score >= 50 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

private fun getSafetyLabel(score: Int): String {
    return when {
        score >= 85 -> "Excellent"
        score >= 70 -> "Good"
        score >= 50 -> "Fair"
        else -> "Poor"
    }
}

private fun getSafetyDescription(score: Int): String {
    return when {
        score >= 85 -> "You're a safe navigator! Keep it up."
        score >= 70 -> "Good safety practices. Room for improvement."
        score >= 50 -> "Be more cautious. Avoid hazard-prone areas."
        else -> "Focus on safer routes and hazard avoidance."
    }
}
