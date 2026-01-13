package com.humblecoders.jewelleryapp.screen.productDetailScreen

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.jewelleryapp.model.Product
import com.humblecoders.jewelleryapp.repository.JewelryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope

class ItemDetailViewModel(
    private val repository: JewelryRepository
) : ViewModel() {

    private val _product = MutableStateFlow<Product?>(null)
    val product: StateFlow<Product?> = _product.asStateFlow()

    private val _similarProducts = MutableStateFlow<List<Product>>(emptyList())
    val similarProducts = _similarProducts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isInWishlist = MutableStateFlow(false)
    val isInWishlist: StateFlow<Boolean> = _isInWishlist.asStateFlow()

    private val _isSimilarProductsLoading = MutableStateFlow(false)
    val isSimilarProductsLoading: StateFlow<Boolean> = _isSimilarProductsLoading.asStateFlow()

    // In ItemDetailViewModel.kt - Add these new state flows after existing ones
    private val _currentImageIndex = MutableStateFlow(0)
    val currentImageIndex: StateFlow<Int> = _currentImageIndex.asStateFlow()

    private val _isFullScreenMode = MutableStateFlow(false)
    val isFullScreenMode: StateFlow<Boolean> = _isFullScreenMode.asStateFlow()

    private val _imageUrls = MutableStateFlow<List<String>>(emptyList())
    val imageUrls: StateFlow<List<String>> = _imageUrls.asStateFlow()

    // Track current productId to detect changes and clear old data
    private var currentProductId: String? = null

    // Clear product data immediately (called when navigating to new product)
    fun clearProduct() {
        _product.value = null
        _imageUrls.value = emptyList()
        _currentImageIndex.value = 0
        _similarProducts.value = emptyList()
        _isInWishlist.value = false
        _error.value = null
        currentProductId = null
        _isLoading.value = true // Set loading to show skeleton
    }
    
    // Clear all state when user signs out
    fun clearState() {
        Log.d(TAG, "Clearing all item detail state")
        clearProduct()
        _isLoading.value = false
        _isSimilarProductsLoading.value = false
        _isFullScreenMode.value = false
    }

    fun toggleSimilarProductWishlist(productId: String) {
        viewModelScope.launch {
            try {
                val similarProduct = _similarProducts.value.find { it.id == productId }
                if (similarProduct != null) {
                    val currentWishlistStatus = similarProduct.isFavorite

                    if (currentWishlistStatus) {
                        repository.removeFromWishlist(productId)
                    } else {
                        repository.addToWishlist(productId)
                    }

                    // Update similar products list immediately for responsive UI
                    _similarProducts.value = _similarProducts.value.map {
                        if (it.id == productId) it.copy(isFavorite = !currentWishlistStatus) else it
                    }

                    Log.d(TAG, "Toggled wishlist for similar product $productId to ${!currentWishlistStatus}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling wishlist for similar product", e)
            }
        }
    }



    // In ItemDetailViewModel.kt - Add these new methods for image navigation and zoom
    fun navigateToImage(index: Int) {
        val imageCount = _imageUrls.value.size
        if (index in 0 until imageCount) {
            _currentImageIndex.value = index
        }
    }



    fun toggleFullScreenMode() {
        _isFullScreenMode.value = !_isFullScreenMode.value
    }

    fun exitFullScreen() {
        _isFullScreenMode.value = false
    }

    fun toggleWishlist() {
        viewModelScope.launch {
            try {
                val currentProduct = _product.value ?: return@launch
                val currentWishlistStatus = _isInWishlist.value

                // Toggle loading state
                _isLoading.value = true

                if (currentWishlistStatus) {
                    // If currently in wishlist, remove it
                    repository.removeFromWishlist(currentProduct.id)
                    _isInWishlist.value = false
                    Log.d(TAG, "Successfully removed product ${currentProduct.id} from wishlist")
                } else {
                    // If not in wishlist, add it
                    repository.addToWishlist(currentProduct.id)
                    _isInWishlist.value = true
                    Log.d(TAG, "Successfully added product ${currentProduct.id} to wishlist")
                }

                // Update similar products if the toggled product is there
                updateSimilarProductWishlistStatus(currentProduct.id, !currentWishlistStatus)
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling wishlist", e)
                _error.value = e.message ?: "An error occurred while updating wishlist"
            } finally {
                _isLoading.value = false
            }
        }
    }


    private fun updateSimilarProductWishlistStatus(productId: String, isFavorite: Boolean) {
        _similarProducts.value = _similarProducts.value.map {
            if (it.id == productId) it.copy(isFavorite = isFavorite) else it
        }
    }



    fun loadSimilarProducts() {
        val currentProduct = product.value ?: return
        viewModelScope.launch {
            try {
                _isSimilarProductsLoading.value = true

                if (currentProduct.categoryId.isBlank()) {
                    _similarProducts.value = emptyList()
                    return@launch
                }

                val similarProductsList = repository.getProductsByCategory(
                    categoryId = currentProduct.categoryId,
                    excludeProductId = currentProduct.id,
                    limit = 10  // Only get 10 products
                ).first()

                _similarProducts.value = similarProductsList
                Log.d(TAG, "✅ Loaded ${similarProductsList.size} similar products")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading similar products", e)
                _similarProducts.value = emptyList()
            } finally {
                _isSimilarProductsLoading.value = false
            }
        }
    }

    private fun trackProductView(productId: String) {
        viewModelScope.launch {
            try {
                repository.addToRecentlyViewed(productId)
                Log.d(TAG, "Tracked view for product: $productId")
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking product view", e)
                // Don't show error to user as this is background functionality
            }
        }
    }

    // UPDATE the existing loadProduct method in ItemDetailViewModel
    fun loadProduct(productId: String) {
        viewModelScope.launch {
            try {
                // Clear old data immediately if this is a different product
                if (currentProductId != null && currentProductId != productId) {
                    _product.value = null
                    _imageUrls.value = emptyList()
                    _currentImageIndex.value = 0
                    _similarProducts.value = emptyList()
                    _isInWishlist.value = false
                    _error.value = null
                }
                
                currentProductId = productId
                _isLoading.value = true
                _error.value = null

                if (productId.isBlank()) {
                    _error.value = "Invalid product ID"
                    return@launch
                }

                coroutineScope {
                    val productDetailsJob = async(Dispatchers.Default) {
                        repository.getProductDetails(productId).first()
                    }

                    val wishlistStatusJob = async(Dispatchers.Default) {
                        repository.isInWishlist(productId)
                    }

                    val productDetails = productDetailsJob.await()
                    val isInWishlist = wishlistStatusJob.await()

                    // Only update if this is still the current product (prevent race conditions)
                    if (currentProductId == productId) {
                        _product.value = productDetails
                        _isInWishlist.value = isInWishlist

                        // Set up image URLs and reset current index
                        val images = productDetails.images

                        _imageUrls.value = images
                        _currentImageIndex.value = 0

                        // Track product view - ADD THIS LINE
                        trackProductView(productId)

                        Log.d(TAG, "Loaded product with ${images.size} images")
                        Log.d(TAG, "Product $productId wishlist status: $isInWishlist")
                    }
                }
            } catch (e: Exception) {
                // Only set error if this is still the current product
                if (currentProductId == productId) {
                    _error.value = e.message ?: "An error occurred while loading the product"
                }
                Log.e(TAG, "Error loading product details", e)
            } finally {
                if (currentProductId == productId) {
                    _isLoading.value = false
                }
            }
        }
    }


}