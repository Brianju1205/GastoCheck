package com.example.gastocheck.data.database.dao

import androidx.room.*
import com.example.gastocheck.data.database.entity.MetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaDao {

    // --- CONSULTA PRINCIPAL ---
    // 1. Filtramos "WHERE esArchivada = 0" (false) para no mostrar las borradas/completadas.
    // 2. Ordenamos "ORDER BY orden ASC" para que la meta con orden 0 salga primero (Arriba).
    @Query("SELECT * FROM metas WHERE esArchivada = 0 ORDER BY orden ASC")
    fun getMetasActivas(): Flow<List<MetaEntity>>

    // --- INSERTAR ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeta(meta: MetaEntity): Long

    // --- ACTUALIZAR UNO ---
    // Sirve para editar nombre, monto, color, o abonar dinero.
    @Update
    suspend fun updateMeta(meta: MetaEntity)

    // --- ACTUALIZAR VARIOS (NUEVO) ---
    // CRUCIAL: Este método permite guardar la lista completa cuando cambias el orden
    // (cuando pulsas las flechitas arriba/abajo).
    @Update
    suspend fun updateMetas(metas: List<MetaEntity>)

    // --- BORRAR ---
    @Delete
    suspend fun deleteMeta(meta: MetaEntity)

    // --- AUXILIAR PARA EL ORDEN ---
    // Nos dice cuál es el número más alto para poner las nuevas metas al final de la lista.
    @Query("SELECT MAX(orden) FROM metas")
    suspend fun getMaxOrden(): Int?

    // --- (OPCIONAL) TU MÉTODO ANTERIOR ---
    // Puedes dejarlo si lo usas en alguna parte específica, pero 'updateMeta' ya hace esto.
    @Query("UPDATE metas SET montoAhorrado = :nuevoMonto WHERE id = :id")
    suspend fun updateMonto(id: Int, nuevoMonto: Double)
}