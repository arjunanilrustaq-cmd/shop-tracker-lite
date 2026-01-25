package com.example.shoptracklite.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.shoptracklite.data.PriceRange

@Composable
fun PriceRangeManager(
    priceRanges: List<PriceRange>,
    onPriceRangesChanged: (List<PriceRange>) -> Unit,
    modifier: Modifier = Modifier
) {
    var priceRangeList by remember { mutableStateOf(priceRanges.toMutableList()) }
    
    // Update the list when priceRanges parameter changes
    LaunchedEffect(priceRanges) {
        priceRangeList = priceRanges.toMutableList()
    }
    
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Price Ranges",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(
                onClick = {
                    val newRange = PriceRange(
                        productId = 0, // Will be set when saving the product
                        minQuantity = 1,
                        maxQuantity = 10,
                        price = 0.0
                    )
                    priceRangeList.add(newRange)
                    onPriceRangesChanged(priceRangeList.toList())
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Price Range",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Price ranges list (using Column instead of LazyColumn to avoid nested scroll issues)
        if (priceRangeList.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "No price ranges set. Click + to add ranges.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                priceRangeList.forEachIndexed { index, priceRange ->
                    PriceRangeItem(
                        priceRange = priceRange,
                        onUpdate = { updatedRange ->
                            priceRangeList[index] = updatedRange
                            onPriceRangesChanged(priceRangeList.toList())
                        },
                        onDelete = {
                            priceRangeList.removeAt(index)
                            onPriceRangesChanged(priceRangeList.toList())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceRangeItem(
    priceRange: PriceRange,
    onUpdate: (PriceRange) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Quantity range row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Min Quantity
                OutlinedTextField(
                    value = priceRange.minQuantity.toString(),
                    onValueChange = { value ->
                        val newMin = value.toIntOrNull() ?: 1
                        onUpdate(priceRange.copy(minQuantity = newMin))
                    },
                    label = { Text("Min") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )
                
                Text(
                    text = "to",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Max Quantity
                OutlinedTextField(
                    value = priceRange.maxQuantity.toString(),
                    onValueChange = { value ->
                        val newMax = value.toIntOrNull() ?: 10
                        onUpdate(priceRange.copy(maxQuantity = newMax))
                    },
                    label = { Text("Max") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )
            }
            
            // Price and delete button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Price
                OutlinedTextField(
                    value = if (priceRange.price == 0.0) "" else priceRange.price.toString(),
                    onValueChange = { value ->
                        val newPrice = value.toDoubleOrNull() ?: 0.0
                        onUpdate(priceRange.copy(price = newPrice))
                    },
                    label = { Text("Price per unit") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true
                )
                
                // Delete button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
