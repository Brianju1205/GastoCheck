package com.example.gastocheck.ui.theme.screens.agregar

import android.Manifest
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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

    val actionColor = if (esIngreso) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }

    // Aseguramos que al entrar se limpie si es necesario o se inicialice
    // Nota: viewModel.inicializar() debería llamarse idealmente desde el NavigationGraph,
    // pero si navegas hacia atrás y vuelves a entrar, el ViewModel se mantiene vivo.
    // Como añadimos limpieza al guardar, aquí solo observamos.

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
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
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
            BasicTextField(
                value = monto,
                onValueChange = { viewModel.onMontoChange(it) },
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground, fontSize = 56.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(actionColor),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.Center) {
                        if (monto.isEmpty()) Text("$ 0.00", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), fontSize = 56.sp, fontWeight = FontWeight.Bold)
                        innerTextField()
                    }
                }
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 3. DATOS
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItemConfig(icon = CategoriaUtils.getIcono(categoria), iconTint = try { CategoriaUtils.getColor(categoria) } catch (e: Exception) { MaterialTheme.colorScheme.primary }, label = "Categoría", value = categoria, onClick = { showCategorySheet = true })
                    Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)

                    Row(modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Nota", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)

                            // CAMPO NOTA: La lógica inteligente ahora vive en el ViewModel
                            BasicTextField(
                                value = descripcion,
                                onValueChange = { viewModel.onDescripcionChange(it) },
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                                cursorBrush = SolidColor(actionColor),
                                decorationBox = { inner ->
                                    if(descripcion.isEmpty()) Text("Añadir nota (opcional)", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 16.sp)
                                    inner()
                                }
                            )
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)

                    ListItemConfig(icon = Icons.Default.CalendarToday, iconTint = actionColor, label = "Fecha", value = DateUtils.formatearFechaAmigable(fecha), onClick = { datePickerDialog.show() })
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 4. FAB MICROFONO Y BOTON GUARDAR
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SmallFloatingActionButton(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }, containerColor = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.padding(end = 16.dp)) {
                    Icon(Icons.Default.Mic, contentDescription = "Dictar")
                }

                Button(
                    onClick = { viewModel.guardarTransaccion { alRegresar() } },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = actionColor, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Guardar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showCategorySheet) {
        ModalBottomSheet(onDismissRequest = { showCategorySheet = false }, containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Seleccionar Categoría", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 80.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(CategoriaUtils.listaCategorias) { cat ->
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