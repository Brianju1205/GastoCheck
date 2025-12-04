package com.example.gastocheck.data.repository

import androidx.room.withTransaction
import com.example.gastocheck.data.database.AppDatabase
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject

class TransaccionRepositoryImpl @Inject constructor(
    private val transaccionDao: TransaccionDao,
    private val cuentaDao: CuentaDao,
    private val db: AppDatabase
) : TransaccionRepository {

    override fun getTransaccionesGlobales() = transaccionDao.getAllTransacciones()
    override fun getTransaccionesPorCuenta(cuentaId: Int) = transaccionDao.getTransaccionesByCuenta(cuentaId)
    override fun getCuentas() = cuentaDao.getCuentas()

    // --- NUEVO ---
    override fun getCuentaByIdFlow(id: Int): Flow<CuentaEntity?> = cuentaDao.getCuentaByIdFlow(id)

    override suspend fun getTransaccionById(id: Int) = transaccionDao.getTransaccionById(id)
    override suspend fun getCuentaById(id: Int) = cuentaDao.getCuentaById(id)

    override suspend fun insertTransaccion(transaccion: TransaccionEntity) = transaccionDao.insertTransaccion(transaccion)
    override suspend fun insertCuenta(cuenta: CuentaEntity) = cuentaDao.insertCuenta(cuenta)
    override suspend fun deleteTransaccion(transaccion: TransaccionEntity) = transaccionDao.deleteTransaccion(transaccion)

    override suspend fun realizarTransferencia(origenId: Int, destinoId: Int, monto: Double, fecha: Date) {
        db.withTransaction {
            val cuentaOrigen = cuentaDao.getCuentaById(origenId)
            val cuentaDestino = cuentaDao.getCuentaById(destinoId)

            if (cuentaOrigen != null && cuentaDestino != null) {
                // 1. Salida de origen
                val salida = TransaccionEntity(
                    monto = monto,
                    categoria = "Transferencia",
                    descripcion = "Transferencia a ${cuentaDestino.nombre}",
                    fecha = fecha,
                    esIngreso = false,
                    cuentaId = origenId
                )
                transaccionDao.insertTransaccion(salida)

                // 2. Entrada a destino
                val entrada = TransaccionEntity(
                    monto = monto,
                    categoria = "Transferencia",
                    descripcion = "Recibido de ${cuentaOrigen.nombre}",
                    fecha = fecha,
                    esIngreso = true,
                    cuentaId = destinoId
                )
                transaccionDao.insertTransaccion(entrada)
            }
        }
    }
}