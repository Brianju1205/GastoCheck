package com.example.gastocheck.ui.theme.screens.transferencia

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.ui.theme.util.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarTransferenciaScreen(
    viewModel: TransferenciaViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val cuentas by viewModel.cuentas.collectAsState()
    val origenId by viewModel.origenId.collectAsState()
    val destinoId by viewModel.destinoId.collectAsState()
    val monto by viewModel.monto.collectAsState()
    val nota by viewModel.nota.collectAsState()
    val fecha by viewModel.fecha.collectAsState()

    // --- COLORES DEL TEMA (Dinámicos) ---
    // Usamos los definidos en Theme.kt para que cambien solos
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val placeholderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)

    // Control de menús
    var menuOrigenExpanded by remember { mutableStateOf(false) }
    var menuDestinoExpanded by remember { mutableStateOf(false) }

    // Configurar fechas
    val context = LocalContext.current
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

    // Inicializar ids por defecto
    LaunchedEffect(cuentas) {
        if (origenId == -1 && cuentas.isNotEmpty()) viewModel.setOrigen(cuentas.first().id)
        if (destinoId == -1 && cuentas.size > 1) viewModel.setDestino(cuentas[1].id)
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Registrar Transferencia",
                        fontWeight = FontWeight.Bold,
                        color = onBackgroundColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, null, tint = onBackgroundColor)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- SELECTORES DE CUENTA ---
            Text(
                "Cuenta Origen",
                color = onSurfaceVariantColor,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            CuentaSelector(
                nombre = cuentas.find { it.id == origenId }?.nombre ?: "Seleccionar",
                onClick = { menuOrigenExpanded = true }
            )
            DropdownMenu(expanded = menuOrigenExpanded, onDismissRequest = { menuOrigenExpanded = false }) {
                cuentas.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.nombre) },
                        onClick = { viewModel.setOrigen(c.id); menuOrigenExpanded = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Cuenta Destino",
                color = onSurfaceVariantColor,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            CuentaSelector(
                nombre = cuentas.find { it.id == destinoId }?.nombre ?: "Seleccionar",
                onClick = { menuDestinoExpanded = true }
            )
            DropdownMenu(expanded = menuDestinoExpanded, onDismissRequest = { menuDestinoExpanded = false }) {
                cuentas.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c.nombre) },
                        onClick = { viewModel.setDestino(c.id); menuDestinoExpanded = false }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- MONTO ---
            Text("Monto", color = onSurfaceVariantColor)
            BasicTextField(
                value = monto,
                onValueChange = { viewModel.onMontoChange(it) },
                textStyle = TextStyle(
                    color = onBackgroundColor, // Se adapta a blanco/negro
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(primaryColor), // Cursor verde
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (monto.isEmpty()) {
                            Text(
                                "$ 0.00",
                                color = placeholderColor,
                                fontSize = 56.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        inner()
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- EXTRAS (Nota y Fecha) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceVariantColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    // Nota
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, null, tint = primaryColor)
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (nota.isEmpty()) {
                                Text("Añadir nota (opcional)", color = onSurfaceVariantColor.copy(alpha = 0.7f))
                            }
                            BasicTextField(
                                value = nota,
                                onValueChange = { viewModel.onNotaChange(it) },
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                                cursorBrush = SolidColor(primaryColor)
                            )
                        }
                    }
                    Divider(color = backgroundColor, thickness = 1.dp)
                    // Fecha
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { datePickerDialog.show() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, null, tint = primaryColor)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            DateUtils.formatearFechaAmigable(fecha),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ChevronRight, null, tint = onSurfaceVariantColor)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- BOTÓN ---
            Button(
                onClick = { viewModel.realizarTransferencia { onBack() } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                // Usamos onPrimary para el texto (Negro en Dark, Blanco en Light según Theme.kt)
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Realizar Transferencia", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CuentaSelector(nombre: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant, // Gris oscuro en Dark / Gris claro en Light
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                nombre,
                color = MaterialTheme.colorScheme.onSurface, // Se ve bien sobre el Surface
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Icon(
                Icons.Default.UnfoldMore,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}