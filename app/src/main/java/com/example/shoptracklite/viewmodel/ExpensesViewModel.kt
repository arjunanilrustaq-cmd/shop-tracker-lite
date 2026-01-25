package com.example.shoptracklite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoptracklite.data.Expense
import com.example.shoptracklite.data.ShopTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExpensesUiState(
    val expenses: List<Expense> = emptyList(),
    val todaysExpenses: List<Expense> = emptyList(),
    val monthlyExpenses: List<Expense> = emptyList(),
    val todaysTotal: Double = 0.0,
    val monthlyTotal: Double = 0.0,
    val selectedTab: Int = 0, // 0 = Daily, 1 = Monthly
    val selectedDate: String? = null, // null means show all current month expenses
    val description: String = "",
    val amount: String = "",
    val category: String = "General",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showAddDialog: Boolean = false,
    val showDatePicker: Boolean = false,
    val editingExpense: Expense? = null,
    val currencyCode: String = "USD"
)

class ExpensesViewModel(
    private val repository: ShopTrackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        loadExpenses()
        loadTodaysExpenses()
        loadMonthlyExpenses()
        loadSettings()
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
    
    private fun loadTodaysExpenses() {
        viewModelScope.launch {
            try {
                repository.getTodaysExpenses().collect { expenses ->
                    _uiState.value = _uiState.value.copy(
                        todaysExpenses = expenses,
                        todaysTotal = expenses.sumOf { it.amount }
                    )
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    private fun loadMonthlyExpenses() {
        viewModelScope.launch {
            try {
                repository.getCurrentMonthExpenses().collect { expenses ->
                    _uiState.value = _uiState.value.copy(
                        monthlyExpenses = expenses,
                        monthlyTotal = expenses.sumOf { it.amount }
                    )
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun loadExpenses() {
        viewModelScope.launch {
            try {
                val selectedDate = _uiState.value.selectedDate
                if (selectedDate == null) {
                    // Show current month expenses
                    repository.getCurrentMonthExpenses().collect { expenses ->
                        _uiState.value = _uiState.value.copy(
                            expenses = expenses,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                } else {
                    // Show expenses for selected date
                    repository.getExpensesByDateString(selectedDate).collect { expenses ->
                        _uiState.value = _uiState.value.copy(
                            expenses = expenses,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }
    
    fun selectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }
    
    fun selectDate(date: String) {
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            showDatePicker = false
        )
        loadExpenses()
    }
    
    fun clearDateFilter() {
        _uiState.value = _uiState.value.copy(
            selectedDate = null,
            showDatePicker = false
        )
        loadExpenses()
    }
    
    fun showDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = true)
    }
    
    fun hideDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = false)
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingExpense = null,
            description = "",
            amount = "",
            category = "General"
        )
    }

    fun showEditDialog(expense: Expense) {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingExpense = expense,
            description = expense.description,
            amount = expense.amount.toString(),
            category = expense.category
        )
    }

    fun hideDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = false,
            editingExpense = null,
            description = "",
            amount = "",
            category = "General"
        )
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount)
    }

    fun updateCategory(category: String) {
        _uiState.value = _uiState.value.copy(category = category)
    }

    fun saveExpense() {
        val description = _uiState.value.description
        val amount = _uiState.value.amount.toDoubleOrNull()
        val category = _uiState.value.category

        if (description.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a description")
            return
        }

        if (amount == null || amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid amount")
            return
        }

        viewModelScope.launch {
            try {
                val editingExpense = _uiState.value.editingExpense
                if (editingExpense != null) {
                    // Update existing expense
                    val updatedExpense = editingExpense.copy(
                        description = description,
                        amount = amount,
                        category = category
                    )
                    repository.updateExpense(updatedExpense)
                    _uiState.value = _uiState.value.copy(
                        showAddDialog = false,
                        editingExpense = null,
                        description = "",
                        amount = "",
                        category = "General",
                        successMessage = "Expense updated successfully!",
                        errorMessage = null
                    )
                } else {
                    // Add new expense
                    val expense = Expense(
                        description = description,
                        amount = amount,
                        category = category
                    )
                    repository.insertExpense(expense)
                    _uiState.value = _uiState.value.copy(
                        showAddDialog = false,
                        description = "",
                        amount = "",
                        category = "General",
                        successMessage = "Expense added successfully!",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to save expense"
                )
            }
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            try {
                repository.deleteExpense(expense)
                _uiState.value = _uiState.value.copy(
                    successMessage = "Expense deleted successfully!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to delete expense"
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

