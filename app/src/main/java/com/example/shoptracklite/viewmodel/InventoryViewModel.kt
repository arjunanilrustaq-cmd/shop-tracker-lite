package com.example.shoptracklite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoptracklite.data.Category
import com.example.shoptracklite.data.Product
import com.example.shoptracklite.data.PriceRange
import com.example.shoptracklite.data.ShopTrackRepository
import com.example.shoptracklite.data.Supply
import com.example.shoptracklite.data.ProductSupplyLink
import com.example.shoptracklite.data.PurchaseBill
import com.example.shoptracklite.data.PurchaseBillWithItems
import com.example.shoptracklite.data.PurchaseItem
import com.example.shoptracklite.data.PurchaseItemType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InventoryUiState(
    // Tab selection
    val selectedTab: Int = 0,  // 0 = Products, 1 = Supplies, 2 = Purchases
    
    // Products state
    val products: List<Product> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val showAddDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val selectedProduct: Product? = null,
    val showDeleteConfirmation: Boolean = false,
    val productToDelete: Product? = null,
    val showShareDialog: Boolean = false,
    val showRestockDialog: Boolean = false,
    val showProductDetailsDialog: Boolean = false,
    val scannedProduct: Product? = null,
    val scannedBarcodeForNewProduct: String? = null,
    val initialBarcodeForAdd: String? = null,
    val wholesaleModeEnabled: Boolean = false,
    val currencyCode: String = "USD",
    val categories: List<Category> = emptyList(),
    
    // Supplies state
    val supplies: List<Supply> = emptyList(),
    val filteredSupplies: List<Supply> = emptyList(),
    val suppliesSearchQuery: String = "",
    val showAddSupplyDialog: Boolean = false,
    val showEditSupplyDialog: Boolean = false,
    val selectedSupply: Supply? = null,
    val showDeleteSupplyConfirmation: Boolean = false,
    val supplyToDelete: Supply? = null,
    val showLinkSupplyDialog: Boolean = false,
    val productForLinking: Product? = null,
    val existingLinks: List<ProductSupplyLink> = emptyList(),
    
    // Purchases state
    val purchaseBills: List<PurchaseBillWithItems> = emptyList(),
    val showAddPurchaseDialog: Boolean = false,
    val showPurchaseDetailsDialog: Boolean = false,
    val selectedPurchaseBill: PurchaseBillWithItems? = null
)

class InventoryViewModel(
    private val repository: ShopTrackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
        loadCategories()
        loadSupplies()
        loadPurchaseBills()
        loadSettings()
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                repository.getAllCategories().collect { categories ->
                    _uiState.value = _uiState.value.copy(categories = categories)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    fun selectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                repository.getSettings().collect { settings ->
                    _uiState.value = _uiState.value.copy(
                        wholesaleModeEnabled = settings?.wholesaleModeEnabled ?: false,
                        currencyCode = settings?.currencyCode ?: "USD"
                    )
                }
            } catch (e: Exception) {
                // Ignore settings errors
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            try {
                repository.getAllProducts().collect { products ->
                    _uiState.value = _uiState.value.copy(
                        products = products,
                        filteredProducts = filterProducts(products, _uiState.value.searchQuery),
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    private fun filterProducts(products: List<Product>, query: String): List<Product> {
        if (query.isBlank()) return products
        return products.filter { product ->
            product.name.contains(query, ignoreCase = true) ||
            product.barcode?.contains(query, ignoreCase = true) == true
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredProducts = filterProducts(_uiState.value.products, query)
        )
    }

    fun addProduct(product: Product, priceRanges: List<PriceRange>) {
        viewModelScope.launch {
            try {
                val productId = repository.insertProduct(product)
                
                // Save price ranges if quantity-based pricing is enabled
                if (product.hasQuantityBasedPricing && priceRanges.isNotEmpty()) {
                    val rangesWithProductId = priceRanges.map { it.copy(productId = productId) }
                    repository.insertPriceRanges(rangesWithProductId)
                }
                
                _uiState.value = _uiState.value.copy(showAddDialog = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun updateProduct(product: Product, priceRanges: List<PriceRange>) {
        viewModelScope.launch {
            try {
                repository.updateProduct(product)
                
                // Update price ranges
                repository.deletePriceRangesByProduct(product.id)
                if (product.hasQuantityBasedPricing && priceRanges.isNotEmpty()) {
                    val rangesWithProductId = priceRanges.map { it.copy(productId = product.id) }
                    repository.insertPriceRanges(rangesWithProductId)
                }
                
                _uiState.value = _uiState.value.copy(
                    showEditDialog = false,
                    selectedProduct = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun showDeleteConfirmation(product: Product) {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = true,
            productToDelete = product
        )
    }

    fun confirmDelete() {
        val product = _uiState.value.productToDelete
        if (product != null) {
            viewModelScope.launch {
                try {
                    repository.deleteProduct(product)
                    _uiState.value = _uiState.value.copy(
                        showDeleteConfirmation = false,
                        productToDelete = null
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(errorMessage = e.message)
                }
            }
        }
    }

    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(
            showDeleteConfirmation = false,
            productToDelete = null
        )
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }
    
    fun addCategory(name: String) {
        viewModelScope.launch {
            try {
                repository.insertCategory(Category(name = name.trim()))
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun showAddDialogWithBarcode(barcode: String) {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            initialBarcodeForAdd = barcode,
            scannedBarcodeForNewProduct = null
        )
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = false,
            initialBarcodeForAdd = null
        )
    }

    fun dismissBarcodeNotFoundDialog() {
        _uiState.value = _uiState.value.copy(scannedBarcodeForNewProduct = null)
    }

    fun showEditDialog(product: Product) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            selectedProduct = product
        )
    }

    fun hideEditDialog() {
        _uiState.value = _uiState.value.copy(
            showEditDialog = false,
            selectedProduct = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun showShareDialog() {
        _uiState.value = _uiState.value.copy(showShareDialog = true)
    }

    fun hideShareDialog() {
        _uiState.value = _uiState.value.copy(showShareDialog = false)
    }

    fun showRestockDialog() {
        _uiState.value = _uiState.value.copy(showRestockDialog = true)
    }

    fun hideRestockDialog() {
        _uiState.value = _uiState.value.copy(showRestockDialog = false)
    }

    fun restockProduct(product: Product, quantityToAdd: Int) {
        viewModelScope.launch {
            try {
                val updatedProduct = product.copy(
                    quantityInStock = product.quantityInStock + quantityToAdd
                )
                repository.updateProduct(updatedProduct)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun scanBarcodeForDetails(barcode: String) {
        viewModelScope.launch {
            try {
                val product = repository.getProductByBarcode(barcode)
                if (product != null) {
                    _uiState.value = _uiState.value.copy(
                        scannedProduct = product,
                        showProductDetailsDialog = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        scannedBarcodeForNewProduct = barcode
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun showProductDetails(product: Product) {
        _uiState.value = _uiState.value.copy(
            scannedProduct = product,
            showProductDetailsDialog = true
        )
    }

    fun hideProductDetailsDialog() {
        _uiState.value = _uiState.value.copy(
            scannedProduct = null,
            showProductDetailsDialog = false
        )
    }
    
    // ==================== SUPPLIES ====================
    
    private fun loadSupplies() {
        viewModelScope.launch {
            try {
                repository.getAllSupplies().collect { supplies ->
                    _uiState.value = _uiState.value.copy(
                        supplies = supplies,
                        filteredSupplies = filterSupplies(supplies, _uiState.value.suppliesSearchQuery)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    private fun filterSupplies(supplies: List<Supply>, query: String): List<Supply> {
        if (query.isBlank()) return supplies
        return supplies.filter { supply ->
            supply.name.contains(query, ignoreCase = true)
        }
    }
    
    fun updateSuppliesSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            suppliesSearchQuery = query,
            filteredSupplies = filterSupplies(_uiState.value.supplies, query)
        )
    }
    
    fun showAddSupplyDialog() {
        _uiState.value = _uiState.value.copy(showAddSupplyDialog = true)
    }
    
    fun hideAddSupplyDialog() {
        _uiState.value = _uiState.value.copy(showAddSupplyDialog = false)
    }
    
    fun showEditSupplyDialog(supply: Supply) {
        _uiState.value = _uiState.value.copy(
            showEditSupplyDialog = true,
            selectedSupply = supply
        )
    }
    
    fun hideEditSupplyDialog() {
        _uiState.value = _uiState.value.copy(
            showEditSupplyDialog = false,
            selectedSupply = null
        )
    }
    
    fun addSupply(supply: Supply) {
        viewModelScope.launch {
            try {
                repository.insertSupply(supply)
                _uiState.value = _uiState.value.copy(showAddSupplyDialog = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun updateSupply(supply: Supply) {
        viewModelScope.launch {
            try {
                repository.updateSupply(supply)
                _uiState.value = _uiState.value.copy(
                    showEditSupplyDialog = false,
                    selectedSupply = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun showDeleteSupplyConfirmation(supply: Supply) {
        _uiState.value = _uiState.value.copy(
            showDeleteSupplyConfirmation = true,
            supplyToDelete = supply
        )
    }
    
    fun confirmDeleteSupply() {
        val supply = _uiState.value.supplyToDelete
        if (supply != null) {
            viewModelScope.launch {
                try {
                    repository.deleteSupply(supply)
                    _uiState.value = _uiState.value.copy(
                        showDeleteSupplyConfirmation = false,
                        supplyToDelete = null
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(errorMessage = e.message)
                }
            }
        }
    }
    
    fun cancelDeleteSupply() {
        _uiState.value = _uiState.value.copy(
            showDeleteSupplyConfirmation = false,
            supplyToDelete = null
        )
    }
    
    fun adjustSupplyQuantity(supply: Supply, newQuantity: Double) {
        viewModelScope.launch {
            try {
                repository.setSupplyQuantity(supply.id, newQuantity)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    // Product-Supply Linking
    fun showLinkSupplyDialog(product: Product) {
        viewModelScope.launch {
            try {
                val existingLinks = repository.getSupplyLinksForProduct(product.id)
                _uiState.value = _uiState.value.copy(
                    showLinkSupplyDialog = true,
                    productForLinking = product,
                    existingLinks = existingLinks
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun hideLinkSupplyDialog() {
        _uiState.value = _uiState.value.copy(
            showLinkSupplyDialog = false,
            productForLinking = null,
            existingLinks = emptyList()
        )
    }
    
    fun saveSupplyLinks(productId: Long, links: List<ProductSupplyLink>) {
        viewModelScope.launch {
            try {
                repository.updateSupplyLinksForProduct(productId, links)
                _uiState.value = _uiState.value.copy(
                    showLinkSupplyDialog = false,
                    productForLinking = null,
                    existingLinks = emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    // ==================== PURCHASES ====================
    
    private fun loadPurchaseBills() {
        viewModelScope.launch {
            try {
                repository.getAllPurchaseBillsWithItems().collect { bills ->
                    _uiState.value = _uiState.value.copy(purchaseBills = bills)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun showAddPurchaseDialog() {
        _uiState.value = _uiState.value.copy(showAddPurchaseDialog = true)
    }
    
    fun hideAddPurchaseDialog() {
        _uiState.value = _uiState.value.copy(showAddPurchaseDialog = false)
    }
    
    fun showPurchaseDetails(bill: PurchaseBillWithItems) {
        _uiState.value = _uiState.value.copy(
            showPurchaseDetailsDialog = true,
            selectedPurchaseBill = bill
        )
    }
    
    fun hidePurchaseDetailsDialog() {
        _uiState.value = _uiState.value.copy(
            showPurchaseDetailsDialog = false,
            selectedPurchaseBill = null
        )
    }
    
    fun recordPurchase(
        bill: PurchaseBill,
        items: List<PurchaseItem>,
        recordAsExpense: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                repository.recordPurchase(bill, items, recordAsExpense)
                _uiState.value = _uiState.value.copy(showAddPurchaseDialog = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    // Data class for new product info from purchase
    data class NewProductInfo(
        val name: String,
        val costPrice: Double,
        val sellingPrice: Double,
        val quantity: Int,
        val barcode: String?
    )
    
    // Record purchase with new product creation
    fun recordPurchaseWithNewProducts(
        bill: PurchaseBill,
        items: List<PurchaseItem>,
        newProductInfos: List<Pair<Int, NewProductInfo>>, // Pair of item index to new product info
        recordAsExpense: Boolean = true
    ) {
        viewModelScope.launch {
            try {
                // Create new products first and get their IDs
                val updatedItems = items.toMutableList()
                for ((index, productInfo) in newProductInfos) {
                    val product = Product(
                        name = productInfo.name,
                        costPrice = productInfo.costPrice,
                        sellingPrice = productInfo.sellingPrice,
                        quantityInStock = productInfo.quantity,
                        barcode = productInfo.barcode?.ifBlank { null }
                    )
                    val productId = repository.insertProduct(product)
                    // Update the item with the new product ID
                    updatedItems[index] = updatedItems[index].copy(itemId = productId)
                }
                
                // Now record the purchase with updated items
                repository.recordPurchase(bill, updatedItems, recordAsExpense)
                _uiState.value = _uiState.value.copy(showAddPurchaseDialog = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun deletePurchaseBill(bill: PurchaseBill) {
        viewModelScope.launch {
            try {
                repository.deletePurchaseBill(bill)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    // Create new product from purchase
    fun createProductFromPurchase(
        name: String,
        costPrice: Double,
        sellingPrice: Double,
        quantity: Int,
        barcode: String? = null
    ): Long {
        var productId = 0L
        viewModelScope.launch {
            try {
                val product = Product(
                    name = name,
                    costPrice = costPrice,
                    sellingPrice = sellingPrice,
                    quantityInStock = quantity,
                    barcode = barcode?.ifBlank { null }
                )
                productId = repository.insertProduct(product)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
        return productId
    }
    
    // Create new supply from purchase
    fun createSupplyFromPurchase(
        name: String,
        quantity: Double,
        unit: String,
        costPerUnit: Double
    ): Long {
        var supplyId = 0L
        viewModelScope.launch {
            try {
                val supply = Supply(
                    name = name,
                    quantity = quantity,
                    unit = unit,
                    costPerUnit = costPerUnit
                )
                supplyId = repository.insertSupply(supply)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
        return supplyId
    }
}
