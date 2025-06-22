package com.example.jewelleryapp.screen.homeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewelleryapp.model.Category
import com.example.jewelleryapp.model.Material
import com.example.jewelleryapp.repository.JewelryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DrawerViewModel(private val repository: JewelryRepository) : ViewModel() {

    private val _materials = MutableStateFlow<List<Material>>(emptyList())
    val materials: StateFlow<List<Material>> = _materials.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _isMetalExpanded = MutableStateFlow(false)
    val isMetalExpanded: StateFlow<Boolean> = _isMetalExpanded.asStateFlow()

    private val _isCollectionsExpanded = MutableStateFlow(false)
    val isCollectionsExpanded: StateFlow<Boolean> = _isCollectionsExpanded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadDrawerData()
    }

    private fun loadDrawerData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                coroutineScope {
                    val materialsJob = async {
                        repository.getMaterials().collect { materials ->
                            _materials.value = materials
                        }
                    }

                    val categoriesJob = async {
                        repository.getCategories().collect { categories ->
                            _categories.value = categories
                        }
                    }

                    materialsJob.await()
                    categoriesJob.await()
                }

            } catch (e: Exception) {
                // Handle error silently for drawer
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleMetalExpansion() {
        _isMetalExpanded.value = !_isMetalExpanded.value
        // Collapse collections if metal is expanded
        if (_isMetalExpanded.value) {
            _isCollectionsExpanded.value = false
        }
    }

    fun toggleCollectionsExpansion() {
        _isCollectionsExpanded.value = !_isCollectionsExpanded.value
        // Collapse metal if collections is expanded
        if (_isCollectionsExpanded.value) {
            _isMetalExpanded.value = false
        }
    }


}