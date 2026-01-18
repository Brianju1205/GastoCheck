package com.example.gastocheck.ui.theme.screens.metas

import android.app.DatePickerDialog
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
// ✅ Librería Reorderable (Indispensable)
import org.burnoutcrew.reorderable.*
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MetasScreen(viewModel: MetasViewModel = hiltViewModel()) {
    // 1. DATOS DE LA BD
    val rawMetas by viewModel.metasFiltradas.collectAsState()
    val filtroActual by viewModel.filtroActual.collectAsState()
    val cuentas by viewModel.cuentas.collectAsState()
    val haptic = LocalHapticFeedback.current

    // 🔴 2. LISTA LOCAL MUTABLE (CLAVE PARA FLUIDEZ)
    // Usamos esta lista para la animación instantánea, sin esperar a la BD.
    val localMetas = remember { mutableStateListOf<MetaEntity>() }

    // Sincronización: Si la BD cambia (carga inicial o filtros), actualizamos la local.
    LaunchedEffect(rawMetas) {
        val unicas = rawMetas.distinctBy { it.id }
        if (localMetas.isEmpty() || localMetas.toList() != unicas) {
            localMetas.clear()
            localMetas.addAll(unicas)
        }
    }

    // 3. ESTADOS UI
    var mostrarPantallaCrear by remember { mutableStateOf(false) }
    var metaParaEditar by remember { mutableStateOf<MetaEntity?>(null) }
    var metaSeleccionadaDetalle by remember { mutableStateOf<MetaEntity?>(null) }
    var mostrarDialogoAbonar by remember { mutableStateOf(false) }

    // Solo permitimos arrastrar en filtros de orden manual
    val puedeReordenar = (filtroActual == FiltroMeta.PROGRESO || filtroActual == FiltroMeta.TODOS)

    // 🔴 4. CONFIGURACIÓN REORDERABLE
    val state = rememberReorderableLazyListState(
        onMove = { from, to ->
            // A. MOVIMIENTO VISUAL INSTANTÁNEO
            // Esto hace que los items se intercambien al momento en la pantalla
            localMetas.apply {
                add(to.index, removeAt(from.index))
            }
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        },
        onDragEnd = { _, _ ->
            // B. GUARDADO EN BD AL SOLTAR
            // Importante: Asegúrate de tener esta función en tu ViewModel
            viewModel.onReorder(0, 0, localMetas.toList())
        }
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                CenterAlignedTopAppBar(
                    title = { Text("Mis Metas", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                // CHIPS DE FILTRO
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = FiltroMeta.values()
                    items(tabs.size) { index ->
                        val filtro = tabs[index]
                        val isSelected = filtroActual == filtro
                        val titulo = when(filtro) {
                            FiltroMeta.PROGRESO -> "En Progreso"; FiltroMeta.CUMPLIDAS -> "Cumplidas"
                            FiltroMeta.VENCIDAS -> "Vencidas"; FiltroMeta.PAUSADAS -> "Pausadas"; FiltroMeta.TODOS -> "Todos"
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.cambiarFiltro(filtro) },
                            label = { Text(titulo) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary),
                            shape = RoundedCornerShape(50)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { metaParaEditar = null; mostrarPantallaCrear = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) { Icon(Icons.Default.Add, "Nueva Meta") }
        }
    ) { padding ->

        if (localMetas.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay metas para mostrar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            // 5. LAZY COLUMN (Usando localMetas)
            LazyColumn(
                state = state.listState,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .reorderable(state),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp, start = 16.dp, end = 16.dp)
            ) {
                // 🔴 IMPORTANTE: Usamos 'localMetas' aquí
                items(items = localMetas, key = { it.id }) { meta ->

                    ReorderableItem(state, key = meta.id) { isDragging ->

                        // Efectos visuales
                        val elevation by animateDpAsState(if (isDragging) 16.dp else 2.dp, label = "elev")
                        val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scale")
                        val alpha by animateFloatAsState(if (isDragging) 0.9f else 1f, label = "alpha")

                        LaunchedEffect(isDragging) {
                            if (isDragging) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                // 🔴 MAGIA 1: Z-INDEX (El que se mueve flota arriba)
                                .zIndex(if (isDragging) 1f else 0f)
                                // 🔴 MAGIA 2: ANIMACIÓN (Los otros se apartan)
                                .animateItemPlacement(
                                    animationSpec = spring(
                                        stiffness = Spring.StiffnessMediumLow,
                                        dampingRatio = Spring.DampingRatioLowBouncy
                                    )
                                )
                        ) {
                            MetaItemCard(
                                meta = meta,
                                elevation = elevation,
                                scale = scale,
                                alpha = alpha,
                                // Pasamos el detector de gestos SOLO si se puede reordenar
                                dragModifier = if (puedeReordenar) Modifier.detectReorder(state) else Modifier,
                                canDrag = puedeReordenar,
                                onClick = { if (!isDragging) metaSeleccionadaDetalle = meta },
                                onAbonarClick = { if (!isDragging) { metaSeleccionadaDetalle = meta; mostrarDialogoAbonar = true } }
                            )
                        }
                    }
                }
            }
        }
    }

    // --- PANTALLAS Y DIALOGOS ---
    if (mostrarPantallaCrear) {
        Dialog(onDismissRequest = { mostrarPantallaCrear = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            PantallaCrearEditarMeta(metaExistente = metaParaEditar, cuentas = cuentas, onDismiss = { mostrarPantallaCrear = false }, onConfirm = { nombre, obj, icon, color, fecha, cId, nota -> viewModel.guardarMeta(id = metaParaEditar?.id ?: 0, nombre, obj, icon, color, fecha, cId, nota); mostrarPantallaCrear = false })
        }
    }

    if (metaSeleccionadaDetalle != null && !mostrarDialogoAbonar) {
        val historialAbonos by viewModel.obtenerHistorialAbonos(metaSeleccionadaDetalle!!.id).collectAsState(initial = emptyList())
        DetalleMetaOpcionesDialog(
            meta = metaSeleccionadaDetalle!!,
            historialAbonos = historialAbonos,
            onDismiss = { metaSeleccionadaDetalle = null },
            onEditar = { metaParaEditar = metaSeleccionadaDetalle; metaSeleccionadaDetalle = null; mostrarPantallaCrear = true },
            onEliminar = { viewModel.borrarMeta(metaSeleccionadaDetalle!!); metaSeleccionadaDetalle = null },
            onAbonar = { mostrarDialogoAbonar = true },
            onEditarAbono = { abono, nuevoMonto -> viewModel.editarAbono(metaSeleccionadaDetalle!!, abono, nuevoMonto) },
            onTogglePausa = { viewModel.togglePausaMeta(metaSeleccionadaDetalle!!); metaSeleccionadaDetalle = null }
        )
    }

    if (mostrarDialogoAbonar && metaSeleccionadaDetalle != null) {
        DialogoAbonar(meta = metaSeleccionadaDetalle!!, onDismiss = { mostrarDialogoAbonar = false; metaSeleccionadaDetalle = null }, onConfirm = { monto -> viewModel.abonarAMeta(metaSeleccionadaDetalle!!, monto); mostrarDialogoAbonar = false; metaSeleccionadaDetalle = null })
    }
}

// -------------------------------------------------------------------------
// TARJETA DE META (ESTILO RULES TAB CON MANIJA DERECHA)
// -------------------------------------------------------------------------
@Composable
fun MetaItemCard(
    meta: MetaEntity,
    elevation: androidx.compose.ui.unit.Dp,
    scale: Float,
    alpha: Float,
    dragModifier: Modifier,
    canDrag: Boolean,
    onClick: () -> Unit,
    onAbonarClick: () -> Unit
) {
    val progreso = if (meta.montoObjetivo > 0) (meta.montoAhorrado / meta.montoObjetivo).toFloat().coerceIn(0f, 1f) else 0f
    val porcentaje = (progreso * 100).toInt()
    val colorMeta = try { Color(android.graphics.Color.parseColor(meta.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }

    val icon = getIconoByName(meta.icono)
    val bgAvatar = colorMeta.copy(alpha = 0.15f)
    val borderStroke = if (elevation > 2.dp) BorderStroke(2.dp, colorMeta) else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(elevation),
        shape = RoundedCornerShape(16.dp),
        border = borderStroke,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila Superior
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono
                Surface(
                    color = bgAvatar,
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(imageVector = icon, contentDescription = null, tint = colorMeta, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Textos
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = meta.nombre,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (meta.esPausada) {
                            Spacer(Modifier.width(8.dp))
                            MetaBadge(text = "Pausada", color = MaterialTheme.colorScheme.error)
                        } else if (progreso >= 1f) {
                            Spacer(Modifier.width(8.dp))
                            MetaBadge(text = "¡Lograda!", color = colorMeta)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${CurrencyUtils.formatCurrency(meta.montoAhorrado)} / ${CurrencyUtils.formatCurrency(meta.montoObjetivo)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.size(3.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(text = "$porcentaje%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = colorMeta)
                    }
                }

                // Manija de arrastre (Derecha)
                if (canDrag) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                    Icon(
                        imageVector = Icons.Rounded.DragHandle,
                        contentDescription = "Mover",
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier
                            .padding(start = 12.dp, end = 4.dp)
                            .size(24.dp)
                            .then(dragModifier) // ⚠️ GESTO AQUÍ
                    )
                }
            }

            // Barra Progreso y Botón
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))) {
                Box(modifier = Modifier.fillMaxWidth(progreso).fillMaxHeight().clip(RoundedCornerShape(50)).background(if(meta.esPausada) Color.Gray else colorMeta))
            }

            if (progreso < 1f && !meta.esPausada) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAbonarClick,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorMeta.copy(alpha = 0.1f), contentColor = colorMeta),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("Abonar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun MetaBadge(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
        Text(text = text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// -------------------------------------------------------------------------
// FUNCIONES AUXILIARES
// -------------------------------------------------------------------------
@Composable
fun DetalleMetaOpcionesDialog(meta: MetaEntity, historialAbonos: List<AbonoEntity>, onDismiss: () -> Unit, onEditar: () -> Unit, onEliminar: () -> Unit, onAbonar: () -> Unit, onEditarAbono: (AbonoEntity, Double) -> Unit, onTogglePausa: () -> Unit) {
    var mostrarConfirmacionBorrar by remember { mutableStateOf(false) }
    var abonoParaEditar by remember { mutableStateOf<AbonoEntity?>(null) }
    val colorMeta = try { Color(android.graphics.Color.parseColor(meta.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
    val colorTexto = MaterialTheme.colorScheme.onSurface
    val diasRestantesTexto = remember(meta.fechaLimite) {
        if (meta.fechaLimite != null && meta.fechaLimite > 0) {
            val hoy = System.currentTimeMillis(); val diferencia = meta.fechaLimite - hoy
            if (diferencia > 0) "${java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diferencia)} días restantes" else "Fecha límite vencida"
        } else null
    }
    if (abonoParaEditar != null) { DialogoEditarAbono(abono = abonoParaEditar!!, onDismiss = { abonoParaEditar = null }, onConfirm = { nuevoMonto -> onEditarAbono(abonoParaEditar!!, nuevoMonto); abonoParaEditar = null }) }
    if (mostrarConfirmacionBorrar) {
        AlertDialog(onDismissRequest = { mostrarConfirmacionBorrar = false }, title = { Text("¿Eliminar Meta?") }, text = { Text("Se perderá todo el historial.") }, confirmButton = { Button(onClick = onEliminar, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Eliminar") } }, dismissButton = { TextButton(onClick = { mostrarConfirmacionBorrar = false }) { Text("Cancelar") } })
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(getIconoByName(meta.icono), null, tint = colorMeta, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(meta.nombre, color = colorTexto, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            if (meta.esPausada) Text("(Pausada)", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Objetivo: ${CurrencyUtils.formatCurrency(meta.montoObjetivo)}", color = colorTexto)
                    Text("Ahorrado: ${CurrencyUtils.formatCurrency(meta.montoAhorrado)}", color = colorMeta, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    if (diasRestantesTexto != null) Text(diasRestantesTexto, color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
                    if (meta.nota.isNotEmpty()) Text(meta.nota, color = MaterialTheme.colorScheme.outline, fontSize = 14.sp, maxLines = 3)
                    Spacer(Modifier.height(16.dp)); Divider(); Spacer(Modifier.height(16.dp))
                    Text("Historial", color = colorTexto, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    if (historialAbonos.isEmpty()) Text("Sin abonos", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                    else { historialAbonos.take(3).forEach { abono -> ItemHistorialAbono(abono = abono, onEditClick = { abonoParaEditar = abono }) } }
                    Spacer(Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onTogglePausa) { Icon(imageVector = if (meta.esPausada) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                        Row { IconButton(onClick = onEditar) { Icon(Icons.Default.Edit, null) }; IconButton(onClick = { mostrarConfirmacionBorrar = true }) { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }; Button(onClick = onAbonar, colors = ButtonDefaults.buttonColors(containerColor = colorMeta)) { Text("Abonar") } }
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cerrar") }
                }
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
    val datePickerDialog = DatePickerDialog(
        context,
        { _, y, m, d ->
            val cal = Calendar.getInstance(); cal.set(y, m, d); fechaSeleccionada = cal.time
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    var menuCuentasExpanded by remember { mutableStateOf(false) }
    val colorFondo = MaterialTheme.colorScheme.background
    val colorSurface = MaterialTheme.colorScheme.surfaceVariant
    val colorPrimario = MaterialTheme.colorScheme.primary

    Scaffold(modifier = Modifier.imePadding(), containerColor = colorFondo, topBar = { CenterAlignedTopAppBar(title = { Text(if (metaExistente == null) "Agregar Meta" else "Editar Meta", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } }, colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colorFondo)) }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(24.dp))
            val colorPreview = try { Color(android.graphics.Color.parseColor(colorSeleccionado)) } catch (e: Exception) { colorPrimario }
            Box(modifier = Modifier.size(80.dp).border(2.dp, colorPreview.copy(alpha = 0.5f), CircleShape).padding(4.dp).clip(CircleShape).background(Color.Transparent), contentAlignment = Alignment.Center) { Icon(getIconoByName(iconoSeleccionado), null, tint = colorPreview, modifier = Modifier.size(40.dp)) }
            Spacer(modifier = Modifier.height(12.dp))
            Text("Personaliza tu meta", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(20.dp))
            InputLabel("Icono")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) { items(iconosDisponibles.map { it to getIconoByName(it) }) { (nombre, vector) -> val isSelected = nombre == iconoSeleccionado; Box(modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(if (isSelected) colorPreview else colorSurface).clickable { iconoSeleccionado = nombre }, contentAlignment = Alignment.Center) { Icon(imageVector = vector, contentDescription = null, tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant) } } }
            Spacer(modifier = Modifier.height(20.dp))
            InputLabel("Color")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) { items(listaColores) { colorHex -> val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color.Gray }; val isSelected = colorSeleccionado == colorHex; Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(color).border(if (isSelected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape).clickable { colorSeleccionado = colorHex }) } }
            Spacer(modifier = Modifier.height(32.dp))
            InputLabel("Nombre de la Meta")
            CampoTextoTematico(value = nombre, onValueChange = { nombre = it }, placeholder = "Ej. Viaje")
            Spacer(modifier = Modifier.height(20.dp))
            InputLabel("Monto Objetivo")
            CampoTextoTematico(value = objetivo, onValueChange = { objetivo = it }, placeholder = "$ 0.00", keyboardType = KeyboardType.Number)
            Spacer(modifier = Modifier.height(20.dp))
            InputLabel("Cuenta Asociada")
            Box(modifier = Modifier.fillMaxWidth()) { val nombreCuenta = cuentas.find { it.id == cuentaSeleccionadaId }?.nombre ?: "Seleccionar Cuenta"; Row(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(colorSurface).clickable { menuCuentasExpanded = true }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text(nombreCuenta, color = MaterialTheme.colorScheme.onSurfaceVariant); Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }; DropdownMenu(expanded = menuCuentasExpanded, onDismissRequest = { menuCuentasExpanded = false }) { cuentas.forEach { c -> DropdownMenuItem(text = { Text(c.nombre) }, onClick = { cuentaSeleccionadaId = c.id; menuCuentasExpanded = false }) } } }
            Spacer(modifier = Modifier.height(20.dp))
            InputLabel("Fecha Límite")
            Row(modifier = Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(12.dp)).background(colorSurface).clickable { datePickerDialog.show() }.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { val textoFecha = if(fechaSeleccionada != null) DateUtils.formatearFechaAmigable(fechaSeleccionada!!) else "Seleccionar fecha"; val textColor = if(fechaSeleccionada != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f); Text(textoFecha, color = textColor); Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            Spacer(modifier = Modifier.height(20.dp))
            InputLabel("Notas")
            CampoTextoTematico(value = nota, onValueChange = { nota = it }, placeholder = "Descripción...", singleLine = false, modifier = Modifier.height(80.dp))
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { val obj = objetivo.toDoubleOrNull() ?: 0.0; if (nombre.isNotEmpty() && obj > 0) onConfirm(nombre, obj, iconoSeleccionado, colorSeleccionado, fechaSeleccionada, cuentaSeleccionadaId, nota) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = colorPreview, contentColor = Color.White)) { Text(if (metaExistente == null) "Crear Meta" else "Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DialogoAbonar(meta: MetaEntity, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var monto by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Abonar a ${meta.nombre}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                InputLabel("Cantidad")
                CampoTextoTematico(value = monto, onValueChange = { monto = it }, placeholder = "$ 0.00", keyboardType = KeyboardType.Number)
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { val m = monto.toDoubleOrNull(); if(m!=null && m>0) onConfirm(m) }) { Text("Abonar") }
                }
            }
        }
    }
}

@Composable
fun ItemHistorialAbono(abono: AbonoEntity, onEditClick: () -> Unit) { Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(CurrencyUtils.formatCurrency(abono.monto)); IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)) } } }

@Composable
fun DialogoEditarAbono(abono: AbonoEntity, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) { var montoStr by remember { mutableStateOf(abono.monto.toString()) }; AlertDialog(onDismissRequest = onDismiss, title = { Text("Editar Abono") }, text = { OutlinedTextField(value = montoStr, onValueChange = { montoStr = it }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) }, confirmButton = { Button(onClick = { val m = montoStr.toDoubleOrNull(); if (m != null) onConfirm(m) }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }) }

@Composable
fun CampoTextoTematico(value: String, onValueChange: (String) -> Unit, placeholder: String, keyboardType: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, modifier: Modifier = Modifier) { val colorCursor = MaterialTheme.colorScheme.primary; val colorTexto = MaterialTheme.colorScheme.onSurfaceVariant; val colorFondo = MaterialTheme.colorScheme.surfaceVariant; BasicTextField(value = value, onValueChange = onValueChange, textStyle = androidx.compose.ui.text.TextStyle(color = colorTexto, fontSize = 16.sp), keyboardOptions = KeyboardOptions(keyboardType = keyboardType), singleLine = singleLine, cursorBrush = SolidColor(colorCursor), modifier = modifier.fillMaxWidth(), decorationBox = { innerTextField -> Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(colorFondo).padding(horizontal = 16.dp, vertical = 16.dp)) { if (value.isEmpty()) Text(placeholder, color = colorTexto.copy(alpha = 0.5f)); innerTextField() } }) }

@Composable
fun InputLabel(text: String) { Text(text = text, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp).fillMaxWidth()) }

fun getIconoByName(nombre: String): ImageVector { return when (nombre) { "DirectionsCar" -> Icons.Default.DirectionsCar; "TwoWheeler" -> Icons.Default.TwoWheeler; "Home" -> Icons.Default.Home; "Flight" -> Icons.Default.Flight; "Smartphone" -> Icons.Default.Smartphone; "Computer" -> Icons.Default.Computer; "School" -> Icons.Default.School; "Pets" -> Icons.Default.Pets; "ShoppingBag" -> Icons.Default.ShoppingBag; else -> Icons.Default.Savings } }