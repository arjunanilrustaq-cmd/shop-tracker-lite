package com.example.shoptracklite.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.core.view.drawToBitmap
import com.example.shoptracklite.data.Product
import com.example.shoptracklite.utils.CurrencyUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InventoryShareDialog(
    products: List<Product>,
    currencyCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var inventoryView by remember { mutableStateOf<android.view.View?>(null) }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Inventory Stock Report",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Content with AndroidView wrapper for capturing
                AndroidView(
                    factory = { ctx ->
                        ComposeView(ctx).apply {
                            setContent {
                                MaterialTheme {
                                    InventoryReportContent(products, currencyCode)
                                }
                            }
                            post {
                                inventoryView = this
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // Bottom buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Close")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                shareInventory(context, inventoryView)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share")
                    }
                }
            }
        }
    }
}

@Composable
fun InventoryReportContent(products: List<Product>, currencyCode: String) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    val currentDate = dateFormat.format(Date())
    
    // Calculate totals
    val totalItems = products.size
    val totalStock = products.sumOf { it.quantityInStock }
    val totalValue = products.sumOf { it.quantityInStock * it.sellingPrice }
    val lowStockCount = products.count { it.quantityInStock < 10 }
    val outOfStockCount = products.count { it.quantityInStock == 0 }

    // Use LazyColumn instead of Column to allow scrolling but in capture mode, all items will be visible
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Store info and date
        Text(
            text = "ShopTrack Lite",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black
        )
        Text(
            text = currentDate,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            color = Color.DarkGray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Divider(thickness = 2.dp, color = Color.Black)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Summary cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(
                title = "Total Items",
                value = "$totalItems",
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Total Stock",
                value = "$totalStock",
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard(
                title = "Total Value",
                value = CurrencyUtils.formatCurrency(totalValue, currencyCode),
                modifier = Modifier.weight(1f)
            )
            SummaryCard(
                title = "Low Stock",
                value = "$lowStockCount",
                textColor = if (lowStockCount > 0) Color.Red else Color.Black,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Table header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0E0E0))
                .border(1.dp, Color.Black)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Product",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(2f),
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                color = Color.Black
            )
            Text(
                text = "Stock",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                color = Color.Black
            )
            Text(
                text = "Value",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1.5f),
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                color = Color.Black
            )
        }
        
        // Table rows - Using Column instead of LazyColumn to capture all items
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            products.forEachIndexed { index, product ->
                val backgroundColor = if (index % 2 == 0) Color.White else Color(0xFFF5F5F5)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(backgroundColor)
                        .border(0.5.dp, Color.Gray)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(2f)) {
                        Text(
                            text = product.name,
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                        if (product.barcode != null) {
                            Text(
                                text = "Barcode: ${product.barcode}",
                                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                                color = Color.DarkGray
                            )
                        }
                    }
                    Text(
                        text = "${product.quantityInStock}",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        color = if (product.quantityInStock == 0) Color.Red 
                               else if (product.quantityInStock < 10) Color(0xFFFF6F00)
                               else Color.Black
                    )
                    Text(
                        text = CurrencyUtils.formatCurrency(product.quantityInStock * product.sellingPrice, currencyCode),
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.5f),
                        fontSize = MaterialTheme.typography.bodySmall.fontSize,
                        color = Color.Black
                    )
                }
            }
        }
        
        // Footer
        Divider(thickness = 2.dp, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE8F5E9))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "TOTAL INVENTORY VALUE",
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                color = Color.Black
            )
            Text(
                text = CurrencyUtils.formatCurrency(totalValue, currencyCode),
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                color = Color.Black
            )
        }
        
        if (outOfStockCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "⚠ $outOfStockCount item(s) out of stock",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    textColor: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun shareInventory(context: Context, view: android.view.View?) {
    try {
        if (view == null) {
            Toast.makeText(context, "Report not ready yet. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }

        // Capture the view as bitmap
        val bitmap = view.drawToBitmap()

        // Save bitmap to cache directory
        val cachePath = File(context.cacheDir, "inventory")
        cachePath.mkdirs()
        val file = File(cachePath, "inventory_report_${System.currentTimeMillis()}.jpg")
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
            putExtra(Intent.EXTRA_SUBJECT, "Inventory Stock Report")
            putExtra(Intent.EXTRA_TEXT, "Inventory Report from ShopTrack Lite")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Share Inventory Report"))
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share inventory: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

