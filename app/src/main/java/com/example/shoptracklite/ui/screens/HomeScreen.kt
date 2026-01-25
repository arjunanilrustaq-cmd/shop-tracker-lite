package com.example.shoptracklite.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.shoptracklite.data.*
import com.example.shoptracklite.viewmodel.HomeViewModel
import com.example.shoptracklite.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    repository: ShopTrackRepository,
    viewModel: HomeViewModel = viewModel { HomeViewModel(repository) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showBarcodeScanner by remember { mutableStateOf(false) }
    
    // Drag and drop state for favorites reordering
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with Cart Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Checkout",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                // Cart Button
                FloatingActionButton(
                    onClick = { viewModel.toggleCart() },
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Badge(
                        modifier = Modifier.offset(x = 12.dp, y = (-12).dp),
                        containerColor = MaterialTheme.colorScheme.error
                    ) {
                        Text(uiState.cartItems.size.toString())
                    }
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Cart",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar with Barcode Scanner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    label = { Text("Search products...") },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true
                )
                FloatingActionButton(
                    onClick = { showBarcodeScanner = true },
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Barcode"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error/Success Messages
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearMessages() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            uiState.successMessage?.let { success ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = success,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearMessages() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Search Results
                    if (uiState.searchQuery.isNotBlank()) {
                        item {
                            Text(
                                text = "Search Results",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (uiState.searchResults.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = "No Results",
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No products found",
                                            style = MaterialTheme.typography.headlineSmall,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Try a different search term",
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(uiState.searchResults) { product ->
                                ProductCard(
                                    product = product,
                                    currencyCode = uiState.currencyCode,
                                    onAddToCart = { quantity -> viewModel.addToCart(product, quantity) },
                                    onToggleFavorite = { 
                                        if (uiState.favoriteProducts.any { it.id == product.id }) {
                                            viewModel.removeFromFavorites(product)
                                        } else {
                                            viewModel.addToFavorites(product)
                                        }
                                    },
                                    isFavorite = uiState.favoriteProducts.any { it.id == product.id }
                                )
                            }
                        }
                    } else {
                        // Favorites Section
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Hot Selling Items",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap star to add/remove",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        if (uiState.favoriteProducts.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "No Favorites",
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No favorite items yet",
                                            style = MaterialTheme.typography.headlineSmall,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Search for products and tap the star to add them to favorites",
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                                // Vertical scrollable list for hot selling items with drag-and-drop
                            itemsIndexed(
                                items = uiState.favoriteProducts,
                                key = { _, product -> product.id }
                            ) { index, product ->
                                val isDragging = draggedItemIndex == index
                                val elevation by animateDpAsState(
                                    targetValue = if (isDragging) 8.dp else 4.dp,
                                    label = "elevation"
                                )
                                
                                VerticalFavoriteProductCard(
                                    product = product,
                                    currencyCode = uiState.currencyCode,
                                    onAddToCart = { 
                                        viewModel.addToCart(product, 1)
                                        Toast.makeText(context, "${product.name} added to cart", Toast.LENGTH_SHORT).show()
                                    },
                                    onToggleFavorite = { viewModel.removeFromFavorites(product) },
                                    isFavorite = true,
                                    isDragging = isDragging,
                                    elevation = elevation,
                                    onDragStart = { draggedItemIndex = index },
                                    onDrag = { delta ->
                                        dragOffset += delta
                                        val itemHeight = 100 // Approximate item height in dp
                                        val targetIndex = (index + (dragOffset / itemHeight).toInt())
                                            .coerceIn(0, uiState.favoriteProducts.size - 1)
                                        if (targetIndex != index) {
                                            viewModel.reorderFavorites(index, targetIndex)
                                            draggedItemIndex = targetIndex
                                            dragOffset = 0f
                                        }
                                    },
                                    onDragEnd = {
                                        draggedItemIndex = null
                                        dragOffset = 0f
                                    }
                                )
                            }
                            
                            // Hint for reordering
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Hold and drag items to reorder",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Cart Bottom Sheet
        if (uiState.showCart) {
            CartBottomSheet(
                cartItems = uiState.cartItems,
                cartTotal = uiState.cartTotal,
                cartWholesaleTotal = uiState.cartWholesaleTotal,
                discount = uiState.discount,
                paymentMethod = uiState.paymentMethod,
                wholesaleModeEnabled = uiState.wholesaleModeEnabled,
                customerName = uiState.customerName,
                currencyCode = uiState.currencyCode,
                onUpdateQuantity = { productId, quantity -> 
                    viewModel.updateCartItemQuantity(productId, quantity) 
                },
                onRemoveItem = { productId -> 
                    viewModel.removeFromCart(productId) 
                },
                onUpdatePaymentMethod = { paymentMethod -> 
                    viewModel.updatePaymentMethod(paymentMethod) 
                },
                onUpdateCustomerName = { name ->
                    viewModel.updateCustomerName(name)
                },
                onUpdateDiscount = { discount ->
                    viewModel.updateDiscount(discount)
                },
                onCheckout = { isWholesale -> viewModel.checkout(isWholesale) },
                onDismiss = { viewModel.toggleCart() }
            )
        }
        
        // Receipt Dialog
        uiState.receiptData?.let { receiptData ->
            if (uiState.showReceipt) {
                ReceiptDialog(
                    receiptData = receiptData,
                    currencyCode = uiState.currencyCode,
                    onDismiss = { viewModel.dismissReceipt() }
                )
            }
        }
        
        // Barcode Scanner Dialog
        if (showBarcodeScanner) {
            com.example.shoptracklite.ui.components.BarcodeScannerDialog(
                onBarcodeScanned = { barcode ->
                    viewModel.scanAndAddToCart(barcode)
                },
                onDismiss = { showBarcodeScanner = false }
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    currencyCode: String,
    onAddToCart: (Int) -> Unit,
    onToggleFavorite: () -> Unit,
    isFavorite: Boolean
) {
    var quantityText by remember { mutableStateOf(TextFieldValue("1", TextRange(0, 1))) }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Stock: ${product.quantityInStock}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (product.quantityInStock > 0) 
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = CurrencyUtils.formatCurrency(product.sellingPrice, currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Quantity input field - always visible
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { newValue ->
                        // Only allow positive integers
                        if (newValue.text.isEmpty()) {
                            // Allow empty text temporarily while user is typing
                            quantityText = newValue
                        } else if (newValue.text.all { it.isDigit() } && newValue.text.toIntOrNull() != null && newValue.text.toInt() > 0) {
                            quantityText = newValue
                        }
                    },
                    modifier = Modifier
                        .width(60.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                // Select all text when focused
                                quantityText = quantityText.copy(
                                    selection = TextRange(0, quantityText.text.length)
                                )
                            } else if (focusState.hasFocus.not()) {
                                // If user leaves field empty, set to default value
                                if (quantityText.text.isEmpty()) {
                                    quantityText = TextFieldValue("1", TextRange(0, 1))
                                }
                            }
                        },
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    label = { Text("Qty") }
                )
                
                // Cart icon for quick add (uses quantity from text field)
                IconButton(
                    onClick = {
                        val quantity = quantityText.text.toIntOrNull() ?: 1
                        onAddToCart(quantity)
                        Toast.makeText(context, "${product.name} (x$quantity) added to cart", Toast.LENGTH_SHORT).show()
                    },
                    enabled = product.quantityInStock > 0
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Add to cart",
                        tint = if (product.quantityInStock > 0) 
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun VerticalFavoriteProductCard(
    product: Product,
    currencyCode: String,
    onAddToCart: () -> Unit,
    onToggleFavorite: () -> Unit,
    isFavorite: Boolean,
    isDragging: Boolean = false,
    elevation: androidx.compose.ui.unit.Dp = 4.dp,
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation)
            .zIndex(if (isDragging) 1f else 0f)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.y)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            },
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) 
                MaterialTheme.colorScheme.surfaceVariant 
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle indicator
            Icon(
                Icons.Default.Menu,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 8.dp)
            )
            
            // Product info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stock: ${product.quantityInStock}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (product.quantityInStock > 0) 
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = CurrencyUtils.formatCurrency(product.sellingPrice, currencyCode),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Action buttons - Favourite toggle and Cart
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Favourite toggle button
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Large Cart button - one click adds 1 item
                FilledIconButton(
                    onClick = onAddToCart,
                    enabled = product.quantityInStock > 0,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (product.quantityInStock > 0) 
                            MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.AddShoppingCart,
                        contentDescription = "Add to cart",
                        tint = if (product.quantityInStock > 0) 
                            MaterialTheme.colorScheme.onPrimary 
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBottomSheet(
    cartItems: List<CartItem>,
    cartTotal: Double,
    cartWholesaleTotal: Double,
    discount: Double,
    paymentMethod: PaymentMethod,
    wholesaleModeEnabled: Boolean,
    customerName: String,
    currencyCode: String,
    onUpdateQuantity: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onUpdatePaymentMethod: (PaymentMethod) -> Unit,
    onUpdateCustomerName: (String) -> Unit,
    onUpdateDiscount: (Double) -> Unit,
    onCheckout: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var discountText by remember { mutableStateOf(if (discount > 0) discount.toString() else "") }
    
    // Update discount text when discount changes externally
    LaunchedEffect(discount) {
        if (discount == 0.0 && discountText.isNotEmpty()) {
            // Only reset if user cleared it
        } else if (discount > 0) {
            discountText = discount.toString()
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shopping Cart",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (cartItems.isEmpty()) {
                // Empty Cart
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Empty Cart",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your cart is empty",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Add some products to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Scrollable Cart Items - limit height to leave room for bottom section
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cartItems, key = { it.productId }) { item ->
                        CartItemRow(
                            item = item,
                            currencyCode = currencyCode,
                            onUpdateQuantity = onUpdateQuantity,
                            onRemoveItem = onRemoveItem
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Fixed bottom section - Payment, Discount, Total, Checkout
                Column {
                    // Payment Method Selection
                    Text(
                        text = "Payment Method",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        FilterChip(
                            onClick = { onUpdatePaymentMethod(PaymentMethod.CASH) },
                            label = { Text("Cash") },
                            selected = paymentMethod == PaymentMethod.CASH,
                            modifier = Modifier.weight(1f),
                            leadingIcon = if (paymentMethod == PaymentMethod.CASH) {
                                { Icon(Icons.Default.Check, contentDescription = "Selected") }
                            } else null
                        )
                        FilterChip(
                            onClick = { onUpdatePaymentMethod(PaymentMethod.VISA) },
                            label = { Text("Visa") },
                            selected = paymentMethod == PaymentMethod.VISA,
                            modifier = Modifier.weight(1f),
                            leadingIcon = if (paymentMethod == PaymentMethod.VISA) {
                                { Icon(Icons.Default.Check, contentDescription = "Selected") }
                            } else null
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Discount Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Discount (Optional)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = discountText,
                            onValueChange = { value ->
                                // Allow only numbers and decimal point
                                if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    discountText = value
                                    val discountValue = value.toDoubleOrNull() ?: 0.0
                                    onUpdateDiscount(discountValue)
                                }
                            },
                            modifier = Modifier.width(120.dp),
                            placeholder = { Text("0.00") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            prefix = { Text(CurrencyUtils.getCurrencySymbol(currencyCode)) }
                        )
                    }
                    
                    // Customer Name (if wholesale mode enabled)
                    if (wholesaleModeEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Customer Name (Optional)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = onUpdateCustomerName,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter customer name") },
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Total and Checkout
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Show both retail and wholesale totals if wholesale mode enabled
                            if (wholesaleModeEnabled) {
                                // Retail section
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Retail Subtotal:",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = CurrencyUtils.formatCurrency(cartTotal, currencyCode),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                
                                if (discount > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Discount:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF4CAF50)
                                        )
                                        Text(
                                            text = "-${CurrencyUtils.formatCurrency(discount, currencyCode)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Retail Total:",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = CurrencyUtils.formatCurrency(maxOf(0.0, cartTotal - discount), currencyCode),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = { onCheckout(false) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Checkout (Retail)", style = MaterialTheme.typography.titleMedium)
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                HorizontalDivider()
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Wholesale section
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Wholesale Subtotal:",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = CurrencyUtils.formatCurrency(cartWholesaleTotal, currencyCode),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                
                                if (discount > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Discount:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF4CAF50)
                                        )
                                        Text(
                                            text = "-${CurrencyUtils.formatCurrency(discount, currencyCode)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Wholesale Total:",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = CurrencyUtils.formatCurrency(maxOf(0.0, cartWholesaleTotal - discount), currencyCode),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Button(
                                    onClick = { onCheckout(true) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text("Checkout (Wholesale)", style = MaterialTheme.typography.titleMedium)
                                }
                            } else {
                                // Regular mode - show subtotal, discount, total
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Subtotal:",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = CurrencyUtils.formatCurrency(cartTotal, currencyCode),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                
                                if (discount > 0) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Discount:",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF4CAF50)
                                        )
                                        Text(
                                            text = "-${CurrencyUtils.formatCurrency(discount, currencyCode)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Total:",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = CurrencyUtils.formatCurrency(maxOf(0.0, cartTotal - discount), currencyCode),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Button(
                                    onClick = { onCheckout(false) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Checkout", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    currencyCode: String,
    onUpdateQuantity: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var textFieldValue by remember { mutableStateOf(item.quantity.toString()) }
    val focusRequester = remember { FocusRequester() }
    
    // Update text field when quantity changes externally (e.g., via +/- buttons)
    LaunchedEffect(item.quantity) {
        if (!isEditing) {
            textFieldValue = item.quantity.toString()
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = CurrencyUtils.formatCurrency(item.unitPrice, currencyCode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onUpdateQuantity(item.productId, item.quantity - 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Decrease quantity",
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Quantity Text Field
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { newValue: String ->
                        isEditing = true
                        textFieldValue = newValue
                        val quantity = newValue.toIntOrNull() ?: 0
                        if (quantity > 0) {
                            onUpdateQuantity(item.productId, quantity)
                        }
                    },
                    modifier = Modifier
                        .width(60.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused && !isEditing) {
                                // When user clicks to focus, clear the field so they can type over it
                                isEditing = true
                                textFieldValue = ""
                            } else if (!focusState.isFocused) {
                                isEditing = false
                                // Validate and set a minimum value of 1 if empty or invalid
                                val quantity = textFieldValue.toIntOrNull()
                                if (quantity == null || quantity <= 0) {
                                    textFieldValue = "1"
                                    onUpdateQuantity(item.productId, 1)
                                }
                            }
                        },
                    textStyle = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
                
                IconButton(
                    onClick = { onUpdateQuantity(item.productId, item.quantity + 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Increase quantity",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Text(
                text = CurrencyUtils.formatCurrency(item.totalAmount, currencyCode),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
            
            IconButton(
                onClick = { onRemoveItem(item.productId) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove item",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
