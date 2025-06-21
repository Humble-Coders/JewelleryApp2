package com.example.jewelleryapp.model

// Model classes for Firebase data

data class Category(
    val id: String,
    val name: String,
    val imageUrl: String
)

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val currency: String = "Rs",
    val imageUrl: String,
    val isFavorite: Boolean = false,
    val material: String = "",
    val stone: String = "",
    val clarity: String = "",
    val cut: String = "",
    val categoryId: String = "",
    val materialId: String? = null,
    val materialType : String? = null,
    val description: String = "",
    val category: String = ""
)

data class Collection(
    val id: String,
    val name: String,
    val imageUrl: String,
    val description: String = ""
)

data class CarouselItem(
    val id: String,
    val imageUrl: String,
    val title: String,
    val subtitle: String,
    val buttonText: String
)

data class CategoryProductsState(
    val allProducts: List<Product> = emptyList(),
    val displayedProducts: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasMorePages: Boolean = true,
    val currentPage: Int = 0,
    val totalProducts: Int = 0
)

data class FilterSortState(
    val selectedMaterial: String? = null, // "Gold", "Silver", or null
    val sortOption: SortOption = SortOption.NONE,
    val searchQuery: String = "",
    val availableMaterials: List<String> = emptyList()
)

enum class SortOption(val displayName: String, val value: String?) {
    NONE("Default", null),
    PRICE_LOW_TO_HIGH("Price: Low to High", "price_asc"),
    PRICE_HIGH_TO_LOW("Price: High to Low", "price_desc")
}