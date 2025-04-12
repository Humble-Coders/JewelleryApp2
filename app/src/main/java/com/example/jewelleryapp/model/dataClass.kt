package com.example.jewelleryapp.model

// Model classes for Firebase data

data class Category(
    val id: String,
    val name: String,
    val imageUrl: String
)

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val currency: String = "Rs",
    val imageUrl: String,
    val isFavorite: Boolean = false,
    val material: String = "",
    val stone: String = "",
    val clarity: String = "",
    val cut: String = "",
    val categoryId: String = "",
    val materialId: String? = null,
    val materialType : String? = null,
    val description: String = "",
    val category: String = ""
)

data class Collection(
    val id: String,
    val name: String,
    val imageUrl: String,
    val description: String = ""
)

data class CarouselItem(
    val id: String,
    val imageUrl: String,
    val title: String,
    val subtitle: String,
    val buttonText: String
)