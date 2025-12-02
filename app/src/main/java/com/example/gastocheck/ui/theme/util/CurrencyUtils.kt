package com.example.gastocheck.ui.theme.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        return format.format(amount)
    }

    fun formatCurrency(amount: Int): String {
        return formatCurrency(amount.toDouble())
    }
}