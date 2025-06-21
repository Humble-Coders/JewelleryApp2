package com.example.jewelleryapp.screen.homeScreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.model.CarouselItem
import com.example.jewelleryapp.model.Category
import com.example.jewelleryapp.model.Collection
import com.example.jewelleryapp.model.Product
import com.example.jewelleryapp.repository.JewelryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: JewelryRepository) : ViewModel() {
    private val tag = "HomeViewModel"

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

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filteredCategories = MutableStateFlow<List<Category>>(emptyList())
    val filteredCategories: StateFlow<List<Category>> = _filteredCategories.asStateFlow()



    // Initialize by loading all data
    init {
        Log.d("HomeViewModel", "HomeViewModel instance created: ${System.currentTimeMillis()}")
        loadData()
    }


    fun toggleSearch() {
        _isSearchActive.value = !_isSearchActive.value
        if (!_isSearchActive.value) {
            _searchQuery.value = ""
            _filteredCategories.value = emptyList()
        } else {
            // Show first 5 categories initially
            _filteredCategories.value = _categories.value.take(5)
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            // Show first 5 categories when query is empty
            _filteredCategories.value = _categories.value.take(5)
        } else {
            // Split query into words and search
            val queryWords = query.split(" ").map { it.trim().lowercase() }

            _filteredCategories.value = _categories.value.filter { category ->
                val categoryWords = category.name.split(" ").map { it.lowercase() }

                // Check if any query word matches any category word
                queryWords.any { queryWord ->
                    categoryWords.any { categoryWord ->
                        categoryWord.contains(queryWord)
                    }
                }
            }
        }
    }


    // In HomeViewModel.kt - Fix the loadData function
    fun loadData() {
        // In HomeViewModel.loadData()
        Log.d("HomeViewModel", "=== LoadData Start ===")
// ... after each job completes

        Log.d("HomeViewModel", "Starting loadData - Firebase connection time: ${System.currentTimeMillis()}")
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
                            Log.e(tag, "Error loading categories", e)
                            errors.add("Categories: ${e.message}")
                            emptyList()
                        }
                    }

                    Log.d("HomeViewModel", "Categories loaded: ${System.currentTimeMillis()}")


                    val featuredProductsJob = async(Dispatchers.Default) {
                        try {
                            repository.getFeaturedProducts().first()
                        } catch (e: Exception) {
                            Log.e(tag, "Error loading featured products", e)
                            errors.add("Featured Products: ${e.message}")
                            emptyList()
                        }
                    }

                    Log.d("HomeViewModel", "FeaturedProducts loaded: ${System.currentTimeMillis()}")


                    val collectionsJob = async(Dispatchers.Default) {
                        try {
                            repository.getThemedCollections().first()
                        } catch (e: Exception) {
                            Log.e(tag, "Error loading collections", e)
                            errors.add("Collections: ${e.message}")
                            emptyList()
                        }
                    }

                    Log.d("HomeViewModel", "Collections loaded: ${System.currentTimeMillis()}")


                    val carouselItemsJob = async(Dispatchers.Default) {
                        try {
                            repository.getCarouselItems().first()
                        } catch (e: Exception) {
                            Log.e(tag, "Error loading carousel items", e)
                            errors.add("Carousel Items: ${e.message}")
                            emptyList()
                        }
                    }

                    Log.d("HomeViewModel", "Carousel loaded: ${System.currentTimeMillis()}")


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
                Log.e(tag, "Failed to load data", e)
                _error.value = "Failed to load data: ${e.message}"
                _isLoading.value = false
            }
        }
        Log.d("HomeViewModel", "=== LoadData Complete ===")
        // In HomeViewModel.loadData()
        Log.d("HomeViewModel", "Data loaded, starting UI update: ${System.currentTimeMillis()}")
    }

    // Function to refresh all data
    fun refreshData() {
        loadData()
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

                Log.d(tag, "Product $productId wishlist status: $isInWishlist")
            } catch (e: Exception) {
                Log.e(tag, "Error checking wishlist status for product $productId", e)
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

                    Log.d(tag, "Toggled favorite for product $productId to ${!isCurrentlyFavorite}")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error toggling favorite for product $productId", e)
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