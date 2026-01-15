package com.humblecoders.jewelleryapp.model

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

// Product stone data class for stones array
data class ProductStone(
    val name: String = "",
    val color: String = "",
    val rate: Double = 0.0,
    val quantity: Double = 0.0,
    val weight: Double = 0.0,
    val amount: Double = 0.0,
    val purity: String = ""
)

// Product show configuration for field-level visibility
data class ProductShowConfig(
    val name: Boolean = true,
    val description: Boolean = true,
    val price: Boolean = true,
    val categoryId: Boolean = true,
    val materialId: Boolean = true,
    val materialType: Boolean = true,
    val materialName: Boolean = true,
    val gender: Boolean = true,
    val weight: Boolean = true,
    val makingCharges: Boolean = true,
    val available: Boolean = true,
    val featured: Boolean = true,
    val images: Boolean = true,
    val quantity: Boolean = true,
    val totalWeight: Boolean = true,
    val hasStones: Boolean = true,
    val stones: Boolean = true,
    val hasCustomPrice: Boolean = true,
    val customPrice: Boolean = true,
    val customMetalRate: Boolean = true,
    val makingRate: Boolean = true,
    val materialWeight: Boolean = true,
    val stoneWeight: Boolean = true,
    val makingPercent: Boolean = true,
    val labourCharges: Boolean = true,
    val effectiveWeight: Boolean = true,
    val effectiveMetalWeight: Boolean = true,
    val labourRate: Boolean = true,
    val stoneAmount: Boolean = true,
    val isCollectionProduct: Boolean = true,
    val collectionId: Boolean = true
)

// Product data class with new schema
data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val categoryId: String = "",
    val materialId: String = "", // Metal ID
    val materialType: String = "", // Metal purity (e.g., "24K", "22K")
    val materialName: String = "", // Metal name (e.g., "Gold", "Silver")
    val gender: String = "",
    val weight: String = "",
    val makingCharges: Double = 0.0, // New field for making charges per gram
    val available: Boolean = true,
    val featured: Boolean = false,
    val images: List<String> = emptyList(),
    val quantity: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    // New fields for enhanced product details
    val autoGenerateId: Boolean = true, // Radio button for auto-generating product ID
    val totalWeight: Double = 0.0, // Total weight in grams (same as gross weight)
    val hasStones: Boolean = false, // Radio button for has stones (kept for backward compatibility)
    val stones: List<ProductStone> = emptyList(), // Array of stones with all information
    val hasCustomPrice: Boolean = false, // Checkbox for custom price
    val customPrice: Double = 0.0, // Custom price value when hasCustomPrice is true
    val customMetalRate: Double = 0.0, // Custom metal rate for this specific product
    val makingRate: Double = 0.0, // Custom making rate for this specific product
    // New weight fields
    val materialWeight: Double = 0.0, // Metal weight in grams (from materials table)
    val stoneWeight: Double = 0.0, // Stone weight in grams
    val makingPercent: Double = 0.0, // Making percentage (%)
    val labourCharges: Double = 0.0, // Labour charges
    val effectiveWeight: Double = 0.0, // Effective weight in grams (new weight = totalWeight + makingWeight)
    val effectiveMetalWeight: Double = 0.0, // Effective metal weight in grams (from calculation)
    val labourRate: Double = 0.0, // Labour rate per gram
    // Stone fields - calculated from stones array
    val stoneAmount: Double = 0.0, // Sum of amount of all stones in stones array
    // Collection product fields
    val isCollectionProduct: Boolean = false, // Checkbox for collection product
    val collectionId: String = "", // ID of the themed collection this product belongs to
    // Field-level visibility configuration
    val show: ProductShowConfig = ProductShowConfig(),
    // Client-side only (not in Firestore)
    val isFavorite: Boolean = false
) {
    /**
     * Check if a field should be displayed in the UI based on the show config
     * @param fieldName The name of the field to check (e.g., "name", "price", "quantity")
     * @return true if the field should be shown, false otherwise
     */
    fun shouldShow(fieldName: String): Boolean {
        return when (fieldName) {
            "name" -> show.name
            "description" -> show.description
            "price" -> show.price
            "categoryId" -> show.categoryId
            "materialId" -> show.materialId
            "materialType" -> show.materialType
            "materialName" -> show.materialName
            "gender" -> show.gender
            "weight" -> show.weight
            "makingCharges" -> show.makingCharges
            "available" -> show.available
            "featured" -> show.featured
            "images" -> show.images
            "quantity" -> show.quantity
            "totalWeight" -> show.totalWeight
            "hasStones" -> show.hasStones
            "stones" -> show.stones
            "hasCustomPrice" -> show.hasCustomPrice
            "customPrice" -> show.customPrice
            "customMetalRate" -> show.customMetalRate
            "makingRate" -> show.makingRate
            "materialWeight" -> show.materialWeight
            "stoneWeight" -> show.stoneWeight
            "makingPercent" -> show.makingPercent
            "labourCharges" -> show.labourCharges
            "effectiveWeight" -> show.effectiveWeight
            "effectiveMetalWeight" -> show.effectiveMetalWeight
            "labourRate" -> show.labourRate
            "stoneAmount" -> show.stoneAmount
            "isCollectionProduct" -> show.isCollectionProduct
            "collectionId" -> show.collectionId
            else -> true // Default to true if field not found
        }
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
    val description: String = "",
    val productIds: List<String> = emptyList()
)

data class CarouselItem(
    val id: String,
    val imageUrl: String,
    val title: String,
    val subtitle: String,
    val buttonText: String,
    val productIds: List<String> = emptyList()
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
    val googleId: String = "", // Google ID for Google sign-in users, empty for email/password users
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
 * Material type with purity and rate
 * Used within Material data class
 */
data class MaterialType(
    val purity: String = "",
    val rate: String = "" // Stored as string in Firestore
)

/**
 * Gold and Silver rates for 24K purity
 * Fetched from materials collection - finds materials with name "gold" and "silver", 
 * then extracts the rate from their types array where purity = "24K"
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
    val id: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val types: List<MaterialType> = emptyList(), // Updated to support purity and rate
    val createdAt: Long = System.currentTimeMillis()
)


data class CustomerTestimonial(
    val id: String,
    val customerName: String,
    val age: Int = 0,
    val testimonial: String,
    val imageUrl: String,
    val productId: String? = null
)

data class EditorialImage(
    val id: String,
    val imagePos: Int,
    val imageUrl: String,
    val productId: String? = null
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

// Wishlist change events for incremental updates
sealed class WishlistChange {
    data class Added(val productId: String) : WishlistChange()
    data class Removed(val productId: String) : WishlistChange()
    data class InitialLoad(val productIds: List<String>) : WishlistChange()
}

sealed class AccountDeletionState {
    object Idle : AccountDeletionState()
    object Loading : AccountDeletionState()
    object ReauthenticationRequired : AccountDeletionState()
    object Success : AccountDeletionState()
    data class Error(val message: String) : AccountDeletionState()
}

// Order Item data class for items in an order
data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val quantity: Int = 0,
    val weight: Double = 0.0,
    val price: Double = 0.0
)

// Order data class
data class Order(
    val id: String = "",
    val customerId: String = "",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "",
    val status: String = "",
    val isGstIncluded: Boolean = false,
    val timestamp: Long = 0L
) {
    /**
     * Get formatted total amount with currency
     */
    fun getFormattedTotal(): String {
        return "₹${String.format("%,.2f", totalAmount)}"
    }
    
    /**
     * Get formatted date from timestamp
     */
    fun getFormattedDate(): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
        return format.format(date)
    }
}