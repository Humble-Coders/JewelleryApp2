# Price Calculation Debugging Guide

**Issue:** Price showing as ₹0.00 when custom_price is false  
**Status:** 🔍 Enhanced logging added

---

## 🔍 Comprehensive Logging Added

### Now you'll see EVERY step of the calculation in logs:

```
D/JewelryRepository: === Starting price calculation for {productId} ===
D/JewelryRepository: Material ID: mat_gold_001
D/JewelryRepository: Material Name: Gold
D/JewelryRepository: Material Type: 22K
D/JewelryRepository: Purity: 22 K
D/JewelryRepository: Weights - Gross: 12.0, Less: 2.0, Net: 10.0, Stone: 0.5
D/JewelryRepository: Charges - Making: 500, Stone: 5000, VA: 1000, Discount: 5%, GST: 3%, SaleType: intrastate, Qty: 1
D/JewelryRepository: Getting 24K rate for material: gold
D/JewelryRepository: Fetching from Firestore: rates where material_name='gold' AND material_type='24K'
D/JewelryRepository: Query returned 1 documents for gold
D/JewelryRepository: Found rate document: abc123, price_per_gram=6500.0
D/JewelryRepository: ✅ Cached 24K rate for gold: ₹6500.0/g
D/JewelryRepository: 24K Rate for Gold: ₹6500.0/g
D/JewelryRepository: Purity Factor: 0.9167 (for 22 K)
D/JewelryRepository: Step 2 - Effective Rate: ₹5958.33/g, Material Amount: ₹59583.33
D/JewelryRepository: Step 3 - Making Charges: ₹5000.0
D/JewelryRepository: Step 4 - Stone Charges: ₹2500.0
D/JewelryRepository: Step 5 - Total Before Discount: ₹68083.33
D/JewelryRepository: Step 6 - Discount: ₹3404.17, After Discount: ₹64679.16
D/JewelryRepository: Step 7 - Intrastate: CGST=₹970.19, SGST=₹970.19
D/JewelryRepository: Step 8 - Total Tax: ₹1940.38, FINAL AMOUNT: ₹66619.54
D/JewelryRepository: === Price calculation complete: ₹66619.54 ===
```

---

## 🐛 Common Issues & What Logs Will Show

### Issue 1: Material ID Not Set
**Logs:**
```
D/JewelryRepository: Material ID: 
E/JewelryRepository: Material ID is blank, cannot calculate price
```
**Fix:** Add `material_id` field to product in Firebase

---

### Issue 2: Material Name Not Found
**Logs:**
```
D/JewelryRepository: Material ID: mat_gold_001
D/JewelryRepository: Material Name: 
E/JewelryRepository: Material name is blank for ID: mat_gold_001
```
**Fix:** Ensure `materials/mat_gold_001` exists with `name` field

---

### Issue 3: Net Weight is Zero
**Logs:**
```
D/JewelryRepository: Weights - Gross: 0.0, Less: 0.0, Net: 0.0, Stone: 0.0
E/JewelryRepository: Net weight is 0 or negative: 0.0
```
**Fix:** Add `net_weight` field to product in Firebase

---

### Issue 4: No 24K Rate Found
**Logs:**
```
D/JewelryRepository: Getting 24K rate for material: gold
D/JewelryRepository: Fetching from Firestore: rates where material_name='gold' AND material_type='24K'
D/JewelryRepository: Query returned 0 documents for gold
W/JewelryRepository: No 24K rate document found for material: gold
E/JewelryRepository: No 24K rate found for material: gold
```
**Fix:** Add Gold 24K rate to `rates` collection:
```json
{
  "material_name": "gold",
  "material_type": "24K",
  "price_per_gram": 6500
}
```

---

### Issue 5: Wrong Material Name in Rates
**Logs:**
```
D/JewelryRepository: Material Name: Gold
D/JewelryRepository: Getting 24K rate for material: gold
D/JewelryRepository: Query returned 0 documents for gold
```
**Check:** Ensure `rates` collection has `material_name: "gold"` (lowercase)

---

## 🔍 Step-by-Step Debugging

### Run the app and check these specific logs:

#### 1. Check if calculation starts:
```
✅ Look for: "=== Starting price calculation for..."
❌ If missing: Calculation not being triggered
```

#### 2. Check material ID:
```
✅ Look for: "Material ID: mat_gold_001" (non-empty)
❌ If blank: Product missing material_id field
```

#### 3. Check material name:
```
✅ Look for: "Material Name: Gold" (non-empty)
❌ If blank: Material document not found in materials collection
```

#### 4. Check net weight:
```
✅ Look for: "Net: 10.0" (> 0)
❌ If 0: Product missing net_weight field
```

#### 5. Check 24K rate fetch:
```
✅ Look for: "Query returned 1 documents for gold"
❌ If 0: No rate document in rates collection
```

#### 6. Check final calculation:
```
✅ Look for: "=== Price calculation complete: ₹66619.54 ==="
❌ If 0: Check previous steps for errors
```

---

## 📋 Required Firebase Data Checklist

### Product Document Must Have:

```json
{
  "material_id": "mat_gold_001",  // ✅ Non-empty
  "material_type": "22K",         // ✅ Valid (24K, 22K, 18K)
  "net_weight": 10.0,             // ✅ > 0
  "show": {
    "custom_price": false         // ✅ Set to false for calculation
  }
}
```

### Materials Collection Must Have:

```json
materials/mat_gold_001 {
  "name": "Gold"  // ✅ Exact material name
}
```

### Rates Collection Must Have:

```json
rates/{auto-id} {
  "material_name": "gold",  // ✅ Lowercase, matching material name
  "material_type": "24K",   // ✅ Exactly "24K"
  "price_per_gram": 6500    // ✅ Number > 0
}
```

---

## 🎯 What to Check in Your Firebase

### 1. Check Product:
```javascript
// In Firebase Console, check your product document
db.collection('products').doc('YOUR_PRODUCT_ID').get()

// Verify these fields exist and have values:
✓ material_id: "mat_gold_001" or similar (not empty)
✓ net_weight: 10.0 or any number > 0
✓ material_type: "22K" or "18K" or "24K"
✓ show.custom_price: false
```

### 2. Check Material:
```javascript
// Check the material document exists
db.collection('materials').doc('mat_gold_001').get()

// Verify:
✓ name: "Gold" (must exist)
```

### 3. Check Rates:
```javascript
// Check 24K rate exists for the material
db.collection('rates')
  .where('material_name', '==', 'gold')  // lowercase
  .where('material_type', '==', '24K')
  .get()

// Verify:
✓ At least 1 document returned
✓ price_per_gram: 6500 (or any number > 0)
```

---

## 🚀 Quick Test

### Run the app and look for this sequence in logs:

```
✅ === Starting price calculation for productId ===
✅ Material ID: mat_gold_001
✅ Material Name: Gold
✅ Net: 10.0
✅ Getting 24K rate for material: gold
✅ Query returned 1 documents for gold
✅ Found rate document: abc123, price_per_gram=6500.0
✅ 24K Rate for Gold: ₹6500.0/g
✅ Step 2 - Material Amount: ₹59583.33
✅ === Price calculation complete: ₹66619.54 ===
```

**If any step shows 0, blank, or error → That's the problem!**

---

## 📝 Most Likely Issues

### Issue #1: Material Name Case Mismatch
**Problem:** Material name is "Gold" but rates has "Gold" instead of "gold"  
**Solution:** Rates collection must have `material_name: "gold"` (lowercase)

### Issue #2: Missing 24K Rate
**Problem:** No rate document for the material  
**Solution:** Add Gold and Silver 24K rates to `rates` collection

### Issue #3: Net Weight is 0
**Problem:** Product has no `net_weight` field  
**Solution:** Add `net_weight` field with value > 0

### Issue #4: Material ID is Blank
**Problem:** Product has no `material_id` field  
**Solution:** Add `material_id` referencing materials collection

---

## 🎯 Action Steps

1. **Run the app** and open a product with `custom_price: false`
2. **Check logs** (filter by "JewelryRepository")
3. **Find the step that shows 0 or error**
4. **Fix that specific data in Firebase**
5. **Test again**

The comprehensive logs will tell you **exactly** what's missing!

---

**Status:** 🔍 Ready to debug with detailed logs  
**Next:** Share the complete logs from "=== Starting price calculation ===" to "=== Price calculation complete ==="




