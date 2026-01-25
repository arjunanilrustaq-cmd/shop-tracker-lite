package com.example.shoptracklite.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT p.* FROM products p INNER JOIN favorites f ON p.id = f.productId ORDER BY f.displayOrder ASC")
    fun getFavoriteProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: Favorite)

    @Delete
    suspend fun removeFavorite(favorite: Favorite)

    @Query("SELECT COUNT(*) FROM favorites WHERE productId = :productId")
    suspend fun isFavorite(productId: Long): Int
    
    @Query("SELECT MAX(displayOrder) FROM favorites")
    suspend fun getMaxDisplayOrder(): Int?
    
    @Query("UPDATE favorites SET displayOrder = :newOrder WHERE productId = :productId")
    suspend fun updateDisplayOrder(productId: Long, newOrder: Int)
    
    @Query("SELECT productId FROM favorites ORDER BY displayOrder ASC")
    suspend fun getFavoriteProductIdsOrdered(): List<Long>
}
