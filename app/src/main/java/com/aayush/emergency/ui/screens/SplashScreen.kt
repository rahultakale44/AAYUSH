package com.aayush.emergency.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aayush.emergency.ui.theme.DeepNavy
import com.aayush.emergency.ui.theme.White
import kotlinx.coroutines.delay

/**
 * Premium Splash Screen
 * Shows only the AAYUSH logo (आयुष) in Devanagari on a premium dark background
 * Fades in, displays for 1.5-2 seconds, then fades out to role selection
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    // Fade in/out animation
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "splash_fade"
    )
    
    // Trigger animation and navigation
    LaunchedEffect(Unit) {
        // Fade in
        startAnimation = true
        // Stay visible
        delay(2000)
        // Fade out
        startAnimation = false
        delay(800)
        // Navigate
        onSplashComplete()
    }
    
    // Premium dark background with white logo
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "आयुष",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = White,
            textAlign = TextAlign.Center,
            letterSpacing = 4.sp,
            modifier = Modifier.alpha(alpha)
        )
    }
}
