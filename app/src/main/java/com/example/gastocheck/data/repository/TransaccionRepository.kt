package com.example.gastocheck.data.repository

import com.example.gastocheck.data.database.entity.BalanceSnapshotEntity
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface TransaccionRepository {
    fun getTransaccionesGlobales(): Flow<List<TransaccionEntity>>
    fun getTransaccionesPorCuenta(cuentaId: Int): Flow<List<TransaccionEntity>>
    fun getCuentas(): Flow<List<CuentaEntity>>

    // --- NUEVO: Para observar una sola cuenta en tiempo real ---
    fun getCuentaByIdFlow(id: Int): Flow<CuentaEntity?>

    // --- NUEVO: Para obtener el historial de saldos (Carrusel) ---
    fun getHistorialSaldos(cuentaId: Int): Flow<List<BalanceSnapshotEntity>>

    suspend fun getTransaccionById(id: Int): TransaccionEntity?
    suspend fun getCuentaById(id: Int): CuentaEntity?

    suspend fun insertTransaccion(transaccion: TransaccionEntity)
    suspend fun insertCuenta(cuenta: CuentaEntity)
    suspend fun deleteTransaccion(transaccion: TransaccionEntity)

    suspend fun realizarTransferencia(origenId: Int, destinoId: Int, monto: Double, fecha: Date)
}