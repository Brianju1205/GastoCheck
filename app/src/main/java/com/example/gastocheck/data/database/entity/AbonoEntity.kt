package com.example.gastocheck.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "abonos")
data class AbonoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val metaId: Int, // Para saber a qué meta pertenece
    val monto: Double,
    val fecha: Long, // Fecha en milisegundos
    val nota: String = ""
)