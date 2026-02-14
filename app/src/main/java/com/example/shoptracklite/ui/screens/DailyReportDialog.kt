package com.example.shoptracklite.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.widget.ScrollView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.shoptracklite.data.Sale
import com.example.shoptracklite.utils.CurrencyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class DailyReportData(
    val selectedDate: String,
    val sales: List<Sale>,
    val expenses: List<com.example.shoptracklite.data.Expense>,
    val totalSales: Int,
    val totalRevenue: Double,
    val expensesTotal: Double,
    val profit: Double,
    val cashRevenue: Double,
    val visaRevenue: Double,
    val cashSalesCount: Int,
    val visaSalesCount: Int,
    // Cash Reconciliation
    val openingCash: Double = 0.0,
    val expectedCash: Double = 0.0,
    val actualCashCounted: Double = 0.0,
    val cashDifference: Double = 0.0,
    val changeForTomorrow: Double = 0.0,
    val cashToTakeOut: Double = 0.0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportDialog(
    reportData: DailyReportData,
    currencyCode: String,
    shopName: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reportView by remember { mutableStateOf<View?>(null) }
    
    // Display name: use shop name if set, otherwise default
    val displayShopName = shopName.ifBlank { "ShopTrack Lite" }
    
    fun shareReport() {
        scope.launch {
            try {
                val view = reportView
                if (view != null) {
                    // Measure full content size by scrolling to get total height
                    val scrollView = view as? ScrollView
                    val childView = scrollView?.getChildAt(0)
                    
                    val width = view.width
                    val height = if (childView != null) {
                        childView.height
                    } else {
                        view.height
                    }
                    
                    // Create bitmap with full content size
                    val bitmap = withContext(Dispatchers.Main) {
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        
                        if (childView != null) {
                            childView.draw(canvas)
                        } else {
                            view.draw(canvas)
                        }
                        bmp
                    }
                    
                    // Save bitmap to cache directory
                    withContext(Dispatchers.IO) {
                        val cachePath = File(context.cacheDir, "reports")
                        cachePath.mkdirs()
                        val file = File(cachePath, "report_${System.currentTimeMillis()}.png")
                        val fileOutputStream = FileOutputStream(file)
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
                        fileOutputStream.flush()
                        fileOutputStream.close()
                        
                        // Get URI for file
                        val contentUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        
                        // Create share intent
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/png"
                            putExtra(Intent.EXTRA_STREAM, contentUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        
                        withContext(Dispatchers.Main) {
                            context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
                        }
                    }
                } else {
                    Toast.makeText(context, "Report not ready yet. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to share report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Report Content (capturable) - Using ScrollView for full content capture
                AndroidView(
                    factory = { ctx ->
                        ScrollView(ctx).apply {
                            reportView = this
                            setBackgroundColor(android.graphics.Color.WHITE)
                            addView(ComposeView(ctx).apply {
                                setContent {
                                    ReportContent(
                                        reportData = reportData,
                                        currencyCode = currencyCode,
                                        displayShopName = displayShopName
                                    )
                                }
                            })
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                
                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Close")
                    }
                    
                    Button(
                        onClick = { shareReport() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportContent(
    reportData: DailyReportData,
    currencyCode: String,
    displayShopName: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with Shop Name
        Text(
            text = displayShopName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
        Text(
            text = "DAILY SALES REPORT",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        HorizontalDivider(color = Color.Black, thickness = 2.dp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Date
        val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        val inputDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDate = try {
            val date = inputDateFormatter.parse(reportData.selectedDate)
            dateFormatter.format(date ?: Date())
        } catch (e: Exception) {
            reportData.selectedDate
        }
        
        Text(
            text = displayDate,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        HorizontalDivider(color = Color.Gray, thickness = 1.dp)
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Main Metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total Sales",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Text(
                text = CurrencyUtils.formatCurrency(reportData.totalRevenue, currencyCode),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4CAF50)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Expenses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Text(
                text = "- " + CurrencyUtils.formatCurrency(reportData.expensesTotal, currencyCode),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF44336)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        HorizontalDivider(color = Color.Black, thickness = 2.dp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Profit Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profit",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = CurrencyUtils.formatCurrency(reportData.profit, currencyCode),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (reportData.profit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        HorizontalDivider(color = Color.Gray, thickness = 1.dp)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Payment Breakdown
        Text(
            text = "Payment Breakdown",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cash Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                    .background(Color(0xFF4CAF50).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CASH",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = CurrencyUtils.formatCurrency(reportData.cashRevenue, currencyCode),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${reportData.cashSalesCount} sales",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                }
            }
            
            // Card Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(2.dp, Color(0xFF2196F3), RoundedCornerShape(8.dp))
                    .background(Color(0xFF2196F3).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "CARD",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = CurrencyUtils.formatCurrency(reportData.visaRevenue, currencyCode),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${reportData.visaSalesCount} sales",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        HorizontalDivider(color = Color.Gray, thickness = 1.dp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Cash Reconciliation (only if user entered actual cash)
        if (reportData.actualCashCounted > 0) {
            Text(
                text = "Cash Reconciliation",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Opening Cash (if applicable)
            if (reportData.openingCash > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Opening Cash",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.DarkGray
                    )
                    Text(
                        text = CurrencyUtils.formatCurrency(reportData.openingCash, currencyCode),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2196F3)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Expected Cash",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.DarkGray
                )
                Text(
                    text = CurrencyUtils.formatCurrency(reportData.expectedCash, currencyCode),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Actual Cash Counted",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.DarkGray
                )
                Text(
                    text = CurrencyUtils.formatCurrency(reportData.actualCashCounted, currencyCode),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val differenceColor = when {
                reportData.cashDifference > 0 -> Color(0xFF4CAF50)
                reportData.cashDifference < 0 -> Color(0xFFF44336)
                else -> Color.Black
            }
            val differenceText = when {
                reportData.cashDifference > 0 -> "Over"
                reportData.cashDifference < 0 -> "Short"
                else -> "Exact"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Difference ($differenceText)",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = differenceColor
                )
                Text(
                    text = CurrencyUtils.formatCurrency(kotlin.math.abs(reportData.cashDifference), currencyCode),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = differenceColor
                )
            }
            
            if (reportData.changeForTomorrow > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.LightGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Change for Tomorrow",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.DarkGray
                    )
                    Text(
                        text = CurrencyUtils.formatCurrency(reportData.changeForTomorrow, currencyCode),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cash to Take Out",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = CurrencyUtils.formatCurrency(reportData.cashToTakeOut, currencyCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            HorizontalDivider(color = Color.Gray, thickness = 1.dp)
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Expense Breakdown
        if (reportData.expenses.isNotEmpty()) {
            Text(
                text = "Expense Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            reportData.expenses.forEach { expense ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = expense.description,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray
                        )
                    }
                    Text(
                        text = CurrencyUtils.formatCurrency(expense.amount, currencyCode),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            HorizontalDivider(color = Color.Gray, thickness = 1.dp)
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Footer
        Text(
            text = "Generated by ShopTrack Lite",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        val timestamp = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            .format(Date())
        Text(
            text = timestamp,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

