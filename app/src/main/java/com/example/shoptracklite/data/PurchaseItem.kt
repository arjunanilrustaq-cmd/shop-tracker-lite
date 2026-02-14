package com.example.shoptracklite.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PurchaseItemType {
    PRODUCT,
    SUPPLY
}

@Entity(tableName = "purchase_items")
data class PurchaseItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billId: Long,
    val itemType: PurchaseItemType,
    val itemId: Long? = null,  // productId or supplyId (null if new item created)
    val itemName: String,
    val quantity: Double,
    val unitCost: Double,
    val totalCost: Double
)
