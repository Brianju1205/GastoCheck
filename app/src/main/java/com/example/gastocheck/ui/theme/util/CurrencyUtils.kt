package com.example.gastocheck.ui.theme.util

import android.icu.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    /*fun formatCurrency(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US) // Usa comas para miles y punto para decimales
        return format.format(amount)
    }*/

    // Formato solo números con comas (ej: 1,250) para animaciones o textos específicos
    fun formatWithCommas(amount: Number): String {
        val formatter = DecimalFormat("#,###")
        return formatter.format(amount)
    }
    private val TASAS_CAMBIO = mapOf(
        "MXN" to 1.0,
        "USD" to 20.50,  // 1 Dólar = 20.50 Pesos
        "EUR" to 21.80,  // 1 Euro = 21.80 Pesos
        "COP" to 0.005,  // 1 Peso Col = 0.005 Pesos MX
        "CLP" to 0.022   // 1 Peso Chileno = 0.022 Pesos MX
    )

    fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("es", "MX")).format(amount)
    }

    // Convierte cualquier moneda a MXN para guardarlo en la base de datos
    fun convertirAMxn(monto: Double, monedaOrigen: String): Double {
        val tasa = TASAS_CAMBIO[monedaOrigen] ?: 1.0
        return monto * tasa
    }

    fun obtenerMonedasDisponibles(): List<String> {
        return TASAS_CAMBIO.keys.toList()
    }
}