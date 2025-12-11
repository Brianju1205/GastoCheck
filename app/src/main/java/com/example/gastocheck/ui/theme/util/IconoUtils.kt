package com.example.gastocheck.ui.theme.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object IconoUtils {
    fun getIconoByName(name: String): ImageVector {
        return when(name) {
            // --- Iconos Generales de la App ---
            "Wallet" -> Icons.Default.Wallet
            "CreditCard" -> Icons.Default.CreditCard
            "Savings" -> Icons.Default.Savings
            "AttachMoney" -> Icons.Default.AttachMoney
            "AccountBalance" -> Icons.Default.AccountBalance
            "ShoppingCart" -> Icons.Default.ShoppingCart
            "Work" -> Icons.Default.Work
            "TrendingUp" -> Icons.Default.TrendingUp
            "Home" -> Icons.Default.Home
            "School" -> Icons.Default.School
            "Restaurant" -> Icons.Default.Restaurant
            "DirectionsCar" -> Icons.Default.DirectionsCar
            "LocalHospital" -> Icons.Default.LocalHospital
            "SportsEsports" -> Icons.Default.SportsEsports
            "Checkroom" -> Icons.Default.Checkroom

            // --- Iconos de Suscripciones (NUEVOS) ---
            "Netflix" -> Icons.Default.Tv
            "Spotify" -> Icons.Default.MusicNote
            "Youtube" -> Icons.Default.PlayArrow
            "Apple" -> Icons.Default.PhoneIphone
            "Disney" -> Icons.Default.Star
            "HBO" -> Icons.Default.Movie
            "Amazon" -> Icons.Default.ShoppingCart
            "Figma" -> Icons.Default.Brush // O School si prefieres
            "Notion" -> Icons.Default.Edit
            "Colegio" -> Icons.Default.School
            "Agua" -> Icons.Default.WaterDrop
            "Luz" -> Icons.Default.Lightbulb
            "Gas" -> Icons.Default.LocalFireDepartment
            "Internet" -> Icons.Default.Wifi
            "Celular" -> Icons.Default.Smartphone
            "Gimnasio" -> Icons.Default.FitnessCenter
            "Seguro" -> Icons.Default.Security
            "Otro" -> Icons.Default.Receipt

            // Default
            else -> Icons.Default.Wallet
        }
    }
}

// Objeto auxiliar para los colores de las marcas
object ServiceColorUtils {
    fun getColorByName(name: String): Color {
        return when (name) {
            "Netflix", "Youtube" -> Color(0xFFE50914) // Rojo
            "Spotify" -> Color(0xFF1DB954) // Verde
            "Apple" -> Color(0xFFA2AAAD) // Gris Plata
            "Disney" -> Color(0xFF113CCF) // Azul
            "HBO" -> Color(0xFF9900FF) // Morado
            "Amazon" -> Color(0xFFFF9900) // Naranja
            "Figma" -> Color(0xFFF24E1E) // Rojo Naranja
            "Agua", "Internet" -> Color(0xFF00B0FF) // Azul Claro
            "Luz" -> Color(0xFFFFD600) // Amarillo
            "Gas" -> Color(0xFFFF5722) // Naranja Fuerte
            else -> Color(0xFF00E676) // Verde Neon por defecto
        }
    }
}