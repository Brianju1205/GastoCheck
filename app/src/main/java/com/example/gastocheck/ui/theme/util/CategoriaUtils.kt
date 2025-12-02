package com.example.gastocheck.ui.theme.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// Definimos una clase simple para manejar los datos de la categoría
data class CategoriaInfo(
    val nombre: String,
    val icono: ImageVector,
    val color: Color
)

object CategoriaUtils {
    // Lista maestra de categorías disponibles
    val listaCategorias = listOf(
        CategoriaInfo("Comida", Icons.Default.Restaurant, Color(0xFFFFB74D)), // Naranja
        CategoriaInfo("Transporte", Icons.Default.DirectionsBus, Color(0xFF64B5F6)), // Azul
        CategoriaInfo("Hogar", Icons.Default.Home, Color(0xFF81C784)), // Verde
        CategoriaInfo("Salud", Icons.Default.LocalHospital, Color(0xFFE57373)), // Rojo
        CategoriaInfo("Ocio", Icons.Default.Movie, Color(0xFFBA68C8)), // Morado
        CategoriaInfo("Ropa", Icons.Default.Checkroom, Color(0xFFFF8A65)), // Coral
        CategoriaInfo("Salario", Icons.Default.AttachMoney, Color(0xFF4CAF50)), // Verde oscuro
        CategoriaInfo("Regalo", Icons.Default.CardGiftcard, Color(0xFFF06292)), // Rosa
        CategoriaInfo("Otros", Icons.Default.MoreHoriz, Color(0xFF90A4AE)) // Gris
    )

    // Función para obtener el icono dado un nombre (string)
    fun getIcono(nombre: String): ImageVector {
        return listaCategorias.find { it.nombre == nombre }?.icono ?: Icons.Default.Category
    }

    // Función para obtener el color dado un nombre
    fun getColor(nombre: String): Color {
        return listaCategorias.find { it.nombre == nombre }?.color ?: Color.Gray
    }
}