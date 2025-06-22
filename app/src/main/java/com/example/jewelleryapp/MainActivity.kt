package com.example.jewelleryapp

import android.app.Activity
import android.content.Intent
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
import com.example.jewelleryapp.screen.categoriesScreen.CategoriesViewModel
import com.example.jewelleryapp.screen.categoriesScreen.CategoryScreenView
import com.example.jewelleryapp.screen.categoryProducts.CategoryProductsScreen
import com.example.jewelleryapp.screen.categoryProducts.CategoryProductsViewModel
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
import com.example.jewelleryapp.screen.wishlist.WishlistScreen
import com.example.jewelleryapp.screen.wishlist.WishlistViewModel
import com.example.jewelleryapp.ui.theme.JewelleryAppTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
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







    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Update the launcher in MainActivity
        // Update the launcher in MainActivity to handle both login and register
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.d("MainActivity", "Google Sign-In result: ${result.resultCode}")

            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    Log.d("MainActivity", "Google Sign-In OK, processing result")
                    // Handle for both login and register ViewModels
                    loginViewModel.handleGoogleSignInResult(result.data)
                    registerViewModel.handleGoogleSignInResult(result.data)
                }
                Activity.RESULT_CANCELED -> {
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



        enableEdgeToEdge()
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
                        profileViewModel
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
    profileViewModel: ProfileViewModel // Add this parameter

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
        "login"
    }

    NavHost(navController = navController, startDestination = startDestination) {
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
                    // Navigate to login and clear back stack
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onAccountDeleted = {
                    // Navigate to login and clear back stack
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Home Screen
        // Update HomeScreen call in AppNavigation
        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                navController = navController, // Pass navController for bottom bar and drawer
                onCategoryClick = { categoryId ->
                    // Navigate to category detail screen
                    navController.navigate("category/$categoryId")
                },
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
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                profileViewModel = profileViewModel // Pass ProfileViewModel for profile actions
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