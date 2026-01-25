package com.example.shoptracklite.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Settings(
    @PrimaryKey
    val id: Int = 1,
    val wholesaleModeEnabled: Boolean = false,
    val currencyCode: String = "USD" // Default to USD
)

