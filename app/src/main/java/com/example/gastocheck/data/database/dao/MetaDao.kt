package com.example.gastocheck.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gastocheck.data.database.entity.MetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaDao {
    // AQUI: Usamos "metas" porque así lo pusiste en tu Entity
    @Query("SELECT * FROM metas ORDER BY id DESC")
    fun getMetas(): Flow<List<MetaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeta(meta: MetaEntity)

    @Update
    suspend fun updateMeta(meta: MetaEntity)

    @Delete
    suspend fun deleteMeta(meta: MetaEntity)

    // AQUI TAMBIÉN: "UPDATE metas ..."
    @Query("UPDATE metas SET montoAhorrado = :nuevoMonto WHERE id = :id")
    suspend fun updateMonto(id: Int, nuevoMonto: Double)
}