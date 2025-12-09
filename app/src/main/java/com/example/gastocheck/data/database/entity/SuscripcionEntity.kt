package com.example.gastocheck.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suscripciones")
data class SuscripcionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val monto: Double,
    val fechaPago: Long,
    val frecuencia: String,
    val icono: String,
    val cuentaId: Int,
    val nota: String = "",
    val recordatorio: String = "1 día antes",
    val horaRecordatorio: String = "09:00",

    // CAMBIO: Ahora es un String para soportar los 3 estados
    // Valores posibles: "PAGADO", "PENDIENTE", "CANCELADO", o null (Automático)
    val estadoActual: String? = null
)