package com.example.strogholodapp

data class Product(
    val id: Int,
    val name: String,
    val price: String,
    val oldPrice: String? = null,    // ← новый параметр с умолчанием
    val description: String?,
    val priceUpdatedAt: String,
    val photo: String,
    val category: String
)
