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
            // Get product IDs from category_products collection
            val categoryDoc = withContext(Dispatchers.IO) {
                firestore.collection("category_products")
                    .document("category_${categoryId.lowercase()}")
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

                    // Use first image as primary, but store all images
                    val primaryImageUrl = imageUrls.firstOrNull() ?: ""

                    val isFavorite = wishlistCache[doc.id] == true
                    val materialId = doc.getString("material_id")
                    val materialType = doc.getString("material_type")

                    Log.d(tag, "Product ${doc.id}: Found ${imageUrls.size} images: $imageUrls")

                    Product(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        currency = "Rs",
                        imageUrl = primaryImageUrl, // Keep for backward compatibility
                        imageUrls = imageUrls, // Add new field for all images
                        isFavorite = isFavorite,
                        category = doc.getString("type") ?: "",
                        materialId = materialId,
                        materialType = materialType
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

    // Fix for getCategories function
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
                        // Direct HTTPS URLs - no conversion needed
                        val imageUrl = doc.getString("image_url") ?: ""

                        Category(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            imageUrl = imageUrl
                        )
                    }
                }.awaitAll()
            }

            Log.d(tag, "Fetched ${categories.size} categories")
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
                        // Direct HTTPS URL - no conversion needed
                        val imageUrl = doc.getString("imageUrl") ?: ""

                        Collection(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            imageUrl = imageUrl,
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

            // Fetch products asynchronously in batches
            val products = coroutineScope {
                // Firebase allows a maximum of 10 items in a whereIn query
                // Process batches in parallel
                val batchResults = productIds.chunked(10).map { batch ->
                    async(Dispatchers.IO) {
                        val snapshot = firestore.collection("products")
                            .whereIn("id", batch)
                            .get()
                            .await()

                        snapshot.documents
                    }
                }.awaitAll().flatten()

                // Process each document in parallel
                // REPLACE the product creation part in getFeaturedProducts method with this:

// Process each document in parallel
                batchResults.map { doc ->
                    async(Dispatchers.Default) {
                        // Get all images from the images array
                        val images = doc.get("images") as? List<*>
                        val imageUrls = images?.mapNotNull { it as? String }?.filter { it.isNotBlank() } ?: emptyList()

                        // Use first image as primary
                        val primaryImageUrl = imageUrls.firstOrNull() ?: ""

                        // Check wishlist status from cache
                        val productId = doc.getString("id") ?: doc.id
                        val isFavorite = wishlistCache[productId] == true

                        Log.d(tag, "Featured Product $productId: Found ${imageUrls.size} images: $imageUrls")

                        Product(
                            id = productId,
                            name = doc.getString("name") ?: "",
                            price = doc.getDouble("price") ?: 0.0,
                            currency = "Rs", // Using Rs as default currency
                            imageUrl = primaryImageUrl, // Keep for backward compatibility
                            imageUrls = imageUrls, // Add all images for cycling
                            isFavorite = isFavorite
                        )
                    }
                }.awaitAll()
            }

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

                val isInWishlist = wishlistCache[productId] == true

                Log.d(tag, "Product $productId: Found ${imageUrls.size} images")

                val product = Product(
                    id = documentSnapshot.id,
                    name = documentSnapshot.getString("name") ?: "",
                    price = documentSnapshot.getDouble("price") ?: 0.0,
                    currency = documentSnapshot.getString("currency") ?: "Rs",
                    categoryId = documentSnapshot.getString("category_id") ?: "",
                    imageUrl = primaryImageUrl, // Keep for backward compatibility
                    imageUrls = imageUrls, // Add new field for all images
                    materialId = documentSnapshot.getString("material_id"),
                    materialType = documentSnapshot.getString("material_type"),
                    stone = documentSnapshot.getString("stone") ?: "",
                    clarity = documentSnapshot.getString("clarity") ?: "",
                    cut = documentSnapshot.getString("cut") ?: "",
                    description = documentSnapshot.getString("description") ?: "",
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
                            val imageUrls = doc.get("images") as? List<*>
                            val imageUrl = imageUrls?.firstOrNull()?.toString() ?: ""
                            val isInWishlist = wishlistCache[doc.id] == true

                            Product(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                price = doc.getDouble("price") ?: 0.0,
                                currency = doc.getString("currency") ?: "Rs",
                                categoryId = doc.getString("category_id") ?: "",
                                imageUrl = imageUrl,
                                material = doc.getString("material") ?: "",
                                stone = doc.getString("stone") ?: "",
                                clarity = doc.getString("clarity") ?: "",
                                cut = doc.getString("cut") ?: "",
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

            // First get product IDs from category_products collection
            val categoryDoc = withContext(Dispatchers.IO) {
                firestore.collection("category_products")
                    .document(categoryId.lowercase()) // Remove "category_" prefix here too
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
            val categoryDoc = withContext(Dispatchers.IO) {
                firestore.collection("category_products")
                    .document(categoryId.lowercase()) // Remove "category_" prefix here too
                    .get()
                    .await()
            }

            val productIds = (categoryDoc.get("product_ids") as? List<*>)?.size ?: 0
            Log.d(tag, "Category $categoryId has $productIds total products")
            productIds

        } catch (e: Exception) {
            Log.e(tag, "Error getting category products count", e)
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
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("gold_silver_rates")
                    .document("current_rates")
                    .get()
                    .await()
            }

            if (snapshot.exists()) {
                val rateChangePercentage = snapshot.get("rate_change_percentage") as? Map<String, String> ?: emptyMap()

                val rates = GoldSilverRates(
                    goldRatePerGram = snapshot.getDouble("gold_rate_per_gram") ?: 0.0,
                    silverRatePerGram = snapshot.getDouble("silver_rate_per_gram") ?: 0.0,
                    lastUpdated = snapshot.getLong("last_updated") ?: 0L,
                    previousGoldRate = snapshot.getDouble("previous_gold_rate") ?: 0.0,
                    previousSilverRate = snapshot.getDouble("previous_silver_rate") ?: 0.0,
                    currency = snapshot.getString("currency") ?: "INR",
                    rateChangePercentage = rateChangePercentage
                )
                emit(rates)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching gold silver rates", e)
            throw e
        }
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
                            // Get all images from the images array
                            val images = doc.get("images") as? List<*>
                            val imageUrls = images?.mapNotNull { it as? String }?.filter { it.isNotBlank() } ?: emptyList()
                            val primaryImageUrl = imageUrls.firstOrNull() ?: ""

                            val isInWishlist = wishlistCache[doc.id] == true

                            Product(
                                id = doc.id,
                                name = doc.getString("name") ?: "",
                                price = doc.getDouble("price") ?: 0.0,
                                currency = doc.getString("currency") ?: "Rs",
                                imageUrl = primaryImageUrl,
                                imageUrls = imageUrls,
                                isFavorite = isInWishlist,
                                category = doc.getString("type") ?: "",
                                categoryId = doc.getString("category_id") ?: "",
                                materialId = doc.getString("material_id"),
                                materialType = doc.getString("material_type"),
                                stone = doc.getString("stone") ?: "",
                                clarity = doc.getString("clarity") ?: "",
                                cut = doc.getString("cut") ?: "",
                                description = doc.getString("description") ?: ""
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

}