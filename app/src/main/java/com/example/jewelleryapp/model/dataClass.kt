package com.example.jewelleryapp.model

// Model classes for Firebase data

data class Category(
    val id: String,
    val name: String,
    val imageUrl: String
)

// In dataClass.kt - Update Product data class
data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val currency: String = "INR",
    val imageUrl: String, // Keep for backward compatibility
    val imageUrls: List<String> = emptyList(), // Add this new field
    val isFavorite: Boolean = false,
    val material: String = "",
    val stone: String = "",
    val clarity: String = "",
    val cut: String = "",
    val categoryId: String = "",
    val materialId: String? = null,
    val materialType: String? = null,
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

// Add these data classes to your existing dataClass.kt file

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val dateOfBirth: String = "", // Format: "yyyy-MM-dd"
    val profilePictureUrl: String = "",
    val isGoogleSignIn: Boolean = false,
    val createdAt: Long = 0L,
    val localImagePath: String = "" // For locally stored images
)

data class ProfileUpdateRequest(
    val name: String,
    val phone: String,
    val dateOfBirth: String
)

data class GoldSilverRates(
    val goldRatePerGram: Double = 0.0,
    val silverRatePerGram: Double = 0.0,
    val lastUpdated: Long = 0L,
    val previousGoldRate: Double = 0.0,
    val previousSilverRate: Double = 0.0,
    val currency: String = "INR",
    val rateChangePercentage: Map<String, String> = emptyMap()
)

data class StoreInfo(
    val name: String = "",
    val address: String = "",
    val phonePrimary: String = "",
    val phoneSecondary: String = "",
    val whatsappNumber: String = "",
    val email: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val storeHours: Map<String, String> = emptyMap(),
    val storeImages: List<String> = emptyList(),
    val establishedYear: String = "",
    val whatsappDefaultMessage: String = ""
)

data class Material(
    val id: String,
    val name: String,
    val imageUrl: String
)


sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val profile: UserProfile) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class ProfileUpdateState {
    object Idle : ProfileUpdateState()
    object Loading : ProfileUpdateState()
    object Success : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}

sealed class AccountDeletionState {
    object Idle : AccountDeletionState()
    object Loading : AccountDeletionState()
    object ReauthenticationRequired : AccountDeletionState()
    object Success : AccountDeletionState()
    data class Error(val message: String) : AccountDeletionState()
}