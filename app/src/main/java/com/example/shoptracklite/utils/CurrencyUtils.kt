package com.example.shoptracklite.utils

import java.text.NumberFormat
import java.util.*

object CurrencyUtils {
    
    data class Currency(
        val code: String,
        val name: String,
        val symbol: String
    )
    
    val supportedCurrencies = listOf(
        Currency("USD", "US Dollar", "$"),
        Currency("OMR", "Omani Rial", "OMR"),
        Currency("EUR", "Euro", "€"),
        Currency("GBP", "British Pound", "£"),
        Currency("AED", "UAE Dirham", "AED"),
        Currency("SAR", "Saudi Riyal", "SAR"),
        Currency("INR", "Indian Rupee", "₹"),
        Currency("QAR", "Qatari Riyal", "QAR"),
        Currency("KWD", "Kuwaiti Dinar", "KWD"),
        Currency("BHD", "Bahraini Dinar", "BHD"),
        Currency("JPY", "Japanese Yen", "¥"),
        Currency("CNY", "Chinese Yuan", "¥"),
        Currency("AUD", "Australian Dollar", "A$"),
        Currency("CAD", "Canadian Dollar", "C$"),
        Currency("CHF", "Swiss Franc", "CHF"),
        Currency("SEK", "Swedish Krona", "SEK"),
        Currency("NZD", "New Zealand Dollar", "NZ$"),
        Currency("SGD", "Singapore Dollar", "S$"),
        Currency("HKD", "Hong Kong Dollar", "HK$"),
        Currency("KRW", "South Korean Won", "₩")
    )
    
    fun formatCurrency(amount: Double, currencyCode: String): String {
        return try {
            val currency = java.util.Currency.getInstance(currencyCode)
            val numberFormat = NumberFormat.getNumberInstance().apply {
                maximumFractionDigits = currency.defaultFractionDigits
                minimumFractionDigits = currency.defaultFractionDigits
            }
            val formattedAmount = numberFormat.format(amount)
            val symbol = getCurrencySymbol(currencyCode)
            "$symbol $formattedAmount"
        } catch (e: Exception) {
            // Fallback to USD if currency code is invalid
            val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 2
            }
            val formattedAmount = numberFormat.format(amount)
            "$ $formattedAmount"
        }
    }
    
    fun getCurrencySymbol(currencyCode: String): String {
        return supportedCurrencies.find { it.code == currencyCode }?.symbol ?: currencyCode
    }
    
    fun getCurrencyName(currencyCode: String): String {
        return supportedCurrencies.find { it.code == currencyCode }?.name ?: currencyCode
    }
}

