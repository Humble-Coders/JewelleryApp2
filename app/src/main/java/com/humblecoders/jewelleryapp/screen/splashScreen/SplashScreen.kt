package com.humblecoders.jewelleryapp.screen.splashScreen

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humblecoders.jewelleryapp.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    // Animation values with faster timing
    val logoScale = remember { Animatable(0.3f) }
    val logoAlpha = remember { Animatable(0f) }
    val crownRotation = remember { Animatable(-20f) }
    
    val brandNameAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }
    val footerAlpha = remember { Animatable(0f) }
    
    val shimmerAlpha = remember { Animatable(0f) }
    
    val scope = rememberCoroutineScope()
    
    // Orchestrated animations
    LaunchedEffect(Unit) {
        scope.launch {
            // Crown logo animation (0-600ms)
            launch {
                delay(100)
                logoAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
            }
            launch {
                delay(100)
                logoScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                delay(100)
                crownRotation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            
            // Shimmer effect (300-900ms)
            launch {
                delay(300)
                shimmerAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(200)
                )
                shimmerAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(400)
                )
            }
            
            // Brand name fade in - slow and elegant (500-1500ms)
            launch {
                delay(500)
                brandNameAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(1000, easing = LinearOutSlowInEasing)
                )
            }
            
            // Tagline fade in - gentle (800-1800ms)
            launch {
                delay(800)
                taglineAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(1000, easing = LinearOutSlowInEasing)
                )
            }
            
            // Footer fade in - subtle (1100-2100ms)
            launch {
                delay(1100)
                footerAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(1000, easing = LinearOutSlowInEasing)
                )
            }
        }
        
        // Auth check with total 3 second display
        delay(3000)
        
        val user = FirebaseAuth.getInstance().currentUser
        val isAuthenticated = user != null
        
        Log.d("SplashScreen", "Auth check complete - User: ${user?.uid}, Authenticated: $isAuthenticated")
        
        onAuthCheckComplete(isAuthenticated)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White,
                        ThemeColorLight.copy(alpha = 0.15f),
                        Color.White
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        // Main content centered
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Crown Logo with animations
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            ) {
                // Shimmer effect
                Image(
                    painter = painterResource(id = R.drawable.crown),
                    contentDescription = "Crown Shimmer",
                    modifier = Modifier
                        .size(130.dp)
                        .alpha(shimmerAlpha.value * 0.6f)
                        .scale(1.1f)
                )
                
                // Subtle shadow
                Image(
                    painter = painterResource(id = R.drawable.crown),
                    contentDescription = "Crown Shadow",
                    modifier = Modifier
                        .size(120.dp)
                        .alpha(0.1f)
                        .offset(x = 3.dp, y = 3.dp)
                )
                
                // Main crown with rotation
                Image(
                    painter = painterResource(id = R.drawable.crown),
                    contentDescription = "Crown Logo",
                    modifier = Modifier
                        .size(120.dp)
                        .graphicsLayer {
                            rotationZ = crownRotation.value
                        }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Brand Name - fades in gradually
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(brandNameAlpha.value)
            ) {
                // GAGAN - Elegant serif
                Text(
                    text = "GAGAN",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = NavyBlue,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // JEWELLERS - Refined sans-serif
                Text(
                    text = "JEWELLERS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.SansSerif,
                    color = Gold,
                    letterSpacing = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Elegant tagline - fades in gradually
            Text(
                text = "Crafting Timeless Elegance",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.Serif,
                color = NavyBlue.copy(alpha = 0.6f),
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha.value)
            )
        }
        
        // Footer - Humble Solutions - fades in gradually
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .alpha(footerAlpha.value)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Decorative line
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(1.dp)
                        .background(NavyBlue.copy(alpha = 0.2f))
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "A Humble Solutions Product",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.SansSerif,
                    color = NavyBlue.copy(alpha = 0.5f),
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}