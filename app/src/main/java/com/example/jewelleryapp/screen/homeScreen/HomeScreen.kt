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
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close




@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onCategoryClick: (String) -> Unit = {},
    onProductClick: (String) -> Unit = {},
    onCollectionClick: (String) -> Unit = {},
    navController: NavController
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

    val scrollState = rememberScrollState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                DrawerContent(navController) { scope.launch { drawerState.close() } }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppbar(
                    title = "Gagan Jewellers",
                    onMenuClick = { scope.launch { drawerState.open() } },
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
                        .background(if (index == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.5f))
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

@Composable
fun DrawerContent(navController: NavController, onCloseDrawer: () -> Unit) {
    val amberColor = Color(0xFFB78628)
    val context = LocalContext.current

    // Get current user info
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userName = currentUser?.displayName ?: "Guest"
    val userPhotoUrl = currentUser?.photoUrl?.toString()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(amberColor, CircleShape)
                    .padding(3.dp)
            ) {
                if (userPhotoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(userPhotoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.ic_launcher_background)
                    )
                } else {
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

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Hi $userName!",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.padding(bottom = 8.dp),
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Rest of your existing drawer content...

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(thickness = 1.dp, color = Color.LightGray) // Line under "Hi!"

        Spacer(modifier = Modifier.height(16.dp))

        // Profile and Order History
        DrawerItem(
            icon = Icons.Outlined.Person,
            text = "My Profile",
            onClick = {
                navController.navigate("profile")
                onCloseDrawer()
            }
        )

        DrawerItem(
            icon = Icons.Outlined.History,
            text = "Order History",
            onClick = {
                // Navigate to order history when implemented
                onCloseDrawer()
            }
        )

        SectionHeader("Shop By")
        DrawerItem(
            text = "All Jewellery",
            onClick = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
                onCloseDrawer()
            }
        )

        DrawerItem(
            text = "Metal",
            onClick = {
                // Navigate to metal category
                navController.navigate("category/metals")
                onCloseDrawer()
            }
        )

        DrawerItem(
            text = "Collections",
            onClick = {
                // Navigate to collections
                navController.navigate("collection/all")
                onCloseDrawer()
            }
        )

        SectionHeader("Shop For")
        DrawerItem(
            text = "Men",
            onClick = {
                // Navigate to men's category
                navController.navigate("category/men")
                onCloseDrawer()
            }
        )

        DrawerItem(
            text = "Kids",
            onClick = {
                // Navigate to kids category
                navController.navigate("category/kids")
                onCloseDrawer()
            }
        )

        SectionHeader("More")
        DrawerItem(
            icon = Icons.Outlined.AttachMoney,
            text = "Gold Rate",
            onClick = {
                // Navigate to gold rate screen when implemented
                onCloseDrawer()
            }
        )

        DrawerItem(
            icon = Icons.Outlined.Headset,
            text = "Get In Touch",
            onClick = {
                // Navigate to contact screen when implemented
                onCloseDrawer()
            }
        )

        DrawerItem(
            icon = Icons.Outlined.LocationOn,
            text = "Store Locator",
            onClick = {
                // Navigate to store locator when implemented
                onCloseDrawer()
            }
        )

        DrawerItem(
            icon = Icons.AutoMirrored.Outlined.ExitToApp,
            text = "Logout",
            onClick = {
                // Handle logout
                FirebaseAuth.getInstance().signOut()
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
                onCloseDrawer()
            }
        )
    }
}
@Composable
fun DrawerItem(text: String, onClick: () -> Unit, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
            Spacer(modifier = Modifier.width(16.dp)) // Space between icon & text
        }
        Text(
            text = text,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f), // Pushes text to the left
            color = Color.Black
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight, // Right arrow icon
            contentDescription = "Arrow",
            tint = Color.Gray
        )
    }
}

@Composable
fun SectionHeader(text: String) {

    val amberColor = Color(0xFFB78628)
    Text(
        text = text,
        color = amberColor,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
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