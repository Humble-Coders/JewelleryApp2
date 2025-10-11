package com.example.jewelleryapp.screen.allProducts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jewelleryapp.model.Product
import com.example.jewelleryapp.model.SortOption
import com.example.jewelleryapp.screen.categoryProducts.FilterSortBottomSheet
import com.example.jewelleryapp.screen.homeScreen.BottomNavigationBar
import com.example.jewelleryapp.screen.homeScreen.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllProductsScreen(
    viewModel: AllProductsViewModel,
    navController: NavController
) {
    val state by viewModel.state.collectAsState()
    val filterSortState by viewModel.filterSortState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val displayProductsCount by viewModel.displayProductsCount.collectAsState()

    var showFilterBottomSheet by remember { mutableStateOf(false) }
    val lazyGridState = rememberLazyGridState()

    // Load more when reaching end of list
    LaunchedEffect(lazyGridState) {
        snapshotFlow {
            val layoutInfo = lazyGridState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > (totalItemsNumber - 5)
        }.collect { shouldLoadMore ->
            if (shouldLoadMore && state.hasMorePages && !state.isLoadingMore) {
                viewModel.loadMoreProducts()
            }
        }
    }

    Scaffold(
        topBar = {
            AllProductsTopBar(
                isSearchActive = isSearchActive,
                searchQuery = searchQuery,
                onBackClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onSearchToggle = { viewModel.toggleSearch() },
                onSearchQueryChange = { viewModel.onSearchQueryChange(it) }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
        ) {
            // Category filter row
            CategoryFilterRow(
                categories = categories,
                selectedCategoryId = selectedCategoryId,
                onCategorySelected = { categoryId ->
                    viewModel.selectCategory(categoryId)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filter and Sort Bar
            FilterSortBar(
                filterSortState = filterSortState,
                totalProducts = displayProductsCount, // Use filtered count instead of total
                onFilterSortClick = { showFilterBottomSheet = true }
            )

            // Products Grid
            when {
                state.isLoading && state.displayedProducts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF896C6C))
                    }
                }
                state.error != null -> {
                    ErrorView(
                        error = state.error!!,
                        onRetry = { viewModel.refreshProducts() }
                    )
                }
                state.displayedProducts.isEmpty() -> {
                    EmptyProductsView(
                        isSearching = searchQuery.isNotBlank(),
                        searchQuery = searchQuery
                    )
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = lazyGridState,
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = state.displayedProducts,
                            key = { product -> product.id }
                        ) { product ->
                            ProductCard(
                                product = product,
                                onProductClick = {
                                    navController.navigate("itemDetail/${product.id}")
                                },
                                onFavoriteClick = {
                                    viewModel.toggleFavorite(product.id)
                                }
                            )
                        }

                        // Loading more indicator
                        if (state.isLoadingMore) {
                            item(span = { GridItemSpan(2) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF896C6C),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterBottomSheet) {
        FilterSortBottomSheet(
            filterSortState = filterSortState,
            onDismiss = { showFilterBottomSheet = false },
            onApplyFilter = { material -> viewModel.applyFilter(material) },
            onApplySort = { sortOption -> viewModel.applySorting(sortOption) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllProductsTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onBackClick: () -> Unit,
    onSearchToggle: () -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val goldColor = Color(0xFF896C6C)

    TopAppBar(
        title = {
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search all products...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { focusManager.clearFocus() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = goldColor,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            } else {
                Text(
                    text = "All Jewellery",
                    color = goldColor,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = goldColor
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchToggle) {
                Icon(
                    imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (isSearchActive) "Close Search" else "Search",
                    tint = goldColor
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
private fun CategoryFilterRow(
    categories: List<com.example.jewelleryapp.model.Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // "All" category button
        CategoryFilterButton(
            text = "All",
            isSelected = selectedCategoryId == null,
            onClick = { onCategorySelected(null) }
        )

        // Individual category buttons
        categories.forEach { category ->
            CategoryFilterButton(
                text = category.name,
                isSelected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) }
            )
        }
    }
}

@Composable
private fun CategoryFilterButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) Color(0xFF896C6C) else Color(0xFFF5F5F5))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Black,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun FilterSortBar(
    filterSortState: com.example.jewelleryapp.model.FilterSortState,
    totalProducts: Int,
    onFilterSortClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Active filters chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
//            Text(
//                text = "$totalProducts items",
//                fontSize = 14.sp,
//                color = Color.Gray
//            )

            if (filterSortState.selectedMaterial != null) {
                FilterChip(
                    text = filterSortState.selectedMaterial,
                    onRemove = { /* Will be handled in bottom sheet */ }
                )
            }

            if (filterSortState.sortOption != SortOption.NONE) {
                FilterChip(
                    text = filterSortState.sortOption.displayName,
                    onRemove = { /* Will be handled in bottom sheet */ }
                )
            }
        }

        // Filter & Sort button
        OutlinedButton(
            onClick = onFilterSortClick,
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF896C6C)
            )
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter & Sort",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Filter & Sort",
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun FilterChip(
    text: String,
    onRemove: () -> Unit
) {
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF896C6C).copy(alpha = 0.1f),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                color = Color(0xFF896C6C)
            )
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    onProductClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProductClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Wishlist button
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (product.isFavorite) "Remove from Wishlist" else "Add to Wishlist",
                        tint = if (product.isFavorite) Color(0xFF896C6C) else Color(0xFF896C6C),
                        modifier = Modifier.size(20.dp)
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

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatPrice(product.price, product.currency),
                    fontSize = 14.sp,
                    color = Color(0xFF896C6C),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = error,
                color = Color.Red,
                fontSize = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF896C6C)
                )
            ) {
                Text("Try Again")
            }
        }
    }
}

@Composable
private fun EmptyProductsView(
    isSearching: Boolean,
    searchQuery: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isSearching) {
                Text(
                    text = "No products found for \"$searchQuery\"",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Try searching with different keywords",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            } else {
                Text(
                    text = "No products available",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Check back later for new arrivals",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}