package com.example.jewelleryapp.screen.productDetailScreen

import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.model.Product
import com.example.jewelleryapp.repository.JewelryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll

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

    // In ItemDetailViewModel.kt - Add this new state flow
    private val _resetZoomTrigger = MutableStateFlow(0)
    val resetZoomTrigger: StateFlow<Int> = _resetZoomTrigger.asStateFlow()

    // Add this method to trigger zoom reset
    fun triggerZoomReset() {
        _resetZoomTrigger.value += 1
        Log.d(TAG, "Zoom reset triggered")
    }


    // In ItemDetailViewModel.kt - Add this method
    fun resetImageZoom() {
        // This will be called from the UI to reset zoom state
        _currentImageIndex.value = 0
        Log.d(TAG, "Resetting image zoom state")
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


    // In ItemDetailViewModel.kt - Update loadProduct method to handle multiple images
    fun loadProduct(productId: String) {
        viewModelScope.launch {
            try {
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

                    _product.value = productDetails
                    _isInWishlist.value = isInWishlist

                    // Set up image URLs and reset current index
                    val images = if (productDetails.imageUrls.isNotEmpty()) {
                        productDetails.imageUrls
                    } else if (productDetails.imageUrl.isNotBlank()) {
                        listOf(productDetails.imageUrl)
                    } else {
                        emptyList()
                    }

                    _imageUrls.value = images
                    _currentImageIndex.value = 0

                    Log.d(TAG, "Loaded product with ${images.size} images")
                    Log.d(TAG, "Product $productId wishlist status: $isInWishlist")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred while loading the product"
                Log.e(TAG, "Error loading product details", e)
            } finally {
                _isLoading.value = false
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

    fun navigateToNextImage() {
        val imageCount = _imageUrls.value.size
        if (imageCount > 1) {
            _currentImageIndex.value = (_currentImageIndex.value + 1) % imageCount
        }
    }

    fun navigateToPreviousImage() {
        val imageCount = _imageUrls.value.size
        if (imageCount > 1) {
            val currentIndex = _currentImageIndex.value
            _currentImageIndex.value = if (currentIndex == 0) imageCount - 1 else currentIndex - 1
        }
    }

    fun toggleFullScreenMode() {
        _isFullScreenMode.value = !_isFullScreenMode.value
    }

    fun exitFullScreen() {
        _isFullScreenMode.value = false
    }

    fun getImageCount(): Int = _imageUrls.value.size

    fun hasMultipleImages(): Boolean = _imageUrls.value.size > 1

    // In ItemDetailViewModel.kt - Add method to get current image URL
    fun getCurrentImageUrl(): String {
        val images = _imageUrls.value
        val currentIndex = _currentImageIndex.value
        return if (images.isNotEmpty() && currentIndex < images.size) {
            images[currentIndex]
        } else {
            ""
        }
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
}