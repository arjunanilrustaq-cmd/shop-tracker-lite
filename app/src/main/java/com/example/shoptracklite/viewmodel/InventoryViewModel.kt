package com.example.shoptracklite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoptracklite.data.Product
import com.example.shoptracklite.data.PriceRange
import com.example.shoptracklite.data.ShopTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InventoryUiState(
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
    val wholesaleModeEnabled: Boolean = false,
    val currencyCode: String = "USD"
)

class InventoryViewModel(
    private val repository: ShopTrackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
        loadSettings()
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

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
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
                        errorMessage = "No product found with barcode: $barcode"
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
}
