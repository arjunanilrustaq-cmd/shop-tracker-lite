package com.example.shoptracklite.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.shoptracklite.data.Expense
import com.example.shoptracklite.data.MonthlySalesSummary
import com.example.shoptracklite.data.Sale
import com.example.shoptracklite.data.PaymentMethod
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object ReportShareUtils {

    fun generateReportBitmap(
        context: Context,
        selectedDate: String,
        selectedDateSales: List<Sale>,
        monthlySalesByDate: List<MonthlySalesSummary>
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val width = (800 * density).toInt()
        val height = (1200 * density).toInt()
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Fill background
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
        }
        
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 24 * density
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val headerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 18 * density
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 14 * density
        }
        
        val smallPaint = Paint().apply {
            isAntiAlias = true
            color = Color.GRAY
            textSize = 12 * density
        }
        
        var y = 50 * density
        
        // Title
        canvas.drawText("ShopTrackLite - Monthly Report", 50 * density, y, titlePaint)
        y += 40 * density
        
        // Date
        val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val inputDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDate = try {
            val date = inputDateFormatter.parse(selectedDate)
            dateFormatter.format(date ?: Date())
        } catch (e: Exception) {
            selectedDate
        }
        
        canvas.drawText("Selected Date: $displayDate", 50 * density, y, headerPaint)
        y += 30 * density
        
        // Summary for selected date (bill-wise)
        if (selectedDateSales.isNotEmpty()) {
            val groupedBills = selectedDateSales.groupBy { it.transactionId ?: it.id.toLong() }.values
            val totalSales = groupedBills.size
            val totalRevenue = selectedDateSales.sumOf { it.totalAmount }
            val totalProfit = selectedDateSales.sumOf { it.profit }
            val totalItems = selectedDateSales.sumOf { it.quantitySold }
            val cashRevenue = selectedDateSales.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalAmount }
            val visaRevenue = selectedDateSales.filter { it.paymentMethod == PaymentMethod.VISA }.sumOf { it.totalAmount }
            
            canvas.drawText("Daily Summary:", 50 * density, y, headerPaint)
            y += 25 * density
            
            canvas.drawText("Bills: $totalSales", 50 * density, y, textPaint)
            y += 20 * density
            canvas.drawText("Items Sold: $totalItems", 50 * density, y, textPaint)
            y += 20 * density
            canvas.drawText("Revenue: ${NumberFormat.getCurrencyInstance(Locale.getDefault()).format(totalRevenue)}", 50 * density, y, textPaint)
            y += 20 * density
            canvas.drawText("Profit: ${NumberFormat.getCurrencyInstance(Locale.getDefault()).format(totalProfit)}", 50 * density, y, textPaint)
            y += 20 * density
            canvas.drawText("Cash: ${NumberFormat.getCurrencyInstance(Locale.getDefault()).format(cashRevenue)}", 50 * density, y, textPaint)
            y += 20 * density
            canvas.drawText("Visa: ${NumberFormat.getCurrencyInstance(Locale.getDefault()).format(visaRevenue)}", 50 * density, y, textPaint)
            y += 30 * density
            
            // Sales details (bill-wise)
            canvas.drawText("Sales Details (by Bill):", 50 * density, y, headerPaint)
            y += 25 * density
            
            groupedBills.forEach { billSales ->
                val billTotal = billSales.sumOf { it.totalAmount }
                val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(billSales.first().saleDate)
                canvas.drawText("Bill $timeStr - ${billSales.size} item(s) - ${NumberFormat.getCurrencyInstance(Locale.getDefault()).format(billTotal)}", 
                    50 * density, y, textPaint)
                y += 18 * density
                billSales.forEach { sale ->
                    canvas.drawText("  ${sale.productName} x${sale.quantitySold} - ${NumberFormat.getCurrencyInstance(Locale.getDefault()).format(sale.totalAmount)}", 
                        50 * density, y, textPaint)
                    y += 16 * density
                }
                y += 4 * density
            }
            y += 20 * density
        }
        
        // Monthly summary
        if (monthlySalesByDate.isNotEmpty()) {
            canvas.drawText("Monthly Sales Summary:", 50 * density, y, headerPaint)
            y += 25 * density
            
            monthlySalesByDate.forEach { summary ->
                val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
                val inputDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val displayDate = try {
                    val date = inputDateFormatter.parse(summary.date)
                    dateFormatter.format(date ?: Date())
                } catch (e: Exception) {
                    summary.date
                }
                
                canvas.drawText("$displayDate: ${summary.salesCount} sales, ${NumberFormat.getCurrencyInstance(Locale.getDefault()).format(summary.totalRevenue)}", 
                    50 * density, y, textPaint)
                y += 18 * density
            }
        }
        
        // Footer
        y += 30 * density
        canvas.drawText("Generated on: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())}", 
            50 * density, y, smallPaint)
        
        return bitmap
    }
    
    fun shareReportAsImage(context: Context, bitmap: Bitmap) {
        try {
            val fileName = "shop_report_${System.currentTimeMillis()}.jpg"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ShopTrackLite Report")
                putExtra(Intent.EXTRA_TEXT, "Monthly sales report from ShopTrackLite")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun generateDailyExpenseReportBitmap(
        context: Context,
        expenses: List<Expense>,
        totalExpense: Double,
        currencyCode: String
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val width = (800 * density).toInt()
        val baseHeight = 400 + (expenses.size * 25)
        val height = (baseHeight.coerceAtLeast(500) * density).toInt()
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Fill background
        canvas.drawColor(Color.WHITE)
        
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 24 * density
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val headerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 18 * density
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 14 * density
        }
        
        val amountPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#F44336") // Red color for expenses
            textSize = 14 * density
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val totalPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#F44336")
            textSize = 22 * density
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val smallPaint = Paint().apply {
            isAntiAlias = true
            color = Color.GRAY
            textSize = 12 * density
        }
        
        var y = 50 * density
        
        // Title
        canvas.drawText("ShopTrackLite - Daily Expenses", 50 * density, y, titlePaint)
        y += 40 * density
        
        // Date
        val dateFormatter = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
        canvas.drawText("Date: ${dateFormatter.format(Date())}", 50 * density, y, headerPaint)
        y += 40 * density
        
        // Total
        canvas.drawText("Total Daily Expenses:", 50 * density, y, headerPaint)
        y += 30 * density
        canvas.drawText(CurrencyUtils.formatCurrency(totalExpense, currencyCode), 50 * density, y, totalPaint)
        y += 50 * density
        
        // Expense details
        if (expenses.isNotEmpty()) {
            canvas.drawText("Expense Details:", 50 * density, y, headerPaint)
            y += 30 * density
            
            // Group by category
            val groupedExpenses = expenses.groupBy { it.category }
            groupedExpenses.forEach { (category, categoryExpenses) ->
                val categoryTotal = categoryExpenses.sumOf { it.amount }
                canvas.drawText("$category: ${CurrencyUtils.formatCurrency(categoryTotal, currencyCode)}", 
                    50 * density, y, textPaint)
                y += 22 * density
                
                categoryExpenses.forEach { expense ->
                    canvas.drawText("  • ${expense.description}: ${CurrencyUtils.formatCurrency(expense.amount, currencyCode)}", 
                        70 * density, y, smallPaint)
                    y += 18 * density
                }
                y += 8 * density
            }
        } else {
            canvas.drawText("No expenses recorded today", 50 * density, y, textPaint)
            y += 25 * density
        }
        
        // Footer
        y += 30 * density
        canvas.drawText("Generated on: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())}", 
            50 * density, y, smallPaint)
        
        return bitmap
    }
    
    fun generateMonthlyExpenseReportBitmap(
        context: Context,
        expenses: List<Expense>,
        totalExpense: Double,
        currencyCode: String
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val width = (800 * density).toInt()
        val baseHeight = 500 + (expenses.size * 20)
        val height = (baseHeight.coerceAtLeast(600) * density).toInt()
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Fill background
        canvas.drawColor(Color.WHITE)
        
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 24 * density
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val headerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 18 * density
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 14 * density
        }
        
        val totalPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#F44336")
            textSize = 22 * density
            typeface = Typeface.DEFAULT_BOLD
        }
        
        val smallPaint = Paint().apply {
            isAntiAlias = true
            color = Color.GRAY
            textSize = 12 * density
        }
        
        var y = 50 * density
        
        // Title
        canvas.drawText("ShopTrackLite - Monthly Expenses", 50 * density, y, titlePaint)
        y += 40 * density
        
        // Month
        val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        canvas.drawText("Month: ${monthFormatter.format(Date())}", 50 * density, y, headerPaint)
        y += 40 * density
        
        // Total
        canvas.drawText("Total Monthly Expenses:", 50 * density, y, headerPaint)
        y += 30 * density
        canvas.drawText(CurrencyUtils.formatCurrency(totalExpense, currencyCode), 50 * density, y, totalPaint)
        y += 50 * density
        
        // Summary by category
        if (expenses.isNotEmpty()) {
            canvas.drawText("Summary by Category:", 50 * density, y, headerPaint)
            y += 30 * density
            
            val groupedExpenses = expenses.groupBy { it.category }
            groupedExpenses.forEach { (category, categoryExpenses) ->
                val categoryTotal = categoryExpenses.sumOf { it.amount }
                val count = categoryExpenses.size
                canvas.drawText("$category ($count items): ${CurrencyUtils.formatCurrency(categoryTotal, currencyCode)}", 
                    50 * density, y, textPaint)
                y += 25 * density
            }
            
            y += 20 * density
            
            // Recent expenses
            canvas.drawText("Recent Expenses:", 50 * density, y, headerPaint)
            y += 25 * density
            
            val dateFormatter = SimpleDateFormat("MMM dd", Locale.getDefault())
            expenses.take(15).forEach { expense ->
                val dateStr = dateFormatter.format(expense.date)
                canvas.drawText("$dateStr - ${expense.description}: ${CurrencyUtils.formatCurrency(expense.amount, currencyCode)}", 
                    50 * density, y, smallPaint)
                y += 18 * density
            }
            
            if (expenses.size > 15) {
                canvas.drawText("... and ${expenses.size - 15} more expenses", 50 * density, y, smallPaint)
                y += 18 * density
            }
        } else {
            canvas.drawText("No expenses recorded this month", 50 * density, y, textPaint)
            y += 25 * density
        }
        
        // Footer
        y += 30 * density
        canvas.drawText("Generated on: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())}", 
            50 * density, y, smallPaint)
        
        return bitmap
    }
    
    fun shareExpenseReportAsImage(context: Context, bitmap: Bitmap, isDaily: Boolean) {
        try {
            val reportType = if (isDaily) "daily" else "monthly"
            val fileName = "expense_${reportType}_${System.currentTimeMillis()}.jpg"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val title = if (isDaily) "Daily Expense Report" else "Monthly Expense Report"
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ShopTrackLite - $title")
                putExtra(Intent.EXTRA_TEXT, "$title from ShopTrackLite")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share $title"))
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
