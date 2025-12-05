package com.example.gastocheck.ui.theme.util

import java.util.Locale

object NotaUtils {

    // Lista de palabras clave para identificar comercios o servicios comunes
    private val palabrasClave = listOf(
        // Tiendas
        "oxxo", "7eleven", "seven", "walmart", "soriana", "chedraui", "costco", "sams", "liverpool", "amazon", "mercado libre",
        // Servicios Digitales
        "netflix", "spotify", "disney", "youtube", "uber", "didi", "rappi", "apple", "google",
        // Servicios Básicos
        "cfe", "luz", "agua", "gas", "internet", "telcel", "movistar", "totalplay", "izzi",
        // Comida / Salidas
        "starbucks", "café", "tacos", "pizza", "sushi", "hamburguesa", "cine", "restaurante", "bar",
        // Transporte
        "gasolina", "pemex", "bp", "shell", "taller", "estacionamiento",
        // Salud
        "farmacia", "doctor", "consulta", "hospital",
        // General
        "pago", "transferencia", "depósito"
    )

    fun generarResumen(notaCompleta: String, categoria: String): String {
        val notaLower = notaCompleta.lowercase(Locale.ROOT)

        // Buscamos si la nota contiene alguna palabra clave
        val palabraEncontrada = palabrasClave.find { keyword ->
            notaLower.contains(keyword)
        }

        return if (palabraEncontrada != null) {
            // Retornamos la palabra encontrada con la primera letra mayúscula
            palabraEncontrada.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        } else {
            // Fallback: Si no hay palabras clave, usamos la Categoría
            categoria
        }
    }
}