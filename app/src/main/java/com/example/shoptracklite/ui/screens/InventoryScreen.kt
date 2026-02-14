package com.example.shoptracklite.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shoptracklite.data.Category
import com.example.shoptracklite.data.Product
import com.example.shoptracklite.data.ShopTrackRepository
import com.example.shoptracklite.ui.components.BluetoothBarcodeDetector
import com.example.shoptracklite.viewmodel.InventoryViewModel
import com.example.shoptracklite.utils.CurrencyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    repository: ShopTrackRepository,
    viewModel: InventoryViewModel = viewModel { InventoryViewModel(repository) }
) {
    val uiState by viewModel.uiState.collectAsState()
    var showBarcodeScanner by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = uiState.selectedTab) { 3 }
    val coroutineScope = rememberCoroutineScope()
    val tabs = listOf("Products", "Supplies", "Purchases")

    // Sync pager with tab selection
    LaunchedEffect(pagerState.currentPage) {
        viewModel.selectTab(pagerState.currentPage)
    }

    // Bluetooth barcode reader support - wraps content to capture keyboard input
    BluetoothBarcodeDetector(
        enabled = !showBarcodeScanner && !uiState.showAddDialog && !uiState.showAddPurchaseDialog && uiState.scannedBarcodeForNewProduct == null,
        onBarcodeScanned = { barcode ->
            viewModel.scanBarcodeForDetails(barcode)
        }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Inventory",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pagerState.currentPage == 0) {
                    IconButton(onClick = { viewModel.showShareDialog() }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share Inventory",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                FloatingActionButton(
                    onClick = {
                        when (pagerState.currentPage) {
                            0 -> viewModel.showAddDialog()
                            1 -> viewModel.showAddSupplyDialog()
                            2 -> viewModel.showAddPurchaseDialog()
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Row
        TabRow(
            selectedTabIndex = pagerState.currentPage
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { 
                        Text(
                            text = title,
                            fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = when (index) {
                                0 -> Icons.Default.Inventory
                                1 -> Icons.Default.Inventory2
                                else -> Icons.Default.Receipt
                            },
                            contentDescription = title
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error Message
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
                    IconButton(onClick = { viewModel.clearError() }) {
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

        // Pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> ProductsTab(
                    products = uiState.products,
                    filteredProducts = uiState.filteredProducts,
                    searchQuery = uiState.searchQuery,
                    currencyCode = uiState.currencyCode,
                    isLoading = uiState.isLoading,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    onScanBarcode = { showBarcodeScanner = true },
                    onEditProduct = { viewModel.showEditDialog(it) },
                    onDeleteProduct = { viewModel.showDeleteConfirmation(it) },
                    onLinkSupplies = { viewModel.showLinkSupplyDialog(it) },
                    onRestock = { viewModel.showRestockDialog() }
                )
                1 -> SuppliesTab(
                    supplies = uiState.supplies,
                    filteredSupplies = uiState.filteredSupplies,
                    searchQuery = uiState.suppliesSearchQuery,
                    currencyCode = uiState.currencyCode,
                    onSearchQueryChange = { viewModel.updateSuppliesSearchQuery(it) },
                    onAddSupply = { viewModel.showAddSupplyDialog() },
                    onEditSupply = { viewModel.showEditSupplyDialog(it) },
                    onDeleteSupply = { viewModel.showDeleteSupplyConfirmation(it) },
                    onAdjustQuantity = { supply, qty -> viewModel.adjustSupplyQuantity(supply, qty) }
                )
                2 -> PurchasesTab(
                    purchaseBills = uiState.purchaseBills,
                    currencyCode = uiState.currencyCode,
                    onAddPurchase = { viewModel.showAddPurchaseDialog() },
                    onViewDetails = { viewModel.showPurchaseDetails(it) },
                    onDeleteBill = { viewModel.deletePurchaseBill(it) }
                )
            }
        }
    }

    // Barcode scanner dialog
    if (showBarcodeScanner) {
        com.example.shoptracklite.ui.components.BarcodeScannerDialog(
            onBarcodeScanned = { barcode ->
                viewModel.scanBarcodeForDetails(barcode)
                showBarcodeScanner = false
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    // Barcode not found - offer to add new product
    uiState.scannedBarcodeForNewProduct?.let { barcode ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissBarcodeNotFoundDialog() },
            title = { Text("Product Not Found") },
            text = {
                Text("No product found with barcode: $barcode. Would you like to add a new product with this barcode?")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.showAddDialogWithBarcode(barcode) }
                ) {
                    Text("Add Product")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissBarcodeNotFoundDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Product Dialog
    if (uiState.showAddDialog) {
        ProductDialog(
            product = null,
            initialBarcode = uiState.initialBarcodeForAdd,
            categories = uiState.categories,
            onAddCategory = { viewModel.addCategory(it) },
            wholesaleModeEnabled = uiState.wholesaleModeEnabled,
            onDismiss = { viewModel.hideAddDialog() },
            onSave = { product, priceRanges -> viewModel.addProduct(product, priceRanges) }
        )
    }

    // Edit Product Dialog
    if (uiState.showEditDialog && uiState.selectedProduct != null) {
        ProductDialog(
            product = uiState.selectedProduct,
            categories = uiState.categories,
            onAddCategory = { viewModel.addCategory(it) },
            wholesaleModeEnabled = uiState.wholesaleModeEnabled,
            onDismiss = { viewModel.hideEditDialog() },
            onSave = { product, priceRanges -> viewModel.updateProduct(product, priceRanges) }
        )
    }

    // Delete Product Confirmation Dialog
    if (uiState.showDeleteConfirmation && uiState.productToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text("Delete Product") },
            text = { 
                Text("Are you sure you want to delete \"${uiState.productToDelete!!.name}\"? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDelete() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Share Inventory Dialog
    if (uiState.showShareDialog) {
        InventoryShareDialog(
            products = uiState.products,
            currencyCode = uiState.currencyCode,
            onDismiss = { viewModel.hideShareDialog() }
        )
    }

    // Restock Dialog
    if (uiState.showRestockDialog) {
        RestockDialog(
            products = uiState.products,
            onRestock = { product, quantity ->
                viewModel.restockProduct(product, quantity)
            },
            onDismiss = { viewModel.hideRestockDialog() }
        )
    }

    // Product Details Dialog (from barcode scan)
    if (uiState.showProductDetailsDialog && uiState.scannedProduct != null) {
        ProductDetailsDialog(
            product = uiState.scannedProduct!!,
            onEdit = { viewModel.showEditDialog(uiState.scannedProduct!!) },
            onDismiss = { viewModel.hideProductDetailsDialog() }
        )
    }

    // ===== SUPPLIES DIALOGS =====
    
    // Add Supply Dialog
    if (uiState.showAddSupplyDialog) {
        AddEditSupplyDialog(
            supply = null,
            onSave = { viewModel.addSupply(it) },
            onDismiss = { viewModel.hideAddSupplyDialog() }
        )
    }
    
    // Edit Supply Dialog
    if (uiState.showEditSupplyDialog && uiState.selectedSupply != null) {
        AddEditSupplyDialog(
            supply = uiState.selectedSupply,
            onSave = { viewModel.updateSupply(it) },
            onDismiss = { viewModel.hideEditSupplyDialog() }
        )
    }
    
    // Delete Supply Confirmation Dialog
    if (uiState.showDeleteSupplyConfirmation && uiState.supplyToDelete != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteSupply() },
            title = { Text("Delete Supply") },
            text = { 
                Text("Are you sure you want to delete \"${uiState.supplyToDelete!!.name}\"? This will also remove all product links.")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteSupply() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeleteSupply() }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Link Supply to Product Dialog
    if (uiState.showLinkSupplyDialog && uiState.productForLinking != null) {
        LinkSupplyDialog(
            product = uiState.productForLinking!!,
            supplies = uiState.supplies,
            existingLinks = uiState.existingLinks,
            onSave = { links -> viewModel.saveSupplyLinks(uiState.productForLinking!!.id, links) },
            onDismiss = { viewModel.hideLinkSupplyDialog() }
        )
    }

    // ===== PURCHASES DIALOGS =====
    
    // Add Purchase Dialog
    if (uiState.showAddPurchaseDialog) {
        AddPurchaseDialog(
            products = uiState.products,
            supplies = uiState.supplies,
            currencyCode = uiState.currencyCode,
            onSave = { bill, items, newProducts ->
                if (newProducts.isEmpty()) {
                    // No new products, use simple save
                    viewModel.recordPurchase(bill, items)
                } else {
                    // Has new products, use extended save
                    val newProductInfos = newProducts.map { np ->
                        np.itemIndex to InventoryViewModel.NewProductInfo(
                            name = np.name,
                            costPrice = np.costPrice,
                            sellingPrice = np.sellingPrice,
                            quantity = np.quantity,
                            barcode = np.barcode
                        )
                    }
                    viewModel.recordPurchaseWithNewProducts(bill, items, newProductInfos)
                }
            },
            onDismiss = { viewModel.hideAddPurchaseDialog() }
        )
    }
    
    // Purchase Details Dialog
    if (uiState.showPurchaseDetailsDialog && uiState.selectedPurchaseBill != null) {
        PurchaseDetailsDialog(
            billWithItems = uiState.selectedPurchaseBill!!,
            currencyCode = uiState.currencyCode,
            onDismiss = { viewModel.hidePurchaseDetailsDialog() }
        )
    }
    } // End BluetoothBarcodeDetector
}

@Composable
fun ProductsTab(
    products: List<Product>,
    filteredProducts: List<Product>,
    searchQuery: String,
    currencyCode: String,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onScanBarcode: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onLinkSupplies: (Product) -> Unit,
    onRestock: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar with barcode scanner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search products...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            FloatingActionButton(
                onClick = onScanBarcode,
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

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (products.isEmpty()) {
            // Empty State
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
                        Icons.Default.Inventory,
                        contentDescription = "No Products",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No products in inventory",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the + button to add your first product",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (filteredProducts.isEmpty() && searchQuery.isNotEmpty()) {
            // No search results
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try a different search term",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Product List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts) { product ->
                    ProductCard(
                        product = product,
                        currencyCode = currencyCode,
                        onEdit = { onEditProduct(product) },
                        onDelete = { onDeleteProduct(product) },
                        onLinkSupplies = { onLinkSupplies(product) }
                    )
                }
            }
            
            // Restock Button at the bottom
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRestock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Quick Restock",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    currencyCode: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onLinkSupplies: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                ProductImageOrColorBox(
                    imagePath = product.imagePath,
                    colorHex = product.colorHex,
                    context = context,
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (product.trackInventory) {
                        Text(
                            text = "Stock: ${product.quantityInStock}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (product.quantityInStock > 0) 
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = "No stock tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row {
                    if (onLinkSupplies != null) {
                        IconButton(onClick = onLinkSupplies) {
                            Icon(Icons.Default.Link, contentDescription = "Link Supplies")
                        }
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cost: ${CurrencyUtils.formatCurrency(product.costPrice, currencyCode)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Retail: ${CurrencyUtils.formatCurrency(product.sellingPrice, currencyCode)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                product.wholesalePrice?.let { wholesalePrice ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Wholesale: ${CurrencyUtils.formatCurrency(wholesalePrice, currencyCode)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkSupplyDialog(
    product: Product,
    supplies: List<com.example.shoptracklite.data.Supply>,
    existingLinks: List<com.example.shoptracklite.data.ProductSupplyLink>,
    onSave: (List<com.example.shoptracklite.data.ProductSupplyLink>) -> Unit,
    onDismiss: () -> Unit
) {
    // Track which supplies are linked and their consumption amounts
    var linkStates by remember {
        mutableStateOf(
            supplies.map { supply ->
                val existingLink = existingLinks.find { it.supplyId == supply.id }
                Triple(supply, existingLink != null, existingLink?.quantityConsumed?.toString() ?: "1")
            }
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link Supplies to ${product.name}") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Select supplies that are consumed when this product is sold. The quantity consumed will be automatically deducted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (supplies.isEmpty()) {
                    Text(
                        text = "No supplies available. Add supplies first in the Supplies tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    linkStates.forEachIndexed { index, (supply, isLinked, consumeAmount) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isLinked,
                                        onCheckedChange = { checked ->
                                            linkStates = linkStates.toMutableList().apply {
                                                this[index] = Triple(supply, checked, consumeAmount)
                                            }
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = supply.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Available: ${String.format("%.1f", supply.quantity)} ${supply.unit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                if (isLinked) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = consumeAmount,
                                        onValueChange = { value ->
                                            if (value.isEmpty() || value.matches(Regex("^\\d*\\.?\\d*$"))) {
                                                linkStates = linkStates.toMutableList().apply {
                                                    this[index] = Triple(supply, isLinked, value)
                                                }
                                            }
                                        },
                                        label = { Text("Consume per sale") },
                                        suffix = { Text(supply.unit) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        supportingText = { 
                                            Text("Amount of ${supply.name} used when selling 1 ${product.name}")
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val links = linkStates
                        .filter { it.second }  // Only linked supplies
                        .mapNotNull { (supply, _, amount) ->
                            val qty = amount.toDoubleOrNull() ?: return@mapNotNull null
                            if (qty <= 0) return@mapNotNull null
                            com.example.shoptracklite.data.ProductSupplyLink(
                                productId = product.id,
                                supplyId = supply.id,
                                quantityConsumed = qty
                            )
                        }
                    onSave(links)
                }
            ) {
                Text("Save")
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
fun ProductDialog(
    product: Product?,
    initialBarcode: String? = null,
    categories: List<Category> = emptyList(),
    onAddCategory: (String) -> Unit = {},
    wholesaleModeEnabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (Product, List<com.example.shoptracklite.data.PriceRange>) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var costPrice by remember { mutableStateOf(product?.costPrice?.toString() ?: "") }
    var sellingPrice by remember { mutableStateOf(product?.sellingPrice?.toString() ?: "") }
    var wholesalePrice by remember { mutableStateOf(product?.wholesalePrice?.toString() ?: "") }
    var quantity by remember { mutableStateOf(product?.quantityInStock?.toString() ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: initialBarcode ?: "") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(product?.categoryId) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var trackInventory by remember { mutableStateOf(product?.trackInventory ?: true) }
    var imagePath by remember { mutableStateOf(product?.imagePath ?: "") }
    var colorHex by remember { mutableStateOf(product?.colorHex ?: "") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val productsDir = File(context.filesDir, "products").apply { mkdirs() }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            coroutineScope.launch {
                val path = withContext(Dispatchers.IO) {
                    try {
                        val fileName = "img_${System.currentTimeMillis()}.jpg"
                        val destFile = File(productsDir, fileName)
                        context.contentResolver.openInputStream(selectedUri)?.use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        "products/$fileName"
                    } catch (e: Exception) {
                        null
                    }
                }
                path?.let {
                    imagePath = it
                    colorHex = ""
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Add Product" else "Edit Product") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Category dropdown
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategoryId?.let { id ->
                            categories.find { it.id == id }?.name ?: "No category"
                        } ?: "No category",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No category") },
                            onClick = {
                                selectedCategoryId = null
                                categoryDropdownExpanded = false
                            }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Add category") },
                            onClick = {
                                categoryDropdownExpanded = false
                                showAddCategoryDialog = true
                            }
                        )
                    }
                }
                if (showAddCategoryDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showAddCategoryDialog = false
                            newCategoryName = ""
                        },
                        title = { Text("Add category") },
                        text = {
                            OutlinedTextField(
                                value = newCategoryName,
                                onValueChange = { newCategoryName = it },
                                label = { Text("Category name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (newCategoryName.isNotBlank()) {
                                        onAddCategory(newCategoryName)
                                        newCategoryName = ""
                                        showAddCategoryDialog = false
                                    }
                                }
                            ) {
                                Text("Add")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                showAddCategoryDialog = false
                                newCategoryName = ""
                            }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = costPrice,
                    onValueChange = { costPrice = it },
                    label = { Text("Cost Price") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = sellingPrice,
                    onValueChange = { sellingPrice = it },
                    label = { Text("Retail Selling Price") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    )
                )
                
                // Wholesale Price (only show if wholesale mode is enabled)
                if (wholesaleModeEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = wholesalePrice,
                        onValueChange = { wholesalePrice = it },
                        label = { Text("Wholesale Price (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Track inventory checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = trackInventory,
                        onCheckedChange = { trackInventory = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Track inventory",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Turn off for services or items you don't track in stock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (trackInventory) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity in Stock") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Barcode input with scan button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Barcode/QR Code (Optional)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = { showBarcodeScanner = true },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                // Optional image and color
                Text(
                    text = "Image or Color (Optional)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Image/Color preview
                    ProductImageOrColorBox(
                        imagePath = imagePath.ifBlank { null },
                        colorHex = colorHex.ifBlank { null },
                        context = context,
                        size = 48.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (imagePath.isBlank()) {
                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add image", style = MaterialTheme.typography.labelMedium)
                                }
                            } else {
                                TextButton(
                                    onClick = {
                                        imagePath = ""
                                    }
                                ) {
                                    Text("Remove image", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Or choose a color:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                "#FF0000" to "Red",
                                "#0000FF" to "Blue",
                                "#00FF00" to "Green",
                                "#FFA500" to "Orange",
                                "#9E9E9E" to "Grey",
                                "#9C27B0" to "Purple"
                            ).forEach { (hex, _) ->
                                ColorChip(
                                    color = Color(android.graphics.Color.parseColor(hex)),
                                    selected = colorHex == hex,
                                    onClick = {
                                        colorHex = if (colorHex == hex) "" else hex
                                        if (colorHex.isNotEmpty()) imagePath = ""
                                    }
                                )
                            }
                            if (colorHex.isNotEmpty()) {
                                TextButton(onClick = { colorHex = "" }) {
                                    Text("Clear", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cost = costPrice.toDoubleOrNull() ?: 0.0
                    val sell = sellingPrice.toDoubleOrNull() ?: 0.0
                    val wholesale = wholesalePrice.toDoubleOrNull()
                    val qty = quantity.toIntOrNull() ?: 0
                    
                    val qtyForStock = if (trackInventory) qty else 0
                    val newProduct = if (product == null) {
                        Product(
                            name = name,
                            costPrice = cost,
                            sellingPrice = sell,
                            wholesalePrice = wholesale,
                            quantityInStock = qtyForStock,
                            hasQuantityBasedPricing = false,
                            barcode = barcode.ifBlank { null },
                            imagePath = imagePath.ifBlank { null },
                            colorHex = colorHex.ifBlank { null },
                            trackInventory = trackInventory,
                            categoryId = selectedCategoryId
                        )
                    } else {
                        product.copy(
                            name = name,
                            costPrice = cost,
                            sellingPrice = sell,
                            wholesalePrice = wholesale,
                            quantityInStock = qtyForStock,
                            hasQuantityBasedPricing = false,
                            barcode = barcode.ifBlank { null },
                            imagePath = imagePath.ifBlank { null },
                            colorHex = colorHex.ifBlank { null },
                            trackInventory = trackInventory,
                            categoryId = selectedCategoryId
                        )
                    }
                    onSave(newProduct, emptyList())
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
    // Barcode scanner dialog
    if (showBarcodeScanner) {
        com.example.shoptracklite.ui.components.BarcodeScannerDialog(
            onBarcodeScanned = { scannedBarcode ->
                barcode = scannedBarcode
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}

@Composable
fun ProductImageOrColorBox(
    imagePath: String?,
    colorHex: String?,
    context: android.content.Context,
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    val bitmap = remember(imagePath) {
        if (imagePath != null) {
            val file = File(context.filesDir, imagePath)
            if (file.exists()) {
                try {
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                } catch (e: Exception) { null }
                } else null
        } else null
    }
    
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(
                when {
                    bitmap != null -> Color.Transparent
                    colorHex != null -> try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (e: Exception) { Color(0xFF9E9E9E) }
                    else -> Color(0xFF9E9E9E)
                }
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = "Product image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun ColorChip(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    )
}
