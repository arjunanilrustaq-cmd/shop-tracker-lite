package com.example.shoptracklite.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "price_ranges",
    indices = [Index(value = ["productId"])]
)
data class PriceRange(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "productId")
    val productId: Long,
    
    @ColumnInfo(name = "minQuantity")
    val minQuantity: Int,
    
    @ColumnInfo(name = "maxQuantity")
    val maxQuantity: Int,
    
    @ColumnInfo(name = "price")
    val price: Double
)
