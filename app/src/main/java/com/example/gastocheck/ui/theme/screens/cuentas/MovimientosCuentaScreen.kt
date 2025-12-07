package com.example.gastocheck.ui.theme.screens.cuentas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.ui.theme.screens.home.DetalleTransaccionDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovimientosCuentaScreen(
    accountId: Int,
    viewModel: DetalleCuentaViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onEditarTransaccion: (Int, String?) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Estados para detalle
    var transaccionSeleccionada by remember { mutableStateOf<TransaccionEntity?>(null) }
    var mostrarDetalleTransaccion by remember { mutableStateOf(false) }
    var mostrarConfirmacionBorrar by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val textColor = MaterialTheme.colorScheme.onBackground

    // DIALOGOS (Misma lógica que en DetalleCuentaScreen)
    if (mostrarDetalleTransaccion && transaccionSeleccionada != null) {
        val t = transaccionSeleccionada!!
        DetalleTransaccionDialog(
            transaccion = t,
            cuenta = state.cuenta,
            onDismiss = { mostrarDetalleTransaccion = false },
            onDelete = { mostrarConfirmacionBorrar = true },
            onEdit = {
                mostrarDetalleTransaccion = false
                if (t.categoria == "Transferencia") {
                    onEditarTransaccion(t.id, "TRANSFERENCIA")
                } else {
                    onEditarTransaccion(t.id, "GASTO_INGRESO")
                }
            }
        )
    }

    if (mostrarConfirmacionBorrar && transaccionSeleccionada != null) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionBorrar = false },
            icon = { Icon(Icons.Default.Warning, null, tint = errorColor) },
            title = { Text("¿Eliminar movimiento?") },
            text = { Text("Esta acción ajustará el saldo de tu cuenta.") },
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
                title = { Text("Todos los Movimientos", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás", tint = textColor) }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(state.transacciones) { transaccion ->
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
            }
        }
    }
}