package org.aurora.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val progress: Float,
    val isUnlocked: Boolean,
    val unlockedDate: String? = null
)

@Composable
fun AchievementBadge(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (achievement.isUnlocked) 1f else 0.85f,
        animationSpec = spring(dampingRatio = 0.6f)
    )
    
    val infiniteTransition = rememberInfiniteTransition()
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    
    Card(
        modifier = modifier.width(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) 
                achievement.color.copy(alpha = 0.2f) 
            else 
                Color(0xFF334155).copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (achievement.isUnlocked) 8.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = if (achievement.isUnlocked) {
                            Brush.linearGradient(
                                colors = listOf(
                                    achievement.color,
                                    achievement.color.copy(alpha = 0.6f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF475569),
                                    Color(0xFF334155)
                                )
                            )
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (achievement.isUnlocked) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f * (1f - shimmer)),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = size.minDimension / 2 * (1f + shimmer * 0.3f)
                            ),
                            center = center,
                            radius = size.minDimension / 2
                        )
                    }
                }
                
                Icon(
                    achievement.icon,
                    contentDescription = null,
                    tint = if (achievement.isUnlocked) Color.White else Color(0xFF64748B),
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                achievement.title,
                color = if (achievement.isUnlocked) Color.White else Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            
            if (!achievement.isUnlocked && achievement.progress > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { achievement.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = achievement.color,
                    trackColor = Color(0xFF334155),
                )
                Text(
                    "${(achievement.progress * 100).toInt()}%",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun AchievementPanel(modifier: Modifier = Modifier) {
    val achievements = remember {
        listOf(
            Achievement(
                "first_ride",
                "First Ride",
                "Complete your first trip",
                Icons.Default.DirectionsBike,
                Color(0xFF3B82F6),
                1f,
                true,
                "2 days ago"
            ),
            Achievement(
                "safe_rider",
                "Safe Rider",
                "Avoid 100 hazards",
                Icons.Default.Shield,
                Color(0xFF10B981),
                0.87f,
                false
            ),
            Achievement(
                "speed_demon",
                "Speed Demon",
                "Save 5 hours total",
                Icons.Default.Speed,
                Color(0xFFF59E0B),
                1f,
                true,
                "1 week ago"
            ),
            Achievement(
                "explorer",
                "Explorer",
                "Travel 1000km",
                Icons.Default.Explore,
                Color(0xFF8B5CF6),
                0.65f,
                false
            ),
            Achievement(
                "night_owl",
                "Night Owl",
                "Complete 10 night rides",
                Icons.Default.NightsStay,
                Color(0xFF6366F1),
                0.3f,
                false
            ),
            Achievement(
                "commuter",
                "Daily Commuter",
                "Ride 30 days straight",
                Icons.Default.CalendarMonth,
                Color(0xFFEC4899),
                0.53f,
                false
            )
        )
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFA726),
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        "Achievements",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    "${achievements.count { it.isUnlocked }}/${achievements.size}",
                    color = Color(0xFF3B82F6),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(achievements) { achievement ->
                    AchievementBadge(achievement)
                }
            }
        }
    }
}
