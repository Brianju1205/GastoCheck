package com.example.gastocheck.data.repository

import androidx.room.withTransaction
import com.example.gastocheck.data.database.AppDatabase
import com.example.gastocheck.data.database.dao.BalanceSnapshotDao
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.database.entity.BalanceSnapshotEntity
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject

class TransaccionRepositoryImpl @Inject constructor(
    private val transaccionDao: TransaccionDao,
    private val cuentaDao: CuentaDao,
    private val balanceSnapshotDao: BalanceSnapshotDao,
    private val db: AppDatabase
) : TransaccionRepository {

    // ... (Otros métodos sin cambios) ...
    override fun getTransaccionesGlobales() = transaccionDao.getAllTransacciones()
    override fun getTransaccionesPorCuenta(cuentaId: Int) = transaccionDao.getTransaccionesByCuenta(cuentaId)
    override fun getCuentas() = cuentaDao.getCuentas()
    override fun getCuentaByIdFlow(id: Int) = cuentaDao.getCuentaByIdFlow(id)
    override fun getHistorialSaldos(cuentaId: Int) = balanceSnapshotDao.getHistorialSaldos(cuentaId)
    override suspend fun getTransaccionById(id: Int) = transaccionDao.getTransaccionById(id)
    override suspend fun getCuentaById(id: Int) = cuentaDao.getCuentaById(id)
    override suspend fun getTransaccionPareja(transaccion: TransaccionEntity) = transaccionDao.getTransaccionPareja(transaccion.fecha, transaccion.monto, transaccion.id)
    override suspend fun insertCuenta(cuenta: CuentaEntity) = cuentaDao.insertCuenta(cuenta)
    override suspend fun insertTransaccion(transaccion: TransaccionEntity) {
        db.withTransaction {
            transaccionDao.insertTransaccion(transaccion)
            registrarSnapshot(transaccion.cuentaId, transaccion.fecha, transaccion.categoria)
            registrarSnapshot(-1, transaccion.fecha, transaccion.categoria)
        }
    }
    override suspend fun deleteTransaccion(transaccion: TransaccionEntity) {
        db.withTransaction {
            transaccionDao.deleteTransaccion(transaccion)
            registrarSnapshot(transaccion.cuentaId, Date(), "Corrección")
            registrarSnapshot(-1, Date(), "Corrección")
        }
    }
    override suspend fun eliminarTransferenciaCompleta(id: Int) {
        db.withTransaction {
            val tOriginal = transaccionDao.getTransaccionById(id) ?: return@withTransaction
            val tPareja = transaccionDao.getTransaccionPareja(tOriginal.fecha, tOriginal.monto, tOriginal.id)
            transaccionDao.deleteTransaccion(tOriginal)
            if (tPareja != null) transaccionDao.deleteTransaccion(tPareja)
            registrarSnapshot(tOriginal.cuentaId, Date(), "Corrección Transferencia")
            if (tPareja != null) registrarSnapshot(tPareja.cuentaId, Date(), "Corrección Transferencia")
            registrarSnapshot(-1, Date(), "Corrección Transferencia")
        }
    }
    override suspend fun realizarTransferencia(origenId: Int, destinoId: Int, monto: Double, fecha: Date) {
        db.withTransaction {
            val cuentaOrigen = cuentaDao.getCuentaById(origenId)
            val cuentaDestino = cuentaDao.getCuentaById(destinoId)
            if (cuentaOrigen != null && cuentaDestino != null) {
                val salida = TransaccionEntity(monto = monto, categoria = "Transferencia", notaCompleta = "Transferencia a ${cuentaDestino.nombre}", notaResumen = "Transferencia Enviada", fecha = fecha, esIngreso = false, cuentaId = origenId)
                transaccionDao.insertTransaccion(salida)
                val entrada = TransaccionEntity(monto = monto, categoria = "Transferencia", notaCompleta = "Recibido de ${cuentaOrigen.nombre}", notaResumen = "Transferencia Recibida", fecha = fecha, esIngreso = true, cuentaId = destinoId)
                transaccionDao.insertTransaccion(entrada)
                registrarSnapshot(origenId, fecha, "Transferencia Enviada")
                registrarSnapshot(destinoId, fecha, "Transferencia Recibida")
                registrarSnapshot(-1, fecha, "Transferencia")
            }
        }
    }

    // --- NUEVO: Implementación Delete Cuenta ---
    override suspend fun deleteCuenta(cuenta: CuentaEntity) {
        db.withTransaction {
            // 1. Borrar snapshots asociados
            balanceSnapshotDao.deleteSnapshotsByCuenta(cuenta.id)
            // 2. Borrar la cuenta (Las transacciones se borran por CASCADE en la BD)
            cuentaDao.deleteCuenta(cuenta)
        }
    }

    // ... (Lógica privada de snapshots igual) ...
    private suspend fun registrarSnapshot(cuentaId: Int, fecha: Date, motivo: String) {
        val saldoCalculado = calcularSaldoActual(cuentaId)
        val snapshot = BalanceSnapshotEntity(cuentaId = cuentaId, saldo = saldoCalculado, fecha = fecha, motivo = motivo)
        balanceSnapshotDao.insertSnapshot(snapshot)
    }
    private suspend fun calcularSaldoActual(cuentaId: Int): Double {
        if (cuentaId == -1) {
            val cuentas = cuentaDao.getCuentasList()
            val transacciones = transaccionDao.getAllTransaccionesList()
            val saldoInicialTotal = cuentas.sumOf { it.saldoInicial }
            val ingresos = transacciones.filter { it.esIngreso }.sumOf { it.monto }
            val gastos = transacciones.filter { !it.esIngreso }.sumOf { it.monto }
            return saldoInicialTotal + ingresos - gastos
        } else {
            val cuenta = cuentaDao.getCuentaById(cuentaId) ?: return 0.0
            val transacciones = transaccionDao.getTransaccionesByCuentaList(cuentaId)
            val ingresos = transacciones.filter { it.esIngreso }.sumOf { it.monto }
            val gastos = transacciones.filter { !it.esIngreso }.sumOf { it.monto }
            return cuenta.saldoInicial + ingresos - gastos
        }
    }
}