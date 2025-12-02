package com.example.gastocheck.ui.theme.screens.metas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.data.database.entity.MetaEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasScreen(viewModel: MetasViewModel = hiltViewModel()) {
    val metas by viewModel.metas.collectAsState()
    var mostrarDialogoCrear by remember { mutableStateOf(false) }
    var metaParaAbonar by remember { mutableStateOf<MetaEntity?>(null) }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Mis Metas") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogoCrear = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Meta")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(metas) { meta ->
                MetaItem(
                    meta = meta,
                    onAbonarClick = { metaParaAbonar = meta },
                    onDeleteClick = { viewModel.borrarMeta(meta) }
                )
            }
        }
    }

    if (mostrarDialogoCrear) {
        DialogoCrearMeta(
            onDismiss = { mostrarDialogoCrear = false },
            onConfirm = { nombre, objetivo ->
                viewModel.crearMeta(nombre, objetivo)
                mostrarDialogoCrear = false
            }
        )
    }

    if (metaParaAbonar != null) {
        DialogoAbonar(
            meta = metaParaAbonar!!,
            onDismiss = { metaParaAbonar = null },
            onConfirm = { monto ->
                viewModel.abonarAMeta(metaParaAbonar!!, monto)
                metaParaAbonar = null
            }
        )
    }
}

@Composable
fun MetaItem(meta: MetaEntity, onAbonarClick: () -> Unit, onDeleteClick: () -> Unit) {
    val progreso = (meta.montoAhorrado / meta.montoObjetivo).toFloat()

    Card(elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(meta.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color.Gray)
                }
            }

            // Barra de progreso
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$${meta.montoAhorrado.toInt()} / $${meta.montoObjetivo.toInt()}", style = MaterialTheme.typography.bodyMedium)
                Text("${(progreso * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onAbonarClick,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                enabled = progreso < 1.0f
            ) {
                Text(if (progreso >= 1.0f) "¡Meta Completada!" else "Abonar Dinero")
            }
        }
    }
}

@Composable
fun DialogoCrearMeta(onDismiss: () -> Unit, onConfirm: (String, Double) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var objetivo by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Meta") },
        text = {
            Column {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre (ej. Viaje)") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = objetivo, onValueChange = { objetivo = it }, label = { Text("Monto Objetivo ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(onClick = {
                val obj = objetivo.toDoubleOrNull() ?: 0.0
                if (nombre.isNotEmpty() && obj > 0) onConfirm(nombre, obj)
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun DialogoAbonar(meta: MetaEntity, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var monto by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abonar a ${meta.nombre}") },
        text = {
            OutlinedTextField(value = monto, onValueChange = { monto = it }, label = { Text("Cantidad a ahorrar ($)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        },
        confirmButton = {
            Button(onClick = {
                val m = monto.toDoubleOrNull() ?: 0.0
                if (m > 0) onConfirm(m)
            }) { Text("Abonar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}