package com.example.shoptracklite.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.example.shoptracklite.viewmodel.ReportType
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
    
    // Group sales by bill (transactionId); null = each sale is its own bill
    val groupedBills = uiState.selectedDateSales
        .groupBy { it.transactionId ?: it.id.toLong() }
        .values
        .map { it.sortedBy { s -> s.saleDate.time } }
        .sortedByDescending { it.first().saleDate.time }
    // Calculate daily metrics (bill-wise: totalSales = number of bills)
    val totalSales = groupedBills.size
    val totalRevenue = uiState.selectedDateSales.sumOf { it.totalAmount }
    val expenses = uiState.selectedDateExpenseTotal
    val profit = totalRevenue - expenses
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

        Spacer(modifier = Modifier.height(12.dp))
        
        // Report Type Toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = uiState.reportType == ReportType.ACCOUNTING,
                    onClick = { viewModel.setReportType(ReportType.ACCOUNTING) },
                    label = { Text("Profit Breakdown") },
                    leadingIcon = if (uiState.reportType == ReportType.ACCOUNTING) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = uiState.reportType == ReportType.CASH_FLOW,
                    onClick = { viewModel.setReportType(ReportType.CASH_FLOW) },
                    label = { Text("Cash Flow") },
                    leadingIcon = if (uiState.reportType == ReportType.CASH_FLOW) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        when (uiState.selectedTabIndex) {
            0 -> DailyReportContent(
                uiState = uiState,
                viewModel = viewModel,
                totalSales = totalSales,
                totalItems = totalItems,
                totalRevenue = totalRevenue,
                expenses = expenses,
                profit = profit,
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
        val expectedCash = viewModel.getExpectedCash()
        val actualCash = uiState.actualCashCounted.toDoubleOrNull() ?: 0.0
        val cashDifference = viewModel.getCashDifference()
        val changeForTomorrow = uiState.changeForTomorrow.toDoubleOrNull() ?: 0.0
        val cashToTakeOut = viewModel.getCashToTakeOut()
        
        val reportData = DailyReportData(
            selectedDate = uiState.selectedDate,
            sales = uiState.selectedDateSales,
            expenses = uiState.selectedDateExpenses,
            totalSales = totalSales,
            totalRevenue = totalRevenue,
            expensesTotal = expenses,
            profit = profit,
            cashRevenue = cashRevenue,
            visaRevenue = visaRevenue,
            cashSalesCount = cashSalesCount,
            visaSalesCount = visaSalesCount,
            openingCash = uiState.openingCash.toDoubleOrNull() ?: 0.0,
            expectedCash = expectedCash,
            actualCashCounted = actualCash,
            cashDifference = cashDifference,
            changeForTomorrow = changeForTomorrow,
            cashToTakeOut = cashToTakeOut
        )
        
        DailyReportDialog(
            reportData = reportData,
            currencyCode = uiState.currencyCode,
            shopName = uiState.shopName,
            onDismiss = { viewModel.hideReportDialog() }
        )
    }
    
    // Bill Receipt Dialog (shareable)
    uiState.billForReceipt?.let { billSales ->
        BillReceiptDialog(
            sales = billSales,
            shopName = uiState.shopName,
            crNumber = uiState.crNumber,
            currencyCode = uiState.currencyCode,
            onDismiss = { viewModel.hideBillReceipt() }
        )
    }
    
    // Cancel Bill Confirmation Dialog
    val billToCancel = uiState.billToCancel
    if (uiState.showCancelConfirmDialog && billToCancel != null) {
        val billTotal = billToCancel.sumOf { it.totalAmount }
        val totalItemsToRestore = billToCancel.sumOf { it.quantitySold }
        AlertDialog(
            onDismissRequest = { viewModel.hideCancelConfirmation() },
            title = {
                Text(
                    text = "Cancel Bill?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text("Are you sure you want to cancel this bill?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${billToCancel.size} item(s), Total: ${CurrencyUtils.formatCurrency(billTotal, uiState.currencyCode)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will restore $totalItemsToRestore units to inventory and update all financial reports.",
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
                    Text("Cancel Bill")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.hideCancelConfirmation() }) {
                    Text("Keep Bill")
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
fun SaleBillCard(
    sales: List<Sale>,
    currencyCode: String,
    onBillClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val firstSale = sales.first()
    val billTotal = sales.sumOf { it.totalAmount }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBillClick() }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bill - ${timeFormat.format(firstSale.saleDate)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${sales.size} item(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = when (firstSale.paymentMethod) {
                                PaymentMethod.CASH -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                PaymentMethod.VISA -> Color(0xFF2196F3).copy(alpha = 0.1f)
                            }
                        )
                    ) {
                        Text(
                            text = firstSale.paymentMethod.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = when (firstSale.paymentMethod) {
                                PaymentMethod.CASH -> Color(0xFF4CAF50)
                                PaymentMethod.VISA -> Color(0xFF2196F3)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = CurrencyUtils.formatCurrency(billTotal, currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = onCancelClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Bill",
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportContent(
    uiState: ReportsUiState,
    viewModel: ReportsViewModel,
    totalSales: Int,
    totalItems: Int,
    totalRevenue: Double,
    expenses: Double,
    profit: Double,
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
    
    // Cash reconciliation values
    val expectedCash = viewModel.getExpectedCash()
    val cashDifference = viewModel.getCashDifference()
    val cashToTakeOut = viewModel.getCashToTakeOut()
    val hasActualCash = uiState.actualCashCounted.isNotBlank()
    
    // Group sales by bill (transactionId); null = each sale is its own bill
    val groupedBills = uiState.selectedDateSales
        .groupBy { it.transactionId ?: it.id.toLong() }
        .values
        .map { it.sortedBy { s -> s.saleDate.time } }
        .sortedByDescending { it.first().saleDate.time }
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date Header
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
                            text = "No sales on this date",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else if (uiState.selectedDateSales.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$totalSales bills ($totalItems items)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Summary Card - changes based on report type
        item {
            val dailyCOGS = viewModel.getDailyCOGS()
            val dailyOperatingExpenses = viewModel.getDailyOperatingExpenses()
            val grossProfit = totalRevenue - dailyCOGS
            val netProfit = grossProfit - dailyOperatingExpenses
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (uiState.reportType == ReportType.CASH_FLOW) "Cash Flow" else "Profit Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (uiState.reportType == ReportType.CASH_FLOW) {
                        // Cash Flow View - Opening Cash, Cash Sales, Visa Sales, Cash Out, Closing Cash
                        val openingCashValue = uiState.openingCash.toDoubleOrNull() ?: 0.0
                        val closingCash = openingCashValue + totalRevenue - expenses
                        
                        // Opening Cash
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Opening Cash", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = CurrencyUtils.formatCurrency(openingCashValue, uiState.currencyCode),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Cash Sales
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Cash Sales", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = CurrencyUtils.formatCurrency(cashRevenue, uiState.currencyCode),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Visa Sales
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Visa Sales", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = CurrencyUtils.formatCurrency(visaRevenue, uiState.currencyCode),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Cash Out
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "- Cash Out (Expenses)", style = MaterialTheme.typography.bodyMedium)
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
                        
                        // Closing Cash
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Closing Cash",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = CurrencyUtils.formatCurrency(closingCash, uiState.currencyCode),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (closingCash >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                    } else {
                        // Accounting View - Detailed with COGS
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
                                text = CurrencyUtils.formatCurrency(dailyCOGS, uiState.currencyCode),
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
                                text = "Gross Profit",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
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
                                text = CurrencyUtils.formatCurrency(dailyOperatingExpenses, uiState.currencyCode),
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
                        text = "Payment Breakdown",
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
                        
                        // Card Summary
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
                                    text = "Card",
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

        // Cash Reconciliation Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cash Reconciliation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (uiState.reconciliationSaved) {
                            Text(
                                text = "✓ Saved",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Opening Cash - Editable on first day, read-only afterwards
                    if (uiState.hasPreviousReconciliation) {
                        // Has previous day record - show read-only opening cash
                        val openingCashValue = uiState.openingCash.toDoubleOrNull() ?: 0.0
                        if (openingCashValue > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Opening Cash",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = CurrencyUtils.formatCurrency(openingCashValue, uiState.currencyCode),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2196F3)
                                )
                            }
                            Text(
                                text = "(From previous day's change)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else {
                        // First day - show editable opening cash input
                        OutlinedTextField(
                            value = uiState.openingCash,
                            onValueChange = { viewModel.updateOpeningCash(it) },
                            label = { Text("Opening Cash") },
                            placeholder = { Text("Enter cash in register") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        Text(
                            text = "(Cash already in register when you started)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Expected Cash (read-only)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Expected Cash",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = CurrencyUtils.formatCurrency(expectedCash, uiState.currencyCode),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        text = if (uiState.openingCash.isNotBlank() && (uiState.openingCash.toDoubleOrNull() ?: 0.0) > 0) 
                            "(Opening Cash + Cash Sales - Expenses)" else "(Cash Sales - Expenses)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Actual Cash Input
                    OutlinedTextField(
                        value = uiState.actualCashCounted,
                        onValueChange = { viewModel.updateActualCash(it) },
                        label = { Text("Actual Cash Counted") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    
                    // Show difference only if actual cash is entered
                    if (hasActualCash) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val differenceColor = when {
                            cashDifference > 0 -> Color(0xFF4CAF50) // Over - green
                            cashDifference < 0 -> Color(0xFFF44336) // Short - red
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        }
                        val differenceText = when {
                            cashDifference > 0 -> "Over"
                            cashDifference < 0 -> "Short"
                            else -> "Exact"
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Difference ($differenceText)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = differenceColor
                            )
                            Text(
                                text = CurrencyUtils.formatCurrency(kotlin.math.abs(cashDifference), uiState.currencyCode),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = differenceColor
                            )
                        }
                        // Notes field when there's extra cash (Over) - e.g. added for change
                        if (cashDifference > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = uiState.reconciliationNotes,
                                onValueChange = { viewModel.updateReconciliationNotes(it) },
                                label = { Text("Notes (extra cash for change?)") },
                                placeholder = { Text("e.g. Added cash for change") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Change for Tomorrow Input
                    OutlinedTextField(
                        value = uiState.changeForTomorrow,
                        onValueChange = { viewModel.updateChangeForTomorrow(it) },
                        label = { Text("Change for Tomorrow") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    
                    // Show cash to take out only if both values are entered
                    if (hasActualCash && uiState.changeForTomorrow.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cash to Take Out",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = CurrencyUtils.formatCurrency(cashToTakeOut, uiState.currencyCode),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                    
                    // Save Button
                    if (hasActualCash) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { viewModel.saveCashReconciliation() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.reconciliationSaved
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (uiState.reconciliationSaved) "Saved" else "Save Reconciliation")
                        }
                    }
                }
            }
        }

        // Quick Date Selection from Monthly Summary
        if (uiState.monthlySalesByDate.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Other Dates",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
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
        
        // Sales Details (Bill-wise)
        if (groupedBills.isNotEmpty()) {
            item {
                Text(
                    text = "Sales Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(groupedBills) { billSales ->
                SaleBillCard(
                    sales = billSales,
                    currencyCode = uiState.currencyCode,
                    onBillClick = { viewModel.showBillReceipt(billSales) },
                    onCancelClick = { viewModel.showCancelBillConfirmation(billSales) }
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
    // Parse selected year-month and format for display
    val inputFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val outputFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val displayMonth = try {
        val date = inputFormatter.parse(uiState.selectedYearMonth)
        outputFormatter.format(date ?: Date())
    } catch (e: Exception) {
        uiState.selectedYearMonth
    }
    
    val isCurrentMonth = viewModel.isCurrentMonth()
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Header with Navigation
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
                    // Month Navigation Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.goToPreviousMonth() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Text(
                            text = "📅 $displayMonth",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        IconButton(
                            onClick = { viewModel.goToNextMonth() },
                            enabled = !isCurrentMonth
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = if (isCurrentMonth) 
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f)
                                else 
                                    MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Text(
                        text = "${uiState.monthlyTotalSales} sales (${uiState.monthlyTotalItems} items)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Monthly Summary Card - changes based on report type
        item {
            val monthlyCOGS = viewModel.getMonthlyCOGS()
            val monthlyOperatingExpenses = viewModel.getMonthlyOperatingExpenses()
            val grossProfit = uiState.monthlyTotalRevenue - monthlyCOGS
            val netProfit = grossProfit - monthlyOperatingExpenses
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = if (uiState.reportType == ReportType.CASH_FLOW) "Cash Flow" else "Profit Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (uiState.reportType == ReportType.CASH_FLOW) {
                        // Cash Flow View - Opening Cash, Cash Sales, Visa Sales, Cash Out, Closing Cash
                        val monthlyClosingCash = uiState.monthlyOpeningCash + uiState.monthlyTotalRevenue - uiState.monthlyTotalExpenses
                        
                        // Opening Cash
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Opening Cash", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = CurrencyUtils.formatCurrency(uiState.monthlyOpeningCash, uiState.currencyCode),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Cash Sales
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Cash Sales", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = CurrencyUtils.formatCurrency(uiState.monthlyCashRevenue, uiState.currencyCode),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Visa Sales
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Visa Sales", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = CurrencyUtils.formatCurrency(uiState.monthlyVisaRevenue, uiState.currencyCode),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2196F3)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Cash Out
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "- Cash Out (Expenses)", style = MaterialTheme.typography.bodyMedium)
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
                        
                        // Closing Cash
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Closing Cash",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = CurrencyUtils.formatCurrency(monthlyClosingCash, uiState.currencyCode),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (monthlyClosingCash >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                    } else {
                        // Accounting View - Detailed with COGS
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
                                text = CurrencyUtils.formatCurrency(monthlyCOGS, uiState.currencyCode),
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
                                text = "Gross Profit",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
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
                                text = CurrencyUtils.formatCurrency(monthlyOperatingExpenses, uiState.currencyCode),
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
        }

        // Payment Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Payment Breakdown",
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
                        
                        // Card Summary
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
                                    text = "Card",
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
