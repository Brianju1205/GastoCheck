package com.example.gastocheck.ui.theme.screens.cuentas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.ui.theme.screens.home.CuentaUiState
import com.example.gastocheck.ui.theme.screens.home.HomeViewModel
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import com.example.gastocheck.ui.theme.util.IconoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuentasListaScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavegarDetalle: (Int) -> Unit,
    onNavegarCrear: () -> Unit
) {
    // Usamos el nuevo estado con saldo calculado
    val cuentasState by viewModel.cuentasConSaldo.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mis Cuentas", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavegarCrear,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Cuenta")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cuentasState) { item ->
                ItemCuentaConSaldo(item, onClick = { onNavegarDetalle(item.cuenta.id) })
            }
        }
    }
}

@Composable
fun ItemCuentaConSaldo(item: CuentaUiState, onClick: () -> Unit) {
    val cuenta = item.cuenta

    // Convertir Hex String a Color
    val colorCuenta = try {
        Color(android.graphics.Color.parseColor(cuenta.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    // Obtener Icono
    val iconoVector = IconoUtils.getIconoByName(cuenta.icono)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ICONO COLOREADO
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorCuenta.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconoVector,
                    contentDescription = null,
                    tint = colorCuenta,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // TEXTOS
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cuenta.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                // SALDO REAL
                Text(
                    text = CurrencyUtils.formatCurrency(item.saldoActual),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.saldoActual >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}