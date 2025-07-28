package com.example.jewelleryapp.screen.registerScreen

// RegisterScreen.kt

import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.jewelleryapp.R
import com.example.jewelleryapp.screen.loginScreen.BrandHeader
import com.example.jewelleryapp.screen.loginScreen.GoldenShade

@Composable
fun RegisterScreen(viewModel: RegisterViewModel
                   , navController: NavController,
                   googleSignInLauncher: ActivityResultLauncher<Intent> // Add this parameter
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isConfirmPasswordVisible by remember { mutableStateOf(false) }
    val registerState by viewModel.registerState.collectAsState()
    val scaffoldState = rememberScaffoldState()
    val focusManager = LocalFocusManager.current
    var phone by remember { mutableStateOf("") } // Add this


    // Password validation error states
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) } // Add this


    // Handle registration state changes
    LaunchedEffect(registerState) {
        Log.d("RegisterScreen", "Register state changed to: $registerState")

        when (registerState) {
            is RegisterState.Success -> {
                Log.d("RegisterScreen", "Navigation to home from register")
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
                viewModel.resetState()
            }
            is RegisterState.Error -> {
                Log.d("RegisterScreen", "Showing register error: ${(registerState as RegisterState.Error).message}")
                scaffoldState.snackbarHostState.showSnackbar(
                    message = (registerState as RegisterState.Error).message
                )
                viewModel.resetState()
            }
            is RegisterState.GoogleSignInLoading -> {
                Log.d("RegisterScreen", "Google Sign-In loading state active in register")
            }
            else -> {}
        }
    }



    Scaffold(
        scaffoldState = scaffoldState
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()) // Add this line
                    .padding(bottom = 16.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        focusManager.clearFocus()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BrandHeader()
                CreateAccountHeader()

                FullNameInput(
                    fullName = fullName,
                    onFullNameChange = {
                        fullName = it
                        nameError = if (it.isBlank()) "Name is required" else null
                    },
                    error = nameError
                )

                Spacer(modifier = Modifier.height(10.dp))

                EmailInput(
                    email = email,
                    onEmailChange = {
                        email = it
                        // Basic email validation
                        emailError = if (it.isBlank()) {
                            "Email is required"
                        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches()) {
                            "Please enter a valid email"
                        } else {
                            null
                        }
                    },
                    error = emailError
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Phone input (optional)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Phone Number (Optional)",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            // Basic phone validation (optional)
                            phoneError = if (it.isNotEmpty() && !isValidPhone(it)) {
                                "Please enter a valid phone number"
                            } else {
                                null
                            }
                        },
                        placeholder = { Text("Enter your phone number") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(8.dp),
                        colors = getTextFieldColors(),
                        isError = phoneError != null
                    )

                    if (phoneError != null) {
                        Text(
                            text = phoneError!!,
                            color = MaterialTheme.colors.error,
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                PasswordInput(
                    password = password,
                    isPasswordVisible = isPasswordVisible,
                    onPasswordChange = {
                        password = it
                        // Password validation
                        passwordError = if (it.length < 6) {
                            "Password must be at least 6 characters"
                        } else {
                            null
                        }

                        // Validate confirmation if already entered
                        if (confirmPassword.isNotEmpty()) {
                            confirmPasswordError = if (it != confirmPassword) {
                                "Passwords don't match"
                            } else {
                                null
                            }
                        }
                    },
                    onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                    label = "Password",
                    placeholder = "Create your password",
                    error = passwordError
                )

                Spacer(modifier = Modifier.height(10.dp))

                PasswordInput(
                    password = confirmPassword,
                    isPasswordVisible = isConfirmPasswordVisible,
                    onPasswordChange = {
                        confirmPassword = it
                        // Confirmation validation
                        confirmPasswordError = if (it != password) {
                            "Passwords don't match"
                        } else {
                            null
                        }
                    },
                    onTogglePasswordVisibility = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                    label = "Confirm Password",
                    placeholder = "Confirm your password",
                    imeAction = ImeAction.Done,
                    error = confirmPasswordError
                )

                Spacer(modifier = Modifier.height(8.dp))

                SignUpButton(
                    isLoading = registerState is RegisterState.Loading,
                    onClick = {
                        // Validate inputs
                        nameError = if (fullName.isBlank()) "Name is required" else null

                        emailError = if (email.isBlank()) {
                            "Email is required"
                        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            "Please enter a valid email"
                        } else {
                            null
                        }

                        passwordError = if (password.length < 6) {
                            "Password must be at least 6 characters"
                        } else {
                            null
                        }

                        confirmPasswordError = if (password != confirmPassword) {
                            "Passwords don't match"
                        } else {
                            null
                        }
                        phoneError = if (phone.isNotEmpty() && !isValidPhone(phone)) {
                            "Please enter a valid phone number"
                        } else {
                            null
                        }


                        // Proceed if all validations pass
                        if (nameError == null && emailError == null && phoneError == null &&
                            passwordError == null && confirmPasswordError == null) {
                            focusManager.clearFocus()
                            viewModel.registerWithEmailAndPassword(fullName, email, password, phone)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                AlternativeSignUpOptions(
                    isLoading = registerState is RegisterState.Loading || registerState is RegisterState.GoogleSignInLoading,
                    isGoogleLoading = registerState is RegisterState.GoogleSignInLoading,
                    onGoogleSignUpClick = {
                        try {
                            val signInIntent = viewModel.startGoogleSignIn()
                            googleSignInLauncher.launch(signInIntent)
                        } catch (e: Exception) {
                            viewModel.cancelGoogleSignIn()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                SignInPrompt(
                    onSignInClick = {
                        navController.popBackStack()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
private fun isValidPhone(phone: String): Boolean {
    // Basic phone validation - you can make this more sophisticated for your needs
    return phone.length >= 10 && phone.all { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' }
}
@Composable
private fun CreateAccountHeader() {
    Text(
        text = "Create Account",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        style = androidx.compose.ui.text.TextStyle(lineHeight = 0.sp)
    )
    Text(
        text = "Please fill in your details to continue",
        fontSize = 14.sp,
        color = Color.Gray
    )

    Spacer(modifier = Modifier.height(3.dp))
}

@Composable
private fun FullNameInput(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    error: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Full Name",
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            placeholder = { Text("Enter your full name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            shape = RoundedCornerShape(8.dp),
            colors = getTextFieldColors(),
            isError = error != null
        )

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun EmailInput(
    email: String,
    onEmailChange: (String) -> Unit,
    error: String? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Email or Phone Number",
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            placeholder = { Text("Enter your email or phone") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            shape = RoundedCornerShape(8.dp),
            colors = getTextFieldColors(),
            isError = error != null
        )

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun PasswordInput(
    password: String,
    isPasswordVisible: Boolean,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    label: String,
    placeholder: String,
    imeAction: ImeAction = ImeAction.Next,
    error: String? = null
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            visualTransformation = if (isPasswordVisible)
                VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            trailingIcon = {
                PasswordVisibilityToggle(
                    isPasswordVisible = isPasswordVisible,
                    onToggleClick = onTogglePasswordVisibility
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = getTextFieldColors(),
            isError = error != null
        )

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
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
private fun SignUpButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = GoldenShade
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
                text = "Sign Up",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AlternativeSignUpOptions(
    isLoading: Boolean,
    isGoogleLoading: Boolean, // Add this parameter
    onGoogleSignUpClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(2.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Divider(
            modifier = Modifier
                .weight(1f)
                .height(1.dp),
            color = Color.LightGray
        )

        Text(
            text = "Or continue with",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Divider(
            modifier = Modifier
                .weight(1f)
                .height(1.dp),
            color = Color.LightGray
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedButton(
        onClick = onGoogleSignUpClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.White
        ),
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
                    color = GoldenShade, // Use your existing golden color
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Signing in...", color = Color.Gray, fontSize = 16.sp)
            } else {
                Image(
                    painter = painterResource(id = R.drawable.google_icon),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continue with Google",
                    color = Color.Black,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SignInPrompt(onSignInClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Already have an account? ",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = "Sign In",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFC4A661),
            modifier = Modifier.clickable(onClick = onSignInClick)
        )
    }
}

@Composable
private fun getTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    focusedBorderColor = Color(0xFFC4A661),
    unfocusedBorderColor = Color.LightGray,
    errorBorderColor = MaterialTheme.colors.error
)