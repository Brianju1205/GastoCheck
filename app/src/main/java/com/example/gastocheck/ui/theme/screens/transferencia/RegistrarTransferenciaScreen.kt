package com.example.gastocheck.ui.theme.screens.transferencia

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.gastocheck.ui.theme.components.CampoMontoOriginal
import com.example.gastocheck.ui.theme.components.DialogoProcesando
import com.example.gastocheck.ui.theme.screens.home.DialogoEscuchandoAnimado
import com.example.gastocheck.ui.theme.screens.transferencia.TransferenciaViewModel.EstadoVoz
import com.example.gastocheck.ui.theme.util.DateUtils
import com.example.gastocheck.ui.theme.util.ImageUtils
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarTransferenciaScreen(
    idTransaccion: Int = -1,
    textoInicial: String? = null,
    viewModel: TransferenciaViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // --- VARIABLES DE ESTADO ---
    // Usamos la lista de fotos del ViewModel
    val fotos by viewModel.fotos.collectAsState()

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var fotoParaVerGrande by remember { mutableStateOf<String?>(null) } // Estado para Zoom

    // 1. Permiso Voz
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) viewModel.iniciarEscuchaInteligente()
        else Toast.makeText(context, "Se requiere permiso para usar voz", Toast.LENGTH_SHORT).show()
    }

    // 2. Launchers Cámara y Galería
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUri != null) {
            viewModel.onFotoCapturada(tempPhotoUri.toString())
        }
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
    val photoPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.onFotoCapturada(uri.toString())
    }

    LaunchedEffect(Unit) { viewModel.uiEvent.collect { mensaje -> Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show() } }
    LaunchedEffect(idTransaccion) { viewModel.inicializar(idTransaccion) }
    LaunchedEffect(textoInicial) { if (!textoInicial.isNullOrBlank()) viewModel.procesarTextoExterno(textoInicial) }

    val cuentas by viewModel.cuentas.collectAsState()
    val origenId by viewModel.origenId.collectAsState()
    val destinoId by viewModel.destinoId.collectAsState()
    val monto by viewModel.monto.collectAsState()
    val nota by viewModel.nota.collectAsState()
    val fecha by viewModel.fecha.collectAsState()
    val estadoVoz by viewModel.estadoVoz.collectAsState()

    var monedaLocal by remember { mutableStateOf("MXN") }

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val transferColor = MaterialTheme.colorScheme.primary

    var menuOrigenExpanded by remember { mutableStateOf(false) }
    var menuDestinoExpanded by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    calendar.time = fecha
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, day: Int ->
            val cal = Calendar.getInstance()
            cal.set(year, month, day)
            viewModel.onFechaChange(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    LaunchedEffect(cuentas) {
        if (idTransaccion == -1 && textoInicial.isNullOrBlank()) {
            if (viewModel.origenId.value == -1 && cuentas.isNotEmpty()) viewModel.setOrigen(cuentas.first().id)
            if (viewModel.destinoId.value == -1 && cuentas.size > 1) viewModel.setDestino(cuentas[1].id)
        }
    }

    when (estadoVoz) {
        is EstadoVoz.Escuchando -> DialogoEscuchandoAnimado(onDismiss = { viewModel.reiniciarEstadoVoz() })
        is EstadoVoz.ProcesandoIA -> DialogoProcesando()
        else -> {}
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = if (idTransaccion != -1) "Editar Transferencia" else "Registrar Transferencia", fontWeight = FontWeight.Bold, color = onBackgroundColor) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.Close, null, tint = onBackgroundColor) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = backgroundColor)
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

            Text("Cuenta Origen", color = onSurfaceVariantColor, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            CuentaSelector(nombre = cuentas.find { it.id == origenId }?.nombre ?: "Seleccionar", onClick = { menuOrigenExpanded = true })
            DropdownMenu(expanded = menuOrigenExpanded, onDismissRequest = { menuOrigenExpanded = false }) {
                cuentas.forEach { c -> DropdownMenuItem(text = { Text(c.nombre) }, onClick = { viewModel.setOrigen(c.id); menuOrigenExpanded = false }) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Cuenta Destino", color = onSurfaceVariantColor, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            CuentaSelector(nombre = cuentas.find { it.id == destinoId }?.nombre ?: "Seleccionar", onClick = { menuDestinoExpanded = true })
            DropdownMenu(expanded = menuDestinoExpanded, onDismissRequest = { menuDestinoExpanded = false }) {
                cuentas.forEach { c -> DropdownMenuItem(text = { Text(c.nombre) }, onClick = { viewModel.setDestino(c.id); menuDestinoExpanded = false }) }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Monto", color = onSurfaceVariantColor)
            CampoMontoOriginal(
                valor = monto,
                onValorChange = { viewModel.onMontoChange(it) },
                monedaSeleccionada = monedaLocal,
                onMonedaChange = { monedaLocal = it },
                colorTexto = transferColor
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(colors = CardDefaults.cardColors(containerColor = surfaceVariantColor), shape = RoundedCornerShape(12.dp)) {
                Column {
                    // CAMPO NOTA
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Edit, null, tint = primaryColor, modifier = Modifier.padding(top = 2.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (nota.isEmpty()) Text("Añadir nota (opcional)", color = onSurfaceVariantColor.copy(alpha = 0.7f))

                            BasicTextField(
                                value = nota,
                                onValueChange = { viewModel.onNotaChange(it) },
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                                cursorBrush = SolidColor(primaryColor),
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Default
                                ),
                                maxLines = 4
                            )
                        }
                        IconButton(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Mic, contentDescription = "Dictar", tint = primaryColor)
                        }
                    }
                    Divider(color = backgroundColor, thickness = 1.dp)

                    // --- SECCIÓN DE FOTOS (CARRUSEL MINIATURA) ---
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Comprobantes (${fotos.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ListaFotosMiniaturaT(
                            fotos = fotos,
                            onEliminar = { viewModel.eliminarFoto(it) },
                            onVerFoto = { fotoParaVerGrande = it },
                            onAgregarMas = { showBottomSheet = true }
                        )
                    }

                    Divider(color = backgroundColor, thickness = 1.dp)

                    // CAMPO FECHA
                    Row(modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, tint = primaryColor)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(DateUtils.formatearFechaAmigable(fecha), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = onSurfaceVariantColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.realizarTransferencia(monedaLocal) { onBack() } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text(if (idTransaccion != -1) "Guardar Cambios" else "Realizar Transferencia", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // --- DIÁLOGO VER FOTO GRANDE ---
        if (fotoParaVerGrande != null) {
            DialogoVerComprobanteT(fotoUri = fotoParaVerGrande!!, onDismiss = { fotoParaVerGrande = null })
        }

        // --- BOTTOM SHEET TRANSFERENCIA ---
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
    }
}

@Composable
fun CuentaSelector(nombre: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(nombre, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Icon(Icons.Default.UnfoldMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// -------------------------------------------------------------
// COMPONENTES AUXILIARES (Copiados para integridad)
// -------------------------------------------------------------

@Composable
fun ListaFotosMiniaturaT(
    fotos: List<String>,
    onEliminar: (String) -> Unit,
    onVerFoto: (String) -> Unit,
    onAgregarMas: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón agregar
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

        items(fotos) { rutaFoto ->
            Box(modifier = Modifier.size(80.dp)) {
                AsyncImage(
                    model = File(rutaFoto),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onVerFoto(rutaFoto) },
                    contentScale = ContentScale.Crop
                )

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
            }
        }
    }
}

@Composable
fun DialogoVerComprobanteT(fotoUri: String, onDismiss: () -> Unit) {
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
                contentDescription = "Comprobante",
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