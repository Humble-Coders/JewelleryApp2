package com.example.jewelleryapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.jewelleryapp.repository.FirebaseAuthRepository
import com.example.jewelleryapp.repository.JewelryRepository
import com.example.jewelleryapp.screen.categoriesScreen.CategoriesViewModel
import com.example.jewelleryapp.screen.categoriesScreen.CategoryScreenView
import com.example.jewelleryapp.screen.categoryProducts.CategoryProductsScreen
import com.example.jewelleryapp.screen.categoryProducts.CategoryProductsViewModel
import com.example.jewelleryapp.screen.homeScreen.HomeScreen
import com.example.jewelleryapp.screen.homeScreen.HomeViewModel
import com.example.jewelleryapp.screen.itemdetailScreen.ItemDetailViewModel
import com.example.jewelleryapp.screen.itemdetailScreen.JewelryProductScreen
import com.example.jewelleryapp.screen.loginScreen.LoginScreen
import com.example.jewelleryapp.screen.loginScreen.LoginViewModel
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
    private lateinit var jewelryRepository: JewelryRepository // Make this a class property




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
         firebaseAuth = FirebaseAuth.getInstance()

        // Initialize Firestore
        val firestore = FirebaseFirestore.getInstance()

        // Initialize Repositories
        val authRepository = FirebaseAuthRepository(firebaseAuth)
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
                        jewelryRepository
                    )
                }
            }
        }
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
    jewelryRepository: JewelryRepository
) {
    val navController = rememberNavController()

    // Check if user is already logged in
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        "home"
    } else {
        "login"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        // Login Screen
        composable("login") {
            LoginScreen(loginViewModel, navController)
        }

        // Register Screen
        composable("register") {
            RegisterScreen(registerViewModel, navController)
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
                }
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

                    // Create CategoryProductsViewModel with the categoryId
                    val categoryProductsViewModel = CategoryProductsViewModel(
                        repository = jewelryRepository,
                        categoryId = categoryId
                    )

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
                    // Handle share functionality (will be implemented later)
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
        composable("profile") {
            // For now, redirect to home since profile isn't implemented
            LaunchedEffect(Unit) {
                navController.navigate("home")
            }
        }
    }
}