package com.example.jewelleryapp

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jewelleryapp.repository.FirebaseAuthRepository
import com.example.jewelleryapp.repository.JewelryRepository
import com.example.jewelleryapp.repository.ProfileRepository
import com.example.jewelleryapp.screen.allProducts.AllProductsViewModel
import com.example.jewelleryapp.screen.categoriesScreen.CategoriesViewModel
import com.example.jewelleryapp.screen.categoriesScreen.CategoryScreenView
import com.example.jewelleryapp.screen.categoryProducts.CategoryProductsScreen
import com.example.jewelleryapp.screen.categoryProducts.CategoryProductsViewModel
import com.example.jewelleryapp.screen.homeScreen.DrawerViewModel
import com.example.jewelleryapp.screen.homeScreen.HomeScreen
import com.example.jewelleryapp.screen.homeScreen.HomeViewModel
import com.example.jewelleryapp.screen.productDetailScreen.ItemDetailViewModel
import com.example.jewelleryapp.screen.productDetailScreen.JewelryProductScreen
import com.example.jewelleryapp.screen.loginScreen.LoginScreen
import com.example.jewelleryapp.screen.loginScreen.LoginViewModel
import com.example.jewelleryapp.screen.profileScreen.ProfileScreen
import com.example.jewelleryapp.screen.profileScreen.ProfileViewModel
import com.example.jewelleryapp.screen.registerScreen.RegisterScreen
import com.example.jewelleryapp.screen.registerScreen.RegisterViewModel
import com.example.jewelleryapp.screen.homeScreen.StoreInfoViewModel
import com.example.jewelleryapp.screen.wishlist.WishlistScreen
import com.example.jewelleryapp.screen.wishlist.WishlistViewModel
import com.example.jewelleryapp.screen.welcomeScreen.WelcomeScreen
import com.example.jewelleryapp.ui.theme.JewelleryAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.jewelleryapp.screen.allProducts.AllProductsScreen
import com.example.jewelleryapp.screen.homeScreen.StoreInfoScreen
import com.example.jewelleryapp.utils.VideoCacheManager

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







    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

            // Update repository with new user ID when auth state changes
            if (newUserId != userId) {
                Log.d("MainActivity", "Auth changed, updating user ID to: $newUserId")
                jewelryRepository.updateUserId(newUserId)

                // If repositories were already passed to ViewModels, we need to refresh them
                if (::wishlistViewModel.isInitialized) {
                    wishlistViewModel.refreshWishlistItems()
                }
                if (::homeViewModel.isInitialized) {
                    homeViewModel.refreshData()
                }
                // Refresh other ViewModels as needed
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

        // Start background video preloading
        startBackgroundVideoPreloading(jewelryRepository)

        enableEdgeToEdge()
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
                        drawerViewModel
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
    drawerViewModel: DrawerViewModel // Add this parameter

) {
    val navController = rememberNavController()

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

    // Check if user is already logged in
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        "home"
    } else {
        "welcome"
    }

    NavHost(navController = navController, startDestination = startDestination) {
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
                    // Navigate to welcome and clear back stack
                    navController.navigate("welcome") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onAccountDeleted = {
                    // Navigate to welcome and clear back stack
                    navController.navigate("welcome") {
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
                    navController.navigate("welcome") {
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
                    // Navigate back to the previous screen
                    navController.popBackStack()
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
