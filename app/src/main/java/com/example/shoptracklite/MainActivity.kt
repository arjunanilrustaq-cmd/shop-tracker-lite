package com.example.shoptracklite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.shoptracklite.data.ShopTrackDatabase
import com.example.shoptracklite.data.ShopTrackRepository
import com.example.shoptracklite.navigation.Screen
import com.example.shoptracklite.ui.screens.HomeScreen
import com.example.shoptracklite.ui.screens.InventoryScreen
import com.example.shoptracklite.ui.screens.ExpensesScreen
import com.example.shoptracklite.ui.screens.ReportsScreen
import com.example.shoptracklite.ui.screens.SalesScreen
import com.example.shoptracklite.ui.screens.SettingsScreen
import com.example.shoptracklite.ui.theme.ShopTrackLiteTheme

class MainActivity : ComponentActivity() {
    
    private val database by lazy { ShopTrackDatabase.getDatabase(this) }
    private val repository by lazy { 
        ShopTrackRepository(
            productDao = database.productDao(),
            categoryDao = database.categoryDao(),
            saleDao = database.saleDao(),
            favoriteDao = database.favoriteDao(),
            priceRangeDao = database.priceRangeDao(),
            settingsDao = database.settingsDao(),
            expenseDao = database.expenseDao(),
            cashReconciliationDao = database.cashReconciliationDao(),
            supplyDao = database.supplyDao(),
            productSupplyLinkDao = database.productSupplyLinkDao(),
            purchaseBillDao = database.purchaseBillDao()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            ShopTrackLiteTheme {
                val navController = rememberNavController()
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            
                            listOf(
                                Screen.Home,
                                Screen.Inventory,
                                Screen.Expenses,
                                Screen.Reports,
                                Screen.Settings
                            ).forEach { screen ->
                                NavigationBarItem(
                                    icon = { 
                                        Icon(
                                            imageVector = getIconForScreen(screen),
                                            contentDescription = screen.title
                                        )
                                    },
                                    label = { Text(screen.title) },
                                    selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                    onClick = {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(repository = repository)
                        }
                        composable(Screen.Inventory.route) {
                            InventoryScreen(repository = repository)
                        }
                        composable(Screen.Expenses.route) {
                            ExpensesScreen(repository = repository)
                        }
                        composable(Screen.Reports.route) {
                            ReportsScreen(repository = repository)
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(repository = repository)
                        }
                    }
                }
            }
        }
    }
    
    private fun getIconForScreen(screen: Screen): ImageVector {
        return when (screen) {
            Screen.Home -> Icons.Default.Home
            Screen.Inventory -> Icons.Default.List
            Screen.Expenses -> Icons.Default.AccountBox
            Screen.Reports -> Icons.Default.Info
            Screen.Settings -> Icons.Default.Settings
        }
    }
}