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

    // NUEVO: Formato ejemplo: "12 Oct, 10:30 AM"
    fun formatearFechaCompleta(date: Date): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(date)
    }
    fun formatearRango(inicio: Long, fin: Long): String {
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        return "${sdf.format(Date(inicio))} - ${sdf.format(Date(fin))}"
    }

    // --- NUEVAS FUNCIONES DE FILTRO ---

    fun esHoy(date: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal2.time = date
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun esEstaSemana(date: Date): Boolean {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek())
        // Reseteamos horas para comparar solo fechas desde el inicio de la semana
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val inicioSemana = cal.time
        return date.after(inicioSemana) || esMismoDia(date, inicioSemana)
    }

    fun esEsteMes(date: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal2.time = date
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    fun esEsteAnio(date: Date): Boolean {
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        cal2.time = date
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
    }

    fun esEnRango(date: Date, inicioMillis: Long?, finMillis: Long?): Boolean {
        if (inicioMillis == null || finMillis == null) return false
        val time = date.time
        // Aseguramos que cubra todo el día final agregando 24h si fuera necesario,
        // pero normalmente DatePicker devuelve millis a las 00:00 UTC.
        // Para simplificar, comparamos >= inicio y <= fin (ajustado al final del día)

        // Ajuste simple: El rango incluye los extremos
        return time >= inicioMillis && time <= (finMillis + 86400000) // +1 día aprox de margen para incluir el día final completo
    }

    private fun esMismoDia(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
