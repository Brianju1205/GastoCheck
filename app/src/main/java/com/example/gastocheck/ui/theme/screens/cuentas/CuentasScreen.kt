package com.example.gastocheck.ui.theme.screens.cuentas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.ui.theme.screens.home.HomeViewModel
import com.example.gastocheck.ui.theme.util.CurrencyUtils

@Composable
fun CuentasScreen(
    viewModel: HomeViewModel = hiltViewModel(), // Reusamos para ver la lista
    onCrearCuenta: () -> Unit
) {
    val cuentas by viewModel.cuentas.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCrearCuenta, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "Nueva")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cuentas) { cuenta ->
                ItemCuenta(cuenta)
            }
        }
    }
}

@Composable
fun ItemCuenta(cuenta: CuentaEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(cuenta.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(cuenta.tipo, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            // Aquí solo mostramos el saldo inicial como referencia, el saldo real se calcula en Home
            Text("Saldo Inicial: ${CurrencyUtils.formatCurrency(cuenta.saldoInicial)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}