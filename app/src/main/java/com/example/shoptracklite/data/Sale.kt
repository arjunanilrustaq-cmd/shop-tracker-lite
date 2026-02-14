package com.example.shoptracklite.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

enum class PaymentMethod {
    CASH, VISA
}

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val quantitySold: Int,
    val unitPrice: Double,
    val totalAmount: Double,
    val costPrice: Double,
    val profit: Double,
    val paymentMethod: PaymentMethod,
    val saleDate: Date = Date(),
    val isWholesale: Boolean = false,
    val isCancelled: Boolean = false,
    val transactionId: Long? = null
)
