package com.example.gastocheck.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cuentas")
data class CuentaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val tipo: String, // Ej: "Efectivo", "Débito", "Crédito"
    val saldoInicial: Double,
    val colorHex: String = "#00E676", // Color para UI
    val esArchivada: Boolean = false
)
