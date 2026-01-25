package com.example.shoptracklite.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shoptracklite.data.Product
import com.example.shoptracklite.data.ShopTrackRepository
import com.example.shoptracklite.viewmodel.InventoryViewModel
import com.example.shoptracklite.utils.CurrencyUtils
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    repository: ShopTrackRepository,
    viewModel: InventoryViewModel = viewModel { InventoryViewModel(repository) }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBarcodeScanner by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Inventory",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { viewModel.showShareDialog() }) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share Inventory",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                FloatingActionButton(
                    onClick = { viewModel.showAddDialog() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search bar with barcode scanner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                label = { Text("Search products...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
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
                    Icons.Default.QrCodeScanner,
                    contentDescription = "Scan Barcode"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error Message
        uiState.errorMessage?.let { error ->
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
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.products.isEmpty()) {
            // Empty State
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = "No Products",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No products in inventory",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the + button to add your first product",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (uiState.filteredProducts.isEmpty() && uiState.searchQuery.isNotEmpty()) {
            // No search results
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "No Results",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No products found",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try a different search term",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Product List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.filteredProducts) { product ->
                    ProductCard(
                        product = product,
                        currencyCode = uiState.currencyCode,
                        onEdit = { viewModel.showEditDialog(product) },
                        onDelete = { viewModel.showDeleteConfirmation(product) }
                    )
                }
            }
            
            // Restock Button at the bottom
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.showRestockDialog() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Restock Items",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    // Barcode scanner dialog
    if (showBarcodeScanner) {
        com.example.shoptracklite.ui.components.BarcodeScannerDialog(
            onBarcodeScanned = { barcode ->
                viewModel.scanBarcodeForDetails(barcode)
                showBarcodeScanner = false
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    // Add Product Dialog
    if (uiState.showAddDialog) {
        ProductDialog(
            product = null,
            wholesaleModeEnabled = uiState.wholesaleModeEnabled,
            onDismiss = { viewModel.hideAddDialog() },
            onSave = { product, priceRanges -> viewModel.addProduct(product, priceRanges) }
        )
    }

    // Edit Product Dialog
    if (uiState.showEditDialog && uiState.selectedProduct != null) {
        ProductDialog(
            product = uiState.selectedProduct,
            wholesaleModeEnabled = uiState.wholesaleModeEnabled,
            onDismiss = { viewModel.hideEditDialog() },
            onSave = { product, priceRanges -> viewModel.updateProduct(product, priceRanges) }
        )
    }

    // Delete Confirmation Dialog
    if (uiState.showDeleteConfirmation && uiState.productToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Delete Product") },
            text = { 
                Text("Are you sure you want to delete \"${uiState.productToDelete!!.name}\"? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Share Inventory Dialog
    if (uiState.showShareDialog) {
        InventoryShareDialog(
            products = uiState.products,
            currencyCode = uiState.currencyCode,
            onDismiss = { viewModel.hideShareDialog() }
        )
    }

    // Restock Dialog
    if (uiState.showRestockDialog) {
        RestockDialog(
            products = uiState.products,
            onRestock = { product, quantity ->
                viewModel.restockProduct(product, quantity)
            },
            onDismiss = { viewModel.hideRestockDialog() }
        )
    }

    // Product Details Dialog (from barcode scan)
    if (uiState.showProductDetailsDialog && uiState.scannedProduct != null) {
        ProductDetailsDialog(
            product = uiState.scannedProduct!!,
            onEdit = { viewModel.showEditDialog(uiState.scannedProduct!!) },
            onDismiss = { viewModel.hideProductDetailsDialog() }
        )
    }
}

@Composable
fun ProductCard(
    product: Product,
    currencyCode: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Stock: ${product.quantityInStock}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (product.quantityInStock > 0) 
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cost: ${CurrencyUtils.formatCurrency(product.costPrice, currencyCode)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Retail: ${CurrencyUtils.formatCurrency(product.sellingPrice, currencyCode)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                product.wholesalePrice?.let { wholesalePrice ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Wholesale: ${CurrencyUtils.formatCurrency(wholesalePrice, currencyCode)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun ProductDialog(
    product: Product?,
    wholesaleModeEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (Product, List<com.example.shoptracklite.data.PriceRange>) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var costPrice by remember { mutableStateOf(product?.costPrice?.toString() ?: "") }
    var sellingPrice by remember { mutableStateOf(product?.sellingPrice?.toString() ?: "") }
    var wholesalePrice by remember { mutableStateOf(product?.wholesalePrice?.toString() ?: "") }
    var quantity by remember { mutableStateOf(product?.quantityInStock?.toString() ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var hasQuantityBasedPricing by remember { mutableStateOf(product?.hasQuantityBasedPricing ?: false) }
    var priceRanges by remember { mutableStateOf(listOf<com.example.shoptracklite.data.PriceRange>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Add Product" else "Edit Product") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = costPrice,
                    onValueChange = { costPrice = it },
                    label = { Text("Cost Price") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { sellingPrice = it },
                    label = { Text("Retail Selling Price") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
                
                // Wholesale Price (only show if wholesale mode is enabled)
                if (wholesaleModeEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = wholesalePrice,
                        onValueChange = { wholesalePrice = it },
                        label = { Text("Wholesale Price (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity in Stock") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Barcode input with scan button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode/QR Code (Optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = { showBarcodeScanner = true },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quantity-based pricing checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasQuantityBasedPricing,
                        onCheckedChange = { hasQuantityBasedPricing = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Use quantity-based pricing (e.g., photocopy services)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                // Price ranges section (only show if checkbox is checked)
                if (hasQuantityBasedPricing) {
                    Spacer(modifier = Modifier.height(16.dp))
                    com.example.shoptracklite.ui.components.PriceRangeManager(
                        priceRanges = priceRanges,
                        onPriceRangesChanged = { priceRanges = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cost = costPrice.toDoubleOrNull() ?: 0.0
                    val sell = sellingPrice.toDoubleOrNull() ?: 0.0
                    val wholesale = wholesalePrice.toDoubleOrNull()
                    val qty = quantity.toIntOrNull() ?: 0
                    
                    val newProduct = if (product == null) {
                        Product(
                            name = name,
                            costPrice = cost,
                            sellingPrice = sell,
                            wholesalePrice = wholesale,
                            quantityInStock = qty,
                            hasQuantityBasedPricing = hasQuantityBasedPricing,
                            barcode = barcode.ifBlank { null }
                        )
                    } else {
                        product.copy(
                            name = name,
                            costPrice = cost,
                            sellingPrice = sell,
                            wholesalePrice = wholesale,
                            quantityInStock = qty,
                            hasQuantityBasedPricing = hasQuantityBasedPricing,
                            barcode = barcode.ifBlank { null }
                        )
                    }
                    onSave(newProduct, priceRanges)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
    // Barcode scanner dialog
    if (showBarcodeScanner) {
        com.example.shoptracklite.ui.components.BarcodeScannerDialog(
            onBarcodeScanned = { scannedBarcode ->
                barcode = scannedBarcode
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}
