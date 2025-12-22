package com.example.gastocheck.ui.theme.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.gastocheck.ui.theme.screens.suscripciones.SuscripcionesScreen
import com.example.gastocheck.ui.theme.screens.ajustes.AjustesScreen
import com.example.gastocheck.ui.theme.screens.estadisticas.EstadisticasScreen // <--- IMPORTANTE
import com.example.gastocheck.ui.theme.screens.transferencia.RegistrarTransferenciaScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val sharedAgregarViewModel: AgregarViewModel = hiltViewModel()

    // AGREGAMOS "estadisticas" A LA LISTA DE RUTAS CON BARRA
    val rutasConBarra = listOf("home", "cuentas_lista", "metas", "suscripciones", "estadisticas")
    val showBars = currentRoute in rutasConBarra

    Scaffold(
        bottomBar = {
            if (showBars) {
                NavigationBar {
                    // 1. INICIO
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
                    // 2. CUENTAS
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
                    // 3. METAS
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
                    // 4. SUSCRIPCIONES
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.EventRepeat, null) },
                        label = { Text("Suscr.") },
                        selected = currentRoute == "suscripciones",
                        onClick = {
                            navController.navigate("suscripciones") {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    // 5. ESTADÍSTICAS (ABREVIADO A "Stats")
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.InsertChart, null) },
                        label = { Text("Stats") }, // <--- CAMBIO AQUÍ
                        selected = currentRoute == "estadisticas",
                        onClick = {
                            navController.navigate("estadisticas") {
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
            modifier = Modifier.fillMaxSize()
        ) {

            // --- HOME ---
            composable("home") {
                Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    HomeScreen(
                        agregarViewModel = sharedAgregarViewModel,
                        onNavegarAjustes = { navController.navigate("ajustes") },
                        onNavegarAgregar = { esIngreso -> navController.navigate("agregar?id=-1&esIngreso=$esIngreso&vieneDeVoz=false") },
                        onNavegarEditar = { id -> navController.navigate("agregar?id=$id&vieneDeVoz=false") },
                        onNavegarMetas = { navController.navigate("metas") },
                        onNavegarHistorial = { accountId -> navController.navigate("historial/$accountId") },
                        onVozDetectada = { esIngresoDetectado -> navController.navigate("agregar?id=-1&esIngreso=$esIngresoDetectado&vieneDeVoz=true") },
                        onNavegarTransferencia = { id, textoVoz ->
                            val ruta = if (textoVoz != null) "registrar_transferencia?id=$id&textoAudio=$textoVoz" else "registrar_transferencia?id=$id"
                            navController.navigate(ruta)
                        }
                    )
                }
            }

            // --- ESTADISTICAS (NUEVA PANTALLA) ---
            composable("estadisticas") {
                Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    EstadisticasScreen()
                }
            }

            // --- SUSCRIPCIONES ---
            composable("suscripciones") {
                Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    SuscripcionesScreen()
                }
            }

            // --- METAS ---
            composable("metas") {
                Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    MetasScreen()
                }
            }

            // --- CUENTAS ---
            composable("cuentas_lista") {
                Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {

                    // CORRECCIÓN: Llamamos a CuentasListaScreen
                    CuentasListaScreen(
                        onNavegarDetalle = { navController.navigate("detalle_cuenta/$it") },
                        onNavegarCrear = { navController.navigate("crear_cuenta?id=-1") }
                    )
                }
            }

            // --- AJUSTES ---
            composable("ajustes") {
                AjustesScreen(onBack = { navController.popBackStack() })
            }

            // --- PANTALLAS SECUNDARIAS ---

            composable("crear_cuenta?id={id}", arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = -1 })) {
                val id = it.arguments?.getInt("id") ?: -1
                CrearCuentaScreen(idCuenta = id, onBack = { navController.popBackStack() })
            }

            composable("detalle_cuenta/{accountId}", arguments = listOf(navArgument("accountId") { type = NavType.IntType })) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getInt("accountId") ?: -1
                DetalleCuentaScreen(accountId = accountId, onBack = { navController.popBackStack() }, onVerTodos = { navController.navigate("movimientos_cuenta/$accountId") }, onEditar = { id -> navController.navigate("crear_cuenta?id=$id") }, onEditarTransaccion = { id, tipo -> if (tipo == "TRANSFERENCIA") navController.navigate("registrar_transferencia?id=$id") else navController.navigate("agregar?id=$id&vieneDeVoz=false") })
            }

            composable("movimientos_cuenta/{accountId}", arguments = listOf(navArgument("accountId") { type = NavType.IntType })) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getInt("accountId") ?: -1
                MovimientosCuentaScreen(accountId = accountId, onBack = { navController.popBackStack() }, onEditarTransaccion = { id, tipo -> if (tipo == "TRANSFERENCIA") navController.navigate("registrar_transferencia?id=$id") else navController.navigate("agregar?id=$id&vieneDeVoz=false") })
            }

            composable("registrar_transferencia?id={id}&textoAudio={textoAudio}", arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = -1 }, navArgument("textoAudio") { type = NavType.StringType; nullable = true; defaultValue = null })) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                val textoAudio = backStackEntry.arguments?.getString("textoAudio")
                RegistrarTransferenciaScreen(idTransaccion = id, textoInicial = textoAudio, onBack = { navController.popBackStack() })
            }

            composable("historial/{accountId}", arguments = listOf(navArgument("accountId") { type = NavType.IntType })) {
                HistorialScreen(onBack = { navController.popBackStack() })
            }

            composable("agregar?id={id}&esIngreso={esIngreso}&vieneDeVoz={vieneDeVoz}", arguments = listOf(navArgument("id") { type = NavType.IntType; defaultValue = -1 }, navArgument("esIngreso") { type = NavType.BoolType; defaultValue = false }, navArgument("vieneDeVoz") { type = NavType.BoolType; defaultValue = false })) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("id") ?: -1
                val esIngreso = backStackEntry.arguments?.getBoolean("esIngreso") ?: false
                val vieneDeVoz = backStackEntry.arguments?.getBoolean("vieneDeVoz") ?: false
                LaunchedEffect(Unit) { sharedAgregarViewModel.inicializar(id, esIngreso, vieneDeVoz) }
                AgregarScreen(viewModel = sharedAgregarViewModel, alRegresar = { navController.popBackStack() })
            }
        }
    }
}