package com.humblecoders.jewelleryapp

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.humblecoders.jewelleryapp.repository.FirebaseAuthRepository
import com.humblecoders.jewelleryapp.repository.JewelryRepository
import com.humblecoders.jewelleryapp.repository.ProfileRepository
import com.humblecoders.jewelleryapp.screen.allProducts.AllProductsViewModel
import com.humblecoders.jewelleryapp.screen.categoriesScreen.CategoriesViewModel
import com.humblecoders.jewelleryapp.screen.categoriesScreen.CategoryScreenView
import com.humblecoders.jewelleryapp.screen.categoryProducts.CategoryProductsScreen
import com.humblecoders.jewelleryapp.screen.categoryProducts.CategoryProductsViewModel
import com.humblecoders.jewelleryapp.screen.homeScreen.DrawerViewModel
import com.humblecoders.jewelleryapp.screen.homeScreen.HomeScreen
import com.humblecoders.jewelleryapp.screen.homeScreen.HomeViewModel
import com.humblecoders.jewelleryapp.screen.productDetailScreen.ItemDetailViewModel
import com.humblecoders.jewelleryapp.screen.productDetailScreen.JewelryProductScreen
import com.humblecoders.jewelleryapp.screen.loginScreen.LoginScreen
import com.humblecoders.jewelleryapp.screen.loginScreen.LoginViewModel
import com.humblecoders.jewelleryapp.screen.profileScreen.ProfileScreen
import com.humblecoders.jewelleryapp.screen.profileScreen.ProfileViewModel
import com.humblecoders.jewelleryapp.screen.registerScreen.RegisterScreen
import com.humblecoders.jewelleryapp.screen.registerScreen.RegisterViewModel
import com.humblecoders.jewelleryapp.screen.homeScreen.StoreInfoViewModel
import com.humblecoders.jewelleryapp.screen.wishlist.WishlistScreen
import com.humblecoders.jewelleryapp.screen.wishlist.WishlistViewModel
import com.humblecoders.jewelleryapp.screen.orderHistory.OrderHistoryScreen
import com.humblecoders.jewelleryapp.screen.orderHistory.OrderHistoryViewModel
import com.humblecoders.jewelleryapp.screen.welcomeScreen.WelcomeScreen
import com.humblecoders.jewelleryapp.screen.splashScreen.SplashScreen
import com.humblecoders.jewelleryapp.ui.theme.JewelleryAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.humblecoders.jewelleryapp.screen.allProducts.AllProductsScreen
import com.humblecoders.jewelleryapp.screen.carouselProducts.CarouselProductsScreen
import com.humblecoders.jewelleryapp.screen.carouselProducts.CarouselProductsViewModel
import com.humblecoders.jewelleryapp.screen.homeScreen.StoreInfoScreen
import com.humblecoders.jewelleryapp.utils.VideoCacheManager
import com.humblecoders.jewelleryapp.repository.VideoBookingRepository
import com.humblecoders.jewelleryapp.screen.booking.ConsultationHistoryScreen
import com.humblecoders.jewelleryapp.screen.booking.MyBookingsScreen
import com.humblecoders.jewelleryapp.screen.booking.VideoBookingViewModel
import com.humblecoders.jewelleryapp.screen.booking.VideoConsultationScreen


class MainActivity : ComponentActivity() {
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var registerViewModel: RegisterViewModel
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var categoryViewModel: CategoriesViewModel
    private lateinit var itemDetailViewModel: ItemDetailViewModel
    private lateinit var wishlistViewModel: WishlistViewModel
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var categoryProductsViewModel: CategoryProductsViewModel // Add this line
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var storeInfoViewModel: StoreInfoViewModel // Add this line
    private lateinit var allProductsViewModel: AllProductsViewModel // Add this line
    private lateinit var drawerViewModel: DrawerViewModel
    private lateinit var orderHistoryViewModel: OrderHistoryViewModel
    
    // Flag to track if sign out is in progress to prevent false navigation
    private var isSigningOut = false







    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Update the launcher in MainActivity
        // Update the launcher in MainActivity to handle both login and register
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.d("MainActivity", "Google Sign-In result: ${result.resultCode}")

            when (result.resultCode) {
                RESULT_OK -> {
                    Log.d("MainActivity", "Google Sign-In OK, processing result")
                    // Handle for both login and register ViewModels
                    loginViewModel.handleGoogleSignInResult(result.data)
                    registerViewModel.handleGoogleSignInResult(result.data)
                }
                RESULT_CANCELED -> {
                    Log.d("MainActivity", "Google Sign-In cancelled by user")
                    loginViewModel.cancelGoogleSignIn()
                    registerViewModel.cancelGoogleSignIn()
                }
                else -> {
                    Log.d("MainActivity", "Google Sign-In failed with code: ${result.resultCode}")
                    loginViewModel.cancelGoogleSignIn()
                    registerViewModel.cancelGoogleSignIn()
                }
            }
        }

        // Initialize Firebase Auth
         firebaseAuth = FirebaseAuth.getInstance()

        // Initialize Firestore
        val firestore = FirebaseFirestore.getInstance()

        // Initialize Repositories
        val authRepository = FirebaseAuthRepository(firebaseAuth,this)
        val profileRepository = ProfileRepository(firebaseAuth, firestore, this)
        // Get current user ID for repository (or empty string if not logged in)
        val userId = firebaseAuth.currentUser?.uid ?: ""
        Log.d("MainActivity", "User ID: $userId")
        val jewelryRepository = JewelryRepository(userId, firestore,firebaseAuth)




        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            val newUserId = user?.uid ?: ""
            val oldUserId = userId

            // Update repository with new user ID when auth state changes
            if (newUserId != oldUserId) {
                Log.d("MainActivity", "Auth changed: oldUserId='$oldUserId', newUserId='$newUserId'")
                
                // Clear all ViewModel states when user changes (sign out or sign in)
                if (oldUserId.isNotEmpty() && newUserId.isEmpty()) {
                    // User signed out - clear all state
                    isSigningOut = true
                    Log.d("MainActivity", "User signed out - clearing all ViewModel states")
                    if (::wishlistViewModel.isInitialized) {
                        wishlistViewModel.clearAllState()
                    }
                    if (::homeViewModel.isInitialized) {
                        homeViewModel.clearAllState()
                    }
                    if (::profileViewModel.isInitialized) {
                        profileViewModel.clearAllState()
                    }
                    if (::itemDetailViewModel.isInitialized) {
                        itemDetailViewModel.clearState()
                    }
                    if (::categoryViewModel.isInitialized) {
                        categoryViewModel.clearState()
                    }
                    if (::allProductsViewModel.isInitialized) {
                        allProductsViewModel.clearState()
                    }
                    
                    // Force clear Google Sign-In client to prevent cached credentials
                    try {
                        authRepository.signOutGoogle()
                        Log.d("MainActivity", "Cleared Google Sign-In client")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error clearing Google Sign-In", e)
                    }
                    
                    // Reset sign out flag after a delay to allow sign out to complete
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        isSigningOut = false
                        Log.d("MainActivity", "Sign out process completed, flag reset")
                    }, 2000) // 2 second delay to ensure sign out is complete
                }
                
                // Reset flag if user signed in (sign out was successful)
                if (oldUserId.isEmpty() && newUserId.isNotEmpty()) {
                    isSigningOut = false
                }
                
                // Update repository with new user ID (this clears all caches)
                jewelryRepository.updateUserId(newUserId)
                
                // If new user signed in, refresh/restart ViewModels
                if (newUserId.isNotEmpty()) {
                    Log.d("MainActivity", "New user signed in - refreshing ViewModels")
                    // Small delay to ensure repository user ID is updated
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (::wishlistViewModel.isInitialized) {
                            wishlistViewModel.restartListener()
                        }
                        if (::homeViewModel.isInitialized) {
                            homeViewModel.refreshData()
                        }
                        // Profile will load when screen is accessed
                    }, 100)
                }
            }
        }

        // Initialize ViewModels
        loginViewModel = LoginViewModel(authRepository)
        registerViewModel = RegisterViewModel(authRepository)
        homeViewModel = HomeViewModel(jewelryRepository)
        categoryViewModel = CategoriesViewModel(jewelryRepository)
        itemDetailViewModel = ItemDetailViewModel(jewelryRepository)
        wishlistViewModel = WishlistViewModel(jewelryRepository)
        categoryProductsViewModel = CategoryProductsViewModel(jewelryRepository, "")
        profileViewModel = ProfileViewModel(profileRepository, authRepository)
        storeInfoViewModel = StoreInfoViewModel(jewelryRepository)
        allProductsViewModel = AllProductsViewModel(jewelryRepository)
        drawerViewModel = DrawerViewModel(jewelryRepository)
        orderHistoryViewModel = OrderHistoryViewModel(jewelryRepository)

        // Start background video preloading
        startBackgroundVideoPreloading(jewelryRepository)


        
        // Configure window to allow content behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.parseColor("#C59E9E") // App's amber color
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        requestNotificationPermission()

        setContent {
            JewelleryAppTheme {
                Surface(
                    modifier = Modifier.background(Color.White)
                ) {

                    AppNavigation(
                        loginViewModel,
                        registerViewModel,
                        homeViewModel,
                        categoryViewModel,
                        itemDetailViewModel,
                        wishlistViewModel,
                        jewelryRepository,
                        activity = this  ,
                        intent = intent,
                        googleSignInLauncher = googleSignInLauncher ,// Pass the launcher
                        profileViewModel,
                        storeInfoViewModel, // Pass the store info ViewModel
                        allProductsViewModel, // Pass the all products ViewModel
                        drawerViewModel,
                        orderHistoryViewModel // Pass the order history ViewModel
                    )
                }
            }
        }

    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle deep links when app is already running
        setIntent(intent)
    }


    override fun onStart() {
        super.onStart()
        // Add auth state listener when activity starts
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        super.onStop()
        // Remove auth state listener when activity stops
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up video cache resources
        VideoCacheManager.releasePreloadPlayer()
    }

    /**
     * Start background video preloading for smooth playback
     */
    private fun startBackgroundVideoPreloading(repository: JewelryRepository) {
        val activityContext = this
        // Use a background thread to preload video data
        Thread {
            try {
                Log.d("MainActivity", "Starting background video preloading...")

                // Initialize video cache
                VideoCacheManager.initializeCache(activityContext)

                Log.d("MainActivity", "Background video preloading initiated")

            } catch (e: Exception) {
                Log.e("MainActivity", "Error in background video preloading", e)
            }
        }.start()
    }

    // Add these methods at the end of your MainActivity class

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            } else {
                // Permission already granted
                initializeFCM()
            }
        } else {
            // For older Android versions, no explicit permission needed
            initializeFCM()
        }
    }

    private fun initializeFCM() {
        FCMHelper.initializeFCM()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "Notification permission granted")
                    initializeFCM()
                } else {
                    Log.d("MainActivity", "Notification permission denied")
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    loginViewModel: LoginViewModel,
    registerViewModel: RegisterViewModel,
    homeViewModel: HomeViewModel,
    categoryViewModel: CategoriesViewModel,
    itemDetailViewModel: ItemDetailViewModel,
    wishlistViewModel: WishlistViewModel,
    jewelryRepository: JewelryRepository,
    activity: ComponentActivity,
    intent: Intent?,
    googleSignInLauncher: ActivityResultLauncher<Intent>,
    profileViewModel: ProfileViewModel, // Add this parameter
    storeInfoViewModel: StoreInfoViewModel, // Add this parameter
    allProductsViewModel: AllProductsViewModel, // Add this parameter
    drawerViewModel: DrawerViewModel, // Add this parameter
    orderHistoryViewModel: OrderHistoryViewModel // Add this parameter
) {
    val navController = rememberNavController()
    
    // Track authentication state properly
    var isAuthenticated by remember { mutableStateOf(false) }
    var isAuthChecked by remember { mutableStateOf(false) }

    // Handle auth check completion from splash screen
    LaunchedEffect(Unit) {
        // Initial auth check
        val user = FirebaseAuth.getInstance().currentUser
        isAuthenticated = user != null
        isAuthChecked = true
        
        Log.d("AppNavigation", "Initial auth check - User: ${user?.uid}, Authenticated: $isAuthenticated")
    }

    // Listen to Firebase Auth state changes for subsequent changes
    LaunchedEffect(Unit) {
        var lastNavigationTime = 0L
        var lastSignOutTime = Long.MAX_VALUE // Initialize to max so cooldown doesn't trigger initially
        val navigationDebounceMs = 2000L // 2 second debounce to prevent rapid navigation
        val signOutCooldownMs = 3000L // 3 second cooldown after sign out before allowing navigation
        
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val currentTime = System.currentTimeMillis()
            val user = auth.currentUser
            val wasAuthenticated = isAuthenticated
            val currentRoute = navController.currentDestination?.route
            
            // CRITICAL: Never navigate if we're on login/register screens
            // This prevents false navigation when user clicks login/signup buttons
            if (currentRoute == "login" || currentRoute == "register") {
                Log.d("AppNavigation", "Blocking navigation - on login/register screen: $currentRoute")
                // Still update state but don't navigate
                isAuthenticated = user != null && user.uid.isNotEmpty()
                return@addAuthStateListener
            }
            
            // Verify user is actually authenticated by checking if user exists and has valid data
            val isActuallyAuthenticated = user != null && user.uid.isNotEmpty()
            
            // Track sign out time when transitioning from authenticated to unauthenticated
            if (wasAuthenticated && !isActuallyAuthenticated) {
                lastSignOutTime = currentTime
                Log.d("AppNavigation", "Sign out detected, starting cooldown period")
            }
            
            // Check if we're in sign out cooldown period (only if we recently signed out)
            val timeSinceSignOut = if (lastSignOutTime != Long.MAX_VALUE) currentTime - lastSignOutTime else Long.MAX_VALUE
            val inCooldownPeriod = timeSinceSignOut < signOutCooldownMs && lastSignOutTime != Long.MAX_VALUE
            
            if (inCooldownPeriod && isActuallyAuthenticated) {
                Log.d("AppNavigation", "Blocking navigation - in sign out cooldown period (${timeSinceSignOut}ms < ${signOutCooldownMs}ms), preventing false login")
                isAuthenticated = false
                return@addAuthStateListener
            }
            
            // Reset cooldown if enough time has passed
            if (timeSinceSignOut >= signOutCooldownMs && lastSignOutTime != Long.MAX_VALUE) {
                lastSignOutTime = Long.MAX_VALUE
                Log.d("AppNavigation", "Sign out cooldown period ended")
            }
            
            Log.d("AppNavigation", "Auth state changed - User: ${user?.uid}, Authenticated: $isActuallyAuthenticated, Was: $wasAuthenticated, CurrentRoute: $currentRoute, AuthChecked: $isAuthChecked, TimeSinceSignOut: ${if (timeSinceSignOut != Long.MAX_VALUE) timeSinceSignOut else "N/A"}ms")
            
            // Only navigate if:
            // 1. Auth check is complete
            // 2. Auth state actually changed
            // 3. We're not already on the target screen
            // 4. We're not on login/register screens (already checked above)
            // 5. Enough time has passed since last navigation (debounce)
            // 6. Not in sign out cooldown period
            if (isAuthChecked && wasAuthenticated != isActuallyAuthenticated && 
                (currentTime - lastNavigationTime) > navigationDebounceMs &&
                !inCooldownPeriod) {
                
                isAuthenticated = isActuallyAuthenticated
                
                if (isActuallyAuthenticated) {
                    // User logged in - only navigate if not already on home
                    if (currentRoute != "home" && currentRoute != "splash") {
                        Log.d("AppNavigation", "Navigating to home - user authenticated")
                        lastNavigationTime = currentTime
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        Log.d("AppNavigation", "Skipping navigation to home - already on $currentRoute")
                    }
                } else {
                    // User logged out - only navigate if not already on welcome
                    if (currentRoute != "welcome" && currentRoute != "splash") {
                        Log.d("AppNavigation", "Navigating to welcome - user signed out")
                        lastNavigationTime = currentTime
                        navController.navigate("welcome") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        Log.d("AppNavigation", "Skipping navigation to welcome - already on $currentRoute")
                    }
                }
            } else {
                // Update state even if not navigating
                isAuthenticated = isActuallyAuthenticated
            }
        }
    }

    LaunchedEffect(intent) {
        intent?.data?.let { uri ->
            when {
                // GitHub Pages: https://yourusername.github.io/gagan-jewellers-links/?product=ID
                uri.host == "humble-coders.github.io" -> {
                    val productId = uri.getQueryParameter("product")
                    if (!productId.isNullOrBlank()) {
                        navController.navigate("itemDetail/$productId") {
                            popUpTo("home")
                        }
                    }
                }
                // Custom scheme backup
                uri.scheme == "gaganjewellers" -> {
                    val productId = uri.lastPathSegment
                    if (!productId.isNullOrBlank()) {
                        navController.navigate("itemDetail/$productId") {
                            popUpTo("home")
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(activity.intent) {
        activity.intent?.data?.let { uri ->
            handleDeepLink(uri, navController)
        }
    }

    // Determine start destination based on auth state
    val startDestination = "splash" // Always start with splash screen

    // Smooth navigation transitions - using slide + fade for better visibility
    fun smoothEnterTransition(): EnterTransition {
        return fadeIn(
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + slideInHorizontally(
            initialOffsetX = { it / 4 }, // Slide from right (1/4 of screen width)
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        )
    }

    fun smoothExitTransition(): ExitTransition {
        return fadeOut(
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + slideOutHorizontally(
            targetOffsetX = { -it / 4 }, // Slide to left
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        )
    }

    fun smoothPopEnterTransition(): EnterTransition {
        return fadeIn(
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + slideInHorizontally(
            initialOffsetX = { -it / 4 }, // Slide from left when going back
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        )
    }

    fun smoothPopExitTransition(): ExitTransition {
        return fadeOut(
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        ) + slideOutHorizontally(
            targetOffsetX = { it / 4 }, // Slide to right when going back
            animationSpec = tween(350, easing = FastOutSlowInEasing)
        )
    }

    NavHost(
        navController = navController, 
        startDestination = startDestination,
        enterTransition = { smoothEnterTransition() },
        exitTransition = { smoothExitTransition() },
        popEnterTransition = { smoothPopEnterTransition() },
        popExitTransition = { smoothPopExitTransition() }
    ) {
        // Splash Screen - handles initial auth check (no transition for splash)
        composable(
            route = "splash",
            enterTransition = { fadeIn(animationSpec = tween(200)) },
            exitTransition = { fadeOut(animationSpec = tween(200)) }
        ) {
            SplashScreen(
                navController = navController,
                onAuthCheckComplete = { isAuthenticated ->
                    if (isAuthenticated) {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("welcome") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }
        
        // Welcome Screen
        composable("welcome") {
            WelcomeScreen(navController = navController)
        }
        
        // Login Screen
        composable("login") {
            LoginScreen(loginViewModel, navController,googleSignInLauncher = googleSignInLauncher)
        }

        // Register Screen
        composable("register") {
            RegisterScreen(registerViewModel, navController,googleSignInLauncher // Pass the launcher
            )
        }

        composable("profile") {
            ProfileScreen(
                viewModel = profileViewModel, // Use existing instance
                navController = navController,
                onSignOut = {
                    // Navigate to splash and clear back stack
                    navController.navigate("splash") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onAccountDeleted = {
                    // Navigate to splash and clear back stack
                    navController.navigate("splash") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // All Products Screen (existing)
        composable("allProducts") {
            AllProductsScreen(
                viewModel = allProductsViewModel,
                navController = navController
            )
        }

// All Products with pre-selected material
        composable(
            route = "allProducts/{materialName}",
            arguments = listOf(
                navArgument("materialName") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val materialName = backStackEntry.arguments?.getString("materialName") ?: ""

            val allProductsViewModel: AllProductsViewModel = viewModel(
                key = "allProducts_material_$materialName"
            ) {
                AllProductsViewModel(
                    repository = jewelryRepository,
                    preSelectedMaterial = materialName
                )
            }

            AllProductsScreen(
                viewModel = allProductsViewModel,
                navController = navController
            )
        }

// All Products with pre-selected category
        composable(
            route = "allProducts/{categoryId}/category",
            arguments = listOf(
                navArgument("categoryId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""

            val allProductsViewModel: AllProductsViewModel = viewModel(
                key = "allProducts_category_$categoryId"
            ) {
                AllProductsViewModel(
                    repository = jewelryRepository,
                    preSelectedCategoryId = categoryId
                )
            }

            AllProductsScreen(
                viewModel = allProductsViewModel,
                navController = navController
            )
        }

        // Carousel Products Screen
        composable(
            route = "carouselProducts/{productIds}/{title}",
            arguments = listOf(
                navArgument("productIds") {
                    type = NavType.StringType
                    nullable = false
                },
                navArgument("title") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val productIdsString = backStackEntry.arguments?.getString("productIds") ?: ""
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: "Collection"
            
            // Decode productIds from comma-separated string
            val productIds = productIdsString.split(",").filter { it.isNotBlank() }
            
            // Decode title (replace underscores back to slashes if needed, or use URL decoding)
            val title = encodedTitle.replace("_", "/")

            val carouselProductsViewModel: CarouselProductsViewModel = viewModel(
                key = "carouselProducts_${productIdsString}_$encodedTitle"
            ) {
                CarouselProductsViewModel(
                    repository = jewelryRepository,
                    productIds = productIds
                )
            }

            CarouselProductsScreen(
                viewModel = carouselProductsViewModel,
                navController = navController,
                carouselTitle = title
            )
        }

        // Home Screen
        // Update HomeScreen call in AppNavigation
        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                navController = navController, // Pass navController for bottom bar and drawer
                onProductClick = { productId ->
                    // Navigate to product detail screen
                    navController.navigate("itemDetail/$productId")
                },
                onCollectionClick = { collectionId ->
                    // Navigate to collection screen
                    navController.navigate("collection/$collectionId")
                },
                onLogout = {
                    // Use ProfileViewModel's signOut method
                    profileViewModel.signOut()
                    navController.navigate("splash") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                profileViewModel = profileViewModel, // Pass ProfileViewModel for profile actions
                drawerViewModel = drawerViewModel
            )
        }

        // Categories Screen
        composable(
            route = "category/{categoryId}",
            arguments = listOf(
                navArgument("categoryId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            CategoryScreenView(
                viewModel = categoryViewModel,
                navController = navController // Pass navController for bottom navigation
            )
        }

        // In your NavHost
        composable("store_info") {
            StoreInfoScreen(
                viewModel = storeInfoViewModel,
                navController = navController,
                onBackClick = {
                    //Navigate to home screen when back is clicked
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }

                }
            )
        }

// Add this new composable route in your NavHost, after the existing "category/{categoryId}" route
        composable(
            route = "categoryProducts/{categoryId}/{categoryName}",
            arguments = listOf(
                navArgument("categoryId") {
                    type = NavType.StringType
                    nullable = false
                },
                navArgument("categoryName") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""

            // Use viewModel() with key to ensure single instance per category
            val categoryProductsViewModel: CategoryProductsViewModel = viewModel(
                key = "categoryProducts_$categoryId"
            ) {
                CategoryProductsViewModel(
                    repository = jewelryRepository,
                    categoryId = categoryId
                )
            }

            CategoryProductsScreen(
                categoryId = categoryId,
                categoryName = categoryName,
                viewModel = categoryProductsViewModel,
                navController = navController
            )
        }

        // Item Detail Screen
        composable(
            route = "itemDetail/{productId}",
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            // Extract the product ID from navigation arguments
            val productId = backStackEntry.arguments?.getString("productId") ?: ""

            // Display the Jewelry Product Screen
            JewelryProductScreen(
                productId = productId,
                viewModel = itemDetailViewModel,
                navController = navController, // Pass navController for bottom navigation
                onBackClick = {
                    // Navigate back to the previous screen with animation
                    // Using popBackStack() which respects the pop transitions
                    if (!navController.popBackStack()) {
                        // If popBackStack returns false, navigate to home as fallback
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                        }
                    }
                },
                onShareClick = {

                },
                onProductClick = { selectedProductId ->
                    // Navigate to the selected product's detail screen
                    navController.navigate("itemDetail/$selectedProductId") {
                        // Pop up to the current product detail to avoid stacking multiple details
                        popUpTo("itemDetail/$productId") {
                            inclusive = true
                        }
                    }
                }
            )
        }

        // Collection Screen - Placeholder for future implementation
        composable(
            route = "collection/{collectionId}",
            arguments = listOf(
                navArgument("collectionId") {
                    type = NavType.StringType
                    nullable = false
                }
            )
        ) { backStackEntry ->
            // For now, we'll redirect to categories screen as a placeholder
            CategoryScreenView(
                viewModel = categoryViewModel,
                navController = navController // Pass navController for bottom navigation
            )
        }

        // Wishlist Screen
        composable("wishlist") {
            WishlistScreen(
                viewModel = wishlistViewModel,
                navController = navController // Pass navController for bottom navigation
            )
        }

        // Order History Screen
        composable("orderHistory") {
            OrderHistoryScreen(
                viewModel = orderHistoryViewModel,
                navController = navController
            )
        }

        // Video Consultation Screens
        composable("videoConsultation") {
            // Create repository and viewmodel scoped to this destination
            val repo = VideoBookingRepository(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance())
            val vm: VideoBookingViewModel = viewModel(key = "videoBookingVM") {
                VideoBookingViewModel(repo)
            }
            VideoConsultationScreen(navController = navController, viewModel = vm)
        }

        composable("myBookings") {
            val repo = VideoBookingRepository(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance())
            val vm: VideoBookingViewModel = viewModel(key = "videoBookingVM") {
                VideoBookingViewModel(repo)
            }
            MyBookingsScreen(navController = navController, viewModel = vm)
        }

        composable("consultation_history") {
            val repo = VideoBookingRepository(FirebaseFirestore.getInstance(), FirebaseAuth.getInstance())
            val vm: VideoBookingViewModel = viewModel(key = "videoBookingVM") {
                VideoBookingViewModel(repo)
            }
            ConsultationHistoryScreen(navController = navController, viewModel = vm)
        }

        // Profile Screen placeholder - this would be implemented in the future

    }


}

private fun handleDeepLink(uri: Uri, navController: NavController) {
    when {
        // Handle Dynamic Links: https://gaganjewellers.page.link/xxx
        uri.host == "gaganjewellers.page.link" -> {
            // Extract product ID from query parameter
            val productId = uri.getQueryParameter("productId")
            if (!productId.isNullOrBlank()) {
                navController.navigate("itemDetail/$productId") {
                    popUpTo("home")
                }
            }
        }
        // Handle custom scheme: gaganjewellers://product/ID (backup)
        uri.scheme == "gaganjewellers" -> {
            val productId = uri.lastPathSegment
            if (!productId.isNullOrBlank()) {
                navController.navigate("itemDetail/$productId") {
                    popUpTo("home")
                }
            }
        }
    }

}
