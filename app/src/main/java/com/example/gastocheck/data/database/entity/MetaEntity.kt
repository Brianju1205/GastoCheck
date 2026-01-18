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

    // --- CAMPOS DE DISEÑO Y DETALLE ---
    val icono: String = "Savings",
    val colorHex: String = "#00E676",
    val fechaLimite: Long? = null,
    val cuentaId: Int = -1,
    val nota: String = "",

    // --- CAMPO PARA REORDENAR (Funcionalidad de subir/bajar prioridad) ---
    val orden: Int = 0,

    // --- CAMPO PARA BORRADO LÓGICO (Recomendado para no perder historial) ---
    val esArchivada: Boolean = false,
    val esPausada: Boolean = false
)