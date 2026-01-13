package com.humblecoders.jewelleryapp.screen.categoriesScreen

// CategoriesViewModel.kt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import com.humblecoders.jewelleryapp.model.Category
import com.humblecoders.jewelleryapp.model.Collection
import com.humblecoders.jewelleryapp.repository.JewelryRepository

class CategoriesViewModel(private val repository: JewelryRepository) : ViewModel(){
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _collections = MutableStateFlow<List<Collection>>(emptyList())
    val collections: StateFlow<List<Collection>> = _collections

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        // Load both categories and collections in parallel
        loadCategoriesAndCollections()
    }

    private fun loadCategoriesAndCollections() {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Load both data sources in parallel using coroutineScope
                coroutineScope {
                    val categoriesDeferred = async(Dispatchers.Default) {
                        repository.getCategories().first()
                    }

                    val collectionsDeferred = async(Dispatchers.Default) {
                        repository.getThemedCollections().first()
                    }

                    // Await both results
                    val categoriesResult = categoriesDeferred.await()
                    val collectionsResult = collectionsDeferred.await()

                    // Update UI state with results
                    _categories.value = categoriesResult
                    _collections.value = collectionsResult
                }
            } catch (_: Exception) {
                // Handle errors
            } finally {
                _isLoading.value = false
            }
        }
    }

//    private fun loadCategories() {
//        viewModelScope.launch {
//            _isLoading.value = true
//            repository.getCategories().collect { categories ->
//                _categories.value = categories
//                _isLoading.value = false
//            }
//        }
//    }
//
//    private fun loadCollections() {
//        viewModelScope.launch {
//            repository.getThemedCollections().collect { collections ->
//                _collections.value = collections
//            }
//        }
//    }
//
//    // Public method to manually refresh data
//    fun refreshData() {
//        loadCategoriesAndCollections()
//    }
    
    // Clear all state when user signs out
    fun clearState() {
        _categories.value = emptyList()
        _collections.value = emptyList()
        _isLoading.value = false
    }
}