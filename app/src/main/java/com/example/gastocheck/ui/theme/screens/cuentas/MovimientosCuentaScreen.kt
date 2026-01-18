package com.example.gastocheck.ui.theme.screens.cuentas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
//import androidx.compose.foundation.content.MediaType.Companion.Text
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
//import androidx.compose.ui.autofill.ContentDataType.Companion.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon.Companion.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType.Companion.Text
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.ui.theme.screens.home.DetalleTransaccionDialog
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import com.example.gastocheck.ui.theme.util.DateUtils
import com.example.gastocheck.ui.theme.util.IconoUtils
import com.example.gastocheck.ui.theme.util.ServiceColorUtils
import kotlin.math.sign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientosCuentaScreen(
    accountId: Int,
    viewModel: DetalleCuentaViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onEditarTransaccion: (Int, String?) -> Unit
) {
    // 1. CARGAR DATOS AL ENTRAR
    LaunchedEffect(accountId) {
        viewModel.inicializar(accountId)
    }

    val state by viewModel.uiState.collectAsState()

    // Estados para detalle
    var transaccionSeleccionada by remember { mutableStateOf<TransaccionEntity?>(null) }
    var mostrarDetalleTransaccion by remember { mutableStateOf(false) }
    var mostrarConfirmacionBorrar by remember { mutableStateOf(false) }

    val errorColor = MaterialTheme.colorScheme.error

    // --- DIÁLOGO DE DETALLE ---
    if (mostrarDetalleTransaccion && transaccionSeleccionada != null) {
        val t = transaccionSeleccionada!!
        DetalleTransaccionDialog(
            transaccion = t,
            cuenta = state.cuenta, // Pasamos la cuenta actual para contexto
            onDismiss = { mostrarDetalleTransaccion = false },
            onDelete = { mostrarConfirmacionBorrar = true },
            onEdit = {
                mostrarDetalleTransaccion = false
                val tipoEdicion = if (t.categoria == "Transferencia" || t.categoria == "Abono") "TRANSFERENCIA" else "GASTO_INGRESO"
                onEditarTransaccion(t.id, tipoEdicion)
            }
        )
    }

    // --- DIÁLOGO DE CONFIRMACIÓN BORRAR ---
    if (mostrarConfirmacionBorrar && transaccionSeleccionada != null) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionBorrar = false },
            icon = { Icon(Icons.Default.Warning, null, tint = errorColor) },
            title = { Text("¿Eliminar movimiento?") },
            text = { Text("Esta acción revertirá el saldo en la cuenta.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.borrarTransaccion(transaccionSeleccionada!!)
                        mostrarConfirmacionBorrar = false
                        mostrarDetalleTransaccion = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = errorColor)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { mostrarConfirmacionBorrar = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Movimientos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        // Si no hay transacciones
        if (state.transacciones.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay movimientos registrados", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(state.transacciones) { transaccion ->
                    ItemMovimiento(
                        t = transaccion,
                        onClick = {
                            transaccionSeleccionada = transaccion
                            mostrarDetalleTransaccion = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ItemMovimiento(
    t: TransaccionEntity,
    onClick: () -> Unit
) {
    // Definir colores según si es ingreso o gasto
    val colorMonto = if (t.esIngreso) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurface
    val iconoColor = ServiceColorUtils.getColorByName(t.categoria) // Usamos tu util de colores
    val signo = if (t.esIngreso) "+ " else "- "

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconoColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = IconoUtils.getIconoByName(t.categoria),
                    contentDescription = null,
                    tint = iconoColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Textos Centrales
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (t.notaResumen.isNotEmpty()) t.notaResumen else t.categoria,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = DateUtils.formatearFechaAmigable(t.fecha),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Monto
            Text(
                // CAMBIO AQUÍ: $signo en lugar de $sign
                text = "$signo${CurrencyUtils.formatCurrency(t.monto)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = colorMonto
            )
        }
    }
}