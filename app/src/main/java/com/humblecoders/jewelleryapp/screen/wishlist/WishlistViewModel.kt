package com.humblecoders.jewelleryapp.screen.wishlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.jewelleryapp.model.Category
import com.humblecoders.jewelleryapp.model.Product
import com.humblecoders.jewelleryapp.model.WishlistChange
import com.humblecoders.jewelleryapp.repository.JewelryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

class WishlistViewModel(private val repository: JewelryRepository) : ViewModel() {
    private val _wishlistItems = MutableStateFlow<List<Product>>(emptyList())
    val wishlistItems: StateFlow<List<Product>> = _wishlistItems

    private val _selectedCategory = MutableStateFlow<String>("All")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _categoryProducts = MutableStateFlow<List<Product>>(emptyList())
    private val _allWishlistItems = MutableStateFlow<List<Product>>(emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Flag to track if data has been loaded at least once
    private var hasLoadedData = false
    
    // Job to track the listener coroutine so we can cancel it
    private var listenerJob: kotlinx.coroutines.Job? = null

    init {
        Log.d("WishlistViewModel", "Initializing WishlistViewModel")
        // Load categories
        viewModelScope.launch {
            loadCategories()
        }
        // Start listening to wishlist changes
        startWishlistListener()
    }

    fun refreshWishlistItems(forceRefresh: Boolean = false) {
        // The real-time listener handles updates automatically
        // This method is kept for backward compatibility but doesn't need to do anything
        // since the listener is always active
        if (!forceRefresh && hasLoadedData) {
            Log.d("WishlistViewModel", "Wishlist listener is active, no manual refresh needed")
            return
        }
        
        // If forcing refresh, we can trigger a cache refresh
        if (forceRefresh) {
            viewModelScope.launch {
                try {
                    repository.refreshWishlistCache()
                    Log.d("WishlistViewModel", "Cache refreshed")
                } catch (e: Exception) {
                    Log.e("WishlistViewModel", "Error refreshing cache", e)
                }
            }
        }
    }

    private fun loadCategoryProducts(categoryId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getCategoryProducts(categoryId).collect { products ->
                    Log.d("WishlistViewModel", "Loaded ${products.size} products for category: $categoryId")
                    _categoryProducts.value = products
                    filterWishlistItems()
                }
            } catch (e: Exception) {
                Log.e("WishlistViewModel", "Failed to load category products", e)
                _error.value = "Failed to load category products: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadCategories() {
        try {
            repository.getCategories().collect { fetchedCategories ->
                _categories.value = fetchedCategories
                Log.d("WishlistViewModel", "Loaded ${fetchedCategories.size} categories")
            }
        } catch (e: Exception) {
            _error.value = "Failed to load categories: ${e.message}"
            Log.e("WishlistViewModel", "Failed to load categories", e)
        }
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
        if (category != "All") {
            loadCategoryProducts(category)
        } else {
            _categoryProducts.value = emptyList()
            filterWishlistItems()
        }
    }

    // Start listening to real-time wishlist changes
    private fun startWishlistListener() {
        // Cancel previous listener if it exists
        listenerJob?.cancel()
        
        listenerJob = viewModelScope.launch {
            try {
                if (!hasLoadedData) {
                    _isLoading.value = true
                }
                _error.value = null

                Log.d("WishlistViewModel", "Starting wishlist real-time listener")
                
                // Add timeout to prevent infinite loading
                val timeoutJob = launch {
                    delay(10000) // 10 second timeout
                    if (!hasLoadedData && _isLoading.value) {
                        Log.w("WishlistViewModel", "Wishlist listener timeout - stopping loading")
                        _isLoading.value = false
                        _error.value = "Failed to load wishlist: Connection timeout"
                    }
                }

                repository.getWishlistChanges().collect { change ->
                    // Cancel timeout once we receive first change
                    timeoutJob.cancel()
                    when (change) {
                        is WishlistChange.InitialLoad -> {
                            Log.d("WishlistViewModel", "Initial load: ${change.productIds.size} items")
                            if (change.productIds.isNotEmpty()) {
                                // Fetch all products for initial load
                                val products = repository.fetchProductsByIds(change.productIds)
                                val productsWithFavorite = products.map { it.copy(isFavorite = true) }
                                _allWishlistItems.value = productsWithFavorite
                                _wishlistItems.value = productsWithFavorite
                                
                                if (_selectedCategory.value != "All") {
                                    filterWishlistItems()
                                }
                                
                                hasLoadedData = true
                                _isLoading.value = false
                                Log.d("WishlistViewModel", "Initial load complete: ${productsWithFavorite.size} items")
                            } else {
                                _allWishlistItems.value = emptyList()
                                _wishlistItems.value = emptyList()
                                hasLoadedData = true
                                _isLoading.value = false
                                Log.d("WishlistViewModel", "Initial load: wishlist is empty")
                            }
                        }
                        is WishlistChange.Added -> {
                            Log.d("WishlistViewModel", "Item added: ${change.productId}")
                            // Fetch only the new product (read-optimized)
                            val product = repository.fetchProductById(change.productId)
                            if (product != null) {
                                val productWithFavorite = product.copy(isFavorite = true)
                                // Add to list if not already present
                                val currentList = _allWishlistItems.value.toMutableList()
                                if (currentList.none { it.id == change.productId }) {
                                    currentList.add(productWithFavorite)
                                    _allWishlistItems.value = currentList
                                    
                                    // Update filtered list if needed
                                    if (_selectedCategory.value == "All" || 
                                        productWithFavorite.categoryId == _selectedCategory.value) {
                                        val filteredList = _wishlistItems.value.toMutableList()
                                        if (filteredList.none { it.id == change.productId }) {
                                            filteredList.add(productWithFavorite)
                                            _wishlistItems.value = filteredList
                                        }
                                    } else {
                                        filterWishlistItems()
                                    }
                                    Log.d("WishlistViewModel", "Added product: ${product.name}")
                                }
                            } else {
                                Log.e("WishlistViewModel", "Failed to fetch added product: ${change.productId}")
                            }
                        }
                        is WishlistChange.Removed -> {
                            Log.d("WishlistViewModel", "Item removed: ${change.productId}")
                            // Remove from list (read-optimized - no fetch needed)
                            val currentList = _allWishlistItems.value.toMutableList()
                            currentList.removeAll { it.id == change.productId }
                            _allWishlistItems.value = currentList
                            
                            // Update filtered list
                            val filteredList = _wishlistItems.value.toMutableList()
                            filteredList.removeAll { it.id == change.productId }
                            _wishlistItems.value = filteredList
                            
                            Log.d("WishlistViewModel", "Removed product: ${change.productId}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("WishlistViewModel", "Error in wishlist listener", e)
                _error.value = "Failed to load wishlist: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun filterWishlistItems() {
        viewModelScope.launch(Dispatchers.Default) {
            val wishlistIds = _allWishlistItems.value.map { it.id }.toSet()
            Log.d("WishlistViewModel", "Filtering items. Total wishlist items: ${wishlistIds.size}")

            val filteredItems = when (_selectedCategory.value) {
                "All" -> _allWishlistItems.value
                else -> _categoryProducts.value.filter { it.id in wishlistIds }
            }

            Log.d("WishlistViewModel", "Filtered to ${filteredItems.size} items for category: ${_selectedCategory.value}")
            _wishlistItems.value = filteredItems
        }
    }

    fun removeFromWishlist(productId: String) {
        viewModelScope.launch {
            try {
                Log.d("WishlistViewModel", "Removing product $productId from wishlist")

                // Optimistic update - remove from UI immediately
                val currentList = _allWishlistItems.value.toMutableList()
                currentList.removeAll { it.id == productId }
                _allWishlistItems.value = currentList
                
                val filteredList = _wishlistItems.value.toMutableList()
                filteredList.removeAll { it.id == productId }
                _wishlistItems.value = filteredList

                // Call repository to remove from wishlist
                // The real-time listener will confirm the removal
                repository.removeFromWishlist(productId)

                Log.d("WishlistViewModel", "Successfully removed product from wishlist")
            } catch (e: Exception) {
                Log.e("WishlistViewModel", "Failed to remove from wishlist", e)
                _error.value = "Failed to remove from wishlist: ${e.message}"
                // Revert optimistic update on error by refreshing
                refreshWishlistItems(forceRefresh = true)
            }
        }
    }
    
    // Clear all state when user signs out
    fun clearAllState() {
        Log.d("WishlistViewModel", "Clearing all wishlist state")
        // Cancel the listener
        listenerJob?.cancel()
        listenerJob = null
        
        _wishlistItems.value = emptyList()
        _allWishlistItems.value = emptyList()
        _categoryProducts.value = emptyList()
        _categories.value = emptyList()
        _selectedCategory.value = "All"
        _isLoading.value = false
        _error.value = null
        hasLoadedData = false
    }
    
    // Restart listener with new user ID
    fun restartListener() {
        Log.d("WishlistViewModel", "Restarting wishlist listener with new user")
        // Clear old state first
        clearAllState()
        // Restart listener
        hasLoadedData = false
        startWishlistListener()
    }
}