package com.example.gastocheck.ui.theme.util

import android.icu.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US) // Usa comas para miles y punto para decimales
        return format.format(amount)
    }

    // Formato solo números con comas (ej: 1,250) para animaciones o textos específicos
    fun formatWithCommas(amount: Number): String {
        val formatter = DecimalFormat("#,###")
        return formatter.format(amount)
    }
}