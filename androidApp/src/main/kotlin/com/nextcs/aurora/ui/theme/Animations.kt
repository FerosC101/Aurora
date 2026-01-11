package com.nextcs.aurora.ui.theme

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Facebook-style color palette
 */
object AppColors {
    val Primary = Color(0xFF007AFF)
    val Secondary = Color(0xFF34C759)
    val Danger = Color(0xFFFF3B30)
    val Warning = Color(0xFFFF9500)
    val Background = Color(0xFFF5F5F7)
    val Surface = Color.White
    val TextPrimary = Color(0xFF1C1C1E)
    val TextSecondary = Color(0xFF8E8E93)
    val Divider = Color(0xFFE5E5EA)
    val BlueBubble = Color(0xFF007AFF)
    val GrayBubble = Color(0xFFE5E5EA)
}

/**
 * Smooth fade-in animation for content
 */
@Composable
fun AnimatedContent(
    visible: Boolean = true,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ) + expandVertically(
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 200)
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = 200)
        )
    ) {
        content()
    }
}

/**
 * Smooth scale animation for press feedback
 */
@Composable
fun PressableScale(
    interactionSource: MutableInteractionSource,
    targetScale: Float = 0.95f,
    content: @Composable (Modifier) -> Unit
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    content(Modifier.scale(scale))
}

/**
 * Slide in from bottom animation
 */
fun slideInFromBottom() = slideInVertically(
    initialOffsetY = { it },
    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
) + fadeIn(animationSpec = tween(durationMillis = 300))

/**
 * Slide out to bottom animation
 */
fun slideOutToBottom() = slideOutVertically(
    targetOffsetY = { it },
    animationSpec = tween(durationMillis = 250)
) + fadeOut(animationSpec = tween(durationMillis = 250))

/**
 * Slide in from right animation
 */
fun slideInFromRight() = slideInHorizontally(
    initialOffsetX = { it },
    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
) + fadeIn(animationSpec = tween(durationMillis = 300))

/**
 * Slide out to right animation
 */
fun slideOutToRight() = slideOutHorizontally(
    targetOffsetX = { it },
    animationSpec = tween(durationMillis = 250)
) + fadeOut(animationSpec = tween(durationMillis = 250))

/**
 * Bounce animation for success feedback
 */
@Composable
fun rememberBounceAnimation(): Animatable<Float, AnimationVector1D> {
    val bounceAnim = remember { Animatable(1f) }
    return bounceAnim
}

/**
 * Pulse animation for attention
 */
@Composable
fun PulseAnimation(
    durationMillis: Int = 1000,
    content: @Composable (Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    content(scale)
}
