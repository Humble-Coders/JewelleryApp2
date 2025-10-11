package com.example.jewelleryapp.screen.homeScreen

import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jewelleryapp.R
import com.example.jewelleryapp.model.Category
import com.example.jewelleryapp.repository.ProfileRepository
import com.example.jewelleryapp.screen.profileScreen.ProfileViewModel
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import com.example.jewelleryapp.model.CarouselItem as CarouselItemModel
import com.example.jewelleryapp.model.Category as CategoryModel
import com.example.jewelleryapp.model.Collection as CollectionModel
import com.example.jewelleryapp.model.Product as ProductModel
import com.example.jewelleryapp.screen.homeScreen.VideoSection
import com.example.jewelleryapp.screen.homeScreen.CustomerTestimonialsWithCurvedString
import com.example.jewelleryapp.screen.homeScreen.Testimonial
import com.example.jewelleryapp.screen.homeScreen.ExactPatternJewelryGrid
import com.example.jewelleryapp.screen.homeScreen.JewelryItem


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onProductClick: (String) -> Unit = {},
    onCollectionClick: (String) -> Unit = {},
    navController: NavController,
    onLogout: () -> Unit,
    profileViewModel: ProfileViewModel,
    drawerViewModel: DrawerViewModel
) {
    // Collect existing state flows
    val categories by viewModel.categories.collectAsState()
    val featuredProducts by viewModel.featuredProducts.collectAsState()
    val collections by viewModel.collections.collectAsState()
    val carouselItems by viewModel.carouselItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // Add these new state collections
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredCategories by viewModel.filteredCategories.collectAsState()

    val recentlyViewedProducts by viewModel.recentlyViewedProducts.collectAsState()
    val isRecentlyViewedLoading by viewModel.isRecentlyViewedLoading.collectAsState()

    // Video state
    val video by viewModel.video.collectAsState()
    val isVideoLoading by viewModel.isVideoLoading.collectAsState()
    
    // Hardcoded testimonials data
    val testimonials = listOf(
        Testimonial(
            name = "Akanksha Khanna",
            age = 27,
            imageRes = R.drawable.goldbracelet_homescreen,
            text = "Obsessed with my engagement ring, my husband chose perfectly and it's everything I wanted in a ring. Handcrafted with love!",
            rotation = -6f,
            clipPosition = 0.3f,
            stringDepth = 0.25f
        ),
        Testimonial(
            name = "Nutan Mishra",
            age = 33,
            imageRes = R.drawable.goldbracelet_homescreen,
            text = "I got a necklace for my baby boy from this brand and it's so beautiful! It gave me happiness and security knowing it's pure.",
            rotation = 4f,
            clipPosition = 0.7f,
            stringDepth = 0.35f
        ),
        Testimonial(
            name = "Sarah Johnson",
            age = 28,
            imageRes = R.drawable.goldbracelet_homescreen,
            text = "Amazing quality and beautiful designs. The customer service was exceptional and I couldn't be happier!",
            rotation = -2f,
            clipPosition = 0.4f,
            stringDepth = 0.2f
        ),
        Testimonial(
            name = "Priya Sharma",
            age = 25,
            imageRes = R.drawable.goldbracelet_homescreen,
            text = "The jewelry is stunning and exactly what I was looking for. Fast delivery and beautiful packaging too!",
            rotation = 5f,
            clipPosition = 0.6f,
            stringDepth = 0.4f
        )
    )

    // Hardcoded jewelry items data for the grid
    val jewelryGridItems = listOf(
        JewelryItem(R.drawable.goldbracelet_homescreen, isLarge = true),
        JewelryItem(R.drawable.necklace_homescreen),
        JewelryItem(R.drawable.goldbracelet_homescreen),
        JewelryItem(R.drawable.necklace_homescreen),
        JewelryItem(R.drawable.goldbracelet_homescreen),
        JewelryItem(R.drawable.necklace_homescreen),
        JewelryItem(R.drawable.goldbracelet_homescreen),
        JewelryItem(R.drawable.necklace_homescreen, isLarge = true),
        JewelryItem(R.drawable.goldbracelet_homescreen),
        JewelryItem(R.drawable.necklace_homescreen),
        JewelryItem(R.drawable.goldbracelet_homescreen),
        JewelryItem(R.drawable.necklace_homescreen)
    )

    // Key fix: Use a stable key for drawer state to prevent recreation
    val currentRoute = navController.currentDestination?.route
    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    LaunchedEffect(navController.currentDestination?.route) {
        if (navController.currentDestination?.route == "home") {
            viewModel.refreshRecentlyViewed()
        }
    }

    LaunchedEffect(Unit) {
        profileViewModel.loadUserProfile()
    }

    // Create a stable coroutine scope
    val scope = rememberCoroutineScope()

    // Dispose drawer state properly when leaving home screen
    DisposableEffect(currentRoute) {
        onDispose {
            // Clean up drawer state when leaving
            if (drawerState.isOpen) {
                try {
                    // Don't use coroutine here, just reset the state
                } catch (e: Exception) {
                    Log.e("HomeScreen", "Error disposing drawer", e)
                }
            }
        }
    }

    // Force close drawer when returning to home from other screens
    LaunchedEffect(currentRoute) {
        if (currentRoute == "home" && drawerState.isOpen) {
            try {
                drawerState.close()
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error closing drawer on route change", e)
            }
        }
    }

    // Create stable drawer content to prevent recomposition issues
    val drawerContent = remember(navController) {
        @Composable {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.75f),
                drawerContainerColor = Color.White
            ) {
                DrawerContent(
                    navController = navController,
                    onCloseDrawer = {
                        scope.launch {
                            try {
                                drawerState.close()
                            } catch (e: Exception) {
                                Log.e("HomeScreen", "Error closing drawer", e)
                            }
                        }
                    },
                    onLogout = {
                        onLogout()
                    },
                    homeViewModel = viewModel,
                    drawerViewModel = drawerViewModel
                )
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = drawerContent,
        gesturesEnabled = currentRoute == "home"
    ) {
        Scaffold(
            topBar = {
                TopAppbar(
                    title = "    Gagan Jewellers",
                    onMenuClick = {
                        if (currentRoute == "home") {
                            scope.launch {
                                try {
                                    if (drawerState.isClosed) {
                                        drawerState.open()
                                    } else {
                                        drawerState.close()
                                    }
                                } catch (e: Exception) {
                                    Log.e("HomeScreen", "Drawer toggle failed", e)
                                }
                            }
                        }
                    },
                    isSearchActive = isSearchActive,
                    searchQuery = searchQuery,
                    onSearchToggle = { viewModel.toggleSearch() },
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) },
                    onWishlistClick = {
                        navController.navigate("wishlist")
                    },
                )
            },
            bottomBar = { BottomNavigationBar(navController = navController) },
            containerColor = Color.White
        ) { paddingValues ->
            // Main content without outer Box
            if (error != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Something went wrong",
                            color = Color.Red,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.refreshData() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF896C6C)
                            )
                        ) {
                            Text("Try Again")
                        }
                    }
                }
            } else {
                var isRefreshing by remember { mutableStateOf(false) }

                // Handle refresh
                LaunchedEffect(isLoading) {
                    if (isLoading) {
                        isRefreshing = true
                    } else {
                        isRefreshing = false
                    }
                }

                // SwipeRefresh directly in Scaffold content
                SwipeRefresh(
                    state = rememberSwipeRefreshState(isRefreshing),
                    onRefresh = {
                        viewModel.refreshData()
                    },
                    modifier = Modifier.fillMaxSize(),
                    indicator = { state, refreshTrigger ->
                        SwipeRefreshIndicator(
                            state = state,
                            refreshTriggerDistance = refreshTrigger,
                            backgroundColor = Color(0xFF896C6C),
                            contentColor = Color.White
                        )
                    }
                ) {
                    Box {
                        // In your HomeScreen LazyColumn, add these optimizations:
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            verticalArrangement = Arrangement.spacedBy(0.dp),
                            // Add this for better performance
                            userScrollEnabled = true
                        ) {
                            // Gradient header with bangles image and promotional text
                            item(key = "gradient_header") {
                                GradientHeaderWithBangles(
                                    categories = categories,
                                    onCategoryClick = { categoryId ->
                                        Log.d("HomeScreen", "Category clicked: $categoryId")
                                        val categoryName = categories.find { it.id == categoryId }?.name ?: "Products"
                                        navController.navigate("categoryProducts/$categoryId/$categoryName")
                                    }
                                )
                            }

                            // Add stable keys for better performance
                            item(key = "categories") {
                                if (categories.isNotEmpty()) {
                                    CategoryRow(categories, onCategoryClick = { categoryId ->
                                        Log.d("HomeScreen", "Category clicked: $categoryId")
                                        val categoryName = categories.find { it.id == categoryId }?.name ?: "Products"
                                        navController.navigate("categoryProducts/$categoryId/$categoryName")
                                    })
                                } else {
                                    ShimmerCategoryPlaceholder()
                                }
                            }

                            item(key = "recently_viewed") {
                                if (recentlyViewedProducts.isNotEmpty() || isRecentlyViewedLoading) {
                                    RecentlyViewedSection(
                                        products = recentlyViewedProducts,
                                        isLoading = isRecentlyViewedLoading,
                                        onProductClick = { productId ->
                                            viewModel.onRecentlyViewedProductClick(productId)
                                            onProductClick(productId)
                                        },
                                        onFavoriteClick = { productId ->
                                            viewModel.toggleRecentlyViewedFavorite(productId)
                                        }
                                    )
                                }
                            }

                            item(key = "video_section") {
                                VideoSection(
                                    video = video,
                                    isLoading = isVideoLoading
                                )
                            }
                            item{
                                Spacer(modifier = Modifier.height(32.dp))
                            }

                            item(key = "carousel") {
                                if (carouselItems.isNotEmpty()) {
                                    ElegantCarouselSection(carouselItems)
                                } else {
                                    ShimmerCarouselPlaceholder()
                                }
                            }

                            item(key = "featured_title") {
                                FeaturedProductsTitle()
                            }

                            // Use itemsIndexed with keys for featured products
                            itemsIndexed(
                                items = featuredProducts.chunked(2),
                                key = { index, productPair ->
                                    "products_${productPair.joinToString("_") { it.id }}"
                                }
                            ) { index, productPair ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    productPair.forEach { product ->
                                        FeaturedProductCard(
                                            product = product,
                                            onProductClick = onProductClick,
                                            viewModel = viewModel,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    if (productPair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }

                            item(key = "collections") {
                                if (collections.isNotEmpty()) {
                                    ThemedCollectionsSection(collections, onCollectionClick)
                                } else {
                                    ShimmerCollectionsPlaceholder()
                                }
                            }

                            item(key = "customer_testimonials") {
                                CustomerTestimonialsWithCurvedString(
                                    testimonials = testimonials,
                                    clipDrawableRes = R.drawable.paper_clip
                                )
                            }

                            item(key = "jewelry_grid") {
                                ExactPatternJewelryGrid(
                                    items = jewelryGridItems,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            item(key = "bottom_spacer") {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        // Search results overlay
                        CategorySearchResults(
                            categories = filteredCategories,
                            isVisible = isSearchActive,
                            onCategoryClick = { categoryId ->
                                viewModel.toggleSearch()
                                val categoryName = categories.find { it.id == categoryId }?.name ?: "Products"
                                navController.navigate("categoryProducts/$categoryId/$categoryName")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(paddingValues)
                        )
                    }
                }
            }
        }
    }

    // Rates dialog
    val showRatesDialog by viewModel.showRatesDialog.collectAsState()
    val goldSilverRates by viewModel.goldSilverRates.collectAsState()
    val isRatesLoading by viewModel.isRatesLoading.collectAsState()

    if (showRatesDialog) {
        RatesDialog(
            rates = goldSilverRates,
            isLoading = isRatesLoading,
            onDismiss = { viewModel.hideRatesDialog() }
        )
    }
}

// In HomeScreen.kt - UPDATE StableDrawerContent function

// Replace your existing DrawerContent composable in HomeScreen.kt with this updated version

@Composable
fun DrawerContent(
    navController: NavController,
    onCloseDrawer: () -> Unit,
    onLogout: () -> Unit,
    homeViewModel: HomeViewModel,
    drawerViewModel: DrawerViewModel // Add this parameter
) {
    val amberColor = Color(0xFF896C6C)
    val context = LocalContext.current

    // Collect drawer state
    val materials by drawerViewModel.materials.collectAsState()
    val categories by drawerViewModel.categories.collectAsState()
    val isMetalExpanded by drawerViewModel.isMetalExpanded.collectAsState()
    val isCollectionsExpanded by drawerViewModel.isCollectionsExpanded.collectAsState()

    // Get current user and profile data
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val userName = remember(currentUser) { currentUser?.displayName ?: "Guest" }

    // Profile image state (existing code)
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var localImagePath by remember { mutableStateOf<String?>(null) }

    // Existing profile loading code...
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            try {
                val profileDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                if (profileDoc.exists()) {
                    profileImageUrl = profileDoc.getString("profilePictureUrl")
                    val profileRepository = ProfileRepository(
                        FirebaseAuth.getInstance(),
                        FirebaseFirestore.getInstance(),
                        context
                    )
                    val userProfile = profileRepository.getCurrentUserProfile()
                    userProfile.fold(
                        onSuccess = { profile ->
                            localImagePath = profile.localImagePath
                        },
                        onFailure = { /* Handle error silently */ }
                    )
                }
            } catch (e: Exception) {
                Log.e("DrawerContent", "Error loading profile data", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // Make drawer scrollable
    ) {
        // Profile header (existing code)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(amberColor, CircleShape)
                    .padding(3.dp)
            ) {
                when {
                    !localImagePath.isNullOrEmpty() -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(File(localImagePath))
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.ic_launcher_background)
                        )
                    }
                    !profileImageUrl.isNullOrEmpty() -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(profileImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.ic_launcher_background)
                        )
                    }
                    else -> {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_background),
                            contentDescription = "Default Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Hi $userName!",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))

        // Navigation items
        DrawerItem(
            icon = Icons.Outlined.Person,
            text = "My Profile",
            onClick = {
                onCloseDrawer()
                navController.navigate("profile")
            }
        )

//        DrawerItem(
//            icon = Icons.Outlined.History,
//            text = "Order History",
//            onClick = { onCloseDrawer() }
//        )

        Text(
            text = "Shop By",
            color = amberColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        DrawerItem(
            text = "All Jewellery",
            onClick = {
                onCloseDrawer()
                navController.navigate("allProducts") {
                    popUpTo("home")
                }
            }
        )

        // Expandable Metal Section
        ExpandableDrawerItem(
            text = "Metal",
            isExpanded = isMetalExpanded,
            onToggle = { drawerViewModel.toggleMetalExpansion() }
        )

        // Metal items (expanded)
        if (isMetalExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp) // Fixed height with scrolling
                    .padding(start = 16.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(materials) { material ->
                        SubDrawerItem(
                            text = material.name,
                            onClick = {
                                onCloseDrawer()
                                // Navigate to allProducts with pre-selected material filter
                                navController.navigate("allProducts/${material.name}") {
                                    popUpTo("home")
                                }
                            }
                        )
                    }
                }
            }
        }

        // Expandable Collections Section
        ExpandableDrawerItem(
            text = "Collections",
            isExpanded = isCollectionsExpanded,
            onToggle = { drawerViewModel.toggleCollectionsExpansion() }
        )

        // Collections items (expanded)
        if (isCollectionsExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 120.dp) // Fixed height with scrolling
                    .padding(start = 16.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categories) { category ->
                        SubDrawerItem(
                            text = category.name,
                            onClick = {
                                onCloseDrawer()
                                // Navigate to allProducts with pre-selected category
                                navController.navigate("allProducts/${category.id}/category") {
                                    popUpTo("home")
                                }
                            }
                        )
                    }
                }
            }
        }

        Text(
            text = "More",
            color = amberColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // Existing more items...
        DrawerItem(
            icon = Icons.Outlined.LocationOn,
            text = "Store Info",
            onClick = {
                onCloseDrawer()
                navController.navigate("store_info")
            }
        )

        DrawerItem(
            icon = Icons.Outlined.Headset,
            text = "Get In Touch",
            onClick = {
                onCloseDrawer()
                homeViewModel.openWhatsApp(context)
            }
        )

        DrawerItem(
            icon = Icons.Outlined.LocationOn,
            text = "Store Locator",
            onClick = {
                onCloseDrawer()
                homeViewModel.openGoogleMaps(context)
            }
        )

        DrawerItem(
            icon = Icons.AutoMirrored.Outlined.ExitToApp,
            text = "Logout",
            onClick = {
                onCloseDrawer()
                onLogout()
            }
        )

        // Existing rates section...
        LaunchedEffect(Unit) {
            homeViewModel.loadGoldSilverRates()
        }

        val goldSilverRates by homeViewModel.goldSilverRates.collectAsState()
        val isRatesLoading by homeViewModel.isRatesLoading.collectAsState()

        RatesDrawerItem(
            goldRate = goldSilverRates?.goldRatePerGram,
            silverRate = goldSilverRates?.silverRatePerGram,
            isLoading = isRatesLoading,
            onClick = {
                onCloseDrawer()
                homeViewModel.showRatesDialog()
            }
        )
    }
}

// Add these new composables

@Composable
private fun ExpandableDrawerItem(
    text: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
            color = Color.Black
        )
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (isExpanded) "Collapse" else "Expand",
            tint = Color.Gray
        )
    }
}

@Composable
private fun SubDrawerItem(
    text: String,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontSize = 16.sp,
        color = Color.DarkGray,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 16.dp)
    )
}

@Composable
fun DrawerItem(
    text: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    // Stable click handler
    val stableOnClick = remember(onClick) { onClick }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { stableOnClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = Color.DarkGray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Text(
            text = text,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
            color = Color.Black
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = "Arrow",
            tint = Color.Gray
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppbar(
    title: String,
    onMenuClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    onSearchToggle: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onWishlistClick: () -> Unit = {}
) {
    val amberColor = Color(0xFF896C6C)
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val view = LocalView.current

    // Set status bar color to C59E9E
    SideEffect {
        val window = (view.context as ComponentActivity).window
        window.statusBarColor = Color(0xFFC59E9E).toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
    }

    // Voice search states
    var isVoiceSearchActive by remember { mutableStateOf(false) }
    var voiceSearchError by remember { mutableStateOf<String?>(null) }
    var voiceState by remember { mutableStateOf("idle") }

    // Create stable callback references
    val onVoiceResult = remember {
        { result: String ->
            onSearchQueryChange(result)
            isVoiceSearchActive = false
            voiceSearchError = null
            voiceState = "idle"
        }
    }

    val onVoiceError = remember {
        { error: String ->
            voiceSearchError = error
            isVoiceSearchActive = false
            voiceState = "idle"
        }
    }

    val onVoiceReady = remember { { voiceState = "ready" } }
    val onVoiceBeginning = remember { { voiceState = "speaking"; voiceSearchError = null } }
    val onVoiceEnd = remember { { voiceState = "processing" } }

    // Voice search manager
    val voiceSearchManager = remember(onVoiceResult, onVoiceError, onVoiceReady, onVoiceBeginning, onVoiceEnd) {
        VoiceSearchManager(
            context = context,
            onResult = onVoiceResult,
            onError = onVoiceError,
            onReadyForSpeech = onVoiceReady,
            onBeginningOfSpeech = onVoiceBeginning,
            onEndOfSpeech = onVoiceEnd
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isVoiceSearchActive = true
            voiceSearchError = null
            voiceState = "idle"
            voiceSearchManager.startListening()
        } else {
            voiceSearchError = "Microphone permission required"
        }
    }

    val requestVoiceSearchPermission = remember {
        {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            )

            when (permissionStatus) {
                PackageManager.PERMISSION_GRANTED -> {
                    isVoiceSearchActive = true
                    voiceSearchError = null
                    voiceState = "idle"
                    voiceSearchManager.startListening()
                }
                PackageManager.PERMISSION_DENIED -> {
                    permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    val normalHeight = 50.dp
    val expandedHeight = 100.dp

    val currentHeight by animateDpAsState(
        targetValue = if (isSearchActive) expandedHeight else normalHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "topbar_height"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .systemBarsPadding()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(currentHeight),
            color = Color(0xFFC59E9E),
            shadowElevation = 0.dp
        ) {
            Column {
                // Main app bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(normalHeight)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Menu/Back button
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = Color(0xFFEEDDCA).copy(alpha = 0.2f),
                        onClick = onBackClick ?: onMenuClick
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (onBackClick != null) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
                                contentDescription = if (onBackClick != null) "Back" else "Menu",
                                tint = Color(0xFFEEDDCA),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Title
                    AnimatedVisibility(
                        visible = !isSearchActive,
                        enter = fadeIn(animationSpec = tween(200)),
                        exit = fadeOut(animationSpec = tween(150))
                    ) {
                        Text(
                            text = title,
                            color = Color(0xFFEEDDCA),
                            fontWeight = FontWeight.Normal,
                            fontSize = 18.sp
                        )
                    }

                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search button
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = if (isSearchActive) Color(0xFFEEDDCA).copy(alpha = 0.3f) else Color(0xFFEEDDCA).copy(alpha = 0.2f),
                            onClick = {
                                onSearchToggle()
                                if (!isSearchActive) {
                                    if (isVoiceSearchActive) {
                                        voiceSearchManager.stopListening()
                                        isVoiceSearchActive = false
                                        voiceState = "idle"
                                    }
                                }
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = if (isSearchActive) "Close Search" else "Search",
                                    tint = Color(0xFFEEDDCA),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Wishlist button
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = Color(0xFFEEDDCA).copy(alpha = 0.2f),
                            onClick = onWishlistClick
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FavoriteBorder,
                                    contentDescription = "Wishlist",
                                    tint = Color(0xFFEEDDCA),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Search bar section
                AnimatedVisibility(
                    visible = isSearchActive,
                    enter = slideInVertically(
                        initialOffsetY = { -it / 2 },
                        animationSpec = tween(250)
                    ) + fadeIn(animationSpec = tween(250)),
                    exit = slideOutVertically(
                        targetOffsetY = { -it / 2 },
                        animationSpec = tween(200)
                    ) + fadeOut(animationSpec = tween(200))
                ) {
                    // Search input
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = amberColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Fixed TextField - Removed the Box wrapper
                            BasicTextField(
                                value = if (isVoiceSearchActive && voiceState == "speaking") "Listening..." else searchQuery,
                                onValueChange = { if (!isVoiceSearchActive) onSearchQueryChange(it) },
                                modifier = Modifier.weight(1f),
                                enabled = !isVoiceSearchActive,
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 16.sp,
                                    color = if (isVoiceSearchActive && voiceState == "speaking") amberColor else Color.Black,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(amberColor),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { focusManager.clearFocus() }
                                ),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty() && !isVoiceSearchActive) {
                                        Text(
                                            text = "Search categories...",
                                            color = Color.Gray.copy(alpha = 0.7f),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    } else if (isVoiceSearchActive) {
                                        Text(
                                            text = when (voiceState) {
                                                "ready" -> "Ready to speak..."
                                                "processing" -> "Processing..."
                                                "speaking" -> "Listening..."
                                                else -> "Search categories..."
                                            },
                                            color = amberColor.copy(alpha = 0.8f),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            // Voice search button
                            IconButton(
                                onClick = {
                                    if (isVoiceSearchActive) {
                                        voiceSearchManager.stopListening()
                                        isVoiceSearchActive = false
                                        voiceState = "idle"
                                    } else {
                                        requestVoiceSearchPermission()
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                when {
                                    isVoiceSearchActive && voiceState == "speaking" -> {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = "Stop",
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    isVoiceSearchActive -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = amberColor
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Voice Search",
                                            tint = amberColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Clear button
                            AnimatedVisibility(
                                visible = searchQuery.isNotEmpty() && !isVoiceSearchActive,
                                enter = scaleIn() + fadeIn(),
                                exit = scaleOut() + fadeOut()
                            ) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// Updated CategorySearchResults to handle proper positioning
@Composable
fun CategorySearchResults(
    categories: List<Category>,
    isVisible: Boolean,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)) +
                slideInVertically(
                    initialOffsetY = { -20 },
                    animationSpec = tween(250)
                ),
        exit = fadeOut(animationSpec = tween(150)) +
                slideOutVertically(
                    targetOffsetY = { -20 },
                    animationSpec = tween(200)
                ),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp), // Limit max height
                verticalArrangement = Arrangement.spacedBy(1.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(categories) { category ->
                    CategorySearchItem(
                        category = category,
                        onClick = { onCategoryClick(category.id) }
                    )
                }

                if (categories.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "No results",
                                    tint = Color.Gray.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No categories found",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Updated CategorySearchItem for better visibility
@Composable
fun CategorySearchItem(
    category: Category,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(category.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = category.name,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.necklace_homescreen)
            )

            Text(
                text = category.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Go to category",
                tint = Color(0xFF896C6C),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CategoryRow(categories: List<Category>, onCategoryClick: (String) -> Unit) {
    if (categories.isEmpty()) return

    val listState = rememberLazyListState()
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val infiniteCategories = remember(categories) { categories + categories + categories }

    // Start at middle section for seamless infinite scroll
    LaunchedEffect(categories.size) {
        if (categories.size > 1) {
            listState.scrollToItem(categories.size)
        }
    }

    // Simplified scroll detection
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            lastInteractionTime = System.currentTimeMillis()
        }
    }

    // Optimized auto-scroll with less frequent updates
    LaunchedEffect(categories.size) {
        if (categories.size > 1) {
            while (true) {
                delay(1000) // Check every second instead of 500ms
                val timeSinceLastInteraction = System.currentTimeMillis() - lastInteractionTime

                if (timeSinceLastInteraction > 4000 && !listState.isScrollInProgress) { // Increased delay
                    try {
                        val itemWidth = 84f
                        listState.animateScrollBy(
                            value = itemWidth * categories.size,
                            animationSpec = tween(
                                durationMillis = categories.size * 6000, // Slower animation
                                easing = LinearEasing
                            )
                        )

                        val newIndex = listState.firstVisibleItemIndex
                        if (newIndex >= categories.size * 2) {
                            listState.scrollToItem(categories.size)
                        }

                        lastInteractionTime = System.currentTimeMillis()
                    } catch (e: Exception) {
                        delay(2000) // Longer delay on error
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(top = 8.dp, bottom = 8.dp,start=2.dp,end=2.dp)
    ) {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            userScrollEnabled = true
        ) {
            items(
                items = infiniteCategories,
                key = { category -> "${category.id}_${infiniteCategories.indexOf(category)}" } // Add stable keys
            ) { category ->
                CategoryItem(category, onCategoryClick)
            }
        }
    }
}

// Individual category item
@Composable
fun CategoryItem(category: CategoryModel, onCategoryClick: (String) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onCategoryClick(category.id) }
    ) {
        // Use AsyncImage with Coil to load from URL
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(category.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = category.name,
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.necklace_homescreen) // Placeholder from resources
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = category.name,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

// Featured products section
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal
    )
}




// Removed Recently Viewed section as requested

@Composable
fun BottomNavigationBar(navController: NavController) {
    val amberColor = Color(0xFF896C6C)
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val hapticManager = remember { HapticFeedbackManager(context) }

    // Get current route to determine which item is selected
    val currentRoute = navController.currentDestination?.route
    val currentRouteBase = currentRoute?.split("/")?.firstOrNull()

    NavigationBar(
        containerColor = Color(0xFF896C6C),
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)  // Increased height to accommodate labels
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF896C6C),
                        Color(0xFF896C6C).copy(alpha = 0.98f)
                    )
                )
            ),
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            NavigationItem(Icons.Default.Home, "Home", "home", 0),
            NavigationItem(Icons.Default.GridView, "Categories", "category", 0),
            NavigationItem(Icons.Default.FavoriteBorder, "Favorites", "wishlist", 0),
            NavigationItem(Icons.Default.Person, "Profile", "profile", 0)
        )

        items.forEach { item ->
            val selected = when {
                item.route == "home" && currentRouteBase == "home" -> true
                item.route == "category" && currentRouteBase == "category" -> true
                item.route == "wishlist" && currentRouteBase == "wishlist" -> true
                item.route == "profile" && currentRouteBase == "profile" -> true
                else -> false
            }

            // Simple color animations - no size changes
            val iconColor by animateColorAsState(
                targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
                animationSpec = tween(300),
                label = "icon_color"
            )

            val labelColor by animateColorAsState(
                targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
                animationSpec = tween(300),
                label = "label_color"
            )

            NavigationBarItem(
                icon = {
                    // Simple icon without scaling
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)  // Fixed size, no scaling
                    )
                },
                label = {
                    // Always show label with color animation only
                    Text(
                        text = item.label,
                        fontSize = 12.sp,
                        color = labelColor,
                        fontWeight = FontWeight.Normal  // Not bold, keeping it sleek
                    )
                },
                selected = false,  // Always false to remove selection effects
                onClick = {
                    if (!selected) {
                        // Haptic feedback on selection
                        hapticManager.performHapticFeedback(HapticType.LIGHT_CLICK)
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                        when (item.route) {
                            "home" -> {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                                hapticManager.performHapticFeedback(HapticType.SUCCESS)
                            }
                            "category" -> {
                                navController.navigate("category/all") {
                                    popUpTo("home")
                                }
                            }
                            "wishlist" -> {
                                navController.navigate("wishlist") {
                                    popUpTo("home")
                                }
                            }
                            "profile" -> {
                                Log.d("Navigation", "Profile clicked, current route: $currentRoute")
                                try {
                                    navController.navigate("profile") {
                                        popUpTo("home")
                                    }
                                    Log.d("Navigation", "Profile navigation successful")
                                } catch (e: Exception) {
                                    Log.e("Navigation", "Profile navigation failed", e)
                                    hapticManager.performHapticFeedback(HapticType.ERROR)
                                }
                            }
                        }
                    } else {
                        hapticManager.performHapticFeedback(HapticType.LIGHT_CLICK)
                    }
                },
                interactionSource = remember { MutableInteractionSource() },
                modifier = Modifier.height(64.dp)  // Fixed height for label space
            )
        }
    }
}

// ADD these new composables:

data class NavigationItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val route: String,
    val badgeCount: Int
)



// Function to format price with currency
fun formatPrice(price: Double, currency: String): String {
    return when (currency) {
        "USD" -> "$${price.toInt()}"
        "EUR" -> "€${price.toInt()}"
        "INR" -> "₹${price.toInt()}"
        else -> "${price.toInt()} $currency"
    }
}

@Composable
fun RecentlyViewedSection(
    products: List<ProductModel>,
    isLoading: Boolean,
    onProductClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit
) {
    // Only show section if there are products or currently loading
    if (products.isEmpty() && !isLoading) return

    Column(
        modifier = Modifier.padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recently Viewed",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8B4513)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            // Loading state
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(3) {
                    RecentlyViewedPlaceholder()
                }
            }
        } else {
            // Products list
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                items(products) { product ->
                    RecentlyViewedItem(
                        product = product,
                        onProductClick = onProductClick,
                        onFavoriteClick = onFavoriteClick
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// 3. ADD RecentlyViewedItem composable

@Composable
fun RecentlyViewedItem(
    product: ProductModel,
    onProductClick: (String) -> Unit,
    onFavoriteClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(180.dp)
            .clickable { onProductClick(product.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEDDCA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.diamondring_homescreen)
                )

                // Favorite icon
                IconButton(
                    onClick = { onFavoriteClick(product.id) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .background(
                            Color.White.copy(alpha = 0.8f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (product.isFavorite) Color(0xFF8B4513) else Color(0xFF8B4513),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = product.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(32.dp),
                    color = Color(0xFF8B4513)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatPrice(product.price, product.currency),
                    fontSize = 13.sp,
                    color = Color(0xFF8B4513),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 4. ADD RecentlyViewedPlaceholder for loading state

@Composable
fun RecentlyViewedPlaceholder() {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(180.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEDDCA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Placeholder image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF8B4513).copy(alpha = 0.2f),
                                Color(0xFF8B4513).copy(alpha = 0.1f),
                                Color(0xFF8B4513).copy(alpha = 0.2f)
                            )
                        )
                    )
            )

            // Placeholder text
            Column(modifier = Modifier.padding(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(12.dp)
                        .background(
                            Color(0xFF8B4513).copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(10.dp)
                        .background(
                            Color(0xFF8B4513).copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun ElegantCarouselSection(items: List<CarouselItemModel>) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })
    val scope = rememberCoroutineScope()

    // Auto-scroll effect
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4000) // 4 seconds
            val nextPage = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(
                page = nextPage,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // "Discover Our Finest Collections" heading
        Text(
            text = "Discover Our Finest Collections",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF8B4513),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = TextAlign.Center
        )

        // Horizontal line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF8B4513).copy(alpha = 0.3f))
                .padding(bottom = 16.dp)
        )

        // Main carousel content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.height(200.dp)
        ) { page ->
            val item = items[page]
            ElegantCarouselItem(item = item)
        }

        // Pagination dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(items.size) { index ->
                val isSelected = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 10.dp else 8.dp)
                        .background(
                            color = if (isSelected) Color(0xFF8B4513) else Color(0xFFCCCCCC),
                            shape = CircleShape
                        )
                )
                
                // Add spacing between dots (except for the last one)
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}

@Composable
fun ElegantCarouselItem(item: CarouselItemModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side - Smaller circular image
        Box(
            modifier = Modifier
                .size(160.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.swipeable_img1)
            )
        }

        // Right side - Text and button
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B4513),
                lineHeight = 24.sp
            )
            
            Text(
                text = item.subtitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B4513),
                lineHeight = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dynamic button with Firebase text
                Button(
                    onClick = { /* Handle click */ },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(1.dp, Color(0xFFD2B48C)),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        text = item.buttonText,
                        color = Color(0xFF8B4513),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Navigation arrow
                IconButton(
                    onClick = { /* Handle next */ },
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(0xFF8B4513),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedProductItem(
    product: ProductModel,
    onProductClick: (String) -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    // Use stable keys to prevent unnecessary recomposition
    val productId = remember(product.id) { product.id }
    var currentImageIndex by remember(productId) { mutableIntStateOf(0) }

    // Optimize image list creation - make it stable
    val imageUrls = remember(productId, product.imageUrls.size, product.imageUrl) {
        buildList {
            if (product.imageUrls.isNotEmpty()) {
                addAll(product.imageUrls.filter { it.isNotBlank() })
            }
            if (isEmpty() && product.imageUrl.isNotBlank()) {
                add(product.imageUrl)
            }
        }.distinct()
    }

    // Reduce auto-scroll frequency to improve performance
    LaunchedEffect(productId, imageUrls.size) {
        if (imageUrls.size > 1) {
            while (true) {
                delay(4000) // Increased from 3000ms
                currentImageIndex = (currentImageIndex + 1) % imageUrls.size
            }
        }
    }

    // Remove this expensive check that happens on every composition
     LaunchedEffect(product.id) {
         viewModel.checkWishlistStatus(product.id)
     }

    // Use Surface instead of Card for better performance
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProductClick(productId) },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp // Reduced elevation
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                // Simplified image display - remove expensive AnimatedContent
                if (imageUrls.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrls.getOrNull(currentImageIndex))
                            .crossfade(300) // Reduced crossfade duration
                            .build(),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.diamondring_homescreen),
                        error = painterResource(id = R.drawable.diamondring_homescreen)
                    )
                }

                // Simplified favorite button
                IconButton(
                    onClick = { viewModel.toggleFavorite(productId) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .background(
                            Color.White.copy(alpha = 0.9f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (product.isFavorite) Color(0xFF896C6C) else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Simplified dots indicator - only show if multiple images
                if (imageUrls.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(8.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(imageUrls.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(
                                        width = if (index == currentImageIndex) 16.dp else 6.dp,
                                        height = 6.dp
                                    )
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (index == currentImageIndex)
                                            Color.White else Color.White.copy(alpha = 0.4f)
                                    )
                            )
                        }
                    }
                }
            }

            // Simplified text section
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = product.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = formatPrice(product.price, product.currency),
                    fontSize = 13.sp,
                    color = Color(0xFF896C6C),
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun ThemedCollectionsSection(
    collections: List<CollectionModel>,
    onCollectionClick: (String) -> Unit
) {
    if (collections.isEmpty()) return

    Column(
        modifier = Modifier.padding(16.dp)
    ) {


        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            itemsIndexed(collections) { index, collection ->
                CollectionItem(
                    collection = collection,
                    onCollectionClick = onCollectionClick
                )
            }
        }
    }
}

@Composable
fun CollectionItem(
    collection: CollectionModel,
    onCollectionClick: (String) -> Unit
) {
        Card(
            modifier = Modifier
                .width(320.dp)
                .height(280.dp)
                .clickable { onCollectionClick(collection.id) },
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F1E9)) // Light beige background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Collection Title
                Text(
                    text = collection.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF896C6C), // Dark brown color
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Three Images Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Display up to 3 images from imageUrls, fallback to imageUrl if needed
                    val imagesToShow = if (collection.imageUrls.isNotEmpty()) {
                        collection.imageUrls.take(3)
                    } else {
                        listOf(collection.imageUrl).take(3)
                    }
                    
                    // Debug logging
                    Log.d("CollectionCard", "Collection ${collection.name}: imageUrls=${collection.imageUrls.size}, imagesToShow=${imagesToShow.size}")

                    imagesToShow.forEachIndexed { imageIndex, imageUrl ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "${collection.name} image ${imageIndex + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(id = R.drawable.collectioin_img1)
                            )
                        }
                    }

                    // Fill remaining space if less than 3 images
                    repeat(3 - imagesToShow.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = collection.description,
                    fontSize = 14.sp,
                    color = Color(0xFF896C6C), // Dark brown color
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // See All Products Button
                Button(
                    onClick = { onCollectionClick(collection.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF896C6C) // Theme color background
                    ),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Text(
                        text = "See All Products",
                        color = Color(0xFFF8F1E9),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pagination dots (simplified version)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(3) { dotIndex ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (dotIndex == 0) Color(0xFF8B4513) else Color(0xFFD3D3D3),
                                    shape = CircleShape
                                )
                        )
                        if (dotIndex < 2) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }
            }
        }
    }

// SHIMMER PLACEHOLDERS

@Composable
fun ShimmerCarouselPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)  // Add same padding as real carousel
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))  // Add same rounded corners
            .shimmerEffect()
    )
}

@Composable
fun ShimmerCategoryPlaceholder() {
    LazyRow(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(5) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(12.dp)
                        .shimmerEffect()
                )
            }
        }
    }
}



@Composable
fun ShimmerCollectionsPlaceholder() {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(24.dp)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(3) {
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shimmerEffect()
                )
            }
        }
    }
}

@Composable
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    background(Color.LightGray.copy(alpha = alpha))
}


@Composable
fun GradientHeaderWithBangles(
    categories: List<Category>,
    onCategoryClick: (String) -> Unit = {}
) {
    var isGradientSearchActive by remember { mutableStateOf(false) }
    var gradientSearchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    
    // Filter categories based on search query
    val filteredCategories = remember(gradientSearchQuery, categories) {
        if (gradientSearchQuery.isEmpty()) {
            categories
        } else {
            categories.filter { category ->
                category.name.contains(gradientSearchQuery, ignoreCase = true)
            }
        }
    }
    val gradientColors = listOf(
        Color(0xFFC59E9E), // Top: C59E9E
        Color(0xFFE5BEB5), // Middle: E5BEB5
        Color(0xFFEEDDCA)  // Bottom: EEDDCA
    )

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(
                    brush = Brush.verticalGradient(colors = gradientColors)
                )
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(id = R.drawable.bangles),
                contentDescription = "Bangles Header",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .align(Alignment.BottomCenter),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text="Pick the                 ",
                        fontSize = 22.sp,
                        color = Color(0xFF564444),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row (modifier = Modifier.fillMaxWidth().padding(end = 48.dp),
                    horizontalArrangement = Arrangement.End) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold)) {
                                append("Perfect")
                            }
                            withStyle(style = SpanStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)) {
                                append(" Gift")
                            }
                            withStyle(style = SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold)) {
                                append(" for your")
                            }
                        },
                        color = Color(0xFF564444)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth().padding(end=24.dp),
                    horizontalArrangement = Arrangement.End) {
                    Text(
                        text="loved ones",
                        fontSize = 22.sp,
                        color = Color(0xFF564444),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        
        // Search bar with smooth gradient transition
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFEEDDCA), // Start from gradient end
                            Color(0xFFF5F5F5), // Light transition
                            Color.White        // End with white
                        )
                    )
                )
        ) {
            // Search bar positioned in the center
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp)
                    .align(Alignment.Center)
                    .clickable { isGradientSearchActive = !isGradientSearchActive },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, Color(0xFF564444).copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isGradientSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isGradientSearchActive) "Close Search" else "Search",
                        tint = Color(0xFF564444),
                        modifier = Modifier.size(20.dp)
                    )

                    if (isGradientSearchActive) {
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = gradientSearchQuery,
                            onValueChange = { gradientSearchQuery = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = Color(0xFF564444),
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color(0xFF564444)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { focusManager.clearFocus() }
                            ),
                            decorationBox = { innerTextField ->
                                if (gradientSearchQuery.isEmpty()) {
                                    Text(
                                        text = "Search by Category",
                                        color = Color(0xFF564444).copy(alpha = 0.6f),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                                innerTextField()
                            }
                        )
                    } else {
                        Text(
                            text = "Search by Category",
                            color = Color(0xFF564444),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        // Search results section
        AnimatedVisibility(
            visible = isGradientSearchActive,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(200)
            ) + fadeOut(animationSpec = tween(200))
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Column {
                    if (gradientSearchQuery.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(filteredCategories) { category ->
                                GradientSearchItem(
                                    category = category,
                                    onClick = { 
                                        onCategoryClick(category.id)
                                        isGradientSearchActive = false
                                        gradientSearchQuery = ""
                                    }
                                )
                            }
                            
                            if (filteredCategories.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "No results",
                                                tint = Color.Gray.copy(alpha = 0.5f),
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "No categories found",
                                                color = Color.Gray,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Show empty state when search is active but no query
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(vertical = 32.dp),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Column(
//                                horizontalAlignment = Alignment.CenterHorizontally
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Default.Search,
//                                    contentDescription = "Search",
//                                    tint = Color.Gray.copy(alpha = 0.5f),
//                                    modifier = Modifier.size(32.dp)
//                                )
//                                Spacer(modifier = Modifier.height(8.dp))
//                                Text(
//                                    text = "Start typing to search categories",
//                                    color = Color.Gray,
//                                    fontSize = 14.sp
//                                )
//                            }
//                        }
                    }
                }
            }
        }
    }
}

// Featured Products Title Composable
@Composable
fun FeaturedProductsTitle() {
    Text(
        text = "Featured Products",
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF333333),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        textAlign = TextAlign.Center
    )
}

// Featured Product Card Composable matching RecentlyViewedItem structure exactly
@Composable
fun FeaturedProductCard(
    product: ProductModel,
    onProductClick: (String) -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val productId = remember(product.id) { product.id }
    var currentImageIndex by remember(productId) { mutableIntStateOf(0) }

    // Optimize image list creation
    val imageUrls = remember(productId, product.imageUrls.size, product.imageUrl) {
        buildList {
            if (product.imageUrls.isNotEmpty()) {
                addAll(product.imageUrls.filter { it.isNotBlank() })
            }
            if (isEmpty() && product.imageUrl.isNotBlank()) {
                add(product.imageUrl)
            }
        }.distinct()
    }

    // Auto-scroll images
    LaunchedEffect(productId, imageUrls.size) {
        if (imageUrls.size > 1) {
            while (true) {
                delay(4000)
                currentImageIndex = (currentImageIndex + 1) % imageUrls.size
            }
        }
    }

    LaunchedEffect(product.id) {
        viewModel.checkWishlistStatus(product.id)
    }

    // Card matching RecentlyViewedItem structure exactly with blue background
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clickable { onProductClick(productId) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD8F6FF) // Light blue background
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                if (imageUrls.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrls.getOrNull(currentImageIndex))
                            .crossfade(true)
                            .build(),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.diamondring_homescreen)
                    )
                }

                // Favorite icon
                IconButton(
                    onClick = { viewModel.toggleFavorite(productId) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .background(
                            Color.White.copy(alpha = 0.8f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (product.isFavorite) Color(0xFF8B4513) else Color(0xFF8B4513),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Image dots indicator
                if (imageUrls.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(6.dp)
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        repeat(imageUrls.size) { index ->
                            Box(
                                modifier = Modifier
                                    .size(
                                        width = if (index == currentImageIndex) 8.dp else 4.dp,
                                        height = 4.dp
                                    )
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (index == currentImageIndex)
                                            Color.White else Color.White.copy(alpha = 0.4f)
                                    )
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = product.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(32.dp),
                    color = Color(0xFF8B4513)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatPrice(product.price, product.currency),
                    fontSize = 13.sp,
                    color = Color(0xFF8B4513),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}