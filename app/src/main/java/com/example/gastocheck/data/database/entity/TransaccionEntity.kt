package com.example.gastocheck.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "transacciones",
    foreignKeys = [
        ForeignKey(
            entity = CuentaEntity::class,
            parentColumns = ["id"],
            childColumns = ["cuentaId"],
            onDelete = ForeignKey.CASCADE // Si borras la cuenta, se borran sus gastos
        )
    ],
    indices = [Index(value = ["cuentaId"])]
)
data class TransaccionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val monto: Double,
    val categoria: String,
    val descripcion: String,
    val fecha: Date,
    val esIngreso: Boolean,
    val cuentaId: Int // <--- NUEVO CAMPO (Relación)
)
