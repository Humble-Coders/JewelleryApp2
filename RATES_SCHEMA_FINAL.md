# Rates Collection - Final Schema Documentation

**Date:** October 11, 2025  
**Status:** ✅ COMPLETE - Matches actual Firebase schema

---

## 📊 Exact Firebase Schema

### Collection: `rates`

Each document has **auto-generated ID** with these fields:

| Field Name | Data Type | Description | Example Value |
|------------|-----------|-------------|---------------|
| `created_at` | Number | Creation timestamp (milliseconds) | 1760180733989 |
| `is_active` | Boolean | Whether this rate is currently active | true |
| `karat` | Number | Purity in karats | 24 |
| `material_id` | String | Reference to materials collection | "RGn5bvoJpM3KXRI2RoSX" |
| `material_name` | String | Name of the material (lowercase) | "silver" |
| `material_type` | String | Type/purity designation | "24K" |
| `price_per_gram` | Number | Current price per gram | 3000 |
| `updated_at` | Number | Last update timestamp (milliseconds) | 1760180759633 |

---

## 🔍 Query Logic

### Filters Applied:

```kotlin
firestore.collection("rates")
    .whereEqualTo("material_type", "24K")   // Only 24K purity
    .whereEqualTo("is_active", true)        // Only active rates
    .get()
```

**Result:** Returns only **active 24K** rates for Gold and Silver

---

## 📝 Example Firebase Documents

### Gold 24K Rate:
```json
{
  "created_at": 1760180733989,
  "is_active": true,
  "karat": 24,
  "material_id": "RGn5bvoJpM3KXRI2RoSX",
  "material_name": "gold",
  "material_type": "24K",
  "price_per_gram": 6500,
  "updated_at": 1760180759633
}
```

### Silver 24K Rate:
```json
{
  "created_at": 1760180733989,
  "is_active": true,
  "karat": 24,
  "material_id": "abc123xyz456",
  "material_name": "silver",
  "material_type": "24K",
  "price_per_gram": 3000,
  "updated_at": 1760180759633
}
```

---

## 💻 Implementation Details

### Field Mapping:

```kotlin
// Material identification (case-insensitive)
val materialName = doc.getString("material_name") ?: ""

when (materialName.lowercase()) {
    "gold" -> {
        goldRate = parseDoubleField(doc, "price_per_gram")
        lastUpdated = parseLongField(doc, "updated_at")
    }
    "silver" -> {
        silverRate = parseDoubleField(doc, "price_per_gram")
        lastUpdated = parseLongField(doc, "updated_at")
    }
}
```

### Additional Fields Logged:
```kotlin
val karat = parseIntField(doc, "karat")
val isActive = doc.getBoolean("is_active") ?: false
```

---

## 🔄 Complete Data Flow

```
1. App queries rates collection
   ↓
2. Firestore filters: material_type = "24K" AND is_active = true
   ↓
3. Returns documents (should be 2: Gold and Silver)
   ↓
4. For each document:
   - Read material_name
   - Read price_per_gram
   - Read updated_at
   ↓
5. Build GoldSilverRates object:
   - goldRatePerGram: 6500
   - silverRatePerGram: 3000
   - lastUpdated: 1760180759633
   ↓
6. UI displays rates
```

---

## 🎯 Why These Filters?

### `material_type = "24K"`
- Shows only 24K (pure) metal rates
- 24K is the standard reference
- Other purities calculated from 24K base

### `is_active = true`
- Shows only current/active rates
- Inactive rates are hidden but preserved
- Easy to deactivate old rates without deleting

---

## 📋 Required Firebase Setup

### Minimum Setup (2 documents needed):

**Document 1 - Gold:**
```javascript
await db.collection('rates').add({
  created_at: Date.now(),
  is_active: true,
  karat: 24,
  material_id: "mat_gold_001",  // Reference to materials collection
  material_name: "gold",         // Lowercase
  material_type: "24K",
  price_per_gram: 6500,
  updated_at: Date.now()
});
```

**Document 2 - Silver:**
```javascript
await db.collection('rates').add({
  created_at: Date.now(),
  is_active: true,
  karat: 24,
  material_id: "mat_silver_001",  // Reference to materials collection
  material_name: "silver",         // Lowercase
  material_type: "24K",
  price_per_gram: 3000,
  updated_at: Date.now()
});
```

---

## 🔧 Updating Rates

### Update Gold Rate:
```javascript
const goldRateDoc = await db.collection('rates')
  .where('material_name', '==', 'gold')
  .where('material_type', '==', '24K')
  .where('is_active', '==', true)
  .get();

if (!goldRateDoc.empty) {
  await goldRateDoc.docs[0].ref.update({
    price_per_gram: 6600,  // New rate
    updated_at: Date.now()
  });
}
```

### Deactivate Old Rate:
```javascript
// Instead of deleting, just deactivate
await rateDocRef.update({
  is_active: false,
  updated_at: Date.now()
});
```

---

## 🎨 UI Display

### Rates Dialog Shows:

```
┌─────────────────────────────────┐
│  Today's Rates (24K Pure)        │
│                                  │
│  💛 Gold 24K                     │
│  ₹6,500.00 per gram             │
│                                  │
│  🪙 Silver 24K                   │
│  ₹3,000.00 per gram             │
│                                  │
│  Last Updated: 2 hours ago       │
└─────────────────────────────────┘
```

---

## ✅ Key Features

1. **Only Active Rates** - `is_active = true` filter
2. **Only 24K Purity** - `material_type = "24K"` filter
3. **Latest Timestamp** - Uses `updated_at` field
4. **Case-Insensitive** - Matches "gold", "Gold", "GOLD"
5. **Safe Parsing** - Works with String or Number types
6. **Material Reference** - Links to materials collection via `material_id`

---

## 🔍 Debugging

### Expected Logs:
```
D/JewelryRepository: Fetching gold and silver rates for 24K
D/JewelryRepository: Found rate: gold 24K (karat: 24, active: true)
D/JewelryRepository: Found rate: silver 24K (karat: 24, active: true)
D/JewelryRepository: Rates fetched - Gold: ₹6500/g, Silver: ₹3000/g (24K, active only)
```

### If No Rates Found:
```
W/JewelryRepository: No active 24K rates found in rates collection
```

**Action:**
- Check if documents exist in `rates` collection
- Verify `material_type` is exactly "24K"
- Verify `is_active` is `true`
- Verify `material_name` is "gold" or "silver" (case-insensitive)

---

## 📊 Complete Rate Document Fields

### All Fields Used:

```kotlin
// Read from Firebase:
created_at: Long         → For record keeping
is_active: Boolean       → Filter active rates
karat: Int               → Purity value (24)
material_id: String      → Reference to materials collection
material_name: String    → "gold" or "silver" (used for matching)
material_type: String    → "24K" (used for filtering)
price_per_gram: Double   → The actual rate
updated_at: Long         → Last update timestamp
currency: String         → "INR", "USD", etc. (optional)
```

---

## 🎯 Data Integrity

### Relationship with Materials Collection:

```
rates/abc123 {
  material_id: "RGn5bvoJpM3KXRI2RoSX",
  material_name: "silver"
}
    ↓ (references)
materials/RGn5bvoJpM3KXRI2RoSX {
  name: "Silver",
  image_url: "https://..."
}
```

**Note:** The `material_id` in rates should reference the same materials collection used by products.

---

## ✨ Benefits of This Structure

### 1. Flexibility
- Easy to add more purities (22K, 18K)
- Easy to add more metals (Platinum, Palladium)
- Can have multiple active rates

### 2. Version Control
- Keep historical rates (set `is_active = false`)
- Track when rates change (`created_at`, `updated_at`)
- No data loss when updating

### 3. Easy Management
- Activate/deactivate rates without deletion
- Query specific purities easily
- Clear field names and structure

### 4. Scalability
```
rates/
├── doc1 { material_name: "gold", material_type: "24K", is_active: true }
├── doc2 { material_name: "silver", material_type: "24K", is_active: true }
├── doc3 { material_name: "gold", material_type: "22K", is_active: true }  ← Future
├── doc4 { material_name: "platinum", material_type: "24K", is_active: true } ← Future
├── doc5 { material_name: "gold", material_type: "24K", is_active: false }  ← Old rate
└── ...
```

---

## 🚀 Production Checklist

- [ ] Create Gold 24K rate document with `is_active: true`
- [ ] Create Silver 24K rate document with `is_active: true`
- [ ] Both have `material_type: "24K"`
- [ ] Both have `price_per_gram` as Number
- [ ] Both have `updated_at` timestamp
- [ ] Test rates dialog displays correctly
- [ ] Verify timestamp shows correctly

---

## 📝 Quick Setup Script

```javascript
const db = admin.firestore();

// Add Gold 24K rate
await db.collection('rates').add({
  created_at: Date.now(),
  is_active: true,
  karat: 24,
  material_id: "mat_gold_001",  // Your material ID
  material_name: "gold",
  material_type: "24K",
  price_per_gram: 6500,
  updated_at: Date.now(),
  currency: "INR"
});

// Add Silver 24K rate
await db.collection('rates').add({
  created_at: Date.now(),
  is_active: true,
  karat: 24,
  material_id: "mat_silver_001",  // Your material ID
  material_name: "silver",
  material_type: "24K",
  price_per_gram: 3000,
  updated_at: Date.now(),
  currency: "INR"
});

console.log('✅ Rates setup complete!');
```

---

## ✅ Summary

Your app now:
- ✅ Fetches rates from `rates` collection
- ✅ Filters for **24K and active** only
- ✅ Uses exact field names from your schema
- ✅ Handles `price_per_gram`, `updated_at`, `is_active`
- ✅ Case-insensitive material name matching
- ✅ Safe type parsing (String/Number compatible)
- ✅ Uses latest `updated_at` timestamp

**Ready to display rates!** Just add the Gold and Silver 24K documents to Firebase. 🎉

---

**Last Updated:** October 11, 2025  
**Schema Version:** Final (matches actual Firebase)

