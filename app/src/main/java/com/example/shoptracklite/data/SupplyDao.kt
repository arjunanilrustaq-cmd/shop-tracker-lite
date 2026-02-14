package com.example.shoptracklite.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplyDao {
    @Query("SELECT * FROM supplies ORDER BY name ASC")
    fun getAllSupplies(): Flow<List<Supply>>
    
    @Query("SELECT * FROM supplies ORDER BY name ASC")
    suspend fun getAllSuppliesList(): List<Supply>
    
    @Query("SELECT * FROM supplies WHERE id = :id")
    suspend fun getSupplyById(id: Long): Supply?
    
    @Query("SELECT * FROM supplies WHERE quantity <= lowStockThreshold")
    fun getLowStockSupplies(): Flow<List<Supply>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupply(supply: Supply): Long
    
    @Update
    suspend fun updateSupply(supply: Supply)
    
    @Delete
    suspend fun deleteSupply(supply: Supply)
    
    @Query("UPDATE supplies SET quantity = quantity + :amount WHERE id = :supplyId")
    suspend fun incrementQuantity(supplyId: Long, amount: Double)
    
    @Query("UPDATE supplies SET quantity = quantity - :amount WHERE id = :supplyId")
    suspend fun decrementQuantity(supplyId: Long, amount: Double)
    
    @Query("UPDATE supplies SET quantity = :newQuantity WHERE id = :supplyId")
    suspend fun setQuantity(supplyId: Long, newQuantity: Double)
}
