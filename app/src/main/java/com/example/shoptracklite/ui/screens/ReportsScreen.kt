package com.example.shoptracklite.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.shoptracklite.data.PaymentMethod
import com.example.shoptracklite.data.Sale
import com.example.shoptracklite.data.ShopTrackRepository
import com.example.shoptracklite.viewmodel.ReportsViewModel
import com.example.shoptracklite.viewmodel.ReportsUiState
import com.example.shoptracklite.ui.components.DatePickerDialog
import com.example.shoptracklite.ui.components.MonthlySalesCard
import com.example.shoptracklite.utils.CurrencyUtils
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    repository: ShopTrackRepository,
    viewModel: ReportsViewModel = viewModel { ReportsViewModel(repository) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // Calculate daily metrics at the composable level for use in dialog
    val totalSales = uiState.selectedDateSales.size
    val totalRevenue = uiState.selectedDateSales.sumOf { it.totalAmount }
    val cogs = uiState.selectedDateCOGS
    val expenses = uiState.selectedDateExpenseTotal
    val grossProfit = uiState.selectedDateSales.sumOf { it.profit }
    val netProfit = grossProfit - expenses
    val totalItems = uiState.selectedDateSales.sumOf { it.quantitySold }
    val cashRevenue = uiState.selectedDateSales.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalAmount }
    val visaRevenue = uiState.selectedDateSales.filter { it.paymentMethod == PaymentMethod.VISA }.sumOf { it.totalAmount }
    val cashSalesCount = uiState.selectedDateSales.count { it.paymentMethod == PaymentMethod.CASH }
    val visaSalesCount = uiState.selectedDateSales.count { it.paymentMethod == PaymentMethod.VISA }
    
    val tabTitles = listOf("Daily", "Monthly")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with Share and Refresh buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reports",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Row {
                if (uiState.selectedTabIndex == 0) {
                    IconButton(onClick = { viewModel.showDatePicker() }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                }
                IconButton(onClick = { viewModel.showReportDialog() }) {
                    Icon(Icons.Default.Share, contentDescription = "Share Report")
                }
                IconButton(onClick = { viewModel.refreshReports() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        // Tab Row
        TabRow(
            selectedTabIndex = uiState.selectedTabIndex
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = uiState.selectedTabIndex == index,
                    onClick = { viewModel.setSelectedTab(index) },
                    text = { Text(title, fontWeight = if (uiState.selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content
        when (uiState.selectedTabIndex) {
            0 -> DailyReportContent(
                uiState = uiState,
                viewModel = viewModel,
                totalSales = totalSales,
                totalItems = totalItems,
                totalRevenue = totalRevenue,
                cogs = cogs,
                expenses = expenses,
                netProfit = netProfit,
                cashRevenue = cashRevenue,
                visaRevenue = visaRevenue,
                cashSalesCount = cashSalesCount,
                visaSalesCount = visaSalesCount
            )
            1 -> MonthlyReportContent(
                uiState = uiState,
                viewModel = viewModel
            )
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
    
    // Report Share Dialog
    if (uiState.showReportDialog) {
        val reportData = DailyReportData(
            selectedDate = uiState.selectedDate,
            sales = uiState.selectedDateSales,
            expenses = uiState.selectedDateExpenses,
            totalSales = totalSales,
            totalRevenue = totalRevenue,
            cogs = cogs,
            expensesTotal = expenses,
            grossProfit = grossProfit,
            netProfit = netProfit,
            cashRevenue = cashRevenue,
            visaRevenue = visaRevenue,
            cashSalesCount = cashSalesCount,
            visaSalesCount = visaSalesCount
        )
        
        DailyReportDialog(
            reportData = reportData,
            currencyCode = uiState.currencyCode,
            onDismiss = { viewModel.hideReportDialog() }
        )
    }
    
    // Cancel Sale Confirmation Dialog
    val saleToCancel = uiState.saleToCancel
    if (uiState.showCancelConfirmDialog && saleToCancel != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideCancelConfirmation() },
            title = {
                Text(
                    text = "Cancel Sale?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Are you sure you want to cancel this sale?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Product: ${saleToCancel.productName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Quantity: ${saleToCancel.quantitySold}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Amount: ${CurrencyUtils.formatCurrency(saleToCancel.totalAmount, uiState.currencyCode)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ This will restore ${saleToCancel.quantitySold} units to inventory and update all financial reports.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.cancelSale() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    )
                ) {
                    Text("Cancel Sale")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.hideCancelConfirmation() }) {
                    Text("Keep Sale")
                }
            }
        )
    }
    
    // Success/Error Messages
    val successMessage = uiState.successMessage
    val errorMessage = uiState.errorMessage
    
    LaunchedEffect(successMessage, errorMessage) {
        if (successMessage != null || errorMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }
    
    if (successMessage != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = Color(0xFF4CAF50)
        ) {
            Text(successMessage)
        }
    }
    
    if (errorMessage != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = Color(0xFFF44336)
        ) {
            Text(errorMessage)
        }
    }
}

@Composable
fun SaleItemCard(sale: Sale, currencyCode: String, onCancelClick: () -> Unit) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sale.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Qty: ${sale.quantitySold} × ${CurrencyUtils.formatCurrency(sale.unitPrice, currencyCode)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyUtils.formatCurrency(sale.totalAmount, currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Profit: ${CurrencyUtils.formatCurrency(sale.profit, currencyCode)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (sale.profit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Time: ${timeFormat.format(sale.saleDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Payment Method Badge
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (sale.paymentMethod) {
                                PaymentMethod.CASH -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                PaymentMethod.VISA -> Color(0xFF2196F3).copy(alpha = 0.1f)
                            }
                        )
                    ) {
                        Text(
                            text = sale.paymentMethod.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = when (sale.paymentMethod) {
                                PaymentMethod.CASH -> Color(0xFF4CAF50)
                                PaymentMethod.VISA -> Color(0xFF2196F3)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                // Cancel Button
                IconButton(
                    onClick = onCancelClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Sale",
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseDetailCard(expense: com.example.shoptracklite.data.Expense, currencyCode: String) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
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
                    text = "Time: ${timeFormat.format(expense.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = CurrencyUtils.formatCurrency(expense.amount, currencyCode),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun DailyReportContent(
    uiState: ReportsUiState,
    viewModel: ReportsViewModel,
    totalSales: Int,
    totalItems: Int,
    totalRevenue: Double,
    cogs: Double,
    expenses: Double,
    netProfit: Double,
    cashRevenue: Double,
    visaRevenue: Double,
    cashSalesCount: Int,
    visaSalesCount: Int
) {
    // Date Display
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val inputDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val displayDate = try {
        val date = inputDateFormatter.parse(uiState.selectedDate)
        dateFormatter.format(date ?: Date())
    } catch (e: Exception) {
        uiState.selectedDate
    }
    
    val grossProfit = totalRevenue - cogs
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Viewing: $displayDate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (uiState.selectedDateSales.isEmpty() && uiState.monthlySalesByDate.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "⚠️ No sales on this date!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "👇 Click a date below with sales data to view details",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else if (uiState.selectedDateSales.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "✅ Showing ${uiState.selectedDateSales.size} sales ($totalItems items)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }
        }

        // Profit Breakdown Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Profit Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Revenue", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = CurrencyUtils.formatCurrency(totalRevenue, uiState.currencyCode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "- Cost of Goods Sold", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = CurrencyUtils.formatCurrency(cogs, uiState.currencyCode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Gross Profit", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = CurrencyUtils.formatCurrency(grossProfit, uiState.currencyCode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (grossProfit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "- Expenses", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = CurrencyUtils.formatCurrency(expenses, uiState.currencyCode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Net Profit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = CurrencyUtils.formatCurrency(netProfit, uiState.currencyCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (netProfit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                }
            }
        }

        // Payment Method Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Payment Method Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cash Summary
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Cash",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                                Text(
                                    text = CurrencyUtils.formatCurrency(cashRevenue, uiState.currencyCode),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                                Text(
                                    text = "$cashSalesCount sales",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                        
                        // Visa Summary
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Visa",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                                Text(
                                    text = CurrencyUtils.formatCurrency(visaRevenue, uiState.currencyCode),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                                Text(
                                    text = "$visaSalesCount sales",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF2196F3)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Date Selection from Monthly Summary
        if (uiState.monthlySalesByDate.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.selectedDateSales.isEmpty()) 
                            MaterialTheme.colorScheme.errorContainer 
                        else 
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = if (uiState.selectedDateSales.isEmpty())
                                "👇 CLICK A DATE BELOW TO VIEW SALES 👇"
                            else
                                "📊 Quick Date Selection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.selectedDateSales.isEmpty())
                                MaterialTheme.colorScheme.onErrorContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (uiState.selectedDateSales.isEmpty()) {
                            Text(
                                text = "Your sales data is here! Just tap any date card below.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else {
                            Text(
                                text = "Select a different date to view its sales and expenses",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(uiState.monthlySalesByDate) { monthlySummary ->
                MonthlySalesCard(
                    summary = monthlySummary,
                    currencyCode = uiState.currencyCode,
                    isSelected = monthlySummary.date == uiState.selectedDate,
                    onClick = { viewModel.selectDate(monthlySummary.date) }
                )
            }
        }

        // Loading indicator
        if (uiState.isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        
        // Sales Details
        if (uiState.selectedDateSales.isNotEmpty()) {
            item {
                Text(
                    text = "Sales Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(uiState.selectedDateSales) { sale ->
                SaleItemCard(
                    sale = sale,
                    currencyCode = uiState.currencyCode,
                    onCancelClick = { viewModel.showCancelConfirmation(sale) }
                )
            }
        }
        
        // Expense Details Section
        if (uiState.selectedDateExpenses.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Expense Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(uiState.selectedDateExpenses) { expense ->
                ExpenseDetailCard(expense = expense, currencyCode = uiState.currencyCode)
            }
        }

        // Error Message
        uiState.errorMessage?.let { error ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Error Loading Data",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyReportContent(
    uiState: ReportsUiState,
    viewModel: ReportsViewModel
) {
    // Get current month name
    val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val currentMonth = monthFormatter.format(Date())
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📅 $currentMonth",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${uiState.monthlyTotalSales} sales (${uiState.monthlyTotalItems} items)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Profit Breakdown Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Profit Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Revenue", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = CurrencyUtils.formatCurrency(uiState.monthlyTotalRevenue, uiState.currencyCode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "- Cost of Goods Sold", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = CurrencyUtils.formatCurrency(uiState.monthlyTotalCOGS, uiState.currencyCode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Gross Profit", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = CurrencyUtils.formatCurrency(uiState.monthlyGrossProfit, uiState.currencyCode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.monthlyGrossProfit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "- Expenses", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = CurrencyUtils.formatCurrency(uiState.monthlyTotalExpenses, uiState.currencyCode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Net Profit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = CurrencyUtils.formatCurrency(uiState.monthlyNetProfit, uiState.currencyCode),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.monthlyNetProfit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                }
            }
        }

        // Payment Method Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Payment Method Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cash Summary
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Cash",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                                Text(
                                    text = CurrencyUtils.formatCurrency(uiState.monthlyCashRevenue, uiState.currencyCode),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                                Text(
                                    text = "${uiState.monthlyCashSalesCount} sales",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                        
                        // Visa Summary
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Visa",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                                Text(
                                    text = CurrencyUtils.formatCurrency(uiState.monthlyVisaRevenue, uiState.currencyCode),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                                Text(
                                    text = "${uiState.monthlyVisaSalesCount} sales",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF2196F3)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Daily breakdown header
        if (uiState.monthlySalesByDate.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Daily Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            items(uiState.monthlySalesByDate) { daySummary ->
                MonthlySalesCard(
                    summary = daySummary,
                    currencyCode = uiState.currencyCode,
                    isSelected = false,
                    onClick = { 
                        viewModel.selectDate(daySummary.date)
                        viewModel.setSelectedTab(0) // Switch to daily tab
                    }
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Sales This Month",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Record sales to see your monthly report here",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Error Message
        uiState.errorMessage?.let { error ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Error Loading Data",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
