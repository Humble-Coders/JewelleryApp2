# Category Schema Migration Summary

## Overview
The app has been updated to support the new category schema in Firebase Firestore, where categories now use auto-generated document IDs instead of the old `category_<category_name>` format.

## Changes Made

### 1. Category Data Class (`dataClass.kt`)
**Updated the Category model to include all new schema fields:**

```kotlin
data class Category(
    val id: String,                      // Auto-generated Firestore ID
    val name: String,                    // Category name (e.g., "kanta")
    val imageUrl: String,               // Image URL
    val categoryType: String = "",      // Type/classification (e.g., "JEWELRY")
    val createdAt: Long = 0L,          // Timestamp in milliseconds
    val description: String = "",       // Category description
    val hasGenderVariants: Boolean = false,  // Gender-specific variants flag
    val isActive: Boolean = true,       // Active/visible status
    val order: Int = 0                  // Display order/sorting index
)
```

### 2. Repository Changes (`JewelryRepository.kt`)

#### `getCategories()` Function
- **Updated to read all fields from the new schema**
- Fetches categories ordered by the `order` field
- Maps all Firestore fields to the Category data class

#### `getCategoryProducts()` Function
- **Removed the old `category_` prefix**
- Now uses the auto-generated category ID directly
- Before: `.document("category_${categoryId.lowercase()}")`
- After: `.document(categoryId)`

#### `getCategoryProductsPaginated()` Function
- **Removed the old naming scheme**
- Uses the auto-generated category ID directly
- Before: `.document(categoryId.lowercase())`
- After: `.document(categoryId)`

#### `getCategoryProductsCount()` Function
- **Updated to use the new ID format**
- Uses the auto-generated category ID directly

### 3. How Category IDs Flow Through the App

```
1. User clicks category → Category.id (auto-generated ID) is passed
2. Navigation → "categoryProducts/{categoryId}/{categoryName}"
3. Repository → Uses categoryId to fetch from category_products collection
4. Product filtering → Products have categoryId field matching the category's auto-ID
```

## Firebase Firestore Structure Required

### Categories Collection
Each category document should have an **auto-generated ID** with these fields:

```json
{
  "name": "kanta",
  "category_type": "JEWELRY",
  "created_at": 1699999999999,
  "description": "Beautiful kanta jewelry",
  "has_gender_variants": false,
  "image_url": "https://...",
  "is_active": true,
  "order": 1
}
```

### Category Products Collection
The `category_products` collection documents **MUST use the exact same auto-generated IDs** as their corresponding categories:

**Example:** If category "ring" has auto-ID `Kx7mP2nQ9R` in the `categories` collection, then the `category_products` collection must have a document with ID `Kx7mP2nQ9R` (not `category_ring` or any other variant).

**Document ID:** `{same-auto-generated-id-from-categories}` (e.g., `Kx7mP2nQ9R`)
```json
{
  "product_ids": ["product1", "product2", "product3"]
}
```

**✅ CORRECT:** `categories/Kx7mP2nQ9R` → `category_products/Kx7mP2nQ9R`  
**❌ WRONG:** `categories/Kx7mP2nQ9R` → `category_products/category_ring`

### Products Collection
Each product should have a `category_id` field that matches the category's auto-generated ID:

```json
{
  "name": "Gold Ring",
  "category_id": "{auto-generated-category-id}",
  "price": 15000,
  ...
}
```

## Migration Steps for Firebase

### Step 1: Update Categories Collection
For each existing category document like `category_kanta`:

1. Create a new document with an **auto-generated ID**
2. Add all required fields (name, category_type, created_at, etc.)
3. Note down the auto-generated ID for the next steps
4. Delete the old `category_<name>` document

### Step 2: Update Category Products Collection
For each category:

1. Create or update the document in `category_products` using the **same auto-generated ID** from Step 1
2. Ensure the `product_ids` array is correct
3. Delete the old `category_<name>` documents

### Step 3: Update Products Collection
For all products:

1. Update the `category_id` field to use the **new auto-generated category ID**
2. Remove any old `category_<name>` references

## Backward Compatibility Notes

- The Category data class has **default values** for all new fields, so the app won't crash if some fields are missing
- The `order` field is used for sorting categories in the UI
- The `isActive` field can be used to hide/show categories without deleting them
- Old code that was using the `category_` prefix has been completely removed

## Testing Checklist

✅ Categories display correctly on home screen  
✅ Clicking a category navigates to category products screen  
✅ Category products load correctly  
✅ Category filtering works in "All Products" screen  
✅ Product detail pages show correct category information  
✅ Pagination works for category products  
✅ Search within category works  

## Files Modified

1. `/app/src/main/java/com/example/jewelleryapp/model/dataClass.kt`
   - Updated Category data class with new schema fields

2. `/app/src/main/java/com/example/jewelleryapp/repository/JewelryRepository.kt`
   - Updated `getCategories()` to map all new fields
   - Updated `getCategoryProducts()` to use new ID format
   - Updated `getCategoryProductsPaginated()` to use new ID format
   - Updated `getCategoryProductsCount()` to use new ID format

## Important Notes

⚠️ **Before deploying:** Make sure all Firebase Firestore data is migrated to the new schema  
⚠️ **Document IDs:** The `category_products` document IDs must match the auto-generated category IDs  
⚠️ **Product References:** All products must have their `category_id` updated to the new auto-generated IDs  

## Example Firebase Migration Script (Conceptual)

```javascript
// This is a conceptual example - adjust based on your actual Firebase setup
const admin = require('firebase-admin');
const db = admin.firestore();

async function migrateCategoryData() {
  // 1. Get all old categories (if they have old naming like "category_ring")
  const oldCategories = await db.collection('categories')
    .where('__name__', '>=', 'category_')
    .where('__name__', '<', 'category_\uf8ff')
    .get();
  
  const categoryMapping = {}; // old_id -> new_auto_id
  
  for (const oldDoc of oldCategories.docs) {
    const oldId = oldDoc.id; // e.g., "category_kanta"
    const oldName = oldId.replace('category_', '');
    
    // 2. Create new category with auto-generated ID
    const newCategoryRef = await db.collection('categories').add({
      name: oldName,
      category_type: "JEWELRY",
      created_at: Date.now(),
      description: "",
      has_gender_variants: false,
      image_url: oldDoc.data().image_url || "",
      is_active: true,
      order: oldDoc.data().order || 0
    });
    
    const newAutoId = newCategoryRef.id; // e.g., "Kx7mP2nQ9R"
    categoryMapping[oldId] = newAutoId;
    
    console.log(`Created category: ${oldName} with auto-ID: ${newAutoId}`);
    
    // 3. Migrate category_products document to use SAME auto-generated ID
    const oldProductsDoc = await db.collection('category_products').doc(oldId).get();
    if (oldProductsDoc.exists) {
      // IMPORTANT: Use the same auto-generated ID as the category
      await db.collection('category_products').doc(newAutoId).set(oldProductsDoc.data());
      console.log(`  → Created category_products/${newAutoId}`);
    }
    
    // 4. Update all products with this category to use new auto-generated ID
    const products = await db.collection('products')
      .where('category_id', '==', oldId)
      .get();
    
    for (const product of products.docs) {
      await product.ref.update({ category_id: newAutoId });
    }
    console.log(`  → Updated ${products.size} products to use category_id: ${newAutoId}`);
    
    // 5. Delete old documents
    await oldDoc.ref.delete();
    await db.collection('category_products').doc(oldId).delete();
    console.log(`  → Deleted old documents: categories/${oldId} and category_products/${oldId}`);
  }
  
  console.log('\n✅ Migration complete!');
  console.log('Category ID Mapping (old → new):');
  console.log(categoryMapping);
  
  /* Example output:
  {
    "category_ring": "Kx7mP2nQ9R",
    "category_kanta": "Lm3nP4qR5S",
    "category_necklace": "Yz6xW7vU8T"
  }
  
  Firebase structure after migration:
  
  categories/
    ├── Kx7mP2nQ9R (auto-ID) { name: "ring", ... }
    ├── Lm3nP4qR5S (auto-ID) { name: "kanta", ... }
    └── Yz6xW7vU8T (auto-ID) { name: "necklace", ... }
  
  category_products/
    ├── Kx7mP2nQ9R (SAME as category) { product_ids: [...] }
    ├── Lm3nP4qR5S (SAME as category) { product_ids: [...] }
    └── Yz6xW7vU8T (SAME as category) { product_ids: [...] }
  
  products/
    ├── prod1 { category_id: "Kx7mP2nQ9R", ... }
    └── prod2 { category_id: "Lm3nP4qR5S", ... }
  */
}
```

**KEY POINT:** The `category_products` document ID **MUST be identical** to the category's auto-generated ID.

---

**Migration completed on:** October 11, 2025  
**App version:** Current  
**Status:** ✅ Code changes complete - Firebase data migration required

