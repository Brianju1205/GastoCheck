package com.example.gastocheck.ui.theme.navigation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.gastocheck.ui.theme.screens.agregar.AgregarScreen
import com.example.gastocheck.ui.theme.screens.agregar.AgregarViewModel
import com.example.gastocheck.ui.theme.screens.cuentas.*
import com.example.gastocheck.ui.theme.screens.historial.HistorialScreen
import com.example.gastocheck.ui.theme.screens.home.HomeScreen
import com.example.gastocheck.ui.theme.screens.metas.MetasScreen
import com.example.gastocheck.ui.theme.screens.transferencia.RegistrarTransferenciaScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // --- INSTANCIA COMPARTIDA DEL VIEWMODEL (Para manejar la voz desde el Home) ---
    val sharedAgregarViewModel: AgregarViewModel = hiltViewModel()

    val rutasConBarra = listOf("home", "cuentas_lista", "metas")
    val showBars = currentRoute in rutasConBarra

    Scaffold(
        bottomBar = {
            if (showBars) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("Inicio") },
                        selected = currentRoute == "home",
                        onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AccountBalance, null) },
                        label = { Text("Cuentas") },
                        selected = currentRoute == "cuentas_lista",
                        onClick = {
                            navController.navigate("cuentas_lista") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Star, null) },
                        label = { Text("Metas") },
                        selected = currentRoute == "metas",
                        onClick = {
                            navController.navigate("metas") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {

            composable("home") {
                HomeScreen(
                    agregarViewModel = sharedAgregarViewModel,
                    onNavegarAgregar = { esIngreso ->
                        navController.navigate("agregar?id=-1&esIngreso=$esIngreso&vieneDeVoz=false")
                    },
                    onNavegarEditar = { id ->
                        navController.navigate("agregar?id=$id&vieneDeVoz=false")
                    },
                    onNavegarMetas = { navController.navigate("metas") },
                    onNavegarHistorial = { accountId ->
                        navController.navigate("historial/$accountId")
                    },
                    onVozDetectada = { esIngresoDetectado ->
                        navController.navigate("agregar?id=-1&esIngreso=$esIngresoDetectado&vieneDeVoz=true")
                    },
                    // --- ACTUALIZADO: ACEPTA ID Y TEXTO DE VOZ OPCIONAL ---
                    onNavegarTransferencia = { id, textoVoz ->
                        val ruta = if (textoVoz != null) {
                            "registrar_transferencia?id=$id&textoAudio=$textoVoz"
                        } else {
                            "registrar_transferencia?id=$id"
                        }
                        navController.navigate(ruta)
                    }
                )
            }

            composable("metas") { MetasScreen() }

            composable("cuentas_lista") {
                CuentasListaScreen(
                    onNavegarDetalle = { navController.navigate("detalle_cuenta/$it") },
                    onNavegarCrear = { navController.navigate("crear_cuenta") }
                )
            }

            composable("crear_cuenta") {
                CrearCuentaScreen(onBack = { navController.popBackStack() })
            }

            composable(
                "detalle_cuenta/{accountId}",
                arguments = listOf(navArgument("accountId") { type = NavType.IntType })
            ) { backStackEntry ->
                DetalleCuentaScreen(
                    accountId = backStackEntry.arguments?.getInt("accountId") ?: -1,
                    onBack = { navController.popBackStack() }
                )
            }

            // --- RUTA TRANSFERENCIA ACTUALIZADA (Soporta ID y Texto Audio) ---
            composable(
                route = "registrar_transferencia?id={id}&textoAudio={textoAudio}",
                arguments = listOf(
                    navArgument("id") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("textoAudio") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                val textoAudio = backStackEntry.arguments?.getString("textoAudio")

                RegistrarTransferenciaScreen(
                    idTransaccion = id,
                    textoInicial = textoAudio, // Pasamos el texto detectado
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                "historial/{accountId}",
                arguments = listOf(navArgument("accountId") { type = NavType.IntType })
            ) {
                HistorialScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = "agregar?id={id}&esIngreso={esIngreso}&vieneDeVoz={vieneDeVoz}",
                arguments = listOf(
                    navArgument("id") { type = NavType.IntType; defaultValue = -1 },
                    navArgument("esIngreso") { type = NavType.BoolType; defaultValue = false },
                    navArgument("vieneDeVoz") { type = NavType.BoolType; defaultValue = false }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                val esIngreso = backStackEntry.arguments?.getBoolean("esIngreso") ?: false
                val vieneDeVoz = backStackEntry.arguments?.getBoolean("vieneDeVoz") ?: false

                LaunchedEffect(Unit) {
                    sharedAgregarViewModel.inicializar(id, esIngreso, vieneDeVoz)
                }

                AgregarScreen(
                    viewModel = sharedAgregarViewModel,
                    alRegresar = { navController.popBackStack() }
                )
            }
        }
    }
}