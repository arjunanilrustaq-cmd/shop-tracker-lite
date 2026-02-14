package com.example.shoptracklite.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cash_reconciliation")
data class CashReconciliation(
    @PrimaryKey
    val date: String, // Format: "yyyy-MM-dd"
    val openingCash: Double = 0.0, // Manual entry for first day, or from previous day
    val actualCashCounted: Double = 0.0,
    val changeForTomorrow: Double = 0.0,
    val notes: String = ""
)
