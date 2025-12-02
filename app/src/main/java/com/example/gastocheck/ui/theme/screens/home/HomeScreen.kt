package com.example.gastocheck.ui.theme.screens.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.data.database.entity.MetaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.ui.theme.components.DialogoProcesando
import com.example.gastocheck.ui.theme.components.DonutChart
import com.example.gastocheck.ui.theme.screens.agregar.AgregarViewModel
import com.example.gastocheck.ui.theme.screens.agregar.AgregarViewModel.EstadoVoz
import com.example.gastocheck.ui.theme.util.CategoriaUtils
import com.example.gastocheck.ui.theme.util.CurrencyUtils // <--- IMPORTANTE
import com.example.gastocheck.ui.theme.util.DateUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    agregarViewModel: AgregarViewModel = hiltViewModel(),
    onNavegarAgregar: (Boolean) -> Unit,
    onNavegarEditar: (Int) -> Unit,
    onNavegarMetas: () -> Unit,
    onVozDetectada: (String) -> Unit
) {
    val listaTransacciones by viewModel.transacciones.collectAsState()
    val listaMetas by viewModel.metas.collectAsState()
    val saldoBase by viewModel.saldoManual.collectAsState()

    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Resumen", "Gastos", "Ingresos")

    val totalIngresos = listaTransacciones.filter { it.esIngreso }.sumOf { it.monto }
    val totalGastos = listaTransacciones.filter { !it.esIngreso }.sumOf { it.monto }
    val saldoDisponible = (saldoBase + totalIngresos) - totalGastos

    // --- COLOR DINÁMICO DEL SALDO ---
    val colorSaldo = if (saldoDisponible < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    var mostrarMenuAgregar by remember { mutableStateOf(false) }
    var mostrarDialogoSaldo by remember { mutableStateOf(false) }
    var transaccionSeleccionada by remember { mutableStateOf<TransaccionEntity?>(null) }
    var mostrarDetalle by remember { mutableStateOf(false) }

    val estadoVoz by agregarViewModel.estadoVoz.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) agregarViewModel.iniciarEscuchaInteligente()
    }
    fun activarMicrofono() { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }

    when (estadoVoz) {
        is EstadoVoz.Escuchando -> DialogoEscuchandoAnimado(onDismiss = { agregarViewModel.reiniciarEstadoVoz() })
        is EstadoVoz.ProcesandoIA -> DialogoProcesando()
        is EstadoVoz.Exito -> {
            val texto = (estadoVoz as EstadoVoz.Exito).texto
            agregarViewModel.reiniciarEstadoVoz()
            onVozDetectada(texto)
        }
        is EstadoVoz.Error -> { LaunchedEffect(Unit) { agregarViewModel.reiniciarEstadoVoz() } }
        else -> {}
    }

    if (mostrarDetalle && transaccionSeleccionada != null) {
        val t = transaccionSeleccionada!!
        DetalleTransaccionDialog(
            transaccion = t,
            onDismiss = { mostrarDetalle = false },
            onDelete = { viewModel.borrarTransaccion(t); mostrarDetalle = false },
            onEdit = { mostrarDetalle = false; onNavegarEditar(t.id) }
        )
    }

    if (mostrarDialogoSaldo) {
        DialogoEditarSaldo(saldoActual = saldoBase, onDismiss = { mostrarDialogoSaldo = false }) { nuevo ->
            viewModel.actualizarSaldoManual(nuevo)
            mostrarDialogoSaldo = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                CenterAlignedTopAppBar(
                    title = { Text("Mi Dinero", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.onBackground),
                    navigationIcon = { Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 8.dp)) }
                )
                TabRow(selectedTabIndex = tabIndex, containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.primary, indicator = { tabPositions -> TabRowDefaults.Indicator(Modifier.tabIndicatorOffset(tabPositions[tabIndex]), color = MaterialTheme.colorScheme.primary) }) {
                    tabs.forEachIndexed { index, title -> Tab(text = { Text(title, color = if(tabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }, selected = tabIndex == index, onClick = { tabIndex = index }) }
                }
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (tabIndex == 0 && mostrarMenuAgregar) {
                    SmallFloatingActionButton(onClick = { mostrarMenuAgregar = false; activarMicrofono() }, containerColor = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.padding(bottom = 8.dp)) { Row(Modifier.padding(8.dp)) { Icon(Icons.Default.Mic, null); Spacer(Modifier.width(4.dp)); Text("Voz") } }
                    SmallFloatingActionButton(onClick = { mostrarMenuAgregar = false; onNavegarMetas() }, containerColor = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.padding(bottom = 8.dp)) { Row(Modifier.padding(8.dp)) { Icon(Icons.Default.Star, null); Spacer(Modifier.width(4.dp)); Text("Meta") } }
                    SmallFloatingActionButton(onClick = { mostrarMenuAgregar = false; onNavegarAgregar(true) }, containerColor = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.padding(bottom = 8.dp)) { Row(Modifier.padding(8.dp)) { Icon(Icons.Default.ArrowUpward, null); Spacer(Modifier.width(4.dp)); Text("Ingreso") } }
                    SmallFloatingActionButton(onClick = { mostrarMenuAgregar = false; onNavegarAgregar(false) }, containerColor = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.padding(bottom = 8.dp)) { Row(Modifier.padding(8.dp)) { Icon(Icons.Default.ArrowDownward, null); Spacer(Modifier.width(4.dp)); Text("Gasto") } }
                }
                Surface(shape = FloatingActionButtonDefaults.shape, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shadowElevation = 6.dp, modifier = Modifier.combinedClickable(onClick = { if (tabIndex == 0) mostrarMenuAgregar = !mostrarMenuAgregar else onNavegarAgregar(tabIndex == 2) }, onLongClick = { activarMicrofono() })) {
                    Box(modifier = Modifier.defaultMinSize(minWidth = 56.dp, minHeight = 56.dp), contentAlignment = Alignment.Center) { Icon(if (tabIndex == 0 && mostrarMenuAgregar) Icons.Default.Close else Icons.Default.Add, contentDescription = "Agregar") }
                }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
            if (tabIndex == 0) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(modifier = Modifier.fillMaxWidth().clickable { mostrarDialogoSaldo = true }, horizontalAlignment = Alignment.CenterHorizontally) {
                        // --- USO DE CURRENCYUTILS Y COLOR DINÁMICO ---
                        Text(
                            text = CurrencyUtils.formatCurrency(saldoDisponible),
                            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold, color = colorSaldo), // Usamos colorSaldo
                            fontSize = 56.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) { Text("Disponible (Toca para editar)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp).padding(start = 4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                if (listaMetas.isNotEmpty()) { item { val m = listaMetas.last(); CardProgresoAhorro(m.nombre, m.montoAhorrado, m.montoObjetivo) } }
                item { Text("Últimos Movimientos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 8.dp)) }
                val ultimos = listaTransacciones.take(4)
                items(ultimos) { t -> ItemTransaccionModerno(t, onEdit = { onNavegarEditar(t.id) }, onDelete = { viewModel.borrarTransaccion(t) }, onItemClick = { transaccionSeleccionada = t; mostrarDetalle = true }) }
            }
            if (tabIndex != 0) {
                val filtrada = if (tabIndex == 1) listaTransacciones.filter { !it.esIngreso } else listaTransacciones.filter { it.esIngreso }
                item {
                    if (filtrada.isNotEmpty()) { Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp), contentAlignment = Alignment.Center) { DonutChart(filtrada, size = 200.dp) } }
                    else { Box(modifier = Modifier.height(150.dp).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Sin datos aún", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                }
                val agrupado = filtrada.groupBy { DateUtils.formatearFechaAmigable(it.fecha) }
                agrupado.forEach { (f, ts) ->
                    item { Text(f, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), textAlign = TextAlign.Center) }
                    items(ts) { t -> ItemTransaccionModerno(t, onEdit = { onNavegarEditar(t.id) }, onDelete = { viewModel.borrarTransaccion(t) }, onItemClick = { transaccionSeleccionada = t; mostrarDetalle = true }) }
                }
            }
        }
    }
}

// --- COMPONENTES (Actualizados con formato) ---
@Composable
fun DialogoEscuchandoAnimado(onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.2f, animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse))
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(80.dp).scale(scale).clip(CircleShape).background(Color(0xFF00E676).copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color(0xFF00E676)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Mic, null, tint = Color.Black, modifier = Modifier.size(32.dp)) }
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text("Escuchando...", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun DialogoEditarSaldo(saldoActual: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var texto by remember { mutableStateOf(saldoActual.toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Ajustar Saldo Base") }, text = { OutlinedTextField(value = texto, onValueChange = { texto = it }, label = { Text("Saldo Inicial ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }, confirmButton = { Button(onClick = { val n = texto.toDoubleOrNull(); if (n != null) onConfirm(n) }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}

@Composable
fun CardProgresoAhorro(nombre: String, ahorrado: Double, meta: Double) {
    val progreso = (ahorrado / meta).toFloat().coerceIn(0f, 1f)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Meta: $nombre", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                // FORMATO MONEDA
                Text("${CurrencyUtils.formatCurrency(ahorrado)} / ${CurrencyUtils.formatCurrency(meta)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemTransaccionModerno(transaccion: TransaccionEntity, onEdit: () -> Unit, onDelete: () -> Unit, onItemClick: () -> Unit = {}) {
    var mostrarMenu by remember { mutableStateOf(false) }
    val icono = CategoriaUtils.getIcono(transaccion.categoria)
    val colorIcono = CategoriaUtils.getColor(transaccion.categoria)
    val signo = if (transaccion.esIngreso) "+" else "-"
    val colorMonto = if (transaccion.esIngreso) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Box {
        Row(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onItemClick() }, onLongClick = { mostrarMenu = true }).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) { Icon(imageVector = icono, contentDescription = null, tint = colorIcono, modifier = Modifier.size(24.dp)) }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) { Text(text = transaccion.descripcion.ifEmpty { transaccion.categoria }, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium); Text(text = transaccion.categoria, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            // FORMATO MONEDA
            Text(text = "$signo${CurrencyUtils.formatCurrency(transaccion.monto)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colorMonto)
        }
        DropdownMenu(expanded = mostrarMenu, onDismissRequest = { mostrarMenu = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            DropdownMenuItem(text = { Text("Editar", color = MaterialTheme.colorScheme.onSurface) }, onClick = { mostrarMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurface) })
            DropdownMenuItem(text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) }, onClick = { mostrarMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
        }
    }
}

@Composable
fun DetalleTransaccionDialog(transaccion: TransaccionEntity, onDismiss: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, icon = { Icon(CategoriaUtils.getIcono(transaccion.categoria), null, tint = CategoriaUtils.getColor(transaccion.categoria), modifier = Modifier.size(48.dp)) }, title = { Text(transaccion.categoria, color = MaterialTheme.colorScheme.onSurface) }, text = { Column {
        // FORMATO MONEDA
        Text(CurrencyUtils.formatCurrency(transaccion.monto), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface); Spacer(Modifier.height(8.dp)); Text(transaccion.descripcion, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(4.dp)); Text(DateUtils.formatearFechaAmigable(transaccion.fecha), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }, confirmButton = { TextButton(onClick = onEdit) { Text("Editar", color = MaterialTheme.colorScheme.primary) } }, dismissButton = { Row { TextButton(onClick = onDelete) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }; TextButton(onClick = onDismiss) { Text("Cerrar", color = MaterialTheme.colorScheme.onSurface) } } })
}