package com.example.shoptracklite.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shoptracklite.data.Expense
import com.example.shoptracklite.data.ShopTrackRepository
import com.example.shoptracklite.viewmodel.ExpensesViewModel
import com.example.shoptracklite.utils.CurrencyUtils
import com.example.shoptracklite.utils.ReportShareUtils
import com.example.shoptracklite.ui.components.DatePickerDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    repository: ShopTrackRepository,
    viewModel: ExpensesViewModel = viewModel { ExpensesViewModel(repository) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = uiState.selectedTab) { 2 }
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf("Daily", "Monthly")

    // Sync pager with tab selection
    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectTab(pagerState.currentPage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Expenses",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Tab Row
        TabRow(
            selectedTabIndex = pagerState.currentPage
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { 
                        Text(
                            text = title,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = if (index == 0) Icons.Default.Today else Icons.Default.DateRange,
                            contentDescription = title
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> DailyExpenseTab(
                    expenses = uiState.todaysExpenses,
                    totalExpense = uiState.todaysTotal,
                    currencyCode = uiState.currencyCode,
                    isLoading = uiState.isLoading,
                    onEdit = { viewModel.showEditDialog(it) },
                    onDelete = { viewModel.deleteExpense(it) },
                    onShare = {
                        val bitmap = ReportShareUtils.generateDailyExpenseReportBitmap(
                            context = context,
                            expenses = uiState.todaysExpenses,
                            totalExpense = uiState.todaysTotal,
                            currencyCode = uiState.currencyCode
                        )
                        ReportShareUtils.shareExpenseReportAsImage(context, bitmap, isDaily = true)
                    }
                )
                1 -> MonthlyExpenseTab(
                    expenses = uiState.monthlyExpenses,
                    totalExpense = uiState.monthlyTotal,
                    currencyCode = uiState.currencyCode,
                    isLoading = uiState.isLoading,
                    onEdit = { viewModel.showEditDialog(it) },
                    onDelete = { viewModel.deleteExpense(it) },
                    onShare = {
                        val bitmap = ReportShareUtils.generateMonthlyExpenseReportBitmap(
                            context = context,
                            expenses = uiState.monthlyExpenses,
                            totalExpense = uiState.monthlyTotal,
                            currencyCode = uiState.currencyCode
                        )
                        ReportShareUtils.shareExpenseReportAsImage(context, bitmap, isDaily = false)
                    }
                )
            }
        }
    }
    
    // Date Picker Dialog
    if (uiState.showDatePicker) {
        DatePickerDialog(
            onDateSelected = { date ->
                viewModel.selectDate(date)
            },
            onDismiss = {
                viewModel.hideDatePicker()
            }
        )
    }

    // Add/Edit Expense Dialog
    if (uiState.showAddDialog) {
        AddExpenseDialog(
            description = uiState.description,
            amount = uiState.amount,
            category = uiState.category,
            isEditing = uiState.editingExpense != null,
            onDescriptionChange = { viewModel.updateDescription(it) },
            onAmountChange = { viewModel.updateAmount(it) },
            onCategoryChange = { viewModel.updateCategory(it) },
            onSave = { viewModel.saveExpense() },
            onDismiss = { viewModel.hideDialog() }
        )
    }

    // Snackbar Messages
    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
        Snackbar(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(error)
        }
    }

    uiState.successMessage?.let { success ->
        LaunchedEffect(success) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessages()
        }
    }
}

@Composable
fun DailyExpenseTab(
    expenses: List<Expense>,
    totalExpense: Double,
    currencyCode: String,
    isLoading: Boolean,
    onEdit: (Expense) -> Unit,
    onDelete: (Expense) -> Unit,
    onShare: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Summary Card with Share button
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Today's Expenses",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormatter.format(Date()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onShare,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share Daily Report",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = CurrencyUtils.formatCurrency(totalExpense, currencyCode),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Expenses List
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (expenses.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "No Expenses",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No expenses today",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add an expense using the + button",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenses) { expense ->
                    ExpenseItemCard(
                        expense = expense,
                        currencyCode = currencyCode,
                        onEdit = { onEdit(expense) },
                        onDelete = { onDelete(expense) }
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyExpenseTab(
    expenses: List<Expense>,
    totalExpense: Double,
    currencyCode: String,
    isLoading: Boolean,
    onEdit: (Expense) -> Unit,
    onDelete: (Expense) -> Unit,
    onShare: () -> Unit
) {
    val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Summary Card with Share button
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Monthly Expenses",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = monthFormatter.format(Date()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onShare,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share Monthly Report",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = CurrencyUtils.formatCurrency(totalExpense, currencyCode),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
                
                // Category breakdown
                if (expenses.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "By Category",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val groupedExpenses = expenses.groupBy { it.category }
                    groupedExpenses.entries.sortedByDescending { it.value.sumOf { e -> e.amount } }.take(4).forEach { (category, categoryExpenses) ->
                        val categoryTotal = categoryExpenses.sumOf { it.amount }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = CurrencyUtils.formatCurrency(categoryTotal, currencyCode),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFF44336)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Expenses List
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (expenses.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxSize(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "No Expenses",
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No expenses this month",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Add an expense using the + button",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenses) { expense ->
                    ExpenseItemCard(
                        expense = expense,
                        currencyCode = currencyCode,
                        onEdit = { onEdit(expense) },
                        onDelete = { onDelete(expense) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    currencyCode: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = expense.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = dateFormat.format(expense.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = CurrencyUtils.formatCurrency(expense.amount, currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFFF44336)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    description: String,
    amount: String,
    category: String,
    isEditing: Boolean,
    onDescriptionChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var expandedCategory by remember { mutableStateOf(false) }
    val categories = listOf("General", "Rent", "Utilities", "Supplies", "Salary", "Marketing", "Transportation", "Other")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Edit Expense" else "Add Expense")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                            onAmountChange(value)
                        }
                    },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                ExposedDropdownMenuBox(
                    expanded = expandedCategory,
                    onExpandedChange = { expandedCategory = !expandedCategory }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCategory)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedCategory,
                        onDismissRequest = { expandedCategory = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    onCategoryChange(cat)
                                    expandedCategory = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

