package com.example.gastocheck.ui.theme.screens.agregar

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.ui.theme.components.CampoMontoOriginal
import com.example.gastocheck.ui.theme.util.CategoriaUtils
import com.example.gastocheck.ui.theme.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarScreen(
    viewModel: AgregarViewModel = hiltViewModel(),
    alRegresar: () -> Unit
) {
    val monto by viewModel.monto.collectAsState()
    val descripcion by viewModel.descripcion.collectAsState()
    val categoria by viewModel.categoria.collectAsState()
    val fecha by viewModel.fecha.collectAsState()
    val esIngreso by viewModel.esIngreso.collectAsState()
    val cuentaId by viewModel.cuentaIdSeleccionada.collectAsState()
    val cuentas by viewModel.cuentas.collectAsState()

    var monedaLocal by remember { mutableStateOf("MXN") }
    val actionColor = if (esIngreso) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) viewModel.iniciarEscuchaInteligente()
    }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    calendar.time = fecha

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            viewModel.onFechaChange(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        modifier = Modifier.imePadding(), // <--- 1. Ajuste automático por teclado
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (esIngreso) "Registrar Ingreso" else "Registrar Gasto", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = alRegresar) { Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onBackground) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // <--- 2. Habilita el scroll
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. SELECTOR DE CUENTA
            Box {
                Surface(
                    onClick = { showAccountMenu = true },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Cuenta", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Text(text = cuentas.find { it.id == cuentaId }?.nombre ?: "Efectivo", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        }
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                DropdownMenu(expanded = showAccountMenu, onDismissRequest = { showAccountMenu = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                    cuentas.forEach { c ->
                        DropdownMenuItem(text = { Text(c.nombre, color = MaterialTheme.colorScheme.onSurface) }, onClick = { viewModel.setCuentaOrigen(c.id); showAccountMenu = false })
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 2. MONTO
            Text("Monto", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            CampoMontoOriginal(
                valor = monto,
                onValorChange = { viewModel.onMontoChange(it) },
                monedaSeleccionada = monedaLocal,
                onMonedaChange = { monedaLocal = it },
                colorTexto = actionColor
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3. DATOS
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItemConfig(icon = CategoriaUtils.getIcono(categoria), iconTint = try { CategoriaUtils.getColor(categoria) } catch (e: Exception) { MaterialTheme.colorScheme.primary }, label = "Categoría", value = categoria, onClick = { showCategorySheet = true })
                    Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)

                    // CAMPO NOTA
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nota", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

                            Box(modifier = Modifier.fillMaxWidth()) {
                                if(descripcion.isEmpty()) {
                                    Text("Añadir nota (opcional)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 16.sp)
                                }
                                BasicTextField(
                                    value = descripcion,
                                    onValueChange = { viewModel.onDescripcionChange(it) },
                                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                                    cursorBrush = SolidColor(actionColor),
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Default
                                    ),
                                    maxLines = 4
                                )
                            }
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)

                    ListItemConfig(icon = Icons.Default.CalendarToday, iconTint = actionColor, label = "Fecha", value = DateUtils.formatearFechaAmigable(fecha), onClick = { datePickerDialog.show() })
                }
            }

            // Cambiamos el Spacer flexible por uno fijo para que funcione el scroll
            Spacer(modifier = Modifier.height(32.dp))

            // 4. BOTÓN GUARDAR
            Button(
                onClick = { viewModel.guardarTransaccion(monedaLocal) { alRegresar() } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = actionColor)
            ) {
                Text("Guardar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // Espacio extra al final para que el teclado no tape el botón
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCategorySheet) {
        ModalBottomSheet(onDismissRequest = { showCategorySheet = false }, containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = if (esIngreso) "Categorías de Ingreso" else "Categorías de Gasto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                val listaMostrar = CategoriaUtils.obtenerCategoriasPorTipo(esIngreso)
                LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 80.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(listaMostrar) { cat ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { viewModel.onCategoriaChange(cat.nombre); showCategorySheet = false }.padding(8.dp)) {
                            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(cat.color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Icon(cat.icono, null, tint = cat.color) }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(cat.nombre, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, maxLines = 1)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ListItemConfig(icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: androidx.compose.ui.graphics.Color, label: String, value: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().height(64.dp).clickable(onClick = onClick).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = iconTint)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}