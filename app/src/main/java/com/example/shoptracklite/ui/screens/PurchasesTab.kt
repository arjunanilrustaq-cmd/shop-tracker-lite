package com.example.shoptracklite.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.shoptracklite.data.*
import com.example.shoptracklite.ui.components.BarcodeScannerDialog
import com.example.shoptracklite.ui.components.BluetoothBarcodeDetector
import com.example.shoptracklite.utils.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PurchasesTab(
    purchaseBills: List<PurchaseBillWithItems>,
    currencyCode: String,
    onAddPurchase: () -> Unit,
    onViewDetails: (PurchaseBillWithItems) -> Unit,
    onDeleteBill: (PurchaseBill) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (purchaseBills.isEmpty()) {
            // Empty state
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
                        Icons.Default.Receipt,
                        contentDescription = "No Purchases",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No purchase records",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Record inventory purchases to track stock and expenses.\nTap + to add your first purchase",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Group bills by date
            val groupedBills = purchaseBills.groupBy { 
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.bill.date)
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedBills.forEach { (dateStr, bills) ->
                    item {
                        val displayDate = try {
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
                            SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault()).format(date!!)
                        } catch (e: Exception) { dateStr }
                        
                        Text(
                            text = displayDate,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(bills) { billWithItems ->
                        PurchaseBillCard(
                            billWithItems = billWithItems,
                            currencyCode = currencyCode,
                            onViewDetails = { onViewDetails(billWithItems) },
                            onDelete = { onDeleteBill(billWithItems.bill) }
                        )
                    }
                }
            }
        }
        
        // Add button at bottom
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddPurchase,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Purchase")
        }
    }
}

@Composable
fun PurchaseBillCard(
    billWithItems: PurchaseBillWithItems,
    currencyCode: String,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit
) {
    val bill = billWithItems.bill
    val items = billWithItems.items
    val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onViewDetails
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bill.supplierName ?: "Purchase",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${items.size} items • ${dateFormat.format(bill.date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!bill.notes.isNullOrBlank()) {
                        Text(
                            text = bill.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyUtils.formatCurrency(bill.totalAmount, currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // Show brief item list
            if (items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                items.take(3).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.itemName} x${item.quantity.toInt()}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = CurrencyUtils.formatCurrency(item.totalCost, currencyCode),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (items.size > 3) {
                    Text(
                        text = "... and ${items.size - 3} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PurchaseDetailsDialog(
    billWithItems: PurchaseBillWithItems,
    currencyCode: String,
    onDismiss: () -> Unit
) {
    val bill = billWithItems.bill
    val items = billWithItems.items
    val dateFormat = SimpleDateFormat("EEEE, MMM dd, yyyy hh:mm a", Locale.getDefault())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(bill.supplierName ?: "Purchase Details") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = dateFormat.format(bill.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (!bill.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notes: ${bill.notes}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Items",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.itemName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${item.quantity.toInt()} x ${CurrencyUtils.formatCurrency(item.unitCost, currencyCode)} • ${item.itemType.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatCurrency(item.totalCost, currencyCode),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = CurrencyUtils.formatCurrency(bill.totalAmount, currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// Data class for purchase item being added
data class PurchaseItemInput(
    val id: Int = 0,
    val itemType: PurchaseItemType = PurchaseItemType.PRODUCT,
    val existingItemId: Long? = null,
    val name: String = "",
    val quantity: String = "",
    val unitCost: String = "",
    val sellingPrice: String = "",  // Only for new products
    val unit: String = "units",     // Only for new supplies
    val barcode: String = "",       // Barcode for new products
    val isNew: Boolean = false
)

// Data class for new product info to be created
data class NewProductFromPurchase(
    val itemIndex: Int,
    val name: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val quantity: Int,
    val barcode: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPurchaseDialog(
    products: List<Product>,
    supplies: List<Supply>,
    currencyCode: String,
    onSave: (PurchaseBill, List<PurchaseItem>, List<NewProductFromPurchase>) -> Unit,
    onDismiss: () -> Unit
) {
    var supplierName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var purchaseItems by remember { mutableStateOf(listOf(PurchaseItemInput(id = 1))) }
    var nextItemId by remember { mutableIntStateOf(2) }
    var recordAsExpense by remember { mutableStateOf(true) }
    
    val totalAmount = purchaseItems.sumOf { item ->
        val qty = item.quantity.toDoubleOrNull() ?: 0.0
        val cost = item.unitCost.toDoubleOrNull() ?: 0.0
        qty * cost
    }
    
    // Handler for Bluetooth barcode scanner
    val handleBluetoothBarcode: (String) -> Unit = { barcode ->
        // Find the first empty item or the last item to update
        val targetIndex = purchaseItems.indexOfFirst { 
            it.existingItemId == null && it.name.isBlank() && it.itemType == PurchaseItemType.PRODUCT
        }.takeIf { it >= 0 } ?: (purchaseItems.size - 1)
        
        val targetItem = purchaseItems[targetIndex]
        
        // Only process if the target item is a Product type
        if (targetItem.itemType == PurchaseItemType.PRODUCT) {
            val existingProduct = products.find { it.barcode == barcode }
            if (existingProduct != null) {
                // Auto-select existing product
                purchaseItems = purchaseItems.toMutableList().apply {
                    this[targetIndex] = targetItem.copy(
                        isNew = false,
                        existingItemId = existingProduct.id,
                        name = existingProduct.name,
                        barcode = barcode
                    )
                }
            } else {
                // Switch to new product mode with barcode pre-filled
                purchaseItems = purchaseItems.toMutableList().apply {
                    this[targetIndex] = targetItem.copy(
                        isNew = true,
                        existingItemId = null,
                        name = "",
                        barcode = barcode
                    )
                }
            }
        }
    }
    
    // Bluetooth barcode reader support
    BluetoothBarcodeDetector(
        enabled = true,
        onBarcodeScanned = handleBluetoothBarcode
    ) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Purchase") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("Supplier Name (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Items list
                Text(
                    text = "Items",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                purchaseItems.forEachIndexed { index, item ->
                    PurchaseItemInputCard(
                        item = item,
                        products = products,
                        supplies = supplies,
                        currencyCode = currencyCode,
                        onItemChange = { updatedItem ->
                            purchaseItems = purchaseItems.toMutableList().apply {
                                this[index] = updatedItem
                            }
                        },
                        onRemove = {
                            if (purchaseItems.size > 1) {
                                purchaseItems = purchaseItems.toMutableList().apply {
                                    removeAt(index)
                                }
                            }
                        },
                        canRemove = purchaseItems.size > 1,
                        onBarcodeScanned = { barcode ->
                            // Find existing product by barcode
                            val existingProduct = products.find { it.barcode == barcode }
                            if (existingProduct != null) {
                                // Auto-select existing product
                                purchaseItems = purchaseItems.toMutableList().apply {
                                    this[index] = item.copy(
                                        isNew = false,
                                        existingItemId = existingProduct.id,
                                        name = existingProduct.name,
                                        barcode = barcode
                                    )
                                }
                            } else {
                                // Switch to new product mode with barcode pre-filled
                                purchaseItems = purchaseItems.toMutableList().apply {
                                    this[index] = item.copy(
                                        isNew = true,
                                        existingItemId = null,
                                        name = "",
                                        barcode = barcode
                                    )
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Add item button
                OutlinedButton(
                    onClick = {
                        purchaseItems = purchaseItems + PurchaseItemInput(id = nextItemId)
                        nextItemId++
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Item")
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = recordAsExpense,
                        onCheckedChange = { recordAsExpense = it }
                    )
                    Text(
                        text = "Record as expense",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = CurrencyUtils.formatCurrency(totalAmount, currencyCode),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val bill = PurchaseBill(
                        date = Date(),
                        supplierName = supplierName.ifBlank { null },
                        totalAmount = totalAmount,
                        notes = notes.ifBlank { null }
                    )
                    
                    // Build items list and collect new product info
                    val newProducts = mutableListOf<NewProductFromPurchase>()
                    val items = purchaseItems.mapIndexedNotNull { index, input ->
                        val qty = input.quantity.toDoubleOrNull() ?: return@mapIndexedNotNull null
                        val cost = input.unitCost.toDoubleOrNull() ?: return@mapIndexedNotNull null
                        if (input.name.isBlank() && input.existingItemId == null) return@mapIndexedNotNull null
                        
                        // If this is a new product, add to new products list
                        if (input.isNew && input.itemType == PurchaseItemType.PRODUCT) {
                            val sellingPrice = input.sellingPrice.toDoubleOrNull() ?: cost
                            newProducts.add(
                                NewProductFromPurchase(
                                    itemIndex = index,
                                    name = input.name,
                                    costPrice = cost,
                                    sellingPrice = sellingPrice,
                                    quantity = qty.toInt(),
                                    barcode = input.barcode.ifBlank { null }
                                )
                            )
                        }
                        
                        PurchaseItem(
                            billId = 0, // Will be set by repository
                            itemType = input.itemType,
                            itemId = input.existingItemId,
                            itemName = input.name,
                            quantity = qty,
                            unitCost = cost,
                            totalCost = qty * cost
                        )
                    }
                    
                    if (items.isNotEmpty()) {
                        onSave(bill, items, newProducts)
                    }
                },
                enabled = purchaseItems.any { 
                    (it.name.isNotBlank() || it.existingItemId != null) &&
                    it.quantity.toDoubleOrNull() != null &&
                    it.unitCost.toDoubleOrNull() != null
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
    } // End BluetoothBarcodeDetector
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseItemInputCard(
    item: PurchaseItemInput,
    products: List<Product>,
    supplies: List<Supply>,
    currencyCode: String,
    onItemChange: (PurchaseItemInput) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
    onBarcodeScanned: (String) -> Unit
) {
    var expandedType by remember { mutableStateOf(false) }
    var expandedItem by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    
    // Barcode Scanner Dialog
    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            onBarcodeScanned = { barcode ->
                showBarcodeScanner = false
                onBarcodeScanned(barcode)
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type selector
                ExposedDropdownMenuBox(
                    expanded = expandedType,
                    onExpandedChange = { expandedType = !expandedType },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (item.itemType == PurchaseItemType.PRODUCT) "Product" else "Supply",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                        modifier = Modifier.menuAnchor(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(
                        expanded = expandedType,
                        onDismissRequest = { expandedType = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Product") },
                            onClick = {
                                onItemChange(item.copy(
                                    itemType = PurchaseItemType.PRODUCT,
                                    existingItemId = null,
                                    name = "",
                                    barcode = "",
                                    isNew = false
                                ))
                                expandedType = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Supply") },
                            onClick = {
                                onItemChange(item.copy(
                                    itemType = PurchaseItemType.SUPPLY,
                                    existingItemId = null,
                                    name = "",
                                    barcode = "",
                                    isNew = false
                                ))
                                expandedType = false
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Barcode scan button (only for Products)
                if (item.itemType == PurchaseItemType.PRODUCT) {
                    IconButton(
                        onClick = { showBarcodeScanner = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Scan Barcode",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (canRemove) {
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Item selector or new item toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isNew,
                    onCheckedChange = { isNew ->
                        onItemChange(item.copy(
                            isNew = isNew,
                            existingItemId = null,
                            name = ""
                        ))
                    }
                )
                Text(
                    text = "New ${if (item.itemType == PurchaseItemType.PRODUCT) "product" else "supply"}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            if (item.isNew) {
                // New item fields
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onItemChange(item.copy(name = it)) },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                
                if (item.itemType == PurchaseItemType.PRODUCT) {
                    // Barcode field (may be pre-filled from scan)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = item.barcode,
                        onValueChange = { onItemChange(item.copy(barcode = it)) },
                        label = { Text("Barcode") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        trailingIcon = {
                            IconButton(
                                onClick = { showBarcodeScanner = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "Scan Barcode",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = item.sellingPrice,
                        onValueChange = { 
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                onItemChange(item.copy(sellingPrice = it))
                            }
                        },
                        label = { Text("Selling Price") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                // Existing item selector
                val itemsList = if (item.itemType == PurchaseItemType.PRODUCT) {
                    products.map { it.id to it.name }
                } else {
                    supplies.map { it.id to it.name }
                }
                
                ExposedDropdownMenuBox(
                    expanded = expandedItem,
                    onExpandedChange = { expandedItem = !expandedItem },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = item.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select ${if (item.itemType == PurchaseItemType.PRODUCT) "Product" else "Supply"}") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedItem) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenu(
                        expanded = expandedItem,
                        onDismissRequest = { expandedItem = false }
                    ) {
                        itemsList.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    onItemChange(item.copy(
                                        existingItemId = id,
                                        name = name
                                    ))
                                    expandedItem = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Quantity and cost
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = item.quantity,
                    onValueChange = { 
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            onItemChange(item.copy(quantity = it))
                        }
                    },
                    label = { Text("Qty") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = item.unitCost,
                    onValueChange = { 
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            onItemChange(item.copy(unitCost = it))
                        }
                    },
                    label = { Text("Unit Cost") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
            
            // Show calculated total
            val qty = item.quantity.toDoubleOrNull() ?: 0.0
            val cost = item.unitCost.toDoubleOrNull() ?: 0.0
            if (qty > 0 && cost > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Subtotal: ${CurrencyUtils.formatCurrency(qty * cost, currencyCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
