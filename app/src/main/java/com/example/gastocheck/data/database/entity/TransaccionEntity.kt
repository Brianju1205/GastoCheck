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
    val notaCompleta: String, // Antes "descripcion". Mantiene el detalle completo (Voz/Texto).
    val notaResumen: String,  // NUEVO: Para mostrar en la lista principal (ej. "Oxxo").
    val fecha: Date,
    val esIngreso: Boolean,
    val cuentaId: Int,
    val fotoUri: String? = null
)