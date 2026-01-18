package com.example.gastocheck.data.repository

import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.entity.CuentaEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CuentaRepositoryImpl @Inject constructor(
    private val cuentaDao: CuentaDao
) : CuentaRepository {

    override fun getCuentas(): Flow<List<CuentaEntity>> {
        return cuentaDao.getCuentas()
    }

    override suspend fun getCuentaById(id: Int): CuentaEntity? {
        return cuentaDao.getCuentaById(id)
    }

    override suspend fun insertCuenta(cuenta: CuentaEntity): Long {
        return cuentaDao.insertCuenta(cuenta)
    }

    override suspend fun updateCuenta(cuenta: CuentaEntity) {
        cuentaDao.updateCuenta(cuenta)
    }

    override suspend fun deleteCuenta(cuenta: CuentaEntity) {
        cuentaDao.deleteCuenta(cuenta)
    }

    override suspend fun actualizarSaldo(cuentaId: Int, nuevoSaldo: Double) {
        cuentaDao.actualizarSaldo(cuentaId, nuevoSaldo)
        // Asegúrate de tener @Query("UPDATE cuentas SET saldoActual = :nuevoSaldo WHERE id = :cuentaId") en tu DAO
        // Si no tienes ese método en el DAO, puedes usar updateCuenta pasando la entidad completa.
    }
}