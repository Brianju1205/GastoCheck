package com.example.gastocheck.ui.theme.screens.cuentas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import com.example.gastocheck.ui.theme.util.IconoUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearCuentaScreen(
    idCuenta: Int = -1,
    onBack: () -> Unit,
    viewModel: CrearCuentaViewModel = hiltViewModel()
) {
    LaunchedEffect(idCuenta) { viewModel.inicializar(idCuenta) }

    val nombre by viewModel.nombre.collectAsState()
    val saldo by viewModel.saldo.collectAsState() // Representa el Límite en Crédito
    val deudaInicial by viewModel.deudaInicial.collectAsState()
    val tipoSeleccionado by viewModel.tipo.collectAsState()
    val colorSeleccionado by viewModel.colorSeleccionado.collectAsState()
    val iconoSeleccionado by viewModel.iconoSeleccionado.collectAsState()

    val esCredito by viewModel.esCredito.collectAsState()
    val diaCorte by viewModel.diaCorte.collectAsState()
    val diaPago by viewModel.diaPago.collectAsState()
    val tasaInteres by viewModel.tasaInteres.collectAsState()
    val crearRecordatorios by viewModel.crearRecordatorios.collectAsState()

    var showDatePickerCorte by remember { mutableStateOf(false) }
    var showDatePickerPago by remember { mutableStateOf(false) }
    val datePickerStateCorte = rememberDatePickerState()
    val datePickerStatePago = rememberDatePickerState()

    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    var expandedTipo by remember { mutableStateOf(false) }

    if (showDatePickerCorte) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerCorte = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDiaCorteSelected(datePickerStateCorte.selectedDateMillis)
                    showDatePickerCorte = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePickerCorte = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerStateCorte) }
    }

    if (showDatePickerPago) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerPago = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDiaPagoSelected(datePickerStatePago.selectedDateMillis)
                    showDatePickerPago = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePickerPago = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerStatePago) }
    }

    Scaffold(
        containerColor = background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if(idCuenta == -1) "Nueva Cuenta" else "Editar Cuenta", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null, tint = onSurface) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // TABS
            Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(surface).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TabButton(text = "Efectivo / Débito", selected = !esCredito, modifier = Modifier.weight(1f), onClick = { viewModel.onEsCreditoChange(false) })
                TabButton(text = "Crédito", selected = esCredito, modifier = Modifier.weight(1f), onClick = { viewModel.onEsCreditoChange(true) })
            }

            // VISTA PREVIA
            val colorTarjeta = try { Color(android.graphics.Color.parseColor(colorSeleccionado)) } catch (e: Exception) { primary }
            Card(modifier = Modifier.fillMaxWidth().height(160.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = colorTarjeta)) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text(text = if (esCredito) "CRÉDITO" else tipoSeleccionado.uppercase(), color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.TopStart))
                    Icon(imageVector = IconoUtils.getIconoByName(iconoSeleccionado), contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.TopEnd).size(48.dp))

                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        Text(text = if (nombre.isEmpty()) "Nombre" else nombre, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))

                        // --- LÓGICA VISUAL CORREGIDA ---
                        val limite = saldo.toDoubleOrNull() ?: 0.0
                        val deuda = deudaInicial.toDoubleOrNull() ?: 0.0

                        // Si es crédito: Disponible = Límite - Deuda
                        // Si es débito: Saldo = Lo que escribe el usuario
                        val mostrarMonto = if(esCredito) limite - deuda else limite
                        val etiqueta = if (esCredito) "Saldo disponible (Estimado)" else "Saldo Actual"

                        Text(text = etiqueta, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        Text(text = CurrencyUtils.formatCurrency(mostrarMonto), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // NOMBRE
            CampoTexto(label = "Nombre", value = nombre, onValueChange = { viewModel.onNombreChange(it) }, placeholder = if(esCredito) "Ej: Visa Oro" else "Ej: Nómina")

            // MONTO PRINCIPAL (LÍMITE O SALDO)
            CampoTexto(
                label = if (esCredito) "Límite de Crédito" else "Saldo Actual",
                value = saldo,
                onValueChange = { viewModel.onSaldoChange(it) },
                placeholder = "$ 0.00",
                keyboardType = KeyboardType.Number
            )

            // TIPO (Solo si no es crédito)
            AnimatedVisibility(visible = !esCredito) {
                Column {
                    Text("Tipo de Cuenta", color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = tipoSeleccionado, onValueChange = {}, readOnly = true,
                            modifier = Modifier.fillMaxWidth(), trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = onSurface) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = surface, unfocusedContainerColor = surface, focusedBorderColor = primary, unfocusedBorderColor = Color.Transparent)
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { expandedTipo = true })
                        DropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }, modifier = Modifier.background(surface)) {
                            viewModel.listaTipos.forEach { tipo ->
                                DropdownMenuItem(text = { Text(tipo, color = onSurface) }, onClick = { viewModel.onTipoChange(tipo); expandedTipo = false })
                            }
                        }
                    }
                }
            }

            // CAMPOS CRÉDITO
            AnimatedVisibility(visible = esCredito) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {

                    // DEUDA ACTUAL (Siempre visible)
                    CampoTexto(
                        label = "Deuda Actual",
                        value = deudaInicial,
                        onValueChange = { viewModel.onDeudaChange(it) },
                        placeholder = "$ 0.00",
                        keyboardType = KeyboardType.Number
                    )

                    if (idCuenta != -1) {
                        Text("Al editar: Si cambias este valor, se creará un ajuste de saldo automáticamente.", style = MaterialTheme.typography.bodySmall, color = onSurface.copy(alpha = 0.6f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Día de Corte", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            CampoFecha(value = diaCorte, placeholder = "Seleccionar", onClick = { showDatePickerCorte = true })
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Día de Pago", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            CampoFecha(value = diaPago, placeholder = "Seleccionar", onClick = { showDatePickerPago = true })
                        }
                    }

                    CampoTexto(
                        label = "Tasa de Interés (Opcional)",
                        value = tasaInteres,
                        onValueChange = { viewModel.onTasaInteresChange(it) },
                        placeholder = "Ej: 45.5",
                        suffix = "%",
                        keyboardType = KeyboardType.Number
                    )

                    Row(modifier = Modifier.fillMaxWidth().border(1.dp, outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recordatorios de Pago", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Avisar 1 día antes del corte y pago", fontSize = 12.sp, color = onSurface.copy(alpha = 0.6f))
                        }
                        Switch(checked = crearRecordatorios, onCheckedChange = { viewModel.onRecordatoriosChange(it) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = primary))
                    }
                }
            }

            // COLOR Y ICONO
            Column {
                Text("Color", fontWeight = FontWeight.Bold, fontSize = 14.sp); Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(viewModel.listaColores) { colorHex ->
                        val isSelected = colorSeleccionado == colorHex
                        val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch(e:Exception){ Color.Gray }
                        Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(color).border(width = if (isSelected) 3.dp else 0.dp, color = onSurface, shape = CircleShape).clickable { viewModel.onColorChange(colorHex) }, contentAlignment = Alignment.Center) { if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White) }
                    }
                }
            }

            Column {
                Text("Icono", fontWeight = FontWeight.Bold, fontSize = 14.sp); Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.height(200.dp)) {
                    LazyVerticalGrid(columns = GridCells.Fixed(5), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(viewModel.listaIconos) { iconName ->
                            val isSelected = iconoSeleccionado == iconName
                            val primaryColor = try { Color(android.graphics.Color.parseColor(colorSeleccionado)) } catch(e:Exception){ primary }
                            Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)).background(surface).border(width = if (isSelected) 2.dp else 0.dp, color = if (isSelected) primaryColor else Color.Transparent, shape = RoundedCornerShape(12.dp)).clickable { viewModel.onIconoChange(iconName) }, contentAlignment = Alignment.Center) { Icon(imageVector = IconoUtils.getIconoByName(iconName), contentDescription = null, tint = if (isSelected) primaryColor else outline) }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.guardarCuenta { onBack() } }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                Text(if(idCuenta == -1) "Guardar Cuenta" else "Guardar Cambios", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TabButton(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(10.dp)).background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent).clickable(onClick = onClick).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
        Text(text = text, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
@Composable
fun CampoTexto(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String, suffix: String? = null, keyboardType: KeyboardType = KeyboardType.Text) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.outline) },
            suffix = if (suffix != null) { { Text(suffix) } } else null,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color.Transparent)
        )
    }
}
@Composable
fun CampoFecha(value: String, placeholder: String, onClick: () -> Unit) {
    OutlinedTextField(
        value = if(value.isNotEmpty()) "Día $value" else "",
        onValueChange = {},
        placeholder = { Text(placeholder) },
        readOnly = true,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }, // Click en todo el campo
        enabled = false, // Deshabilitamos input manual pero habilitamos click con interactionSource
        shape = RoundedCornerShape(12.dp),
        trailingIcon = { Icon(Icons.Default.CalendarToday, null) },
        colors = OutlinedTextFieldDefaults.colors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = Color.Transparent,
            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface
        ),
        interactionSource = remember { MutableInteractionSource() }
            .also { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect {
                        if (it is PressInteraction.Release) {
                            onClick()
                        }
                    }
                }
            }
    )
}