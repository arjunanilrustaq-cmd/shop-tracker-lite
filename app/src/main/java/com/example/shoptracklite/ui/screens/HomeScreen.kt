package com.example.shoptracklite.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import com.example.shoptracklite.ui.components.BluetoothBarcodeDetector
import com.example.shoptracklite.viewmodel.CategoryFilter
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

    // Bluetooth barcode reader support - wraps content to capture keyboard input
    BluetoothBarcodeDetector(
        enabled = !showBarcodeScanner && !uiState.showCart && !uiState.showFavoritesSheet,
        onBarcodeScanned = { barcode ->
            viewModel.scanAndAddToCart(barcode)
        }
    ) {
    // Display products based on filter: Favorites or category
    val baseProducts = when {
        uiState.selectedFilter.isFavorites -> uiState.favoriteProducts
        else -> uiState.categoryProducts
    }
    val displayProducts = if (uiState.searchQuery.isBlank()) {
        baseProducts
    } else {
        val q = uiState.searchQuery.lowercase()
        baseProducts.filter {
            it.name.lowercase().contains(q) || it.barcode?.lowercase()?.contains(q) == true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header: Ticket with item count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ticket",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.cartItems.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${uiState.cartItems.size}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { showBarcodeScanner = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                    }
                    IconButton(onClick = { viewModel.toggleFavoritesSheet() }) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Manage favorites",
                            tint = if (uiState.favoriteProducts.isNotEmpty()) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CHARGE button - prominent, always visible
            val chargeTotal = maxOf(0.0, uiState.cartTotal - uiState.discount)
            Button(
                onClick = {
                    if (uiState.cartItems.isNotEmpty()) {
                        viewModel.toggleCart()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                enabled = uiState.cartItems.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CHARGE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = CurrencyUtils.formatCurrency(chargeTotal, uiState.currencyCode),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category filter + Search bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category dropdown
                val filterLabel = when {
                    uiState.selectedFilter.isFavorites -> "Favourite items"
                    uiState.selectedFilter.categoryId != null -> {
                        uiState.categories.find { it.id == uiState.selectedFilter.categoryId }?.name ?: "Favourite items"
                    }
                    else -> "Favourite items"
                }
                var filterExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = filterExpanded,
                        onExpandedChange = { filterExpanded = it }
                    ) {
                        OutlinedCard(
                            onClick = { filterExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = filterLabel,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select category",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        ExposedDropdownMenu(
                            expanded = filterExpanded,
                            onDismissRequest = { filterExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Favourite items") },
                                onClick = {
                                    viewModel.selectFilter(CategoryFilter(isFavorites = true))
                                    filterExpanded = false
                                }
                            )
                            uiState.categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        viewModel.selectFilter(CategoryFilter(isFavorites = false, categoryId = category.id))
                                        filterExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { viewModel.toggleSearchBar() }) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (uiState.showSearchBar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (uiState.showSearchBar) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search items...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (displayProducts.isEmpty()) {
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
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No items yet",
                                        style = MaterialTheme.typography.headlineSmall,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Tap the star icon above to add items",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = displayProducts,
                            key = { product -> product.id }
                        ) { product ->
                            ProductCard(
                                product = product,
                                currencyCode = uiState.currencyCode,
                                onAddToCart = { quantity -> viewModel.addToCart(product, quantity) }
                            )
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
                amountPaid = uiState.amountPaid,
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
                onUpdateAmountPaid = { viewModel.updateAmountPaid(it) },
                onCheckout = { isWholesale -> viewModel.checkout(isWholesale) },
                onDismiss = { viewModel.toggleCart() }
            )
        }
        
        // Favorites Management Bottom Sheet
        if (uiState.showFavoritesSheet) {
            FavoritesBottomSheet(
                favoriteProducts = uiState.favoriteProducts,
                allProducts = uiState.allProducts,
                categories = uiState.categories,
                currencyCode = uiState.currencyCode,
                onAddToFavorites = { viewModel.addToFavorites(it) },
                onRemoveFromFavorites = { viewModel.removeFromFavorites(it) },
                onReorderFavorites = { from, to -> viewModel.reorderFavorites(from, to) },
                onDismiss = { viewModel.toggleFavoritesSheet() }
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
    } // End BluetoothBarcodeDetector
}

@Composable
fun ProductCard(
    product: Product,
    currencyCode: String,
    onAddToCart: (Int) -> Unit
) {
    var showQuantityDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val canAddToCart = !product.trackInventory || product.quantityInStock > 0
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (canAddToCart) {
                        onAddToCart(1)
                        Toast.makeText(context, "${product.name} added to cart", Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = { if (canAddToCart) showQuantityDialog = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductImageOrColorBox(
                imagePath = product.imagePath,
                colorHex = product.colorHex,
                context = context,
                size = 44.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = CurrencyUtils.formatCurrency(product.sellingPrice, currencyCode),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    
    if (showQuantityDialog) {
        QuantityDialog(
            productName = product.name,
            onConfirm = { qty ->
                onAddToCart(qty)
                Toast.makeText(context, "${product.name} (x$qty) added to cart", Toast.LENGTH_SHORT).show()
                showQuantityDialog = false
            },
            onDismiss = { showQuantityDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesBottomSheet(
    favoriteProducts: List<Product>,
    allProducts: List<Product>,
    categories: List<com.example.shoptracklite.data.Category> = emptyList(),
    currencyCode: String,
    onAddToFavorites: (Product) -> Unit,
    onRemoveFromFavorites: (Product) -> Unit,
    onReorderFavorites: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var accumulatedDrag by remember { mutableStateOf(0f) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    val favoriteIds = favoriteProducts.map { it.id }.toSet()
    val availableProducts = allProducts.filter { it.id !in favoriteIds }
    val productsToAdd = if (selectedCategoryId == null) {
        availableProducts
    } else {
        availableProducts.filter { it.categoryId == selectedCategoryId }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Manage Favorites",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Text(
                text = "Long-press the handle (≡) and drag to reorder. Tap items below to add.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Favorites list (reorderable)
            if (favoriteProducts.isNotEmpty()) {
                Text(
                    text = "Your favorites",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(
                        items = favoriteProducts,
                        key = { _, p -> p.id }
                    ) { index, product ->
                        val isDragging = draggedIndex == index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .pointerInput(favoriteProducts.size) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggedIndex = index
                                                accumulatedDrag = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val idx = draggedIndex ?: index
                                                accumulatedDrag = accumulatedDrag + dragAmount.y
                                                val targetIndex = (idx + (accumulatedDrag / 40f).toInt())
                                                    .coerceIn(0, favoriteProducts.size - 1)
                                                if (targetIndex != idx) {
                                                    onReorderFavorites(idx, targetIndex)
                                                    draggedIndex = targetIndex
                                                    accumulatedDrag = 0f
                                                }
                                            },
                                            onDragEnd = {
                                                draggedIndex = null
                                                accumulatedDrag = 0f
                                            },
                                            onDragCancel = {
                                                draggedIndex = null
                                                accumulatedDrag = 0f
                                            }
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = "Drag to reorder",
                                        modifier = Modifier
                                            .size(28.dp)
                                            .padding(end = 8.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    ProductImageOrColorBox(
                                        imagePath = product.imagePath,
                                        colorHex = product.colorHex,
                                        context = context,
                                        size = 36.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = CurrencyUtils.formatCurrency(product.sellingPrice, currencyCode),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onRemoveFromFavorites(product) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.RemoveCircle,
                                    contentDescription = "Remove from favorites",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Add items section with category filter
            if (categories.isNotEmpty()) {
                var catExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategoryId?.let { id ->
                            categories.find { it.id == id }?.name ?: "All categories"
                        } ?: "All categories",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filter by category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All categories") },
                            onClick = {
                                selectedCategoryId = null
                                catExpanded = false
                            }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = if (productsToAdd.isEmpty()) "No items to add" else "Tap to add to favorites",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(productsToAdd, key = { it.id }) { product ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddToFavorites(product) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProductImageOrColorBox(
                            imagePath = product.imagePath,
                            colorHex = product.colorHex,
                            context = context,
                            size = 36.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = CurrencyUtils.formatCurrency(product.sellingPrice, currencyCode),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add to favorites",
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuantityDialog(
    productName: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var quantityText by remember { mutableStateOf("1") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to cart") },
        text = {
            Column {
                Text(
                    text = "Quantity for $productName",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            quantityText = newValue
                        }
                    },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    onConfirm(qty)
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartBottomSheet(
    cartItems: List<CartItem>,
    cartTotal: Double,
    cartWholesaleTotal: Double,
    discount: Double,
    paymentMethod: PaymentMethod,
    amountPaid: Double = 0.0,
    wholesaleModeEnabled: Boolean,
    customerName: String,
    currencyCode: String,
    onUpdateQuantity: (Long, Int) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onUpdatePaymentMethod: (PaymentMethod) -> Unit,
    onUpdateCustomerName: (String) -> Unit,
    onUpdateDiscount: (Double) -> Unit,
    onUpdateAmountPaid: (Double) -> Unit = {},
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
                    
                    // Amount paid and change (Cash only)
                    if (paymentMethod == PaymentMethod.CASH) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val finalTotal = maxOf(0.0, cartTotal - discount)
                        val change = maxOf(0.0, amountPaid - finalTotal)
                        Text(
                            text = "Amount Paid",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        OutlinedTextField(
                            value = if (amountPaid > 0) amountPaid.toString() else "",
                            onValueChange = { value ->
                                if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    val v = value.toDoubleOrNull() ?: 0.0
                                    onUpdateAmountPaid(v)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter amount received") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            prefix = { Text(CurrencyUtils.getCurrencySymbol(currencyCode)) }
                        )
                        if (amountPaid > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Change to give:",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = CurrencyUtils.formatCurrency(change, currencyCode),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
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
    val context = LocalContext.current
    
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
            ProductImageOrColorBox(
                imagePath = item.imagePath,
                colorHex = item.colorHex,
                context = context,
                size = 36.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
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
