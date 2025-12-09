package com.example.gastocheck.ui.theme.screens.suscripciones

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import java.util.Calendar
import java.util.Date

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
    val filtroActual by viewModel.filtroActual.collectAsState()
    val cuentas by viewModel.cuentas.collectAsState()

    var mostrarCrear by remember { mutableStateOf(false) }
    var suscripcionEditar by remember { mutableStateOf<SuscripcionEntity?>(null) }
    var suscripcionDetalle by remember { mutableStateOf<SuscripcionEntity?>(null) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Suscripciones", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { suscripcionEditar = null; mostrarCrear = true },
                containerColor = GreenNeon,
                contentColor = Color.Black
            ) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. TOTAL MENSUAL
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Total mensual en suscripciones", color = Color.Gray, fontSize = 14.sp)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(CurrencyUtils.formatCurrency(totalMensual), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        if (alertas.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("${alertas.size} suscripciones vencen pronto", color = Color(0xFFFFD700), fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // 2. FILTROS (Actualizado con LazyRow y nuevos filtros)
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { FiltroChip("Ver todas", filtroActual == FiltroSuscripcion.TODAS) { viewModel.cambiarFiltro(FiltroSuscripcion.TODAS) } }
                    item { FiltroChip("Próximas", filtroActual == FiltroSuscripcion.PROXIMAS) { viewModel.cambiarFiltro(FiltroSuscripcion.PROXIMAS) } }
                    item { FiltroChip("Atrasadas", filtroActual == FiltroSuscripcion.ATRASADAS) { viewModel.cambiarFiltro(FiltroSuscripcion.ATRASADAS) } }
                    item { FiltroChip("Pagadas", filtroActual == FiltroSuscripcion.PAGADAS) { viewModel.cambiarFiltro(FiltroSuscripcion.PAGADAS) } }
                    item { FiltroChip("Canceladas", filtroActual == FiltroSuscripcion.CANCELADAS) { viewModel.cambiarFiltro(FiltroSuscripcion.CANCELADAS) } }
                }
            }

            // 3. ALERTAS PRÓXIMAS (CARRUSEL) - Solo si no estamos viendo canceladas/pagadas
            if (alertas.isNotEmpty() && filtroActual != FiltroSuscripcion.ATRASADAS && filtroActual != FiltroSuscripcion.CANCELADAS && filtroActual != FiltroSuscripcion.PAGADAS) {
                item {
                    Text("Alertas próximas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(alertas) { sub ->
                            CardAlertaProxima(sub, viewModel) { suscripcionDetalle = sub }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            // 4. LISTA GENERAL
            item {
                Text(
                    when(filtroActual){
                        FiltroSuscripcion.TODAS -> "Todos los servicios"
                        FiltroSuscripcion.PROXIMAS -> "Próximos vencimientos"
                        FiltroSuscripcion.ATRASADAS -> "Pagos atrasados"
                        FiltroSuscripcion.PAGADAS -> "Suscripciones pagadas"
                        FiltroSuscripcion.CANCELADAS -> "Servicios cancelados"
                    },
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (suscripciones.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No hay resultados", color = Color.Gray)
                    }
                }
            } else {
                items(suscripciones) { sub ->
                    ItemSuscripcion(sub, viewModel) { suscripcionDetalle = sub }
                }
            }
        }
    }

    // --- MODALES ---

    if (mostrarCrear) {
        Dialog(
            onDismissRequest = { mostrarCrear = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            PantallaCrearEditarSuscripcion(
                existente = suscripcionEditar,
                cuentas = cuentas,
                onDismiss = { mostrarCrear = false },
                onConfirm = { nombre, monto, fecha, frec, icono, cta, nota, recordatorio, hora ->
                    viewModel.guardarSuscripcion(suscripcionEditar?.id ?: 0, nombre, monto, fecha, frec, icono, cta, nota, recordatorio, hora)
                    mostrarCrear = false
                }
            )
        }
    }

    if (suscripcionDetalle != null) {
        DetalleSuscripcionDialog(
            sub = suscripcionDetalle!!,
            viewModel = viewModel,
            onDismiss = { suscripcionDetalle = null },
            onEdit = {
                suscripcionEditar = suscripcionDetalle
                suscripcionDetalle = null
                mostrarCrear = true
            },
            onDelete = {
                viewModel.borrarSuscripcion(suscripcionDetalle!!)
                suscripcionDetalle = null
            }
        )
    }
}

// --- COMPONENTES VISUALES ---

@Composable
fun FiltroChip(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (seleccionado) GreenNeon else CardBg,
        shape = RoundedCornerShape(50),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = texto,
            color = if (seleccionado) Color.Black else Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CardAlertaProxima(sub: SuscripcionEntity, viewModel: SuscripcionesViewModel, onClick: () -> Unit) {
    val dias = viewModel.diasRestantes(sub.fechaPago)

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(150.dp).clickable(onClick = onClick).border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(getIconoSuscripcion(sub.icono), null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(sub.nombre, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
            Text(
                text = if (dias == 0L) "Vence hoy" else "Vence en $dias días",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(Modifier.height(8.dp))
            Text(CurrencyUtils.formatCurrency(sub.monto), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        }
    }
}

@Composable
fun ItemSuscripcion(sub: SuscripcionEntity, viewModel: SuscripcionesViewModel, onClick: () -> Unit) {
    val estado = viewModel.calcularEstado(sub)

    val (colorEstado, textoEstado) = when(estado) {
        EstadoSuscripcion.PAGADO -> GreenNeon to "Pagado"
        EstadoSuscripcion.PENDIENTE -> Color(0xFFFFD700) to "Pendiente"
        EstadoSuscripcion.ATRASADO -> MaterialTheme.colorScheme.error to "Atrasado"
        EstadoSuscripcion.CANCELADO -> GrayInactive to "Cancelado"
    }

    val opacity = if (estado == EstadoSuscripcion.CANCELADO) 0.5f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .background(CardBg.copy(alpha = opacity), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(getIconoSuscripcion(sub.icono), null, tint = Color.White.copy(alpha = opacity), modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(sub.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White.copy(alpha = opacity))
            val fechaStr = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(Date(sub.fechaPago))
            Text("Próximo pago: $fechaStr", fontSize = 12.sp, color = Color.Gray)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(CurrencyUtils.formatCurrency(sub.monto), fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = opacity))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colorEstado))
                Spacer(Modifier.width(4.dp))
                Text(textoEstado, fontSize = 10.sp, color = colorEstado)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCrearEditarSuscripcion(
    existente: SuscripcionEntity?,
    cuentas: List<CuentaEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Long, String, String, Int, String, String, String) -> Unit
) {
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

    // Date Picker
    val datePickerDialog = DatePickerDialog(context, { _, y, m, d ->
        calendar.set(y, m, d)
        fecha = calendar.timeInMillis
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    // Time Picker
    val horaSplit = horaRecordatorio.split(":")
    val timePickerDialog = TimePickerDialog(context, { _, h, m ->
        horaRecordatorio = String.format("%02d:%02d", h, m)
    }, horaSplit[0].toInt(), horaSplit[1].toInt(), true)

    val iconos = listOf(
        "Netflix", "Spotify", "Youtube", "Apple", "Disney", "HBO", "Amazon",
        "Figma", "Notion", "Agua", "Luz", "Gas", "Internet", "Celular",
        "Colegio", "Gimnasio", "Seguro", "Otro"
    )

    var menuCuentas by remember { mutableStateOf(false) }
    var menuFrecuencia by remember { mutableStateOf(false) }
    val frecuencias = listOf("Semanal", "Mensual", "Anual")

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (existente == null) "Agregar Suscripción" else "Editar", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("Elige un servicio", modifier = Modifier.align(Alignment.Start), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))

            // Grid de Iconos MANUAL
            iconos.chunked(4).forEach { fila ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    fila.forEach { iconName ->
                        val selected = iconName == icono
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { icono = iconName }.width(70.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) GreenNeon else CardBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(getIconoSuscripcion(iconName), null, tint = if(selected) Color.Black else Color.Gray)
                            }
                            Text(iconName, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp), color = if(selected) GreenNeon else Color.Gray, maxLines = 1)
                        }
                    }
                    repeat(4 - fila.size) { Spacer(Modifier.width(70.dp)) }
                }
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(24.dp))

            // CAMPOS DE TEXTO
            Text("Nombre del servicio", color = Color.White, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
            CampoTextoSuscripcion(nombre, { nombre = it }, "Ej: Netflix, Spotify...")

            Spacer(Modifier.height(16.dp))

            Text("Monto mensual", color = Color.White, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
            CampoTextoSuscripcion(monto, { monto = it }, "$ 0.00", KeyboardType.Number)

            Spacer(Modifier.height(16.dp))

            // CUENTA
            Text("Cuenta desde la que se paga", color = Color.White, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
            Box {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(CardBg).clickable { menuCuentas = true }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(cuentas.find { it.id == cuentaId }?.nombre ?: "Seleccionar cuenta", color = Color.White)
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                }
                DropdownMenu(expanded = menuCuentas, onDismissRequest = { menuCuentas = false }) {
                    cuentas.forEach { c -> DropdownMenuItem(text = { Text(c.nombre) }, onClick = { cuentaId = c.id; menuCuentas = false }) }
                }
            }

            Spacer(Modifier.height(16.dp))

            // FRECUENCIA
            Text("Frecuencia", color = Color.White, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
            Box {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(CardBg).clickable { menuFrecuencia = true }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(frecuencia, color = Color.White)
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                }
                DropdownMenu(expanded = menuFrecuencia, onDismissRequest = { menuFrecuencia = false }) {
                    frecuencias.forEach { f -> DropdownMenuItem(text = { Text(f) }, onClick = { frecuencia = f; menuFrecuencia = false }) }
                }
            }

            Spacer(Modifier.height(16.dp))

            // FECHA Y HORA (NUEVO CAMPO)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // FECHA
                Column(modifier = Modifier.weight(1f)) {
                    Text("Fecha límite", color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(CardBg).clickable { datePickerDialog.show() }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(DateUtils.formatearFechaAmigable(Date(fecha)), color = Color.White, fontSize = 12.sp)
                        Icon(Icons.Default.CalendarToday, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
                // HORA (NUEVO)
                Column(modifier = Modifier.weight(1f)) {
                    Text("Horario aviso", color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(CardBg).clickable { timePickerDialog.show() }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(horaRecordatorio, color = Color.White, fontSize = 12.sp)
                        Icon(Icons.Default.Schedule, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // RECORDATORIOS (CHIPS)
            Text("Recordatorios", color = Color.White, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("1 día antes", "3 días antes", "7 días antes").forEach { r ->
                    FilterChip(
                        selected = recordatorio == r,
                        onClick = { recordatorio = r },
                        label = { Text(r) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenNeon,
                            selectedLabelColor = Color.Black,
                            containerColor = CardBg,
                            labelColor = Color.White
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // NOTAS
            Text("Notas", color = Color.White, modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp))
            CampoTextoSuscripcion(nota, { nota = it }, "Añade notas adicionales aquí...", singleLine = false, modifier = Modifier.height(100.dp))

            Spacer(Modifier.height(32.dp))

            // Botón en la parte inferior del scroll
            Button(
                onClick = {
                    val m = monto.toDoubleOrNull()
                    if (nombre.isNotEmpty() && m != null) onConfirm(nombre, m, fecha, frecuencia, icono, cuentaId, nota, recordatorio, horaRecordatorio)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenNeon, contentColor = Color.Black)
            ) { Text("Guardar", fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun DetalleSuscripcionDialog(
    sub: SuscripcionEntity,
    viewModel: SuscripcionesViewModel,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Estado para controlar la alerta de confirmación
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    // Calculamos el estado actual para mostrarlo seleccionado
    val estadoActual = viewModel.calcularEstado(sub)

    // Función helper para cambiar estado
    fun cambiar(nuevo: String) {
        viewModel.cambiarEstadoSuscripcion(sub, nuevo)
    }

    // --- 1. ALERTA DE CONFIRMACIÓN DE ELIMINAR ---
    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            containerColor = CardBg,
            title = { Text("¿Eliminar Suscripción?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Esta acción eliminará la suscripción permanentemente.", color = Color.Gray) },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete() // Aquí sí borramos de verdad
                        mostrarConfirmacion = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    // --- 2. DIÁLOGO DE DETALLES PRINCIPAL ---
    // Solo mostramos este si NO estamos mostrando la confirmación (opcional, pueden encimarse)
    // O simplemente dejamos que el AlertDialog se dibuje encima (lo estándar).

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        icon = { Icon(getIconoSuscripcion(sub.icono), null, modifier = Modifier.size(48.dp), tint = GreenNeon) },
        title = { Text(sub.nombre, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(CurrencyUtils.formatCurrency(sub.monto), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)

                Spacer(Modifier.height(24.dp))

                // SELECTOR DE ESTADOS (3 BOTONES)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 1. PENDIENTE
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SelectableStatusButton(
                            selected = estadoActual == EstadoSuscripcion.PENDIENTE,
                            color = Color(0xFFFFD700),
                            icon = Icons.Default.Schedule,
                            onClick = { cambiar("PENDIENTE") }
                        )
                        Text("Pendiente", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top=4.dp))
                    }

                    // 2. PAGADO
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SelectableStatusButton(
                            selected = estadoActual == EstadoSuscripcion.PAGADO,
                            color = GreenNeon,
                            icon = Icons.Default.Check,
                            onClick = { cambiar("PAGADO") }
                        )
                        Text("Pagado", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top=4.dp))
                    }

                    // 3. CANCELADO
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SelectableStatusButton(
                            selected = estadoActual == EstadoSuscripcion.CANCELADO,
                            color = GrayInactive,
                            icon = Icons.Default.Cancel,
                            onClick = { cambiar("CANCELADO") }
                        )
                        Text("Cancelado", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top=4.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))
                Divider(color = Color.Gray.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))

                // DETALLES INFERIORES
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Fecha Pago", color = Color.Gray, fontSize = 12.sp)
                        Text(DateUtils.formatearFechaAmigable(Date(sub.fechaPago)), color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Hora Aviso", color = Color.Gray, fontSize = 12.sp)
                        Text(sub.horaRecordatorio, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(containerColor = GreenNeon, contentColor = Color.Black)
            ) {
                Text("Editar")
            }
        },
        dismissButton = {
            Row {
                // CAMBIO AQUI: En lugar de onDelete directo, activamos la confirmación
                TextButton(onClick = { mostrarConfirmacion = true }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = Color.Gray)
                }
            }
        }
    )
}

@Composable
fun SelectableStatusButton(selected: Boolean, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(if (selected) color else Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .border(1.dp, if(selected) color else Color.Transparent, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = if (selected) Color.Black else Color.Gray)
    }
}
@Composable
fun CampoTextoSuscripcion(value: String, onValueChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, modifier: Modifier = Modifier) {
    BasicTextField(value = value, onValueChange = onValueChange, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp), keyboardOptions = KeyboardOptions(keyboardType = keyboardType), singleLine = singleLine, cursorBrush = SolidColor(GreenNeon), modifier = modifier.fillMaxWidth(), decorationBox = { innerTextField -> Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(CardBg).padding(16.dp)) { if (value.isEmpty()) Text(placeholder, color = Color.Gray); innerTextField() } })
}

fun getIconoSuscripcion(nombre: String): ImageVector {
    return when(nombre) {
        "Netflix" -> Icons.Default.Tv; "Spotify" -> Icons.Default.MusicNote; "Youtube" -> Icons.Default.PlayArrow; "Apple" -> Icons.Default.PhoneIphone; "Disney" -> Icons.Default.Star; "HBO" -> Icons.Default.Movie; "Amazon" -> Icons.Default.ShoppingCart; "Figma", "Notion", "Colegio" -> Icons.Default.School; "Agua" -> Icons.Default.WaterDrop; "Luz" -> Icons.Default.Lightbulb; "Gas" -> Icons.Default.LocalFireDepartment; "Internet", "Celular" -> Icons.Default.Wifi; "Gimnasio" -> Icons.Default.FitnessCenter; "Seguro" -> Icons.Default.Security; else -> Icons.Default.CreditCard
    }
}