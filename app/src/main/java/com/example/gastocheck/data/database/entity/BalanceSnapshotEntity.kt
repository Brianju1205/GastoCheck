package com.example.gastocheck.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "balance_snapshots")
data class BalanceSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val cuentaId: Int, // -1 para Global, o el ID de la cuenta específica
    val saldo: Double,
    val fecha: Date,
    val motivo: String // Ej: "Gasto en Oxxo"
)