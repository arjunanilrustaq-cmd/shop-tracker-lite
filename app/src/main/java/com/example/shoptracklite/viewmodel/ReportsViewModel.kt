package com.example.shoptracklite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoptracklite.data.Sale
import com.example.shoptracklite.data.Expense
import com.example.shoptracklite.data.ShopTrackRepository
import com.example.shoptracklite.data.MonthlySalesSummary
import com.example.shoptracklite.data.CashReconciliation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ReportType {
    CASH_FLOW,    // Simple: Cash In - Cash Out = Net
    ACCOUNTING    // Detailed: Revenue - COGS = Gross Profit - Expenses = Net Profit
}

data class ReportsUiState(
    val todaysSales: List<Sale> = emptyList(),
    val monthlySalesByDate: List<MonthlySalesSummary> = emptyList(),
    val selectedDateSales: List<Sale> = emptyList(),
    val selectedDateExpenses: List<Expense> = emptyList(),
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val selectedDateExpenseTotal: Double = 0.0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showDatePicker: Boolean = false,
    val showReportDialog: Boolean = false,
    val currencyCode: String = "USD",
    val shopName: String = "",
    val crNumber: String = "",
    val billForReceipt: List<Sale>? = null,
    val allSalesCount: Int = 0,
    val allExpensesCount: Int = 0,
    // Tab state: 0 = Daily (default), 1 = Monthly
    val selectedTabIndex: Int = 0,
    // Report type toggle
    val reportType: ReportType = ReportType.ACCOUNTING,
    // Selected year-month for monthly reports (format: "YYYY-MM")
    val selectedYearMonth: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
    // Monthly totals
    val monthlyTotalSales: Int = 0,
    val monthlyTotalItems: Int = 0,
    val monthlyTotalRevenue: Double = 0.0,
    val monthlyTotalExpenses: Double = 0.0,
    val monthlyProfit: Double = 0.0,
    val monthlyCashRevenue: Double = 0.0,
    val monthlyVisaRevenue: Double = 0.0,
    val monthlyCashSalesCount: Int = 0,
    val monthlyVisaSalesCount: Int = 0,
    val monthlyAllSales: List<Sale> = emptyList(),
    val monthlyAllExpenses: List<Expense> = emptyList(),
    // Monthly opening cash (from first day of the month)
    val monthlyOpeningCash: Double = 0.0,
    // COGS (Cost of Goods Sold) - calculated from sales
    val dailyCOGS: Double = 0.0,
    val monthlyCOGS: Double = 0.0,
    // Operating expenses (excludes inventory purchases for accounting view)
    val dailyOperatingExpenses: Double = 0.0,
    val monthlyOperatingExpenses: Double = 0.0,
    // Cancel sale confirmation
    val showCancelConfirmDialog: Boolean = false,
    val saleToCancel: Sale? = null,
    val billToCancel: List<Sale>? = null,
    // Cash Reconciliation
    val openingCash: String = "",  // Editable on first day, or auto-filled from previous day
    val hasPreviousReconciliation: Boolean = false,  // True if previous day has saved data
    val actualCashCounted: String = "",
    val changeForTomorrow: String = "",
    val reconciliationNotes: String = "",  // Notes when extra cash (e.g. added for change)
    val reconciliationSaved: Boolean = false
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
                        currencyCode = settings?.currencyCode ?: "USD",
                        shopName = settings?.shopName ?: "",
                        crNumber = settings?.crNumber ?: ""
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
        val selectedYearMonth = _uiState.value.selectedYearMonth
        viewModelScope.launch {
            try {
                repository.getMonthlySalesByDateForMonth(selectedYearMonth).collect { monthlySales ->
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
        
        // Reset cash reconciliation when date changes
        _uiState.update { it.copy(
            openingCash = "",
            hasPreviousReconciliation = false,
            actualCashCounted = "", 
            changeForTomorrow = "",
            reconciliationNotes = "",
            reconciliationSaved = false
        ) }
        
        // Load cash reconciliation for this date and previous day's float
        loadCashReconciliation(selectedDate)
        
        // Collect sales independently
        viewModelScope.launch {
            try {
                repository.getSalesByDateString(selectedDate).collect { sales ->
                    android.util.Log.d("ReportsViewModel", "Sales collected: ${sales.size} items for $selectedDate")
                    
                    _uiState.update { currentState ->
                        currentState.copy(
                            selectedDateSales = sales,
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
    
    private fun loadCashReconciliation(date: String) {
        viewModelScope.launch {
            try {
                // Load previous day's reconciliation to get opening cash
                val previousDayRecon = repository.getPreviousDayReconciliation(date)
                val hasPreviousRecord = previousDayRecon != null
                val openingCashFromPrevious = previousDayRecon?.changeForTomorrow ?: 0.0
                
                // Load saved reconciliation for this date
                val savedRecon = repository.getCashReconciliationByDateSync(date)
                
                _uiState.update { currentState ->
                    currentState.copy(
                        hasPreviousReconciliation = hasPreviousRecord,
                        // If previous day exists, use its changeForTomorrow as opening cash (read-only)
                        // If no previous day but this day is saved, use saved opening cash
                        // If first day and not saved, leave empty for user to enter
                        openingCash = when {
                            hasPreviousRecord -> openingCashFromPrevious.toString()
                            savedRecon != null && savedRecon.openingCash > 0 -> savedRecon.openingCash.toString()
                            else -> ""  // First day, user needs to enter
                        },
                        actualCashCounted = if (savedRecon != null && savedRecon.actualCashCounted > 0) 
                            savedRecon.actualCashCounted.toString() else "",
                        changeForTomorrow = if (savedRecon != null && savedRecon.changeForTomorrow > 0) 
                            savedRecon.changeForTomorrow.toString() else "",
                        reconciliationNotes = savedRecon?.notes ?: "",
                        reconciliationSaved = savedRecon != null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ReportsViewModel", "Error loading cash reconciliation", e)
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
    
    fun toggleReportType() {
        val currentType = _uiState.value.reportType
        val newType = if (currentType == ReportType.CASH_FLOW) ReportType.ACCOUNTING else ReportType.CASH_FLOW
        _uiState.value = _uiState.value.copy(reportType = newType)
    }
    
    fun setReportType(reportType: ReportType) {
        _uiState.value = _uiState.value.copy(reportType = reportType)
    }
    
    // Calculate COGS from sales
    fun getDailyCOGS(): Double {
        return _uiState.value.selectedDateSales.sumOf { it.costPrice * it.quantitySold }
    }
    
    fun getMonthlyCOGS(): Double {
        return _uiState.value.monthlyAllSales.sumOf { it.costPrice * it.quantitySold }
    }
    
    // Get operating expenses (exclude "Inventory Purchase" category for accounting view)
    fun getDailyOperatingExpenses(): Double {
        return _uiState.value.selectedDateExpenses
            .filter { it.category != "Inventory Purchase" }
            .sumOf { it.amount }
    }
    
    fun getMonthlyOperatingExpenses(): Double {
        return _uiState.value.monthlyAllExpenses
            .filter { it.category != "Inventory Purchase" }
            .sumOf { it.amount }
    }
    
    // Get total expenses (all categories, for cash flow view)
    fun getDailyTotalExpenses(): Double {
        return _uiState.value.selectedDateExpenses.sumOf { it.amount }
    }
    
    fun getMonthlyTotalExpenses(): Double {
        return _uiState.value.monthlyAllExpenses.sumOf { it.amount }
    }
    
    fun goToPreviousMonth() {
        val currentYearMonth = _uiState.value.selectedYearMonth
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        try {
            val date = dateFormat.parse(currentYearMonth)
            val calendar = Calendar.getInstance()
            calendar.time = date ?: Date()
            calendar.add(Calendar.MONTH, -1)
            val newYearMonth = dateFormat.format(calendar.time)
            _uiState.value = _uiState.value.copy(selectedYearMonth = newYearMonth)
            loadMonthlySalesByDate()
            loadMonthlyTotals()
        } catch (e: Exception) {
            android.util.Log.e("ReportsViewModel", "Error navigating to previous month", e)
        }
    }
    
    fun goToNextMonth() {
        val currentYearMonth = _uiState.value.selectedYearMonth
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonthFormat = dateFormat.format(Date())
        
        // Don't allow going beyond current month
        if (currentYearMonth >= currentMonthFormat) {
            return
        }
        
        try {
            val date = dateFormat.parse(currentYearMonth)
            val calendar = Calendar.getInstance()
            calendar.time = date ?: Date()
            calendar.add(Calendar.MONTH, 1)
            val newYearMonth = dateFormat.format(calendar.time)
            _uiState.value = _uiState.value.copy(selectedYearMonth = newYearMonth)
            loadMonthlySalesByDate()
            loadMonthlyTotals()
        } catch (e: Exception) {
            android.util.Log.e("ReportsViewModel", "Error navigating to next month", e)
        }
    }
    
    fun isCurrentMonth(): Boolean {
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonth = dateFormat.format(Date())
        return _uiState.value.selectedYearMonth == currentMonth
    }
    
    private fun loadMonthlyTotals() {
        val selectedYearMonth = _uiState.value.selectedYearMonth
        // Combine both sales and expenses flows to calculate profit
        viewModelScope.launch {
            try {
                // Load opening cash from the first day of the month
                val firstDayOfMonth = "$selectedYearMonth-01"
                val firstDayReconciliation = repository.getCashReconciliationByDateSync(firstDayOfMonth)
                val monthlyOpeningCash = firstDayReconciliation?.openingCash ?: 0.0
                
                combine(
                    repository.getSalesByYearMonth(selectedYearMonth),
                    repository.getExpensesByYearMonth(selectedYearMonth)
                ) { sales, expenses ->
                    Pair(sales, expenses)
                }.collect { (sales, expenses) ->
                    val totalRevenue = sales.sumOf { it.totalAmount }
                    val totalExpenses = expenses.sumOf { it.amount }
                    // Simplified profit: Revenue - Expenses
                    val profit = totalRevenue - totalExpenses
                    
                    val cashSales = sales.filter { it.paymentMethod == com.example.shoptracklite.data.PaymentMethod.CASH }
                    val visaSales = sales.filter { it.paymentMethod == com.example.shoptracklite.data.PaymentMethod.VISA }
                    val billCount = sales.groupBy { it.transactionId ?: it.id }.size
                    val cashBillCount = cashSales.groupBy { it.transactionId ?: it.id }.size
                    val visaBillCount = visaSales.groupBy { it.transactionId ?: it.id }.size
                    _uiState.update { currentState ->
                        currentState.copy(
                            monthlyAllSales = sales,
                            monthlyAllExpenses = expenses,
                            monthlyTotalSales = billCount,
                            monthlyTotalItems = sales.sumOf { it.quantitySold },
                            monthlyTotalRevenue = totalRevenue,
                            monthlyTotalExpenses = totalExpenses,
                            monthlyProfit = profit,
                            monthlyCashRevenue = cashSales.sumOf { it.totalAmount },
                            monthlyVisaRevenue = visaSales.sumOf { it.totalAmount },
                            monthlyCashSalesCount = cashBillCount,
                            monthlyVisaSalesCount = visaBillCount,
                            monthlyOpeningCash = monthlyOpeningCash
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
            saleToCancel = sale,
            billToCancel = listOf(sale)
        )
    }
    
    fun showCancelBillConfirmation(sales: List<Sale>) {
        _uiState.value = _uiState.value.copy(
            showCancelConfirmDialog = true,
            saleToCancel = null,
            billToCancel = sales
        )
    }
    
    fun hideCancelConfirmation() {
        _uiState.value = _uiState.value.copy(
            showCancelConfirmDialog = false,
            saleToCancel = null,
            billToCancel = null
        )
    }
    
    fun showBillReceipt(billSales: List<Sale>) {
        _uiState.value = _uiState.value.copy(billForReceipt = billSales)
    }
    
    fun hideBillReceipt() {
        _uiState.value = _uiState.value.copy(billForReceipt = null)
    }
    
    fun cancelSale() {
        val bill = _uiState.value.billToCancel ?: return
        
        viewModelScope.launch {
            try {
                val success = repository.cancelSalesByTransaction(bill)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Bill cancelled successfully. Inventory has been restored.",
                        showCancelConfirmDialog = false,
                        saleToCancel = null,
                        billToCancel = null,
                        errorMessage = null
                    )
                    refreshReports()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to cancel bill",
                        showCancelConfirmDialog = false,
                        saleToCancel = null,
                        billToCancel = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error: ${e.message}",
                    showCancelConfirmDialog = false,
                    saleToCancel = null,
                    billToCancel = null
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
    
    // Cash Reconciliation Functions
    fun updateOpeningCash(value: String) {
        // Only allow numeric input with optional decimal (only for first day)
        if (!_uiState.value.hasPreviousReconciliation) {
            val filtered = value.filter { it.isDigit() || it == '.' }
            _uiState.update { it.copy(openingCash = filtered, reconciliationSaved = false) }
        }
    }
    
    fun updateActualCash(value: String) {
        // Only allow numeric input with optional decimal
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(actualCashCounted = filtered, reconciliationSaved = false) }
    }
    
    fun updateChangeForTomorrow(value: String) {
        // Only allow numeric input with optional decimal
        val filtered = value.filter { it.isDigit() || it == '.' }
        _uiState.update { it.copy(changeForTomorrow = filtered, reconciliationSaved = false) }
    }
    
    fun updateReconciliationNotes(value: String) {
        _uiState.update { it.copy(reconciliationNotes = value, reconciliationSaved = false) }
    }
    
    // Get opening cash value (either from previous day or manual entry)
    fun getOpeningCashValue(): Double {
        return _uiState.value.openingCash.toDoubleOrNull() ?: 0.0
    }
    
    // Calculate expected cash (Opening Cash + Cash Sales - Cash Expenses)
    fun getExpectedCash(): Double {
        val state = _uiState.value
        val openingCash = state.openingCash.toDoubleOrNull() ?: 0.0
        val cashRevenue = state.selectedDateSales
            .filter { it.paymentMethod == com.example.shoptracklite.data.PaymentMethod.CASH }
            .sumOf { it.totalAmount }
        // Expected = Opening Cash + Cash Sales - Expenses (paid in cash)
        return openingCash + cashRevenue - state.selectedDateExpenseTotal
    }
    
    // Calculate cash difference (Actual - Expected)
    fun getCashDifference(): Double {
        val actualCash = _uiState.value.actualCashCounted.toDoubleOrNull() ?: 0.0
        return actualCash - getExpectedCash()
    }
    
    // Calculate cash to take out (Actual - Change for Tomorrow)
    fun getCashToTakeOut(): Double {
        val actualCash = _uiState.value.actualCashCounted.toDoubleOrNull() ?: 0.0
        val changeForTomorrow = _uiState.value.changeForTomorrow.toDoubleOrNull() ?: 0.0
        return actualCash - changeForTomorrow
    }
    
    // Save cash reconciliation to database
    fun saveCashReconciliation() {
        val state = _uiState.value
        val openingCash = state.openingCash.toDoubleOrNull() ?: 0.0
        val actualCash = state.actualCashCounted.toDoubleOrNull() ?: 0.0
        val changeForTomorrow = state.changeForTomorrow.toDoubleOrNull() ?: 0.0
        
        // On first day without previous record, opening cash is required
        if (!state.hasPreviousReconciliation && openingCash <= 0 && state.openingCash.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter the opening cash") }
            return
        }
        
        if (actualCash <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter the actual cash counted") }
            return
        }
        
        viewModelScope.launch {
            try {
                val reconciliation = CashReconciliation(
                    date = state.selectedDate,
                    openingCash = openingCash,
                    actualCashCounted = actualCash,
                    changeForTomorrow = changeForTomorrow,
                    notes = state.reconciliationNotes
                )
                repository.saveCashReconciliation(reconciliation)
                _uiState.update { it.copy(
                    reconciliationSaved = true,
                    successMessage = "Cash reconciliation saved"
                ) }
            } catch (e: Exception) {
                android.util.Log.e("ReportsViewModel", "Error saving cash reconciliation", e)
                _uiState.update { it.copy(errorMessage = "Failed to save: ${e.message}") }
            }
        }
    }
}
