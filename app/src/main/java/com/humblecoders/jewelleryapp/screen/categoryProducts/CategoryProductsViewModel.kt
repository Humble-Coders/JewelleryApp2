package com.humblecoders.jewelleryapp.screen.categoryProducts

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
class CategoryProductsViewModel(
    private val repository: JewelryRepository,
    private val categoryId: String
) : ViewModel() {
    private val tag = "CategoryProductsVM"

    private val _state = MutableStateFlow(CategoryProductsState())
    val state: StateFlow<CategoryProductsState> = _state.asStateFlow()

    private val _filterSortState = MutableStateFlow(FilterSortState())
    val filterSortState: StateFlow<FilterSortState> = _filterSortState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    // Cache for loaded products to avoid re-fetching
    private val loadedProducts = mutableListOf<Product>()

    companion object {
        private const val PAGE_SIZE = 20
    }

    init {
        Log.d(tag, "Initializing CategoryProductsViewModel for category: $categoryId")
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

                    val productsJob = async(Dispatchers.Default) {
                        loadProductsForPage(0)
                    }

                    val totalCountJob = async(Dispatchers.Default) {
                        repository.getCategoryProductsCount(categoryId)
                    }

                    materialsJob.await()
                    productsJob.await()
                    val totalCount = totalCountJob.await()

                    _state.value = _state.value.copy(totalProducts = totalCount)
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

    private suspend fun loadProductsForPage(page: Int) {
        try {
            repository.getCategoryProductsPaginated(
                categoryId = categoryId,
                page = page,
                pageSize = PAGE_SIZE,
                materialFilter = null, // Don't filter in repository
                sortBy = null // Don't sort in repository
            ).collect { products ->
                if (page == 0) {
                    loadedProducts.clear()
                    loadedProducts.addAll(products)
                    _state.value = _state.value.copy(
                        allProducts = loadedProducts.toList(),
                        currentPage = 0,
                        hasMorePages = products.size == PAGE_SIZE,
                        isLoading = false,
                        isLoadingMore = false
                    )
                    // Apply current filters after loading
                    applyFiltersAndSort()
                } else {
                    loadedProducts.addAll(products)
                    _state.value = _state.value.copy(
                        allProducts = loadedProducts.toList(),
                        currentPage = page,
                        hasMorePages = products.size == PAGE_SIZE,
                        isLoadingMore = false
                    )
                    // Apply current filters after loading
                    applyFiltersAndSort()
                }

                Log.d(tag, "Loaded ${products.size} products for page $page. Total: ${loadedProducts.size}")
            }

        } catch (e: Exception) {
            Log.e(tag, "Error loading products for page $page", e)
            _state.value = _state.value.copy(
                isLoading = false,
                isLoadingMore = false,
                error = if (page == 0) "Failed to load products: ${e.message}" else null
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

                // Apply material filter if selected - get actual material document ID
                val filteredResults = if (filterState.selectedMaterial != null) {
                    try {
                        val materialId = repository.getMaterialIdByName(filterState.selectedMaterial!!)
                        Log.d(tag, "Search filter: Material name '${filterState.selectedMaterial}' maps to material_id: $materialId")
                        
                        if (materialId == null) {
                            Log.w(tag, "⚠️ Material ID not found for name '${filterState.selectedMaterial}' in search, showing no products")
                            emptyList()
                        } else {
                            val filtered = searchResults.filter { product ->
                                val matches = product.materialId?.equals(materialId, ignoreCase = true) ?: false
                                if (matches) {
                                    Log.d(tag, "✅ Search product ${product.id} matches material filter: material_id='${product.materialId}'")
                                }
                                matches
                            }
                            Log.d(tag, "✅ Filtered search results from ${searchResults.size} to ${filtered.size} using material_id: $materialId")
                            filtered
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "❌ Error applying material filter in search", e)
                        emptyList()
                    }
                } else {
                    searchResults
                }

                // Apply current sorting
                val sortedResults = applySorting(filteredResults, filterState.sortOption)

                _state.value = _state.value.copy(
                    displayedProducts = sortedResults,
                    hasMorePages = false // No pagination for search results
                )

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

    fun loadMoreProducts() {
        if (_state.value.isLoadingMore || !_state.value.hasMorePages || _searchQuery.value.isNotBlank()) {
            Log.d(tag, "Load more blocked - isLoadingMore: ${_state.value.isLoadingMore}, hasMorePages: ${_state.value.hasMorePages}, searchActive: ${_searchQuery.value.isNotBlank()}")
            return
        }

        Log.d(tag, "Loading more products - current page: ${_state.value.currentPage}")
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoadingMore = true)
            loadProductsForPage(_state.value.currentPage + 1)
        }
    }

    fun applyFilter(materialFilter: String?) {
        viewModelScope.launch {
            Log.d(tag, "=== Applying material filter: $materialFilter ===")
            _filterSortState.value = _filterSortState.value.copy(selectedMaterial = materialFilter)

            if (_searchQuery.value.isNotBlank()) {
                Log.d(tag, "Search query is active, performing search with filter")
                performSearch(_searchQuery.value)
            } else {
                Log.d(tag, "No search query, reloading products with filter")
                reloadProductsWithCurrentSettings()
            }
        }
    }

    fun applySorting(sortOption: SortOption) {
        viewModelScope.launch {
            Log.d(tag, "=== Applying sort option: $sortOption ===")
            _filterSortState.value = _filterSortState.value.copy(sortOption = sortOption)

            if (_searchQuery.value.isNotBlank()) {
                Log.d(tag, "Search query is active, performing search with sort")
                performSearch(_searchQuery.value)
            } else {
                Log.d(tag, "No search query, applying filters and sort")
                applyFiltersAndSort()
            }
        }
    }

    private suspend fun applyFiltersAndSort() {
        val currentState = _state.value
        val filterState = _filterSortState.value

        Log.d(tag, "=== Applying filters and sort ===")
        Log.d(tag, "Total products loaded: ${currentState.allProducts.size}")
        Log.d(tag, "Selected material filter: ${filterState.selectedMaterial}")
        Log.d(tag, "Selected sort option: ${filterState.sortOption}")

        // Debug: Log all product material_ids to understand the data
        Log.d(tag, "=== Product material_ids debug ===")
        currentState.allProducts.take(5).forEachIndexed { index, product ->
            Log.d(tag, "Product $index: id=${product.id}, name=${product.name}, material_id=${product.materialId}")
        }
        if (currentState.allProducts.size > 5) {
            Log.d(tag, "... and ${currentState.allProducts.size - 5} more products")
        }
        Log.d(tag, "=== End material_ids debug ===")

        // Fixed material filter logic - get actual material document ID from materials collection
        val filteredProducts = if (filterState.selectedMaterial != null) {
            try {
                val materialId = repository.getMaterialIdByName(filterState.selectedMaterial!!)
                Log.d(tag, "Material name '${filterState.selectedMaterial}' maps to material_id: $materialId")
                
                if (materialId == null) {
                    Log.w(tag, "⚠️ Material ID not found for name '${filterState.selectedMaterial}', showing no products")
                    _state.value = currentState.copy(displayedProducts = emptyList())
                    return
                }

                val filtered = currentState.allProducts.filter { product ->
                    val matches = product.materialId?.equals(materialId, ignoreCase = true) ?: false
                    if (matches) {
                        Log.d(tag, "✅ Product ${product.id} matches material filter: material_id='${product.materialId}'")
                    }
                    matches
                }

                Log.d(tag, "✅ Filtered ${currentState.allProducts.size} products to ${filtered.size} using material_id: $materialId")
                filtered
            } catch (e: Exception) {
                Log.e(tag, "❌ Error applying material filter", e)
                emptyList()
            }
        } else {
            Log.d(tag, "No material filter selected, showing all products")
            currentState.allProducts
        }

        val sortedProducts = applySorting(filteredProducts, filterState.sortOption)
        Log.d(tag, "Applied sorting to ${sortedProducts.size} products")
        _state.value = currentState.copy(displayedProducts = sortedProducts)
    }

    private fun applySorting(products: List<Product>, sortOption: SortOption): List<Product> {
        return when (sortOption) {
            SortOption.PRICE_LOW_TO_HIGH -> products.sortedBy { it.price }
            SortOption.PRICE_HIGH_TO_LOW -> products.sortedByDescending { it.price }
            SortOption.NONE -> products
        }
    }

    private fun reloadProductsWithCurrentSettings() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            loadProductsForPage(0)
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