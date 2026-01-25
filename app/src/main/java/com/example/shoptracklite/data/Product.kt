package com.example.shoptracklite.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val wholesalePrice: Double? = null,
    val quantityInStock: Int,
    val hasQuantityBasedPricing: Boolean = false,
    val barcode: String? = null,
    val createdAt: Date = Date()
)
