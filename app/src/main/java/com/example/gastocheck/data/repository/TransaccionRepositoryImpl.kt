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
                // 1. Salida de origen (Gasto)
                val salida = TransaccionEntity(
                    monto = monto,
                    categoria = "Transferencia",
                    // CORRECCIÓN: Usamos notaCompleta y notaResumen en lugar de descripcion
                    notaCompleta = "Transferencia a ${cuentaDestino.nombre}",
                    notaResumen = "Transferencia Enviada",
                    fecha = fecha,
                    esIngreso = false,
                    cuentaId = origenId
                )
                transaccionDao.insertTransaccion(salida)

                // 2. Entrada a destino (Ingreso)
                val entrada = TransaccionEntity(
                    monto = monto,
                    categoria = "Transferencia",
                    // CORRECCIÓN: Ajustamos los campos aquí también
                    notaCompleta = "Recibido de ${cuentaOrigen.nombre}",
                    notaResumen = "Transferencia Recibida",
                    fecha = fecha,
                    esIngreso = true,
                    cuentaId = destinoId
                )
                transaccionDao.insertTransaccion(entrada)
            }
        }
    }
}