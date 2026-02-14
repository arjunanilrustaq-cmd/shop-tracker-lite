package com.example.shoptracklite.data

import kotlinx.coroutines.flow.Flow
import java.util.Date

class ShopTrackRepository(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val saleDao: SaleDao,
    private val favoriteDao: FavoriteDao,
    private val priceRangeDao: PriceRangeDao,
    private val settingsDao: SettingsDao,
    private val expenseDao: ExpenseDao,
    private val cashReconciliationDao: CashReconciliationDao,
    private val supplyDao: SupplyDao? = null,
    private val productSupplyLinkDao: ProductSupplyLinkDao? = null,
    private val purchaseBillDao: PurchaseBillDao? = null
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

    // Category operations
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    
    suspend fun getAllCategoriesSync(): List<Category> = categoryDao.getAllCategoriesSync()
    
    suspend fun insertCategory(category: Category): Long = categoryDao.insertCategory(category)
    
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)
    
    fun getProductsByCategory(categoryId: Long?): Flow<List<Product>> = 
        productDao.getProductsByCategory(categoryId)

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
    
    // Sales for a specific year-month (format: "YYYY-MM")
    fun getSalesByYearMonth(yearMonth: String): Flow<List<Sale>> = saleDao.getSalesByYearMonth(yearMonth)
    
    fun getMonthlySalesByDateForMonth(yearMonth: String): Flow<List<MonthlySalesSummary>> = 
        saleDao.getMonthlySalesByDateForMonth(yearMonth)
    
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
    
    // Expenses for a specific year-month (format: "YYYY-MM")
    fun getExpensesByYearMonth(yearMonth: String): Flow<List<Expense>> = expenseDao.getExpensesByYearMonth(yearMonth)
    
    suspend fun getTodaysExpenseTotal(): Double = expenseDao.getTodaysExpenseTotal() ?: 0.0
    
    suspend fun getExpenseTotalByDate(date: String): Double = expenseDao.getExpenseTotalByDate(date) ?: 0.0
    
    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)
    
    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)
    
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    // Business logic operations
    suspend fun recordSale(productId: Long, quantitySold: Int, paymentMethod: PaymentMethod, isWholesale: Boolean = false, discountAmount: Double = 0.0, transactionId: Long? = null): Boolean {
        val product = getProductById(productId) ?: return false
        
        if (product.trackInventory && product.quantityInStock < quantitySold) return false
        
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
            isWholesale = isWholesale,
            transactionId = transactionId
        )
        
        insertSale(sale)
        if (product.trackInventory) {
            updateProductQuantity(productId, product.quantityInStock - quantitySold)
            deductSuppliesForSale(productId, quantitySold)
        }
        
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
    
    // Cancel all sales in a transaction (bill)
    suspend fun cancelSalesByTransaction(sales: List<Sale>): Boolean {
        var allSuccess = true
        for (sale in sales) {
            if (!cancelSale(sale.id)) allSuccess = false
        }
        return allSuccess
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
    
    // Cash Reconciliation operations
    fun getCashReconciliationByDate(date: String): Flow<CashReconciliation?> = 
        cashReconciliationDao.getByDate(date)
    
    suspend fun getCashReconciliationByDateSync(date: String): CashReconciliation? = 
        cashReconciliationDao.getByDateSync(date)
    
    suspend fun getPreviousDayReconciliation(date: String): CashReconciliation? = 
        cashReconciliationDao.getPreviousDay(date)
    
    suspend fun saveCashReconciliation(reconciliation: CashReconciliation) = 
        cashReconciliationDao.insertOrUpdate(reconciliation)
    
    fun getAllCashReconciliations(): Flow<List<CashReconciliation>> = 
        cashReconciliationDao.getAll()
    
    // Supply operations
    fun getAllSupplies(): Flow<List<Supply>> = supplyDao?.getAllSupplies() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    
    suspend fun getAllSuppliesList(): List<Supply> = supplyDao?.getAllSuppliesList() ?: emptyList()
    
    suspend fun getSupplyById(id: Long): Supply? = supplyDao?.getSupplyById(id)
    
    fun getLowStockSupplies(): Flow<List<Supply>> = supplyDao?.getLowStockSupplies() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    
    suspend fun insertSupply(supply: Supply): Long = supplyDao?.insertSupply(supply) ?: 0L
    
    suspend fun updateSupply(supply: Supply) = supplyDao?.updateSupply(supply)
    
    suspend fun deleteSupply(supply: Supply) {
        // Delete all links to this supply first
        productSupplyLinkDao?.deleteLinksForSupply(supply.id)
        supplyDao?.deleteSupply(supply)
    }
    
    suspend fun incrementSupplyQuantity(supplyId: Long, amount: Double) = supplyDao?.incrementQuantity(supplyId, amount)
    
    suspend fun decrementSupplyQuantity(supplyId: Long, amount: Double) = supplyDao?.decrementQuantity(supplyId, amount)
    
    suspend fun setSupplyQuantity(supplyId: Long, newQuantity: Double) = supplyDao?.setQuantity(supplyId, newQuantity)
    
    // Product-Supply Link operations
    suspend fun getSupplyLinksForProduct(productId: Long): List<ProductSupplyLink> = 
        productSupplyLinkDao?.getLinksForProduct(productId) ?: emptyList()
    
    fun getSuppliesForProduct(productId: Long): Flow<List<Supply>> = 
        productSupplyLinkDao?.getSuppliesForProduct(productId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    
    fun getProductsForSupply(supplyId: Long): Flow<List<Product>> = 
        productSupplyLinkDao?.getProductsForSupply(supplyId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    
    suspend fun addSupplyLink(link: ProductSupplyLink) = productSupplyLinkDao?.insertLink(link)
    
    suspend fun removeSupplyLink(link: ProductSupplyLink) = productSupplyLinkDao?.deleteLink(link)
    
    suspend fun updateSupplyLinksForProduct(productId: Long, links: List<ProductSupplyLink>) = 
        productSupplyLinkDao?.updateLinksForProduct(productId, links)
    
    // Deduct supplies when a product is sold
    suspend fun deductSuppliesForSale(productId: Long, quantitySold: Int) {
        val links = productSupplyLinkDao?.getLinksForProduct(productId) ?: return
        for (link in links) {
            val consumedAmount = link.quantityConsumed * quantitySold
            supplyDao?.decrementQuantity(link.supplyId, consumedAmount)
        }
    }
    
    // Purchase Bill operations
    fun getAllPurchaseBills(): Flow<List<PurchaseBill>> = 
        purchaseBillDao?.getAllBills() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    
    fun getAllPurchaseBillsWithItems(): Flow<List<PurchaseBillWithItems>> = 
        purchaseBillDao?.getAllBillsWithItems() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    
    suspend fun getPurchaseBillWithItems(billId: Long): PurchaseBillWithItems? = 
        purchaseBillDao?.getBillWithItems(billId)
    
    fun getPurchaseBillsForDateRange(startDate: Date, endDate: Date): Flow<List<PurchaseBill>> = 
        purchaseBillDao?.getBillsForDateRange(startDate, endDate) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    
    suspend fun getTotalPurchasesForDateRange(startDate: Date, endDate: Date): Double = 
        purchaseBillDao?.getTotalPurchasesForDateRange(startDate, endDate) ?: 0.0
    
    suspend fun insertPurchaseBill(bill: PurchaseBill): Long = purchaseBillDao?.insertBill(bill) ?: 0L
    
    suspend fun deletePurchaseBill(bill: PurchaseBill) = purchaseBillDao?.deleteBillWithItems(bill)
    
    suspend fun insertPurchaseBillWithItems(bill: PurchaseBill, items: List<PurchaseItem>): Long = 
        purchaseBillDao?.insertBillWithItems(bill, items) ?: 0L
    
    // Complete purchase flow: create bill, update inventory/supplies, optionally record expense
    suspend fun recordPurchase(
        bill: PurchaseBill,
        items: List<PurchaseItem>,
        recordAsExpense: Boolean = true
    ): Long {
        // Insert the bill and items
        val billId = insertPurchaseBillWithItems(bill, items)
        
        // Update inventory/supplies based on item types
        for (item in items) {
            when (item.itemType) {
                PurchaseItemType.PRODUCT -> {
                    item.itemId?.let { productId ->
                        val product = getProductById(productId)
                        product?.let {
                            updateProductQuantity(productId, it.quantityInStock + item.quantity.toInt())
                        }
                    }
                }
                PurchaseItemType.SUPPLY -> {
                    item.itemId?.let { supplyId ->
                        incrementSupplyQuantity(supplyId, item.quantity)
                    }
                }
            }
        }
        
        // Optionally record as expense
        if (recordAsExpense && bill.totalAmount > 0) {
            val expenseCategory = if (items.any { it.itemType == PurchaseItemType.SUPPLY }) {
                "Supplies"
            } else {
                "Inventory Purchase"
            }
            val expense = Expense(
                description = bill.supplierName?.let { "Purchase from $it" } ?: "Inventory Purchase",
                amount = bill.totalAmount,
                category = expenseCategory,
                date = bill.date
            )
            insertExpense(expense)
        }
        
        return billId
    }
}
