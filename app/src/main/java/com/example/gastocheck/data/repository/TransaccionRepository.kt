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
    fun getCuentaByIdFlow(id: Int): Flow<CuentaEntity?>
    fun getHistorialSaldos(cuentaId: Int): Flow<List<BalanceSnapshotEntity>>

    suspend fun getTransaccionById(id: Int): TransaccionEntity?
    suspend fun getCuentaById(id: Int): CuentaEntity?

    // --- NUEVO: Obtener la transacción pareja ---
    suspend fun getTransaccionPareja(transaccion: TransaccionEntity): TransaccionEntity?

    suspend fun insertTransaccion(transaccion: TransaccionEntity)
    suspend fun insertCuenta(cuenta: CuentaEntity)
    suspend fun deleteTransaccion(transaccion: TransaccionEntity)

    // --- NUEVO: Borrar cuenta ---
    suspend fun deleteCuenta(cuenta: CuentaEntity)

    suspend fun eliminarTransferenciaCompleta(id: Int)
    suspend fun realizarTransferencia(
        origenId: Int,
        destinoId: Int,
        monto: Double,
        notaUsuario: String,
        detalleTecnico: String?, // Aquí irá la conversión (opcional)
        fecha: Date
    )}