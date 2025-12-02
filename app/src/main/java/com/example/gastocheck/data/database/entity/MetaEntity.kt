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
    // Podríamos agregar fecha límite después
)