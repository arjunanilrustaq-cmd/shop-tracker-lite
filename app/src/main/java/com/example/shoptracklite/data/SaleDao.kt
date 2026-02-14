package com.example.shoptracklite.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE isCancelled = 0 ORDER BY saleDate DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE isCancelled = 0 AND DATE(saleDate/1000, 'unixepoch') = DATE(:date/1000, 'unixepoch')")
    fun getSalesByDate(date: Date): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE isCancelled = 0 AND DATE(saleDate/1000, 'unixepoch') = DATE('now')")
    fun getTodaysSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :saleId")
    suspend fun getSaleById(saleId: Long): Sale?

    @Insert
    suspend fun insertSale(sale: Sale): Long

    @Update
    suspend fun updateSale(sale: Sale)

    @Query("SELECT SUM(totalAmount) FROM sales WHERE isCancelled = 0 AND DATE(saleDate/1000, 'unixepoch') = DATE('now')")
    suspend fun getTodaysRevenue(): Double?

    @Query("SELECT SUM(profit) FROM sales WHERE isCancelled = 0 AND DATE(saleDate/1000, 'unixepoch') = DATE('now')")
    suspend fun getTodaysProfit(): Double?

    @Query("SELECT COUNT(*) FROM sales WHERE isCancelled = 0 AND DATE(saleDate/1000, 'unixepoch') = DATE('now')")
    suspend fun getTodaysSalesCount(): Int

    @Query("SELECT SUM(totalAmount) FROM sales WHERE isCancelled = 0 AND DATE(saleDate/1000, 'unixepoch') = DATE('now') AND paymentMethod = 'CASH'")
    suspend fun getTodaysCashRevenue(): Double?

    @Query("SELECT SUM(totalAmount) FROM sales WHERE isCancelled = 0 AND DATE(saleDate/1000, 'unixepoch') = DATE('now') AND paymentMethod = 'VISA'")
    suspend fun getTodaysVisaRevenue(): Double?

    @Query("SELECT COUNT(*) FROM sales WHERE isCancelled = 0 AND DATE(saleDate/1000, 'unixepoch') = DATE('now') AND paymentMethod = 'CASH'")
    suspend fun getTodaysCashSalesCount(): Int

    @Query("SELECT COUNT(*) FROM sales WHERE isCancelled = 0 AND DATE(saleDate/1000, 'unixepoch') = DATE('now') AND paymentMethod = 'VISA'")
    suspend fun getTodaysVisaSalesCount(): Int

    // Monthly reports queries
    @Query("SELECT * FROM sales WHERE isCancelled = 0 AND strftime('%Y-%m', saleDate/1000, 'unixepoch') = strftime('%Y-%m', 'now') ORDER BY saleDate DESC")
    fun getCurrentMonthSales(): Flow<List<Sale>>

    @Query("SELECT DATE(saleDate/1000, 'unixepoch', 'localtime') as date, COUNT(*) as salesCount, SUM(totalAmount) as totalRevenue, SUM(profit) as totalProfit FROM sales WHERE isCancelled = 0 AND strftime('%Y-%m', saleDate/1000, 'unixepoch', 'localtime') = strftime('%Y-%m', 'now', 'localtime') GROUP BY DATE(saleDate/1000, 'unixepoch', 'localtime') ORDER BY date DESC")
    fun getMonthlySalesByDate(): Flow<List<MonthlySalesSummary>>

    // Sales for a specific year-month (format: "YYYY-MM")
    @Query("SELECT * FROM sales WHERE isCancelled = 0 AND strftime('%Y-%m', saleDate/1000, 'unixepoch', 'localtime') = :yearMonth ORDER BY saleDate DESC")
    fun getSalesByYearMonth(yearMonth: String): Flow<List<Sale>>

    @Query("SELECT DATE(saleDate/1000, 'unixepoch', 'localtime') as date, COUNT(*) as salesCount, SUM(totalAmount) as totalRevenue, SUM(profit) as totalProfit FROM sales WHERE isCancelled = 0 AND strftime('%Y-%m', saleDate/1000, 'unixepoch', 'localtime') = :yearMonth GROUP BY DATE(saleDate/1000, 'unixepoch', 'localtime') ORDER BY date DESC")
    fun getMonthlySalesByDateForMonth(yearMonth: String): Flow<List<MonthlySalesSummary>>

    @Query("SELECT * FROM sales WHERE isCancelled = 0 AND DATE(saleDate/1000, 'unixepoch', 'localtime') = :date ORDER BY saleDate DESC")
    fun getSalesByDateString(date: String): Flow<List<Sale>>

    @Query("SELECT SUM(totalAmount) FROM sales WHERE isCancelled = 0 AND DATE(saleDate/1000, 'unixepoch', 'localtime') = :date")
    suspend fun getRevenueByDate(date: String): Double?

    @Query("SELECT SUM(profit) FROM sales WHERE isCancelled = 0 AND DATE(saleDate/1000, 'unixepoch', 'localtime') = :date")
    suspend fun getProfitByDate(date: String): Double?

    @Query("SELECT COUNT(*) FROM sales WHERE isCancelled = 0 AND DATE(saleDate/1000, 'unixepoch', 'localtime') = :date")
    suspend fun getSalesCountByDate(date: String): Int
}

data class MonthlySalesSummary(
    val date: String,
    val salesCount: Int,
    val totalRevenue: Double,
    val totalProfit: Double
)
