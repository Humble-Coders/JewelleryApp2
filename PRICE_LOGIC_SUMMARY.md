# Product Price Logic - Implementation Summary

**Date:** October 11, 2025  
**Status:** ✅ IMPLEMENTED - Exactly as specified

---

## 🎯 Price Display Logic

### Simple Rule:

```kotlin
if (show["custom_price"] == true) {
    // Use the custom_price field from Firebase
    displayPrice = product.custom_price
} else {
    // Calculate price using the comprehensive formula
    displayPrice = calculateJewelryPrice(product)
}
```

---

## 📊 Two Pricing Modes

### Mode 1: Custom Price (Fixed/Override Price)

**Firebase Configuration:**
```json
{
  "name": "Special Gold Ring",
  "custom_price": 50000,
  "show": {
    "custom_price": true  // ← KEY: Set to true
  }
}
```

**Result:**
- App uses: `custom_price` field
- Displays: **₹50,000.00**
- No calculation performed
- Price is fixed regardless of material rates

**Use Cases:**
- Special offers/promotions
- Antique/collectible items
- Custom-made pieces
- Clearance sales
- Items with sentimental value pricing

---

### Mode 2: Calculated Price (Dynamic/Auto)

**Firebase Configuration:**
```json
{
  "name": "Gold Ring",
  "material_id": "mat_gold_001",
  "material_type": "22K",
  "net_weight": 10.0,
  "total_weight": 12.0,
  "less_weight": 2.0,
  "default_making_rate": 500,
  "va_charges": 1000,
  "cw_weight": 0.5,
  "stone_rate": 5000,
  "discount_percent": 5,
  "gst_rate": 3,
  "sale_type": "intrastate",
  "quantity": 1,
  "show": {
    "custom_price": false  // ← KEY: Set to false
  }
}
```

**Result:**
- App calculates: Uses comprehensive formula
- Fetches: 24K material rate from `rates` collection
- Adjusts: For purity (22K = 91.67% of 24K)
- Includes: All charges, discounts, GST
- Displays: **₹66,619.54** (calculated)
- Updates: When 24K rates change

**Use Cases:**
- Standard jewelry items
- Regular inventory
- Material-based pricing
- Items following standard calculations

---

## 🔄 Complete Data Flow

### For Custom Price:

```
Product Document
    ↓
show["custom_price"] = true
    ↓
Read: custom_price = 50000
    ↓
Set: product.price = 50000
    ↓
Display: ₹50,000.00
```

**No calculation, no rate fetching, just displays the custom value.**

---

### For Calculated Price:

```
Product Document
    ↓
show["custom_price"] = false
    ↓
Get: material_id → "mat_gold_001"
    ↓
Fetch: materials/mat_gold_001 → name = "Gold"
    ↓
Fetch: rates where material_name="gold" AND material_type="24K"
    ↓
Get: price_per_gram = 6500
    ↓
Extract: material_type = "22K" → purity = 22
    ↓
Calculate: 
  - Purity Factor = 22/24 = 0.9167
  - Effective Rate = 6500 × 0.9167 = ₹5,958.33/g
  - Material Cost = net_weight × effective_rate
  - + Making Charges
  - + Stone Charges  
  - + VA Charges
  - - Discount
  - + GST
    ↓
Set: product.price = 66619.54
    ↓
Display: ₹66,619.54
```

**Full calculation with all components included.**

---

## 📝 Field Usage Summary

### Custom Price Mode (`show["custom_price"] = true`)

**Required Fields:**
- ✅ `custom_price` - The price to display
- ✅ `show.custom_price` - Set to true

**Optional Fields:**
- All calculation fields (ignored)

---

### Calculated Price Mode (`show["custom_price"] = false`)

**Required Fields:**
- ✅ `material_id` - References materials collection
- ✅ `material_type` - Purity ("22K", "18K", etc.)
- ✅ `net_weight` - Net material weight in grams
- ✅ `show.custom_price` - Set to false

**Important Fields:**
- ✅ `default_making_rate` - Making charges per gram
- ✅ `va_charges` - Value addition charges
- ✅ `total_weight` - Gross weight
- ✅ `less_weight` - Non-metal weight

**For Stone Jewelry:**
- ✅ `cw_weight` - Stone carat weight
- ✅ `stone_rate` - Stone price per carat

**For Pricing Details:**
- ✅ `discount_percent` - Discount %
- ✅ `gst_rate` - GST % (default 3%)
- ✅ `sale_type` - "intrastate" or "interstate"
- ✅ `quantity` - Quantity (default 1)

**External Dependency:**
- ✅ `rates` collection must have 24K rate for the material

---

## 🎨 UI Display Examples

### Product Card (List View):
```
┌──────────────────┐
│ [Image]          │
│ Gold Ring        │
│ ₹50,000.00       │ ← Custom price
└──────────────────┘

┌──────────────────┐
│ [Image]          │
│ Silver Bracelet  │
│ ₹66,619.54       │ ← Calculated price
└──────────────────┘
```

### Product Detail Page:
```
┌─────────────────────────┐
│  Gold Diamond Ring      │
│                         │
│  ₹66,619.54            │ ← Calculated
│  Price                  │
│                         │
│  Material: Gold 22K     │
│  Net Weight: 10.0g      │
│  Making: ₹5,000.00     │
└─────────────────────────┘
```

---

## 🔍 Debugging

### Custom Price Product:
```
D/JewelryRepository: Product abc123: UseCustom=true, Price=₹50000.0
```

### Calculated Price Product:
```
D/JewelryRepository: Cached 24K rate for gold: ₹6500/g
D/JewelryRepository: Price calculation: Material=gold(22K), NetWt=10.0, Rate24K=6500.0, Final=₹66619.54
D/JewelryRepository: Product xyz789: UseCustom=false, Price=₹66619.54
```

---

## ✅ Implementation Checklist

- [x] Added `customPrice` field to Product model
- [x] Added `discountPercent`, `gstRate`, `saleType` fields
- [x] Created `calculateJewelryPrice()` function
- [x] Created `getMaterial24KRate()` with caching
- [x] Added `materialRatesCache` for performance
- [x] Updated `fetchProductsByIds()` with price logic
- [x] Updated `getProductDetails()` with price logic
- [x] Updated `getProductsByCategory()` with price logic
- [x] Updated `getAllProductsPaginated()` with price logic
- [x] Removed discount display from UI
- [x] Updated price display formatting

---

## 🎯 Summary

**The system now works exactly as you specified:**

1. **Check `show["custom_price"]`**
   - If `true` → Use `custom_price` field
   - If `false` → Calculate using formula

2. **Calculation uses:**
   - 24K material rate from `rates` collection
   - Purity adjustment (22K = 91.67% of 24K)
   - All weights, charges, discounts, taxes

3. **Performance:**
   - Caches 24K rates
   - Caches material names
   - Fast subsequent loads

**Your jewelry pricing system is complete and production-ready!** 🎉

---

**Status:** ✅ COMPLETE  
**Logic:** Exactly as specified  
**Ready:** Yes, test with Firebase data

