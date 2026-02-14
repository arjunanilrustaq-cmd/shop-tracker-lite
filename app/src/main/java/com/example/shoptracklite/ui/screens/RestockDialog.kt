package com.example.shoptracklite.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shoptracklite.data.Product
import com.example.shoptracklite.ui.components.BluetoothBarcodeDetector
import java.text.NumberFormat
import java.util.*

@Composable
fun RestockDialog(
    products: List<Product>,
    onRestock: (Product, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var restockQuantity by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Filter products based on search - only show products that track inventory
    val filteredProducts = remember(products, searchQuery) {
        val trackableProducts = products.filter { it.trackInventory }
        if (searchQuery.isBlank()) {
            trackableProducts
        } else {
            trackableProducts.filter { product ->
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.barcode?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Bluetooth barcode reader support
        BluetoothBarcodeDetector(
            enabled = !showBarcodeScanner && selectedProduct == null,
            onBarcodeScanned = { barcode ->
                // Search for product with this barcode
                val product = products.find { it.barcode == barcode && it.trackInventory }
                if (product != null) {
                    selectedProduct = product
                    restockQuantity = ""
                } else {
                    Toast.makeText(
                        context,
                        "No product found with barcode: $barcode",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Restock Items",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Search bar with barcode scanner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search by name or barcode") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    FloatingActionButton(
                        onClick = { showBarcodeScanner = true },
                        modifier = Modifier.size(56.dp),
                        containerColor = MaterialTheme.colorScheme.secondary
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Scan Barcode"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Product list
                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "No products in inventory" else "No products found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredProducts) { product ->
                            RestockProductCard(
                                product = product,
                                onClick = {
                                    selectedProduct = product
                                    restockQuantity = ""
                                }
                            )
                        }
                    }
                }

                // Bottom button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Close")
                }
            }
        }
        } // End BluetoothBarcodeDetector
    }

    // Barcode scanner dialog
    if (showBarcodeScanner) {
        com.example.shoptracklite.ui.components.BarcodeScannerDialog(
            onBarcodeScanned = { barcode ->
                // Search for product with this barcode
                val product = products.find { it.barcode == barcode && it.trackInventory }
                if (product != null) {
                    selectedProduct = product
                    restockQuantity = ""
                    showBarcodeScanner = false
                } else {
                    Toast.makeText(
                        context,
                        "No product found with barcode: $barcode",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    // Restock quantity dialog
    if (selectedProduct != null) {
        AlertDialog(
            onDismissRequest = { selectedProduct = null },
            title = { Text("Restock ${selectedProduct!!.name}") },
            text = {
                Column {
                    Text(
                        text = "Current stock: ${selectedProduct!!.quantityInStock}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = restockQuantity,
                        onValueChange = { 
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                restockQuantity = it
                            }
                        },
                        label = { Text("Quantity to add") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Enter quantity") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val quantity = restockQuantity.toIntOrNull()
                        if (quantity != null && quantity > 0) {
                            onRestock(selectedProduct!!, quantity)
                            Toast.makeText(
                                context,
                                "Added $quantity units to ${selectedProduct!!.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                            selectedProduct = null
                            restockQuantity = ""
                        } else {
                            Toast.makeText(
                                context,
                                "Please enter a valid quantity",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = restockQuantity.toIntOrNull()?.let { it > 0 } ?: false
                ) {
                    Text("Add Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProduct = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RestockProductCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Stock: ${product.quantityInStock}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (product.quantityInStock > 10)
                            MaterialTheme.colorScheme.primary
                        else if (product.quantityInStock > 0)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(product.sellingPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (product.barcode != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Barcode: ${product.barcode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Stock",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

