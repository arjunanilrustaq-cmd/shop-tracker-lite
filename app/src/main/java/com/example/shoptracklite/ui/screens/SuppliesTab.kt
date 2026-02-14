package com.example.shoptracklite.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.shoptracklite.data.Supply
import com.example.shoptracklite.utils.CurrencyUtils

@Composable
fun SuppliesTab(
    supplies: List<Supply>,
    filteredSupplies: List<Supply>,
    searchQuery: String,
    currencyCode: String,
    onSearchQueryChange: (String) -> Unit,
    onAddSupply: () -> Unit,
    onEditSupply: (Supply) -> Unit,
    onDeleteSupply: (Supply) -> Unit,
    onAdjustQuantity: (Supply, Double) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search supplies...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (supplies.isEmpty()) {
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
                        Icons.Default.Inventory2,
                        contentDescription = "No Supplies",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No supplies added yet",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Track consumables like paper, ink, etc.\nTap + to add your first supply",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (filteredSupplies.isEmpty() && searchQuery.isNotEmpty()) {
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
                        text = "No supplies found",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSupplies) { supply ->
                    SupplyCard(
                        supply = supply,
                        currencyCode = currencyCode,
                        onEdit = { onEditSupply(supply) },
                        onDelete = { onDeleteSupply(supply) },
                        onAdjustQuantity = { newQty -> onAdjustQuantity(supply, newQty) }
                    )
                }
            }
        }
        
        // Add button at bottom
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddSupply,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Supply")
        }
    }
}

@Composable
fun SupplyCard(
    supply: Supply,
    currencyCode: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustQuantity: (Double) -> Unit
) {
    var showAdjustDialog by remember { mutableStateOf(false) }
    val isLowStock = supply.quantity <= supply.lowStockThreshold
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (isLowStock) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = supply.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Qty: ${String.format("%.1f", supply.quantity)} ${supply.unit}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        if (isLowStock) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Low Stock",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Low",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Text(
                        text = "Cost: ${CurrencyUtils.formatCurrency(supply.costPerUnit, currencyCode)} per ${supply.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = { showAdjustDialog = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Adjust Quantity")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
    
    // Adjust quantity dialog
    if (showAdjustDialog) {
        AdjustSupplyQuantityDialog(
            supply = supply,
            onAdjust = { newQty ->
                onAdjustQuantity(newQty)
                showAdjustDialog = false
            },
            onDismiss = { showAdjustDialog = false }
        )
    }
}

@Composable
fun AdjustSupplyQuantityDialog(
    supply: Supply,
    onAdjust: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var quantity by remember { mutableStateOf(supply.quantity.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust ${supply.name}") },
        text = {
            Column {
                Text(
                    text = "Current: ${String.format("%.1f", supply.quantity)} ${supply.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { 
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            quantity = it
                        }
                    },
                    label = { Text("New Quantity") },
                    suffix = { Text(supply.unit) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newQty = quantity.toDoubleOrNull() ?: supply.quantity
                    onAdjust(newQty)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSupplyDialog(
    supply: Supply?,
    onSave: (Supply) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(supply?.name ?: "") }
    var quantity by remember { mutableStateOf(supply?.quantity?.toString() ?: "") }
    var unit by remember { mutableStateOf(supply?.unit ?: "sheets") }
    var costPerUnit by remember { mutableStateOf(supply?.costPerUnit?.toString() ?: "") }
    var lowStockThreshold by remember { mutableStateOf(supply?.lowStockThreshold?.toString() ?: "10") }
    var expandedUnit by remember { mutableStateOf(false) }
    
    val units = listOf("sheets", "packs", "rolls", "pieces", "liters", "ml", "kg", "g", "units")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (supply == null) "Add Supply" else "Edit Supply") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Supply Name") },
                    placeholder = { Text("e.g., Photo Paper, A4 Paper") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { 
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                quantity = it
                            }
                        },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = expandedUnit,
                        onExpandedChange = { expandedUnit = !expandedUnit },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedUnit)
                            },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedUnit,
                            onDismissRequest = { expandedUnit = false }
                        ) {
                            units.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        expandedUnit = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = costPerUnit,
                    onValueChange = { 
                        if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            costPerUnit = it
                        }
                    },
                    label = { Text("Cost per $unit") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = lowStockThreshold,
                    onValueChange = { 
                        if (it.isEmpty() || it.matches(Regex("^\\d*$"))) {
                            lowStockThreshold = it
                        }
                    },
                    label = { Text("Low Stock Alert Threshold") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    supportingText = { Text("Alert when quantity falls below this") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newSupply = Supply(
                        id = supply?.id ?: 0,
                        name = name,
                        quantity = quantity.toDoubleOrNull() ?: 0.0,
                        unit = unit,
                        costPerUnit = costPerUnit.toDoubleOrNull() ?: 0.0,
                        lowStockThreshold = lowStockThreshold.toIntOrNull() ?: 10,
                        createdAt = supply?.createdAt ?: java.util.Date()
                    )
                    onSave(newSupply)
                },
                enabled = name.isNotBlank()
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
}
