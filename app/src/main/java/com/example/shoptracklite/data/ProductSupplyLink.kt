package com.example.shoptracklite.data

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "product_supply_links",
    primaryKeys = ["productId", "supplyId"]
)
data class ProductSupplyLink(
    @ColumnInfo(name = "productId")
    val productId: Long,
    @ColumnInfo(name = "quantityConsumed")
    val quantityConsumed: Double,  // Amount of supply consumed per product sale
    @ColumnInfo(name = "supplyId")
    val supplyId: Long
)
