package com.example.gastocheck.ui.theme.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconoUtils {
    fun getIconoByName(name: String): ImageVector {
        return when(name) {
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
            else -> Icons.Default.Wallet
        }
    }
}