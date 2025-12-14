package com.example.gastocheck.ui.theme.screens.transferencia

import android.Manifest
import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.example.gastocheck.ui.theme.components.DialogoProcesando
import com.example.gastocheck.ui.theme.screens.home.DialogoEscuchandoAnimado
import com.example.gastocheck.ui.theme.screens.transferencia.TransferenciaViewModel.EstadoVoz
import com.example.gastocheck.ui.theme.util.DateUtils
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
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) viewModel.iniciarEscuchaInteligente()
        else Toast.makeText(context, "Se requiere permiso para usar voz", Toast.LENGTH_SHORT).show()
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
        modifier = Modifier.imePadding(), // <--- 1. Ajuste teclado
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
                .verticalScroll(rememberScrollState()) // <--- 2. Scroll habilitado
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
                    Row(modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, tint = primaryColor)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(DateUtils.formatearFechaAmigable(fecha), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = onSurfaceVariantColor)
                    }
                }
            }

            // Cambiamos Spacer flexible por fijo
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