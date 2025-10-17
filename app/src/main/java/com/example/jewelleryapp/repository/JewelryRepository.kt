package com.example.jewelleryapp.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.example.jewelleryapp.model.Category
import com.example.jewelleryapp.model.Collection
import com.example.jewelleryapp.model.Product
import com.example.jewelleryapp.model.CarouselItem
import com.example.jewelleryapp.model.GoldSilverRates
import com.example.jewelleryapp.model.Material
import com.example.jewelleryapp.model.StoreInfo
import com.example.jewelleryapp.model.Video
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.async
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
    
    /**
     * Calculate jewelry price based on material rates, weights, and charges
     * Implements comprehensive pricing logic for 22K/18K jewelry
     */
    private suspend fun calculateJewelryPrice(product: com.google.firebase.firestore.DocumentSnapshot): Double {
        try {
            Log.d(tag, "=== Starting price calculation for ${product.id} ===")
            
            // Get material information
            val materialId = product.getString("material_id") ?: ""
            Log.d(tag, "Material ID: $materialId")
            
            if (materialId.isBlank()) {
                Log.e(tag, "Material ID is blank, cannot calculate price")
                return 0.0
            }
            
            val materialName = getMaterialName(materialId)
            Log.d(tag, "Material Name: $materialName")
            
            if (materialName.isBlank()) {
                Log.e(tag, "Material name is blank for ID: $materialId, cannot calculate price")
                return 0.0
            }
            
            val materialType = product.getString("material_type") ?: "24K"
            Log.d(tag, "Material Type: $materialType")
            
            // Extract purity (e.g., "22K" -> 22)
            val purity = materialType.replace("K", "").replace("k", "").toIntOrNull() ?: 24
            Log.d(tag, "Purity: $purity K")
            
            // Get weights
            val grossWeight = parseDoubleField(product, "total_weight")
            val lessWeight = parseDoubleField(product, "less_weight")
            val storedNetWeight = parseDoubleField(product, "net_weight")
            val cwWeight = parseDoubleField(product, "cw_weight")
            
            // Calculate net weight as gross - less (correct formula)
            val netWeight = if (grossWeight > 0 && lessWeight > 0) {
                grossWeight - lessWeight
            } else {
                storedNetWeight // Fallback to stored value if calculation not possible
            }
            
            Log.d(tag, "Weights - Gross: $grossWeight, Less: $lessWeight, Net: $netWeight (calculated), Stone: $cwWeight")
            
            // Get charges and rates
            val makingRate = parseDoubleField(product, "default_making_rate")
            val stoneRate = parseDoubleField(product, "stone_rate")
            val stoneQuantity = parseDoubleField(product, "stone_quantity")
            val vaCharges = parseDoubleField(product, "va_charges")
            val discountPercent = parseDoubleField(product, "discount_percent")
            val gstRate = parseDoubleField(product, "gst_rate").takeIf { it > 0 } ?: 3.0
            val saleType = product.getString("sale_type") ?: "intrastate"
            val includeGstInPrice = product.getBoolean("include_gst_in_price") ?: true
            val qty = parseIntField(product, "quantity").takeIf { it > 0 } ?: 1
            
            Log.d(tag, "Charges - Making: $makingRate (5% GST), Stone Rate: $stoneRate, Stone Qty: $stoneQuantity (3% GST), VA: $vaCharges (3% GST), Discount: $discountPercent%, SaleType: $saleType, IncludeGST: $includeGstInPrice, Qty: $qty")
            
            // Validation
            if (netWeight <= 0) {
                Log.e(tag, "Net weight is 0 or negative: $netWeight")
                return 0.0
            }
            
            if (grossWeight > 0 && grossWeight < lessWeight) {
                Log.w(tag, "Gross weight ($grossWeight) < Less weight ($lessWeight) - using net weight anyway")
            }
            
            // Get 24K material rate from cache or fetch
            val material24KRate = getMaterial24KRate(materialName.lowercase())
            Log.d(tag, "24K Rate for $materialName: ₹$material24KRate/g")
            
            if (material24KRate == 0.0) {
                Log.e(tag, "No 24K rate found for material: $materialName")
                return 0.0
            }
            
            // Calculate purity factor (e.g., 22K = 22/24 = 0.9167)
            // Round to 4 decimal places to match admin app precision
            val purityFactor = kotlin.math.round((purity / 24.0) * 10000.0) / 10000.0
            Log.d(tag, "Purity Factor: $purityFactor (for $purity K)")
            
            // Step 2: Base Material Amount (using purity-adjusted rate)
            val effectiveMaterialRate = material24KRate * purityFactor
            val materialAmount = (netWeight * effectiveMaterialRate * qty).round(2)
            Log.d(tag, "Step 2 - Effective Rate: ₹$effectiveMaterialRate/g, Material Amount: ₹$materialAmount")
            
            // Step 3: Making Charges
            val makingCharges = (netWeight * makingRate * qty).round(2)
            Log.d(tag, "Step 3 - Making Charges: ₹$makingCharges")
            
            // Step 4: Stone Charges (correct formula: stoneRate * stoneQuantity * cwWeight)
            val stoneCharges = (stoneRate * stoneQuantity * cwWeight * qty).round(2)
            Log.d(tag, "Step 4 - Stone Charges: ₹$stoneCharges (stoneRate=$stoneRate * stoneQty=$stoneQuantity * cwWeight=$cwWeight * qty=$qty)")
            
            // Step 5: Total Before Discount
            val totalBeforeDiscount = (materialAmount + makingCharges + stoneCharges + vaCharges).round(2)
            Log.d(tag, "Step 5 - Total Before Discount: ₹$totalBeforeDiscount (Material: ₹$materialAmount + Making: ₹$makingCharges + Stone: ₹$stoneCharges + VA: ₹$vaCharges)")
            
            // Step 6: Apply Discount
            val discountAmount = (totalBeforeDiscount * (discountPercent / 100.0)).round(2)
            val amountAfterDiscount = (totalBeforeDiscount - discountAmount).round(2)
            Log.d(tag, "Step 6 - Discount: ₹$discountAmount, After Discount: ₹$amountAfterDiscount")
            
            // Step 7: Calculate Tax (Differential GST rates)
            // Metal + Stone: 3% GST, Making Charges: 5% GST
            val metalStoneAmount = materialAmount + stoneCharges
            val makingAmount = makingCharges
            val otherAmount = vaCharges // VA charges typically follow the same rate as metal/stone
            
            Log.d(tag, "Step 7 - Tax Calculation: Metal+Stone=₹$metalStoneAmount (3% GST), Making=₹$makingAmount (5% GST), Other=₹$otherAmount (3% GST)")
            
            val totalTax = when (saleType.lowercase()) {
                "intrastate" -> {
                    // 3% GST on metal, stone, and VA charges
                    val metalStoneCgst = (metalStoneAmount * (3.0 / 2.0 / 100.0)).round(2)
                    val metalStoneSgst = (metalStoneAmount * (3.0 / 2.0 / 100.0)).round(2)
                    val otherCgst = (otherAmount * (3.0 / 2.0 / 100.0)).round(2)
                    val otherSgst = (otherAmount * (3.0 / 2.0 / 100.0)).round(2)
                    
                    // 5% GST on making charges
                    val makingCgst = (makingAmount * (5.0 / 2.0 / 100.0)).round(2)
                    val makingSgst = (makingAmount * (5.0 / 2.0 / 100.0)).round(2)
                    
                    val totalCgst = metalStoneCgst + otherCgst + makingCgst
                    val totalSgst = metalStoneSgst + otherSgst + makingSgst
                    
                    Log.d(tag, "Step 7 - Intrastate: CGST=₹$totalCgst (Metal+Stone: ₹$metalStoneCgst, Making: ₹$makingCgst, Other: ₹$otherCgst), SGST=₹$totalSgst")
                    (totalCgst + totalSgst).round(2)
                }
                "interstate" -> {
                    // 3% IGST on metal, stone, and VA charges
                    val metalStoneIgst = (metalStoneAmount * (3.0 / 100.0)).round(2)
                    val otherIgst = (otherAmount * (3.0 / 100.0)).round(2)
                    
                    // 5% IGST on making charges
                    val makingIgst = (makingAmount * (5.0 / 100.0)).round(2)
                    
                    val totalIgst = metalStoneIgst + otherIgst + makingIgst
                    Log.d(tag, "Step 7 - Interstate: IGST=₹$totalIgst (Metal+Stone: ₹$metalStoneIgst, Making: ₹$makingIgst, Other: ₹$otherIgst)")
                    totalIgst
                }
                else -> {
                    Log.d(tag, "Step 7 - No tax (unknown sale type: $saleType)")
                    0.0
                }
            }
            
            // Step 8: Final Amount (with or without GST based on configuration)
            val finalAmount = if (includeGstInPrice) {
                (amountAfterDiscount + totalTax).round(2)
            } else {
                amountAfterDiscount.round(2)
            }
            
            Log.d(tag, "Step 8 - Total Tax: ₹$totalTax, FINAL AMOUNT (includeGST=$includeGstInPrice): ₹$finalAmount")
            
            Log.d(tag, "=== Price calculation complete for ${product.id}: ₹$finalAmount ===")
            
            return finalAmount
            
        } catch (e: Exception) {
            Log.e(tag, "Error calculating price for product ${product.id}", e)
            e.printStackTrace()
            return 0.0
        }
    }
    
    /**
     * Get 24K material rate (Gold or Silver) with caching
     */
    private suspend fun getMaterial24KRate(materialName: String): Double {
        val cacheKey = materialName.lowercase()
        
        Log.d(tag, "Getting 24K rate for material: $cacheKey")
        
        // Check cache first
        materialRatesCache[cacheKey]?.let { 
            Log.d(tag, "Found in cache: $cacheKey → ₹$it/g")
            return it 
        }
        
        return try {
            Log.d(tag, "Fetching from Firestore: rates where material_type='24K'")
            
            // Get all 24K rates and filter by material name (case-insensitive)
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("rates")
                    .whereEqualTo("material_type", "24K")
                    .get()
                    .await()
            }
            
            Log.d(tag, "Query returned ${snapshot.size()} total 24K rate documents")
            
            // Find the matching material (case-insensitive)
            val matchingDoc = snapshot.documents.firstOrNull { doc ->
                val docMaterialName = doc.getString("material_name")?.lowercase() ?: ""
                val matches = docMaterialName == cacheKey
                Log.d(tag, "Checking document ${doc.id}: material_name='${doc.getString("material_name")}' (lowercase: $docMaterialName) matches '$cacheKey'? $matches")
                matches
            }
            
            val rate = if (matchingDoc != null) {
                val rateValue = parseDoubleField(matchingDoc, "price_per_gram")
                Log.d(tag, "✅ Found rate document: ${matchingDoc.id}, price_per_gram=$rateValue")
                rateValue
            } else {
                Log.w(tag, "❌ No 24K rate document found for material: $cacheKey (searched case-insensitive)")
                0.0
            }
            
            // Cache the result
            if (rate > 0) {
                materialRatesCache[cacheKey] = rate
                Log.d(tag, "✅ Cached 24K rate for $materialName: ₹$rate/g")
            } else {
                Log.e(tag, "❌ Rate is 0 for $materialName, not caching")
            }
            
            rate
        } catch (e: Exception) {
            Log.e(tag, "❌ Error fetching 24K rate for $materialName", e)
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
    
    // Helper function to fetch material name by ID
    private suspend fun getMaterialName(materialId: String): String {
        if (materialId.isBlank()) return ""
        
        // Check cache first
        materialNameCache[materialId]?.let { return it }
        
        return try {
            val doc = withContext(Dispatchers.IO) {
                firestore.collection("materials")
                    .document(materialId)
                    .get()
                    .await()
            }
            
            val materialName = doc.getString("name") ?: ""
            // Cache the result
            if (materialName.isNotBlank()) {
                materialNameCache[materialId] = materialName
            }
            materialName
        } catch (e: Exception) {
            Log.e(tag, "Error fetching material name for ID: $materialId", e)
            ""
        }
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

    // Optimized with async
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
    private suspend fun fetchProductsByIds(productIds: List<String>): List<Product> = coroutineScope {
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
                    
                    // Determine if we should use custom price or calculate it
                    val useCustomPrice = show["custom_price"] ?: false
                    val customPriceValue = parseDoubleField(doc, "custom_price")
                    val netWeightValue = parseDoubleField(doc, "net_weight")
                    val calculatedPrice = if (!useCustomPrice && netWeightValue > 0) {
                        calculateJewelryPrice(doc)
                    } else {
                        0.0
                    }
                    val finalPrice = if (useCustomPrice) customPriceValue else calculatedPrice

                    Log.d(tag, "Product ${doc.id}: Material=$materialName, Images=${imageUrls.size}, UseCustom=$useCustomPrice, Price=₹$finalPrice")

                    Product(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        price = finalPrice,
                        customPrice = customPriceValue,
                        quantity = parseIntField(doc, "quantity"),
                        
                        // Images
                        images = imageUrls,
                        imageUrl = primaryImageUrl,
                        
                        // Category & Material
                        categoryId = doc.getString("category_id") ?: "",
                        materialId = materialId,
                        materialName = materialName,
                        materialType = doc.getString("material_type") ?: "",
                        karat = doc.getString("karat") ?: "",
                        
                        // Weights
                        netWeight = parseDoubleField(doc, "net_weight"),
                        totalWeight = parseDoubleField(doc, "total_weight"),
                        lessWeight = parseDoubleField(doc, "less_weight"),
                        cwWeight = parseDoubleField(doc, "cw_weight"),
                        
                        // Charges & Costs
                        defaultMakingRate = parseDoubleField(doc, "default_making_rate"),
                        vaCharges = parseDoubleField(doc, "va_charges"),
                        totalProductCost = parseDoubleField(doc, "total_product_cost"),
                        discountPercent = parseDoubleField(doc, "discount_percent"),
                        gstRate = parseDoubleField(doc, "gst_rate").takeIf { it > 0 } ?: 3.0,
                        saleType = doc.getString("sale_type") ?: "intrastate",
                        
                        // Stone details
                        hasStones = doc.getBoolean("has_stones") ?: false,
                        stoneName = doc.getString("stone_name") ?: "",
                        stoneColor = doc.getString("stone_color") ?: "",
                        stoneRate = parseDoubleField(doc, "stone_rate"),
                        stoneQuantity = parseDoubleField(doc, "stone_quantity"),
                        
                        // Other properties
                        isOtherThanGold = doc.getBoolean("is_other_than_gold") ?: false,
                        available = doc.getBoolean("available") ?: true,
                        featured = doc.getBoolean("featured") ?: false,
                        barcodeIds = barcodeIds,
                        createdAt = parseLongField(doc, "created_at"),
                        autoGenerateId = doc.getBoolean("auto_generate_id") ?: false,
                        customProductId = doc.getString("custom_product_id"),
                        
                        // UI visibility control
                        show = show,
                        
                        // Client-side
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

                        Log.d(tag, "Collection ${doc.id}: Found ${imageUrls.size} images: $imageUrls")

                        Collection(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            imageUrl = primaryImageUrl,
                            imageUrls = imageUrls,
                            description = doc.getString("description") ?: ""
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

                        CarouselItem(
                            id = doc.id,
                            imageUrl = imageUrl,
                            title = doc.getString("title") ?: "",
                            subtitle = doc.getString("subtitle") ?: "",
                            buttonText = doc.getString("buttonText") ?: ""
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
                
                // Determine if we should use custom price or calculate it
                val useCustomPrice = show["custom_price"] ?: false
                val customPriceValue = parseDoubleField(documentSnapshot, "custom_price")
                val netWeightValue = parseDoubleField(documentSnapshot, "net_weight")
                val calculatedPrice = if (!useCustomPrice && netWeightValue > 0) {
                    calculateJewelryPrice(documentSnapshot)
                } else {
                    0.0
                }
                val finalPrice = if (useCustomPrice) customPriceValue else calculatedPrice

                Log.d(tag, "Product $productId: Material=$materialName, Images=${imageUrls.size}, UseCustom=$useCustomPrice, Price=₹$finalPrice")

                val product = Product(
                    id = documentSnapshot.id,
                    name = documentSnapshot.getString("name") ?: "",
                    description = documentSnapshot.getString("description") ?: "",
                    price = finalPrice,
                    customPrice = customPriceValue,
                    quantity = parseIntField(documentSnapshot, "quantity"),
                    
                    // Images
                    images = imageUrls,
                    imageUrl = primaryImageUrl,
                    
                    // Category & Material
                    categoryId = documentSnapshot.getString("category_id") ?: "",
                    materialId = materialId,
                    materialName = materialName,
                    materialType = documentSnapshot.getString("material_type") ?: "",
                    karat = documentSnapshot.getString("karat") ?: "",
                    
                    // Weights
                    netWeight = parseDoubleField(documentSnapshot, "net_weight"),
                    totalWeight = parseDoubleField(documentSnapshot, "total_weight"),
                    lessWeight = parseDoubleField(documentSnapshot, "less_weight"),
                    cwWeight = parseDoubleField(documentSnapshot, "cw_weight"),
                    
                    // Charges & Costs
                    defaultMakingRate = parseDoubleField(documentSnapshot, "default_making_rate"),
                    vaCharges = parseDoubleField(documentSnapshot, "va_charges"),
                    totalProductCost = parseDoubleField(documentSnapshot, "total_product_cost"),
                    
                    // Stone details
                    hasStones = documentSnapshot.getBoolean("has_stones") ?: false,
                    stoneName = documentSnapshot.getString("stone_name") ?: "",
                    stoneColor = documentSnapshot.getString("stone_color") ?: "",
                    stoneRate = parseDoubleField(documentSnapshot, "stone_rate"),
                    stoneQuantity = parseDoubleField(documentSnapshot, "stone_quantity"),
                    
                    // Other properties
                    isOtherThanGold = documentSnapshot.getBoolean("is_other_than_gold") ?: false,
                    available = documentSnapshot.getBoolean("available") ?: true,
                    featured = documentSnapshot.getBoolean("featured") ?: false,
                    barcodeIds = barcodeIds,
                    createdAt = parseLongField(documentSnapshot, "created_at"),
                    autoGenerateId = documentSnapshot.getBoolean("auto_generate_id") ?: false,
                    customProductId = documentSnapshot.getString("custom_product_id"),
                    
                    // UI visibility control
                    show = show,
                    
                    // Client-side
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
                            
                            // Determine if we should use custom price or calculate it
                            val useCustomPrice = show["custom_price"] ?: false
                            val customPriceValue = parseDoubleField(doc, "custom_price")
                            val netWeightValue = parseDoubleField(doc, "net_weight")
                            val calculatedPrice = if (!useCustomPrice && netWeightValue > 0) {
                                calculateJewelryPrice(doc)
                            } else {
                                0.0
                            }
                            val finalPrice = if (useCustomPrice) customPriceValue else calculatedPrice

                            Product(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                description = doc.getString("description") ?: "",
                                price = finalPrice,
                                customPrice = customPriceValue,
                                quantity = parseIntField(doc, "quantity"),
                                images = imageUrls,
                                imageUrl = primaryImageUrl,
                                categoryId = doc.getString("category_id") ?: "",
                                materialId = materialId,
                                materialName = materialName,
                                materialType = doc.getString("material_type") ?: "",
                                karat = doc.getString("karat") ?: "",
                                netWeight = parseDoubleField(doc, "net_weight"),
                                totalWeight = parseDoubleField(doc, "total_weight"),
                                lessWeight = parseDoubleField(doc, "less_weight"),
                                cwWeight = parseDoubleField(doc, "cw_weight"),
                                defaultMakingRate = parseDoubleField(doc, "default_making_rate"),
                                vaCharges = parseDoubleField(doc, "va_charges"),
                                totalProductCost = parseDoubleField(doc, "total_product_cost"),
                                hasStones = doc.getBoolean("has_stones") ?: false,
                                stoneName = doc.getString("stone_name") ?: "",
                                stoneColor = doc.getString("stone_color") ?: "",
                                stoneRate = parseDoubleField(doc, "stone_rate"),
                                stoneQuantity = parseDoubleField(doc, "stone_quantity"),
                                isOtherThanGold = doc.getBoolean("is_other_than_gold") ?: false,
                                available = doc.getBoolean("available") ?: true,
                                featured = doc.getBoolean("featured") ?: false,
                                barcodeIds = barcodeIds,
                                createdAt = parseLongField(doc, "created_at"),
                                autoGenerateId = doc.getBoolean("auto_generate_id") ?: false,
                                customProductId = doc.getString("custom_product_id"),
                                show = show,
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
        userId = newUserId
        // Clear cache when user ID changes
        wishlistCache.clear()
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
            Log.d(tag, "Fetching gold and silver rates for 24K")
            
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("rates")
                    .whereEqualTo("material_type", "24K")
            
                    .get()
                    .await()
            }

            if (snapshot.isEmpty) {
                Log.w(tag, "No active 24K rates found in rates collection")
                emit(GoldSilverRates())
                return@flow
            }

            var goldRate = 0.0
            var silverRate = 0.0
            var lastUpdated = 0L
            var currency = "INR"

            // Find Gold and Silver rates from the documents
            snapshot.documents.forEach { doc ->
                val materialName = doc.getString("material_name") ?: ""
                val materialType = doc.getString("material_type") ?: ""
                val isActive = doc.getBoolean("is_active") ?: false
                val karat = parseIntField(doc, "karat")
                
                Log.d(tag, "Found rate: $materialName $materialType (karat: $karat, active: $isActive)")
                
                when (materialName.lowercase()) {
                    "gold" -> {
                        goldRate = parseDoubleField(doc, "price_per_gram")
                        // Get latest timestamp from updated_at
                        val docTimestamp = parseLongField(doc, "updated_at")
                        if (docTimestamp > lastUpdated) {
                            lastUpdated = docTimestamp
                        }
                        currency = doc.getString("currency") ?: "INR"
                    }
                    "silver" -> {
                        silverRate = parseDoubleField(doc, "price_per_gram")
                        // Get latest timestamp if not already set
                        val docTimestamp = parseLongField(doc, "updated_at")
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
            
            Log.d(tag, "Rates fetched - Gold: ₹$goldRate/g, Silver: ₹$silverRate/g (24K, active only)")
            emit(rates)
            
        } catch (e: Exception) {
            Log.e(tag, "Error fetching gold silver rates", e)
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
                            
                            // Determine if we should use custom price or calculate it
                            val useCustomPrice = show["custom_price"] ?: false
                            val customPriceValue = parseDoubleField(doc, "custom_price")
                            val netWeightValue = parseDoubleField(doc, "net_weight")
                            val calculatedPrice = if (!useCustomPrice && netWeightValue > 0) {
                                calculateJewelryPrice(doc)
                            } else {
                                0.0
                            }
                            val finalPrice = if (useCustomPrice) customPriceValue else calculatedPrice

                            Product(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                description = doc.getString("description") ?: "",
                                price = finalPrice,
                                customPrice = customPriceValue,
                                quantity = parseIntField(doc, "quantity"),
                                images = imageUrls,
                                imageUrl = primaryImageUrl,
                                categoryId = doc.getString("category_id") ?: "",
                                materialId = materialId,
                                materialName = materialName,
                                materialType = doc.getString("material_type") ?: "",
                                karat = doc.getString("karat") ?: "",
                                netWeight = parseDoubleField(doc, "net_weight"),
                                totalWeight = parseDoubleField(doc, "total_weight"),
                                lessWeight = parseDoubleField(doc, "less_weight"),
                                cwWeight = parseDoubleField(doc, "cw_weight"),
                                defaultMakingRate = parseDoubleField(doc, "default_making_rate"),
                                vaCharges = parseDoubleField(doc, "va_charges"),
                                totalProductCost = parseDoubleField(doc, "total_product_cost"),
                                hasStones = doc.getBoolean("has_stones") ?: false,
                                stoneName = doc.getString("stone_name") ?: "",
                                stoneColor = doc.getString("stone_color") ?: "",
                                stoneRate = parseDoubleField(doc, "stone_rate"),
                                stoneQuantity = parseDoubleField(doc, "stone_quantity"),
                                isOtherThanGold = doc.getBoolean("is_other_than_gold") ?: false,
                                available = doc.getBoolean("available") ?: true,
                                featured = doc.getBoolean("featured") ?: false,
                                barcodeIds = barcodeIds,
                                createdAt = parseLongField(doc, "created_at"),
                                autoGenerateId = doc.getBoolean("auto_generate_id") ?: false,
                                customProductId = doc.getString("custom_product_id"),
                                show = show,
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

    // Add these methods to your existing JewelryRepository class


    /**
     * Add product to recently viewed (limit 10, remove oldest when full)
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
     */
    fun getRecentlyViewedProducts(): Flow<List<Product>> = flow {
        if (!hasValidUser()) {
            Log.e(tag, "Cannot fetch recently viewed: No valid user")
            emit(emptyList())
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
            emit(emptyList())
            return@flow
        }

        // Fetch product details
        val products = fetchProductsByIds(productIds)

        // Maintain the order from recently viewed (most recent first)
        val orderedProducts = productIds.mapNotNull { productId ->
            products.find { it.id == productId }
        }

        Log.d(tag, "Successfully fetched ${orderedProducts.size} recently viewed products")
        emit(orderedProducts)
    }.catch { e ->
        Log.e(tag, "Error fetching recently viewed products", e)
        emit(emptyList())
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

}