package com.example.gastocheck.ui.theme.screens.agregar

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.gastocheck.ui.theme.components.CampoMontoOriginal
import com.example.gastocheck.ui.theme.util.CategoriaUtils
import com.example.gastocheck.ui.theme.util.DateUtils
import com.example.gastocheck.ui.theme.util.ImageUtils
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarScreen(
    viewModel: AgregarViewModel = hiltViewModel(),
    alRegresar: () -> Unit
) {
    // --- ESTADOS DE DATOS ---
    val monto by viewModel.monto.collectAsState()
    val descripcion by viewModel.descripcion.collectAsState()
    val categoria by viewModel.categoria.collectAsState()
    val fecha by viewModel.fecha.collectAsState()
    val esIngreso by viewModel.esIngreso.collectAsState()
    val cuentaId by viewModel.cuentaIdSeleccionada.collectAsState()
    val cuentas by viewModel.cuentas.collectAsState()

    // --- ESTADOS DE FOTOS (LISTA) ---
    val fotos by viewModel.fotos.collectAsState()
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempScanUri by remember { mutableStateOf<Uri?>(null) }

    // Estado para ver foto en grande
    var fotoParaVerGrande by remember { mutableStateOf<String?>(null) }

    // Control de Menús
    var showBottomSheet by remember { mutableStateOf(false) }
    var showScanSourceDialog by remember { mutableStateOf(false) }

    var monedaLocal by remember { mutableStateOf("MXN") }
    val actionColor = if (esIngreso) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    var showCategorySheet by remember { mutableStateOf(false) }
    var showAccountMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // ----------------------------------------------------------------
    // 1. LAUNCHERS PARA "ADJUNTAR MANUALMENTE"
    // ----------------------------------------------------------------
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUri != null) viewModel.onFotoCapturada(tempPhotoUri.toString())
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onFotoCapturada(uri.toString())
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val uri = ImageUtils.crearUriParaFoto(context)
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Se requiere permiso para tomar fotos", Toast.LENGTH_SHORT).show()
        }
    }

    // ----------------------------------------------------------------
    // 2. LAUNCHERS PARA "ESCANEAR RECIBO" (OCR + IA + SUMA)
    // ----------------------------------------------------------------
    val scannerCameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempScanUri != null) viewModel.escanearRecibo(tempScanUri!!)
    }

    val scannerPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.escanearRecibo(uri)
    }

    val scannerPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val uri = ImageUtils.crearUriParaFoto(context)
            tempScanUri = uri
            scannerCameraLauncher.launch(uri)
        }
    }

    // --- DATE PICKER ---
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
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (esIngreso) "Registrar Ingreso" else "Registrar Gasto",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = alRegresar) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                // BOTÓN DE ESCANEAR (BARRA SUPERIOR)
                actions = {
                    IconButton(onClick = { showScanSourceDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Escanear recibo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ... (Selector Cuenta) ...
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

            // MONTO
            Text("Monto", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            CampoMontoOriginal(
                valor = monto,
                onValorChange = { viewModel.onMontoChange(it) },
                monedaSeleccionada = monedaLocal,
                onMonedaChange = { monedaLocal = it },
                colorTexto = actionColor
            )

            Spacer(modifier = Modifier.height(32.dp))

            // TARJETA DE DATOS
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column {
                    // CATEGORÍA
                    ListItemConfig(icon = CategoriaUtils.getIcono(categoria), iconTint = try { CategoriaUtils.getColor(categoria) } catch (e: Exception) { MaterialTheme.colorScheme.primary }, label = "Categoría", value = categoria, onClick = { showCategorySheet = true })

                    Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)

                    // NOTA
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

                    // --- SECCIÓN DE FOTOS (TIRA MINIATURA) ---
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Comprobantes (${fotos.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ListaFotosMiniatura(
                            fotos = fotos,
                            onEliminar = { viewModel.eliminarFoto(it) },
                            onVerFoto = { fotoParaVerGrande = it },
                            onAgregarMas = { showBottomSheet = true }
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.background, thickness = 1.dp)

                    // FECHA
                    ListItemConfig(icon = Icons.Default.CalendarToday, iconTint = actionColor, label = "Fecha", value = DateUtils.formatearFechaAmigable(fecha), onClick = { datePickerDialog.show() })
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // BOTÓN GUARDAR
            Button(
                onClick = { viewModel.guardarTransaccion(monedaLocal) { alRegresar() } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = actionColor)
            ) {
                Text("Guardar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- DIÁLOGO VER FOTO GRANDE ---
        if (fotoParaVerGrande != null) {
            DialogoVerComprobante(fotoUri = fotoParaVerGrande!!, onDismiss = { fotoParaVerGrande = null })
        }

        // --- BOTTOM SHEET (ADJUNTAR MANUAL) ---
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(bottom = 32.dp)) {
                    Text(
                        "Adjuntar comprobante",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
                    )

                    // 1. SUBIR ARCHIVO
                    ListItem(
                        headlineContent = { Text("Subir archivo") },
                        leadingContent = { Icon(Icons.Default.UploadFile, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            showBottomSheet = false
                            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    )

                    // 2. TOMAR FOTO
                    ListItem(
                        headlineContent = { Text("Tomar foto") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.clickable {
                            showBottomSheet = false
                            val permission = Manifest.permission.CAMERA
                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                val uri = ImageUtils.crearUriParaFoto(context)
                                tempPhotoUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(permission)
                            }
                        }
                    )
                }
            }
        }

        // --- DIÁLOGO ESCANEAR RECIBO (Fuente) ---
        if (showScanSourceDialog) {
            AlertDialog(
                onDismissRequest = { showScanSourceDialog = false },
                icon = { Icon(Icons.Default.DocumentScanner, null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Escanear recibo con IA") },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Elige el origen de la imagen para extraer los datos:")
                        Spacer(modifier = Modifier.height(8.dp))

                        // Botón CÁMARA
                        OutlinedButton(
                            onClick = {
                                showScanSourceDialog = false
                                val permission = Manifest.permission.CAMERA
                                if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                    val uri = ImageUtils.crearUriParaFoto(context)
                                    tempScanUri = uri
                                    scannerCameraLauncher.launch(uri)
                                } else {
                                    scannerPermissionLauncher.launch(permission)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CameraAlt, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Usar Cámara")
                        }

                        // Botón GALERÍA
                        OutlinedButton(
                            onClick = {
                                showScanSourceDialog = false
                                scannerPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PhotoLibrary, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Elegir de Galería")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showScanSourceDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }

    // ... (Categoría Sheet igual) ...
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

// -------------------------------------------------------------
// COMPONENTES AUXILIARES
// -------------------------------------------------------------

@Composable
fun ListaFotosMiniatura(
    fotos: List<String>,
    onEliminar: (String) -> Unit,
    onVerFoto: (String) -> Unit,
    onAgregarMas: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp), // Altura contenida
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón para agregar más
        item {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onAgregarMas() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary)
                    Text("Adjuntar", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Lista de fotos
        items(fotos) { rutaFoto ->
            Box(modifier = Modifier.size(80.dp)) {
                // Imagen recortada
                AsyncImage(
                    model = File(rutaFoto), // Carga desde File (Almacenamiento interno)
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onVerFoto(rutaFoto) },
                    contentScale = ContentScale.Crop
                )

                // Botón X para eliminar
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.Red.copy(alpha = 0.8f))
                        .clickable { onEliminar(rutaFoto) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }

                // Icono Zoom (Visual)
                Icon(
                    Icons.Default.ZoomIn,
                    null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .size(16.dp)
                        .background(Color.Black.copy(alpha=0.3f), CircleShape)
                )
            }
        }
    }
}

@Composable
fun DialogoVerComprobante(fotoUri: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = File(fotoUri),
                contentDescription = "Comprobante Full Screen",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White)
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