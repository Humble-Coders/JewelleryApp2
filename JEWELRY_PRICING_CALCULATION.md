# Jewelry Pricing Calculation System

**Date:** October 11, 2025  
**Status:** ✅ COMPLETE - Dynamic pricing based on 24K rates implemented

---

## 🎯 Overview

Products can now have **custom prices** OR **auto-calculated prices** based on:
- Material type (Gold/Silver)
- Purity (24K, 22K, 18K)
- Weights (net weight, stone weight)
- Making charges
- Stone charges
- VA charges
- Discounts
- GST

---

## 📊 Pricing Logic

### Price Determination:

```kotlin
if (show["custom_price"] == true) {
    // Use custom_price field from Firebase
    price = product.custom_price
} else {
    // Calculate based on formula
    price = calculateJewelryPrice(product)
}
```

---

## 🧮 Calculation Formula

### Step 1: Net Weight
```
NT_WT = GS_WT - LESS_WT
(Net Weight = Gross Weight - Less Weight)
```

### Step 2: Base Material Amount
```
// Get 24K rate for the material
GOLD_RATE_24K = fetchFrom rates collection

// Adjust for purity
Purity Factor = material_type / 24
  - 24K → 24/24 = 1.000 (100% pure)
  - 22K → 22/24 = 0.9167 (91.67% pure)
  - 18K → 18/24 = 0.7500 (75% pure)

Effective Rate = GOLD_RATE_24K × Purity Factor
MATERIAL_AMOUNT = NT_WT × Effective Rate × QTY
```

### Step 3: Making Charges
```
MAKING_CHARGES = NT_WT × MAKING_RATE × QTY
```

### Step 4: Stone Charges
```
STONE_CHARGES = CW_WT × STONE_RATE × QTY
(If has_stones = false, this = 0)
```

### Step 5: Total Before Discount
```
TOTAL = MATERIAL_AMOUNT + MAKING_CHARGES + STONE_CHARGES + VA_CHARGES
```

### Step 6: Apply Discount
```
DISCOUNT_AMOUNT = TOTAL × (discount_percent / 100)
AMOUNT_AFTER_DISCOUNT = TOTAL - DISCOUNT_AMOUNT
```

### Step 7: Calculate GST

**For Intrastate Sales:**
```
CGST = AMOUNT_AFTER_DISCOUNT × (gst_rate / 2 / 100)
SGST = AMOUNT_AFTER_DISCOUNT × (gst_rate / 2 / 100)
TOTAL_TAX = CGST + SGST
```

**For Interstate Sales:**
```
IGST = AMOUNT_AFTER_DISCOUNT × (gst_rate / 100)
TOTAL_TAX = IGST
```

### Step 8: Final Price
```
FINAL_PRICE = AMOUNT_AFTER_DISCOUNT + TOTAL_TAX
(Rounded to 2 decimal places)
```

---

## 📋 Product Fields Used

### Existing Fields:
- `net_weight` - NT_WT (net weight in grams)
- `total_weight` - GS_WT (gross weight in grams)
- `less_weight` - LESS_WT (non-gold weight)
- `cw_weight` - CW_WT (stone carat weight)
- `default_making_rate` - Making charge per gram
- `stone_rate` - Stone charge per carat
- `va_charges` - Value addition charges
- `material_id` - Reference to materials collection
- `material_type` - Purity (e.g., "22K", "18K")
- `quantity` - QTY

### New Fields Added:
- `custom_price` - Override/custom price value
- `discount_percent` - Discount percentage (0-100)
- `gst_rate` - GST percentage (default 3%)
- `sale_type` - "intrastate" or "interstate"

### Show Map Control:
- `show["custom_price"]` - true = use custom_price, false = calculate

---

## 💻 Implementation Details

### Price Calculation Function:

```kotlin
private suspend fun calculateJewelryPrice(product: DocumentSnapshot): Double {
    // 1. Get material name and type
    val materialName = getMaterialName(product.getString("material_id"))  // "gold" or "silver"
    val materialType = product.getString("material_type")  // "22K", "18K", etc.
    val purity = materialType.replace("K", "").toInt()  // 22, 18, etc.
    
    // 2. Get 24K rate for the material
    val rate24K = getMaterial24KRate(materialName)  // e.g., ₹6500/g for gold
    
    // 3. Calculate purity factor
    val purityFactor = purity / 24.0  // e.g., 22/24 = 0.9167
    
    // 4. Calculate effective rate
    val effectiveRate = rate24K × purityFactor  // e.g., 6500 × 0.9167 = ₹5958.33/g
    
    // 5. Apply formula (steps 2-8)
    // Returns final calculated price
}
```

### Material Rate Caching:

```kotlin
// Cached to avoid repeated Firestore queries
materialRatesCache = {
    "gold": 6500.0,    // 24K Gold rate
    "silver": 3000.0   // 24K Silver rate
}
```

---

## 🔄 Example Calculation

### Product: 22K Gold Ring with Diamond

**Firebase Data:**
```json
{
  "name": "Gold Diamond Ring",
  "material_id": "mat_gold_001",
  "material_type": "22K",
  "net_weight": 10.0,
  "total_weight": 12.0,
  "less_weight": 2.0,
  "cw_weight": 0.5,
  "default_making_rate": 500,
  "stone_rate": 5000,
  "va_charges": 1000,
  "discount_percent": 5,
  "gst_rate": 3,
  "sale_type": "intrastate",
  "quantity": 1,
  "show": {
    "custom_price": false
  }
}
```

**Calculation:**

```
Material: Gold (24K rate = ₹6500/g)
Purity: 22K → 22/24 = 0.9167

1. Net Weight: 10.0g (verified: 12.0 - 2.0 = 10.0 ✅)

2. Material Amount:
   Effective Rate = 6500 × 0.9167 = ₹5,958.33/g
   Material Amount = 10.0 × 5,958.33 × 1 = ₹59,583.33

3. Making Charges:
   = 10.0 × 500 × 1 = ₹5,000.00

4. Stone Charges:
   = 0.5 × 5,000 × 1 = ₹2,500.00

5. Total Before Discount:
   = 59,583.33 + 5,000 + 2,500 + 1,000 = ₹68,083.33

6. Discount (5%):
   Discount = 68,083.33 × 0.05 = ₹3,404.17
   After Discount = 68,083.33 - 3,404.17 = ₹64,679.16

7. GST (3% Intrastate):
   CGST = 64,679.16 × 0.015 = ₹970.19
   SGST = 64,679.16 × 0.015 = ₹970.19
   Total Tax = ₹1,940.38

8. Final Price:
   = 64,679.16 + 1,940.38 = ₹66,619.54
```

**Result:** Product displays at **₹66,619.54**

---

## 🎨 UI Display Logic

### If `show["custom_price"] = true`:
```
Firebase: custom_price = 50000
Display: ₹50,000.00
(No calculation, just shows custom_price)
```

### If `show["custom_price"] = false`:
```
Calculate based on:
  - Material 24K rate
  - Purity conversion
  - All charges and weights
  - Discounts and taxes
Display: ₹66,619.54 (calculated)
```

---

## 📝 Firebase Schema Required

### Product Document with Custom Price:
```json
{
  "name": "Gold Ring",
  "custom_price": 50000,
  "show": {
    "custom_price": true
  }
}
```
**Result:** Shows ₹50,000.00

### Product Document with Calculated Price:
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
    "custom_price": false
  }
}
```
**Result:** Calculates and shows ₹66,619.54

---

## ⚡ Performance Optimization

### Three-Level Caching:

```kotlin
1. materialNameCache:
   "mat_gold_001" → "Gold"
   (Fetched once per material ID)

2. materialRatesCache:
   "gold" → 6500.0  (24K rate)
   "silver" → 3000.0  (24K rate)
   (Fetched once per material name)

3. wishlistCache:
   "product_id" → true/false
   (Updated in background)
```

**Benefits:**
- Material names fetched once
- 24K rates fetched once per material type
- Subsequent products use cached values
- Minimal Firestore reads

---

## 🔑 Purity Conversion Table

| Purity | Factor | Example Calculation |
|--------|--------|---------------------|
| 24K | 1.000 | 6500 × 1.000 = ₹6,500/g |
| 22K | 0.9167 | 6500 × 0.9167 = ₹5,958.33/g |
| 18K | 0.7500 | 6500 × 0.7500 = ₹4,875/g |
| 14K | 0.5833 | 6500 × 0.5833 = ₹3,791.67/g |

**Formula:** `Effective Rate = 24K Rate × (Purity / 24)`

---

## 🚨 Validation Rules

The calculation enforces these rules:

| Rule | Validation |
|------|------------|
| Net weight positive | `net_weight > 0` |
| Gross ≥ Less weight | `total_weight >= less_weight` |
| Quantity valid | `quantity >= 1` |
| Material rate exists | `24K rate > 0` |
| Discount range | `0 ≤ discount_percent ≤ 100` |

**If validation fails:** Returns price = 0.0

---

## 🔍 Debugging Logs

### Successful Calculation:
```
D/JewelryRepository: Cached 24K rate for gold: ₹6500/g
D/JewelryRepository: Price calculation for productId: Material=gold(22K), NetWt=10.0, Rate24K=6500.0, Final=₹66619.54
D/JewelryRepository: Product productId: Material=Gold, UseCustom=false, Price=₹66619.54
```

### Using Custom Price:
```
D/JewelryRepository: Product productId: Material=Gold, UseCustom=true, Price=₹50000.0
```

### Calculation Failed:
```
W/JewelryRepository: Invalid weights for product productId
W/JewelryRepository: No 24K rate found for material: unknown_material
```

---

## 📱 User Experience

### Product Lists:
- Shows calculated/custom price
- Formatted: ₹66,619.54
- Updates when rates change

### Product Details:
- Shows final price (custom or calculated)
- No discount badges (removed)
- Clean price display

### Admin Control:
- Toggle `show["custom_price"]` per product
- Set custom prices when needed
- Auto-calculate for standard products

---

## 🎯 Use Cases

### Use Case 1: Standard Product (Auto-Calculate)
```json
{
  "show": { "custom_price": false },
  "net_weight": 10.0,
  "material_type": "22K"
}
```
→ App calculates price based on current 24K rates

### Use Case 2: Special Offer (Custom Price)
```json
{
  "show": { "custom_price": true },
  "custom_price": 45000
}
```
→ App shows ₹45,000 (fixed price, no calculation)

### Use Case 3: Antique/Collectible (Custom Price)
```json
{
  "show": { "custom_price": true },
  "custom_price": 250000
}
```
→ App shows ₹2,50,000 (value-based pricing)

---

## ✅ Implementation Summary

### Code Changes:

1. **Product Data Class:**
   - Added `customPrice` field
   - Added `discountPercent`, `gstRate`, `saleType`

2. **Repository:**
   - Added `calculateJewelryPrice()` function
   - Added `getMaterial24KRate()` with caching
   - Added `materialRatesCache`
   - Updated all product fetching methods

3. **UI:**
   - Removed discount display
   - Shows calculated or custom price
   - Clean, simple price presentation

---

## 🔧 Firebase Data Requirements

### Products Collection:

**For Auto-Calculated Pricing:**
```json
{
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
    "custom_price": false
  }
}
```

**For Custom Pricing:**
```json
{
  "custom_price": 50000,
  "show": {
    "custom_price": true
  }
}
```

### Rates Collection (Required for Calculation):
```json
// Gold 24K
{
  "material_name": "gold",
  "material_type": "24K",
  "price_per_gram": 6500
}

// Silver 24K
{
  "material_name": "silver",
  "material_type": "24K",
  "price_per_gram": 3000
}
```

---

## 🎯 Key Benefits

1. **Flexibility** - Custom prices for special items, calculated for regular items
2. **Accuracy** - Uses real-time 24K rates, adjusts for purity
3. **Comprehensive** - Includes all charges, discounts, taxes
4. **Cached** - Fast performance with minimal Firestore reads
5. **Transparent** - Logs show complete calculation breakdown

---

## 📊 Complete Example

### Scenario: 22K Gold Mangalsutra

**Inputs:**
- Net Weight: 27.0g
- Material: Gold 22K
- Making Rate: ₹400/g
- Stone: Diamond, 2ct
- Stone Rate: ₹8,000/ct
- VA Charges: ₹2,000
- Discount: 0%
- GST: 3%
- Sale Type: Intrastate

**24K Gold Rate:** ₹6,500/g

**Calculation:**
```
1. Purity Factor: 22/24 = 0.9167
2. Effective Gold Rate: 6,500 × 0.9167 = ₹5,958.33/g
3. Gold Amount: 27.0 × 5,958.33 × 1 = ₹160,875.00
4. Making: 27.0 × 400 × 1 = ₹10,800.00
5. Stones: 2.0 × 8,000 × 1 = ₹16,000.00
6. VA: ₹2,000.00
7. Total: ₹189,675.00
8. Discount (0%): ₹0.00
9. After Discount: ₹189,675.00
10. CGST (1.5%): ₹2,845.13
11. SGST (1.5%): ₹2,845.13
12. Tax Total: ₹5,690.25
13. FINAL PRICE: ₹195,365.25
```

**Product displays at:** ₹1,95,365.25

---

## ✅ Testing Checklist

- [ ] Product with custom_price=true shows custom_price value
- [ ] Product with custom_price=false calculates price
- [ ] 24K gold products calculate correctly (purity factor = 1.0)
- [ ] 22K gold products calculate correctly (purity factor = 0.9167)
- [ ] 18K gold products calculate correctly (purity factor = 0.75)
- [ ] Silver products calculate with silver 24K rate
- [ ] Stone charges included when has_stones=true
- [ ] Discount applied correctly
- [ ] Intrastate GST splits into CGST+SGST
- [ ] Interstate GST uses IGST
- [ ] Price updates when 24K rates change
- [ ] Caching works (same material = 1 rate fetch)

---

**Status:** ✅ Complete - Comprehensive jewelry pricing system implemented  
**Next:** Test with real products and verify calculations

