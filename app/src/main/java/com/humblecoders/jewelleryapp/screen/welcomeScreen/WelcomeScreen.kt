package com.humblecoders.jewelleryapp.screen.welcomeScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.humblecoders.jewelleryapp.R

// Define colors
private val ButtonColor = Color(0xFF896C6C)
private val NavyBlue = Color(0xFF0E1A3D)
private val Gold = Color(0xFFE4BE67)

// Custom curved shape for the welcome image - steeper towards right
class CurvedBottomShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - 620f) // Steeper drop on right
            
            // Create a curve that's steeper towards the right
            quadraticTo(
                size.width * 0.3f, size.height + 20f, // Control point shifted left
                0f, size.height - 40f // Less steep on left
            )
            
            lineTo(0f, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun WelcomeScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Background Image - Welcome.png with curved bottom (no fade overlay)
        Image(
            painter = painterResource(id = R.drawable.welcome),
            contentDescription = "Welcome Background",
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .clip(CurvedBottomShape()),
            contentScale = ContentScale.Crop
        )

        // Content - positioned below the curved image
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spacer to push content below the curved image
            Spacer(modifier = Modifier.fillMaxHeight(0.55f))
            
            // Crown Icon
            Image(
                painter = painterResource(id = R.drawable.crown),
                contentDescription = "Crown Logo",
                modifier = Modifier.size(50.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Brand Name with elegant typography
            // GAGAN - Bodoni/Didot style (high-contrast serif)
            Text(
                text = "GAGAN",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif, // Using serif for Bodoni/Didot effect
                color = NavyBlue,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // JEWELLERS - Futura Light style (thin, spaced sans-serif)
            Text(
                text = "JEWELLERS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.SansSerif, // Clean sans-serif for Futura effect
                color = Gold,
                letterSpacing = 8.sp, // Increased letter spacing for Futura style
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Buttons with proper padding
            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Login Button
                Button(
                    onClick = {
                        navController.navigate("login") {
                            popUpTo("welcome") { inclusive = false }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonColor
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = "Login",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sign Up Button
                OutlinedButton(
                    onClick = {
                        navController.navigate("register") {
                            popUpTo("welcome") { inclusive = false }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = ButtonColor
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = ButtonColor
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text(
                        text = "Sign up",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = ButtonColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}