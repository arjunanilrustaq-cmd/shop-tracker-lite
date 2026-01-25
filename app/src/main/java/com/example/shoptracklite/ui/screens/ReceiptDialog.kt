package com.example.shoptracklite.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import android.widget.Toast
import androidx.core.view.drawToBitmap
import com.example.shoptracklite.viewmodel.ReceiptData
import com.example.shoptracklite.utils.CurrencyUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDialog(
    receiptData: ReceiptData,
    currencyCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var receiptView by remember { mutableStateOf<View?>(null) }
    
    fun shareReceipt() {
        scope.launch {
            try {
                val view = receiptView
                if (view != null) {
                    // Capture the view as bitmap
                    val bitmap = view.drawToBitmap()
                    
                    // Save bitmap to cache directory
                    val cachePath = File(context.cacheDir, "receipts")
                    cachePath.mkdirs()
                    val file = File(cachePath, "receipt_${System.currentTimeMillis()}.jpg")
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
                    
                    context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
                } else {
                    Toast.makeText(context, "Receipt not ready yet. Please try again.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to share receipt: ${e.message}", Toast.LENGTH_SHORT).show()
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
                // Receipt Content (capturable)
                AndroidView(
                    factory = { ctx ->
                        ComposeView(ctx).apply {
                            receiptView = this
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
                                text = if (receiptData.isWholesale) "WHOLESALE RECEIPT" else "RECEIPT",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (receiptData.isWholesale) Color(0xFF1976D2) else Color.Black
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Divider(color = Color.Black, thickness = 2.dp)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Date and Time
                            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            val dateStr = dateFormat.format(Date(receiptData.timestamp))
                            
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                            
                            // Customer Name (if provided)
                            receiptData.customerName?.let { name ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Customer: $name",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                            
                            // Show retail total for wholesale receipts
                            if (receiptData.isWholesale) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFE3F2FD)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Retail Value:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = CurrencyUtils.formatCurrency(
                                                receiptData.items.sumOf { it.totalAmount },
                                                currencyCode
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Divider(color = Color.Gray, thickness = 1.dp)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        // Items
                        items(receiptData.items) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    val priceToShow = if (receiptData.isWholesale) {
                                        item.wholesalePrice ?: item.unitPrice
                                    } else {
                                        item.unitPrice
                                    }
                                    Text(
                                        text = "${item.quantity} x ${CurrencyUtils.formatCurrency(priceToShow, currencyCode)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.DarkGray
                                    )
                                }
                                val totalToShow = if (receiptData.isWholesale) {
                                    item.wholesaleTotalAmount ?: item.totalAmount
                                } else {
                                    item.totalAmount
                                }
                                Text(
                                    text = CurrencyUtils.formatCurrency(totalToShow, currencyCode),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Divider(color = Color.Black, thickness = 2.dp)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Subtotal (if there's a discount)
                            if (receiptData.discount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Subtotal:",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = CurrencyUtils.formatCurrency(receiptData.subtotal, currencyCode),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.Black
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                // Discount
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Discount:",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Text(
                                        text = "-${CurrencyUtils.formatCurrency(receiptData.discount, currencyCode)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            // Total
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOTAL:",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = CurrencyUtils.formatCurrency(receiptData.total, currencyCode),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Payment Method
                            Text(
                                text = "Payment Method: ${receiptData.paymentMethod.name}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Black
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Divider(color = Color.Gray, thickness = 1.dp)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Footer
                            Text(
                                text = "Thank you for your purchase!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.Black,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Receipt #${receiptData.timestamp}",
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
                        onClick = { shareReceipt() },
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

