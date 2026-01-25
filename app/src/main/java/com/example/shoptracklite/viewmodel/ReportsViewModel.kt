package com.example.shoptracklite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoptracklite.data.Sale
import com.example.shoptracklite.data.Expense
import com.example.shoptracklite.data.ShopTrackRepository
import com.example.shoptracklite.data.MonthlySalesSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReportsUiState(
    val todaysSales: List<Sale> = emptyList(),
    val monthlySalesByDate: List<MonthlySalesSummary> = emptyList(),
    val selectedDateSales: List<Sale> = emptyList(),
    val selectedDateExpenses: List<Expense> = emptyList(),
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val selectedDateCOGS: Double = 0.0,
    val selectedDateExpenseTotal: Double = 0.0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showDatePicker: Boolean = false,
    val showReportDialog: Boolean = false,
    val currencyCode: String = "USD",
    val allSalesCount: Int = 0,
    val allExpensesCount: Int = 0,
    // Tab state: 0 = Daily (default), 1 = Monthly
    val selectedTabIndex: Int = 0,
    // Monthly totals
    val monthlyTotalSales: Int = 0,
    val monthlyTotalItems: Int = 0,
    val monthlyTotalRevenue: Double = 0.0,
    val monthlyTotalCOGS: Double = 0.0,
    val monthlyTotalExpenses: Double = 0.0,
    val monthlyGrossProfit: Double = 0.0,
    val monthlyNetProfit: Double = 0.0,
    val monthlyCashRevenue: Double = 0.0,
    val monthlyVisaRevenue: Double = 0.0,
    val monthlyCashSalesCount: Int = 0,
    val monthlyVisaSalesCount: Int = 0,
    val monthlyAllSales: List<Sale> = emptyList(),
    val monthlyAllExpenses: List<Expense> = emptyList(),
    // Cancel sale confirmation
    val showCancelConfirmDialog: Boolean = false,
    val saleToCancel: Sale? = null
)

class ReportsViewModel(
    private val repository: ShopTrackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadTodaysSales()
        loadMonthlySalesByDate()
        loadSelectedDateData()
        loadSettings()
        loadAllDataCounts()
        loadMonthlyTotals()
    }
    
    private fun loadAllDataCounts() {
        // Load all sales to verify database has data
        viewModelScope.launch {
            try {
                repository.getAllSales().collect { allSales ->
                    android.util.Log.d("ReportsViewModel", "Total sales in DB: ${allSales.size}")
                    // Log all sale dates to see what's actually stored
                    allSales.forEachIndexed { index, sale ->
                        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(sale.saleDate)
                        android.util.Log.d("ReportsViewModel", "Sale #$index date: $dateStr (${sale.productName})")
                    }
                    _uiState.value = _uiState.value.copy(allSalesCount = allSales.size)
                }
            } catch (e: Exception) {
                android.util.Log.e("ReportsViewModel", "Error loading all sales", e)
            }
        }
        
        // Load all expenses to verify database has data
        viewModelScope.launch {
            try {
                repository.getAllExpenses().collect { allExpenses ->
                    android.util.Log.d("ReportsViewModel", "Total expenses in DB: ${allExpenses.size}")
                    // Log all expense dates to see what's actually stored
                    allExpenses.forEachIndexed { index, expense ->
                        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(expense.date)
                        android.util.Log.d("ReportsViewModel", "Expense #$index date: $dateStr (${expense.description})")
                    }
                    _uiState.value = _uiState.value.copy(allExpensesCount = allExpenses.size)
                }
            } catch (e: Exception) {
                android.util.Log.e("ReportsViewModel", "Error loading all expenses", e)
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                repository.getSettings().collect { settings ->
                    _uiState.value = _uiState.value.copy(
                        currencyCode = settings?.currencyCode ?: "USD"
                    )
                }
            } catch (e: Exception) {
                // Ignore settings errors
            }
        }
    }

    private fun loadTodaysSales() {
        viewModelScope.launch {
            try {
                repository.getTodaysSales().collect { sales ->
                    _uiState.value = _uiState.value.copy(
                        todaysSales = sales,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    private fun loadMonthlySalesByDate() {
        viewModelScope.launch {
            try {
                repository.getMonthlySalesByDate().collect { monthlySales ->
                    _uiState.value = _uiState.value.copy(
                        monthlySalesByDate = monthlySales,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Monthly data error: ${e.message}"
                )
            }
        }
    }

    private fun loadSelectedDateData() {
        val selectedDate = _uiState.value.selectedDate
        android.util.Log.d("ReportsViewModel", "Loading data for date: $selectedDate")
        
        // Collect sales independently
        viewModelScope.launch {
            try {
                repository.getSalesByDateString(selectedDate).collect { sales ->
                    android.util.Log.d("ReportsViewModel", "Sales collected: ${sales.size} items for $selectedDate")
                    // Log first sale date if exists
                    if (sales.isNotEmpty()) {
                        val saleDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(sales.first().saleDate)
                        android.util.Log.d("ReportsViewModel", "First sale date: $saleDate")
                    }
                    
                    _uiState.update { currentState ->
                        currentState.copy(
                            selectedDateSales = sales,
                            selectedDateCOGS = sales.sumOf { it.costPrice * it.quantitySold },
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ReportsViewModel", "Error loading sales for $selectedDate", e)
                _uiState.update { it.copy(errorMessage = "Sales: ${e.message}", isLoading = false) }
            }
        }
        
        // Collect expenses independently
        viewModelScope.launch {
            try {
                repository.getExpensesByDateString(selectedDate).collect { expenses ->
                    android.util.Log.d("ReportsViewModel", "Expenses collected: ${expenses.size} items for $selectedDate")
                    // Log first expense date if exists
                    if (expenses.isNotEmpty()) {
                        val expenseDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(expenses.first().date)
                        android.util.Log.d("ReportsViewModel", "First expense date: $expenseDate")
                    }
                    
                    _uiState.update { currentState ->
                        currentState.copy(
                            selectedDateExpenses = expenses,
                            selectedDateExpenseTotal = expenses.sumOf { it.amount },
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ReportsViewModel", "Error loading expenses for $selectedDate", e)
                _uiState.update { it.copy(errorMessage = "Expenses: ${e.message}", isLoading = false) }
            }
        }
    }

    fun refreshReports() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadTodaysSales()
        loadMonthlySalesByDate()
        loadSelectedDateData()
    }

    fun selectDate(date: String) {
        _uiState.value = _uiState.value.copy(selectedDate = date)
        loadSelectedDateData()
    }

    fun showDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = true)
    }

    fun hideDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = false)
    }
    
    fun showReportDialog() {
        _uiState.value = _uiState.value.copy(showReportDialog = true)
    }
    
    fun hideReportDialog() {
        _uiState.value = _uiState.value.copy(showReportDialog = false)
    }
    
    fun setSelectedTab(tabIndex: Int) {
        _uiState.value = _uiState.value.copy(selectedTabIndex = tabIndex)
    }
    
    private fun loadMonthlyTotals() {
        // Combine both sales and expenses flows to calculate net profit correctly
        viewModelScope.launch {
            try {
                combine(
                    repository.getCurrentMonthSales(),
                    repository.getCurrentMonthExpenses()
                ) { sales, expenses ->
                    Pair(sales, expenses)
                }.collect { (sales, expenses) ->
                    val totalRevenue = sales.sumOf { it.totalAmount }
                    val totalCOGS = sales.sumOf { it.costPrice * it.quantitySold }
                    val grossProfit = sales.sumOf { it.profit }
                    val totalExpenses = expenses.sumOf { it.amount }
                    val netProfit = grossProfit - totalExpenses
                    
                    // Debug logging
                    android.util.Log.d("ReportsViewModel", "Monthly Totals - Revenue: $totalRevenue, COGS: $totalCOGS, Gross Profit: $grossProfit, Expenses: $totalExpenses, Net Profit: $netProfit")
                    
                    val cashSales = sales.filter { it.paymentMethod == com.example.shoptracklite.data.PaymentMethod.CASH }
                    val visaSales = sales.filter { it.paymentMethod == com.example.shoptracklite.data.PaymentMethod.VISA }
                    
                    _uiState.update { currentState ->
                        currentState.copy(
                            monthlyAllSales = sales,
                            monthlyAllExpenses = expenses,
                            monthlyTotalSales = sales.size,
                            monthlyTotalItems = sales.sumOf { it.quantitySold },
                            monthlyTotalRevenue = totalRevenue,
                            monthlyTotalCOGS = totalCOGS,
                            monthlyGrossProfit = grossProfit,
                            monthlyTotalExpenses = totalExpenses,
                            monthlyNetProfit = netProfit,
                            monthlyCashRevenue = cashSales.sumOf { it.totalAmount },
                            monthlyVisaRevenue = visaSales.sumOf { it.totalAmount },
                            monthlyCashSalesCount = cashSales.size,
                            monthlyVisaSalesCount = visaSales.size
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ReportsViewModel", "Error loading monthly totals", e)
            }
        }
    }
    
    fun showCancelConfirmation(sale: Sale) {
        _uiState.value = _uiState.value.copy(
            showCancelConfirmDialog = true,
            saleToCancel = sale
        )
    }
    
    fun hideCancelConfirmation() {
        _uiState.value = _uiState.value.copy(
            showCancelConfirmDialog = false,
            saleToCancel = null
        )
    }
    
    fun cancelSale() {
        val sale = _uiState.value.saleToCancel ?: return
        
        viewModelScope.launch {
            try {
                val success = repository.cancelSale(sale.id)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Sale cancelled successfully. Inventory has been restored.",
                        showCancelConfirmDialog = false,
                        saleToCancel = null,
                        errorMessage = null
                    )
                    // Refresh data to reflect changes
                    refreshReports()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to cancel sale",
                        showCancelConfirmDialog = false,
                        saleToCancel = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error: ${e.message}",
                    showCancelConfirmDialog = false,
                    saleToCancel = null
                )
            }
        }
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
