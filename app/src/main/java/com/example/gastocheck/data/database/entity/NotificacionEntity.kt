package com.example.gastocheck.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "notificaciones")
data class NotificacionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val titulo: String,
    val mensaje: String,
    val fecha: Date = Date(),
    val tipo: String, // Ej: "CREDITO", "SUSCRIPCION", "META", "SISTEMA"
    val leida: Boolean = false,
    val iconoRef: String = "Notifications" // Para mostrar un icono específico
)