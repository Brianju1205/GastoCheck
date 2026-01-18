package com.example.gastocheck.data.repository

import com.example.gastocheck.data.database.entity.CuentaEntity
import kotlinx.coroutines.flow.Flow

interface CuentaRepository {
    // Para obtener listas observables (Flow)
    fun getCuentas(): Flow<List<CuentaEntity>>

    // Para obtener una cuenta específica (CRUCIAL para tu lógica de crédito)
    suspend fun getCuentaById(id: Int): CuentaEntity?

    // Acciones de escritura
    suspend fun insertCuenta(cuenta: CuentaEntity): Long
    suspend fun updateCuenta(cuenta: CuentaEntity)
    suspend fun deleteCuenta(cuenta: CuentaEntity)

    // Opcional: Si tienes lógica de actualizar saldo directamente
    suspend fun actualizarSaldo(cuentaId: Int, nuevoSaldo: Double)
}