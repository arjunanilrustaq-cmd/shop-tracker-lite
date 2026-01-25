package com.example.shoptracklite.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoptracklite.data.PaymentMethod
import com.example.shoptracklite.data.Product
import com.example.shoptracklite.data.ShopTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SalesUiState(
    val products: List<Product> = emptyList(),
    val selectedProduct: Product? = null,
    val quantity: String = "",
    val totalAmount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class SalesViewModel(
    private val repository: ShopTrackRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesUiState())
    val uiState: StateFlow<SalesUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            try {
                repository.getAllProducts().collect { products ->
                    _uiState.value = _uiState.value.copy(
                        products = products,
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

    fun selectProduct(product: Product) {
        _uiState.value = _uiState.value.copy(
            selectedProduct = product,
            quantity = "",
            totalAmount = 0.0,
            paymentMethod = PaymentMethod.CASH,
            errorMessage = null
        )
    }

    fun updateQuantity(quantity: String) {
        _uiState.value = _uiState.value.copy(
            quantity = quantity,
            errorMessage = null
        )
        calculateTotal()
    }

    fun updatePaymentMethod(paymentMethod: PaymentMethod) {
        _uiState.value = _uiState.value.copy(
            paymentMethod = paymentMethod,
            errorMessage = null
        )
    }

    private fun calculateTotal() {
        val selectedProduct = _uiState.value.selectedProduct
        val quantity = _uiState.value.quantity.toIntOrNull() ?: 0
        
        if (selectedProduct != null && quantity > 0) {
            viewModelScope.launch {
                val total = calculateItemTotal(selectedProduct, quantity)
                _uiState.value = _uiState.value.copy(totalAmount = total)
            }
        } else {
            _uiState.value = _uiState.value.copy(totalAmount = 0.0)
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

    fun recordSale() {
        val selectedProduct = _uiState.value.selectedProduct
        val quantity = _uiState.value.quantity.toIntOrNull()
        
        if (selectedProduct == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select a product")
            return
        }
        
        if (quantity == null || quantity <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid quantity")
            return
        }
        
        if (quantity > selectedProduct.quantityInStock) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Insufficient stock. Available: ${selectedProduct.quantityInStock}"
            )
            return
        }
        
        viewModelScope.launch {
            try {
                val success = repository.recordSale(selectedProduct.id, quantity, _uiState.value.paymentMethod, isWholesale = false)
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        selectedProduct = null,
                        quantity = "",
                        totalAmount = 0.0,
                        paymentMethod = PaymentMethod.CASH,
                        successMessage = "Sale recorded successfully!",
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to record sale"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to record sale"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
