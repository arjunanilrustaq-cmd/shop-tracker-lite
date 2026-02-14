package com.example.shoptracklite.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductSupplyLinkDao {
    @Query("SELECT * FROM product_supply_links WHERE productId = :productId")
    suspend fun getLinksForProduct(productId: Long): List<ProductSupplyLink>
    
    @Query("SELECT * FROM product_supply_links WHERE supplyId = :supplyId")
    suspend fun getLinksForSupply(supplyId: Long): List<ProductSupplyLink>
    
    @Query("""
        SELECT s.* FROM supplies s
        INNER JOIN product_supply_links psl ON s.id = psl.supplyId
        WHERE psl.productId = :productId
    """)
    fun getSuppliesForProduct(productId: Long): Flow<List<Supply>>
    
    @Query("""
        SELECT p.* FROM products p
        INNER JOIN product_supply_links psl ON p.id = psl.productId
        WHERE psl.supplyId = :supplyId
    """)
    fun getProductsForSupply(supplyId: Long): Flow<List<Product>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: ProductSupplyLink)
    
    @Delete
    suspend fun deleteLink(link: ProductSupplyLink)
    
    @Query("DELETE FROM product_supply_links WHERE productId = :productId")
    suspend fun deleteLinksForProduct(productId: Long)
    
    @Query("DELETE FROM product_supply_links WHERE supplyId = :supplyId")
    suspend fun deleteLinksForSupply(supplyId: Long)
    
    @Transaction
    suspend fun updateLinksForProduct(productId: Long, links: List<ProductSupplyLink>) {
        deleteLinksForProduct(productId)
        links.forEach { insertLink(it) }
    }
}
