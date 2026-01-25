package com.example.shoptracklite.data

import kotlinx.coroutines.flow.Flow
import java.util.Date

class ShopTrackRepository(
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val favoriteDao: FavoriteDao,
    private val priceRangeDao: PriceRangeDao,
    private val settingsDao: SettingsDao,
    private val expenseDao: ExpenseDao
) {
    // Product operations
    fun getAllProducts(): Flow<List<Product>> = productDao.getAllProducts()
    
    suspend fun getProductById(id: Long): Product? = productDao.getProductById(id)
    
    suspend fun getProductByBarcode(barcode: String): Product? = productDao.getProductByBarcode(barcode)
    
    suspend fun insertProduct(product: Product): Long = productDao.insertProduct(product)
    
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)
    
    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)
    
    suspend fun updateProductQuantity(productId: Long, newQuantity: Int) = 
        productDao.updateProductQuantity(productId, newQuantity)

    // Sale operations
    fun getAllSales(): Flow<List<Sale>> = saleDao.getAllSales()
    
    fun getSalesByDate(date: Date): Flow<List<Sale>> = saleDao.getSalesByDate(date)
    
    fun getTodaysSales(): Flow<List<Sale>> = saleDao.getTodaysSales()
    
    suspend fun getSaleById(saleId: Long): Sale? = saleDao.getSaleById(saleId)
    
    suspend fun insertSale(sale: Sale): Long = saleDao.insertSale(sale)
    
    suspend fun updateSale(sale: Sale) = saleDao.updateSale(sale)
    
    suspend fun getTodaysRevenue(): Double = saleDao.getTodaysRevenue() ?: 0.0
    
    suspend fun getTodaysProfit(): Double = saleDao.getTodaysProfit() ?: 0.0
    
    suspend fun getTodaysSalesCount(): Int = saleDao.getTodaysSalesCount()

    suspend fun getTodaysCashRevenue(): Double = saleDao.getTodaysCashRevenue() ?: 0.0
    
    suspend fun getTodaysVisaRevenue(): Double = saleDao.getTodaysVisaRevenue() ?: 0.0
    
    suspend fun getTodaysCashSalesCount(): Int = saleDao.getTodaysCashSalesCount()
    
    suspend fun getTodaysVisaSalesCount(): Int = saleDao.getTodaysVisaSalesCount()

    // Monthly reports operations
    fun getCurrentMonthSales(): Flow<List<Sale>> = saleDao.getCurrentMonthSales()
    
    fun getMonthlySalesByDate(): Flow<List<MonthlySalesSummary>> = saleDao.getMonthlySalesByDate()
    
    fun getSalesByDateString(date: String): Flow<List<Sale>> = saleDao.getSalesByDateString(date)
    
    suspend fun getRevenueByDate(date: String): Double = saleDao.getRevenueByDate(date) ?: 0.0
    
    suspend fun getProfitByDate(date: String): Double = saleDao.getProfitByDate(date) ?: 0.0
    
    suspend fun getSalesCountByDate(date: String): Int = saleDao.getSalesCountByDate(date)

    // Price Range operations
    fun getPriceRangesByProduct(productId: Long): Flow<List<PriceRange>> = 
        priceRangeDao.getPriceRangesByProduct(productId)
    
    suspend fun getPriceRangesByProductSync(productId: Long): List<PriceRange> = 
        priceRangeDao.getPriceRangesByProductSync(productId)
    
    suspend fun insertPriceRange(priceRange: PriceRange): Long = 
        priceRangeDao.insertPriceRange(priceRange)
    
    suspend fun insertPriceRanges(priceRanges: List<PriceRange>) = 
        priceRangeDao.insertPriceRanges(priceRanges)
    
    suspend fun updatePriceRange(priceRange: PriceRange) = 
        priceRangeDao.updatePriceRange(priceRange)
    
    suspend fun deletePriceRange(priceRange: PriceRange) = 
        priceRangeDao.deletePriceRange(priceRange)
    
    suspend fun deletePriceRangesByProduct(productId: Long) = 
        priceRangeDao.deletePriceRangesByProduct(productId)
    
    suspend fun getPriceForQuantity(productId: Long, quantity: Int): Double? = 
        priceRangeDao.getPriceForQuantity(productId, quantity)

    // Favorites operations
    fun getFavoriteProducts(): Flow<List<Product>> = favoriteDao.getFavoriteProducts()
    
    suspend fun addToFavorites(productId: Long) {
        val maxOrder = favoriteDao.getMaxDisplayOrder() ?: 0
        favoriteDao.addFavorite(Favorite(productId, maxOrder + 1))
    }
    
    suspend fun removeFromFavorites(productId: Long) {
        val currentOrder = favoriteDao.getFavoriteProductIdsOrdered().indexOf(productId)
        favoriteDao.removeFavorite(Favorite(productId, currentOrder))
    }
    
    suspend fun isFavorite(productId: Long): Boolean = favoriteDao.isFavorite(productId) > 0
    
    suspend fun updateFavoriteOrder(productIds: List<Long>) {
        productIds.forEachIndexed { index, productId ->
            favoriteDao.updateDisplayOrder(productId, index)
        }
    }
    
    // Settings operations
    fun getSettings(): Flow<Settings?> = settingsDao.getSettings()
    
    suspend fun getSettingsSync(): Settings {
        return settingsDao.getSettingsSync() ?: Settings(id = 1, wholesaleModeEnabled = false, currencyCode = "USD")
    }
    
    suspend fun updateSettings(settings: Settings) = settingsDao.updateSettings(settings)
    
    suspend fun getCurrencyCode(): String {
        return getSettingsSync().currencyCode
    }
    
    // Search operations
    fun searchProducts(query: String): Flow<List<Product>> {
        return if (query.isBlank()) {
            productDao.getAllProducts()
        } else {
            productDao.searchProducts("%$query%")
        }
    }

    // Expense operations
    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()
    
    fun getTodaysExpenses(): Flow<List<Expense>> = expenseDao.getTodaysExpenses()
    
    fun getCurrentMonthExpenses(): Flow<List<Expense>> = expenseDao.getCurrentMonthExpenses()
    
    fun getExpensesByDateString(date: String): Flow<List<Expense>> = expenseDao.getExpensesByDateString(date)
    
    suspend fun getTodaysExpenseTotal(): Double = expenseDao.getTodaysExpenseTotal() ?: 0.0
    
    suspend fun getExpenseTotalByDate(date: String): Double = expenseDao.getExpenseTotalByDate(date) ?: 0.0
    
    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)
    
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    // Business logic operations
    suspend fun recordSale(productId: Long, quantitySold: Int, paymentMethod: PaymentMethod, isWholesale: Boolean = false, discountAmount: Double = 0.0): Boolean {
        val product = getProductById(productId) ?: return false
        
        if (product.quantityInStock < quantitySold) return false
        
        // Calculate total amount based on sale type (before discount)
        val totalAmountBeforeDiscount = if (isWholesale && product.wholesalePrice != null) {
            // Use wholesale price
            quantitySold * product.wholesalePrice
        } else if (product.hasQuantityBasedPricing) {
            // Use quantity-based pricing
            val quantityBasedPrice = getPriceForQuantity(productId, quantitySold)
            if (quantityBasedPrice != null) {
                quantitySold * quantityBasedPrice
            } else {
                quantitySold * product.sellingPrice
            }
        } else {
            // Use regular selling price
            quantitySold * product.sellingPrice
        }
        
        // Apply discount to get actual revenue
        val totalAmount = maxOf(0.0, totalAmountBeforeDiscount - discountAmount)
        
        val totalCost = quantitySold * product.costPrice
        val profit = totalAmount - totalCost
        val unitPrice = totalAmount / quantitySold
        
        val sale = Sale(
            productId = productId,
            productName = product.name,
            quantitySold = quantitySold,
            unitPrice = unitPrice,
            totalAmount = totalAmount,
            costPrice = product.costPrice,
            profit = profit,
            paymentMethod = paymentMethod,
            isWholesale = isWholesale
        )
        
        insertSale(sale)
        updateProductQuantity(productId, product.quantityInStock - quantitySold)
        
        return true
    }
    
    // Get COGS (Cost of Goods Sold) for a specific date
    suspend fun getCOGSByDate(date: String): Double {
        val sales = saleDao.getSalesByDateString(date)
        var cogs = 0.0
        sales.collect { salesList ->
            cogs = salesList.sumOf { it.costPrice * it.quantitySold }
        }
        return cogs
    }
    
    // Get COGS for today
    suspend fun getTodaysCOGS(): Double {
        val sales = saleDao.getTodaysSales()
        var cogs = 0.0
        sales.collect { salesList ->
            cogs = salesList.sumOf { it.costPrice * it.quantitySold }
        }
        return cogs
    }
    
    // Cancel a sale - restores inventory and marks sale as cancelled
    suspend fun cancelSale(saleId: Long): Boolean {
        try {
            val sale = getSaleById(saleId) ?: return false
            
            // Don't cancel if already cancelled
            if (sale.isCancelled) return false
            
            // Get the product to restore inventory
            val product = getProductById(sale.productId) ?: return false
            
            // Restore inventory quantity
            val newQuantity = product.quantityInStock + sale.quantitySold
            updateProductQuantity(sale.productId, newQuantity)
            
            // Mark sale as cancelled
            val cancelledSale = sale.copy(isCancelled = true)
            updateSale(cancelledSale)
            
            return true
        } catch (e: Exception) {
            android.util.Log.e("ShopTrackRepository", "Error cancelling sale", e)
            return false
        }
    }
}
