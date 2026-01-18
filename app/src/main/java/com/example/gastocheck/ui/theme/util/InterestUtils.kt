package com.example.gastocheck.ui.theme.util

import java.math.RoundingMode

object InterestUtils {

    /**
     * Calcula el interés mensual aproximado basado en la tasa anual.
     * Fórmula simplificada: (Deuda * (TasaAnual / 100)) / 12
     */
    fun calcularInteresEstimado(deuda: Double, tasaAnual: Double): Double {
        if (deuda <= 0 || tasaAnual <= 0) return 0.0

        val interesMensual = (deuda * (tasaAnual / 100)) / 12
        return interesMensual.toBigDecimal().setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    /**
     * Genera un mensaje amigable basado en la tasa y la deuda.
     */
    fun obtenerMensajeInteres(deuda: Double, tasaAnual: Double): String {
        val interes = calcularInteresEstimado(deuda, tasaAnual)

        return if (interes > 0) {
            // Mensaje informativo y motivador (Requisito 5)
            "Si dejas saldo pendiente, podrías generar aprox. ${CurrencyUtils.formatCurrency(interes)} de interés este mes."
        } else {
            "Mantén tu racha de pagos totales para seguir sin intereses."
        }
    }
    fun generarMensajeNotificacion(deuda: Double, tasaAnual: Double): String {
        val interesEstimado = calcularInteresEstimado(deuda, tasaAnual)

        return if (interesEstimado > 0) {
            "Si no cubres el total, podrías generar aprox. ${CurrencyUtils.formatCurrency(interesEstimado)} de intereses. ¡Evítalo!"
        } else {
            "Recuerda pagar el total para mantenerte sin intereses."
        }
    }

    // Agregamos este alias por si en alguna parte del código lo llamaste diferente

}