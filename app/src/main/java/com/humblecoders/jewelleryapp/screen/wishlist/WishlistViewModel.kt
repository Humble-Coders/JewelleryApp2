package com.humblecoders.jewelleryapp.screen.wishlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.jewelleryapp.model.Category
import com.humblecoders.jewelleryapp.model.Product
import com.humblecoders.jewelleryapp.repository.JewelryRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope

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

    init {
        Log.d("WishlistViewModel", "Initializing WishlistViewModel")
        // Load categories and wishlist items in parallel
        viewModelScope.launch {
            coroutineScope {
                val categoriesJob = async(Dispatchers.Default) {
                    loadCategories()
                }
                val wishlistJob = async(Dispatchers.Default) {
                    refreshWishlistItems()
                }

                // Await both operations
                categoriesJob.await()
                wishlistJob.await()
            }
        }
    }

    fun refreshWishlistItems() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null  // Clear any previous errors

                Log.d("WishlistViewModel", "Refreshing wishlist items")

                // Load wishlist items
                loadWishlistItems()

                Log.d("WishlistViewModel", "Refresh complete")
            } catch (e: Exception) {
                Log.e("WishlistViewModel", "Error during refresh", e)
                _error.value = "Failed to load wishlist: ${e.message}"
            } finally {
                _isLoading.value = false
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

    private suspend fun loadWishlistItems() {
        try {
            _isLoading.value = true
            _error.value = null  // Clear any previous errors

            Log.d("WishlistViewModel", "Starting to load wishlist items")

            // Force refresh of wishlist cache first for better consistency
            coroutineScope {
                val refreshCacheJob = async(Dispatchers.Default) {
                    repository.refreshWishlistCache()
                }
                refreshCacheJob.await()
            }

            repository.getWishlistItems().collect { items ->
                Log.d("WishlistViewModel", "Received ${items.size} wishlist items")

                if (items.isEmpty()) {
                    Log.d("WishlistViewModel", "Wishlist is empty")
                }

                items.forEach { product ->
                    Log.d("WishlistViewModel", "Product: id=${product.id}, name=${product.name}, category=${product.category}, imageUrl=${product.imageUrl}")
                }

                _allWishlistItems.value = items
                _wishlistItems.value = items  // Update immediately for All category

                if (_selectedCategory.value != "All") {
                    filterWishlistItems()
                }
            }
        } catch (e: Exception) {
            Log.e("WishlistViewModel", "Failed to load wishlist items", e)
            _error.value = "Failed to load wishlist items: ${e.message}"
            _wishlistItems.value = emptyList()
            _allWishlistItems.value = emptyList()
        } finally {
            _isLoading.value = false
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
                _isLoading.value = true
                Log.d("WishlistViewModel", "Removing product $productId from wishlist")

                // Call repository to remove from wishlist
                repository.removeFromWishlist(productId)

                // Remove from local lists immediately for responsive UI
                _allWishlistItems.value = _allWishlistItems.value.filter { it.id != productId }
                _wishlistItems.value = _wishlistItems.value.filter { it.id != productId }

                Log.d("WishlistViewModel", "Successfully removed product from wishlist")
            } catch (e: Exception) {
                Log.e("WishlistViewModel", "Failed to remove from wishlist", e)
                _error.value = "Failed to remove from wishlist: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}