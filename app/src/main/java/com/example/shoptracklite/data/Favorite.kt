package com.example.shoptracklite.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey
    val productId: Long,
    val displayOrder: Int = 0
)
