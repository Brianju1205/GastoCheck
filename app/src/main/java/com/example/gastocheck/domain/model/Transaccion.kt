package com.example.gastocheck.domain.model

import java.util.Date

data class Transaccion(
    val id: Int = 0,
    val monto: Double,
    val categoria: String,
    val descripcion: String,
    val fecha: Date,
    val esIngreso: Boolean // true = Ingreso, false = Gasto
)