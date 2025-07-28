package com.example.jewelleryapp.screen.productDetailScreen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.viewpager2.widget.ViewPager2.ScrollState
import coil.compose.AsyncImage
import com.example.jewelleryapp.R
import com.example.jewelleryapp.model.Product
import com.example.jewelleryapp.screen.homeScreen.BottomNavigationBar
import com.example.jewelleryapp.screen.loginScreen.GoldenShade
import java.util.Locale

// Enhanced color palette with gradients
private val PrimaryGold = Color(0xFFD4AF37)
private val SecondaryGold = Color(0xFFF4E4BC)
private val AccentGold = Color(0xFFB8860B)
private val DeepGold = Color(0xFF8B7355)
private val TextPrimary = Color(0xFF1F2937)
private val TextSecondary = Color(0xFF6B7280)
private val TextMuted = Color(0xFF9CA3AF)
private val BackgroundPrimary = Color(0xFFFAFAFA)
private val BackgroundSecondary = Color(0xFFFFFFFF)
private val SurfaceElevated = Color(0xFFFFFFFF)
private val SuccessGreen = Color(0xFF10B981)
private val ErrorRed = Color(0xFFEF4444)

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
    onShareClick: () -> Unit,
    onProductClick: (String) -> Unit = {}
) {
    val isWishlisted by viewModel.isInWishlist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val scrollState = rememberScrollState()
    val product by viewModel.product.collectAsState()
    val context = LocalContext.current

    val similarProducts by viewModel.similarProducts.collectAsState()
    val isSimilarProductsLoading by viewModel.isSimilarProductsLoading.collectAsState()

    val imageUrls by viewModel.imageUrls.collectAsState()
    val currentImageIndex by viewModel.currentImageIndex.collectAsState()
    val isFullScreenMode by viewModel.isFullScreenMode.collectAsState()

    // Calculate parallax effect
    val scrollValue = scrollState.value
    val imageHeight = 420.dp
    val cardOffset = 32.dp
    val parallaxFactor = 0.5f

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    LaunchedEffect(product) {
        product?.let {
            if (it.categoryId.isNotBlank()) {
                Log.d("JewelryProductScreen", "Loading similar products for category: ${it.categoryId}")
                viewModel.loadSimilarProducts()
            }
        }
    }

    if (isFullScreenMode) {
        Dialog(
            onDismissRequest = { viewModel.exitFullScreen() },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            FullScreenImageDialog(
                imageUrls = imageUrls,
                currentIndex = currentImageIndex,
                onDismiss = { viewModel.exitFullScreen() },
                onImageChange = { viewModel.navigateToImage(it) }
            )
        }
    }

    Scaffold(
        topBar = {
            EnhancedProductTopAppBar(
                title = product?.name ?: "Luxury Jewelry",
                isWishlisted = isWishlisted,
                onBackClick = onBackClick,
                onWishlistClick = { viewModel.toggleWishlist() },
                onShareClick = {
                    product?.let { prod ->
                        shareProduct(context, prod)
                    }
                }
            )
        },
        bottomBar = { BottomNavigationBar(navController) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BackgroundPrimary,
                            SecondaryGold.copy(alpha = 0.1f)
                        )
                    )
                )
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = PrimaryGold,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Loading your treasure...",
                            color = TextSecondary,
                            fontSize = 16.sp
                        )
                    }
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EnhancedErrorState(
                        error = error,
                        onRetry = { viewModel.loadProduct(productId) }
                    )
                }
            } else {
                product?.let { prod ->
                    val specs = createProductSpecs(prod)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // Scrollable content
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            // Replace the existing image section in your JewelryProductScreen composable
// with this updated version that keeps controls visible longer

// Inside your JewelryProductScreen composable, replace the Box containing the image section:

// Interactive image section at the top
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(imageHeight)
                            ) {
                                // Background parallax image (non-interactive)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(
                                            // Keep controls visible much longer during scroll
                                            when {
                                                scrollValue <= 400 -> 1f  // Stay visible until 400px
                                                scrollValue >= 700 -> 0f  // Fade completely at 700px
                                                else -> 1f - ((scrollValue - 400) / 300f)  // Slower fade transition
                                            }
                                        )
                                ) {
                                    EnhancedImageSection(
                                        imageUrls = imageUrls,
                                        currentImageIndex = currentImageIndex,
                                        onImageChange = { viewModel.navigateToImage(it) },
                                        onFullScreenToggle = { viewModel.toggleFullScreenMode() },
                                        isInteractive = true
                                    )
                                }

                                // Interactive image layer (normal scrolling)
                                // This layer stays visible even longer
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .alpha(
                                            when {
                                                scrollValue <= 300 -> 1f  // Stay visible until 300px
                                                scrollValue >= 600 -> 0f  // Fade completely at 600px
                                                else -> 1f - ((scrollValue - 300) / 300f)  // Gradual fade
                                            }
                                        )
                                ) {
                                    EnhancedImageSection(
                                        imageUrls = imageUrls,
                                        currentImageIndex = currentImageIndex,
                                        onImageChange = { viewModel.navigateToImage(it) },
                                        onFullScreenToggle = { viewModel.toggleFullScreenMode() },
                                        isInteractive = true
                                    )
                                }
                            }

                            // Spacer to create the sliding effect
                            Spacer(modifier = Modifier.height(-cardOffset))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Enhanced product details with smooth rounded corners
                            EnhancedProductDetailsCard(
                                product = prod,
                                specs = specs,
                                isWishlisted = isWishlisted,
                                onWishlistClick = { viewModel.toggleWishlist() }
                            )

                            // Enhanced similar products in a card
                            if (similarProducts.isNotEmpty() || isSimilarProductsLoading) {
                                EnhancedSimilarProductsSection(
                                    products = similarProducts,
                                    isLoading = isSimilarProductsLoading,
                                    onProductClick = onProductClick,
                                    onWishlistToggle = { productId ->
                                        viewModel.toggleSimilarProductWishlist(productId)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedImageSection(
    imageUrls: List<String>,
    currentImageIndex: Int,
    onImageChange: (Int) -> Unit,
    onFullScreenToggle: () -> Unit,
    isInteractive: Boolean = true
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (imageUrls.isNotEmpty()) {
            if (isInteractive) {
                ZoomableImageViewer(
                    imageUrls = imageUrls,
                    currentIndex = currentImageIndex,
                    onImageChange = onImageChange,
                    onFullScreenToggle = onFullScreenToggle,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Non-interactive version for parallax background
                AsyncImage(
                    model = imageUrls.getOrNull(currentImageIndex),
                    contentDescription = "Product Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.diamondring_homescreen)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SecondaryGold.copy(alpha = 0.3f),
                                Color.LightGray.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "No image",
                        tint = TextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Image not available",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                }
            }
        }


    }
}

@Composable
private fun EnhancedProductDetailsCard(
    product: Product,
    specs: List<ProductSpec>,
    isWishlisted: Boolean,
    onWishlistClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(28.dp), // Increased corner radius for smoother look
        elevation = CardDefaults.cardElevation(
            defaultElevation = 12.dp,
            pressedElevation = 16.dp
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Premium badge
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = PrimaryGold.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = PrimaryGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Premium Collection",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Product title with enhanced typography
            Text(
                text = product.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Enhanced price section with better visual hierarchy
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = SecondaryGold.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Rs ${product.price.toInt()}.0",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Rs ${(product.price * 1.2).toInt()}",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textDecoration = TextDecoration.LineThrough
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = SuccessGreen
                                ),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Save 17%",
                                    fontSize = 8.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Enhanced wishlist button
                    EnhancedWishlistButton(
                        isWishlisted = isWishlisted,
                        onClick = onWishlistClick
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Enhanced specifications grid
            EnhancedSpecificationsGrid(specs = specs)

            Spacer(modifier = Modifier.height(24.dp))

            // Enhanced description section
            Text(
                text = "Description",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            val description = if (product.description.isNotBlank()) {
                product.description
            } else {
                "Exquisite craftsmanship meets timeless elegance in this stunning 22-karat gold piece. Every detail has been meticulously designed to create a masterpiece that celebrates luxury and sophistication."
            }

            Text(
                text = description,
                fontSize = 14.sp,
                lineHeight = 24.sp,
                color = TextSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedProductTopAppBar(
    title: String,
    isWishlisted: Boolean,
    onBackClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val heartScale by animateFloatAsState(
        targetValue = if (isWishlisted) 1.2f else 1f,
        animationSpec = tween(300)
    )

    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = TextPrimary
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(
                        BackgroundSecondary.copy(alpha = 0.9f),
                        CircleShape
                    )
                    .size(40.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
        },
        actions = {
            IconButton(
                onClick = onWishlistClick,
                modifier = Modifier
                    .scale(heartScale)
                    .background(
                        BackgroundSecondary.copy(alpha = 0.9f),
                        CircleShape
                    )
                    .size(40.dp)
            ) {
                Icon(
                    if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Wishlist",
                    tint = if (isWishlisted) GoldenShade else TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onShareClick,
                modifier = Modifier
                    .background(
                        BackgroundSecondary.copy(alpha = 0.9f),
                        CircleShape
                    )
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Share",
                    tint = TextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}


@Composable
private fun EnhancedWishlistButton(
    isWishlisted: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isWishlisted) ErrorRed.copy(alpha = 0.1f) else PrimaryGold.copy(alpha = 0.1f),
        animationSpec = tween(300)
    )

    Button(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            contentDescription = null,
            tint = if (isWishlisted) GoldenShade else PrimaryGold,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun EnhancedSpecificationsGrid(specs: List<ProductSpec>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        specs.chunked(2).forEach { rowSpecs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowSpecs.forEach { spec ->
                    EnhancedSpecCard(
                        spec = spec,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowSpecs.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EnhancedSpecCard(
    spec: ProductSpec,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = SecondaryGold.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PrimaryGold.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        PrimaryGold.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = spec.iconId),
                    contentDescription = null,
                    tint = PrimaryGold,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = spec.title,
                fontSize = 12.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = spec.value ?: "Not specified",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EnhancedSimilarProductsSection(
    products: List<Product>,
    isLoading: Boolean,
    onProductClick: (String) -> Unit,
    onWishlistToggle: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "You may also like",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                if (isLoading) {
                    items(3) { index ->
                        EnhancedProductPlaceholder()
                    }
                } else {
                    items(products) { product ->
                        EnhancedSimilarProductCard(
                            product = product,
                            onClick = { onProductClick(product.id) },
                            onWishlistToggle = { onWishlistToggle(product.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedSimilarProductCard(
    product: Product,
    onClick: () -> Unit,
    onWishlistToggle: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(id = R.drawable.diamondring_homescreen)
                )

                // Gradient overlay
//                Box(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .background(
//                            brush = Brush.verticalGradient(
//                                colors = listOf(
//                                    Color.Transparent,
//                                    Color.Black.copy(alpha = 0.3f)
//                                ),
//                                startY = 100f
//                            )
//                        )
//                )
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = TextPrimary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Rs ${product.price.toInt()}.0",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGold
                )
            }
        }
    }
}

@Composable
private fun EnhancedProductPlaceholder() {
    Card(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                SecondaryGold.copy(alpha = 0.2f),
                                SecondaryGold.copy(alpha = 0.05f),
                                SecondaryGold.copy(alpha = 0.2f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Loading",
                    tint = TextMuted,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(modifier = Modifier.padding(12.dp)) {
                repeat(2) { index ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (index == 0) 0.8f else 0.6f)
                            .height(if (index == 0) 14.dp else 16.dp)
                            .background(
                                SecondaryGold.copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                    )
                    if (index == 0) Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun EnhancedErrorState(
    error: String?,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Oops! Something went wrong",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                error ?: "Unknown error occurred",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGold
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Try Again", color = Color.White)
            }
        }
    }
}

private fun createProductSpecs(product: Product): List<ProductSpec> {
    return listOf(
        ProductSpec(
            R.drawable.material_icon,
            "Material",
            if (!product.materialId.isNullOrBlank()) {
                val materialName = product.materialId.replace("material_", "")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                if (!product.materialType.isNullOrBlank()) {
                    "$materialName ${product.materialType}"
                } else {
                    materialName
                }
            } else {
                "Gold 22K"
            }
        ),
        ProductSpec(R.drawable.stone, "Stone", product.stone.ifEmpty { "Premium" }),
        ProductSpec(R.drawable.clarity, "Clarity", product.clarity.ifEmpty { "Excellent" }),
        ProductSpec(R.drawable.cut, "Cut", product.cut.ifEmpty { "Precision" })
    )
}

private fun shareProduct(context: Context, product: Product) {
    val githubUrl = "https://humble-coders.github.io/gagan-jewellers-links/?product=${product.id}&name=${Uri.encode(product.name)}&price=${product.currency} ${product.price}"

    val shareText = """
        ✨ Discover this exquisite ${product.name}!
        
        💎 Premium Quality | 💰 Price: ${product.currency} ${product.price}
        
        🔗 View Details: $githubUrl
        
        ✨ Gagan Jewellers - Where Elegance Meets Craftsmanship
    """.trimIndent()

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "Exquisite Jewelry from Gagan Jewellers")
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share this beautiful piece"))
}