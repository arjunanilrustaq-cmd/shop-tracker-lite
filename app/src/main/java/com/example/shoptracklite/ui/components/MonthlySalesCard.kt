package com.example.shoptracklite.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shoptracklite.data.MonthlySalesSummary
import com.example.shoptracklite.utils.CurrencyUtils
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MonthlySalesCard(
    summary: MonthlySalesSummary,
    currencyCode: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    val inputDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    val displayDate = try {
        val date = inputDateFormatter.parse(summary.date)
        dateFormatter.format(date ?: Date())
    } catch (e: Exception) {
        summary.date
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${summary.salesCount} sales",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = CurrencyUtils.formatCurrency(summary.totalRevenue, currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Profit: ${CurrencyUtils.formatCurrency(summary.totalProfit, currencyCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (summary.totalProfit >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}
