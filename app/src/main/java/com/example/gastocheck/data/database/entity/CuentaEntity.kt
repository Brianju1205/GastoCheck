package com.example.gastocheck.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cuentas")
data class CuentaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val tipo: String,
    val saldoInicial: Double,
    val colorHex: String = "#00E676",
    val icono: String = "Wallet", // <--- NUEVO CAMPO
    val esArchivada: Boolean = false,

    val esCredito: Boolean = false,
    val limiteCredito: Double = 0.0,
    val diaCorte: Int = 0, // Día del mes (1-31)
    val diaPago: Int = 0,  // Día del mes (1-31)
    val tasaInteres: Double = 0.0, // Opcional
    val recordatoriosActivos: Boolean = false
)