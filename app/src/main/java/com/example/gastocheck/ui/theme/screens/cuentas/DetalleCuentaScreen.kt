package com.example.gastocheck.ui.theme.screens.cuentas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.ui.theme.screens.home.CuentaUiState
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import com.example.gastocheck.ui.theme.util.IconoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleCuentaScreen(
    accountId: Int,
    onBack: () -> Unit,
    onEditar: (Int) -> Unit,
    viewModel: DetalleCuentaViewModel = hiltViewModel(),
    onVerTodos: () -> Unit,
    onEditarTransaccion: (Int, String) -> Unit
) {
    // Inicializamos el ViewModel con el ID
    LaunchedEffect(accountId) { viewModel.inicializar(accountId) }

    val state by viewModel.uiState.collectAsState()
    var mostrarConfirmacionBorrar by remember { mutableStateOf(false) }

    val cuenta = state.cuenta

    if (mostrarConfirmacionBorrar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionBorrar = false },
            title = { Text("¿Eliminar Cuenta?") },
            text = { Text("Se borrarán todos los movimientos asociados. Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.borrarCuentaActual {
                            mostrarConfirmacionBorrar = false
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { mostrarConfirmacionBorrar = false }) { Text("Cancelar") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cuenta?.nombre ?: "Detalle") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { onEditar(accountId) }) {
                        Icon(Icons.Default.Edit, "Editar")
                    }
                    IconButton(onClick = { mostrarConfirmacionBorrar = true }) {
                        Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        if (cuenta != null) {
            Column(modifier = Modifier.padding(padding).padding(16.dp)) {

                // TARJETA DE RESUMEN
                val cuentaUi = CuentaUiState(cuenta, state.saldoActual)
                CardResumenDetalle(cuentaUi)

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onVerTodos,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Ver Movimientos")
                }

                if (cuenta.esCredito) {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Fechas de Corte y Pago", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Día de Corte", style = MaterialTheme.typography.labelMedium)
                                    Text(if(cuenta.diaCorte > 0) cuenta.diaCorte.toString() else "N/A", style = MaterialTheme.typography.titleMedium)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Día de Pago", style = MaterialTheme.typography.labelMedium)
                                    Text(if(cuenta.diaPago > 0) cuenta.diaPago.toString() else "N/A", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CardResumenDetalle(item: CuentaUiState) {
    val cuenta = item.cuenta

    // Color seguro
    val colorCuenta = try {
        Color(android.graphics.Color.parseColor(cuenta.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    // Icono
    val iconoVector = IconoUtils.getIconoByName(cuenta.icono)

    Card(
        modifier = Modifier.fillMaxWidth().height(200.dp), // Un poco más alto para acomodar todo
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorCuenta)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- CABECERA: Nombre, Tipo e Icono ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = cuenta.nombre,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Muestra el TIPO (Débito, Ahorro, Crédito, etc.)
                    Text(
                        text = cuenta.tipo.uppercase(),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // ICONO en la esquina superior derecha
                Icon(
                    imageVector = iconoVector,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(32.dp)
                )
            }

            // --- SALDOS ---
            if (cuenta.esCredito) {
                val disponible = item.saldoActual
                val deuda = cuenta.limiteCredito - disponible

                Column {
                    // CAMBIO SOLICITADO: Texto "Saldo Total" en lugar de "Disponible para gastar"
                    Text("Saldo Total", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                    Text(CurrencyUtils.formatCurrency(disponible), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Límite: ${CurrencyUtils.formatCurrency(cuenta.limiteCredito)}", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)

                    // Deuda con fondo blanco y texto rojo
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = " Deuda: ${CurrencyUtils.formatCurrency(deuda)} ",
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

            } else {
                // --- DÉBITO / AHORRO ---
                Column {
                    Text("Saldo Total", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text(CurrencyUtils.formatCurrency(item.saldoActual), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}