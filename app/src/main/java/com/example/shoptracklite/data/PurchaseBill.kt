package com.example.shoptracklite.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "purchase_bills")
data class PurchaseBill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Date = Date(),
    val supplierName: String? = null,
    val totalAmount: Double,
    val notes: String? = null
)
