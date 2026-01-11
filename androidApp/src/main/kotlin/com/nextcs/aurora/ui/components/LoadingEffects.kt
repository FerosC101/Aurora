package com.nextcs.aurora.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shimmer loading effect for skeleton screens
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.3f),
        Color.LightGray.copy(alpha = 0.5f),
        Color.LightGray.copy(alpha = 0.3f)
    )
    
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )
    
    Box(
        modifier = modifier.background(brush)
    )
}

/**
 * Skeleton loading for friend card
 */
@Composable
fun FriendCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile photo skeleton
            ShimmerEffect(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), CircleShape)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                // Name skeleton
                ShimmerEffect(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(20.dp)
                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Email skeleton
                ShimmerEffect(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(16.dp)
                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                )
            }
            
            // Score skeleton
            ShimmerEffect(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), CircleShape)
            )
        }
    }
}

/**
 * Skeleton loading for carpool card
 */
@Composable
fun CarpoolCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Driver info skeleton
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerEffect(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.LightGray.copy(alpha = 0.3f), CircleShape)
                    )
                    
                    Column {
                        ShimmerEffect(
                            modifier = Modifier
                                .width(100.dp)
                                .height(18.dp)
                                .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        ShimmerEffect(
                            modifier = Modifier
                                .width(80.dp)
                                .height(14.dp)
                                .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        )
                    }
                }
                
                // Price skeleton
                ShimmerEffect(
                    modifier = Modifier
                        .width(60.dp)
                        .height(24.dp)
                        .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Route skeleton
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Button skeleton
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            )
        }
    }
}

/**
 * Skeleton loading for profile screen
 */
@Composable
fun ProfileSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F7))
            .padding(16.dp)
    ) {
        // Profile photo skeleton
        ShimmerEffect(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterHorizontally)
                .background(Color.LightGray.copy(alpha = 0.3f), CircleShape)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Name skeleton
        ShimmerEffect(
            modifier = Modifier
                .width(200.dp)
                .height(28.dp)
                .align(Alignment.CenterHorizontally)
                .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Email skeleton
        ShimmerEffect(
            modifier = Modifier
                .width(160.dp)
                .height(18.dp)
                .align(Alignment.CenterHorizontally)
                .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Button skeleton
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        )
    }
}
