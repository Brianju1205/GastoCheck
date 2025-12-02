package com.example.gastocheck.ui.theme.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    fun formatearFechaAmigable(fecha: Date): String {
        val calFecha = Calendar.getInstance().apply { time = fecha }
        val calHoy = Calendar.getInstance()
        val calAyer = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            esMismoDia(calFecha, calHoy) -> "Hoy"
            esMismoDia(calFecha, calAyer) -> "Ayer"
            else -> SimpleDateFormat("dd 'de' MMM", Locale.getDefault()).format(fecha)
        }
    }

    private fun esMismoDia(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
