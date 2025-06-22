package com.example.jewelleryapp.screen.allProducts

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.model.CategoryProductsState
import com.example.jewelleryapp.model.FilterSortState
import com.example.jewelleryapp.model.Product
import com.example.jewelleryapp.model.SortOption
import com.example.jewelleryapp.repository.JewelryRepository
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
class AllProductsViewModel(
    private val repository: JewelryRepository,
    private val preSelectedMaterial: String? = null,
    private val preSelectedCategoryId: String? = null
) : ViewModel() {
    private val tag = "AllProductsVM"

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
    private val _categories = MutableStateFlow<List<com.example.jewelleryapp.model.Category>>(emptyList())
    val categories: StateFlow<List<com.example.jewelleryapp.model.Category>> = _categories.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    // Cache for loaded products to avoid re-fetching
    private val loadedProducts = mutableListOf<Product>()

    companion object {
        private const val PAGE_SIZE = 20
    }

    init {
        Log.d(tag, "Initializing AllProductsViewModel with preSelected - Material: $preSelectedMaterial, Category: $preSelectedCategoryId")

        // Set pre-selected filters
        preSelectedMaterial?.let {
            _filterSortState.value = _filterSortState.value.copy(selectedMaterial = it)
        }
        preSelectedCategoryId?.let {
            _selectedCategoryId.value = it
        }

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
                        loadProductsForPage(0)
                    }

                    val totalCountJob = async(Dispatchers.Default) {
                        repository.getAllProductsCount()
                    }

                    materialsJob.await()
                    categoriesJob.await()
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

    private suspend fun loadProductsForPage(page: Int) {
        try {
            repository.getAllProductsPaginated(
                page = page,
                pageSize = PAGE_SIZE,
                materialFilter = null,
                sortBy = null
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
                    applyFiltersAndSort()
                } else {
                    loadedProducts.addAll(products)
                    _state.value = _state.value.copy(
                        allProducts = loadedProducts.toList(),
                        currentPage = page,
                        hasMorePages = products.size == PAGE_SIZE,
                        isLoadingMore = false
                    )
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
                    hasMorePages = false // No pagination for search results
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
                reloadProductsWithCurrentSettings()
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