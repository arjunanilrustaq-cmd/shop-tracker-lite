package com.example.shoptracklite.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.view.drawToBitmap
import android.widget.Toast
import android.view.View
import com.example.shoptracklite.data.PaymentMethod
import com.example.shoptracklite.data.Sale
import com.example.shoptracklite.utils.CurrencyUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class DailyReportData(
    val selectedDate: String,
    val sales: List<Sale>,
    val expenses: List<com.example.shoptracklite.data.Expense>,
    val totalSales: Int,
    val totalRevenue: Double,
    val cogs: Double,
    val expensesTotal: Double,
    val grossProfit: Double,
    val netProfit: Double,
    val cashRevenue: Double,
    val visaRevenue: Double,
    val cashSalesCount: Int,
    val visaSalesCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportDialog(
    reportData: DailyReportData,
    currencyCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reportView by remember { mutableStateOf<View?>(null) }
    
    fun shareReport() {
        scope.launch {
            try {
                val view = reportView
                if (view != null) {
                    // Capture the view as bitmap
                    val bitmap = view.drawToBitmap()
                    
                    // Save bitmap to cache directory
                    val cachePath = File(context.cacheDir, "reports")
                    cachePath.mkdirs()
                    val file = File(cachePath, "report_${System.currentTimeMillis()}.jpg")
                    val fileOutputStream = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fileOutputStream)
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
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    
                    context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
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
                // Report Content (capturable)
                AndroidView(
                    factory = { ctx ->
                        ComposeView(ctx).apply {
                            reportView = this
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { composeView ->
                    composeView.setContent {
                        MaterialTheme {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                item {
                                    // Header
                                    Text(
                                        text = "ShopTrack Lite",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "DAILY SALES REPORT",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Divider(color = Color.Black, thickness = 2.dp)
                                    
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
                                    
                                    Divider(color = Color.Gray, thickness = 1.dp)
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                                
                                // Main Metrics
                                item {
                                    // Revenue Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Revenue",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.DarkGray
                                        )
                                        Text(
                                            text = CurrencyUtils.formatCurrency(
                                                reportData.totalRevenue,
                                                currencyCode
                                            ),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // COGS Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "COGS (Cost of Goods)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.DarkGray
                                        )
                                        Text(
                                            text = "- " + CurrencyUtils.formatCurrency(
                                                reportData.cogs,
                                                currencyCode
                                            ),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF9800)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Gross Profit Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Gross Profit",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.DarkGray
                                        )
                                        Text(
                                            text = CurrencyUtils.formatCurrency(
                                                reportData.grossProfit,
                                                currencyCode
                                            ),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2196F3)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Expenses Row
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
                                            text = "- " + CurrencyUtils.formatCurrency(
                                                reportData.expensesTotal,
                                                currencyCode
                                            ),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF44336)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Divider(color = Color.Black, thickness = 2.dp)
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Net Profit Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Net Profit",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = CurrencyUtils.formatCurrency(
                                                reportData.netProfit,
                                                currencyCode
                                            ),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (reportData.netProfit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    Divider(color = Color.Gray, thickness = 1.dp)
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                }
                                
                                // Payment Method Breakdown
                                item {
                                    Text(
                                        text = "Payment Method Breakdown",
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
                                                    text = CurrencyUtils.formatCurrency(
                                                        reportData.cashRevenue,
                                                        currencyCode
                                                    ),
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
                                        
                                        // Visa Box
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
                                                    text = "VISA",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2196F3)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = CurrencyUtils.formatCurrency(
                                                        reportData.visaRevenue,
                                                        currencyCode
                                                    ),
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
                                    
                                    Divider(color = Color.Gray, thickness = 1.dp)
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                // Expense Breakdown
                                if (reportData.expenses.isNotEmpty()) {
                                    item {
                                        Text(
                                            text = "Expense Breakdown",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                    
                                    reportData.expenses.forEach { expense ->
                                        item {
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
                                                    text = CurrencyUtils.formatCurrency(
                                                        expense.amount,
                                                        currencyCode
                                                    ),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFF44336)
                                                )
                                            }
                                        }
                                    }
                                    
                                    item {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Divider(color = Color.Gray, thickness = 1.dp)
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                                
                                // Footer
                                item {
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
                        }
                    }
                }
                
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

