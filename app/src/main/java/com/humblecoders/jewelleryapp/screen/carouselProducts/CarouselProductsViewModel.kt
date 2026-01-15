package com.humblecoders.jewelleryapp.screen.carouselProducts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.humblecoders.jewelleryapp.model.CategoryProductsState
import com.humblecoders.jewelleryapp.model.FilterSortState
import com.humblecoders.jewelleryapp.model.Product
import com.humblecoders.jewelleryapp.model.SortOption
import com.humblecoders.jewelleryapp.repository.JewelryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@OptIn(FlowPreview::class)
class CarouselProductsViewModel(
    private val repository: JewelryRepository,
    private val productIds: List<String>
) : ViewModel() {
    private val tag = "CarouselProductsVM"

    private val _state = MutableStateFlow(CategoryProductsState())
    val state: StateFlow<CategoryProductsState> = _state.asStateFlow()

    private val _filterSortState = MutableStateFlow(FilterSortState())
    val filterSortState: StateFlow<FilterSortState> = _filterSortState.asStateFlow()

    // Separate state for display count (filtered products count)
    private val _displayProductsCount = MutableStateFlow(0)
    val displayProductsCount: StateFlow<Int> = _displayProductsCount.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    // Category filtering
    private val _categories = MutableStateFlow<List<com.humblecoders.jewelleryapp.model.Category>>(emptyList())
    val categories: StateFlow<List<com.humblecoders.jewelleryapp.model.Category>> = _categories.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    // Cache for loaded products
    private val loadedProducts = mutableListOf<Product>()

    // Clear all state when user signs out
    fun clearState() {
        Log.d(tag, "Clearing carousel products state")
        _state.value = CategoryProductsState()
        _filterSortState.value = FilterSortState()
        _displayProductsCount.value = 0
        _searchQuery.value = ""
        _isSearchActive.value = false
        _categories.value = emptyList()
        _selectedCategoryId.value = null
        loadedProducts.clear()
    }

    init {
        Log.d(tag, "Initializing CarouselProductsViewModel with ${productIds.size} product IDs")
        loadInitialData()
        setupSearchDebounce()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true, error = null)

                coroutineScope {
                    val materialsJob = async(Dispatchers.Default) {
                        loadAvailableMaterials()
                    }

                    val categoriesJob = async(Dispatchers.Default) {
                        loadCategories()
                    }

                    val productsJob = async(Dispatchers.Default) {
                        loadCarouselProducts()
                    }

                    materialsJob.await()
                    categoriesJob.await()
                    productsJob.await()
                }

            } catch (e: Exception) {
                Log.e(tag, "Error loading initial data", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load products: ${e.message}"
                )
            }
        }
    }

    private suspend fun loadCategories() {
        try {
            repository.getCategories().collect { fetchedCategories ->
                _categories.value = fetchedCategories
                Log.d(tag, "Loaded ${fetchedCategories.size} categories")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error loading categories", e)
        }
    }

    private suspend fun loadAvailableMaterials() {
        try {
            repository.getAvailableMaterials().collect { materials ->
                _filterSortState.value = _filterSortState.value.copy(availableMaterials = materials)
                Log.d(tag, "Loaded ${materials.size} available materials")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error loading available materials", e)
        }
    }

    private suspend fun loadCarouselProducts() {
        try {
            if (productIds.isEmpty()) {
                Log.d(tag, "No product IDs provided, showing empty list")
                loadedProducts.clear()
                _state.value = _state.value.copy(
                    allProducts = emptyList(),
                    displayedProducts = emptyList(),
                    isLoading = false,
                    isLoadingMore = false,
                    hasMorePages = false,
                    totalProducts = 0
                )
                _displayProductsCount.value = 0
                return
            }

            Log.d(tag, "Fetching products for carousel: ${productIds.size} product IDs")
            val products = repository.fetchProductsByIds(productIds)

            loadedProducts.clear()
            loadedProducts.addAll(products)

            Log.d(tag, "Loaded ${products.size} products from ${productIds.size} product IDs")

            _state.value = _state.value.copy(
                allProducts = loadedProducts.toList(),
                currentPage = 0,
                hasMorePages = false, // No pagination for carousel products
                isLoading = false,
                isLoadingMore = false,
                totalProducts = products.size
            )

            applyFiltersAndSort()

        } catch (e: Exception) {
            Log.e(tag, "Error loading carousel products", e)
            _state.value = _state.value.copy(
                isLoading = false,
                isLoadingMore = false,
                error = "Failed to load products: ${e.message}"
            )
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                if (query.isBlank()) {
                    applyFiltersAndSort()
                    return@launch
                }

                Log.d(tag, "Performing search for: '$query'")

                val currentState = _state.value
                val filterState = _filterSortState.value

                // Search within the loaded products by name
                val searchResults = currentState.allProducts.filter { product ->
                    product.name.contains(query, ignoreCase = true)
                }

                Log.d(tag, "Search found ${searchResults.size} products matching '$query'")

                // Apply category filter first
                val categoryFilteredResults = if (_selectedCategoryId.value != null) {
                    searchResults.filter { product ->
                        product.categoryId == _selectedCategoryId.value
                    }
                } else {
                    searchResults
                }

                // Apply material filter if selected
                val filteredResults = if (filterState.selectedMaterial != null) {
                    val expectedMaterialId = "material_${filterState.selectedMaterial!!.lowercase()}"
                    categoryFilteredResults.filter { product ->
                        product.materialId?.equals(expectedMaterialId, ignoreCase = true) ?: false
                    }
                } else {
                    categoryFilteredResults
                }

                // Apply current sorting
                val sortedResults = applySorting(filteredResults, filterState.sortOption)

                _state.value = _state.value.copy(
                    displayedProducts = sortedResults,
                    hasMorePages = false
                )

                // Update display count
                _displayProductsCount.value = sortedResults.size

                Log.d(tag, "Final search results: ${sortedResults.size} products after filtering and sorting")

            } catch (e: Exception) {
                Log.e(tag, "Error performing search", e)
            }
        }
    }

    private fun setupSearchDebounce() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _filterSortState.value = _filterSortState.value.copy(searchQuery = query)
    }

    fun toggleSearch() {
        _isSearchActive.value = !_isSearchActive.value
        if (!_isSearchActive.value) {
            _searchQuery.value = ""
            _filterSortState.value = _filterSortState.value.copy(searchQuery = "")
        }
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId

        if (_searchQuery.value.isNotBlank()) {
            performSearch(_searchQuery.value)
        } else {
            applyFiltersAndSort()
        }

        Log.d(tag, "Selected category: $categoryId")
    }

    fun applyFilter(materialFilter: String?) {
        viewModelScope.launch {
            _filterSortState.value = _filterSortState.value.copy(selectedMaterial = materialFilter)

            if (_searchQuery.value.isNotBlank()) {
                performSearch(_searchQuery.value)
            } else {
                applyFiltersAndSort()
            }
        }
    }

    fun applySorting(sortOption: SortOption) {
        viewModelScope.launch {
            _filterSortState.value = _filterSortState.value.copy(sortOption = sortOption)

            if (_searchQuery.value.isNotBlank()) {
                performSearch(_searchQuery.value)
            } else {
                applyFiltersAndSort()
            }
        }
    }

    private fun applyFiltersAndSort() {
        val currentState = _state.value
        val filterState = _filterSortState.value

        // Apply category filter first
        val categoryFilteredProducts = if (_selectedCategoryId.value != null) {
            currentState.allProducts.filter { product ->
                product.categoryId == _selectedCategoryId.value
            }
        } else {
            currentState.allProducts
        }

        // Apply material filter if selected
        val filteredProducts = if (filterState.selectedMaterial != null) {
            val expectedMaterialId = "material_${filterState.selectedMaterial!!.lowercase()}"
            categoryFilteredProducts.filter { product ->
                val matches = product.materialId?.equals(expectedMaterialId, ignoreCase = true) ?: false
                Log.d(tag, "Product ${product.id}: material_id='${product.materialId}' vs expected='$expectedMaterialId' -> matches=$matches")
                matches
            }
        } else {
            categoryFilteredProducts
        }

        Log.d(tag, "Filtered ${currentState.allProducts.size} products to ${filteredProducts.size} using category: ${_selectedCategoryId.value}, material: ${filterState.selectedMaterial}")

        val sortedProducts = applySorting(filteredProducts, filterState.sortOption)

        _state.value = currentState.copy(displayedProducts = sortedProducts)

        // Update display count with filtered results
        _displayProductsCount.value = sortedProducts.size
    }

    private fun applySorting(products: List<Product>, sortOption: SortOption): List<Product> {
        return when (sortOption) {
            SortOption.PRICE_LOW_TO_HIGH -> products.sortedBy { it.price }
            SortOption.PRICE_HIGH_TO_LOW -> products.sortedByDescending { it.price }
            SortOption.NONE -> products
        }
    }

    fun toggleFavorite(productId: String) {
        viewModelScope.launch {
            try {
                val currentProducts = _state.value.displayedProducts
                val product = currentProducts.find { it.id == productId } ?: return@launch

                if (product.isFavorite) {
                    repository.removeFromWishlist(productId)
                } else {
                    repository.addToWishlist(productId)
                }

                val updatedDisplayProducts = currentProducts.map {
                    if (it.id == productId) it.copy(isFavorite = !it.isFavorite) else it
                }
                val updatedAllProducts = _state.value.allProducts.map {
                    if (it.id == productId) it.copy(isFavorite = !it.isFavorite) else it
                }

                _state.value = _state.value.copy(
                    displayedProducts = updatedDisplayProducts,
                    allProducts = updatedAllProducts
                )

                val productIndex = loadedProducts.indexOfFirst { it.id == productId }
                if (productIndex != -1) {
                    loadedProducts[productIndex] = loadedProducts[productIndex].copy(isFavorite = !product.isFavorite)
                }

                Log.d(tag, "Toggled favorite for product $productId")

            } catch (e: Exception) {
                Log.e(tag, "Error toggling favorite for product $productId", e)
            }
        }
    }

    fun refreshProducts() {
        viewModelScope.launch {
            loadInitialData()
        }
    }
}
