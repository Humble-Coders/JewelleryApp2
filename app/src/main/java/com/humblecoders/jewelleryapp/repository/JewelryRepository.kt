package com.humblecoders.jewelleryapp.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.humblecoders.jewelleryapp.model.Category
import com.humblecoders.jewelleryapp.model.Collection
import com.humblecoders.jewelleryapp.model.Product
import com.humblecoders.jewelleryapp.model.CarouselItem
import com.humblecoders.jewelleryapp.model.GoldSilverRates
import com.humblecoders.jewelleryapp.model.ProductStone
import com.humblecoders.jewelleryapp.model.ProductShowConfig
import com.humblecoders.jewelleryapp.model.Material
import com.humblecoders.jewelleryapp.model.MaterialType
import com.humblecoders.jewelleryapp.model.CustomerTestimonial
import com.humblecoders.jewelleryapp.model.EditorialImage
import com.humblecoders.jewelleryapp.model.StoreInfo
import com.humblecoders.jewelleryapp.model.Video
import com.humblecoders.jewelleryapp.model.Order
import com.humblecoders.jewelleryapp.model.OrderItem
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.async
import com.humblecoders.jewelleryapp.model.WishlistChange
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext
import java.lang.Exception

class JewelryRepository(
    private var userId: String,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val tag = "JewelryRepository"

    // Cache for wishlist status to avoid excessive Firestore calls
    private val wishlistCache = mutableMapOf<String, Boolean>()
    
    // Cache for material names to avoid excessive Firestore calls
    private val materialNameCache = mutableMapOf<String, String>()
    
    // Cache for material rates (24K)
    private val materialRatesCache = mutableMapOf<String, Double>()
    
    // Cache for recently viewed products to avoid reloading on every screen change
    private var recentlyViewedCache: List<Product>? = null
    private var isRecentlyViewedCacheValid = false
    
    /**
     * Calculate jewelry price - NEW SIMPLE CALCULATION
     * Gold: (effectiveMetalWeight × rate) + labourCharges + stoneAmount
     * Silver: (materialWeight × rate) + stoneAmount
     */
    private data class PriceBreakdown(
        val finalAmount: Double,
        val amountBeforeTax: Double
    )

    private fun roundToNearestHundred(value: Double): Double {
        return kotlin.math.round(value / 100.0) * 100.0
    }

    private suspend fun calculateJewelryPrice(product: com.google.firebase.firestore.DocumentSnapshot): PriceBreakdown {
        try {
            Log.d(tag, "=== Starting NEW price calculation for ${product.id} ===")
            
            // Get material information
            val materialId = product.getString("material_id") ?: ""
            if (materialId.isBlank()) {
                Log.e(tag, "Material ID is blank, cannot calculate price")
                return PriceBreakdown(finalAmount = 0.0, amountBeforeTax = 0.0)
            }
            
            val materialName = getMaterialName(materialId)
            if (materialName.isBlank()) {
                Log.e(tag, "Material name is blank for ID: $materialId, cannot calculate price")
                return PriceBreakdown(finalAmount = 0.0, amountBeforeTax = 0.0)
            }
            
            val materialType = product.getString("material_type") ?: "24K"
            val materialNameLower = materialName.lowercase()
            
            Log.d(tag, "Material: $materialName, Type: $materialType")
            
            // Determine if gold or silver
            val isGold = materialNameLower == "gold"
            val isSilver = materialNameLower == "silver"
            
            if (!isGold && !isSilver) {
                Log.e(tag, "Unknown material type: $materialName. Only gold and silver supported.")
                return PriceBreakdown(finalAmount = 0.0, amountBeforeTax = 0.0)
            }
            
            // Fetch rate for the material type from materials collection
            val rate = getMaterialRateByType(materialId, materialType)
            if (rate <= 0) {
                Log.e(tag, "No rate found for $materialName $materialType")
                return PriceBreakdown(finalAmount = 0.0, amountBeforeTax = 0.0)
            }
            
            Log.d(tag, "Rate for $materialName $materialType: ₹$rate/g")
            
            // Calculate stone amount from stones array
            val stones = product.get("stones") as? List<*>
            val stoneAmount = stones?.mapNotNull { stoneMap ->
                when (stoneMap) {
                    is Map<*, *> -> {
                        // Handle both String and Number types (Firestore stores as String)
                        val amountValue = stoneMap["amount"]
                        when (amountValue) {
                            is Number -> amountValue.toDouble()
                            is String -> amountValue.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                    }
                    else -> 0.0
                }
            }?.sum() ?: 0.0
            
            Log.d(tag, "Stone amount (sum of all stones): ₹$stoneAmount (from ${stones?.size ?: 0} stones)")
            
            // Calculate price based on material type
            val finalPrice = when {
                isGold -> {
                    // Gold: (effectiveMetalWeight × rate) + labourCharges + stoneAmount
                    val effectiveMetalWeight = parseDoubleField(product, "effective_metal_weight")
                    val labourCharges = parseDoubleField(product, "labour_charges")
                    
                    Log.d(tag, "Gold calculation: effectiveMetalWeight=$effectiveMetalWeight, rate=$rate, labourCharges=$labourCharges, stoneAmount=$stoneAmount")
                    
                    if (effectiveMetalWeight <= 0) {
                        Log.e(tag, "Effective metal weight is 0 or negative: $effectiveMetalWeight")
                        return PriceBreakdown(finalAmount = 0.0, amountBeforeTax = 0.0)
                    }
                    
                    val metalAmount = effectiveMetalWeight * rate
                    val total = metalAmount + labourCharges + stoneAmount
                    
                    Log.d(tag, "Gold price: (${effectiveMetalWeight} × $rate) + $labourCharges + $stoneAmount = ₹$total")
                    total
                }
                isSilver -> {
                    // Silver: (materialWeight × rate) + stoneAmount
                    val materialWeight = parseDoubleField(product, "material_weight")
                    
                    Log.d(tag, "Silver calculation: materialWeight=$materialWeight, rate=$rate, stoneAmount=$stoneAmount")
                    
                    if (materialWeight <= 0) {
                        Log.e(tag, "Material weight is 0 or negative: $materialWeight")
                        return PriceBreakdown(finalAmount = 0.0, amountBeforeTax = 0.0)
                    }
                    
                    val metalAmount = materialWeight * rate
                    val total = metalAmount + stoneAmount
                    
                    Log.d(tag, "Silver price: (${materialWeight} × $rate) + $stoneAmount = ₹$total")
                    total
                }
                else -> {
                    Log.e(tag, "Unsupported material: $materialName")
                    0.0
                }
            }
            
            val roundedPrice = finalPrice.round(2)
            Log.d(tag, "=== Price calculation complete for ${product.id}: ₹$roundedPrice ===")
            
            return PriceBreakdown(finalAmount = roundedPrice, amountBeforeTax = roundedPrice)
            
        } catch (e: Exception) {
            Log.e(tag, "Error calculating price for product ${product.id}", e)
            e.printStackTrace()
            return PriceBreakdown(finalAmount = 0.0, amountBeforeTax = 0.0)
        }
    }
    
    /**
     * Get material rate by material_type from materials collection
     * Fetches rate for specific material type (e.g., "22K", "24K", "18K")
     */
    private suspend fun getMaterialRateByType(materialId: String, materialType: String): Double {
        val cacheKey = "${materialId}_$materialType"
        
        Log.d(tag, "Getting rate for material ID: $materialId, type: $materialType")
        
        // Check cache first
        materialRatesCache[cacheKey]?.let { 
            Log.d(tag, "Found in cache: $cacheKey → ₹$it/g")
            return it 
        }
        
        return try {
            // Get material from materials collection
            val material = getMaterial(materialId)
            
            if (material == null) {
                Log.e(tag, "❌ Material not found for ID: $materialId")
                return 0.0
            }
            
            Log.d(tag, "Material found: ${material.name}, types count: ${material.types.size}")
            
            // Find the matching type by purity
            val matchingType = material.types.firstOrNull { it.purity == materialType }
            
            val rate = if (matchingType != null && matchingType.rate.isNotBlank()) {
                val rateValue = matchingType.rate.toDoubleOrNull() ?: 0.0
                Log.d(tag, "✅ Found rate for ${material.name} $materialType: ₹$rateValue/g")
                rateValue
            } else {
                Log.w(tag, "❌ No rate found for ${material.name} with purity: $materialType")
                0.0
            }
            
            // Cache the result
            if (rate > 0) {
                materialRatesCache[cacheKey] = rate
                Log.d(tag, "✅ Cached rate for ${material.name} $materialType: ₹$rate/g")
            } else {
                Log.e(tag, "❌ Rate is 0 for ${material.name} $materialType, not caching")
            }
            
            rate
        } catch (e: Exception) {
            Log.e(tag, "❌ Error fetching rate for material ID: $materialId, type: $materialType", e)
            e.printStackTrace()
            0.0
        }
    }
    
    /**
     * Round double to specified decimal places
     */
    private fun Double.round(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }
    
    // Cache for full Material objects
    private val materialCache = mutableMapOf<String, Material>()
    
    /**
     * Fetch full Material object from materials collection
     */
    private suspend fun getMaterial(materialId: String): Material? {
        if (materialId.isBlank()) return null
        
        // Check cache first
        materialCache[materialId]?.let { return it }
        
        return try {
            val doc = withContext(Dispatchers.IO) {
                firestore.collection("materials")
                    .document(materialId)
                    .get()
                    .await()
            }
            
            if (!doc.exists()) {
                Log.w(tag, "Material document not found: $materialId")
                return null
            }
            
            // Parse types array
            val typesList = doc.get("types") as? List<*>
            val materialTypes = typesList?.mapNotNull { typeMap ->
                when (typeMap) {
                    is Map<*, *> -> {
                        MaterialType(
                            purity = (typeMap["purity"] as? String) ?: "",
                            rate = (typeMap["rate"] as? String) ?: ""
                        )
                    }
                    else -> null
                }
            } ?: emptyList()
            
            val material = Material(
                id = doc.id,
                name = doc.getString("name") ?: "",
                imageUrl = doc.getString("image_url") ?: "",
                types = materialTypes,
                createdAt = doc.getLong("created_at")?.toLong() ?: System.currentTimeMillis()
            )
            
            // Cache the result
            materialCache[materialId] = material
            materialNameCache[materialId] = material.name // Also cache name for backward compatibility
            
            material
        } catch (e: Exception) {
            Log.e(tag, "Error fetching material for ID: $materialId", e)
            null
        }
    }
    
    // Helper function to fetch material name by ID (backward compatibility)
    private suspend fun getMaterialName(materialId: String): String {
        if (materialId.isBlank()) return ""
        
        // Check cache first
        materialNameCache[materialId]?.let { return it }
        
        // Try to get from material cache
        materialCache[materialId]?.let { 
            materialNameCache[materialId] = it.name
            return it.name 
        }
        
        // Fetch material
        val material = getMaterial(materialId)
        return material?.name ?: ""
    }
    
    /**
     * Parse stones array from Firestore document to List<ProductStone>
     */
    private fun parseStones(doc: com.google.firebase.firestore.DocumentSnapshot): List<ProductStone> {
        val stones = doc.get("stones") as? List<*>
        return stones?.mapNotNull { stoneMap ->
            when (stoneMap) {
                is Map<*, *> -> {
                    // Helper function to parse number from string or number
                    fun parseDouble(value: Any?): Double {
                        return when (value) {
                            is Number -> value.toDouble()
                            is String -> value.toDoubleOrNull() ?: 0.0
                            else -> 0.0
                        }
                    }
                    
                    ProductStone(
                        name = (stoneMap["name"] as? String) ?: "",
                        color = (stoneMap["color"] as? String) ?: "",
                        rate = parseDouble(stoneMap["rate"]),
                        quantity = parseDouble(stoneMap["quantity"]),
                        weight = parseDouble(stoneMap["weight"]),
                        amount = parseDouble(stoneMap["amount"]),
                        purity = (stoneMap["purity"] as? String) ?: ""
                    )
                }
                else -> null
            }
        } ?: emptyList()
    }
    
    /**
     * Convert show map to ProductShowConfig
     */
    private fun mapToProductShowConfig(showMap: Map<String, Boolean>): ProductShowConfig {
        return ProductShowConfig(
            name = showMap["name"] ?: true,
            description = showMap["description"] ?: true,
            price = showMap["price"] ?: true,
            categoryId = showMap["category_id"] ?: true,
            materialId = showMap["material_id"] ?: true,
            materialType = showMap["material_type"] ?: true,
            materialName = showMap["material_name"] ?: true,
            gender = showMap["gender"] ?: true,
            weight = showMap["weight"] ?: true,
            makingCharges = showMap["making_charges"] ?: true,
            available = showMap["available"] ?: true,
            featured = showMap["featured"] ?: true,
            images = showMap["images"] ?: true,
            quantity = showMap["quantity"] ?: true,
            totalWeight = showMap["total_weight"] ?: true,
            hasStones = showMap["has_stones"] ?: true,
            stones = showMap["stones"] ?: true,
            hasCustomPrice = showMap["has_custom_price"] ?: true,
            customPrice = showMap["custom_price"] ?: true,
            customMetalRate = showMap["custom_metal_rate"] ?: true,
            makingRate = showMap["making_rate"] ?: true,
            materialWeight = showMap["material_weight"] ?: true,
            stoneWeight = showMap["stone_weight"] ?: true,
            makingPercent = showMap["making_percent"] ?: true,
            labourCharges = showMap["labour_charges"] ?: true,
            effectiveWeight = showMap["effective_weight"] ?: true,
            effectiveMetalWeight = showMap["effective_metal_weight"] ?: true,
            labourRate = showMap["labour_rate"] ?: true,
            stoneAmount = showMap["stone_amount"] ?: true,
            isCollectionProduct = showMap["is_collection_product"] ?: true,
            collectionId = showMap["collection_id"] ?: true
        )
    }
    
    // Helper functions to safely parse numeric fields that might be stored as Strings
    private fun parseDoubleField(doc: com.google.firebase.firestore.DocumentSnapshot, fieldName: String): Double {
        return try {
            doc.getDouble(fieldName) ?: 0.0
        } catch (e: Exception) {
            // If it's stored as a String, parse it
            doc.getString(fieldName)?.toDoubleOrNull() ?: 0.0
        }
    }
    
    private fun parseIntField(doc: com.google.firebase.firestore.DocumentSnapshot, fieldName: String): Int {
        return try {
            doc.getLong(fieldName)?.toInt() ?: 0
        } catch (e: Exception) {
            // If it's stored as a String, parse it
            doc.getString(fieldName)?.toIntOrNull() ?: 0
        }
    }
    
    private fun parseLongField(doc: com.google.firebase.firestore.DocumentSnapshot, fieldName: String): Long {
        return try {
            doc.getLong(fieldName) ?: 0L
        } catch (e: Exception) {
            // If it's stored as a String, parse it
            doc.getString(fieldName)?.toLongOrNull() ?: 0L
        }
    }


    // Function to update wishlist cache from Firestore - Optimized with async
    suspend fun refreshWishlistCache() {
        try {
            Log.d(tag, "Refreshing wishlist cache for user: $userId")
            if (userId.isBlank()) {
                Log.d(tag, "User ID is blank, cannot refresh wishlist cache")
                wishlistCache.clear()
                return
            }

            // Clear existing cache
            wishlistCache.clear()

            // Get all wishlist items
            withContext(Dispatchers.IO) {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("wishlist")
                    .get()
                    .await()

                // Update cache with all items - fast batch update
                snapshot.documents.forEach { doc ->
                    wishlistCache[doc.id] = true
                }
            }

            Log.d(tag, "Refreshed wishlist cache with ${wishlistCache.size} items")
        } catch (e: Exception) {
            Log.e(tag, "Error refreshing wishlist cache", e)
            // Don't clear cache on error to prevent data loss
        }
    }

    // Real-time wishlist listener with incremental updates
    fun getWishlistChanges(): Flow<WishlistChange> = callbackFlow {
        if (!hasValidUser()) {
            Log.e(tag, "Cannot listen to wishlist: No valid user")
            close()
            return@callbackFlow
        }

        Log.d(tag, "Setting up real-time wishlist listener for user: $userId")
        
        var isInitialLoad = true

        val listenerRegistration = firestore.collection("users")
            .document(userId)
            .collection("wishlist")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(tag, "Error in wishlist snapshot listener", error)
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    Log.d(tag, "Wishlist snapshot is null")
                    // Even if snapshot is null, emit empty InitialLoad to stop loading
                    if (isInitialLoad) {
                        trySend(WishlistChange.InitialLoad(emptyList()))
                        isInitialLoad = false
                    }
                    return@addSnapshotListener
                }

                // On initial load, emit all current product IDs first
                if (isInitialLoad) {
                    val allProductIds = snapshot.documents.map { it.id }
                    Log.d(tag, "Initial wishlist load: ${allProductIds.size} items")
                    trySend(WishlistChange.InitialLoad(allProductIds))
                    // Update cache
                    allProductIds.forEach { wishlistCache[it] = true }
                    isInitialLoad = false
                    
                    // Don't process document changes on initial load as they're already included
                    return@addSnapshotListener
                }

                // Handle document changes (after initial load)
                snapshot.documentChanges.forEach { change ->
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            val productId = change.document.id
                            Log.d(tag, "Wishlist item added: $productId")
                            trySend(WishlistChange.Added(productId))
                            // Update cache
                            wishlistCache[productId] = true
                            // Invalidate recently viewed cache so it refreshes with updated favorite status
                            invalidateRecentlyViewedCache()
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            val productId = change.document.id
                            Log.d(tag, "Wishlist item removed: $productId")
                            trySend(WishlistChange.Removed(productId))
                            // Update cache
                            wishlistCache[productId] = false
                            // Invalidate recently viewed cache so it refreshes with updated favorite status
                            invalidateRecentlyViewedCache()
                        }
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            // For wishlist, modifications are rare, but handle if needed
                            val productId = change.document.id
                            Log.d(tag, "Wishlist item modified: $productId")
                            // Treat as removed then added to refresh the product
                            trySend(WishlistChange.Removed(productId))
                            trySend(WishlistChange.Added(productId))
                        }
                    }
                }
            }

        // Keep the channel open and clean up on close
        awaitClose {
            Log.d(tag, "Closing wishlist listener")
            listenerRegistration.remove()
        }
    }

    // Legacy method for backward compatibility - now uses real-time listener internally
    fun getWishlistItems(): Flow<List<Product>> = flow {
        try {
            if (!hasValidUser()) {
                Log.e(tag, "Cannot fetch wishlist items: No valid user")
                emit(emptyList())
                return@flow
            }
            Log.d(tag, "Fetching wishlist items for user: $userId")

            // Refresh the cache in the background
            val refreshJob = withContext(Dispatchers.Default) {
                async { refreshWishlistCache() }
            }

            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("users")
                    .document(userId)
                    .collection("wishlist")
                    .get()
                    .await()
            }

            val productIds = snapshot.documents.map { it.id }
            Log.d(tag, "Found ${productIds.size} items in wishlist: $productIds")

            if (productIds.isEmpty()) {
                Log.d(tag, "No items in wishlist")
                emit(emptyList())
                return@flow
            }

            refreshJob.await() // Ensure cache is refreshed before proceeding

            val products = fetchProductsByIds(productIds)
            Log.d(tag, "Successfully fetched ${products.size} products for wishlist")

            // Mark all products as favorites since they're in the wishlist
            val productsWithFavoriteFlag = products.map { it.copy(isFavorite = true) }
            emit(productsWithFavoriteFlag)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching wishlist items", e)
            throw e  // Re-throw so it can be caught and handled in the ViewModel
        }
    }

    suspend fun removeFromWishlist(productId: String) {
        try {


            if (userId.isBlank()) {
                Log.e(tag, "Cannot remove from wishlist: User ID is blank")
                return
            }

            withContext(Dispatchers.IO) {
                firestore.collection("users")
                    .document(userId)
                    .collection("wishlist")
                    .document(productId)
                    .delete()
                    .await()

                // Update cache
                wishlistCache[productId] = false
                // Invalidate recently viewed cache so it refreshes with updated favorite status
                invalidateRecentlyViewedCache()
                Log.d(tag, "Successfully removed product $productId from wishlist")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error removing product from wishlist", e)
            throw e
        }
    }

    suspend fun addToWishlist(productId: String) {
        try {
            if (userId.isBlank()) {
                Log.e(tag, "Cannot add to wishlist: User ID is blank")
                return
            }

            withContext(Dispatchers.IO) {
                firestore.collection("users")
                    .document(userId)
                    .collection("wishlist")
                    .document(productId)
                    .set(mapOf(
                        "addedAt" to System.currentTimeMillis(),
                        "productId" to productId
                    ))
                    .await()

                // Update cache
                wishlistCache[productId] = true
                // Invalidate recently viewed cache so it refreshes with updated favorite status
                invalidateRecentlyViewedCache()
                Log.d(tag, "Successfully added product $productId to wishlist")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error adding product to wishlist", e)
            throw e
        }
    }

     fun getCategoryProducts(categoryId: String): Flow<List<Product>> = flow {
        try {
            Log.d(tag, "Fetching products for category: $categoryId")
            
            // Validate categoryId
            if (categoryId.isBlank()) {
                Log.e(tag, "Invalid categoryId: categoryId is blank")
                emit(emptyList())
                return@flow
            }
            
            // Get product IDs from category_products collection
            // categoryId is now the auto-generated ID from the categories collection
            val categoryDoc = withContext(Dispatchers.IO) {
                firestore.collection("category_products")
                    .document(categoryId)
                    .get()
                    .await()
            }

            val productIds = (categoryDoc.get("product_ids") as? List<*>)?.map { it.toString() } ?: emptyList()

            if (productIds.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            val products = fetchProductsByIds(productIds)
            emit(products)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching category products", e)
            emit(emptyList())
        }
    }

    // In JewelryRepository.kt - Update fetchProductsByIds method
    suspend fun fetchProductsByIds(productIds: List<String>): List<Product> = coroutineScope {
        try {
            Log.d(tag, "Fetching products for IDs: $productIds")

            val productBatches = productIds.chunked(10).map { batch ->
                async(Dispatchers.IO) {
                    Log.d(tag, "Fetching batch of products: $batch")

                    val docDeferred = batch.map { productId ->
                        async(Dispatchers.IO) {
                            try {
                                val doc = firestore.collection("products").document(productId).get().await()
                                if (!doc.exists()) {
                                    Log.e(tag, "Product document $productId does not exist")
                                    null
                                } else {
                                    doc
                                }
                            } catch (e: Exception) {
                                Log.e(tag, "Error fetching product $productId", e)
                                null
                            }
                        }
                    }

                    docDeferred.awaitAll().filterNotNull()
                }
            }

            val allDocuments = productBatches.awaitAll().flatten()

// REPLACE the product creation part in fetchProductsByIds method with this:

            val products = allDocuments.map { doc ->
                async(Dispatchers.Default) {
                    // Get all images from the images array
                    val images = doc.get("images") as? List<*>
                    val imageUrls = images?.mapNotNull { it as? String }?.filter { it.isNotBlank() } ?: emptyList()
                    val primaryImageUrl = imageUrls.firstOrNull() ?: ""

                    // Get barcode IDs
                    val barcodes = doc.get("barcode_ids") as? List<*>
                    val barcodeIds = barcodes?.mapNotNull { it as? String } ?: emptyList()

                    // Get show map for UI visibility control
                    val showMap = doc.get("show") as? Map<*, *>
                    val show = showMap?.mapKeys { it.key.toString() }?.mapValues { it.value as? Boolean ?: false } ?: emptyMap()

                    val isFavorite = wishlistCache[doc.id] == true
                    
                    // Fetch material name from materials collection
                    val materialId = doc.getString("material_id") ?: ""
                    val materialName = getMaterialName(materialId)
                    
                    // Always calculate price using new formula
                    val breakdown = calculateJewelryPrice(doc)
                    val finalPriceRaw = breakdown.finalAmount
                    val finalPrice = roundToNearestHundred(finalPriceRaw)

                    Log.d(tag, "Product ${doc.id}: Material=$materialName, Images=${imageUrls.size}, Price=₹$finalPrice")

                    // Parse stones array
                    val stonesList = parseStones(doc)
                    
                    // Convert show map to ProductShowConfig
                    val showConfig = mapToProductShowConfig(show)
                    
                    Product(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        price = finalPrice,
                        categoryId = doc.getString("category_id") ?: "",
                        materialId = materialId,
                        materialType = doc.getString("material_type") ?: "",
                        materialName = materialName,
                        gender = doc.getString("gender") ?: "",
                        weight = doc.getString("weight") ?: "",
                        makingCharges = parseDoubleField(doc, "making_charges"),
                        available = doc.getBoolean("available") ?: true,
                        featured = doc.getBoolean("featured") ?: false,
                        images = imageUrls,
                        quantity = parseIntField(doc, "quantity"),
                        createdAt = parseLongField(doc, "created_at"),
                        autoGenerateId = doc.getBoolean("auto_generate_id") ?: true,
                        totalWeight = parseDoubleField(doc, "total_weight"),
                        hasStones = doc.getBoolean("has_stones") ?: false,
                        stones = stonesList,
                        hasCustomPrice = false,
                        customPrice = 0.0,
                        customMetalRate = parseDoubleField(doc, "custom_metal_rate"),
                        makingRate = parseDoubleField(doc, "making_rate"),
                        materialWeight = parseDoubleField(doc, "material_weight"),
                        stoneWeight = parseDoubleField(doc, "stone_weight"),
                        makingPercent = parseDoubleField(doc, "making_percent"),
                        labourCharges = parseDoubleField(doc, "labour_charges"),
                        effectiveWeight = parseDoubleField(doc, "effective_weight"),
                        effectiveMetalWeight = parseDoubleField(doc, "effective_metal_weight"),
                        labourRate = parseDoubleField(doc, "labour_rate"),
                        stoneAmount = stonesList.sumOf { it.amount },
                        isCollectionProduct = doc.getBoolean("is_collection_product") ?: false,
                        collectionId = doc.getString("collection_id") ?: "",
                        show = showConfig,
                        isFavorite = isFavorite
                    )
                }
            }.awaitAll()

            Log.d(tag, "Successfully fetched ${products.size} products")
            return@coroutineScope products

        } catch (e: Exception) {
            Log.e(tag, "Error fetching products by IDs: $productIds", e)
            throw e
        }
    }

    // Fetch a single product by ID (for incremental updates)
    suspend fun fetchProductById(productId: String): Product? {
        return try {
            Log.d(tag, "Fetching single product: $productId")
            
            val doc = withContext(Dispatchers.IO) {
                firestore.collection("products").document(productId).get().await()
            }
            
            if (!doc.exists()) {
                Log.e(tag, "Product document $productId does not exist")
                return null
            }
            
            // Reuse the same logic as fetchProductsByIds
            val images = doc.get("images") as? List<*>
            val imageUrls = images?.mapNotNull { it as? String }?.filter { it.isNotBlank() } ?: emptyList()
            
            val barcodes = doc.get("barcode_ids") as? List<*>
            val barcodeIds = barcodes?.mapNotNull { it as? String } ?: emptyList()
            
            val showMap = doc.get("show") as? Map<*, *>
            val show = showMap?.mapKeys { it.key.toString() }?.mapValues { it.value as? Boolean ?: false } ?: emptyMap()
            
            val isFavorite = wishlistCache[doc.id] == true
            
            val materialId = doc.getString("material_id") ?: ""
            val materialName = getMaterialName(materialId)
            
            val breakdown = calculateJewelryPrice(doc)
            val finalPriceRaw = breakdown.finalAmount
            val finalPrice = roundToNearestHundred(finalPriceRaw)
            
            val stonesList = parseStones(doc)
            val showConfig = mapToProductShowConfig(show)
            
            Product(
                id = doc.id,
                name = doc.getString("name") ?: "",
                description = doc.getString("description") ?: "",
                price = finalPrice,
                categoryId = doc.getString("category_id") ?: "",
                materialId = materialId,
                materialType = doc.getString("material_type") ?: "",
                materialName = materialName,
                gender = doc.getString("gender") ?: "",
                weight = doc.getString("weight") ?: "",
                makingCharges = parseDoubleField(doc, "making_charges"),
                available = doc.getBoolean("available") ?: true,
                featured = doc.getBoolean("featured") ?: false,
                images = imageUrls,
                quantity = parseIntField(doc, "quantity"),
                createdAt = parseLongField(doc, "created_at"),
                autoGenerateId = doc.getBoolean("auto_generate_id") ?: true,
                totalWeight = parseDoubleField(doc, "total_weight"),
                hasStones = doc.getBoolean("has_stones") ?: false,
                stones = stonesList,
                hasCustomPrice = false,
                customPrice = 0.0,
                customMetalRate = parseDoubleField(doc, "custom_metal_rate"),
                makingRate = parseDoubleField(doc, "making_rate"),
                materialWeight = parseDoubleField(doc, "material_weight"),
                stoneWeight = parseDoubleField(doc, "stone_weight"),
                makingPercent = parseDoubleField(doc, "making_percent"),
                labourCharges = parseDoubleField(doc, "labour_charges"),
                effectiveWeight = parseDoubleField(doc, "effective_weight"),
                effectiveMetalWeight = parseDoubleField(doc, "effective_metal_weight"),
                labourRate = parseDoubleField(doc, "labour_rate"),
                stoneAmount = stonesList.sumOf { it.amount },
                isCollectionProduct = doc.getBoolean("is_collection_product") ?: false,
                collectionId = doc.getString("collection_id") ?: "",
                show = showConfig,
                isFavorite = isFavorite
            )
        } catch (e: Exception) {
            Log.e(tag, "Error fetching product $productId", e)
            null
        }
    }

    // Get categories with new schema (auto-generated IDs)
     fun getCategories(): Flow<List<Category>> = flow {
        try {
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("categories")
                    .orderBy("order")
                    .get()
                    .await()
            }

            // Process categories in parallel
            val categories = coroutineScope {
                snapshot.documents.map { doc ->
                    async(Dispatchers.Default) {
                        // Map all fields from the new schema
                        Category(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            imageUrl = doc.getString("image_url") ?: "",
                            categoryType = doc.getString("category_type") ?: "",
                            createdAt = doc.getLong("created_at") ?: 0L,
                            description = doc.getString("description") ?: "",
                            hasGenderVariants = doc.getBoolean("has_gender_variants") ?: false,
                            isActive = doc.getBoolean("is_active") ?: true,
                            order = doc.getLong("order")?.toInt() ?: 0
                        )
                    }
                }.awaitAll()
            }

            Log.d(tag, "Fetched ${categories.size} categories with new schema")
            emit(categories)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching categories", e)
            // Don't emit inside catch block - use catch operator instead
        }
    }.catch { e ->
        Log.e(tag, "Error in categories flow", e)
        emit(emptyList<Category>())
    }

    // Fix for getThemedCollections function
     fun getThemedCollections(): Flow<List<Collection>> = flow {
        try {
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("themed_collections")
                    .orderBy("order")
                    .get()
                    .await()
            }

            // Process collections in parallel
            val collections = coroutineScope {
                snapshot.documents.map { doc ->
                    async(Dispatchers.Default) {
                        // Get all images from the imageUrls array
                        val images = doc.get("imageUrls") as? List<*>
                        val imageUrls = images?.mapNotNull { it as? String }?.filter { it.isNotBlank() } ?: emptyList()
                        
                        // Use first image as primary, but store all images
                        val primaryImageUrl = imageUrls.firstOrNull() ?: doc.getString("imageUrl") ?: ""
                        
                        // Fetch productIds - handle both map and list formats
                        val productIds = when {
                            doc.get("productIds") is Map<*, *> -> {
                                // If it's a map (Firestore map with numeric keys like 0, 1, 2)
                                val productIdsMap = doc.get("productIds") as? Map<*, *> ?: emptyMap<Any, Any>()
                                productIdsMap.values.mapNotNull { it.toString() }
                            }
                            doc.get("productIds") is List<*> -> {
                                // If it's a list
                                (doc.get("productIds") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                            }
                            else -> emptyList<String>()
                        }

                        Log.d(tag, "Collection ${doc.id}: Found ${imageUrls.size} images: $imageUrls, ${productIds.size} product IDs")

                        Collection(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            imageUrl = primaryImageUrl,
                            imageUrls = imageUrls,
                            description = doc.getString("description") ?: "",
                            productIds = productIds
                        )
                    }
                }.awaitAll()
            }

            Log.d(tag, "Fetched ${collections.size} themed collections")
            emit(collections)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching themed collections", e)
            // Don't emit inside catch block - use catch operator instead
        }
    }.catch { e ->
        Log.e(tag, "Error in themed collections flow", e)
        emit(emptyList<Collection>())
    }

    // Fix for getCarouselItems function
     fun getCarouselItems(): Flow<List<CarouselItem>> = flow {
        try {
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("carousel_items")
                    .get()
                    .await()
            }

            // Process carousel items in parallel
            val carouselItems = coroutineScope {
                snapshot.documents.map { doc ->
                    async(Dispatchers.Default) {
                        // Direct HTTPS URL - no conversion needed
                        val imageUrl = doc.getString("imageUrl") ?: ""
                        
                        // Fetch productIds - handle both map and list formats
                        val productIds = when {
                            doc.get("productIds") is Map<*, *> -> {
                                // If it's a map (Firestore map with numeric keys like 0, 1, 2)
                                val productIdsMap = doc.get("productIds") as? Map<*, *> ?: emptyMap<Any, Any>()
                                productIdsMap.values.mapNotNull { it.toString() }
                            }
                            doc.get("productIds") is List<*> -> {
                                // If it's a list
                                (doc.get("productIds") as? List<*>)?.mapNotNull { it.toString() } ?: emptyList()
                            }
                            else -> emptyList<String>()
                        }

                        CarouselItem(
                            id = doc.id,
                            imageUrl = imageUrl,
                            title = doc.getString("title") ?: "",
                            subtitle = doc.getString("subtitle") ?: "",
                            buttonText = doc.getString("buttonText") ?: "",
                            productIds = productIds
                        )
                    }
                }.awaitAll()
            }

            Log.d(tag, "Fetched ${carouselItems.size} carousel items")
            emit(carouselItems)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching carousel items", e)
            // Don't emit inside catch block - use catch operator instead
        }
    }.catch { e ->
        Log.e(tag, "Error in carousel items flow", e)
        emit(emptyList<CarouselItem>())
    }

    // Fix for getFeaturedProducts function
     fun getFeaturedProducts(): Flow<List<Product>> = flow {
        try {
            // Make sure the wishlist cache is up to date in the background
            val refreshCacheJob = withContext(Dispatchers.Default) {
                async { refreshWishlistCache() }
            }

            // First get the list of featured product IDs
            val featuredListDoc = withContext(Dispatchers.IO) {
                firestore.collection("featured_products")
                    .document("featured_list")
                    .get()
                    .await()
            }

            val productIds = featuredListDoc.get("product_ids") as? List<*>

            if (productIds.isNullOrEmpty()) {
                Log.d(tag, "No featured product IDs found")
                emit(emptyList<Product>())
                return@flow
            }

            Log.d(tag, "Found ${productIds.size} featured product IDs")

            // Wait for cache refresh to complete
            refreshCacheJob.await()

            // Fetch products using their auto-generated document IDs
            // Use the same fetchProductsByIds method for consistency
            val productIdStrings = productIds.mapNotNull { it.toString() }
            val products = fetchProductsByIds(productIdStrings)

            Log.d(tag, "Fetched ${products.size} featured products")
            emit(products)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching featured products", e)
            // Don't emit inside catch block - use catch operator instead
        }
    }.catch { e ->
        Log.e(tag, "Error in featured products flow", e)
        emit(emptyList<Product>())
    }


    // Function to check if a product is in the user's wishlist
    suspend fun isInWishlist(productId: String): Boolean {
        try {
            // Check the cache first if it exists
            if (wishlistCache.containsKey(productId)) {
                val cachedStatus = wishlistCache[productId] == true
                Log.d(tag, "Found product $productId in wishlist cache: $cachedStatus")
                return cachedStatus
            }

            // If not in cache, check from Firestore
            if (userId.isBlank()) {
                Log.d(tag, "User ID is blank, cannot check wishlist status")
                return false
            }

            val exists = withContext(Dispatchers.IO) {
                val doc = firestore.collection("users")
                    .document(userId)
                    .collection("wishlist")
                    .document(productId)
                    .get()
                    .await()

                doc.exists()
            }

            // Update the cache
            wishlistCache[productId] = exists

            Log.d(tag, "Checked Firestore for product $productId in wishlist: $exists")
            return exists
        } catch (e: Exception) {
            Log.e(tag, "Error checking if product is in wishlist", e)
            return false
        }
    }

    fun getStoreInfo(): Flow<StoreInfo> = flow {
        try {
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("store_info")
                    .document("main_store")
                    .get()
                    .await()
            }

            if (snapshot.exists()) {
                val storeHours = snapshot.get("store_hours") as? Map<String, String> ?: emptyMap()
                val storeImages = snapshot.get("store_images") as? List<String> ?: emptyList()

                val storeInfo = StoreInfo(
                    name = snapshot.getString("name") ?: "",
                    address = snapshot.getString("address") ?: "",
                    phonePrimary = snapshot.getString("phone_primary") ?: "",
                    phoneSecondary = snapshot.getString("phone_secondary") ?: "",
                    whatsappNumber = snapshot.getString("whatsapp_number") ?: "",
                    email = snapshot.getString("email") ?: "",
                    latitude = snapshot.getDouble("latitude") ?: 0.0,
                    longitude = snapshot.getDouble("longitude") ?: 0.0,
                    storeHours = storeHours,
                    storeImages = storeImages,
                    establishedYear = snapshot.getString("established_year") ?: "",
                    whatsappDefaultMessage = snapshot.getString("whatsapp_default_message") ?: ""
                )
                emit(storeInfo)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching store info", e)
            throw e
        }
    }

    // In JewelryRepository.kt - Update getProductDetails method
    fun getProductDetails(productId: String): Flow<Product> = flow {
        try {
            val refreshCacheJob = withContext(Dispatchers.Default) {
                async {
                    if (wishlistCache.isEmpty()) {
                        refreshWishlistCache()
                    }
                }
            }

            val documentSnapshot = withContext(Dispatchers.IO) {
                firestore.collection("products")
                    .document(productId)
                    .get()
                    .await()
            }

            if (documentSnapshot.exists()) {
                refreshCacheJob.await()

                // Get all images from the images array
                val images = documentSnapshot.get("images") as? List<*>
                val imageUrls = images?.mapNotNull { it as? String }?.filter { it.isNotBlank() } ?: emptyList()
                val primaryImageUrl = imageUrls.firstOrNull() ?: ""

                // Get barcode IDs
                val barcodes = documentSnapshot.get("barcode_ids") as? List<*>
                val barcodeIds = barcodes?.mapNotNull { it as? String } ?: emptyList()

                // Get show map for UI visibility control
                val showMap = documentSnapshot.get("show") as? Map<*, *>
                val show = showMap?.mapKeys { it.key.toString() }?.mapValues { it.value as? Boolean ?: false } ?: emptyMap()

                val isInWishlist = wishlistCache[productId] == true
                
                // Fetch material name from materials collection
                val materialId = documentSnapshot.getString("material_id") ?: ""
                val materialName = getMaterialName(materialId)
                
                // Always calculate price using new formula
                val breakdown = calculateJewelryPrice(documentSnapshot)
                val finalPriceRaw = breakdown.finalAmount
                val finalPrice = roundToNearestHundred(finalPriceRaw)

                Log.d(tag, "Product $productId: Material=$materialName, Images=${imageUrls.size}, Price=₹$finalPrice")

                // Parse stones array
                val stonesList = parseStones(documentSnapshot)
                
                // Convert show map to ProductShowConfig
                val showConfig = mapToProductShowConfig(show)
                
                val product = Product(
                    id = documentSnapshot.id,
                    name = documentSnapshot.getString("name") ?: "",
                    description = documentSnapshot.getString("description") ?: "",
                    price = finalPrice,
                    categoryId = documentSnapshot.getString("category_id") ?: "",
                    materialId = materialId,
                    materialType = documentSnapshot.getString("material_type") ?: "",
                    materialName = materialName,
                    gender = documentSnapshot.getString("gender") ?: "",
                    weight = documentSnapshot.getString("weight") ?: "",
                    makingCharges = parseDoubleField(documentSnapshot, "making_charges"),
                    available = documentSnapshot.getBoolean("available") ?: true,
                    featured = documentSnapshot.getBoolean("featured") ?: false,
                    images = imageUrls,
                    quantity = parseIntField(documentSnapshot, "quantity"),
                    createdAt = parseLongField(documentSnapshot, "created_at"),
                    autoGenerateId = documentSnapshot.getBoolean("auto_generate_id") ?: true,
                    totalWeight = parseDoubleField(documentSnapshot, "total_weight"),
                    hasStones = documentSnapshot.getBoolean("has_stones") ?: false,
                    stones = stonesList,
                    hasCustomPrice = false,
                    customPrice = 0.0,
                    customMetalRate = parseDoubleField(documentSnapshot, "custom_metal_rate"),
                    makingRate = parseDoubleField(documentSnapshot, "making_rate"),
                    materialWeight = parseDoubleField(documentSnapshot, "material_weight"),
                    stoneWeight = parseDoubleField(documentSnapshot, "stone_weight"),
                    makingPercent = parseDoubleField(documentSnapshot, "making_percent"),
                    labourCharges = parseDoubleField(documentSnapshot, "labour_charges"),
                    effectiveWeight = parseDoubleField(documentSnapshot, "effective_weight"),
                    effectiveMetalWeight = parseDoubleField(documentSnapshot, "effective_metal_weight"),
                    labourRate = parseDoubleField(documentSnapshot, "labour_rate"),
                    stoneAmount = stonesList.sumOf { it.amount },
                    isCollectionProduct = documentSnapshot.getBoolean("is_collection_product") ?: false,
                    collectionId = documentSnapshot.getString("collection_id") ?: "",
                    show = showConfig,
                    isFavorite = isInWishlist
                )
                emit(product)
            } else {
                throw Exception("Product not found")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching product details", e)
            throw e
        }
    }



    fun getProductsByCategory(
        categoryId: String,
        excludeProductId: String? = null,
        limit: Int = 10
    ): Flow<List<Product>> = flow {
        
        Log.d(tag, "Fetching products by category: $categoryId")
        
        // Validate categoryId
        if (categoryId.isBlank()) {
            Log.e(tag, "Invalid categoryId: categoryId is blank")
            emit(emptyList())
            return@flow
        }

        // Step 1: Get product IDs (same as before)
        val categorySnapshot = withContext(Dispatchers.IO) {
            firestore.collection("category_products")
                .document(categoryId)
                .get()
                .await()
        }

        val productIds = (categorySnapshot.get("product_ids") as? List<*>)
            ?.map { it.toString() }
            ?.filter { it != excludeProductId }  // Filter out current product
            ?.take(limit)  // Take only what we need
            ?: emptyList()

        if (productIds.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        // Step 2: Fetch all products in parallel (FASTER)
        val products = withContext(Dispatchers.IO) {
            productIds.map { productId ->
                async {
                    try {
                        val doc = firestore.collection("products")
                            .document(productId)
                            .get()
                            .await()

                        if (doc.exists()) {
                            // Use the same comprehensive mapping as fetchProductsByIds
                            val images = doc.get("images") as? List<*>
                            val imageUrls = images?.mapNotNull { it as? String }?.filter { it.isNotBlank() } ?: emptyList()
                            val primaryImageUrl = imageUrls.firstOrNull() ?: ""

                            val barcodes = doc.get("barcode_ids") as? List<*>
                            val barcodeIds = barcodes?.mapNotNull { it as? String } ?: emptyList()

                            val showMap = doc.get("show") as? Map<*, *>
                            val show = showMap?.mapKeys { it.key.toString() }?.mapValues { it.value as? Boolean ?: false } ?: emptyMap()

                            val isInWishlist = wishlistCache[doc.id] == true
                            
                            // Fetch material name from materials collection
                            val materialId = doc.getString("material_id") ?: ""
                            val materialName = getMaterialName(materialId)
                            
                            // Always calculate price using new formula
                            val breakdown = calculateJewelryPrice(doc)
                            val finalPriceRaw = breakdown.finalAmount
                            val finalPrice = roundToNearestHundred(finalPriceRaw)

                            // Parse stones array
                            val stonesList = parseStones(doc)
                            
                            // Convert show map to ProductShowConfig
                            val showConfig = mapToProductShowConfig(show)

                            Product(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                description = doc.getString("description") ?: "",
                                price = finalPrice,
                                categoryId = doc.getString("category_id") ?: "",
                                materialId = materialId,
                                materialType = doc.getString("material_type") ?: "",
                                materialName = materialName,
                                gender = doc.getString("gender") ?: "",
                                weight = doc.getString("weight") ?: "",
                                makingCharges = parseDoubleField(doc, "making_charges"),
                                available = doc.getBoolean("available") ?: true,
                                featured = doc.getBoolean("featured") ?: false,
                                images = imageUrls,
                                quantity = parseIntField(doc, "quantity"),
                                createdAt = parseLongField(doc, "created_at"),
                                autoGenerateId = doc.getBoolean("auto_generate_id") ?: true,
                                totalWeight = parseDoubleField(doc, "total_weight"),
                                hasStones = doc.getBoolean("has_stones") ?: false,
                                stones = stonesList,
                                hasCustomPrice = false,
                                customPrice = 0.0,
                                customMetalRate = parseDoubleField(doc, "custom_metal_rate"),
                                makingRate = parseDoubleField(doc, "making_rate"),
                                materialWeight = parseDoubleField(doc, "material_weight"),
                                stoneWeight = parseDoubleField(doc, "stone_weight"),
                                makingPercent = parseDoubleField(doc, "making_percent"),
                                labourCharges = parseDoubleField(doc, "labour_charges"),
                                effectiveWeight = parseDoubleField(doc, "effective_weight"),
                                effectiveMetalWeight = parseDoubleField(doc, "effective_metal_weight"),
                                labourRate = parseDoubleField(doc, "labour_rate"),
                                stoneAmount = stonesList.sumOf { stone -> stone.amount },
                                isCollectionProduct = doc.getBoolean("is_collection_product") ?: false,
                                collectionId = doc.getString("collection_id") ?: "",
                                show = showConfig,
                                isFavorite = isInWishlist
                            )
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error fetching product $productId: ${e.message}")
                        null
                    }
                }
            }.awaitAll().filterNotNull()
        }

        emit(products)
    }.catch { e ->
        Log.e(tag, "Error fetching products by category", e)
        emit(emptyList())
    }


    // Add a function to update user ID if it changes
    fun updateUserId(newUserId: String) {
        val oldUserId = userId
        userId = newUserId
        
        Log.d(tag, "Updating user ID from '$oldUserId' to '$newUserId'")
        
        // Clear all caches and state when user ID changes
        clearAllUserData()
    }
    
    // Comprehensive method to clear all user-specific data and caches
    fun clearAllUserData() {
        Log.d(tag, "Clearing all user data and caches")
        
        // Clear wishlist cache
        wishlistCache.clear()
        
        // Clear recently viewed cache
        recentlyViewedCache = null
        isRecentlyViewedCacheValid = false
        
        // Clear material name cache
        materialNameCache.clear()
        
        // Clear material rates cache
        materialRatesCache.clear()
        
        Log.d(tag, "All user data cleared")
    }
    
    // Helper function to invalidate recently viewed cache
    private fun invalidateRecentlyViewedCache() {
        isRecentlyViewedCacheValid = false
        // Don't clear the cache, just mark it as invalid so it will be refreshed with updated favorite status
        Log.d(tag, "Invalidated recently viewed cache to refresh favorite status")
    }

    // Add a function to check if the repository has a valid user
    private fun hasValidUser(): Boolean {
        // Check current userId first
        if (userId.isNotBlank()) {
            return true
        }

        // If userId is blank, try to get it from auth
        val currentUser = auth.currentUser
        if (currentUser != null) {
            userId = currentUser.uid
            return true
        }

        return false
    }




    // Add these methods to your existing JewelryRepository class

    /**
     * Get paginated products for a specific category with filtering and sorting
     */
    /**
     * Get paginated products for a specific category with filtering and sorting
     */
    fun getCategoryProductsPaginated(
        categoryId: String,
        page: Int,
        pageSize: Int = 20,
        materialFilter: String? = null, // "Gold", "Silver", or null for all
        sortBy: String? = null // "price_asc", "price_desc", or null
    ): Flow<List<Product>> = flow {
        try {
            Log.d(tag, "Fetching paginated products for category: $categoryId, page: $page")
            
            // Validate categoryId
            if (categoryId.isBlank()) {
                Log.e(tag, "Invalid categoryId: categoryId is blank")
                emit(emptyList())
                return@flow
            }

            // First get product IDs from category_products collection
            // categoryId is now the auto-generated ID from the categories collection
            val categoryDoc = withContext(Dispatchers.IO) {
                firestore.collection("category_products")
                    .document(categoryId)
                    .get()
                    .await()
            }

            val allProductIds = (categoryDoc.get("product_ids") as? List<*>)?.map { it.toString() } ?: emptyList()

            if (allProductIds.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            Log.d(tag, "Found ${allProductIds.size} total products for category")

            // Calculate pagination
            val startIndex = page * pageSize
            val endIndex = minOf(startIndex + pageSize, allProductIds.size)

            if (startIndex >= allProductIds.size) {
                emit(emptyList())
                return@flow
            }

            val paginatedIds = allProductIds.subList(startIndex, endIndex)
            Log.d(tag, "Fetching products for page $page: ${paginatedIds.size} items")

            // Fetch products in batches (Firestore limit is 10 for whereIn)
            val products = fetchProductsByIds(paginatedIds)

            // Don't apply filters here - let ViewModel handle filtering and sorting
            Log.d(tag, "Returning ${products.size} products for category $categoryId, page $page")
            emit(products)

        } catch (e: Exception) {
            Log.e(tag, "Error fetching paginated category products", e)
            emit(emptyList()) // Emit empty list instead of throwing
        }
    }
    /**
     * Get total count of products in a category (for pagination calculation)
     */
    suspend fun getCategoryProductsCount(categoryId: String): Int {
        return try {
            Log.d(tag, "Getting product count for category: $categoryId")
            
            // Validate categoryId
            if (categoryId.isBlank()) {
                Log.e(tag, "Invalid categoryId: categoryId is blank")
                return 0
            }
            
            // categoryId is now the auto-generated ID from the categories collection
            val categoryDoc = withContext(Dispatchers.IO) {
                firestore.collection("category_products")
                    .document(categoryId)
                    .get()
                    .await()
            }

            val productIds = (categoryDoc.get("product_ids") as? List<*>)?.size ?: 0
            Log.d(tag, "Category $categoryId has $productIds total products")
            productIds

        } catch (e: Exception) {
            Log.e(tag, "Error getting category products count for categoryId: '$categoryId'", e)
            0
        }
    }


    /**
     * Get available materials for filtering (from materials collection)
     */
    fun getAvailableMaterials(): Flow<List<String>> = flow {
        try {
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("materials")
                    .get()
                    .await()
            }

            val materials = snapshot.documents.mapNotNull { doc ->
                doc.getString("name")
            }

            Log.d(tag, "Available materials: $materials")
            emit(materials)

        } catch (e: Exception) {
            Log.e(tag, "Error fetching available materials", e)
            emit(emptyList()) // Emit empty list instead of throwing
        }
    }
    fun getGoldSilverRates(): Flow<GoldSilverRates> = flow {
        try {
            Log.d(tag, "Fetching gold and silver rates for 24K from materials collection")
            
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("materials")
                    .get()
                    .await()
            }

            if (snapshot.isEmpty) {
                Log.w(tag, "No materials found in materials collection")
                emit(GoldSilverRates())
                return@flow
            }

            var goldRate = 0.0
            var silverRate = 0.0
            var lastUpdated = System.currentTimeMillis()
            var currency = "INR"

            // Find Gold and Silver materials and extract rates
            snapshot.documents.forEach { doc ->
                val materialName = doc.getString("name")?.lowercase() ?: ""
                
                // Parse types array
                val typesList = doc.get("types") as? List<*>
                val materialTypes = typesList?.mapNotNull { typeMap ->
                    when (typeMap) {
                        is Map<*, *> -> {
                            MaterialType(
                                purity = (typeMap["purity"] as? String) ?: "",
                                rate = (typeMap["rate"] as? String) ?: ""
                            )
                        }
                        else -> null
                    }
                } ?: emptyList()
                
                when (materialName) {
                    "gold" -> {
                        // Find 24K rate for gold
                        val type24K = materialTypes.firstOrNull { it.purity == "24K" }
                        val rate24K = type24K?.rate?.toDoubleOrNull() ?: 0.0
                        goldRate = rate24K
                        Log.d(tag, "Material: $materialName, 24K rate: ${if (rate24K > 0) "₹$rate24K/g" else "not found"}")
                        // Use created_at as timestamp
                        val docTimestamp = doc.getLong("created_at")?.toLong() ?: System.currentTimeMillis()
                        if (docTimestamp > lastUpdated) {
                            lastUpdated = docTimestamp
                        }
                    }
                    "silver" -> {
                        // Find 999 purity rate for silver
                        val type999 = materialTypes.firstOrNull { it.purity == "999" }
                        val rate999 = type999?.rate?.toDoubleOrNull() ?: 0.0
                        silverRate = rate999
                        Log.d(tag, "Material: $materialName, 999 rate: ${if (rate999 > 0) "₹$rate999/g" else "not found"}")
                        // Use created_at as timestamp
                        val docTimestamp = doc.getLong("created_at")?.toLong() ?: System.currentTimeMillis()
                        if (lastUpdated == 0L || docTimestamp > lastUpdated) {
                            lastUpdated = docTimestamp
                        }
                    }
                }
            }

            val rates = GoldSilverRates(
                goldRatePerGram = goldRate,
                silverRatePerGram = silverRate,
                lastUpdated = lastUpdated,
                previousGoldRate = 0.0, // Can be calculated if needed
                previousSilverRate = 0.0, // Can be calculated if needed
                currency = currency,
                rateChangePercentage = emptyMap() // Can be calculated if needed
            )
            
            Log.d(tag, "Rates fetched from materials - Gold 24K: ₹$goldRate/g, Silver 999: ₹$silverRate/g")
            emit(rates)
            
        } catch (e: Exception) {
            Log.e(tag, "Error fetching gold silver rates from materials collection", e)
            // Don't emit in catch block - use .catch operator instead
            throw e
        }
    }.catch { e ->
        Log.e(tag, "Error in rates flow", e)
        emit(GoldSilverRates()) // Safe to emit from .catch operator
    }

    // Add these methods to your existing JewelryRepository class

    // Add these methods to your existing JewelryRepository class

    /**
     * Get paginated all products with filtering and sorting
     */
    fun getAllProductsPaginated(
        page: Int,
        pageSize: Int = 20,
        materialFilter: String? = null,
        sortBy: String? = null
    ): Flow<List<Product>> = flow {
        try {
            Log.d(tag, "Fetching paginated all products: page $page, pageSize $pageSize")

            // Get all products with pagination using startAfter for efficient pagination
            val query = firestore.collection("products")
                .orderBy("name") // Order by name for consistent pagination
                .limit(pageSize.toLong())

            // Apply pagination offset
            val snapshot = if (page == 0) {
                withContext(Dispatchers.IO) {
                    query.get().await()
                }
            } else {
                // For subsequent pages, we need to get documents to skip
                val skipCount = page * pageSize
                val skipQuery = firestore.collection("products")
                    .orderBy("name")
                    .limit(skipCount.toLong())

                val lastDoc = withContext(Dispatchers.IO) {
                    val skipSnapshot = skipQuery.get().await()
                    skipSnapshot.documents.lastOrNull()
                }

                if (lastDoc != null) {
                    withContext(Dispatchers.IO) {
                        query.startAfter(lastDoc).get().await()
                    }
                } else {
                    // No more documents
                    withContext(Dispatchers.IO) {
                        query.limit(0).get().await()
                    }
                }
            }

            if (snapshot.isEmpty) {
                emit(emptyList())
                return@flow
            }

            Log.d(tag, "Found ${snapshot.size()} products for page $page")

            // Process products in parallel
            val products = coroutineScope {
                snapshot.documents.map { doc ->
                    async(Dispatchers.Default) {
                        try {
                            // Use the same comprehensive mapping as other methods
                            val images = doc.get("images") as? List<*>
                            val imageUrls = images?.mapNotNull { it as? String }?.filter { it.isNotBlank() } ?: emptyList()
                            val primaryImageUrl = imageUrls.firstOrNull() ?: ""

                            val barcodes = doc.get("barcode_ids") as? List<*>
                            val barcodeIds = barcodes?.mapNotNull { it as? String } ?: emptyList()

                            val showMap = doc.get("show") as? Map<*, *>
                            val show = showMap?.mapKeys { it.key.toString() }?.mapValues { it.value as? Boolean ?: false } ?: emptyMap()

                            val isInWishlist = wishlistCache[doc.id] == true
                            
                            // Fetch material name from materials collection
                            val materialId = doc.getString("material_id") ?: ""
                            val materialName = getMaterialName(materialId)
                            
                            // Always calculate price using new formula
                            val breakdown = calculateJewelryPrice(doc)
                            val finalPriceRaw = breakdown.finalAmount
                            val finalPrice = roundToNearestHundred(finalPriceRaw)

                            // Parse stones array
                            val stonesList = parseStones(doc)
                            
                            // Convert show map to ProductShowConfig
                            val showConfig = mapToProductShowConfig(show)

                            Product(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                description = doc.getString("description") ?: "",
                                price = finalPrice,
                                categoryId = doc.getString("category_id") ?: "",
                                materialId = materialId,
                                materialType = doc.getString("material_type") ?: "",
                                materialName = materialName,
                                gender = doc.getString("gender") ?: "",
                                weight = doc.getString("weight") ?: "",
                                makingCharges = parseDoubleField(doc, "making_charges"),
                                available = doc.getBoolean("available") ?: true,
                                featured = doc.getBoolean("featured") ?: false,
                                images = imageUrls,
                                quantity = parseIntField(doc, "quantity"),
                                createdAt = parseLongField(doc, "created_at"),
                                autoGenerateId = doc.getBoolean("auto_generate_id") ?: true,
                                totalWeight = parseDoubleField(doc, "total_weight"),
                                hasStones = doc.getBoolean("has_stones") ?: false,
                                stones = stonesList,
                                hasCustomPrice = false,
                                customPrice = 0.0,
                                customMetalRate = parseDoubleField(doc, "custom_metal_rate"),
                                makingRate = parseDoubleField(doc, "making_rate"),
                                materialWeight = parseDoubleField(doc, "material_weight"),
                                stoneWeight = parseDoubleField(doc, "stone_weight"),
                                makingPercent = parseDoubleField(doc, "making_percent"),
                                labourCharges = parseDoubleField(doc, "labour_charges"),
                                effectiveWeight = parseDoubleField(doc, "effective_weight"),
                                effectiveMetalWeight = parseDoubleField(doc, "effective_metal_weight"),
                                labourRate = parseDoubleField(doc, "labour_rate"),
                                stoneAmount = stonesList.sumOf { stone -> stone.amount },
                                isCollectionProduct = doc.getBoolean("is_collection_product") ?: false,
                                collectionId = doc.getString("collection_id") ?: "",
                                show = showConfig,
                                isFavorite = isInWishlist
                            )
                        } catch (e: Exception) {
                            Log.e(tag, "Error processing product ${doc.id}", e)
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            Log.d(tag, "Successfully processed ${products.size} products for page $page")
            emit(products)

        } catch (e: Exception) {
            Log.e(tag, "Error fetching paginated all products for page $page", e)
            emit(emptyList())
        }
    }

    /**
     * Get total count of all products
     */
    suspend fun getAllProductsCount(): Int {
        return try {
            withContext(Dispatchers.IO) {
                val snapshot = firestore.collection("products")
                    .get()
                    .await()

                val count = snapshot.size()
                Log.d(tag, "Total products count: $count")
                count
            }
        } catch (e: Exception) {
            Log.e(tag, "Error getting total products count", e)
            0
        }
    }

    fun getMaterials(): Flow<List<Material>> = flow {
        try {
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("materials")
                    .get()
                    .await()
            }

            val materials = snapshot.documents.map { doc ->
                Material(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    imageUrl = doc.getString("image_url") ?: ""
                )
            }

            Log.d(tag, "Fetched ${materials.size} materials")
            emit(materials)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching materials", e)
            emit(emptyList())
        }
    }

    /**
     * Get material ID by material name
     * Returns the document ID of the material with the given name
     */
    suspend fun getMaterialIdByName(materialName: String): String? {
        if (materialName.isBlank()) {
            Log.d(tag, "Material name is blank, returning null")
            return null
        }

        return try {
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("materials")
                    .whereEqualTo("name", materialName)
                    .limit(1)
                    .get()
                    .await()
            }

            val materialId = snapshot.documents.firstOrNull()?.id
            Log.d(tag, "Material ID for name '$materialName': $materialId")
            materialId
        } catch (e: Exception) {
            Log.e(tag, "Error fetching material ID for name: $materialName", e)
            null
        }
    }

    // Add these methods to your existing JewelryRepository class


    /**
     * Add product to recently viewed (limit 10, remove oldest when full)
     * Invalidates cache so it will be refreshed on next fetch
     */
    suspend fun addToRecentlyViewed(productId: String) {
        try {
            if (userId.isBlank()) {
                Log.e(tag, "Cannot add to recently viewed: User ID is blank")
                return
            }

            withContext(Dispatchers.IO) {
                val recentlyViewedRef = firestore.collection("users")
                    .document(userId)
                    .collection("recently_viewed")

                // Get current recently viewed items ordered by timestamp
                val currentItems = recentlyViewedRef
                    .orderBy("viewedAt")
                    .get()
                    .await()

                // Remove existing entry if product already viewed (to update timestamp)
                currentItems.documents.forEach { doc ->
                    if (doc.getString("productId") == productId) {
                        doc.reference.delete().await()
                    }
                }

                // If we have 10+ items after potential removal, delete oldest
                val remainingItems = recentlyViewedRef
                    .orderBy("viewedAt")
                    .get()
                    .await()

                if (remainingItems.size() >= 10) {
                    // Delete oldest items to make room (keep only 9, so after adding new one we have 10)
                    val itemsToDelete = remainingItems.documents.take(remainingItems.size() - 9)
                    itemsToDelete.forEach { doc ->
                        doc.reference.delete().await()
                    }
                }

                // Add new item with current timestamp
                recentlyViewedRef.add(mapOf(
                    "productId" to productId,
                    "viewedAt" to System.currentTimeMillis()
                )).await()

                Log.d(tag, "Successfully added product $productId to recently viewed")
                
                // Invalidate cache so it will be refreshed on next fetch
                isRecentlyViewedCacheValid = false
                recentlyViewedCache = null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error adding product to recently viewed", e)
            // Don't throw error as this is not critical functionality
        }
    }

    /**
     * Get recently viewed products (ordered by most recent first)
     */

    /**
     * Clear all recently viewed products
     */
    /**
     * Get recently viewed products (ordered by most recent first)
     * Uses cache to avoid reloading on every screen change
     */
    fun getRecentlyViewedProducts(forceRefresh: Boolean = false): Flow<List<Product>> = flow {
        if (!hasValidUser()) {
            Log.e(tag, "Cannot fetch recently viewed: No valid user")
            emit(emptyList())
            return@flow
        }

        // Return cached data if available and valid, unless force refresh is requested
        // But update favorite status from current wishlist cache to ensure accuracy
        if (!forceRefresh && isRecentlyViewedCacheValid && recentlyViewedCache != null) {
            Log.d(tag, "Returning cached recently viewed products: ${recentlyViewedCache!!.size} items")
            // Update favorite status from current wishlist cache
            val updatedCache = recentlyViewedCache!!.map { product ->
                val isInWishlist = wishlistCache[product.id] == true
                product.copy(isFavorite = isInWishlist)
            }
            emit(updatedCache)
            return@flow
        }

        Log.d(tag, "Fetching recently viewed products for user: $userId")

        val snapshot = withContext(Dispatchers.IO) {
            firestore.collection("users")
                .document(userId)
                .collection("recently_viewed")
                .orderBy("viewedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()
        }

        val productIds = snapshot.documents.mapNotNull { doc ->
            doc.getString("productId")
        }

        Log.d(tag, "Found ${productIds.size} recently viewed products: $productIds")

        if (productIds.isEmpty()) {
            recentlyViewedCache = emptyList()
            isRecentlyViewedCacheValid = true
            emit(emptyList())
            return@flow
        }

        // Fetch product details
        val products = fetchProductsByIds(productIds)

        // Maintain the order from recently viewed (most recent first)
        val orderedProducts = productIds.mapNotNull { productId ->
            products.find { it.id == productId }
        }

        // Update favorite status from wishlist cache before caching
        val productsWithUpdatedFavorites = orderedProducts.map { product ->
            val isInWishlist = wishlistCache[product.id] == true
            product.copy(isFavorite = isInWishlist)
        }
        
        // Update cache with products that have correct favorite status
        recentlyViewedCache = productsWithUpdatedFavorites
        isRecentlyViewedCacheValid = true

        Log.d(tag, "Successfully fetched ${productsWithUpdatedFavorites.size} recently viewed products and cached them")
        emit(productsWithUpdatedFavorites)
    }.catch { e ->
        Log.e(tag, "Error fetching recently viewed products", e)
        // If we have cached data, return it even on error
        // But update favorite status from current wishlist cache
        if (recentlyViewedCache != null) {
            val updatedCache = recentlyViewedCache!!.map { product ->
                val isInWishlist = wishlistCache[product.id] == true
                product.copy(isFavorite = isInWishlist)
            }
            emit(updatedCache)
        } else {
            emit(emptyList())
        }
    }

    /**
     * Get video from Header collection
     */
    fun getVideo(): Flow<Video?> = flow {
        try {
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("Header")
                    .document("Video")
                    .get()
                    .await()
            }

            if (snapshot.exists()) {
                val videoUrl = snapshot.getString("link") ?: ""
                val title = snapshot.getString("title") ?: ""
                val description = snapshot.getString("description") ?: ""
                val thumbnailUrl = snapshot.getString("thumbnail_url") ?: ""
                val duration = snapshot.getLong("duration") ?: 0L
                val isActive = snapshot.getBoolean("is_active") ?: true

                val video = Video(
                    id = "header_video",
                    title = title,
                    description = description,
                    videoUrl = videoUrl,
                    thumbnailUrl = thumbnailUrl,
                    duration = duration,
                    isActive = isActive,
                    createdAt = snapshot.getLong("created_at") ?: System.currentTimeMillis(),
                    updatedAt = snapshot.getLong("updated_at") ?: System.currentTimeMillis()
                )

                Log.d(tag, "Fetched video: $video")
                emit(video)
            } else {
                Log.d(tag, "Video document does not exist")
                emit(null)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching video", e)
            emit(null)
        }
    }

    /**
     * Preload video for background caching
     */
    suspend fun preloadVideo(videoUrl: String): Boolean {
        return try {
            Log.d(tag, "Starting video preload for: $videoUrl")
            // This will be handled by ExoPlayer's cache mechanism
            // We'll implement this in the VideoPlayer composable
            true
        } catch (e: Exception) {
            Log.e(tag, "Error preloading video", e)
            false
        }
    }

    /**
     * Fetch customer testimonials from CustomerTestomonials collection
     * Documents have auto-id and fields: customer name, age, testomonial and image url
     */
    fun getCustomerTestimonials(): Flow<List<CustomerTestimonial>> = flow {
        val snapshot = withContext(Dispatchers.IO) {
            firestore.collection("CustomerTestomonials")
                .get()
                .await()
        }

        val items = snapshot.documents.map { doc ->
            val name = doc.getString("customer_name")
                ?: doc.getString("customerName")
                ?: doc.getString("name")
                ?: ""

            val age = try {
                doc.getLong("age")?.toInt()
                    ?: doc.getString("age")?.toIntOrNull()
                    ?: 0
            } catch (_: Exception) { 0 }

            val testimonial = doc.getString("testomonial")
                ?: doc.getString("testimonial")
                ?: doc.getString("review")
                ?: ""

            val imageUrl = doc.getString("image_url")
                ?: doc.getString("imageUrl")
                ?: doc.getString("image")
                ?: ""

            val productId = doc.getString("productId")
                ?: doc.getString("product_id")
                ?: null

            CustomerTestimonial(
                id = doc.id,
                customerName = name,
                age = age,
                testimonial = testimonial,
                imageUrl = imageUrl,
                productId = productId
            )
        }

        emit(items)
    }.catch { e ->
        Log.e(tag, "Error fetching customer testimonials", e)
        emit(emptyList())
    }

    /**
     * Fetch editorial images from Editorial collection
     * Documents: image_pos (1..10), image url. Sorted by image_pos ascending
     */
    fun getEditorialImages(): Flow<List<EditorialImage>> = flow {
        val snapshot = withContext(Dispatchers.IO) {
            firestore.collection("Editorial")
                .get()
                .await()
        }

        val items = snapshot.documents.map { doc ->
            val pos = try {
                doc.getLong("image_pos")?.toInt()
                    ?: doc.getString("image_pos")?.toIntOrNull()
                    ?: doc.getLong("position")?.toInt()
                    ?: doc.getString("position")?.toIntOrNull()
                    ?: 0
            } catch (_: Exception) { 0 }

            val imageUrl = doc.getString("image_url")
                ?: doc.getString("imageUrl")
                ?: doc.getString("image")
                ?: ""

            val productId = doc.getString("productId")
                ?: doc.getString("product_id")
                ?: null

            EditorialImage(
                id = doc.id,
                imagePos = pos,
                imageUrl = imageUrl,
                productId = productId
            )
        }
            .sortedBy { it.imagePos }

        emit(items)
    }.catch { e ->
        Log.e(tag, "Error fetching editorial images", e)
        emit(emptyList())
    }

    /**
     * Fetch orders by customer ID from orders collection
     * Orders are sorted by timestamp descending (newest first) in memory to avoid Firebase index requirement
     */
    fun getOrdersByCustomerId(customerId: String): Flow<List<Order>> = flow {
        try {
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("orders")
                    .whereEqualTo("customerId", customerId)
                    .get()
                    .await()
            }

            val orders = snapshot.documents.map { doc ->
                // Parse items array
                val itemsList = mutableListOf<OrderItem>()
                val itemsArray = doc.get("items") as? List<Map<String, Any>> ?: emptyList()
                
                itemsArray.forEach { itemMap ->
                    val orderItem = OrderItem(
                        productId = itemMap["productId"] as? String ?: "",
                        productName = itemMap["productName"] as? String ?: "",
                        quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 0,
                        weight = (itemMap["weight"] as? Number)?.toDouble() ?: 0.0,
                        price = (itemMap["price"] as? Number)?.toDouble() ?: 0.0
                    )
                    itemsList.add(orderItem)
                }

                Order(
                    id = doc.getString("id") ?: doc.id,
                    customerId = doc.getString("customerId") ?: "",
                    items = itemsList,
                    subtotal = (doc.get("subtotal") as? Number)?.toDouble() ?: 0.0,
                    discountAmount = (doc.get("discountAmount") as? Number)?.toDouble() ?: 0.0,
                    gstAmount = (doc.get("gstAmount") as? Number)?.toDouble() ?: 0.0,
                    totalAmount = (doc.get("totalAmount") as? Number)?.toDouble() ?: 0.0,
                    paymentMethod = doc.getString("paymentMethod") ?: "",
                    status = doc.getString("status") ?: "",
                    isGstIncluded = doc.getBoolean("isGstIncluded") ?: false,
                    timestamp = doc.getLong("timestamp") ?: doc.getLong("createdAt") ?: 0L,
                    invoiceUrl = doc.getString("invoiceUrl") ?: ""
                )
            }
                .sortedByDescending { it.timestamp } // Sort in memory instead of using orderBy

            Log.d(tag, "Fetched ${orders.size} orders for customer: $customerId")
            emit(orders)
        } catch (e: Exception) {
            Log.e(tag, "Error fetching orders for customer: $customerId", e)
            emit(emptyList())
        }
    }.catch { e ->
        Log.e(tag, "Error in orders flow", e)
        emit(emptyList())
    }

    /**
     * Fetch user balance from users collection
     * Document ID is userId, field is "balance"
     * Returns inverted balance (positive becomes negative, negative becomes positive)
     */
    suspend fun getUserBalance(userId: String): Double? {
        return try {
            val doc = withContext(Dispatchers.IO) {
                firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()
            }

            if (doc.exists()) {
                val balance = (doc.get("balance") as? Number)?.toDouble() ?: 0.0
                // Invert the balance: positive becomes negative, negative becomes positive
                val invertedBalance = -balance
                Log.d(tag, "Fetched balance for user $userId: $balance (inverted: $invertedBalance)")
                invertedBalance
            } else {
                Log.w(tag, "User document not found for userId: $userId")
                null
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching balance for user: $userId", e)
            null
        }
    }

}