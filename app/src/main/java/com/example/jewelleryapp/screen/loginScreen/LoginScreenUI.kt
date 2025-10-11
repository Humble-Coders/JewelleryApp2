// LoginScreen.kt (modified)
package com.example.jewelleryapp.screen.loginScreen

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jewelleryapp.R
import android.content.Intent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


// Updated theme colors
val ThemeColor = Color(0xFF896C6C)
val NavyBlue = Color(0xFF0E1A3D)
val Gold = Color(0xFFE4BE67)
@Composable
fun LoginScreen(viewModel: LoginViewModel,
                navController: NavController,
                googleSignInLauncher: ActivityResultLauncher<Intent> // Add this parameter
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    val loginState by viewModel.loginState.collectAsState()
    val scaffoldState = rememberScaffoldState()
    val focusManager = LocalFocusManager.current

    // Check if user is already logged in
    LaunchedEffect(Unit) {
        if (viewModel.isUserLoggedIn()) {
            navController.navigate("home") {
                popUpTo("welcome") { inclusive = true }
            }
        }
    }

    // Handle login state changes
    // Update the existing LaunchedEffect for loginState
    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginState.Success -> {
                navController.navigate("home") {
                    popUpTo("welcome") { inclusive = true }
                }
                viewModel.resetState()
            }
            is LoginState.Error -> {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = (loginState as LoginState.Error).message
                )
                viewModel.resetState()
            }
            is LoginState.PasswordResetSent -> {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = "Password reset email sent. Check your inbox."
                )
                viewModel.resetState()
            }
            // Handle Google Sign-In loading state if needed
            is LoginState.GoogleSignInLoading -> {
                // Optional: You can show additional UI feedback here
            }
            else -> {}
        }
    }



    Scaffold(
        scaffoldState = scaffoldState
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()) // Add this line
                .background(Color.White)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandHeader()
            Spacer(Modifier.padding(28.dp))

            WelcomeSection()

            EmailInput(email = email, onEmailChange = { email = it })
            Spacer(modifier = Modifier.height(16.dp))

            PasswordInput(
                password = password,
                isPasswordVisible = isPasswordVisible,
                onPasswordChange = { password = it },
                onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                onForgotPasswordClick = {
                    if (email.isNotEmpty()) {
                        viewModel.resetPassword(email)
                    } else {
                        // Show error that email is required
                    }
                }
            )

            Spacer(modifier = Modifier.height(31.dp))

            SignInButton(
                isLoading = loginState is LoginState.Loading,
                onClick = {
                    focusManager.clearFocus()
                    viewModel.signInWithEmailAndPassword(email, password)
                }
            )

            AlternativeSignInOptions(
                isLoading = loginState is LoginState.Loading || loginState is LoginState.GoogleSignInLoading,
                isGoogleLoading = loginState is LoginState.GoogleSignInLoading,
                onGoogleSignInClick = {
                    try {
                        val signInIntent = viewModel.startGoogleSignIn()
                        googleSignInLauncher.launch(signInIntent)
                    } catch (e: Exception) {
                        // Handle launcher error
                        viewModel.cancelGoogleSignIn()
                    }
                }
            )


        }
    }
}

@Composable
fun BrandHeader() {
    Spacer(modifier = Modifier.height(60.dp))

    Image(
        painter = painterResource(id = R.drawable.crown),
        contentDescription = "Crown Logo",
        modifier = Modifier.size(60.dp)
    )

    Spacer(modifier = Modifier.height(8.dp))
    
    // GAGAN - Bodoni/Didot style (high-contrast serif)
    Text(
        text = "GAGAN",
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Serif,
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
        fontFamily = FontFamily.SansSerif,
        color = Gold,
        letterSpacing = 8.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun WelcomeSection() {
    Text(
        text = "Welcome Back",
        fontSize = 33.sp,
        fontWeight = FontWeight.Medium,
        color = ThemeColor
    )
    Spacer(modifier = Modifier.padding(8.dp))

    Text(
        text = "Please sign in to continue",
        fontSize = 16.sp,
        color = ThemeColor,
        modifier = Modifier.padding(bottom = 24.dp)
    )
}

@Composable
private fun EmailInput(email: String, onEmailChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Email or Phone Number", fontSize = 14.sp, color = ThemeColor)

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = { Text("Enter your email or phone", color = ThemeColor) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            shape = RoundedCornerShape(8.dp),
            colors = getTextFieldColors()
        )
    }
}

@Composable
private fun PasswordInput(
    password: String,
    isPasswordVisible: Boolean,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onForgotPasswordClick: () -> Unit

) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Password", fontSize = 14.sp, color = ThemeColor)

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { Text("Enter your password", color = ThemeColor) },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            trailingIcon = {
                PasswordVisibilityToggle(
                    isPasswordVisible = isPasswordVisible,
                    onToggleClick = onTogglePasswordVisibility
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = getTextFieldColors()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Forgot Password?",
                fontSize = 12.sp,
                color = ThemeColor, // Theme color
                modifier = Modifier.clickable(onClick = onForgotPasswordClick)
            )
        }
    }
}

@Composable
private fun PasswordVisibilityToggle(
    isPasswordVisible: Boolean,
    onToggleClick: () -> Unit
) {
    IconButton(onClick = onToggleClick) {
        Icon(
            imageVector = if (isPasswordVisible)
                Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = "Toggle password visibility"
        )
    }
}

@Composable
private fun SignInButton(isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = ThemeColor
        ),
        shape = RoundedCornerShape(8.dp),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = "Sign In",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AlternativeSignInOptions(
    isLoading: Boolean,
    isGoogleLoading: Boolean, // Add this parameter
    onGoogleSignInClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(24.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Divider(modifier = Modifier.weight(1f), color = ThemeColor.copy(alpha = 0.3f))
        Text("Or continue with", fontSize = 14.sp, color = ThemeColor, modifier = Modifier.padding(horizontal = 8.dp))
        Divider(modifier = Modifier.weight(1f), color = ThemeColor.copy(alpha = 0.3f))
    }

    Spacer(modifier = Modifier.height(29.dp))

    OutlinedButton(
        onClick = onGoogleSignInClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.White),
        border = BorderStroke(1.dp, Color.LightGray),
        shape = RoundedCornerShape(8.dp),
        enabled = !isLoading // Disable when any loading is happening
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Show loading indicator specifically for Google Sign-In
            if (isGoogleLoading) {
                CircularProgressIndicator(
                    color = ThemeColor, // Match new theme color
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Signing in...", color = ThemeColor, fontSize = 16.sp)
            } else {
                Image(
                    painter = painterResource(id = R.drawable.google_icon),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Continue with Google", color = ThemeColor, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SignUpPrompt(onSignUpClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Don't have an account? ", fontSize = 14.sp, color = Color.Gray)
        Text(
            text = "Sign Up",
            fontSize = 14.sp,
            fontWeight = FontWeight.W600,
            color = ThemeColor,
            modifier = Modifier.clickable(onClick = onSignUpClick)
        )
    }
}

@Composable
private fun getTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    focusedBorderColor = ThemeColor,
    unfocusedBorderColor = ThemeColor.copy(alpha = 0.5f),
    textColor = ThemeColor,
    cursorColor = ThemeColor,
    placeholderColor = ThemeColor.copy(alpha = 0.7f),
    focusedLabelColor = ThemeColor,
    unfocusedLabelColor = ThemeColor.copy(alpha = 0.7f)
)