package com.example.gastocheck.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "transacciones")
data class TransaccionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val monto: Double,
    val categoria: String,
    val descripcion: String,
    val fecha: Date,
    val esIngreso: Boolean
)