package com.example.shoptracklite.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CashReconciliationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(reconciliation: CashReconciliation)
    
    @Query("SELECT * FROM cash_reconciliation WHERE date = :date")
    fun getByDate(date: String): Flow<CashReconciliation?>
    
    @Query("SELECT * FROM cash_reconciliation WHERE date = :date")
    suspend fun getByDateSync(date: String): CashReconciliation?
    
    @Query("SELECT * FROM cash_reconciliation WHERE date < :date ORDER BY date DESC LIMIT 1")
    suspend fun getPreviousDay(date: String): CashReconciliation?
    
    @Query("SELECT * FROM cash_reconciliation ORDER BY date DESC")
    fun getAll(): Flow<List<CashReconciliation>>
    
    @Delete
    suspend fun delete(reconciliation: CashReconciliation)
}
