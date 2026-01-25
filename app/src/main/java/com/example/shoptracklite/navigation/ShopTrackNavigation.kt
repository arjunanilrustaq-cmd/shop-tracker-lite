package com.example.shoptracklite.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Checkout")
    object Inventory : Screen("inventory", "Inventory")
    object Expenses : Screen("expenses", "Expenses")
    object Reports : Screen("reports", "Reports")
    object Settings : Screen("settings", "Settings")
}
