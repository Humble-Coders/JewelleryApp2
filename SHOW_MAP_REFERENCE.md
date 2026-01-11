# Show Map Reference Guide

**Quick reference for using the `show` map in products**

---

## 🗺️ What is the Show Map?

The `show` map is a field in each product document that controls which fields are displayed in the UI.

```json
{
  "name": "Gold Ring",
  "price": 4583.33,
  "material_type": "22K",
  "show": {
    "name": true,           // ✅ Display name
    "price": true,          // ✅ Display price
    "material_type": true,  // ✅ Display material
    "description": false    // ❌ Hide description
  }
}
```

---

## 💻 How to Use in Code

### Basic Usage:

```kotlin
if (product.shouldShow("field_name")) {
    // Display the field
    Text("Field: ${product.fieldValue}")
}
```

### With Value Check:

```kotlin
if (product.shouldShow("net_weight") && product.netWeight > 0) {
    Text("Weight: ${product.netWeight}g")
}
```

### Nested Conditions:

```kotlin
if (product.shouldShow("has_stones") && product.hasStones) {
    if (product.shouldShow("stone_name")) {
        Text("Stone: ${product.stoneName}")
    }
}
```

---

## 📋 All Controllable Fields

| Field Name | Data Type | Example Display |
|------------|-----------|-----------------|
| `name` | String | "Gold Ring" |
| `description` | String | "Beautiful gold ring..." |
| `price` | Number | "₹4,583.33" |
| `quantity` | Number | "In Stock (5)" |
| `images` | Array | Image gallery |
| `category_id` | String | Usually internal |
| `material_type` | String | "22K" or "925" |
| `net_weight` | Number | "27.0g" |
| `total_weight` | Number | "50.0g" |
| `less_weight` | Number | "20.0g" |
| `cw_weight` | Number | "3.0 ct" |
| `default_making_rate` | Number | "₹4,000.00" |
| `va_charges` | Number | "₹500.00" |
| `total_product_cost` | Number | "₹10,000.00" |
| `has_stones` | Boolean | Controls stone fields |
| `stone_name` | String | "Diamond" |
| `stone_color` | String | "White" |
| `stone_rate` | Number | "₹5,000.00" |
| `is_other_than_gold` | Boolean | "Mixed Materials: Yes" |
| `material_id` | String | Usually internal |
| `available` | Boolean | Usually internal |
| `featured` | Boolean | Usually internal |

---

## 🎨 Default Behavior

### If field is NOT in show map:
```kotlin
product.shouldShow("some_field")
// Returns: true (shows by default)
```

### If show map is missing entirely:
```kotlin
product.shouldShow("any_field")
// Returns: true (shows all fields)
```

### If field value is empty:
Even if `show["field"] = true`, the UI won't display empty values.

---

## 📊 Common Show Map Configurations

### Minimal Product (Simple Ring):
```json
{
  "show": {
    "name": true,
    "price": true,
    "material_type": true,
    "quantity": true,
    "images": true
  }
}
```
**Displays:** Name, Price, Material, Stock, Images

---

### Full Product (Premium Jewelry):
```json
{
  "show": {
    "name": true,
    "price": true,
    "quantity": true,
    "material_type": true,
    "net_weight": true,
    "has_stones": true,
    "stone_name": true,
    "stone_color": true,
    "cw_weight": true,
    "default_making_rate": true,
    "va_charges": true,
    "total_product_cost": true,
    "is_other_than_gold": true,
    "images": true
  }
}
```
**Displays:** All major fields

---

### Internal/Admin Product:
```json
{
  "show": {
    "name": true,
    "price": true,
    "material_id": true,
    "category_id": true,
    "available": true,
    "featured": true,
    "total_weight": true,
    "less_weight": true,
    "stone_rate": true
  }
}
```
**Displays:** Administrative fields

---

## 🎯 Best Practices

### 1. Always Check Before Displaying:
```kotlin
// ✅ GOOD
if (product.shouldShow("quantity")) {
    Text("Stock: ${product.quantity}")
}

// ❌ BAD - ignores show map
Text("Stock: ${product.quantity}")
```

### 2. Combine with Value Checks:
```kotlin
// ✅ GOOD - checks both visibility AND value
if (product.shouldShow("stone_name") && product.stoneName.isNotBlank()) {
    Text("Stone: ${product.stoneName}")
}

// ❌ BAD - might show "Stone: " with empty value
if (product.shouldShow("stone_name")) {
    Text("Stone: ${product.stoneName}")
}
```

### 3. Use Helper Methods:
```kotlin
// ✅ GOOD - uses helper
Text(product.getFormattedPrice())

// ❌ BAD - manual formatting
Text("₹${product.price}")
```

---

## 🔧 Updating Show Map in Firebase

### For a Single Product:
```javascript
db.collection('products').doc('8xKz2mP9nQ').update({
  show: {
    name: true,
    price: true,
    material_type: true,
    // ... other fields
  }
});
```

### For All Products (Bulk):
```javascript
const products = await db.collection('products').get();

for (const doc of products.docs) {
  await doc.ref.update({
    show: {
      name: true,
      price: true,
      quantity: true,
      material_type: true,
      net_weight: true,
      images: true,
      // Enable fields you want to show
      description: false,
      material_id: false,
      // Hide fields you don't want to show
    }
  });
}
```

---

## 🎨 UI Examples

### Product Card (List View):
```kotlin
@Composable
fun ProductCard(product: Product) {
    Card {
        Column {
            // Always show
            AsyncImage(url = product.images.firstOrNull() ?: "")
            
            // Controlled by show map
            if (product.shouldShow("name")) {
                Text(product.name)
            }
            
            if (product.shouldShow("price")) {
                Text(product.getFormattedPrice())
            }
            
            if (product.shouldShow("quantity")) {
                if (product.isInStock()) {
                    Text("In Stock", color = Green)
                } else {
                    Text("Out of Stock", color = Red)
                }
            }
        }
    }
}
```

### Product Detail Page:
```kotlin
@Composable
fun ProductDetails(product: Product) {
    Column {
        // Build specs based on show map
        val specs = createProductSpecs(product)
        
        // Display specs grid
        SpecsGrid(specs)
        
        // Conditional sections
        if (product.shouldShow("description")) {
            DescriptionSection(product.description)
        }
        
        if (product.shouldShow("has_stones") && product.hasStones) {
            StoneDetailsSection(product)
        }
    }
}
```

---

## 🚀 Quick Reference

### Most Common Fields to Show:
✅ `name`, `price`, `quantity`, `images`  
✅ `material_type`, `net_weight`  
✅ `has_stones`, `stone_name`, `stone_color`  
✅ `default_making_rate`, `va_charges`  

### Usually Hidden:
❌ `material_id`, `category_id`  
❌ `description` (unless product-specific)  
❌ `total_weight`, `less_weight`  
❌ `stone_rate`  
❌ `available`, `featured`  

---

**Last Updated:** October 11, 2025  
**For:** JewelleryApp v2.0

