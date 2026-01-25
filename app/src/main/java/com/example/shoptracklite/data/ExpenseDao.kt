package com.example.shoptracklite.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE DATE(date/1000, 'unixepoch') = DATE('now')")
    fun getTodaysExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE strftime('%Y-%m', date/1000, 'unixepoch') = strftime('%Y-%m', 'now') ORDER BY date DESC")
    fun getCurrentMonthExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE DATE(date/1000, 'unixepoch', 'localtime') = :date")
    fun getExpensesByDateString(date: String): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses WHERE DATE(date/1000, 'unixepoch') = DATE('now')")
    suspend fun getTodaysExpenseTotal(): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE DATE(date/1000, 'unixepoch', 'localtime') = :date")
    suspend fun getExpenseTotalByDate(date: String): Double?

    @Insert
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)
}

