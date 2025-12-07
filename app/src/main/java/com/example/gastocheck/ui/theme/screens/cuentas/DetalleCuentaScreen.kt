package com.example.gastocheck.ui.theme.screens.cuentas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.ui.theme.screens.home.DetalleTransaccionDialog
import com.example.gastocheck.ui.theme.util.CategoriaUtils
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import com.example.gastocheck.ui.theme.util.IconoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleCuentaScreen(
    accountId: Int,
    viewModel: DetalleCuentaViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onEditar: (Int) -> Unit,
    onVerTodos: () -> Unit,
    // Callbacks para el diálogo de transacciones (reutilizamos la lógica de navegación)
    onEditarTransaccion: (Int, String?) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Estados para diálogos
    var mostrarConfirmacionBorrarCuenta by remember { mutableStateOf(false) }
    var mostrarConfirmacionBorrarTransaccion by remember { mutableStateOf(false) }

    var transaccionSeleccionada by remember { mutableStateOf<TransaccionEntity?>(null) }
    var mostrarDetalleTransaccion by remember { mutableStateOf(false) }

    // --- COLORES ---
    val backgroundColor = MaterialTheme.colorScheme.background
    val cardColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onBackground
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    LaunchedEffect(state.cuenta) {
        if (state.cuenta == null && accountId != -1) { /* onBack() */ }
    }

    // --- DIÁLOGOS ---

    // 1. Borrar Cuenta
    if (mostrarConfirmacionBorrarCuenta) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionBorrarCuenta = false },
            icon = { Icon(Icons.Default.Warning, null, tint = errorColor) },
            title = { Text("¿Eliminar cuenta?") },
            text = { Text("Se eliminarán también todos los movimientos de esta cuenta. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.borrarCuentaActual { onBack() }
                        mostrarConfirmacionBorrarCuenta = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = errorColor)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { mostrarConfirmacionBorrarCuenta = false }) { Text("Cancelar") } }
        )
    }

    // 2. Detalle Transacción
    if (mostrarDetalleTransaccion && transaccionSeleccionada != null) {
        val t = transaccionSeleccionada!!

        DetalleTransaccionDialog(
            transaccion = t,
            cuenta = state.cuenta, // Pasamos la cuenta actual
            onDismiss = { mostrarDetalleTransaccion = false },
            onDelete = { mostrarConfirmacionBorrarTransaccion = true },
            onEdit = {
                mostrarDetalleTransaccion = false
                // Usamos el callback de navegación que pasaremos desde AppNavigation
                // Si es transferencia, pasamos null como textoAudio
                if (t.categoria == "Transferencia") {
                    onEditarTransaccion(t.id, "TRANSFERENCIA")
                } else {
                    onEditarTransaccion(t.id, "GASTO_INGRESO")
                }
            }
        )
    }

    // 3. Borrar Transacción
    if (mostrarConfirmacionBorrarTransaccion && transaccionSeleccionada != null) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionBorrarTransaccion = false },
            icon = { Icon(Icons.Default.Warning, null, tint = errorColor) },
            title = { Text("¿Eliminar movimiento?") },
            text = { Text("Esta acción ajustará el saldo de tu cuenta.") },
            confirmButton = {
                Button(
                    onClick = {
                        // Necesitas un método en DetalleCuentaViewModel para borrar transacciones
                        // Por ahora asumimos que existe o lo agregamos
                        viewModel.borrarTransaccion(transaccionSeleccionada!!)
                        mostrarConfirmacionBorrarTransaccion = false
                        mostrarDetalleTransaccion = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = errorColor)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { mostrarConfirmacionBorrarTransaccion = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.cuenta != null) {
                            val colorCuenta = try { Color(android.graphics.Color.parseColor(state.cuenta!!.colorHex)) } catch(e:Exception){ primaryColor }
                            val icono = IconoUtils.getIconoByName(state.cuenta!!.icono)
                            Icon(imageVector = icono, contentDescription = null, tint = colorCuenta, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(text = state.cuenta!!.nombre, color = textColor, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        } else {
                            Text("Cargando...", color = textColor)
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás", tint = textColor) } },
                actions = {
                    IconButton(onClick = { state.cuenta?.let { onEditar(it.id) } }) { Icon(Icons.Default.Edit, "Editar", tint = textColor) }
                    IconButton(onClick = { mostrarConfirmacionBorrarCuenta = true }) { Icon(Icons.Default.Delete, "Eliminar", tint = errorColor) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = backgroundColor)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // SALDO
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = cardColor)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Saldo disponible", color = subTextColor, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = CurrencyUtils.formatCurrency(state.saldoActual), color = textColor, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // MOVIMIENTOS
            item {
                Text("Últimos movimientos", color = textColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            val ultimosMovimientos = state.transacciones.take(5)
            if (ultimosMovimientos.isEmpty()) {
                item { Text("No hay movimientos aún.", color = subTextColor, modifier = Modifier.padding(vertical = 8.dp)) }
            } else {
                items(ultimosMovimientos) { transaccion ->
                    ItemMovimientoCuentaClickable(
                        t = transaccion,
                        greenColor = primaryColor,
                        redColor = errorColor,
                        textColor = textColor,
                        onClick = {
                            transaccionSeleccionada = transaccion
                            mostrarDetalleTransaccion = true
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (state.transacciones.size > 5) {
                item {
                    OutlinedButton(
                        onClick = onVerTodos, // Ahora navega a la nueva lista
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor)
                    ) { Text("Ver todos los movimientos") }
                }
            }
        }
    }
}

@Composable
fun ItemMovimientoCuentaClickable(
    t: TransaccionEntity,
    greenColor: Color,
    redColor: Color,
    textColor: Color,
    onClick: () -> Unit // Nuevo parámetro
) {
    val icon = if(t.categoria == "Transferencia") Icons.Default.SwapHoriz else CategoriaUtils.getIcono(t.categoria)
    val iconTint = if (t.esIngreso) greenColor else redColor
    val bgIcon = iconTint.copy(alpha = 0.1f)
    val signo = if (t.esIngreso) "+" else "-"
    val colorMonto = if (t.esIngreso) greenColor else redColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick) // <--- Hacemos clickeable
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(bgIcon), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = t.notaResumen.ifEmpty { t.categoria }, color = textColor, fontWeight = FontWeight.Medium, fontSize = 16.sp)
        }
        Text(text = "$signo${CurrencyUtils.formatCurrency(t.monto)}", color = colorMonto, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}