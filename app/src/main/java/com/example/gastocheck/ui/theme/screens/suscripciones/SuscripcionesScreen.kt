package com.example.gastocheck.ui.theme.screens.suscripciones

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.SuscripcionEntity
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import com.example.gastocheck.ui.theme.util.DateUtils
import com.example.gastocheck.ui.theme.util.IconoUtils
import com.example.gastocheck.ui.theme.util.ServiceColorUtils
import java.util.Calendar
import java.util.Date

// Colores del tema (para fallback)
val GreenNeon = Color(0xFF00E676)
val DarkBg = Color(0xFF121212)
val CardBg = Color(0xFF1E1E1E)
val GrayInactive = Color(0xFF757575)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuscripcionesScreen(viewModel: SuscripcionesViewModel = hiltViewModel()) {
    val suscripciones by viewModel.suscripcionesFiltradas.collectAsState()
    val alertas by viewModel.alertasProximas.collectAsState()
    val totalMensual by viewModel.totalMensual.collectAsState()
    val advertenciaState by viewModel.estadoAdvertencia.collectAsState()
    val filtroActual by viewModel.filtroActual.collectAsState()
    val cuentas by viewModel.cuentas.collectAsState()

    var mostrarCrear by remember { mutableStateOf(false) }
    var suscripcionEditar by remember { mutableStateOf<SuscripcionEntity?>(null) }
    var suscripcionDetalle by remember { mutableStateOf<SuscripcionEntity?>(null) }

    val launcherPermiso = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcherPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pagos Recurrentes", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { suscripcionEditar = null; mostrarCrear = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. TARJETA RESUMEN
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Total mensual", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(CurrencyUtils.formatCurrency(totalMensual), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (advertenciaState.visible) {
                            Spacer(Modifier.height(12.dp))
                            val colorAviso = if (advertenciaState.esError) MaterialTheme.colorScheme.error else Color(0xFFFFD700)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = colorAviso, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(advertenciaState.mensaje, color = colorAviso, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            // 2. FILTROS
            item {
                LazyRow(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val filtros = listOf(FiltroSuscripcion.TODAS to "Ver todas", FiltroSuscripcion.PROXIMAS to "Próximas", FiltroSuscripcion.ATRASADAS to "Atrasadas", FiltroSuscripcion.PAGADAS to "Pagadas", FiltroSuscripcion.CANCELADAS to "Canceladas")
                    items(filtros) { (filtro, texto) -> FiltroChip(texto, filtroActual == filtro) { viewModel.cambiarFiltro(filtro) } }
                }
            }

            // 3. ALERTAS
            if (alertas.isNotEmpty() && filtroActual != FiltroSuscripcion.ATRASADAS && filtroActual != FiltroSuscripcion.PAGADAS && filtroActual != FiltroSuscripcion.CANCELADAS) {
                item {
                    Text("Alertas próximas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(alertas) { sub -> CardAlertaProxima(sub, viewModel) { suscripcionDetalle = sub } }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            // 4. LISTA
            item {
                val titulo = when(filtroActual){
                    FiltroSuscripcion.TODAS -> "Todos los servicios"
                    FiltroSuscripcion.PROXIMAS -> "Próximos vencimientos"
                    FiltroSuscripcion.ATRASADAS -> "Pagos atrasados"
                    FiltroSuscripcion.PAGADAS -> "Pagadas"
                    FiltroSuscripcion.CANCELADAS -> "Canceladas"
                }
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 12.dp))
            }

            if (suscripciones.isEmpty()) {
                item { Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No hay resultados", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            } else {
                items(suscripciones) { sub -> ItemSuscripcion(sub, viewModel) { suscripcionDetalle = sub } }
            }
        }
    }

    if (mostrarCrear) {
        Dialog(onDismissRequest = { mostrarCrear = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            PantallaCrearEditarSuscripcion(existente = suscripcionEditar, cuentas = cuentas, onDismiss = { mostrarCrear = false }, onConfirm = { nombre, monto, fecha, frec, icono, cta, nota, recordatorio, hora -> viewModel.guardarSuscripcion(suscripcionEditar?.id ?: 0, nombre, monto, fecha, frec, icono, cta, nota, recordatorio, hora); mostrarCrear = false })
        }
    }

    if (suscripcionDetalle != null) {
        val nombreCuenta = cuentas.find { it.id == suscripcionDetalle!!.cuentaId }?.nombre ?: "Desconocida"
        DetalleSuscripcionDialog(sub = suscripcionDetalle!!, nombreCuenta = nombreCuenta, viewModel = viewModel, onDismiss = { suscripcionDetalle = null }, onEdit = { suscripcionEditar = suscripcionDetalle; suscripcionDetalle = null; mostrarCrear = true }, onDelete = { viewModel.borrarSuscripcion(suscripcionDetalle!!); suscripcionDetalle = null })
    }
}

// ... COMPONENTES UI (FiltroChip, CardAlertaProxima, ItemSuscripcion, DetalleDialog, HistorialDialog, PantallaCrearEditar) ...

@Composable
fun FiltroChip(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (seleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(text = texto, color = if (seleccionado) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CardAlertaProxima(sub: SuscripcionEntity, viewModel: SuscripcionesViewModel, onClick: () -> Unit) {
    val dias = viewModel.diasRestantes(sub.fechaPago)
    val brandColor = ServiceColorUtils.getColorByName(sub.icono)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp), modifier = Modifier.width(150.dp).clickable(onClick = onClick).border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(16.dp))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(brandColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Icon(IconoUtils.getIconoByName(sub.icono), null, tint = brandColor, modifier = Modifier.size(20.dp)) }
            Spacer(Modifier.height(12.dp))
            Text(sub.nombre, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(text = if (dias == 0L) "Vence hoy" else "Vence en $dias días", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(CurrencyUtils.formatCurrency(sub.monto), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ItemSuscripcion(sub: SuscripcionEntity, viewModel: SuscripcionesViewModel, onClick: () -> Unit) {
    val estado = viewModel.calcularEstado(sub)
    val (colorEstado, textoEstado) = when(estado) {
        EstadoSuscripcion.PAGADO -> MaterialTheme.colorScheme.primary to "Pagado"
        EstadoSuscripcion.PENDIENTE -> Color(0xFFFFD700) to "Pendiente"
        EstadoSuscripcion.ATRASADO -> MaterialTheme.colorScheme.error to "Atrasado"
        EstadoSuscripcion.CANCELADO -> MaterialTheme.colorScheme.outline to "Cancelado"
    }
    val opacity = if (estado == EstadoSuscripcion.CANCELADO) 0.5f else 1f
    val brandColor = ServiceColorUtils.getColorByName(sub.icono)
    val fechaMostrar = if (estado == EstadoSuscripcion.PAGADO) viewModel.calcularProximoPagoProyectado(sub) else sub.fechaPago

    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f * opacity), RoundedCornerShape(16.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(brandColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) { Icon(IconoUtils.getIconoByName(sub.icono), null, tint = brandColor.copy(alpha = opacity), modifier = Modifier.size(24.dp)) }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(sub.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = opacity))
            val fechaStr = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(Date(fechaMostrar))
            Text("Próximo pago: $fechaStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(CurrencyUtils.formatCurrency(sub.monto), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = opacity))
            Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colorEstado)); Spacer(Modifier.width(4.dp)); Text(textoEstado, fontSize = 10.sp, color = colorEstado) }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
fun DetalleSuscripcionDialog(sub: SuscripcionEntity, nombreCuenta: String, viewModel: SuscripcionesViewModel, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    var mostrarHistorial by remember { mutableStateOf(false) }
    val estadoActual = viewModel.calcularEstado(sub)
    val brandColor = ServiceColorUtils.getColorByName(sub.icono)

    fun cambiar(nuevo: String) {
        when (nuevo) {
            "PAGADO" -> if (estadoActual != EstadoSuscripcion.PAGADO) viewModel.marcarComoPagado(sub)
            "PENDIENTE" -> if (estadoActual == EstadoSuscripcion.PAGADO) viewModel.deshacerPago(sub) else viewModel.cambiarEstadoManual(sub, "PENDIENTE")
            "CANCELADO" -> viewModel.cambiarEstadoManual(sub, "CANCELADO")
        }
        onDismiss()
    }

    if (mostrarHistorial) { HistorialPagosDialog(sub, viewModel) { mostrarHistorial = false } }
    else if (mostrarConfirmacion) {
        AlertDialog(onDismissRequest = { mostrarConfirmacion = false }, title = { Text("¿Eliminar Suscripción?") }, text = { Text("Se borrará permanentemente.") }, confirmButton = { Button(onClick = { onDelete(); mostrarConfirmacion = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Eliminar") } }, dismissButton = { TextButton(onClick = { mostrarConfirmacion = false }) { Text("Cancelar") } })
    } else {
        AlertDialog(
            onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface,
            icon = { Icon(IconoUtils.getIconoByName(sub.icono), null, modifier = Modifier.size(48.dp), tint = brandColor) },
            title = { Text(sub.nombre, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(CurrencyUtils.formatCurrency(sub.monto), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { mostrarHistorial = true }, modifier = Modifier.height(36.dp)) { Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp)); Text("Ver Historial", fontSize = 12.sp) }
                    Spacer(Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { SelectableStatusButton(selected = estadoActual == EstadoSuscripcion.PENDIENTE || estadoActual == EstadoSuscripcion.ATRASADO, color = Color(0xFFFFD700), icon = Icons.Default.Schedule, onClick = { cambiar("PENDIENTE") }); Text("Pendiente", fontSize = 10.sp) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { SelectableStatusButton(selected = estadoActual == EstadoSuscripcion.PAGADO, color = MaterialTheme.colorScheme.primary, icon = Icons.Default.Check, onClick = { cambiar("PAGADO") }); Text("Pagado", fontSize = 10.sp) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { SelectableStatusButton(selected = estadoActual == EstadoSuscripcion.CANCELADO, color = MaterialTheme.colorScheme.outline, icon = Icons.Default.Cancel, onClick = { cambiar("CANCELADO") }); Text("Cancelado", fontSize = 10.sp) }
                    }
                    Spacer(Modifier.height(24.dp)); Divider(); Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("Fecha Pago", fontSize = 12.sp); Text(DateUtils.formatearFechaAmigable(Date(sub.fechaPago))) } ; Column(horizontalAlignment = Alignment.End) { Text("Cuenta", fontSize = 12.sp); Text(nombreCuenta) } }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("Hora Aviso", fontSize = 12.sp); Text(sub.horaRecordatorio) } }
                    if (sub.nota.isNotEmpty()) { Spacer(Modifier.height(12.dp)); Column { Text("Nota", fontSize = 12.sp); Text(sub.nota, fontSize = 14.sp) } }
                }
            },
            confirmButton = { Button(onClick = onEdit) { Text("Editar") } },
            dismissButton = { Row { TextButton(onClick = { mostrarConfirmacion = true }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }; TextButton(onClick = onDismiss) { Text("Cerrar") } } }
        )
    }
}

@Composable
fun HistorialPagosDialog(sub: SuscripcionEntity, viewModel: SuscripcionesViewModel, onBack: () -> Unit) {
    val historial by viewModel.obtenerHistorial(sub.id).collectAsState(initial = emptyList())
    val totalGastado = remember(historial) { historial.sumOf { it.monto } }
    val brandColor = ServiceColorUtils.getColorByName(sub.icono)

    Dialog(onDismissRequest = onBack) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp, max = 600.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }; Spacer(Modifier.width(8.dp)); Text("Historial de Pagos", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(24.dp))
                Card(colors = CardDefaults.cardColors(containerColor = brandColor.copy(alpha = 0.15f)), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Gastado", color = brandColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(CurrencyUtils.formatCurrency(totalGastado), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("${historial.size} pagos registrados", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(24.dp)); Text("Detalle", fontSize = 14.sp); Spacer(Modifier.height(8.dp))
                if (historial.isEmpty()) { Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Aún no hay pagos registrados") } }
                else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(historial) { pago ->
                            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(12.dp)); Text(DateUtils.formatearFechaAmigable(Date(pago.fechaPago))) }
                                Text(CurrencyUtils.formatCurrency(pago.monto), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SelectableStatusButton(selected: Boolean, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(if (selected) color else MaterialTheme.colorScheme.surfaceContainerHighest).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCrearEditarSuscripcion(existente: SuscripcionEntity?, cuentas: List<CuentaEntity>, onDismiss: () -> Unit, onConfirm: (String, Double, Long, String, String, Int, String, String, String) -> Unit) {
    var nombre by remember { mutableStateOf(existente?.nombre ?: "") }
    var monto by remember { mutableStateOf(existente?.monto?.toString() ?: "") }
    var fecha by remember { mutableStateOf(existente?.fechaPago ?: System.currentTimeMillis()) }
    var frecuencia by remember { mutableStateOf(existente?.frecuencia ?: "Mensual") }
    var icono by remember { mutableStateOf(existente?.icono ?: "Netflix") }
    var cuentaId by remember { mutableStateOf(existente?.cuentaId ?: if (cuentas.isNotEmpty()) cuentas.first().id else -1) }
    var nota by remember { mutableStateOf(existente?.nota ?: "") }
    var recordatorio by remember { mutableStateOf(existente?.recordatorio ?: "1 día antes") }
    var horaRecordatorio by remember { mutableStateOf(existente?.horaRecordatorio ?: "09:00") }

    val context = LocalContext.current
    val calendar = Calendar.getInstance().apply { timeInMillis = fecha }
    val datePickerDialog = DatePickerDialog(context, { _, y, m, d -> calendar.set(y, m, d); fecha = calendar.timeInMillis }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    val horaSplit = horaRecordatorio.split(":")
    val timePickerDialog = TimePickerDialog(context, { _, h, m -> horaRecordatorio = String.format("%02d:%02d", h, m) }, horaSplit[0].toInt(), horaSplit[1].toInt(), false)

    val iconos = listOf("Netflix", "Spotify", "Youtube", "Apple", "Disney", "HBO", "Amazon", "Figma", "Notion", "Agua", "Luz", "Gas", "Internet", "Celular", "Colegio", "Gimnasio", "Seguro", "Otro")
    var menuCuentas by remember { mutableStateOf(false) }
    var menuFrecuencia by remember { mutableStateOf(false) }
    val frecuencias = listOf("Semanal", "Quincenal", "Mensual", "Anual")

    // --- SOLUCIÓN: imePadding() para que el teclado no tape el contenido ---
    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { CenterAlignedTopAppBar(title = { Text(if (existente == null) "Agregar Suscripción" else "Editar", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Elige un servicio", modifier = Modifier.align(Alignment.Start), fontWeight = FontWeight.Bold, fontSize = 20.sp); Spacer(Modifier.height(16.dp))
            iconos.chunked(4).forEach { fila ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    fila.forEach { iconName ->
                        val selected = iconName == icono
                        val brandColor = ServiceColorUtils.getColorByName(iconName)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { icono = iconName }.width(70.dp)) {
                            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(if (selected) brandColor else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(IconoUtils.getIconoByName(iconName), null, tint = if(selected) Color.Black else brandColor) }
                            Text(iconName, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                        }
                    }
                    repeat(4 - fila.size) { Spacer(Modifier.width(70.dp)) }
                }
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Nombre del servicio", modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)); CampoTextoSuscripcion(nombre, { nombre = it }, "Ej: Netflix, Spotify...")
            Spacer(Modifier.height(16.dp))
            Text("Monto mensual", modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)); CampoTextoSuscripcion(monto, { monto = it }, "$ 0.00", KeyboardType.Number)
            Spacer(Modifier.height(16.dp))
            Text("Cuenta desde la que se paga", modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
            Box { Row(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { menuCuentas = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(cuentas.find { it.id == cuentaId }?.nombre ?: "Seleccionar cuenta", color = MaterialTheme.colorScheme.onSurface); Icon(Icons.Default.ArrowDropDown, null) }; DropdownMenu(expanded = menuCuentas, onDismissRequest = { menuCuentas = false }) { cuentas.forEach { c -> DropdownMenuItem(text = { Text(c.nombre) }, onClick = { cuentaId = c.id; menuCuentas = false }) } } }
            Spacer(Modifier.height(16.dp))
            Text("Frecuencia", modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
            Box { Row(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { menuFrecuencia = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(frecuencia, color = MaterialTheme.colorScheme.onSurface); Icon(Icons.Default.ArrowDropDown, null) }; DropdownMenu(expanded = menuFrecuencia, onDismissRequest = { menuFrecuencia = false }) { frecuencias.forEach { f -> DropdownMenuItem(text = { Text(f) }, onClick = { frecuencia = f; menuFrecuencia = false }) } } }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) { Text("Fecha límite", modifier = Modifier.padding(bottom = 8.dp)); Row(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { datePickerDialog.show() }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(DateUtils.formatearFechaAmigable(Date(fecha)), fontSize = 12.sp); Icon(Icons.Default.CalendarToday, null) } }
                Column(modifier = Modifier.weight(1f)) { Text("Horario aviso", modifier = Modifier.padding(bottom = 8.dp)); Row(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { timePickerDialog.show() }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(horaRecordatorio, fontSize = 12.sp); Icon(Icons.Default.Schedule, null) } }
            }
            Spacer(Modifier.height(16.dp))
            Text("Recordatorios", modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("1 día antes", "3 días antes", "7 días antes").forEach { r -> FiltroChip(texto = r, seleccionado = recordatorio == r) { recordatorio = r } } }
            Spacer(Modifier.height(16.dp))

            // --- CAMPO NOTAS MEJORADO (Más alto y multilínea) ---
            Text("Notas", modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
            CampoTextoSuscripcion(
                value = nota,
                onValueChange = { nota = it },
                placeholder = "Añade notas adicionales aquí...",
                singleLine = false,
                modifier = Modifier.height(100.dp) // Altura suficiente
            )

            Spacer(Modifier.height(32.dp))
            Button(onClick = { val m = monto.toDoubleOrNull(); if (nombre.isNotEmpty() && m != null) onConfirm(nombre, m, fecha, frecuencia, icono, cuentaId, nota, recordatorio, horaRecordatorio) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp)) { Text("Guardar", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun CampoTextoSuscripcion(value: String, onValueChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, modifier: Modifier = Modifier) {
    BasicTextField(value = value, onValueChange = onValueChange, textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp), keyboardOptions = KeyboardOptions(keyboardType = keyboardType), singleLine = singleLine, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = modifier.fillMaxWidth(), decorationBox = { innerTextField -> Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp)) { if (value.isEmpty()) Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant); innerTextField() } })
}