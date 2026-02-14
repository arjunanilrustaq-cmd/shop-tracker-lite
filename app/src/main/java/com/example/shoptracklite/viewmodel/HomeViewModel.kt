package com.example.shoptracklite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoptracklite.data.Category
import com.example.shoptracklite.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ReceiptData(
    val items: List<CartItem>,
    val subtotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    val paymentMethod: PaymentMethod,
    val isWholesale: Boolean = false,
    val customerName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val shopName: String = "",
    val crNumber: String = "",
    val amountPaid: Double? = null,
    val change: Double? = null
)

/** Represents the selected filter: FAVORITES or a category ID */
data class CategoryFilter(val isFavorites: Boolean, val categoryId: Long? = null)

data class HomeUiState(
    val favoriteProducts: List<Product> = emptyList(),
    val allProducts: List<Product> = emptyList(),
    val categories: List<com.example.shoptracklite.data.Category> = emptyList(),
    val categoryProducts: List<Product> = emptyList(),
    val selectedFilter: CategoryFilter = CategoryFilter(isFavorites = true),
    val searchQuery: String = "",
    val showSearchBar: Boolean = false,
    val cartItems: List<CartItem> = emptyList(),
    val cartTotal: Double = 0.0,
    val cartWholesaleTotal: Double = 0.0,
    val discount: Double = 0.0,
    val showCart: Boolean = false,
    val showFavoritesSheet: Boolean = false,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val amountPaid: Double = 0.0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showReceipt: Boolean = false,
    val receiptData: ReceiptData? = null,
    val wholesaleModeEnabled: Boolean = false,
    val customerName: String = "",
    val clearHotSellingInputs: Boolean = false,
    val currencyCode: String = "USD"
)

class HomeViewModel(
    private val repository: ShopTrackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadFavoriteProducts()
        loadAllProducts()
        loadCategories()
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
    
    private fun loadAllProducts() {
        viewModelScope.launch {
            try {
                repository.getAllProducts().collect { products ->
                    _uiState.value = _uiState.value.copy(allProducts = products)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
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

    private fun loadFavoriteProducts() {
        viewModelScope.launch {
            try {
                repository.getFavoriteProducts().collect { favorites ->
                    _uiState.value = _uiState.value.copy(
                        favoriteProducts = favorites,
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

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
    
    fun toggleSearchBar() {
        _uiState.value = _uiState.value.copy(
            showSearchBar = !_uiState.value.showSearchBar,
            searchQuery = if (_uiState.value.showSearchBar) "" else _uiState.value.searchQuery
        )
    }
    
    fun selectFilter(filter: CategoryFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        if (!filter.isFavorites && filter.categoryId != null) {
            viewModelScope.launch {
                try {
                    repository.getProductsByCategory(filter.categoryId).collect { products ->
                        _uiState.value = _uiState.value.copy(categoryProducts = products)
                    }
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(categoryProducts = emptyList())
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(categoryProducts = emptyList())
        }
    }

    fun addToFavorites(product: Product) {
        viewModelScope.launch {
            try {
                repository.addToFavorites(product.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun removeFromFavorites(product: Product) {
        viewModelScope.launch {
            try {
                repository.removeFromFavorites(product.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun updateFavoriteOrder(productIds: List<Long>) {
        viewModelScope.launch {
            try {
                repository.updateFavoriteOrder(productIds)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }
    
    fun reorderFavorites(fromIndex: Int, toIndex: Int) {
        val currentList = _uiState.value.favoriteProducts.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val item = currentList.removeAt(fromIndex)
            currentList.add(toIndex, item)
            _uiState.value = _uiState.value.copy(favoriteProducts = currentList)
            updateFavoriteOrder(currentList.map { it.id })
        }
    }

    fun scanAndAddToCart(barcode: String) {
        viewModelScope.launch {
            try {
                val product = repository.getProductByBarcode(barcode)
                if (product != null) {
                    if (product.trackInventory && product.quantityInStock <= 0) {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "${product.name} is out of stock"
                        )
                    } else {
                        addToCart(product, 1)
                        _uiState.value = _uiState.value.copy(
                            successMessage = "${product.name} added to cart"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Product not found with barcode: $barcode"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun addToCart(product: Product, quantity: Int = 1) {
        viewModelScope.launch {
            val currentCart = _uiState.value.cartItems.toMutableList()
            val existingItem = currentCart.find { it.productId == product.id }
            
            if (existingItem != null) {
                // Update quantity
                val newQuantity = existingItem.quantity + quantity
                val index = currentCart.indexOf(existingItem)
                val retailTotal = calculateItemTotal(product, newQuantity)
                val wholesaleTotal = if (product.wholesalePrice != null) {
                    newQuantity * product.wholesalePrice
                } else null
                
                val updatedItem = existingItem.copy(
                    quantity = newQuantity,
                    totalAmount = retailTotal,
                    wholesaleTotalAmount = wholesaleTotal
                )
                currentCart[index] = updatedItem
            } else {
                // Add new item
                val retailTotal = calculateItemTotal(product, quantity)
                val wholesaleTotal = if (product.wholesalePrice != null) {
                    quantity * product.wholesalePrice
                } else null
                
                val newItem = CartItem(
                    productId = product.id,
                    productName = product.name,
                    unitPrice = product.sellingPrice,
                    wholesalePrice = product.wholesalePrice,
                    quantity = quantity,
                    totalAmount = retailTotal,
                    wholesaleTotalAmount = wholesaleTotal,
                    imagePath = product.imagePath,
                    colorHex = product.colorHex
                )
                currentCart.add(newItem)
            }
            
            val newTotal = currentCart.sumOf { it.totalAmount }
            val newWholesaleTotal = currentCart.sumOf { it.wholesaleTotalAmount ?: it.totalAmount }
            _uiState.value = _uiState.value.copy(
                cartItems = currentCart,
                cartTotal = newTotal,
                cartWholesaleTotal = newWholesaleTotal
            )
        }
    }

    fun updateCartItemQuantity(productId: Long, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(productId)
            return
        }
        
        viewModelScope.launch {
            val currentCart = _uiState.value.cartItems.toMutableList()
            val index = currentCart.indexOfFirst { it.productId == productId }
            
            if (index != -1) {
                val item = currentCart[index]
                val product = repository.getProductById(productId)
                
                val retailTotal = if (product != null) calculateItemTotal(product, newQuantity) else newQuantity * item.unitPrice
                val wholesaleTotal = if (item.wholesalePrice != null) {
                    newQuantity * item.wholesalePrice
                } else null
                
                val updatedItem = item.copy(
                    quantity = newQuantity,
                    totalAmount = retailTotal,
                    wholesaleTotalAmount = wholesaleTotal
                )
                currentCart[index] = updatedItem
                
                val newTotal = currentCart.sumOf { it.totalAmount }
                val newWholesaleTotal = currentCart.sumOf { it.wholesaleTotalAmount ?: it.totalAmount }
                _uiState.value = _uiState.value.copy(
                    cartItems = currentCart,
                    cartTotal = newTotal,
                    cartWholesaleTotal = newWholesaleTotal
                )
            }
        }
    }

    fun removeFromCart(productId: Long) {
        val currentCart = _uiState.value.cartItems.toMutableList()
        currentCart.removeAll { it.productId == productId }
        
        val newTotal = currentCart.sumOf { it.totalAmount }
        val newWholesaleTotal = currentCart.sumOf { it.wholesaleTotalAmount ?: it.totalAmount }
        _uiState.value = _uiState.value.copy(
            cartItems = currentCart,
            cartTotal = newTotal,
            cartWholesaleTotal = newWholesaleTotal
        )
    }

    fun toggleCart() {
        _uiState.value = _uiState.value.copy(showCart = !_uiState.value.showCart)
    }
    
    fun toggleFavoritesSheet() {
        _uiState.value = _uiState.value.copy(showFavoritesSheet = !_uiState.value.showFavoritesSheet)
    }

    fun updatePaymentMethod(paymentMethod: PaymentMethod) {
        _uiState.value = _uiState.value.copy(
            paymentMethod = paymentMethod,
            amountPaid = if (paymentMethod != PaymentMethod.CASH) 0.0 else _uiState.value.amountPaid
        )
    }

    fun updateAmountPaid(amount: Double) {
        _uiState.value = _uiState.value.copy(amountPaid = maxOf(0.0, amount))
    }
    
    fun updateCustomerName(name: String) {
        _uiState.value = _uiState.value.copy(customerName = name)
    }
    
    fun updateDiscount(discount: Double) {
        _uiState.value = _uiState.value.copy(discount = maxOf(0.0, discount))
    }

    fun checkout(isWholesale: Boolean = false) {
        viewModelScope.launch {
            try {
                var allSuccessful = true
                val cartItems = _uiState.value.cartItems
                val subtotal = if (isWholesale) _uiState.value.cartWholesaleTotal else _uiState.value.cartTotal
                val discount = _uiState.value.discount
                val finalTotal = maxOf(0.0, subtotal - discount)
                val paymentMethod = _uiState.value.paymentMethod
                val customerName = _uiState.value.customerName
                val amountPaid = _uiState.value.amountPaid
                val transactionId = System.currentTimeMillis()
                
                // Distribute discount proportionally across items
                for (item in cartItems) {
                    val itemTotal = if (isWholesale) {
                        item.wholesaleTotalAmount ?: item.totalAmount
                    } else {
                        item.totalAmount
                    }
                    
                    // Calculate this item's share of the discount proportionally
                    val itemDiscountShare = if (subtotal > 0) {
                        (itemTotal / subtotal) * discount
                    } else {
                        0.0
                    }
                    
                    val success = repository.recordSale(
                        item.productId,
                        item.quantity,
                        paymentMethod,
                        isWholesale,
                        itemDiscountShare,
                        transactionId
                    )
                    if (!success) {
                        allSuccessful = false
                        break
                    }
                }
                
                if (allSuccessful) {
                    val settings = repository.getSettingsSync()
                    val cashAmountPaid = if (paymentMethod == PaymentMethod.CASH && amountPaid > 0) amountPaid else null
                    val cashChange = if (cashAmountPaid != null) maxOf(0.0, cashAmountPaid - finalTotal) else null
                    val receipt = ReceiptData(
                        items = cartItems,
                        subtotal = subtotal,
                        discount = discount,
                        total = finalTotal,
                        paymentMethod = paymentMethod,
                        isWholesale = isWholesale,
                        customerName = if (customerName.isNotBlank()) customerName else null,
                        shopName = settings.shopName,
                        crNumber = settings.crNumber,
                        amountPaid = cashAmountPaid,
                        change = cashChange
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        cartItems = emptyList(),
                        cartTotal = 0.0,
                        cartWholesaleTotal = 0.0,
                        discount = 0.0,
                        amountPaid = 0.0,
                        showCart = false,
                        showReceipt = true,
                        receiptData = receipt,
                        errorMessage = null,
                        customerName = "",
                        clearHotSellingInputs = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Some items couldn't be sold. Please check stock availability."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Checkout failed"
                )
            }
        }
    }
    
    fun dismissReceipt() {
        _uiState.value = _uiState.value.copy(
            showReceipt = false,
            receiptData = null,
            clearHotSellingInputs = false
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
    
    fun addBulkToCart(productQuantities: Map<Long, Int>) {
        viewModelScope.launch {
            val currentCart = _uiState.value.cartItems.toMutableList()
            var itemsAdded = 0
            
            productQuantities.forEach { (productId, quantity) ->
                if (quantity > 0) {
                    val product = repository.getProductById(productId)
                    if (product != null) {
                        val existingItem = currentCart.find { it.productId == productId }
                        
                        if (existingItem != null) {
                            // Update quantity
                            val newQuantity = existingItem.quantity + quantity
                            val index = currentCart.indexOf(existingItem)
                            val retailTotal = calculateItemTotal(product, newQuantity)
                            val wholesaleTotal = if (product.wholesalePrice != null) {
                                newQuantity * product.wholesalePrice
                            } else null
                            
                            val updatedItem = existingItem.copy(
                                quantity = newQuantity,
                                totalAmount = retailTotal,
                                wholesaleTotalAmount = wholesaleTotal
                            )
                            currentCart[index] = updatedItem
                        } else {
                            // Add new item
                            val retailTotal = calculateItemTotal(product, quantity)
                            val wholesaleTotal = if (product.wholesalePrice != null) {
                                quantity * product.wholesalePrice
                            } else null
                            
                            val newItem = CartItem(
                                productId = productId,
                                productName = product.name,
                                unitPrice = product.sellingPrice,
                                wholesalePrice = product.wholesalePrice,
                                quantity = quantity,
                                totalAmount = retailTotal,
                                wholesaleTotalAmount = wholesaleTotal,
                                imagePath = product.imagePath,
                                colorHex = product.colorHex
                            )
                            currentCart.add(newItem)
                        }
                        itemsAdded++
                    }
                }
            }
            
            val newTotal = currentCart.sumOf { it.totalAmount }
            val newWholesaleTotal = currentCart.sumOf { it.wholesaleTotalAmount ?: it.totalAmount }
            _uiState.value = _uiState.value.copy(
                cartItems = currentCart,
                cartTotal = newTotal,
                cartWholesaleTotal = newWholesaleTotal
            )
        }
    }
    
    private suspend fun calculateItemTotal(product: Product, quantity: Int): Double {
        return if (product.hasQuantityBasedPricing) {
            // Try to get quantity-based price first
            val quantityBasedPrice = repository.getPriceForQuantity(product.id, quantity)
            if (quantityBasedPrice != null) {
                quantity * quantityBasedPrice
            } else {
                // Fall back to default selling price if no quantity range matches
                quantity * product.sellingPrice
            }
        } else {
            // Use regular selling price for products without quantity-based pricing
            quantity * product.sellingPrice
        }
    }
}
