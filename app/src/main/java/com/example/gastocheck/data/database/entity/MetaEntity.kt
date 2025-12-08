package com.example.gastocheck.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "metas")
data class MetaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val montoObjetivo: Double,
    val montoAhorrado: Double,

    // --- NUEVOS CAMPOS PARA EL DISEÑO COMPLETO ---
    val icono: String = "Savings",       // Nombre del icono (ej. "Savings", "Car")
    val colorHex: String = "#00E676",    // Guardamos el color (Verde neón por defecto)
    val fechaLimite: Long? = null,       // Guardamos la fecha en milisegundos (Long)
    val cuentaId: Int = -1,              // ID de la cuenta donde se guarda el dinero
    val nota: String = ""                // Notas opcionales
)