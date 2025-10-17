package com.example.jewelleryapp.screen.splashScreen

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jewelleryapp.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

// App theme colors
private val ThemeColor = Color(0xFF896C6C)
private val ThemeColorLight = Color(0xFFEEDDCA)
private val NavyBlue = Color(0xFF0E1A3D)
private val Gold = Color(0xFFE4BE67)

@Composable
fun SplashScreen(
    navController: NavController,
    onAuthCheckComplete: (Boolean) -> Unit
) {
    var isCheckingAuth by remember { mutableStateOf(true) }
    
    // Animation values
    val scale = remember { Animatable(0.8f) }
    val alpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    
    // Start animations
    LaunchedEffect(Unit) {
        // Scale and fade in animation
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(800, easing = LinearEasing)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(800, easing = LinearEasing)
        )
        
        // Continuous rotation for loading indicator
        rotation.animateTo(
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }
    
    LaunchedEffect(Unit) {
        // Add a small delay for splash screen effect
        delay(2000)
        
        // Check authentication state
        val user = FirebaseAuth.getInstance().currentUser
        val isAuthenticated = user != null
        
        Log.d("SplashScreen", "Auth check complete - User: ${user?.uid}, Authenticated: $isAuthenticated")
        
        // Notify parent about auth check completion
        onAuthCheckComplete(isAuthenticated)
        isCheckingAuth = false
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        ThemeColorLight.copy(alpha = 0.1f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // Crown Logo with subtle shadow effect
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                // Shadow effect
                Image(
                    painter = painterResource(id = R.drawable.crown),
                    contentDescription = "Crown Logo Shadow",
                    modifier = Modifier
                        .size(100.dp)
                        .alpha(0.1f)
                        .offset(x = 2.dp, y = 2.dp)
                )
                
                // Main crown logo
                Image(
                    painter = painterResource(id = R.drawable.crown),
                    contentDescription = "Crown Logo",
                    modifier = Modifier.size(100.dp)
                )
            }
            
            // Brand Name with elegant typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                // GAGAN - Bodoni/Didot style (high-contrast serif)
                androidx.compose.material3.Text(
                    text = "GAGAN",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = NavyBlue,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                
                // JEWELLERS - Futura Light style (thin, spaced sans-serif)
                androidx.compose.material3.Text(
                    text = "JEWELLERS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.SansSerif,
                    color = Gold,
                    letterSpacing = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Loading indicator with custom styling
            if (isCheckingAuth) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(top = 48.dp)
                ) {
                    // Background circle for loading indicator
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                color = ThemeColorLight.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    )
                    
                    // Custom loading indicator
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = ThemeColor,
                        strokeWidth = 3.dp
                    )
                }
                
                // Loading text
                androidx.compose.material3.Text(
                    text = "Loading...",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = NavyBlue.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
