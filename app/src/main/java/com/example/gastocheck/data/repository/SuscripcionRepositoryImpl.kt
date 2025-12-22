package com.example.gastocheck.data.repository

import com.example.gastocheck.data.database.dao.SuscripcionDao
import com.example.gastocheck.data.database.entity.SuscripcionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SuscripcionRepositoryImpl @Inject constructor(
    private val suscripcionDao: SuscripcionDao
) : SuscripcionRepository {

    override fun getSuscripciones(): Flow<List<SuscripcionEntity>> {
        return suscripcionDao.getSuscripciones()
    }

    override suspend fun getSuscripcionById(id: Int): SuscripcionEntity? {
        return suscripcionDao.getSuscripcionById(id)
    }

    override suspend fun insertSuscripcion(suscripcion: SuscripcionEntity): Long {
        return suscripcionDao.insertSuscripcion(suscripcion)
    }

    override suspend fun updateSuscripcion(suscripcion: SuscripcionEntity) {
        suscripcionDao.updateSuscripcion(suscripcion)
    }

    override suspend fun deleteSuscripcion(suscripcion: SuscripcionEntity) {
        suscripcionDao.deleteSuscripcion(suscripcion)
    }
}