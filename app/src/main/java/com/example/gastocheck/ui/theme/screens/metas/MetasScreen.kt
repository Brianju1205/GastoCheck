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
import com.example.gastocheck.data.database.entity.AbonoEntity
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.MetaEntity
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import com.example.gastocheck.ui.theme.util.DateUtils
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mis Metas", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    metaParaEditar = null
                    mostrarPantallaCrear = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Meta")
            }
        }
    ) { padding ->
        if (metas.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tienes metas activas", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    // DIALOGO DETALLES
    if (metaSeleccionadaDetalle != null && !mostrarDialogoAbonar) {
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
                metaSeleccionadaDetalle = null
            }
        )
    }
}

// -------------------------------------------------------------------------
// COMPONENTES UI
// -------------------------------------------------------------------------

@Composable
fun MetaItemCard(meta: MetaEntity, onClick: () -> Unit, onAbonarClick: () -> Unit) {
    val progreso = if (meta.montoObjetivo > 0) (meta.montoAhorrado / meta.montoObjetivo).toFloat().coerceIn(0f, 1f) else 0f
    val porcentaje = (progreso * 100).toInt()
    val colorPrimario = MaterialTheme.colorScheme.primary
    val colorFondoCard = MaterialTheme.colorScheme.surfaceVariant

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorFondoCard),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val iconoVector = getIconoByName(meta.icono)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colorPrimario.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(iconoVector, null, tint = colorPrimario, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(meta.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("En progreso", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
                Text("$porcentaje%", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progreso)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(colorPrimario)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${CurrencyUtils.formatCurrency(meta.montoAhorrado)} / ${CurrencyUtils.formatCurrency(meta.montoObjetivo)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAbonarClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorPrimario, contentColor = MaterialTheme.colorScheme.onPrimary),
                enabled = progreso < 1.0f
            ) {
                Text(if (progreso >= 1.0f) "Completada" else "Abonar Dinero", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

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
    val colorPrimario = MaterialTheme.colorScheme.primary
    val colorTexto = MaterialTheme.colorScheme.onSurface

    val diasRestantesTexto = remember(meta.fechaLimite) {
        if (meta.fechaLimite != null && meta.fechaLimite > 0) {
            val hoy = System.currentTimeMillis()
            val diferencia = meta.fechaLimite - hoy
            if (diferencia > 0) {
                val dias = TimeUnit.MILLISECONDS.toDays(diferencia)
                "$dias días restantes"
            } else {
                "Fecha límite vencida"
            }
        } else {
            null
        }
    }

    if (abonoParaEditar != null) {
        DialogoEditarAbono(
            abono = abonoParaEditar!!,
            onDismiss = { abonoParaEditar = null },
            onConfirm = { nuevoMonto -> onEditarAbono(abonoParaEditar!!, nuevoMonto); abonoParaEditar = null }
        )
    }

    if (mostrarConfirmacionBorrar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionBorrar = false },
            title = { Text("¿Eliminar Meta?") },
            text = { Text("Se perderá todo el historial de esta meta.") },
            confirmButton = {
                Button(onClick = onEliminar, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Eliminar")
                }
            },
            dismissButton = { TextButton(onClick = { mostrarConfirmacionBorrar = false }) { Text("Cancelar") } }
        )
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(getIconoByName(meta.icono), null, tint = colorPrimario, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(meta.nombre, color = colorTexto, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }

                    Spacer(Modifier.height(16.dp))

                    Text("Objetivo: ${CurrencyUtils.formatCurrency(meta.montoObjetivo)}", color = colorTexto)
                    Text("Ahorrado: ${CurrencyUtils.formatCurrency(meta.montoAhorrado)}", color = colorPrimario, fontWeight = FontWeight.Bold)

                    Spacer(Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (diasRestantesTexto != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(diasRestantesTexto, color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
                            }
                        }

                        if (meta.nota.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(meta.nota, color = MaterialTheme.colorScheme.outline, fontSize = 14.sp, maxLines = 3)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(16.dp))

                    Text("Historial (Últimos 3)", color = colorTexto, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    if (historialAbonos.isEmpty()) {
                        Text("Sin abonos recientes", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            historialAbonos.take(3).forEach { abono ->
                                ItemHistorialAbono(abono = abono, onEditClick = { abonoParaEditar = abono })
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) { Text("Cerrar") }
                        Row {
                            IconButton(onClick = onEditar) { Icon(Icons.Default.Edit, null, tint = colorTexto) }
                            IconButton(onClick = { mostrarConfirmacionBorrar = true }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            Button(
                                onClick = onAbonar,
                                colors = ButtonDefaults.buttonColors(containerColor = colorPrimario, contentColor = MaterialTheme.colorScheme.onPrimary),
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(CurrencyUtils.formatCurrency(abono.monto), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            val fechaStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(Date(abono.fecha))
            Text(fechaStr, color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
        }
        IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun DialogoEditarAbono(abono: AbonoEntity, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var montoStr by remember { mutableStateOf(abono.monto.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Corregir Abono") },
        text = {
            Column {
                Text("Ingresa el monto correcto:", fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = montoStr,
                    onValueChange = { montoStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { val m = montoStr.toDoubleOrNull(); if (m != null && m >= 0) onConfirm(m) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCrearEditarMeta(
    metaExistente: MetaEntity?,
    cuentas: List<CuentaEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, Date?, Int, String) -> Unit
) {
    var nombre by remember { mutableStateOf(metaExistente?.nombre ?: "") }
    var objetivo by remember { mutableStateOf(metaExistente?.montoObjetivo?.toString() ?: "") }
    var iconoSeleccionado by remember { mutableStateOf(metaExistente?.icono ?: "Savings") }
    var fechaSeleccionada by remember { mutableStateOf<Date?>(metaExistente?.fechaLimite?.let { Date(it) }) }
    var cuentaSeleccionadaId by remember { mutableStateOf(metaExistente?.cuentaId ?: if (cuentas.isNotEmpty()) cuentas.first().id else -1) }
    var nota by remember { mutableStateOf(metaExistente?.nota ?: "") }

    val iconosDisponibles = listOf("Savings", "DirectionsCar", "TwoWheeler", "Home", "Flight", "Smartphone", "Computer", "School", "Pets", "ShoppingBag")
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(context, { _: DatePicker, year: Int, month: Int, day: Int ->
        val cal = Calendar.getInstance()
        cal.set(year, month, day)
        fechaSeleccionada = cal.time
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    var menuCuentasExpanded by remember { mutableStateOf(false) }
    val colorFondo = MaterialTheme.colorScheme.background
    val colorSurface = MaterialTheme.colorScheme.surfaceVariant
    val colorPrimario = MaterialTheme.colorScheme.primary

    Scaffold(
        modifier = Modifier.imePadding(), // <--- SOLUCIÓN CLAVE: Ajusta el contenido cuando sale el teclado
        containerColor = colorFondo,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (metaExistente == null) "Agregar Meta" else "Editar Meta", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colorFondo)
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
            Spacer(modifier = Modifier.height(24.dp))

            // ICONO
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(2.dp, colorPrimario.copy(alpha = 0.5f), CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(getIconoByName(iconoSeleccionado), null, tint = colorPrimario, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Seleccionar Icono", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                items(iconosDisponibles.map { it to getIconoByName(it) }) { (nombre, vector) ->
                    val isSelected = nombre == iconoSeleccionado
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) colorPrimario else colorSurface)
                            .clickable { iconoSeleccionado = nombre },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = vector, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            InputLabel("Nombre de la Meta")
            CampoTextoTematico(value = nombre, onValueChange = { nombre = it }, placeholder = "Ej. Viaje")

            Spacer(modifier = Modifier.height(20.dp))

            InputLabel("Monto Objetivo")
            CampoTextoTematico(value = objetivo, onValueChange = { objetivo = it }, placeholder = "$ 0.00", keyboardType = KeyboardType.Number)

            Spacer(modifier = Modifier.height(20.dp))

            InputLabel("Cuenta Asociada")
            Box(modifier = Modifier.fillMaxWidth()) {
                val nombreCuenta = cuentas.find { it.id == cuentaSeleccionadaId }?.nombre ?: "Seleccionar"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorSurface)
                        .clickable { menuCuentasExpanded = true }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(nombreCuenta, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuCuentasExpanded, onDismissRequest = { menuCuentasExpanded = false }) {
                    cuentas.forEach { c -> DropdownMenuItem(text = { Text(c.nombre) }, onClick = { cuentaSeleccionadaId = c.id; menuCuentasExpanded = false }) }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            InputLabel("Fecha Límite")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorSurface)
                    .clickable { datePickerDialog.show() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val textoFecha = if(fechaSeleccionada != null) DateUtils.formatearFechaAmigable(fechaSeleccionada!!) else "Seleccionar fecha"
                val textColor = if(fechaSeleccionada != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                Text(textoFecha, color = textColor)
                Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(20.dp))

            InputLabel("Notas")
            // Ahora este campo crece según lo que escribas o tiene un alto fijo razonable
            CampoTextoTematico(value = nota, onValueChange = { nota = it }, placeholder = "Descripción...", singleLine = false, modifier = Modifier.height(80.dp))

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val obj = objetivo.toDoubleOrNull() ?: 0.0
                    if (nombre.isNotEmpty() && obj > 0) {
                        onConfirm(nombre, obj, iconoSeleccionado, fechaSeleccionada, cuentaSeleccionadaId, nota)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorPrimario, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text(if (metaExistente == null) "Crear Meta" else "Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CampoTextoTematico(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colorCursor = MaterialTheme.colorScheme.primary
    val colorTexto = MaterialTheme.colorScheme.onSurfaceVariant
    val colorFondo = MaterialTheme.colorScheme.surfaceVariant

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(color = colorTexto, fontSize = 16.sp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        cursorBrush = SolidColor(colorCursor),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorFondo)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                if (value.isEmpty()) Text(placeholder, color = colorTexto.copy(alpha = 0.5f))
                innerTextField()
            }
        }
    )
}

@Composable
fun DialogoAbonar(meta: MetaEntity, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var monto by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abonar a ${meta.nombre}") },
        text = {
            OutlinedTextField(
                value = monto,
                onValueChange = { monto = it },
                label = { Text("Cantidad") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        },
        confirmButton = {
            Button(
                onClick = { val m = monto.toDoubleOrNull() ?: 0.0; if (m > 0) onConfirm(m) }
            ) { Text("Abonar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun InputLabel(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 4.dp)
    )
}

fun getIconoByName(nombre: String): ImageVector {
    return when (nombre) {
        "DirectionsCar" -> Icons.Default.DirectionsCar
        "TwoWheeler" -> Icons.Default.TwoWheeler
        "Home" -> Icons.Default.Home
        "Flight" -> Icons.Default.Flight
        "Smartphone" -> Icons.Default.Smartphone
        "Computer" -> Icons.Default.Computer
        "School" -> Icons.Default.School
        "Pets" -> Icons.Default.Pets
        "ShoppingBag" -> Icons.Default.ShoppingBag
        else -> Icons.Default.Savings
    }
}