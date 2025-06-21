package com.example.jewelleryapp.screen.itemdetailScreen

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

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Load product details and check wishlist status in parallel
                coroutineScope {
                    // Create async job for product details
                    val productDetailsJob = async(Dispatchers.Default) {
                        repository.getProductDetails(productId).first()
                    }

                    // Create async job for wishlist status check
                    val wishlistStatusJob = async(Dispatchers.Default) {
                        repository.isInWishlist(productId)
                    }

                    // Wait for both operations to complete
                    val productDetails = productDetailsJob.await()
                    val isInWishlist = wishlistStatusJob.await()

                    // Update UI state with the results
                    _product.value = productDetails
                    _isInWishlist.value = isInWishlist

                    Log.d(TAG, "Loaded product with material_id: ${productDetails.materialId}, material_type: ${productDetails.materialType}")
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

    // Add this method to ItemDetailViewModel.kt
    fun toggleSimilarProductWishlist(productId: String) {
        viewModelScope.launch {
            try {
                // Find the product in the similar products list
                val similarProduct = _similarProducts.value.find { it.id == productId }
                if (similarProduct != null) {
                    val currentWishlistStatus = similarProduct.isFavorite

                    _isLoading.value = true

                    if (currentWishlistStatus) {
                        // If currently in wishlist, remove it
                        repository.removeFromWishlist(productId)

                        // Update similar products list with new wishlist status
                        updateSimilarProductWishlistStatus(productId, false)
                        Log.d(TAG, "Successfully removed similar product $productId from wishlist")
                    } else {
                        // If not in wishlist, add it
                        repository.addToWishlist(productId)

                        // Update similar products list with new wishlist status
                        updateSimilarProductWishlistStatus(productId, true)
                        Log.d(TAG, "Successfully added similar product $productId to wishlist")
                    }

                    // If the toggled product is the current product, update its status too
                    if (productId == _product.value?.id) {
                        _isInWishlist.value = !currentWishlistStatus
                    }
                } else {
                    Log.e(TAG, "Similar product $productId not found in similar products list")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling wishlist for similar product", e)
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
                // Log before fetching
                Log.d(TAG, "Loading similar products for category: ${currentProduct.categoryId}")

                // Collect similar products and check wishlist status in parallel
                coroutineScope {
                    val similarProductsJob = async(Dispatchers.Default) {
                        repository.getProductsByCategory(
                            categoryId = currentProduct.categoryId,
                            excludeProductId = currentProduct.id
                        ).first()
                    }

                    // Wait for similar products to be loaded
                    val similarProductsList = similarProductsJob.await()

                    // Check wishlist status for each product in parallel
                    val productsWithWishlistStatus = similarProductsList.map { product ->
                        async(Dispatchers.Default) {
                            try {
                                val isInWishlist = repository.isInWishlist(product.id)
                                product.copy(isFavorite = isInWishlist)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error checking wishlist status for similar product ${product.id}", e)
                                product
                            }
                        }
                    }.awaitAll()

                    // Update the similar products state
                    _similarProducts.value = productsWithWishlistStatus

                    // Log after fetching
                    Log.d(TAG, "Loaded ${productsWithWishlistStatus.size} similar products")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading similar products", e)
                _similarProducts.value = emptyList()
            }
        }
    }
}