package com.example.shoptracklite.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.shoptracklite.data.Product
import java.text.NumberFormat
import java.util.*

@Composable
fun ProductDetailsDialog(
    product: Product,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Header with product name
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Product details
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Stock status
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                product.quantityInStock == 0 -> MaterialTheme.colorScheme.errorContainer
                                product.quantityInStock < 10 -> Color(0xFFFFF3E0)
                                else -> Color(0xFFE8F5E9)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when {
                                    product.quantityInStock == 0 -> Icons.Default.Warning
                                    product.quantityInStock < 10 -> Icons.Default.Info
                                    else -> Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                tint = when {
                                    product.quantityInStock == 0 -> MaterialTheme.colorScheme.error
                                    product.quantityInStock < 10 -> Color(0xFFFF6F00)
                                    else -> Color(0xFF2E7D32)
                                },
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Current Stock",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${product.quantityInStock} units",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        product.quantityInStock == 0 -> MaterialTheme.colorScheme.error
                                        product.quantityInStock < 10 -> Color(0xFFFF6F00)
                                        else -> Color(0xFF2E7D32)
                                    }
                                )
                            }
                        }
                    }

                    Divider()

                    // Pricing details
                    DetailRow(
                        icon = Icons.Default.AttachMoney,
                        label = "Cost Price",
                        value = currencyFormat.format(product.costPrice),
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )

                    DetailRow(
                        icon = Icons.Default.Sell,
                        label = "Selling Price",
                        value = currencyFormat.format(product.sellingPrice),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    // Profit margin
                    val profit = product.sellingPrice - product.costPrice
                    val profitPercentage = if (product.costPrice > 0) {
                        ((profit / product.costPrice) * 100)
                    } else {
                        0.0
                    }

                    DetailRow(
                        icon = Icons.Default.TrendingUp,
                        label = "Profit per Unit",
                        value = "${currencyFormat.format(profit)} (${String.format("%.1f", profitPercentage)}%)",
                        iconTint = if (profit > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )

                    // Total value
                    val totalValue = product.quantityInStock * product.sellingPrice
                    DetailRow(
                        icon = Icons.Default.Inventory,
                        label = "Total Inventory Value",
                        value = currencyFormat.format(totalValue),
                        iconTint = MaterialTheme.colorScheme.secondary
                    )

                    // Barcode if available
                    if (product.barcode != null) {
                        Divider()
                        DetailRow(
                            icon = Icons.Default.QrCode,
                            label = "Barcode",
                            value = product.barcode,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Quantity-based pricing indicator
                    if (product.hasQuantityBasedPricing) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Quantity-based pricing enabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                    Button(
                        onClick = {
                            onDismiss()
                            onEdit()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

