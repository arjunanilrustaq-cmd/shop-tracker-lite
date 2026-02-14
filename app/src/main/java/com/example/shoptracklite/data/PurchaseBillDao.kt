package com.example.shoptracklite.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

data class PurchaseBillWithItems(
    @Embedded val bill: PurchaseBill,
    @Relation(
        parentColumn = "id",
        entityColumn = "billId"
    )
    val items: List<PurchaseItem>
)

@Dao
interface PurchaseBillDao {
    @Query("SELECT * FROM purchase_bills ORDER BY date DESC")
    fun getAllBills(): Flow<List<PurchaseBill>>
    
    @Transaction
    @Query("SELECT * FROM purchase_bills ORDER BY date DESC")
    fun getAllBillsWithItems(): Flow<List<PurchaseBillWithItems>>
    
    @Transaction
    @Query("SELECT * FROM purchase_bills WHERE id = :billId")
    suspend fun getBillWithItems(billId: Long): PurchaseBillWithItems?
    
    @Query("SELECT * FROM purchase_bills WHERE date >= :startDate AND date < :endDate ORDER BY date DESC")
    fun getBillsForDateRange(startDate: Date, endDate: Date): Flow<List<PurchaseBill>>
    
    @Transaction
    @Query("SELECT * FROM purchase_bills WHERE date >= :startDate AND date < :endDate ORDER BY date DESC")
    fun getBillsWithItemsForDateRange(startDate: Date, endDate: Date): Flow<List<PurchaseBillWithItems>>
    
    @Query("SELECT SUM(totalAmount) FROM purchase_bills WHERE date >= :startDate AND date < :endDate")
    suspend fun getTotalPurchasesForDateRange(startDate: Date, endDate: Date): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: PurchaseBill): Long
    
    @Update
    suspend fun updateBill(bill: PurchaseBill)
    
    @Delete
    suspend fun deleteBill(bill: PurchaseBill)
    
    @Query("SELECT * FROM purchase_items WHERE billId = :billId")
    suspend fun getItemsForBill(billId: Long): List<PurchaseItem>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: PurchaseItem): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<PurchaseItem>)
    
    @Delete
    suspend fun deleteItem(item: PurchaseItem)
    
    @Query("DELETE FROM purchase_items WHERE billId = :billId")
    suspend fun deleteItemsForBill(billId: Long)
    
    @Transaction
    suspend fun insertBillWithItems(bill: PurchaseBill, items: List<PurchaseItem>): Long {
        val billId = insertBill(bill)
        val itemsWithBillId = items.map { it.copy(billId = billId) }
        insertItems(itemsWithBillId)
        return billId
    }
    
    @Transaction
    suspend fun deleteBillWithItems(bill: PurchaseBill) {
        deleteItemsForBill(bill.id)
        deleteBill(bill)
    }
}
