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

    override fun getTransaccionesGlobales() = transaccionDao.getAllTransacciones()
    override fun getTransaccionesPorCuenta(cuentaId: Int) = transaccionDao.getTransaccionesByCuenta(cuentaId)
    override fun getCuentas() = cuentaDao.getCuentas()

    override fun getCuentaByIdFlow(id: Int): Flow<CuentaEntity?> = cuentaDao.getCuentaByIdFlow(id)

    override suspend fun getTransaccionById(id: Int) = transaccionDao.getTransaccionById(id)
    override suspend fun getCuentaById(id: Int) = cuentaDao.getCuentaById(id)

    // Método para el carrusel de historial
    override fun getHistorialSaldos(cuentaId: Int): Flow<List<BalanceSnapshotEntity>> {
        return balanceSnapshotDao.getHistorialSaldos(cuentaId)
    }

    override suspend fun insertCuenta(cuenta: CuentaEntity) = cuentaDao.insertCuenta(cuenta)

    override suspend fun insertTransaccion(transaccion: TransaccionEntity) {
        db.withTransaction {
            // 1. Guardar la transacción
            transaccionDao.insertTransaccion(transaccion)

            // 2. Foto del saldo de la cuenta específica
            registrarSnapshot(transaccion.cuentaId, transaccion.fecha, transaccion.categoria)

            // 3. Foto del saldo global
            registrarSnapshot(-1, transaccion.fecha, transaccion.categoria)
        }
    }

    override suspend fun deleteTransaccion(transaccion: TransaccionEntity) {
        db.withTransaction {
            transaccionDao.deleteTransaccion(transaccion)
            // Actualizar historial tras borrado
            registrarSnapshot(transaccion.cuentaId, Date(), "Corrección (Borrado)")
            registrarSnapshot(-1, Date(), "Corrección (Borrado)")
        }
    }

    override suspend fun realizarTransferencia(origenId: Int, destinoId: Int, monto: Double, fecha: Date) {
        db.withTransaction {
            val cuentaOrigen = cuentaDao.getCuentaById(origenId)
            val cuentaDestino = cuentaDao.getCuentaById(destinoId)

            if (cuentaOrigen != null && cuentaDestino != null) {
                // Salida
                val salida = TransaccionEntity(
                    monto = monto,
                    categoria = "Transferencia",
                    notaCompleta = "Transferencia a ${cuentaDestino.nombre}",
                    notaResumen = "Transferencia Enviada",
                    fecha = fecha,
                    esIngreso = false,
                    cuentaId = origenId
                )
                transaccionDao.insertTransaccion(salida)

                // Entrada
                val entrada = TransaccionEntity(
                    monto = monto,
                    categoria = "Transferencia",
                    notaCompleta = "Recibido de ${cuentaOrigen.nombre}",
                    notaResumen = "Transferencia Recibida",
                    fecha = fecha,
                    esIngreso = true,
                    cuentaId = destinoId
                )
                transaccionDao.insertTransaccion(entrada)

                // Actualizar historiales
                registrarSnapshot(origenId, fecha, "Transferencia Enviada")
                registrarSnapshot(destinoId, fecha, "Transferencia Recibida")
                registrarSnapshot(-1, fecha, "Transferencia")
            }
        }
    }

    // --- Lógica Privada de Cálculo ---

    private suspend fun registrarSnapshot(cuentaId: Int, fecha: Date, motivo: String) {
        val saldoCalculado = calcularSaldoActual(cuentaId)
        val snapshot = BalanceSnapshotEntity(
            cuentaId = cuentaId,
            saldo = saldoCalculado,
            fecha = fecha,
            motivo = motivo
        )
        balanceSnapshotDao.insertSnapshot(snapshot)
    }

    private suspend fun calcularSaldoActual(cuentaId: Int): Double {
        return if (cuentaId == -1) {
            // Global
            val cuentas = cuentaDao.getCuentasList()
            val transacciones = transaccionDao.getAllTransaccionesList()

            val saldoInicialTotal = cuentas.sumOf { it.saldoInicial }
            val ingresos = transacciones.filter { it.esIngreso }.sumOf { it.monto }
            val gastos = transacciones.filter { !it.esIngreso }.sumOf { it.monto }

            saldoInicialTotal + ingresos - gastos
        } else {
            // Individual
            val cuenta = cuentaDao.getCuentaById(cuentaId) ?: return 0.0
            val transacciones = transaccionDao.getTransaccionesByCuentaList(cuentaId)

            val ingresos = transacciones.filter { it.esIngreso }.sumOf { it.monto }
            val gastos = transacciones.filter { !it.esIngreso }.sumOf { it.monto }

            cuenta.saldoInicial + ingresos - gastos
        }
    }
}