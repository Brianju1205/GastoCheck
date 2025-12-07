package com.example.gastocheck.ui.theme.screens.home

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.ui.theme.components.BalanceCarousel
import com.example.gastocheck.ui.theme.components.DialogoProcesando
import com.example.gastocheck.ui.theme.components.DonutChart
import com.example.gastocheck.ui.theme.components.DonutChartGeneric
import com.example.gastocheck.ui.theme.screens.agregar.AgregarViewModel
import com.example.gastocheck.ui.theme.screens.agregar.AgregarViewModel.EstadoVoz
import com.example.gastocheck.ui.theme.util.CategoriaUtils
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import com.example.gastocheck.ui.theme.util.DateUtils
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    agregarViewModel: AgregarViewModel = hiltViewModel(),
    onNavegarAgregar: (Boolean) -> Unit,
    onNavegarEditar: (Int) -> Unit,
    onNavegarMetas: () -> Unit,
    onNavegarHistorial: (Int) -> Unit,
    onVozDetectada: (Boolean) -> Unit,
    onNavegarTransferencia: (Int, String?) -> Unit // <--- Actualizado: ID y TextoOpcional
) {
    val saldoTotal by viewModel.saldoTotal.collectAsState()
    val historial by viewModel.historialSaldos.collectAsState()
    val listaTransacciones by viewModel.transaccionesFiltradas.collectAsState()
    val filtroTiempo by viewModel.filtroTiempo.collectAsState()
    val rangoFechas by viewModel.rangoFechas.collectAsState()
    var mostrarSelectorRango by remember { mutableStateOf(false) }

    val listaMetas by viewModel.metas.collectAsState()
    val cuentaSeleccionadaId by viewModel.cuentaSeleccionadaId.collectAsState()
    val cuentas by viewModel.cuentas.collectAsState()

    val tabs = listOf("Inicio", "Gastos", "Ingresos", "Transferencias")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    var mostrarMenuAgregar by remember { mutableStateOf(false) }
    var mostrarMenuCuentas by remember { mutableStateOf(false) }
    var mostrarMenuFiltro by remember { mutableStateOf(false) }

    var transaccionSeleccionada by remember { mutableStateOf<TransaccionEntity?>(null) }
    var mostrarDetalle by remember { mutableStateOf(false) }
    var mostrarConfirmacionBorrar by remember { mutableStateOf(false) }

    val nombreCuentaActual = if (cuentaSeleccionadaId == -1) "Todas las cuentas" else cuentas.find { it.id == cuentaSeleccionadaId }?.nombre ?: "Seleccionar"

    val estadoVoz by agregarViewModel.estadoVoz.collectAsState()
    val haptic = LocalHapticFeedback.current

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) agregarViewModel.iniciarEscuchaInteligente()
    }

    LaunchedEffect(estadoVoz) {
        if (estadoVoz is EstadoVoz.Exito) {
            val exito = estadoVoz as EstadoVoz.Exito
            onVozDetectada(exito.esIngreso)
            agregarViewModel.reiniciarEstadoVoz()
        }
    }

    // --- ESCUCHAR REDIRECCIÓN AUTOMÁTICA A TRANSFERENCIA ---
    LaunchedEffect(Unit) {
        agregarViewModel.redireccionTransferencia.collect { textoVoz ->
            // -1 porque es una NUEVA transferencia
            onNavegarTransferencia(-1, textoVoz)
        }
    }

    // --- DIALOGOS ---
    if (mostrarSelectorRango) {
        val datePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorRango = false },
            confirmButton = {
                TextButton(onClick = {
                    val inicio = datePickerState.selectedStartDateMillis
                    val fin = datePickerState.selectedEndDateMillis ?: inicio
                    if (inicio != null) viewModel.establecerRangoFechas(inicio, fin!!)
                    mostrarSelectorRango = false
                }) { Text("Aplicar") }
            },
            dismissButton = { TextButton(onClick = { mostrarSelectorRango = false }) { Text("Cancelar") } }
        ) {
            DateRangePicker(
                state = datePickerState,
                title = { Text("Seleccionar rango", modifier = Modifier.padding(16.dp)) },
                modifier = Modifier.height(500.dp),
                showModeToggle = false
            )
        }
    }

    when (estadoVoz) {
        is EstadoVoz.Escuchando -> DialogoEscuchandoAnimado(onDismiss = { agregarViewModel.reiniciarEstadoVoz() })
        is EstadoVoz.ProcesandoIA -> DialogoProcesando()
        else -> {}
    }

    if (mostrarDetalle && transaccionSeleccionada != null) {
        val t = transaccionSeleccionada!!
        val nombreCuenta = cuentas.find { it.id == t.cuentaId }?.nombre ?: "Cuenta"
        DetalleTransaccionDialog(
            transaccion = t,
            nombreCuenta = nombreCuenta,
            onDismiss = { mostrarDetalle = false },
            onDelete = { mostrarConfirmacionBorrar = true },
            onEdit = {
                mostrarDetalle = false
                if (t.categoria == "Transferencia") {
                    onNavegarTransferencia(t.id, null) // Edición normal (sin texto de voz)
                } else {
                    onNavegarEditar(t.id)
                }
            }
        )
    }

    if (mostrarConfirmacionBorrar && transaccionSeleccionada != null) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionBorrar = false },
            icon = { Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Eliminar movimiento?") },
            text = { Text("Esta acción no se puede deshacer y ajustará tus saldos.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.borrarTransaccion(transaccionSeleccionada!!)
                        mostrarConfirmacionBorrar = false
                        mostrarDetalle = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { mostrarConfirmacionBorrar = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                CenterAlignedTopAppBar(
                    title = { Text("Mi Dinero", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    navigationIcon = { Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp)) }
                )
                ScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 16.dp,
                    indicator = { tabPositions -> TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]), color = MaterialTheme.colorScheme.primary) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            text = { Text(title, fontWeight = if(pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal) },
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            val paginaActual = pagerState.currentPage

            // Colores menú FAB
            val fabMenuBackground = MaterialTheme.colorScheme.surfaceContainerHighest
            val fabMenuContent = MaterialTheme.colorScheme.onSurface
            val fabMenuIconColor = MaterialTheme.colorScheme.primary

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (paginaActual == 0 && mostrarMenuAgregar) {
                    // Voz
                    SmallFloatingActionButton(
                        onClick = { mostrarMenuAgregar = false; permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        containerColor = fabMenuBackground, contentColor = fabMenuContent
                    ) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Mic, null, tint = fabMenuIconColor); Spacer(Modifier.width(8.dp)); Text("Voz", fontWeight = FontWeight.Bold) } }

                    // Transferencia (Botón manual)
                    SmallFloatingActionButton(
                        onClick = { mostrarMenuAgregar = false; onNavegarTransferencia(-1, null) },
                        containerColor = fabMenuBackground, contentColor = fabMenuContent
                    ) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.SwapHoriz, null, tint = fabMenuIconColor); Spacer(Modifier.width(8.dp)); Text("Transferencia", fontWeight = FontWeight.Bold) } }

                    // Meta
                    SmallFloatingActionButton(
                        onClick = { mostrarMenuAgregar = false; onNavegarMetas() },
                        containerColor = fabMenuBackground, contentColor = fabMenuContent
                    ) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700)); Spacer(Modifier.width(8.dp)); Text("Meta", fontWeight = FontWeight.Bold) } }

                    // Ingreso
                    SmallFloatingActionButton(
                        onClick = { mostrarMenuAgregar = false; onNavegarAgregar(true) },
                        containerColor = fabMenuBackground, contentColor = fabMenuContent
                    ) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ArrowUpward, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(8.dp)); Text("Ingreso", fontWeight = FontWeight.Bold) } }

                    // Gasto
                    SmallFloatingActionButton(
                        onClick = { mostrarMenuAgregar = false; onNavegarAgregar(false) },
                        containerColor = fabMenuBackground, contentColor = fabMenuContent
                    ) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.ArrowDownward, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(8.dp)); Text("Gasto", fontWeight = FontWeight.Bold) } }
                }

                // Botón Principal
                Surface(
                    shape = FloatingActionButtonDefaults.shape,
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shadowElevation = 6.dp,
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            when (paginaActual) {
                                0 -> mostrarMenuAgregar = !mostrarMenuAgregar
                                1 -> onNavegarAgregar(false)
                                2 -> onNavegarAgregar(true)
                                3 -> onNavegarTransferencia(-1, null) // Manual
                            }
                        },
                        onLongClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                    )
                ) {
                    Box(modifier = Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 56.dp), contentAlignment = Alignment.Center) {
                        val iconoFab = if (paginaActual == 0 && mostrarMenuAgregar) Icons.Default.Close else Icons.Default.Add
                        Icon(iconoFab, contentDescription = "Agregar")
                    }
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding).fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) { page ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                when (page) {
                    0 -> { // --- INICIO ---
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Surface(onClick = { mostrarMenuCuentas = true }, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                                    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = nombreCuentaActual, style = MaterialTheme.typography.labelLarge)
                                        Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(expanded = mostrarMenuCuentas, onDismissRequest = { mostrarMenuCuentas = false }) {
                                    DropdownMenuItem(text = { Text("Todas las cuentas") }, onClick = { viewModel.cambiarFiltroCuenta(-1); mostrarMenuCuentas = false })
                                    cuentas.forEach { cuenta -> DropdownMenuItem(text = { Text(cuenta.nombre) }, onClick = { viewModel.cambiarFiltroCuenta(cuenta.id); mostrarMenuCuentas = false }) }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            BalanceCarousel(saldoActual = saldoTotal, historial = historial, onVerMasClick = { onNavegarHistorial(cuentaSeleccionadaId) })
                        }
                        if (listaMetas.isNotEmpty()) { item { val m = listaMetas.last(); CardProgresoAhorro(m.nombre, m.montoAhorrado, m.montoObjetivo) } }

                        item { Text("Últimos Movimientos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }

                        val ultimos = listaTransacciones.filter {
                            !(it.categoria == "Transferencia" && it.esIngreso)
                        }.take(6)

                        items(ultimos) { t ->
                            val nombreCuenta = cuentas.find { it.id == t.cuentaId }?.nombre ?: "Efectivo"

                            if (t.categoria == "Transferencia") {
                                ItemTransferencia(
                                    transaccion = t,
                                    nombreCuentaOrigen = nombreCuenta,
                                    onItemClick = { transaccionSeleccionada = t; mostrarDetalle = true },
                                    onEdit = { onNavegarTransferencia(t.id, null) },
                                    onDelete = {
                                        transaccionSeleccionada = t
                                        mostrarConfirmacionBorrar = true
                                    }
                                )
                            } else {
                                ItemTransaccionModerno(
                                    transaccion = t,
                                    nombreCuenta = nombreCuenta,
                                    onEdit = { onNavegarEditar(t.id) },
                                    onDelete = {
                                        transaccionSeleccionada = t
                                        mostrarConfirmacionBorrar = true
                                    },
                                    onItemClick = { transaccionSeleccionada = t; mostrarDetalle = true }
                                )
                            }
                        }

                        if (ultimos.isEmpty()) {
                            item { Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { Text("No hay movimientos recientes", color = Color.Gray) } }
                        }
                    }

                    1, 2, 3 -> { // GASTOS, INGRESOS, TRANSFERENCIAS
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                                val tituloFiltro = when(filtroTiempo) {
                                    FiltroTiempo.DIA -> "Hoy"
                                    FiltroTiempo.SEMANA -> "Esta Semana"
                                    FiltroTiempo.MES -> "Este Mes"
                                    FiltroTiempo.ANIO -> "Este Año"
                                    FiltroTiempo.RANGO -> if (rangoFechas != null) DateUtils.formatearRango(rangoFechas!!.first, rangoFechas!!.second) else "Rango"
                                }
                                Text(text = tituloFiltro, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .combinedClickable(
                                                onClick = { mostrarMenuFiltro = true },
                                                onLongClick = { mostrarSelectorRango = true }
                                            )
                                            .padding(8.dp)
                                    ) { Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary) }

                                    DropdownMenu(expanded = mostrarMenuFiltro, onDismissRequest = { mostrarMenuFiltro = false }) {
                                        DropdownMenuItem(text = { Text("Día") }, onClick = { viewModel.cambiarFiltroTiempo(FiltroTiempo.DIA); mostrarMenuFiltro = false })
                                        DropdownMenuItem(text = { Text("Semana") }, onClick = { viewModel.cambiarFiltroTiempo(FiltroTiempo.SEMANA); mostrarMenuFiltro = false })
                                        DropdownMenuItem(text = { Text("Mes") }, onClick = { viewModel.cambiarFiltroTiempo(FiltroTiempo.MES); mostrarMenuFiltro = false })
                                        DropdownMenuItem(text = { Text("Año") }, onClick = { viewModel.cambiarFiltroTiempo(FiltroTiempo.ANIO); mostrarMenuFiltro = false })
                                        DropdownMenuItem(text = { Text("Rango") }, onClick = { mostrarMenuFiltro = false; mostrarSelectorRango = true })
                                    }
                                }
                            }
                        }

                        val listaFiltrada = when(page) {
                            1 -> listaTransacciones.filter { !it.esIngreso && it.categoria != "Transferencia" }
                            2 -> listaTransacciones.filter { it.esIngreso && it.categoria != "Transferencia" }
                            3 -> listaTransacciones.filter { it.categoria == "Transferencia" && !it.esIngreso }
                            else -> emptyList()
                        }

                        item {
                            if (listaFiltrada.isNotEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), contentAlignment = Alignment.Center) {
                                    if (page == 3) {
                                        val datosCuentas = listaFiltrada.groupBy { t ->
                                            cuentas.find { it.id == t.cuentaId }?.nombre ?: "Desconocida"
                                        }.mapValues { entry -> entry.value.sumOf { it.monto } }
                                        DonutChartGeneric(datos = datosCuentas, size = 200.dp)
                                    } else {
                                        DonutChart(transacciones = listaFiltrada, size = 200.dp)
                                    }
                                }
                            }
                        }

                        val agrupado = listaFiltrada.groupBy { DateUtils.formatearFechaAmigable(it.fecha) }
                        agrupado.forEach { (f, ts) ->
                            item { Text(f, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), textAlign = TextAlign.Center) }
                            items(ts) { t ->
                                val nombreCuenta = cuentas.find { it.id == t.cuentaId }?.nombre ?: "Cuenta"
                                if (page == 3) {
                                    ItemTransferencia(
                                        transaccion = t,
                                        nombreCuentaOrigen = nombreCuenta,
                                        onItemClick = { transaccionSeleccionada = t; mostrarDetalle = true },
                                        onEdit = { onNavegarTransferencia(t.id, null) },
                                        onDelete = {
                                            transaccionSeleccionada = t
                                            mostrarConfirmacionBorrar = true
                                        }
                                    )
                                } else {
                                    ItemTransaccionModerno(
                                        transaccion = t,
                                        nombreCuenta = nombreCuenta,
                                        onEdit = { onNavegarEditar(t.id) },
                                        onDelete = {
                                            transaccionSeleccionada = t
                                            mostrarConfirmacionBorrar = true
                                        },
                                        onItemClick = { transaccionSeleccionada = t; mostrarDetalle = true }
                                    )
                                }
                            }
                        }
                        if (listaFiltrada.isEmpty()) item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No hay movimientos", color = Color.Gray) } }
                    }
                }
            }
        }
    }
}

// --- COMPONENTES VISUALES ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemTransaccionModerno(
    transaccion: TransaccionEntity,
    nombreCuenta: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onItemClick: () -> Unit = {}
) {
    var mostrarMenu by remember { mutableStateOf(false) }
    val icono = CategoriaUtils.getIcono(transaccion.categoria)
    val colorIcono = CategoriaUtils.getColor(transaccion.categoria)
    val signo = if (transaccion.esIngreso) "+" else "-"
    val colorMonto = if (transaccion.esIngreso) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Box {
        Row(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onItemClick() }, onLongClick = { mostrarMenu = true }).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Icon(imageVector = icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = transaccion.categoria, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = nombreCuenta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = "$signo${CurrencyUtils.formatCurrency(transaccion.monto)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colorMonto)
        }
        DropdownMenu(expanded = mostrarMenu, onDismissRequest = { mostrarMenu = false }) {
            DropdownMenuItem(text = { Text("Editar") }, onClick = { mostrarMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
            DropdownMenuItem(text = { Text("Eliminar") }, onClick = { mostrarMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null) })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemTransferencia(
    transaccion: TransaccionEntity,
    nombreCuentaOrigen: String,
    onItemClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val nombreDestino = transaccion.notaCompleta.replace("Transferencia a ", "")
    var mostrarMenu by remember { mutableStateOf(false) }

    val textColor = MaterialTheme.colorScheme.onBackground
    val iconBgColor = MaterialTheme.colorScheme.surfaceVariant
    val arrowColor = MaterialTheme.colorScheme.outline
    val montoColor = Color(0xFF2979FF)
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = { mostrarMenu = true }
            )
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(iconBgColor), contentAlignment = Alignment.Center) {
                Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = textColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = nombreCuentaOrigen, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = textColor)
                    PaddingValues(horizontal = 4.dp)
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp), tint = arrowColor)
                    PaddingValues(horizontal = 4.dp)
                    Text(text = nombreDestino, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = textColor)
                }
                Text(text = "Transferencia", style = MaterialTheme.typography.bodySmall, color = subTextColor)
            }
            Text(
                text = "-${CurrencyUtils.formatCurrency(transaccion.monto)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = montoColor
            )
        }

        DropdownMenu(expanded = mostrarMenu, onDismissRequest = { mostrarMenu = false }) {
            DropdownMenuItem(text = { Text("Editar") }, onClick = { mostrarMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, null) })
            DropdownMenuItem(text = { Text("Eliminar") }, onClick = { mostrarMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null) })
        }
    }
    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
}

// ... Resto de Dialogos (Detalle, Escuchando, etc) sin cambios ...
@Composable
fun DetalleTransaccionDialog(transaccion: TransaccionEntity, nombreCuenta: String, onDismiss: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = { Icon(CategoriaUtils.getIcono(transaccion.categoria), null, tint = CategoriaUtils.getColor(transaccion.categoria), modifier = Modifier.size(48.dp)) },
        title = { Text(transaccion.categoria) },
        text = {
            Column {
                Text(CurrencyUtils.formatCurrency(transaccion.monto), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(nombreCuenta, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                Text(transaccion.notaCompleta.ifEmpty { "Sin nota" })
                Spacer(Modifier.height(4.dp))
                Text(DateUtils.formatearFechaAmigable(transaccion.fecha), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onEdit) { Text("Editar") } },
        dismissButton = { Row { TextButton(onClick = onDelete) { Text("Eliminar") }; TextButton(onClick = onDismiss) { Text("Cerrar") } } }
    )
}

@Composable
fun DialogoEscuchandoAnimado(onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.2f, animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse))
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(80.dp).scale(scale).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text("Escuchando...", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CardProgresoAhorro(nombre: String, ahorrado: Double, meta: Double) {
    val progreso = (ahorrado / meta).toFloat().coerceIn(0f, 1f)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Meta: $nombre", fontWeight = FontWeight.Bold)
                Text("${CurrencyUtils.formatCurrency(ahorrado)} / ${CurrencyUtils.formatCurrency(meta)}", fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
        }
    }
}

@Composable
fun DialogoEditarSaldo(saldoActual: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var texto by remember { mutableStateOf(saldoActual.toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Ajustar Saldo Base") }, text = { OutlinedTextField(value = texto, onValueChange = { texto = it }, label = { Text("Saldo Inicial ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }, confirmButton = { Button(onClick = { val n = texto.toDoubleOrNull(); if (n != null) onConfirm(n) }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}