package com.example.jewelleryapp.screen.splashScreen

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.jewelleryapp.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    onAuthCheckComplete: (Boolean) -> Unit
) {
    var isCheckingAuth by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        // Add a small delay for splash screen effect
        delay(1500)
        
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
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        // Crown Logo
        Image(
            painter = painterResource(id = R.drawable.crown),
            contentDescription = "Crown Logo",
            modifier = Modifier.size(80.dp)
        )
        
        // Loading indicator below the logo
        if (isCheckingAuth) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 100.dp)
                    .size(40.dp),
                color = Color(0xFF896C6C)
            )
        }
    }
}
