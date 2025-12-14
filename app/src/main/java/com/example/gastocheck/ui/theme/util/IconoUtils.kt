package com.example.gastocheck.ui.theme.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.* // Asegúrate de tener esta dependencia o usa filled
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object IconoUtils {
    fun getIconoByName(name: String): ImageVector {
        return when(name) {
            // --- GASTOS COMUNES (Mapeo de Categoría -> Icono) ---
            "Comida", "Restaurante", "Alimentos", "Restaurant" -> Icons.Default.Restaurant
            "Transporte", "Uber", "Gasolina", "Auto", "Bus", "DirectionsCar" -> Icons.Default.DirectionsCar
            "Casa", "Renta", "Hogar", "Mantenimiento", "Home" -> Icons.Default.Home
            "Servicios", "Luz", "Agua", "Internet", "Teléfono", "Gas" -> Icons.Default.Lightbulb
            "Salud", "Farmacia", "Doctor", "Medicinas", "LocalHospital" -> Icons.Default.LocalHospital
            "Entretenimiento", "Cine", "Juegos", "Fiesta", "SportsEsports" -> Icons.Default.SportsEsports
            "Ropa", "Compras", "Shopping", "Checkroom" -> Icons.Default.Checkroom
            "Educación", "Escuela", "Libros", "Cursos", "School" -> Icons.Default.School
            "Regalos", "Donaciones" -> Icons.Default.CardGiftcard
            "Mascotas", "Veterinario", "Alimento" -> Icons.Default.Pets
            "Viajes", "Avión", "Hotel", "Vacaciones" -> Icons.Default.Flight
            "Deporte", "Gimnasio", "FitnessCenter" -> Icons.Default.FitnessCenter
            "Trabajo", "Work" -> Icons.Default.Work
            "DirectionsCar", "Auto" -> Icons.Default.DirectionsCar
            "TwoWheeler", "Moto" -> Icons.Default.TwoWheeler
            "Home", "Casa" -> Icons.Default.Home
            "Flight", "Avion", "Viaje" -> Icons.Default.Flight
            "Star", "Meta", "Estrella" -> Icons.Default.Star
            "Savings", "Ahorro", "Cerdito" -> Icons.Default.Savings
            "Smartphone", "Celular", "Telefono" -> Icons.Default.Smartphone
            "Computer", "Laptop", "PC" -> Icons.Default.Computer
            "School", "Educacion", "Escuela" -> Icons.Default.School
            "Pets", "Mascota" -> Icons.Default.Pets
            "ShoppingBag", "Compras", "Bolsa" -> Icons.Default.ShoppingBag
            "Favorite", "Salud", "Corazon" -> Icons.Default.Favorite
            "Build", "Reparacion", "Herramienta" -> Icons.Default.Build
            "FitnessCenter", "Gimnasio" -> Icons.Default.FitnessCenter
            // --- SERVICIOS / SUSCRIPCIONES ---
            "Netflix" -> Icons.Default.Tv
            "Spotify" -> Icons.Default.MusicNote // MusicNote suele verse mejor que Headphones para app
            "Youtube", "YouTube" -> Icons.Default.PlayArrow
            "Amazon", "Prime", "ShoppingCart" -> Icons.Default.ShoppingCart
            "Apple" -> Icons.Default.PhoneIphone
            "Disney" -> Icons.Default.Star
            "HBO" -> Icons.Default.Movie
            "Figma" -> Icons.Default.Brush
            "Notion" -> Icons.Default.Edit
            "Celular" -> Icons.Default.Smartphone
            "Seguro" -> Icons.Default.Security

            // --- INGRESOS ---
            "Sueldo", "Nómina", "Salario", "AttachMoney" -> Icons.Default.AttachMoney
            "Negocio", "Ventas", "Store" -> Icons.Default.Store
            "Ahorro", "Inversión", "Savings" -> Icons.Default.Savings
            "Regalo (Recibido)" -> Icons.Default.VolunteerActivism
            "Freelance", "Extra", "TrendingUp" -> Icons.Default.TrendingUp

            // --- OTROS ---
            "Transferencia" -> Icons.Default.SwapHoriz
            "Ajuste" -> Icons.Default.Build
            "CreditCard" -> Icons.Default.CreditCard
            "AccountBalance" -> Icons.Default.AccountBalance
            "Wallet" -> Icons.Default.Wallet
            "Receipt", "Otro" -> Icons.Default.Receipt

            // Default
            else -> Icons.Default.Category // Icono genérico diferente a Wallet para notar si algo falla
        }
    }
}

// Objeto auxiliar para los colores de las marcas (Mantenlo igual, está bien)
object ServiceColorUtils {
    fun getColorByName(name: String): Color {
        return when (name) {
            "Netflix", "Youtube", "YouTube" -> Color(0xFFE50914) // Rojo
            "Spotify" -> Color(0xFF1DB954) // Verde
            "Apple" -> Color(0xFFA2AAAD) // Gris Plata
            "Disney" -> Color(0xFF113CCF) // Azul
            "HBO" -> Color(0xFF9900FF) // Morado
            "Amazon" -> Color(0xFFFF9900) // Naranja
            "Figma" -> Color(0xFFF24E1E) // Rojo Naranja
            "Agua", "Internet", "Facebook" -> Color(0xFF00B0FF) // Azul Claro
            "Luz" -> Color(0xFFFFD600) // Amarillo
            "Gas" -> Color(0xFFFF5722) // Naranja Fuerte
            else -> Color(0xFF00E676) // Verde Neon por defecto
        }
    }
}