package com.example.shoptracklite.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceRangeDao {
    @Query("SELECT * FROM price_ranges WHERE productId = :productId ORDER BY minQuantity ASC")
    fun getPriceRangesByProduct(productId: Long): Flow<List<PriceRange>>
    
    @Query("SELECT * FROM price_ranges WHERE productId = :productId ORDER BY minQuantity ASC")
    suspend fun getPriceRangesByProductSync(productId: Long): List<PriceRange>
    
    @Insert
    suspend fun insertPriceRange(priceRange: PriceRange): Long
    
    @Insert
    suspend fun insertPriceRanges(priceRanges: List<PriceRange>)
    
    @Update
    suspend fun updatePriceRange(priceRange: PriceRange)
    
    @Delete
    suspend fun deletePriceRange(priceRange: PriceRange)
    
    @Query("DELETE FROM price_ranges WHERE productId = :productId")
    suspend fun deletePriceRangesByProduct(productId: Long)
    
    @Query("SELECT price FROM price_ranges WHERE productId = :productId AND :quantity >= minQuantity AND :quantity <= maxQuantity LIMIT 1")
    suspend fun getPriceForQuantity(productId: Long, quantity: Int): Double?
}
