package com.example.gastocheck.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "historial_pagos_suscripcion")
data class HistorialPagoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val suscripcionId: Int,
    val monto: Double,
    val fechaPago: Long // La fecha en que se realizó o correspondía el pago
)