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
import kotlinx.coroutines.delay

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
            _filteredCategories.value = _categories.value.take(5)
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            _filteredCategories.value = _categories.value.take(5)
        } else {
            val queryWords = query.split(" ").map { it.trim().lowercase() }

            _filteredCategories.value = _categories.value.filter { category ->
                val categoryWords = category.name.split(" ").map { it.lowercase() }
                queryWords.any { queryWord ->
                    categoryWords.any { categoryWord ->
                        categoryWord.contains(queryWord)
                    }
                }
            }
        }
    }

    fun loadData() {
        Log.d("HomeViewModel", "=== LoadData Start ===")
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

                    // Wait for all jobs and update UI state
                    val categories = categoriesJob.await()
                    val featuredProducts = featuredProductsJob.await()
                    val collections = collectionsJob.await()
                    val carouselItems = carouselItemsJob.await()

                    // Update UI states for non-user dependent data first
                    _categories.value = categories
                    _featuredProducts.value = featuredProducts
                    _collections.value = collections
                    _carouselItems.value = carouselItems
                }

                // Load recently viewed products separately with delay to ensure user auth is ready
                loadRecentlyViewedWithDelay()

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

    private fun loadRecentlyViewedWithDelay() {
        viewModelScope.launch {
            try {
                // Small delay to ensure Firebase Auth is ready
                delay(500)

                _isRecentlyViewedLoading.value = true

                // Refresh wishlist cache first to ensure accurate favorite status
                repository.refreshWishlistCache()

                // Use collect instead of first() to handle flow properly
                repository.getRecentlyViewedProducts().collect { recentlyViewed ->
                    _recentlyViewedProducts.value = recentlyViewed
                    Log.d(tag, "Loaded ${recentlyViewed.size} recently viewed products with delay")
                    _isRecentlyViewedLoading.value = false
                }

            } catch (e: Exception) {
                Log.e(tag, "Error loading recently viewed products with delay", e)
                _recentlyViewedProducts.value = emptyList()
                _isRecentlyViewedLoading.value = false
            }
        }
    }

    fun refreshData() {
        loadData()
    }

    fun checkWishlistStatus(productId: String) {
        viewModelScope.launch {
            try {
                val isInWishlist = repository.isInWishlist(productId)

                _featuredProducts.value = _featuredProducts.value.map { product ->
                    if (product.id == productId) {
                        product.copy(isFavorite = isInWishlist)
                    } else {
                        product
                    }
                }

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

    fun toggleFavorite(productId: String) {
        viewModelScope.launch {
            try {
                val currentProduct = _featuredProducts.value.find { it.id == productId }
                    ?: _recentlyViewedProducts.value.find { it.id == productId }

                if (currentProduct != null) {
                    val isCurrentlyFavorite = currentProduct.isFavorite

                    if (isCurrentlyFavorite) {
                        repository.removeFromWishlist(productId)
                    } else {
                        repository.addToWishlist(productId)
                    }

                    updateProductFavoriteStatus(productId, !isCurrentlyFavorite)
                    Log.d(tag, "Toggled favorite for product $productId to ${!isCurrentlyFavorite}")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error toggling favorite for product $productId", e)
            }
        }
    }

    private fun updateProductFavoriteStatus(productId: String, isFavorite: Boolean) {
        _featuredProducts.value = _featuredProducts.value.map { product ->
            if (product.id == productId) {
                product.copy(isFavorite = isFavorite)
            } else {
                product
            }
        }

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
            val latitude = 30.3398
            val longitude = 76.3869
            val storeName = "Gagan Jewellers"

            val geoUri = "geo:$latitude,$longitude?q=$latitude,$longitude($storeName)"
            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
            mapIntent.setPackage("com.google.android.apps.maps")

            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$latitude,$longitude"))
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error opening maps", e)
        }
    }

    fun openWhatsApp(context: Context) {
        try {
            val phoneNumber = "8194963318"
            val message = "Hi! I'm interested in your jewelry collection. Please help me with more details."

            val whatsappIntent = Intent(Intent.ACTION_VIEW)
            whatsappIntent.data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}")
            whatsappIntent.setPackage("com.whatsapp")

            if (whatsappIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(whatsappIntent)
            } else {
                val whatsappBusinessIntent = Intent(Intent.ACTION_VIEW)
                whatsappBusinessIntent.data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}")
                whatsappBusinessIntent.setPackage("com.whatsapp.w4b")

                if (whatsappBusinessIntent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(whatsappBusinessIntent)
                } else {
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

    // This method is called when returning from other screens
    fun refreshRecentlyViewed() {
        viewModelScope.launch {
            try {
                _isRecentlyViewedLoading.value = true

                // Refresh wishlist cache to get latest favorite status
                repository.refreshWishlistCache()

                // Use collect instead of first() to handle flow properly
                repository.getRecentlyViewedProducts().collect { recentlyViewed ->
                    _recentlyViewedProducts.value = recentlyViewed
                    Log.d(tag, "Refreshed ${recentlyViewed.size} recently viewed products")
                    _isRecentlyViewedLoading.value = false
                }

            } catch (e: Exception) {
                Log.e(tag, "Error refreshing recently viewed products", e)
                _recentlyViewedProducts.value = emptyList()
                _isRecentlyViewedLoading.value = false
            }
        }
    }

    fun onRecentlyViewedProductClick(productId: String) {
        Log.d(tag, "Recently viewed product clicked: $productId")
    }

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

                    _recentlyViewedProducts.value = _recentlyViewedProducts.value.map { product ->
                        if (product.id == productId) {
                            product.copy(isFavorite = !isCurrentlyFavorite)
                        } else {
                            product
                        }
                    }

                    updateProductFavoriteStatus(productId, !isCurrentlyFavorite)
                    Log.d(tag, "Toggled favorite for recently viewed product $productId to ${!isCurrentlyFavorite}")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error toggling favorite for recently viewed product $productId", e)
            }
        }
    }
}