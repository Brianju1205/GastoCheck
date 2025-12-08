package com.example.gastocheck.ui.theme.screens.metas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.data.database.entity.MetaEntity
import com.example.gastocheck.ui.theme.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasScreen(viewModel: MetasViewModel = hiltViewModel()) {
    val metas by viewModel.metas.collectAsState()
    var mostrarDialogoCrear by remember { mutableStateOf(false) }
    var metaParaAbonar by remember { mutableStateOf<MetaEntity?>(null) }

    // Colores del tema para consistencia
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onBackground

    Scaffold(
        // 1. IMPORTANTE: Forzar el color de fondo para Edge-to-Edge
        containerColor = backgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mis Metas", fontWeight = FontWeight.Bold) },
                // 2. IMPORTANTE: La barra debe tener el mismo color que el fondo
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = textColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarDialogoCrear = true },
                containerColor = primaryColor,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Meta")
            }
        }
    ) { padding ->
        if (metas.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No tienes metas activas", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                // Espacio extra abajo para que el FAB no tape el último item
                contentPadding = PaddingValues(bottom = 80.dp, top = 16.dp)
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
    }

    // DIALOGOS
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
    val progreso = (meta.montoAhorrado / meta.montoObjetivo).toFloat().coerceIn(0f, 1f)
    val porcentaje = (progreso * 100).toInt()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(0.dp) // Diseño plano moderno
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Encabezado con Icono y Nombre
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD700).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(meta.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                // Botón eliminar discreto
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Barra de progreso
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Textos de dinero
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${CurrencyUtils.formatCurrency(meta.montoAhorrado)} de ${CurrencyUtils.formatCurrency(meta.montoObjetivo)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$porcentaje%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón de acción
            Button(
                onClick = onAbonarClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = progreso < 1.0f,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
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
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre (ej. Viaje)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = objetivo,
                    onValueChange = { objetivo = it },
                    label = { Text("Monto Objetivo ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
            OutlinedTextField(
                value = monto,
                onValueChange = { monto = it },
                label = { Text("Cantidad a ahorrar ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
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