package com.example.shoptracklite.ui.screens

import android.content.Intent
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
import com.example.shoptracklite.data.Sale
import com.example.shoptracklite.utils.CurrencyUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillReceiptDialog(
    sales: List<Sale>,
    shopName: String,
    crNumber: String,
    currencyCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var receiptView by remember { mutableStateOf<View?>(null) }
    val firstSale = sales.firstOrNull() ?: return
    val billTotal = sales.sumOf { it.totalAmount }
    val receiptTimestamp = firstSale.saleDate.time

    fun shareReceipt() {
        scope.launch {
            try {
                val view = receiptView
                if (view != null) {
                    val bitmap = view.drawToBitmap()
                    val cachePath = File(context.cacheDir, "receipts")
                    cachePath.mkdirs()
                    val file = File(cachePath, "bill_receipt_${System.currentTimeMillis()}.jpg")
                    val fileOutputStream = FileOutputStream(file)
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, fileOutputStream)
                    fileOutputStream.flush()
                    fileOutputStream.close()
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
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
            Column(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        ComposeView(ctx).apply { receiptView = this }
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
                                    val displayShopName = shopName.ifBlank { "ShopTrack Lite" }
                                    Text(
                                        text = displayShopName,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    if (crNumber.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "CR No: $crNumber",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.DarkGray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "RECEIPT",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color.Black, thickness = 2.dp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                    Text(
                                        text = dateFormat.format(firstSale.saleDate),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color.Gray, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                items(sales) { sale ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = sale.productName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            Text(
                                                text = "${sale.quantitySold} x ${CurrencyUtils.formatCurrency(sale.unitPrice, currencyCode)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.DarkGray
                                            )
                                        }
                                        Text(
                                            text = CurrencyUtils.formatCurrency(sale.totalAmount, currencyCode),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    HorizontalDivider(color = Color.Black, thickness = 2.dp)
                                    Spacer(modifier = Modifier.height(16.dp))
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
                                            text = CurrencyUtils.formatCurrency(billTotal, currencyCode),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Payment Method: ${firstSale.paymentMethod.name}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    HorizontalDivider(color = Color.Gray, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(16.dp))
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
                                        text = "Receipt #$receiptTimestamp",
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
