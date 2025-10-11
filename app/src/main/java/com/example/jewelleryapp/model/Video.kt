package com.example.jewelleryapp.model

data class Video(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val duration: Long = 0L, // Duration in milliseconds
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
