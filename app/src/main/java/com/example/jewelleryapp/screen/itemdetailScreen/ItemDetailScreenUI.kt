package com.example.jewelleryapp.screen.itemdetailScreen

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.jewelleryapp.R
import com.example.jewelleryapp.model.Product
import com.example.jewelleryapp.screen.homeScreen.BottomNavigationBar
import kotlinx.coroutines.launch
import java.util.Locale

// Defined colors as constants
private val GoldColor = Color(0xFFC4A661)
private val GoldColorTransparent = GoldColor.copy(alpha = 0.3f)
private val ButtonColor = Color(0xFFC4A661)
private val BackgroundColor = Color(0xFFAA8F8F)
private val TextGrayColor = Color.Gray
private val TextDescriptionColor = Color(0xFF4B5563)
private val TextPriceColor = Color(0xFF333333)


data class ProductSpec(
    val iconId: Int,
    val title: String,
    val value: String?,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun JewelryProductScreen(
    productId: String,
    viewModel: ItemDetailViewModel,
    navController: NavController,
    onBackClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onProductClick: (String) -> Unit = {}
) {
    val product by viewModel.product.collectAsState()
    val isWishlisted by viewModel.isInWishlist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val similarProducts by viewModel.similarProducts.collectAsState()

    // Load product when screen opens
    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    // Fetch similar products when product is loaded
    LaunchedEffect(product) {
        product?.let {
            if (it.categoryId.isNotBlank()) {
                Log.d("JewelryProductScreen", "Loading similar products for category: ${it.categoryId}")
                viewModel.loadSimilarProducts()
            }
        }
    }

    Scaffold(
        topBar = {
            ProductTopAppBar(
                title = product?.name ?: "Luxury Jewelry",
                isWishlisted = isWishlisted,
                onBackClick = onBackClick,
                onWishlistClick = { viewModel.toggleWishlist() },
                onShareClick = onShareClick
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GoldColor)
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error: $error", color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadProduct(productId) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldColor)
                    ) {
                        Text("Try Again")
                    }
                }
            }
        } else {
            product?.let { prod ->
                // Create specs list with null handling for missing fields
                val specs = listOf(
                    ProductSpec(
                        R.drawable.material_icon,
                        "Material",
                        if (!prod.materialId.isNullOrBlank()) {
                            val materialName = prod.materialId.replace("material_", "")
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                            if (!prod.materialType.isNullOrBlank()) {
                                "$materialName ${prod.materialType}"
                            } else {
                                materialName
                            }
                        } else {
                            null
                        }
                    ),
                    ProductSpec(R.drawable.stone, "Stone", prod.stone.ifEmpty { null }),
                    ProductSpec(R.drawable.clarity, "Clarity", prod.clarity.ifEmpty { null }),
                    ProductSpec(R.drawable.cut, "Cut", prod.cut.ifEmpty { null })
                )

                // Create a list of image URLs (currently single image from Firebase)
                val imageList = if (prod.imageUrl.isNotBlank()) listOf(prod.imageUrl) else emptyList()
                val pagerState = rememberPagerState(pageCount = { maxOf(imageList.size, 1) })

                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // Image carousel with Firebase images or placeholder
                    if (imageList.isNotEmpty()) {
                        ImageCarouselWithCoil(
                            imageUrls = imageList,
                            pagerState = pagerState,
                            onDotClick = { index ->
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .background(BackgroundColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No image available", color = Color.White)
                        }
                    }

                    // Product details
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Product title and collection
                        ProductHeader(
                            title = prod.name,
                            collection = "Luxury Collection", // Placeholder since Firebase doesn't have collection field
                            currentPrice = "${prod.currency} ${prod.price}",
                            originalPrice = "${prod.currency} ${(prod.price * 1.2).toInt()}" // Placeholder for original price
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Product specifications
                        ProductSpecifications(specs = specs)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Description (using description from Firebase if available)
                        val description = if (prod.description.isNotBlank()) {
                            prod.description
                        } else {
                            "This exquisite piece features perfectly matched stones set in premium metal. Each piece is carefully crafted for exceptional quality and brilliance."
                        }
                        ProductDescription(description = description)

                        // Add this before the SimilarProducts section
                        Spacer(modifier = Modifier.height(16.dp))

                        // Similar products section - now only here, removed duplicate section
                        if (similarProducts.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            SimilarProducts(
                                products = similarProducts,
                                onProductClick = onProductClick,
                                onWishlistToggle = { similarProductId ->
                                    // Toggle wishlist for similar products
                                    viewModel.toggleSimilarProductWishlist(similarProductId)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Wishlist button that changes based on current wishlist status
                        WishlistButton(
                            isInWishlist = isWishlisted,
                            onClick = { viewModel.toggleWishlist() }
                        )
                    }
                }
            } ?: run {
                // No product loaded
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Product not found", color = Color.Red)
                }
            }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductTopAppBar(
    title: String,
    isWishlisted: Boolean,
    onBackClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onShareClick: () -> Unit
) {
    TopAppBar(
        title = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = onWishlistClick) {
                Icon(
                    if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Wishlist",
                    tint = if (isWishlisted) Color.Red else Color.Gray
                )
            }
            IconButton(onClick = onShareClick) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
        }
    )
}

@Composable
private fun WishlistButton(
    isInWishlist: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isInWishlist) Color.White else ButtonColor
        ),
        border = BorderStroke(1.dp, if (isInWishlist) Color.Red else Color.Black)
    ) {
        Text(
            text = if (isInWishlist) "Remove from Wishlist" else "Add to Wishlist",
            color = if (isInWishlist) Color.Red else Color.Black,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ImageCarouselWithCoil(
    imageUrls: List<String>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    onDotClick: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(BackgroundColor)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = imageUrls[page],
                contentDescription = "Product Image ${page + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Dots indicator
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(imageUrls.size) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index) GoldColor else GoldColorTransparent
                        )
                        .clickable { onDotClick(index) }
                )
            }
        }
    }
}

@Composable
private fun ProductHeader(
    title: String,
    collection: String,
    currentPrice: String,
    originalPrice: String
) {
    Text(
        text = title,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )

    Text(
        text = collection,
        fontSize = 14.sp,
        color = TextGrayColor
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = currentPrice,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = originalPrice,
            fontSize = 16.sp,
            color = TextGrayColor,
            textDecoration = TextDecoration.LineThrough,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun ProductSpecifications(specs: List<ProductSpec>) {
    // Display all specs regardless of null values
    Column {
        for (i in specs.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // First spec in the row takes exactly half the space
                Box(modifier = Modifier.weight(1f)) {
                    ProductSpecItem(spec = specs[i])
                }

                // Second spec in the row also takes exactly half the space
                Box(modifier = Modifier.weight(1f)) {
                    // Only add the second item if it exists
                    if (i + 1 < specs.size) {
                        ProductSpecItem(spec = specs[i + 1])
                    }
                }
            }
            if (i + 2 < specs.size) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProductSpecItem(spec: ProductSpec) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(id = spec.iconId),
            contentDescription = null,
            tint = GoldColor,
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(
                text = spec.title,
                fontSize = 12.sp,
                color = TextGrayColor
            )
            Text(
                text = spec.value ?: "Not specified",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}



@Composable
private fun ProductDescription(description: String) {
    Text(
        text = "Description",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = description,
        fontSize = 17.sp,
        lineHeight = 20.sp,
        color = TextDescriptionColor
    )
}

@Composable
private fun SimilarProducts(
    products: List<Product>,
    onProductClick: (String) -> Unit,
    onWishlistToggle: (String) -> Unit // Add wishlist toggle callback
) {
    Text(
        text = "You may also like",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(products) { product ->
            SimilarProductItem(
                product = product,
                onClick = { onProductClick(product.id) },
                onWishlistToggle = { onWishlistToggle(it) }
            )
        }
    }
}

@Composable
private fun SimilarProductItem(
    product: Product,
    onClick: () -> Unit,
    onWishlistToggle: (String) -> Unit = {} // Add wishlist toggle callback
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color.LightGray)
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Add wishlist icon for similar products
            IconButton(
                onClick = { onWishlistToggle(product.id) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp) // Smaller icon for similar products
            ) {
                Icon(
                    imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Wishlist",
                    tint = if (product.isFavorite) Color.Red else Color.White,
                    modifier = Modifier.size(16.dp) // Smaller icon
                )
            }
        }

        Text(
            text = product.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
        )

        Text(
            text = "${product.currency} ${product.price}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextPriceColor,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
        )
    }
}


