package com.example.shoptracklite.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "supplies")
data class Supply(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: Double,
    val unit: String,
    val costPerUnit: Double,
    val lowStockThreshold: Int = 10,
    val createdAt: Date = Date()
)
