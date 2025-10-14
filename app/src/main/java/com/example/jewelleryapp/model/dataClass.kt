package com.example.jewelleryapp.model

// Model classes for Firebase data

data class Category(
    val id: String,
    val name: String,
    val imageUrl: String,
    val categoryType: String = "",
    val createdAt: Long = 0L,
    val description: String = "",
    val hasGenderVariants: Boolean = false,
    val isActive: Boolean = true,
    val order: Int = 0
)

// Product data class with new schema
data class Product(
    // Core fields
    val id: String,
    val name: String,
    val description: String = "",
    val price: Double,  // Calculated or custom price
    val customPrice: Double = 0.0,  // Custom/override price if set
    val quantity: Int = 0,
    
    // Images
    val images: List<String> = emptyList(),
    val imageUrl: String = "", // Keep for backward compatibility (first image)
    
    // Category & Material
    val categoryId: String = "",
    val materialId: String = "",
    val materialName: String = "", // Fetched from materials collection
    val materialType: String = "",
    val karat: String = "",
    
    // Weights
    val netWeight: Double = 0.0,
    val totalWeight: Double = 0.0,
    val lessWeight: Double = 0.0,
    val cwWeight: Double = 0.0,
    
    // Charges & Costs
    val defaultMakingRate: Double = 0.0,
    val vaCharges: Double = 0.0,
    val totalProductCost: Double = 0.0,
    val discountPercent: Double = 0.0,  // Discount percentage (0-100)
    val gstRate: Double = 3.0,  // GST percentage (default 3%)
    val saleType: String = "intrastate",  // "intrastate" or "interstate"
    
    // Stone details
    val hasStones: Boolean = false,
    val stoneName: String = "",
    val stoneColor: String = "",
    val stoneRate: Double = 0.0,
    
    // Other properties
    val isOtherThanGold: Boolean = false,
    val available: Boolean = true,
    val featured: Boolean = false,
    val barcodeIds: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val autoGenerateId: Boolean = false,
    val customProductId: String? = null,
    
    // UI visibility control
    val show: Map<String, Boolean> = emptyMap(),
    
    // Client-side only (not in Firestore)
    val isFavorite: Boolean = false,
    
    // Deprecated fields (for backward compatibility)
    @Deprecated("Use categoryId instead")
    val category: String = "",
    @Deprecated("Use images list instead")
    val imageUrls: List<String> = emptyList(),
    @Deprecated("Use stoneName, stoneColor instead")
    val stone: String = "",
    val clarity: String = "",
    val cut: String = "",
    val material: String = "",
    val currency: String = "Rs"
) {
    /**
     * Check if a field should be displayed in the UI based on the show map
     * @param fieldName The name of the field to check (e.g., "name", "price", "quantity")
     * @return true if the field should be shown, false otherwise
     */
    fun shouldShow(fieldName: String): Boolean {
        return show[fieldName] ?: true // Default to true if not specified
    }
    
    /**
     * Get formatted price with currency
     */
    fun getFormattedPrice(): String {
        return "₹${String.format("%,.2f", price)}"
    }
    
    /**
     * Check if product is in stock
     */
    fun isInStock(): Boolean {
        return available && quantity > 0
    }
}

data class Collection(
    val id: String,
    val name: String,
    val imageUrl: String,
    val imageUrls: List<String> = emptyList(),
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

/**
 * Gold and Silver rates for 24K purity
 * Fetched from rates collection where material_type = "24K"
 */
data class GoldSilverRates(
    val goldRatePerGram: Double = 0.0,      // 24K Gold rate per gram
    val silverRatePerGram: Double = 0.0,    // 24K Silver rate per gram (pure silver)
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