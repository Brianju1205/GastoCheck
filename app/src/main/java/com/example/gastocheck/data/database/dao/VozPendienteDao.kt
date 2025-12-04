package com.example.gastocheck.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.gastocheck.data.database.entity.VozPendienteEntity

@Dao
interface VozPendienteDao {
    @Insert
    suspend fun insertar(pendiente: VozPendienteEntity)

    @Query("SELECT * FROM voz_pendientes")
    suspend fun obtenerTodos(): List<VozPendienteEntity>

    @Delete
    suspend fun eliminar(pendiente: VozPendienteEntity)
}