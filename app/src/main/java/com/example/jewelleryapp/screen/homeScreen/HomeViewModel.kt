package com.example.jewelleryapp.screen.homeScreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.model.CarouselItem
import com.example.jewelleryapp.model.Category
import com.example.jewelleryapp.model.Collection
import com.example.jewelleryapp.model.Product
import com.example.jewelleryapp.repository.JewelryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.flow.first

class HomeViewModel(private val repository: JewelryRepository) : ViewModel() {
    private val TAG = "HomeViewModel"

    // StateFlows to hold data for UI
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _featuredProducts = MutableStateFlow<List<Product>>(emptyList())
    val featuredProducts: StateFlow<List<Product>> = _featuredProducts.asStateFlow()

    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    val collections: StateFlow<List<Collection>> = _collections.asStateFlow()

    private val _carouselItems = MutableStateFlow<List<CarouselItem>>(emptyList())
    val carouselItems: StateFlow<List<CarouselItem>> = _carouselItems.asStateFlow()

    private val _recentlyViewedProducts = MutableStateFlow<List<Product>>(emptyList())
    val recentlyViewedProducts: StateFlow<List<Product>> = _recentlyViewedProducts.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Initialize by loading all data
    init {
        loadData()
    }

    // In HomeViewModel.kt - Fix the loadData function
    fun loadData() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                // Create a list to hold any errors that occur
                val errors = mutableListOf<String>()

                // Launch all data loading operations in parallel using coroutineScope
                coroutineScope {
                    // Create separate async jobs for each data type
                    val categoriesJob = async(Dispatchers.Default) {
                        try {
                            repository.getCategories().first()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading categories", e)
                            errors.add("Categories: ${e.message}")
                            emptyList()
                        }
                    }

                    val featuredProductsJob = async(Dispatchers.Default) {
                        try {
                            repository.getFeaturedProducts().first()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading featured products", e)
                            errors.add("Featured Products: ${e.message}")
                            emptyList()
                        }
                    }

                    val collectionsJob = async(Dispatchers.Default) {
                        try {
                            repository.getThemedCollections().first()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading collections", e)
                            errors.add("Collections: ${e.message}")
                            emptyList()
                        }
                    }

                    val carouselItemsJob = async(Dispatchers.Default) {
                        try {
                            repository.getCarouselItems().first()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading carousel items", e)
                            errors.add("Carousel Items: ${e.message}")
                            emptyList()
                        }
                    }

                    // Wait for all jobs to complete and update UI state
                    val categories = categoriesJob.await()
                    val featuredProducts = featuredProductsJob.await()
                    val collections = collectionsJob.await()
                    val carouselItems = carouselItemsJob.await()

                    // Update all UI states at once
                    _categories.value = categories
                    _featuredProducts.value = featuredProducts
                    _collections.value = collections
                    _carouselItems.value = carouselItems
                }

                // If there are any errors, set the error message
                if (errors.isNotEmpty()) {
                    // Only show the error if we couldn't load anything at all
                    if (_categories.value.isEmpty() &&
                        _featuredProducts.value.isEmpty() &&
                        _collections.value.isEmpty() &&
                        _carouselItems.value.isEmpty()) {
                        _error.value = "Failed to load data. Please check your connection."
                    }
                } else {
                    _error.value = null
                }

                _isLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load data", e)
                _error.value = "Failed to load data: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // Function to refresh all data
    fun refreshData() {
        loadData()
    }

    // Called when a product is viewed
    fun recordProductView(userId: String, productId: String) {
        viewModelScope.launch {
            try {
                repository.recordProductView(userId, productId)
            } catch (e: Exception) {
                Log.e(TAG, "Error recording product view", e)
            }
        }
    }

    // New optimized function to update all products with current wishlist status in parallel
    private suspend fun updateProductsWithWishlistStatus(products: List<Product>) {
        try {
            val productsWithWishlistStatus = coroutineScope {
                products.map { product ->
                    async(Dispatchers.Default) {
                        try {
                            val isInWishlist = repository.isInWishlist(product.id)
                            product.copy(isFavorite = isInWishlist)
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Error checking wishlist status for product ${product.id}",
                                e
                            )
                            product
                        }
                    }
                }.awaitAll()
            }

            _featuredProducts.value = productsWithWishlistStatus
            Log.d(TAG, "Updated wishlist status for ${products.size} products")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating wishlist status for products", e)
        }
    }

    // Check if a product is in wishlist
    fun checkWishlistStatus(productId: String) {
        viewModelScope.launch {
            try {
                val isInWishlist = repository.isInWishlist(productId)

                // Update the featured products list with the current wishlist status
                _featuredProducts.value = _featuredProducts.value.map { product ->
                    if (product.id == productId) {
                        product.copy(isFavorite = isInWishlist)
                    } else {
                        product
                    }
                }

                // Also update recently viewed products if needed
                _recentlyViewedProducts.value = _recentlyViewedProducts.value.map { product ->
                    if (product.id == productId) {
                        product.copy(isFavorite = isInWishlist)
                    } else {
                        product
                    }
                }

                Log.d(TAG, "Product $productId wishlist status: $isInWishlist")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking wishlist status for product $productId", e)
            }
        }
    }

    // Toggle favorite status and update in repository
    fun toggleFavorite(productId: String) {
        viewModelScope.launch {
            try {
                // Get current favorite status from our list
                val currentProduct = _featuredProducts.value.find { it.id == productId }
                    ?: _recentlyViewedProducts.value.find { it.id == productId }

                if (currentProduct != null) {
                    val isCurrentlyFavorite = currentProduct.isFavorite

                    // Toggle in repository
                    if (isCurrentlyFavorite) {
                        repository.removeFromWishlist(productId)
                    } else {
                        repository.addToWishlist(productId)
                    }

                    // Update UI state immediately for responsive feedback
                    updateProductFavoriteStatus(productId, !isCurrentlyFavorite)

                    Log.d(TAG, "Toggled favorite for product $productId to ${!isCurrentlyFavorite}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling favorite for product $productId", e)
            }
        }
    }

    // Helper function to update favorite status in state
    private fun updateProductFavoriteStatus(productId: String, isFavorite: Boolean) {
        // Update featured products
        _featuredProducts.value = _featuredProducts.value.map { product ->
            if (product.id == productId) {
                product.copy(isFavorite = isFavorite)
            } else {
                product
            }
        }

        // Update recently viewed products
        _recentlyViewedProducts.value = _recentlyViewedProducts.value.map { product ->
            if (product.id == productId) {
                product.copy(isFavorite = isFavorite)
            } else {
                product
            }
        }
    }
}