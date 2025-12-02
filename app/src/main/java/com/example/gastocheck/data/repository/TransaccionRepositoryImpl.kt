package com.example.gastocheck.data.repository

import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.database.entity.TransaccionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TransaccionRepositoryImpl @Inject constructor(
    private val dao: TransaccionDao
) : TransaccionRepository {

    override fun getTransacciones(): Flow<List<TransaccionEntity>> {
        return dao.getTransacciones()
    }
    override suspend fun getTransaccionById(id: Int): TransaccionEntity? {
        return dao.getTransaccionById(id)
    }

    override suspend fun insertTransaccion(transaccion: TransaccionEntity) {
        dao.insertTransaccion(transaccion)
    }

    override suspend fun deleteTransaccion(transaccion: TransaccionEntity) {
        dao.deleteTransaccion(transaccion)
    }
}