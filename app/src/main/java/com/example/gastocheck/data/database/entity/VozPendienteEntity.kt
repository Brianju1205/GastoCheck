package com.example.gastocheck.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voz_pendientes")
data class VozPendienteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val textoDetectado: String,
    val fechaCreacion: Long = System.currentTimeMillis()
)