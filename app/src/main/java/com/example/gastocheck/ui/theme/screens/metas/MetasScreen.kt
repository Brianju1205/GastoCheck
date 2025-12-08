package com.example.gastocheck.ui.theme.screens.metas

import android.app.DatePickerDialog
import android.widget.DatePicker
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.data.database.entity.AbonoEntity
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.MetaEntity
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import com.example.gastocheck.ui.theme.util.DateUtils
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

// Colores del Diseño
val NeonGreen = Color(0xFF00E676)
val DarkSurface = Color(0xFF1E1E1E)
val BackgroundColor = Color(0xFF121212)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasScreen(viewModel: MetasViewModel = hiltViewModel()) {
    val metas by viewModel.metas.collectAsState()
    val cuentas by viewModel.cuentas.collectAsState()

    var mostrarPantallaCrear by remember { mutableStateOf(false) }
    var metaParaEditar by remember { mutableStateOf<MetaEntity?>(null) }
    var metaSeleccionadaDetalle by remember { mutableStateOf<MetaEntity?>(null) }
    var mostrarDialogoAbonar by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mis Metas", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundColor,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    metaParaEditar = null
                    mostrarPantallaCrear = true
                },
                containerColor = NeonGreen,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Meta")
            }
        }
    ) { padding ->
        if (metas.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tienes metas activas", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(metas) { meta ->
                    MetaItemCard(
                        meta = meta,
                        onClick = { metaSeleccionadaDetalle = meta },
                        onAbonarClick = {
                            metaSeleccionadaDetalle = meta
                            mostrarDialogoAbonar = true
                        }
                    )
                }
            }
        }
    }

    // PANTALLA CREAR / EDITAR
    if (mostrarPantallaCrear) {
        Dialog(
            onDismissRequest = { mostrarPantallaCrear = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            PantallaCrearEditarMeta(
                metaExistente = metaParaEditar,
                cuentas = cuentas,
                onDismiss = { mostrarPantallaCrear = false },
                onConfirm = { nombre, objetivo, icono, fecha, cuentaId, nota ->
                    viewModel.guardarMeta(
                        id = metaParaEditar?.id ?: 0,
                        nombre = nombre,
                        objetivo = objetivo,
                        icono = icono,
                        fechaLimite = fecha,
                        cuentaId = cuentaId,
                        nota = nota
                    )
                    mostrarPantallaCrear = false
                }
            )
        }
    }

    // DIALOGO DE DETALLES E HISTORIAL
    if (metaSeleccionadaDetalle != null && !mostrarDialogoAbonar) {
        // Obtenemos el historial de abonos para la meta seleccionada
        val historialAbonos by viewModel.obtenerHistorialAbonos(metaSeleccionadaDetalle!!.id)
            .collectAsState(initial = emptyList())

        DetalleMetaOpcionesDialog(
            meta = metaSeleccionadaDetalle!!,
            historialAbonos = historialAbonos,
            onDismiss = { metaSeleccionadaDetalle = null },
            onEditar = {
                metaParaEditar = metaSeleccionadaDetalle
                metaSeleccionadaDetalle = null
                mostrarPantallaCrear = true
            },
            onEliminar = {
                viewModel.borrarMeta(metaSeleccionadaDetalle!!)
                metaSeleccionadaDetalle = null
            },
            onAbonar = { mostrarDialogoAbonar = true },
            onEditarAbono = { abono, nuevoMonto ->
                viewModel.editarAbono(metaSeleccionadaDetalle!!, abono, nuevoMonto)
            }
        )
    }

    // DIALOGO ABONAR
    if (mostrarDialogoAbonar && metaSeleccionadaDetalle != null) {
        DialogoAbonar(
            meta = metaSeleccionadaDetalle!!,
            onDismiss = {
                mostrarDialogoAbonar = false
                metaSeleccionadaDetalle = null
            },
            onConfirm = { monto ->
                viewModel.abonarAMeta(metaSeleccionadaDetalle!!, monto)
                mostrarDialogoAbonar = false
                // No cerramos el detalle, solo el dialogo de abonar, para ver el historial actualizado
                // Pero como metaSeleccionadaDetalle se usa para mostrar el detalle,
                // si queremos volver al detalle debemos asegurarnos de que no sea null.
                // La lógica actual lo pone a null al cerrar este dialogo.
                // Si quieres volver al detalle, comenta la linea de abajo.
                metaSeleccionadaDetalle = null
            }
        )
    }
}

// -------------------------------------------------------------------------
// COMPONENTES UI
// -------------------------------------------------------------------------

@Composable
fun DetalleMetaOpcionesDialog(
    meta: MetaEntity,
    historialAbonos: List<AbonoEntity>,
    onDismiss: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onAbonar: () -> Unit,
    onEditarAbono: (AbonoEntity, Double) -> Unit
) {
    var mostrarConfirmacionBorrar by remember { mutableStateOf(false) }
    var abonoParaEditar by remember { mutableStateOf<AbonoEntity?>(null) }

    // Cálculo de días restantes
    val diasRestantesTexto = remember(meta.fechaLimite) {
        if (meta.fechaLimite != null && meta.fechaLimite > 0) {
            val hoy = System.currentTimeMillis()
            val diferencia = meta.fechaLimite - hoy
            if (diferencia > 0) {
                val dias = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diferencia)
                "$dias días restantes"
            } else {
                "Fecha límite vencida"
            }
        } else {
            null
        }
    }

    // DIALOGO PARA EDITAR UN ABONO ESPECÍFICO
    if (abonoParaEditar != null) {
        DialogoEditarAbono(
            abono = abonoParaEditar!!,
            onDismiss = { abonoParaEditar = null },
            onConfirm = { nuevoMonto ->
                onEditarAbono(abonoParaEditar!!, nuevoMonto)
                abonoParaEditar = null
            }
        )
    }

    if (mostrarConfirmacionBorrar) {
        // ... (Este bloque de confirmación queda igual)
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionBorrar = false },
            containerColor = DarkSurface,
            title = { Text("¿Eliminar Meta?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Se perderá todo el historial de esta meta.", color = Color.Gray) },
            confirmButton = {
                Button(onClick = onEliminar, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = { TextButton(onClick = { mostrarConfirmacionBorrar = false }) { Text("Cancelar", color = Color.Gray) } }
        )
    } else {
        // --- DIÁLOGO PRINCIPAL AJUSTADO ---
        Dialog(onDismissRequest = onDismiss) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight() // IMPORTANTE: Se ajusta al contenido
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // 1. ENCABEZADO
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(getIconoByName(meta.icono), null, tint = NeonGreen, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(meta.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }

                    Spacer(Modifier.height(16.dp))

                    // 2. RESUMEN
                    Text("Objetivo: ${CurrencyUtils.formatCurrency(meta.montoObjetivo)}", color = Color.White)
                    Text("Ahorrado: ${CurrencyUtils.formatCurrency(meta.montoAhorrado)}", color = NeonGreen)

                    Spacer(Modifier.height(12.dp))

                    // 3. INFO EXTRA (Días y Nota)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (diasRestantesTexto != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(diasRestantesTexto, color = Color.Gray, fontSize = 14.sp)
                            }
                        }

                        if (meta.nota.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Description, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(meta.nota, color = Color.Gray, fontSize = 14.sp, maxLines = 3)
                            }
                        } else {
                            Text("Sin notas.", color = Color.Gray.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Divider(color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(Modifier.height(16.dp))

                    // 4. HISTORIAL DE ABONOS (MÁXIMO 3)
                    Text("Historial (Últimos 3)", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    if (historialAbonos.isEmpty()) {
                        // Mensaje pequeño si está vacío
                        Text(
                            "Sin abonos recientes",
                            color = Color.Gray.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        // Usamos Column normal en lugar de LazyColumn para que se ajuste la altura
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // TOMAMOS SOLO LOS PRIMEROS 3
                            historialAbonos.take(3).forEach { abono ->
                                ItemHistorialAbono(abono = abono, onEditClick = { abonoParaEditar = abono })
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // 5. BOTONES
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cerrar", color = Color.Gray) }

                        Row {
                            IconButton(onClick = onEditar) { Icon(Icons.Default.Edit, null, tint = Color.White) }
                            IconButton(onClick = { mostrarConfirmacionBorrar = true }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            Button(
                                onClick = onAbonar,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("Abonar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemHistorialAbono(abono: AbonoEntity, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(CurrencyUtils.formatCurrency(abono.monto), color = Color.White, fontWeight = FontWeight.Bold)
            val fechaStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(Date(abono.fecha))
            Text(fechaStr, color = Color.Gray, fontSize = 12.sp)
        }
        IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, null, tint = NeonGreen, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun DialogoEditarAbono(abono: AbonoEntity, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var montoStr by remember { mutableStateOf(abono.monto.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = { Text("Corregir Abono", color = Color.White) },
        text = {
            Column {
                Text("Ingresa el monto correcto:", color = Color.Gray, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = montoStr,
                    onValueChange = { montoStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val m = montoStr.toDoubleOrNull()
                    if (m != null && m >= 0) onConfirm(m)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
        }
    )
}

// --- RESTO DE COMPONENTES (MetaItemCard, PantallaCrearEditarMeta, etc.) ---
// Asegúrate de incluir aquí el resto del código que ya tenías (MetaItemCard, PantallaCrearEditarMeta, getIconoByName, etc.)
// No los repito para no hacer la respuesta demasiado larga, pero DEBEN estar en el archivo.

@Composable
fun MetaItemCard(meta: MetaEntity, onClick: () -> Unit, onAbonarClick: () -> Unit) {
    // ... (El código de MetaItemCard que te di en la respuesta anterior) ...
    val progreso = if (meta.montoObjetivo > 0) (meta.montoAhorrado / meta.montoObjetivo).toFloat().coerceIn(0f, 1f) else 0f
    val porcentaje = (progreso * 100).toInt()

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val iconoVector = getIconoByName(meta.icono)
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(NeonGreen.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                        Icon(iconoVector, null, tint = NeonGreen, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(meta.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("En progreso", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Text("$porcentaje%", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.1f))) {
                Box(modifier = Modifier.fillMaxWidth(progreso).fillMaxHeight().clip(RoundedCornerShape(50)).background(NeonGreen))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("${CurrencyUtils.formatCurrency(meta.montoAhorrado)} / ${CurrencyUtils.formatCurrency(meta.montoObjetivo)}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onAbonarClick, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black), enabled = progreso < 1.0f) {
                Text(if (progreso >= 1.0f) "Completada" else "Abonar Dinero", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCrearEditarMeta(
    metaExistente: MetaEntity?,
    cuentas: List<CuentaEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Date?, Int, String) -> Unit
) {
    // ... (Mismo código de PantallaCrearEditarMeta de la respuesta anterior) ...
    // Copialo tal cual lo tenías, no ha cambiado la lógica de crear/editar la meta en sí, solo agregamos historial.
    var nombre by remember { mutableStateOf(metaExistente?.nombre ?: "") }
    var objetivo by remember { mutableStateOf(metaExistente?.montoObjetivo?.toString() ?: "") }
    var iconoSeleccionado by remember { mutableStateOf(metaExistente?.icono ?: "Savings") }
    var fechaSeleccionada by remember { mutableStateOf<Date?>(metaExistente?.fechaLimite?.let { Date(it) }) }
    var cuentaSeleccionadaId by remember { mutableStateOf(metaExistente?.cuentaId ?: if (cuentas.isNotEmpty()) cuentas.first().id else -1) }
    var nota by remember { mutableStateOf(metaExistente?.nota ?: "") }

    val iconosDisponibles = listOf("Savings" to Icons.Default.Savings, "DirectionsCar" to Icons.Default.DirectionsCar, "TwoWheeler" to Icons.Default.TwoWheeler, "Home" to Icons.Default.Home, "Flight" to Icons.Default.Flight, "Smartphone" to Icons.Default.Smartphone, "Computer" to Icons.Default.Computer, "School" to Icons.Default.School, "Pets" to Icons.Default.Pets, "ShoppingBag" to Icons.Default.ShoppingBag)
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(context, { _: DatePicker, year: Int, month: Int, day: Int -> val cal = Calendar.getInstance(); cal.set(year, month, day); fechaSeleccionada = cal.time }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    var menuCuentasExpanded by remember { mutableStateOf(false) }

    Scaffold(containerColor = BackgroundColor, topBar = { CenterAlignedTopAppBar(title = { Text(if (metaExistente == null) "Agregar Meta" else "Editar Meta", fontWeight = FontWeight.Bold, color = Color.White) }, navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White) } }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundColor)) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.size(80.dp).border(2.dp, NeonGreen.copy(alpha = 0.5f), CircleShape).padding(4.dp).clip(CircleShape).background(Color.Transparent), contentAlignment = Alignment.Center) {
                Icon(getIconoByName(iconoSeleccionado), null, tint = NeonGreen, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(12.dp)); Text("Seleccionar Icono", fontSize = 14.sp, color = Color.Gray); Spacer(modifier = Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                items(iconosDisponibles) { (nombre, vector) ->
                    val isSelected = nombre == iconoSeleccionado
                    Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(if (isSelected) NeonGreen else DarkSurface).clickable { iconoSeleccionado = nombre }, contentAlignment = Alignment.Center) { Icon(imageVector = vector, contentDescription = null, tint = if (isSelected) Color.Black else Color.Gray) }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            InputLabel("Nombre de la Meta"); CampoTextoDark(value = nombre, onValueChange = { nombre = it }, placeholder = "Ej. Viaje")
            Spacer(modifier = Modifier.height(20.dp))
            InputLabel("Monto Objetivo"); CampoTextoDark(value = objetivo, onValueChange = { objetivo = it }, placeholder = "$ 0.00", keyboardType = KeyboardType.Number)
            Spacer(modifier = Modifier.height(20.dp))
            InputLabel("Cuenta Asociada")
            Box(modifier = Modifier.fillMaxWidth()) {
                val nombreCuenta = cuentas.find { it.id == cuentaSeleccionadaId }?.nombre ?: "Seleccionar"
                Row(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(DarkSurface).clickable { menuCuentasExpanded = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(nombreCuenta, color = Color.White); Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray) }
                DropdownMenu(expanded = menuCuentasExpanded, onDismissRequest = { menuCuentasExpanded = false }) { cuentas.forEach { c -> DropdownMenuItem(text = { Text(c.nombre) }, onClick = { cuentaSeleccionadaId = c.id; menuCuentasExpanded = false }) } }
            }
            Spacer(modifier = Modifier.height(20.dp))
            InputLabel("Fecha Límite")
            Row(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(DarkSurface).clickable { datePickerDialog.show() }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                val textoFecha = if(fechaSeleccionada != null) DateUtils.formatearFechaAmigable(fechaSeleccionada!!) else "Seleccionar fecha"
                Text(textoFecha, color = if(fechaSeleccionada != null) Color.White else Color.Gray); Icon(Icons.Default.CalendarToday, null, tint = Color.Gray)
            }
            Spacer(modifier = Modifier.height(20.dp))
            InputLabel("Notas"); CampoTextoDark(value = nota, onValueChange = { nota = it }, placeholder = "Descripción...", singleLine = false, modifier = Modifier.height(80.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { val obj = objetivo.toDoubleOrNull() ?: 0.0; if (nombre.isNotEmpty() && obj > 0) { onConfirm(nombre, obj, iconoSeleccionado, fechaSeleccionada, cuentaSeleccionadaId, nota) } }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)) { Text(if (metaExistente == null) "Crear Meta" else "Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CampoTextoDark(value: String, onValueChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, modifier: Modifier = Modifier) {
    BasicTextField(value = value, onValueChange = onValueChange, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp), keyboardOptions = KeyboardOptions(keyboardType = keyboardType), singleLine = singleLine, cursorBrush = androidx.compose.ui.graphics.SolidColor(NeonGreen), modifier = modifier.fillMaxWidth(), decorationBox = { innerTextField -> Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(DarkSurface).padding(horizontal = 16.dp, vertical = 16.dp)) { if (value.isEmpty()) Text(placeholder, color = Color.Gray); innerTextField() } })
}

@Composable
fun DialogoAbonar(meta: MetaEntity, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var monto by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = DarkSurface, title = { Text("Abonar a ${meta.nombre}", color = Color.White) }, text = { OutlinedTextField(value = monto, onValueChange = { monto = it }, label = { Text("Cantidad") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, unfocusedBorderColor = Color.Gray, focusedLabelColor = NeonGreen, unfocusedLabelColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White)) }, confirmButton = { Button(onClick = { val m = monto.toDoubleOrNull() ?: 0.0; if (m > 0) onConfirm(m) }, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)) { Text("Abonar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) } })
}

@Composable
fun InputLabel(text: String) { Text(text = text, color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 4.dp)) }

fun getIconoByName(nombre: String): ImageVector { return when (nombre) { "DirectionsCar" -> Icons.Default.DirectionsCar; "TwoWheeler" -> Icons.Default.TwoWheeler; "Home" -> Icons.Default.Home; "Flight" -> Icons.Default.Flight; "Smartphone" -> Icons.Default.Smartphone; "Computer" -> Icons.Default.Computer; "School" -> Icons.Default.School; "Pets" -> Icons.Default.Pets; "ShoppingBag" -> Icons.Default.ShoppingBag; else -> Icons.Default.Savings } }