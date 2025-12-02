package com.example.gastocheck.ui.theme.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.gastocheck.ui.theme.screens.agregar.AgregarScreen
import com.example.gastocheck.ui.theme.screens.home.HomeScreen
import com.example.gastocheck.ui.theme.screens.metas.MetasScreen
import com.example.gastocheck.ui.theme.screens.voz.ConfirmacionVozScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute?.startsWith("home") == true || currentRoute == "metas") {
                NavigationBar {
                    NavigationBarItem(icon = { Icon(Icons.Default.Home, null) }, label = { Text("Inicio") }, selected = currentRoute?.startsWith("home") == true, onClick = { navController.navigate("home") { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } })
                    NavigationBarItem(icon = { Icon(Icons.Default.Star, null) }, label = { Text("Metas") }, selected = currentRoute == "metas", onClick = { navController.navigate("metas") { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } })
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = "home", modifier = Modifier.padding(innerPadding)) {
            composable("home") {
                HomeScreen(
                    onNavegarAgregar = { es -> navController.navigate("agregar?esIngreso=$es") },
                    onNavegarEditar = { id -> navController.navigate("agregar?id=$id") },
                    onNavegarMetas = { navController.navigate("metas") },
                    onVozDetectada = { txt -> navController.navigate("confirmacion_voz?texto=$txt") }
                )
            }
            composable("metas") { MetasScreen() }
            composable("agregar?id={id}&esIngreso={esIngreso}", arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = -1 }, navArgument("esIngreso") { type = NavType.BoolType; defaultValue = false })) {
                AgregarScreen(alRegresar = { navController.popBackStack() })
            }
            composable("confirmacion_voz?texto={texto}", arguments = listOf(navArgument("texto") { type = NavType.StringType; defaultValue = "" })) { backStackEntry ->
                val texto = backStackEntry.arguments?.getString("texto") ?: ""
                ConfirmacionVozScreen(textoDetectado = texto, onConfirmar = { navController.popBackStack("home", inclusive = false) }, onCancelar = { navController.popBackStack() })
            }
        }
    }
}
