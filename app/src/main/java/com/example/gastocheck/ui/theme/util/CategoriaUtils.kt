package com.example.gastocheck.ui.theme.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// Modelo de datos simple para la categoría
data class Categoria(
    val nombre: String,
    val icono: ImageVector,
    val color: Color
)

object CategoriaUtils {

    // --- PALETA DE COLORES ---
    private val ColorComida = Color(0xFFFF9800) // Naranja
    private val ColorTransporte = Color(0xFF2196F3) // Azul
    private val ColorHogar = Color(0xFF795548) // Café
    private val ColorSalud = Color(0xFFF44336) // Rojo
    private val ColorEntretenimiento = Color(0xFF9C27B0) // Morado
    private val ColorCompras = Color(0xFFE91E63) // Rosa
    private val ColorEducacion = Color(0xFF3F51B5) // Indigo
    private val ColorServicios = Color(0xFF00BCD4) // Cyan
    private val ColorMascotas = Color(0xFF8D6E63) // Marrón claro
    private val ColorViajes = Color(0xFFFFC107) // Amber

    // Colores Ingresos (Verdes y Dorados)
    private val ColorSalario = Color(0xFF4CAF50) // Verde Base
    private val ColorNegocio = Color(0xFF009688) // Teal
    private val ColorInversiones = Color(0xFF673AB7) // Deep Purple
    private val ColorRegalos = Color(0xFFFFD700) // Dorado
    private val ColorAhorros = Color(0xFF8BC34A) // Verde Claro
    private val ColorOtros = Color(0xFF607D8B) // Gris azulado

    // --- LISTA DE GASTOS (Más completa) ---
    val listaGastos = listOf(
        Categoria("Comida", Icons.Default.Restaurant, ColorComida),
        Categoria("Transporte", Icons.Default.DirectionsCar, ColorTransporte),
        Categoria("Hogar", Icons.Default.Home, ColorHogar),
        Categoria("Salud", Icons.Default.LocalHospital, ColorSalud),
        Categoria("Entretenimiento", Icons.Default.Movie, ColorEntretenimiento),
        Categoria("Compras", Icons.Default.ShoppingBag, ColorCompras),
        Categoria("Educación", Icons.Default.School, ColorEducacion),
        Categoria("Servicios", Icons.Default.Lightbulb, ColorServicios),
        Categoria("Mascotas", Icons.Default.Pets, ColorMascotas),
        Categoria("Viajes", Icons.Default.Flight, ColorViajes),
        Categoria("Ropa", Icons.Default.Checkroom, ColorCompras),
        Categoria("Deudas", Icons.Default.CreditCard, Color(0xFFD32F2F)),
        Categoria("Otros", Icons.Default.MoreHoriz, ColorOtros)
    )

    // --- LISTA DE INGRESOS (Específicos) ---
    val listaIngresos = listOf(
        Categoria("Salario", Icons.Default.Work, ColorSalario),
        Categoria("Negocio", Icons.Default.Store, ColorNegocio),
        Categoria("Ventas", Icons.Default.Sell, Color(0xFF2E7D32)),
        Categoria("Inversiones", Icons.Default.TrendingUp, ColorInversiones),
        Categoria("Regalos", Icons.Default.CardGiftcard, ColorRegalos),
        Categoria("Préstamo", Icons.Default.AccountBalance, Color(0xFF03A9F4)),
        Categoria("Freelance", Icons.Default.Computer, Color(0xFF00BCD4)),
        Categoria("Reembolso", Icons.Default.Undo, Color(0xFFFF5722)),
        Categoria("Ahorros", Icons.Default.Savings, ColorAhorros),
        Categoria("Otros Ingresos", Icons.Default.AttachMoney, ColorOtros)
    )

    // Lista combinada para búsquedas generales (Historial, Detalles)
    val listaCategorias = listaGastos + listaIngresos

    // --- FUNCIONES HELPER ---

    fun getIcono(nombreCategoria: String): ImageVector {
        // Busca en la lista completa por nombre
        return listaCategorias.find { it.nombre.equals(nombreCategoria, ignoreCase = true) }?.icono
            ?: Icons.Default.Category // Fallback
    }

    fun getColor(nombreCategoria: String): Color {
        return listaCategorias.find { it.nombre.equals(nombreCategoria, ignoreCase = true) }?.color
            ?: Color.Gray // Fallback
    }

    // Esta función decide qué lista mostrar en la pantalla de Agregar
    fun obtenerCategoriasPorTipo(esIngreso: Boolean): List<Categoria> {
        return if (esIngreso) listaIngresos else listaGastos
    }
}