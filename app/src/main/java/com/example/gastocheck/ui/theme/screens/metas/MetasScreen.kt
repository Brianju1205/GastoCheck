package com.example.gastocheck.ui.theme.screens.metas

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.data.database.entity.AbonoEntity
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.MetaEntity
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import com.example.gastocheck.ui.theme.util.DateUtils
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasScreen(viewModel: MetasViewModel = hiltViewModel()) {
    // Datos BD
    val metasBD by viewModel.metas.collectAsState()
    val cuentas by viewModel.cuentas.collectAsState()

    // Lista Local Mutable para la animación fluida
    val metasLocales = remember { mutableStateListOf<MetaEntity>() }

    // Estados UI
    var mostrarPantallaCrear by remember { mutableStateOf(false) }
    var metaParaEditar by remember { mutableStateOf<MetaEntity?>(null) }
    var metaSeleccionadaDetalle by remember { mutableStateOf<MetaEntity?>(null) }
    var mostrarDialogoAbonar by remember { mutableStateOf(false) }

    // Estados Drag & Drop Avanzado
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Índice del item que estamos arrastrando
    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    // Desplazamiento visual (cuánto movimos el dedo en Y)
    var draggingItemOffset by remember { mutableStateOf(0f) }

    // Sincronización inicial y post-cancelación
    LaunchedEffect(metasBD) {
        if (draggingItemIndex == null) {
            metasLocales.clear()
            metasLocales.addAll(metasBD)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mis Metas", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
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
        if (metasLocales.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tienes metas activas", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    // DETECTOR DE GESTOS PRINCIPAL
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { offset ->
                                // Identificar qué item tocamos
                                listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { item -> offset.y.toInt() in item.offset..(item.offset + item.size) }
                                    ?.also { itemInfo ->
                                        val index = itemInfo.index
                                        if (index in metasLocales.indices) {
                                            draggingItemIndex = index
                                            draggingItemOffset = 0f
                                        }
                                    }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val dragIndex = draggingItemIndex ?: return@detectDragGesturesAfterLongPress

                                // 1. Actualizar posición visual de la tarjeta flotante
                                draggingItemOffset += dragAmount.y

                                // 2. Lógica de Intercambio (Swap)
                                val currentItemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == dragIndex }
                                if (currentItemInfo != null) {
                                    // Determinar dirección y próximo índice
                                    val nextIndex = if (draggingItemOffset > 0) dragIndex + 1 else dragIndex - 1

                                    // Verificar límites
                                    if (nextIndex in metasLocales.indices) {
                                        val nextItemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == nextIndex }
                                        // Si solapamos lo suficiente el siguiente item...
                                        if (nextItemInfo != null) {
                                            // Umbral: mover al menos un 30% sobre el otro item
                                            val threshold = (currentItemInfo.size + nextItemInfo.size) / 2 * 0.3f
                                            if (draggingItemOffset.absoluteValue > threshold) {
                                                // SWAP en la lista local
                                                metasLocales.apply { add(nextIndex, removeAt(dragIndex)) }

                                                // Actualizamos índices y reseteamos offset parcial
                                                // (Importante: El offset total visual se mantiene para fluidez,
                                                //  pero ajustamos la base lógica)
                                                draggingItemIndex = nextIndex
                                                draggingItemOffset = 0f
                                            }
                                        }
                                    }

                                    // 3. AUTO-SCROLL en los bordes
                                    // Definir zonas activas (top y bottom)
                                    val viewportHeight = listState.layoutInfo.viewportSize.height
                                    val scrollZone = viewportHeight * 0.15f // 15% superior e inferior

                                    val topEdge = currentItemInfo.offset
                                    val bottomEdge = currentItemInfo.offset + currentItemInfo.size

                                    if (change.position.y < scrollZone) {
                                        // Scroll Arriba
                                        scope.launch { listState.scrollBy(-15f) }
                                    } else if (change.position.y > viewportHeight - scrollZone) {
                                        // Scroll Abajo
                                        scope.launch { listState.scrollBy(15f) }
                                    }
                                }
                            },
                            onDragEnd = {
                                if (draggingItemIndex != null) {
                                    // Guardar orden final en BD
                                    viewModel.actualizarOrdenMetas(metasLocales.toList())
                                }
                                draggingItemIndex = null
                                draggingItemOffset = 0f
                            },
                            onDragCancel = {
                                draggingItemIndex = null
                                draggingItemOffset = 0f
                                // Revertir cambios visuales
                                metasLocales.clear()
                                metasLocales.addAll(metasBD)
                            }
                        )
                    },
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                itemsIndexed(items = metasLocales, key = { _, meta -> meta.id }) { index, meta ->
                    val isDragging = index == draggingItemIndex

                    // --- ANIMACIONES FÍSICAS ---

                    // Escala: Crece un poco al levantarla
                    val scale by animateFloatAsState(
                        targetValue = if (isDragging) 1.05f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "scale"
                    )

                    // Elevación: Sombra más fuerte al levantarla
                    val elevation by animateDpAsState(
                        targetValue = if (isDragging) 16.dp else 0.dp,
                        label = "elevation"
                    )

                    // Traslación Y: Sigue al dedo SOLO si es la tarjeta arrastrada
                    val translationY = if (isDragging) draggingItemOffset else 0f

                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.translationY = translationY // <--- Movimiento fluido
                                shadowElevation = elevation.toPx()
                                shape = RoundedCornerShape(24.dp)
                                clip = true
                                alpha = if (isDragging) 0.95f else 1f
                            }
                            .zIndex(if (isDragging) 10f else 0f) // Siempre encima
                    ) {
                        MetaItemCard(
                            meta = meta,
                            // Desactivamos clicks mientras arrastramos para evitar abrir detalles por error
                            onClick = { if (!isDragging) metaSeleccionadaDetalle = meta },
                            onAbonarClick = {
                                if (!isDragging) {
                                    metaSeleccionadaDetalle = meta
                                    mostrarDialogoAbonar = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // --- PANTALLA CREAR / EDITAR (DISEÑO RESTAURADO) ---
    if (mostrarPantallaCrear) {
        Dialog(
            onDismissRequest = { mostrarPantallaCrear = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            PantallaCrearEditarMeta(
                metaExistente = metaParaEditar,
                cuentas = cuentas,
                onDismiss = { mostrarPantallaCrear = false },
                onConfirm = { nombre, objetivo, icono, colorHex, fecha, cuentaId, nota ->
                    viewModel.guardarMeta(
                        id = metaParaEditar?.id ?: 0,
                        nombre = nombre,
                        objetivo = objetivo,
                        icono = icono,
                        colorHex = colorHex,
                        fechaLimite = fecha,
                        cuentaId = cuentaId,
                        nota = nota
                    )
                    mostrarPantallaCrear = false
                }
            )
        }
    }

    // --- DIALOGO DETALLES ---
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

    // --- DIALOGO ABONAR (DISEÑO RESTAURADO) ---
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
// COMPONENTES UI (DISEÑOS ANTERIORES RESTAURADOS)
// -------------------------------------------------------------------------

@Composable
fun MetaItemCard(meta: MetaEntity, onClick: () -> Unit, onAbonarClick: () -> Unit) {
    val progreso = if (meta.montoObjetivo > 0) (meta.montoAhorrado / meta.montoObjetivo).toFloat().coerceIn(0f, 1f) else 0f
    val porcentaje = (progreso * 100).toInt()

    val colorMeta = try { Color(android.graphics.Color.parseColor(meta.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
    val colorFondoCard = MaterialTheme.colorScheme.surfaceVariant

    // IMPORTANTE: Quitamos el shadowElevation aquí porque lo maneja el Drag & Drop
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
                            .background(colorMeta.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(iconoVector, null, tint = colorMeta, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(meta.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if(porcentaje >= 100) "¡Completada!" else "En progreso", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
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
                        .background(colorMeta)
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
                colors = ButtonDefaults.buttonColors(containerColor = colorMeta, contentColor = Color.White),
                enabled = progreso < 1.0f
            ) {
                Text(if (progreso >= 1.0f) "Meta Cumplida" else "Abonar Dinero", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// --- PANTALLA CREAR/EDITAR (ESTILO "ANTIGUO" RESTAURADO CON COLOR) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCrearEditarMeta(
    metaExistente: MetaEntity?,
    cuentas: List<CuentaEntity>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, Date?, Int, String) -> Unit
) {
    var nombre by remember { mutableStateOf(metaExistente?.nombre ?: "") }
    var objetivo by remember { mutableStateOf(metaExistente?.montoObjetivo?.toString() ?: "") }
    var iconoSeleccionado by remember { mutableStateOf(metaExistente?.icono ?: "Savings") }
    var colorSeleccionado by remember { mutableStateOf(metaExistente?.colorHex ?: "#00E676") }
    var fechaSeleccionada by remember { mutableStateOf<Date?>(metaExistente?.fechaLimite?.let { Date(it) }) }
    var cuentaSeleccionadaId by remember { mutableStateOf(metaExistente?.cuentaId ?: if (cuentas.isNotEmpty()) cuentas.first().id else -1) }
    var nota by remember { mutableStateOf(metaExistente?.nota ?: "") }

    val iconosDisponibles = listOf("Savings", "DirectionsCar", "TwoWheeler", "Home", "Flight", "Smartphone", "Computer", "School", "Pets", "ShoppingBag")
    val listaColores = listOf("#00E676", "#2979FF", "#FFD700", "#FF1744", "#AA00FF", "#FF9100", "#00B0FF", "#00C853", "#607D8B", "#795548")

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(context, { _, y, m, d -> val cal = Calendar.getInstance(); cal.set(y, m, d); fechaSeleccionada = cal.time }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    var menuCuentasExpanded by remember { mutableStateOf(false) }
    val colorFondo = MaterialTheme.colorScheme.background
    val colorSurface = MaterialTheme.colorScheme.surfaceVariant
    val colorPrimario = MaterialTheme.colorScheme.primary

    // Usamos Scaffold para pantalla completa como pediste
    Scaffold(
        modifier = Modifier.imePadding(),
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

            // PREVISUALIZACIÓN (ICONO + COLOR)
            val colorPreview = try { Color(android.graphics.Color.parseColor(colorSeleccionado)) } catch (e: Exception) { colorPrimario }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(2.dp, colorPreview.copy(alpha = 0.5f), CircleShape)
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Icon(getIconoByName(iconoSeleccionado), null, tint = colorPreview, modifier = Modifier.size(40.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Personaliza tu meta", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(20.dp))

            // SELECTOR ICONOS
            InputLabel("Icono")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                items(iconosDisponibles.map { it to getIconoByName(it) }) { (nombre, vector) ->
                    val isSelected = nombre == iconoSeleccionado
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) colorPreview else colorSurface)
                            .clickable { iconoSeleccionado = nombre },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = vector, contentDescription = null, tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SELECTOR COLORES (INTEGRADO EN EL DISEÑO ANTIGUO)
            InputLabel("Color")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                items(listaColores) { colorHex ->
                    val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color.Gray }
                    val isSelected = colorSeleccionado == colorHex
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(if (isSelected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                            .clickable { colorSeleccionado = colorHex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // CAMPOS DE TEXTO (ESTILO TEMÁTICO RESTAURADO)
            InputLabel("Nombre de la Meta")
            CampoTextoTematico(value = nombre, onValueChange = { nombre = it }, placeholder = "Ej. Viaje")

            Spacer(modifier = Modifier.height(20.dp))

            InputLabel("Monto Objetivo")
            CampoTextoTematico(value = objetivo, onValueChange = { objetivo = it }, placeholder = "$ 0.00", keyboardType = KeyboardType.Number)

            Spacer(modifier = Modifier.height(20.dp))

            InputLabel("Cuenta Asociada")
            Box(modifier = Modifier.fillMaxWidth()) {
                val nombreCuenta = cuentas.find { it.id == cuentaSeleccionadaId }?.nombre ?: "Seleccionar Cuenta"
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
            CampoTextoTematico(value = nota, onValueChange = { nota = it }, placeholder = "Descripción...", singleLine = false, modifier = Modifier.height(80.dp))

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val obj = objetivo.toDoubleOrNull() ?: 0.0
                    if (nombre.isNotEmpty() && obj > 0) {
                        onConfirm(nombre, obj, iconoSeleccionado, colorSeleccionado, fechaSeleccionada, cuentaSeleccionadaId, nota)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorPreview, contentColor = Color.White)
            ) {
                Text(if (metaExistente == null) "Crear Meta" else "Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- DIALOGO ABONAR (ESTILO RESTAURADO) ---
@Composable
fun DialogoAbonar(meta: MetaEntity, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var monto by remember { mutableStateOf("") }
    // Usamos un Dialog personalizado en lugar de AlertDialog para restaurar tu diseño previo
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Abonar a ${meta.nombre}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                InputLabel("Cantidad")
                CampoTextoTematico(
                    value = monto,
                    onValueChange = { monto = it },
                    placeholder = "$ 0.00",
                    keyboardType = KeyboardType.Number
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { val m = monto.toDoubleOrNull(); if(m!=null && m>0) onConfirm(m) }
                    ) { Text("Abonar") }
                }
            }
        }
    }
}

// --- COMPONENTES AUXILIARES RESTAURADOS ---
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
fun InputLabel(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 14.sp,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp).fillMaxWidth()
    )
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
    val colorMeta = try { Color(android.graphics.Color.parseColor(meta.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
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
            confirmButton = { Button(onClick = onEliminar, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { mostrarConfirmacionBorrar = false }) { Text("Cancelar") } }
        )
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(getIconoByName(meta.icono), null, tint = colorMeta, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(meta.nombre, color = colorTexto, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Objetivo: ${CurrencyUtils.formatCurrency(meta.montoObjetivo)}", color = colorTexto)
                    Text("Ahorrado: ${CurrencyUtils.formatCurrency(meta.montoAhorrado)}", color = colorMeta, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    if (diasRestantesTexto != null) {
                        Text(diasRestantesTexto, color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
                    }
                    if (meta.nota.isNotEmpty()) {
                        Text(meta.nota, color = MaterialTheme.colorScheme.outline, fontSize = 14.sp, maxLines = 3)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Historial", color = colorTexto, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    if (historialAbonos.isEmpty()) {
                        Text("Sin abonos", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                    } else {
                        historialAbonos.take(3).forEach { abono ->
                            ItemHistorialAbono(abono = abono, onEditClick = { abonoParaEditar = abono })
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onDismiss) { Text("Cerrar") }
                        Row {
                            IconButton(onClick = onEditar) { Icon(Icons.Default.Edit, null) }
                            IconButton(onClick = { mostrarConfirmacionBorrar = true }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                            Button(onClick = onAbonar, colors = ButtonDefaults.buttonColors(containerColor = colorMeta)) { Text("Abonar") }
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(CurrencyUtils.formatCurrency(abono.monto))
        IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)) }
    }
}

@Composable
fun DialogoEditarAbono(abono: AbonoEntity, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var montoStr by remember { mutableStateOf(abono.monto.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Abono") },
        text = { OutlinedTextField(value = montoStr, onValueChange = { montoStr = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
        confirmButton = { Button(onClick = { val m = montoStr.toDoubleOrNull(); if (m != null) onConfirm(m) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
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