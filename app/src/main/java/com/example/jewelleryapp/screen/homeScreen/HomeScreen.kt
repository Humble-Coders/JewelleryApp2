package com.example.jewelleryapp.screen.homeScreen

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jewelleryapp.R
import com.example.jewelleryapp.model.Category
import com.google.firebase.auth.FirebaseAuth
import com.example.jewelleryapp.model.CarouselItem as CarouselItemModel
import com.example.jewelleryapp.model.Category as CategoryModel
import com.example.jewelleryapp.model.Collection as CollectionModel
import com.example.jewelleryapp.model.Product as ProductModel
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import com.example.jewelleryapp.model.UserProfile
import com.example.jewelleryapp.repository.ProfileRepository
import com.example.jewelleryapp.screen.profileScreen.ProfileViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.File


@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCategoryClick: (String) -> Unit = {},
    onProductClick: (String) -> Unit = {},
    onCollectionClick: (String) -> Unit = {},
    navController: NavController,
    onLogout: () -> Unit, // Add this parameter
    profileViewModel: ProfileViewModel, // Add this parameter

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
    val currentProfile by profileViewModel.currentProfile.collectAsState()


    val scrollState = rememberScrollState()

    // Key fix: Use a stable key for drawer state to prevent recreation
    val currentRoute = navController.currentDestination?.route
    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

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
                    // drawerState.close() // This can cause issues in onDispose
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
                        onLogout() // Call the logout function passed from the parent
                    },
                    userProfile = currentProfile, // Pass profile data
                    homeViewModel = viewModel // Pass the HomeViewModel to handle Google Maps

                )
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = drawerContent,
        gesturesEnabled = currentRoute == "home" // Only enable on home screen
    ) {
        Scaffold(
            topBar = {
                TopAppbar(
                    title = "Gagan Jewellers",
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
                    onSearchQueryChange = { viewModel.onSearchQueryChange(it) }
                )
            },
            bottomBar = { BottomNavigationBar(navController = navController) }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                // Main content
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFB78628))
                    }
                } else if (error != null) {
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
                                    containerColor = Color(0xFFB78628)
                                )
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .padding(paddingValues)
                            .verticalScroll(scrollState)
                    ) {
                        ImageCarousel(carouselItems)
                        CategoryRow(categories, onCategoryClick = { categoryId ->
                            Log.d("HomeScreen", "Category clicked: $categoryId")
                            val categoryName = categories.find { it.id == categoryId }?.name ?: "Products"
                            navController.navigate("categoryProducts/$categoryId/$categoryName")
                        })
                        FeaturedProductsSection(featuredProducts, viewModel, onProductClick)
                        ThemedCollectionsSection(collections, onCollectionClick)
                    }
                }

                // Search results overlay
                CategorySearchResults(
                    categories = filteredCategories,
                    isVisible = isSearchActive,
                    onCategoryClick = { categoryId ->
                        // Close search and navigate to category
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

    // Add this before the closing brace of HomeScreen
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

@Composable
fun DrawerContent(
    navController: NavController,
    onCloseDrawer: () -> Unit,
    onLogout: () -> Unit,
    userProfile: UserProfile?, // Pass user profile data
    homeViewModel: HomeViewModel
) {
    val amberColor = Color(0xFFB78628)
    val context = LocalContext.current

    // Get current user and profile data
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val userName = remember(currentUser) { currentUser?.displayName ?: "Guest" }

    // Get profile data to access local image path
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var localImagePath by remember { mutableStateOf<String?>(null) }

    // Fetch profile data when composable loads
    LaunchedEffect(currentUser) {
        currentUser?.let { user ->
            try {
                // Get profile document from Firestore
                val profileDoc = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                if (profileDoc.exists()) {
                    // Get Google profile picture URL
                    profileImageUrl = profileDoc.getString("profilePictureUrl")

                    // Get local image path from DataStore (for email/password users)
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
    ) {
        // Profile header with proper image handling
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
                // Show image based on priority: local image -> Google image -> default
                when {
                    // Local image (for email/password users)
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
                    // Google profile picture
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
                    // Default image
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

        // Rest of your drawer content remains the same...
        HorizontalDivider(thickness = 1.dp, color = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))

        // All your existing navigation items...
        val navigateToProfile = remember {
            {
                onCloseDrawer()
                navController.navigate("profile")
            }
        }

        val navigateToHome = remember {
            {
                onCloseDrawer()
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            }
        }

        val logout = remember {
            {
                onCloseDrawer()
                onLogout()
            }
        }

        DrawerItem(
            icon = Icons.Outlined.Person,
            text = "My Profile",
            onClick = navigateToProfile
        )

        DrawerItem(
            icon = Icons.Outlined.History,
            text = "Order History",
            onClick = { onCloseDrawer() }
        )

        Text(
            text = "Shop By",
            color = amberColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        DrawerItem(
            text = "All Jewellery",
            onClick = navigateToHome
        )

        DrawerItem(
            text = "Metal",
            onClick = {
                onCloseDrawer()
                navController.navigate("category/metals")
            }
        )

        DrawerItem(
            text = "Collections",
            onClick = {
                onCloseDrawer()
                navController.navigate("category/all")


            }
        )



        Text(
            text = "More",
            color = amberColor,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

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
                // You'll need to pass homeViewModel and context to DrawerContent
                homeViewModel.openWhatsApp(context)
            }
        )

        DrawerItem(
            icon = Icons.Outlined.LocationOn,
            text = "Store Locator",
            onClick = {
                onCloseDrawer()
                // Add this line - you'll need to pass viewModel and context to DrawerContent
                homeViewModel.openGoogleMaps(context)
            }
        )

        DrawerItem(
            icon = Icons.AutoMirrored.Outlined.ExitToApp,
            text = "Logout",
            onClick = logout
        )


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





@Composable
fun TopAppbar(
    title: String,
    onMenuClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    // Add these parameters
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    onSearchToggle: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {}
) {
    val amberColor = Color(0xFFB78628)
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(0.5.dp, Color.LightGray)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = amberColor
                    )
                }
            } else {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = amberColor
                    )
                }
            }

            // Animate title visibility
            AnimatedVisibility(
                visible = !isSearchActive,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Text(
                    text = title,
                    color = amberColor,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSearchToggle) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (isSearchActive) "Close Search" else "Search",
                        tint = amberColor
                    )
                }

                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Favorites",
                        tint = amberColor
                    )
                }
            }
        }

        // Animated search field
        AnimatedVisibility(
            visible = isSearchActive,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(300)
            ) + expandVertically(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300)
            ) + shrinkVertically(animationSpec = tween(300))
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        "Search categories...",
                        color = Color.Gray
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = amberColor,
                    unfocusedBorderColor = Color.LightGray,
                    cursorColor = amberColor
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

// 3. Create CategorySearchResults composable in HomeScreen.kt

@Composable
fun CategorySearchResults(
    categories: List<Category>,
    isVisible: Boolean,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
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

@Composable
fun CategorySearchItem(
    category: Category,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                tint = Color(0xFFB78628),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// 4. Update HomeScreen composable




@Composable
fun ImageCarousel(items: List<CarouselItemModel>) {
    if (items.isEmpty()) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val pagerState = rememberPagerState(pageCount = { items.size })
        val scope = rememberCoroutineScope()

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]
            Box(modifier = Modifier.fillMaxSize()) {
                // Use AsyncImage with Coil to load from URL
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.swipeable_img1) // Use placeholder from resources
                )

                // Overlay with text
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )

                // Text overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = item.subtitle,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light
                    )

                    // Increased spacing between subtitle and title
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Increased spacing between title and button
                    Spacer(modifier = Modifier.height(16.dp))

                    // Updated to make the button round
                    Button(
                        onClick = { /* Handle click */ },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .height(40.dp)
                            .border(1.dp, Color.White, CircleShape)
                    ) {
                        Text(
                            text = item.buttonText,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Dots indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(items.size) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == pagerState.currentPage) Color.White else Color.White.copy(
                                alpha = 0.5f
                            )
                        )
                        .padding(4.dp)
                        .clickable {
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

// Category row with circular images
@Composable
fun CategoryRow(categories: List<Category>, onCategoryClick: (String) -> Unit) {
    if (categories.isEmpty()) return

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(categories) { category ->
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
fun FeaturedProductsSection(
    products: List<ProductModel>,
    viewModel: HomeViewModel, // Pass viewModel to handle wishlist functionality
    onProductClick: (String) -> Unit
) {
    if (products.isEmpty()) return

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        SectionTitle("Featured Products")

        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.height(350.dp)
        ) {
            items(products) { product ->
                ProductItem(
                    product = product,
                    onProductClick = onProductClick,
                    viewModel = viewModel // Pass viewModel to handle wishlist
                )
            }
        }
    }
}
// Section title component
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
}

// Individual product item
// Individual product item with wishlist functionality
@Composable
fun ProductItem(
    product: ProductModel,
    onProductClick: (String) -> Unit,
    viewModel: HomeViewModel // Add ViewModel parameter to handle wishlist
) {
    // Track wishlist status with state


    // Track any updates to wishlist status
    LaunchedEffect(product.id) {
        viewModel.checkWishlistStatus(product.id)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProductClick(product.id) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                // Use AsyncImage with Coil to load from URL
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.diamondring_homescreen) // Placeholder from resources
                )

                // Favorite icon with clickable area and proper wishlist status
                IconButton(
                    onClick = {
                        viewModel.toggleFavorite(product.id)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (product.isFavorite) Color(0xFFD4A968) else Color(0xFFD4A968)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = formatPrice(product.price, product.currency),
                    fontSize = 14.sp,
                    color = Color(0xFFB78628) // Amber/gold color for price
                )
            }
        }
    }
}

// Themed Collections section
@Composable
fun ThemedCollectionsSection(collections: List<CollectionModel>, onCollectionClick: (String) -> Unit) {
    if (collections.isEmpty()) return

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        SectionTitle("Themed Collections")

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(collections) { collection ->
                CollectionItem(collection, onCollectionClick)
            }
        }
    }
}

// Individual collection item
@Composable
fun CollectionItem(collection: CollectionModel, onCollectionClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .width(280.dp) // Increased width to fit description
            .height(140.dp) // Increased height to fit description
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCollectionClick(collection.id) }
    ) {
        // Use AsyncImage with Coil to load from URL
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(collection.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = collection.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.collectioin_img1) // Placeholder from resources
        )

        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Slightly darker overlay
        )

        // Collection name, description and View All text
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .fillMaxWidth(0.85f) // Limit width of text
        ) {
            Text(
                text = collection.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = collection.description,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "View Collection",
                color = Color(0xFFB4A06C),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Removed Recently Viewed section as requested

@Composable
fun BottomNavigationBar(navController: NavController) {
    val amberColor = Color(0xFFB4A06C) // Amber/gold color for all icons

    // Get current route to determine which item is selected
    val currentRoute = navController.currentDestination?.route
    val currentRouteBase = currentRoute?.split("/")?.firstOrNull()

    NavigationBar(
        containerColor = Color.White
    ) {
        val items = listOf(
            Triple(Icons.Default.Home, "Home", "home"),
            Triple(Icons.Default.GridView, "Categories", "category"),
            Triple(Icons.Default.FavoriteBorder, "Favorites", "wishlist"),
            Triple(Icons.Default.Person, "Profile", "profile")
        )

        items.forEach { (icon, label, route) ->
            val selected = when {
                route == "home" && currentRouteBase == "home" -> true
                route == "category" && currentRouteBase == "category" -> true
                route == "wishlist" && currentRouteBase == "wishlist" -> true
                route == "profile" && currentRouteBase == "profile" -> true
                else -> false
            }

            NavigationBarItem(
                icon = {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = amberColor // All icons use the amber color
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = if (selected) amberColor else Color.Gray
                    )
                },
                selected = selected,
                onClick = {
                    if (!selected) {
                        when (route) {
                            "home" -> navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                            "category" -> navController.navigate("category/all") {
                                popUpTo("home")
                            }
                            "wishlist" -> navController.navigate("wishlist") {
                                popUpTo("home")
                            }
                            "profile" ->  {
                                // In BottomNavigationBar, update the profile onClick

                                Log.d("Navigation", "Profile clicked, current route: $currentRoute")
                                try {
                                    navController.navigate("profile") {
                                        popUpTo("home")
                                    }
                                    Log.d("Navigation", "Profile navigation successful")
                                } catch (e: Exception) {
                                    Log.e("Navigation", "Profile navigation failed", e)
                                }
                            }


                        }
                    }
                }
            )
        }
    }
}


// Function to format price with currency
fun formatPrice(price: Double, currency: String): String {
    return when (currency) {
        "USD" -> "$${price.toInt()}"
        "EUR" -> "€${price.toInt()}"
        "INR" -> "₹${price.toInt()}"
        else -> "${price.toInt()} $currency"
    }
}

