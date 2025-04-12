package com.example.jewelleryapp.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.example.jewelleryapp.model.Category
import com.example.jewelleryapp.model.Collection
import com.example.jewelleryapp.model.Product
import com.example.jewelleryapp.model.CarouselItem
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
    private val userId: String,
    private val firestore: FirebaseFirestore) {
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

    private suspend fun fetchProductsByIds(productIds: List<String>): List<Product> = coroutineScope {
        try {
            Log.d(tag, "Fetching products for IDs: $productIds")

            // Process in batches of 10 (Firestore limitation) but fetch asynchronously
            val productBatches = productIds.chunked(10).map { batch ->
                async(Dispatchers.IO) {
                    Log.d(tag, "Fetching batch of products: $batch")

                    // Process each product document in parallel within the batch
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

                    // Await all document fetches in this batch
                    docDeferred.awaitAll().filterNotNull()
                }
            }

            // Wait for all batches to complete
            val allDocuments = productBatches.awaitAll().flatten()

            // Process image URLs in parallel
            val products = allDocuments.map { doc ->
                async(Dispatchers.Default) {
                    // Get the image URL from the images array - direct HTTPS URLs
                    val images = doc.get("images") as? List<*>
                    val imageUrl = when {
                        images.isNullOrEmpty() -> ""
                        images[0] is String -> images[0] as String
                        else -> ""
                    }

                    // Check wishlist status from cache
                    val isFavorite = wishlistCache[doc.id] == true

                    Product(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        currency = "Rs",
                        imageUrl = imageUrl, // Direct HTTPS URL
                        isFavorite = isFavorite,
                        category = doc.getString("type") ?: ""
                    )
                }
            }.awaitAll()

            Log.d(tag, "Successfully fetched ${products.size} products")
            return@coroutineScope products

        } catch (e: Exception) {
            Log.e(tag, "Error fetching products by IDs: $productIds", e)
            throw e  // Re-throw to handle in ViewModel
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
                batchResults.map { doc ->
                    async(Dispatchers.Default) {
                        // Direct HTTPS URL - no conversion needed
                        val imageUrl = ((doc.get("images") as? List<*>)?.firstOrNull() ?: "").toString()

                        // Check wishlist status from cache
                        val productId = doc.getString("id") ?: doc.id
                        val isFavorite = wishlistCache[productId] == true

                        Product(
                            id = productId,
                            name = doc.getString("name") ?: "",
                            price = doc.getDouble("price") ?: 0.0,
                            currency = "Rs", // Using Rs as default currency
                            imageUrl = imageUrl,
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

    // This would be called when a user views a product
//    suspend fun recordProductView(userId: String, productId: String) {
//        // In a real app, you'd store this in a user's recently viewed collection
//        try {
//            withContext(Dispatchers.IO) {
//                firestore.collection("users")
//                    .document(userId)
//                    .collection("recently_viewed")
//                    .document(productId)
//                    .set(mapOf(
//                        "timestamp" to com.google.firebase.Timestamp.now()
//                    ))
//
//                Log.d(tag, "Recorded product view for user $userId, product $productId")
//            }
//        } catch (e: Exception) {
//            Log.e(tag, "Error recording product view", e)
//        }
//    }

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

     fun getProductDetails(productId: String): Flow<Product> = flow {
        try {
            // Make sure wishlist cache is up to date in the background
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
                // Ensure cache refresh completes
                refreshCacheJob.await()

                // Get the first image URL from the images array - direct HTTPS URLs
                val imageUrls = documentSnapshot.get("images") as? List<*>
                val imageUrl = imageUrls?.firstOrNull()?.toString() ?: ""

                // Check wishlist status
                val isInWishlist = wishlistCache[productId] == true

                val product = Product(
                    id = documentSnapshot.id,
                    name = documentSnapshot.getString("name") ?: "",
                    price = documentSnapshot.getDouble("price") ?: 0.0,
                    currency = documentSnapshot.getString("currency") ?: "Rs",
                    categoryId = documentSnapshot.getString("category_id") ?: "",
                    imageUrl = imageUrl,
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

     fun getProductsByCategory(categoryId: String, excludeProductId: String? = null): Flow<List<Product>> = flow {
        try {
            // Refresh wishlist cache in the background
            val refreshCacheJob = withContext(Dispatchers.Default) {
                async { refreshWishlistCache() }
            }

            // Step 1: Get product IDs from category_products
            val categorySnapshot = withContext(Dispatchers.IO) {
                firestore.collection("category_products")
                    .document(categoryId)
                    .get()
                    .await()
            }

            val productIds = categorySnapshot.get("product_ids") as? List<String> ?: emptyList()

            // Wait for cache refresh to complete
            refreshCacheJob.await()

            // Step 2: Query products asynchronously using whereIn (max 10)
            if (productIds.isNotEmpty()) {
                val products = coroutineScope {
                    val batchResults = productIds.take(10).chunked(5).map { batch ->
                        async(Dispatchers.IO) {
                            val snapshot = firestore.collection("products")
                                .whereIn("id", batch)
                                .get()
                                .await()

                            snapshot.documents
                                .filter { it.id != excludeProductId }
                        }
                    }.awaitAll().flatten()

                    // Process each document in parallel
                    batchResults.map { doc ->
                        async(Dispatchers.Default) {
                            val imageUrls = doc.get("images") as? List<*>
                            val imageUrl = imageUrls?.firstOrNull()?.toString() ?: ""

                            // Check wishlist status from cache
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
                        }
                    }.awaitAll()
                }

                Log.d(tag, "Fetched ${products.size} products for category $categoryId")
                emit(products)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching products by category", e)
            emit(emptyList())
        }
    }
}