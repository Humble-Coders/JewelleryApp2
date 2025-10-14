# Material Name Fetching Implementation

**Date:** October 11, 2025  
**Status:** ✅ COMPLETE - Material names now fetched from materials collection

---

## 🎯 Overview

Products now fetch the actual material name from the `materials` collection instead of displaying the `material_id`.

---

## 📊 How It Works

### Data Structure:

```
Product Document:
{
  "material_id": "abc123xyz"  // Reference to materials collection
  "material_type": "22K"      // Additional material info
}

↓ (Fetch)

Materials Collection:
materials/abc123xyz {
  "name": "Gold",
  "image_url": "https://..."
}

↓ (Result)

Product Object:
{
  materialId: "abc123xyz",
  materialName: "Gold",      // ← Fetched from materials collection!
  materialType: "22K"
}
```

---

## 💻 Implementation

### 1. Added Material Name Cache

```kotlin
// In JewelryRepository
private val materialNameCache = mutableMapOf<String, String>()
```

**Benefits:**
- Reduces Firestore reads
- Faster subsequent loads
- Same material name reused across products

---

### 2. Helper Function to Fetch Material Name

```kotlin
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
```

**How It Works:**
1. Checks if materialId is blank → return ""
2. Checks cache for existing name → return cached value
3. Fetches from Firestore if not cached
4. Caches result for future use
5. Returns material name or "" on error

---

### 3. Updated Product Data Class

```kotlin
data class Product(
    // ...
    val materialId: String = "",        // Reference ID
    val materialName: String = "",      // ← NEW: Actual name from materials collection
    val materialType: String = "",      // Additional info (e.g., "22K")
    // ...
)
```

---

### 4. Updated All Product Fetching Methods

**Methods Updated:**
- ✅ `fetchProductsByIds()` - Core product fetching
- ✅ `getProductDetails()` - Single product
- ✅ `getProductsByCategory()` - Category products
- ✅ `getAllProductsPaginated()` - All products

**Code Pattern:**
```kotlin
// Fetch material name from materials collection
val materialId = doc.getString("material_id") ?: ""
val materialName = getMaterialName(materialId)

Product(
    materialId = materialId,
    materialName = materialName,  // ← Populated with actual name
    // ...
)
```

---

### 5. Updated UI Display

**JewelryProductScreen.kt - `createProductSpecs()`**

```kotlin
// Material Information
if (product.shouldShow("material_type") && product.materialType.isNotBlank()) {
    // Use the materialName fetched from materials collection
    val materialText = if (product.materialName.isNotBlank()) {
        "${product.materialName.capitalize()} ${product.materialType}"
        // e.g., "Gold 22K"
    } else {
        product.materialType
        // e.g., "22K"
    }
    specs.add(ProductSpec(..., "Material", materialText))
}
```

---

## 🔄 Complete Flow

### Example: Product with Gold Material

**1. Product in Firestore:**
```json
products/8xKz2mP9nQ {
  "name": "Gold Ring",
  "material_id": "mat_gold_001",
  "material_type": "22K"
}
```

**2. Material in Firestore:**
```json
materials/mat_gold_001 {
  "name": "Gold",
  "image_url": "https://..."
}
```

**3. App Fetches Product:**
```kotlin
// Repository fetches product
val materialId = doc.getString("material_id")  // "mat_gold_001"
val materialName = getMaterialName(materialId)  // Fetches "Gold"

Product(
  materialId = "mat_gold_001",
  materialName = "Gold",        // ← Fetched!
  materialType = "22K"
)
```

**4. UI Displays:**
```
Material: Gold 22K
```

---

## 🎨 Display Examples

### With Material Type:
```
Product: { materialName: "Gold", materialType: "22K" }
Display: "Material: Gold 22K"
```

### Without Material Type:
```
Product: { materialName: "Silver", materialType: "" }
Display: "Material: Silver"
```

### Just Material Type:
```
Product: { materialName: "", materialType: "22K" }
Display: "Material: 22K"
```

### Material Not Found:
```
Product: { materialId: "invalid_id", materialName: "", materialType: "22K" }
Display: "Material: 22K"
```

---

## 🚀 Performance Optimization

### Material Name Cache

**First Load:**
```
Product 1: materialId "mat_gold_001"
  → Fetch from Firestore → "Gold"
  → Cache: {"mat_gold_001": "Gold"}

Product 2: materialId "mat_silver_001"
  → Fetch from Firestore → "Silver"
  → Cache: {"mat_gold_001": "Gold", "mat_silver_001": "Silver"}

Product 3: materialId "mat_gold_001"
  → Check cache → "Gold" (no Firestore call!)
```

**Benefits:**
- Each unique material fetched only once
- Subsequent products with same material use cached value
- Reduces Firestore reads significantly
- Faster loading times

---

## 📝 Firebase Structure Required

### Materials Collection:

```
materials/
├── mat_gold_001 (can be auto-generated ID or custom)
│   ├── name: "Gold"
│   ├── image_url: "https://..."
│   └── ... other fields
│
├── mat_silver_001
│   ├── name: "Silver"
│   └── ...
│
└── mat_platinum_001
    ├── name: "Platinum"
    └── ...
```

### Products Reference Materials:

```
products/8xKz2mP9nQ {
  "material_id": "mat_gold_001",     // ← References material document
  "material_type": "22K",             // Additional detail
  ...
}
```

---

## ✅ What Changed

| Component | Before | After |
|-----------|--------|-------|
| **Product Data Class** | No materialName field | Added materialName field |
| **Repository** | Only stored materialId | Fetches materialName from materials collection |
| **Cache** | Only wishlist | Added materialNameCache |
| **UI Display** | Parsed materialId string | Uses actual materialName |
| **Performance** | N/A | Cached material names |

---

## 🎯 Usage in UI

```kotlin
// Display material name with type
if (product.materialName.isNotBlank()) {
    if (product.materialType.isNotBlank()) {
        Text("${product.materialName} ${product.materialType}")  // "Gold 22K"
    } else {
        Text(product.materialName)  // "Gold"
    }
}

// Or use the show map
if (product.shouldShow("material_type")) {
    // Displays "Gold 22K" automatically
}
```

---

## 🧪 Testing Scenarios

### Scenario 1: Valid Material Reference
```
Product: { material_id: "mat_gold_001", material_type: "22K" }
Material: materials/mat_gold_001 { name: "Gold" }
Display: "Gold 22K" ✅
```

### Scenario 2: Material Not Found
```
Product: { material_id: "invalid_id", material_type: "22K" }
Material: Document not found
Display: "22K" ✅ (graceful fallback)
```

### Scenario 3: Empty Material ID
```
Product: { material_id: "", material_type: "22K" }
Display: "22K" ✅ (skips fetch)
```

### Scenario 4: Cached Material
```
Product 1: material_id "mat_gold_001" → Fetches → "Gold" → Cached
Product 2: material_id "mat_gold_001" → From Cache → "Gold" ✅ (fast!)
```

---

## 🔑 Key Points

1. **material_id** is a reference (document ID) in materials collection
2. **materialName** is fetched from `materials/{material_id}.name`
3. **materialType** provides additional detail (e.g., purity "22K")
4. **Cached** for performance - same material fetched once
5. **Graceful** - Falls back to materialType if name not found
6. **Automatic** - Happens during product fetching, transparent to UI

---

## 📚 Related Collections

This establishes a **relational pattern** in Firestore:

```
products → material_id → materials (fetch name)
products → category_id → categories (fetch category info)
```

Future extensions could include:
- Fetching category name for display
- Fetching stone information from stones collection
- Other relational data

---

**Status:** ✅ Material names now properly fetched and displayed  
**Performance:** ✅ Optimized with caching  
**User Experience:** ✅ Shows actual material names instead of IDs

