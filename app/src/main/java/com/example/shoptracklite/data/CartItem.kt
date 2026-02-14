package com.example.shoptracklite.data

data class CartItem(
    val productId: Long,
    val productName: String,
    val unitPrice: Double,
    val wholesalePrice: Double?,
    val quantity: Int,
    val totalAmount: Double,
    val wholesaleTotalAmount: Double?,
    val imagePath: String? = null,
    val colorHex: String? = null
)
