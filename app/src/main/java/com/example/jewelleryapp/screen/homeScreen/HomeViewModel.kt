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
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.jewelleryapp.model.GoldSilverRates

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

    private val _goldSilverRates = MutableStateFlow<GoldSilverRates?>(null)
    val goldSilverRates: StateFlow<GoldSilverRates?> = _goldSilverRates.asStateFlow()

    private val _isRatesLoading = MutableStateFlow(false)
    val isRatesLoading: StateFlow<Boolean> = _isRatesLoading.asStateFlow()

    private val _showRatesDialog = MutableStateFlow(false)
    val showRatesDialog: StateFlow<Boolean> = _showRatesDialog.asStateFlow()

    private val _recentlyViewedProducts = MutableStateFlow<List<Product>>(emptyList())
    val recentlyViewedProducts: StateFlow<List<Product>> = _recentlyViewedProducts.asStateFlow()

    private val _isRecentlyViewedLoading = MutableStateFlow(false)
    val isRecentlyViewedLoading: StateFlow<Boolean> = _isRecentlyViewedLoading.asStateFlow()

    private val _isRecentlyViewedLoaded = MutableStateFlow(false)
    val isRecentlyViewedLoaded: StateFlow<Boolean> = _isRecentlyViewedLoaded.asStateFlow()





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
//    fun loadData() {
//        // In HomeViewModel.loadData()
//        Log.d("HomeViewModel", "=== LoadData Start ===")
//// ... after each job completes
//
//        Log.d("HomeViewModel", "Starting loadData - Firebase connection time: ${System.currentTimeMillis()}")
//        _isLoading.value = true
//        _error.value = null
//
//        viewModelScope.launch {
//            try {
//                // Create a list to hold any errors that occur
//                val errors = mutableListOf<String>()
//
//                // Launch all data loading operations in parallel using coroutineScope
//                coroutineScope {
//                    // Create separate async jobs for each data type
//                    val categoriesJob = async(Dispatchers.Default) {
//                        try {
//                            repository.getCategories().first()
//                        } catch (e: Exception) {
//                            Log.e(tag, "Error loading categories", e)
//                            errors.add("Categories: ${e.message}")
//                            emptyList()
//                        }
//                    }
//
//                    Log.d("HomeViewModel", "Categories loaded: ${System.currentTimeMillis()}")
//
//
//                    val featuredProductsJob = async(Dispatchers.Default) {
//                        try {
//                            repository.getFeaturedProducts().first()
//                        } catch (e: Exception) {
//                            Log.e(tag, "Error loading featured products", e)
//                            errors.add("Featured Products: ${e.message}")
//                            emptyList()
//                        }
//                    }
//
//                    Log.d("HomeViewModel", "FeaturedProducts loaded: ${System.currentTimeMillis()}")
//
//
//                    val collectionsJob = async(Dispatchers.Default) {
//                        try {
//                            repository.getThemedCollections().first()
//                        } catch (e: Exception) {
//                            Log.e(tag, "Error loading collections", e)
//                            errors.add("Collections: ${e.message}")
//                            emptyList()
//                        }
//                    }
//
//                    Log.d("HomeViewModel", "Collections loaded: ${System.currentTimeMillis()}")
//
//
//                    val carouselItemsJob = async(Dispatchers.Default) {
//                        try {
//                            repository.getCarouselItems().first()
//                        } catch (e: Exception) {
//                            Log.e(tag, "Error loading carousel items", e)
//                            errors.add("Carousel Items: ${e.message}")
//                            emptyList()
//                        }
//                    }
//
//                    Log.d("HomeViewModel", "Carousel loaded: ${System.currentTimeMillis()}")
//
//
//                    // Wait for all jobs to complete and update UI state
//                    val categories = categoriesJob.await()
//                    val featuredProducts = featuredProductsJob.await()
//                    val collections = collectionsJob.await()
//                    val carouselItems = carouselItemsJob.await()
//
//                    // Update all UI states at once
//                    _categories.value = categories
//                    _featuredProducts.value = featuredProducts
//                    _collections.value = collections
//                    _carouselItems.value = carouselItems
//                }
//
//                // If there are any errors, set the error message
//                if (errors.isNotEmpty()) {
//                    // Only show the error if we couldn't load anything at all
//                    if (_categories.value.isEmpty() &&
//                        _featuredProducts.value.isEmpty() &&
//                        _collections.value.isEmpty() &&
//                        _carouselItems.value.isEmpty()) {
//                        _error.value = "Failed to load data. Please check your connection."
//                    }
//                } else {
//                    _error.value = null
//                }
//
//                _isLoading.value = false
//            } catch (e: Exception) {
//                Log.e(tag, "Failed to load data", e)
//                _error.value = "Failed to load data: ${e.message}"
//                _isLoading.value = false
//            }
//        }
//        Log.d("HomeViewModel", "=== LoadData Complete ===")
//        // In HomeViewModel.loadData()
//        Log.d("HomeViewModel", "Data loaded, starting UI update: ${System.currentTimeMillis()}")
//    }

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

    fun openGoogleMaps(context: Context) {
        try {
            // You can use your store coordinates or a general location
            val latitude = 30.3398  // Patiala coordinates
            val longitude = 76.3869
            val storeName = "Gagan Jewellers"

            val geoUri = "geo:$latitude,$longitude?q=$latitude,$longitude($storeName)"
            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
            mapIntent.setPackage("com.google.android.apps.maps")

            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Fallback to browser if Google Maps not installed
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$latitude,$longitude"))
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error opening maps", e)
        }
    }

    fun openWhatsApp(context: Context) {
        try {
            val phoneNumber = "8194963318" // Your WhatsApp number without + and spaces
            val message = "Hi! I'm interested in your jewelry collection. Please help me with more details."

            // Try WhatsApp first
            val whatsappIntent = Intent(Intent.ACTION_VIEW)
            whatsappIntent.data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}")
            whatsappIntent.setPackage("com.whatsapp")

            if (whatsappIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(whatsappIntent)
            } else {
                // Try WhatsApp Business
                val whatsappBusinessIntent = Intent(Intent.ACTION_VIEW)
                whatsappBusinessIntent.data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}")
                whatsappBusinessIntent.setPackage("com.whatsapp.w4b")

                if (whatsappBusinessIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(whatsappBusinessIntent)
                } else {
                    // Fallback to browser
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"))
                    context.startActivity(browserIntent)
                }
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error opening WhatsApp", e)
        }
    }

    fun loadGoldSilverRates() {
        viewModelScope.launch {
            try {
                _isRatesLoading.value = true
                val rates = repository.getGoldSilverRates().first()
                _goldSilverRates.value = rates
            } catch (e: Exception) {
                Log.e(tag, "Error loading gold silver rates", e)
            } finally {
                _isRatesLoading.value = false
            }
        }
    }

    fun showRatesDialog() {
        _showRatesDialog.value = true
        loadGoldSilverRates()
    }

    fun hideRatesDialog() {
        _showRatesDialog.value = false
    }

    private fun loadRecentlyViewedProducts() {
        viewModelScope.launch {
            try {
                _isRecentlyViewedLoading.value = true
                repository.getRecentlyViewedProducts().collect { products ->
                    _recentlyViewedProducts.value = products
                    Log.d(tag, "Loaded ${products.size} recently viewed products")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error loading recently viewed products", e)
                _recentlyViewedProducts.value = emptyList()
            } finally {
                _isRecentlyViewedLoading.value = false
            }
        }
    }

    // UPDATE the existing loadData method in HomeViewModel to include recently viewed
    fun loadData() {
        Log.d("HomeViewModel", "=== LoadData Start ===")
        Log.d("HomeViewModel", "Starting loadData - Firebase connection time: ${System.currentTimeMillis()}")
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val errors = mutableListOf<String>()

                coroutineScope {
                    val categoriesJob = async(Dispatchers.Default) {
                        try {
                            repository.getCategories().first()
                        } catch (e: Exception) {
                            Log.e(tag, "Error loading categories", e)
                            errors.add("Categories: ${e.message}")
                            emptyList()
                        }
                    }

                    val featuredProductsJob = async(Dispatchers.Default) {
                        try {
                            repository.getFeaturedProducts().first()
                        } catch (e: Exception) {
                            Log.e(tag, "Error loading featured products", e)
                            errors.add("Featured Products: ${e.message}")
                            emptyList()
                        }
                    }

                    val collectionsJob = async(Dispatchers.Default) {
                        try {
                            repository.getThemedCollections().first()
                        } catch (e: Exception) {
                            Log.e(tag, "Error loading collections", e)
                            errors.add("Collections: ${e.message}")
                            emptyList()
                        }
                    }

                    val carouselItemsJob = async(Dispatchers.Default) {
                        try {
                            repository.getCarouselItems().first()
                        } catch (e: Exception) {
                            Log.e(tag, "Error loading carousel items", e)
                            errors.add("Carousel Items: ${e.message}")
                            emptyList()
                        }
                    }

                    // ADD this new job for recently viewed
                    val recentlyViewedJob = async(Dispatchers.Default) {
                        try {
                            repository.getRecentlyViewedProducts().first()
                        } catch (e: Exception) {
                            Log.e(tag, "Error loading recently viewed", e)
                            emptyList()
                        }
                    }

                    // Wait for all jobs and update UI state
                    val categories = categoriesJob.await()
                    val featuredProducts = featuredProductsJob.await()
                    val collections = collectionsJob.await()
                    val carouselItems = carouselItemsJob.await()
                    val recentlyViewed = recentlyViewedJob.await() // ADD this line

                    // Update all UI states at once
                    _categories.value = categories
                    _featuredProducts.value = featuredProducts
                    _collections.value = collections
                    _carouselItems.value = carouselItems
                    _recentlyViewedProducts.value = recentlyViewed // ADD this line
                }

                if (errors.isNotEmpty()) {
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
    }

    // Add this method to refresh recently viewed when returning from product detail
    fun refreshRecentlyViewed() {
        loadRecentlyViewedProducts()
    }

    // Add this method to handle recently viewed product clicks
    fun onRecentlyViewedProductClick(productId: String) {
        // The click will navigate to product detail, which will automatically track the view
        Log.d(tag, "Recently viewed product clicked: $productId")
    }

    // Add this method to toggle wishlist for recently viewed products
    fun toggleRecentlyViewedFavorite(productId: String) {
        viewModelScope.launch {
            try {
                val currentProduct = _recentlyViewedProducts.value.find { it.id == productId }
                if (currentProduct != null) {
                    val isCurrentlyFavorite = currentProduct.isFavorite

                    if (isCurrentlyFavorite) {
                        repository.removeFromWishlist(productId)
                    } else {
                        repository.addToWishlist(productId)
                    }

                    // Update recently viewed list with new favorite status
                    _recentlyViewedProducts.value = _recentlyViewedProducts.value.map { product ->
                        if (product.id == productId) {
                            product.copy(isFavorite = !isCurrentlyFavorite)
                        } else {
                            product
                        }
                    }

                    // Also update featured products list if product exists there
                    updateProductFavoriteStatus(productId, !isCurrentlyFavorite)

                    Log.d(tag, "Toggled favorite for recently viewed product $productId to ${!isCurrentlyFavorite}")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error toggling favorite for recently viewed product $productId", e)
            }
        }
    }
}