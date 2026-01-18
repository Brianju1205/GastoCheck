package com.example.gastocheck.ui.theme.screens.notificaciones

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.gastocheck.data.database.entity.NotificacionEntity
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesScreen(
    navController: NavController,
    viewModel: NotificacionesViewModel = hiltViewModel()
) {
    val notificaciones by viewModel.notificaciones.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    if (notificaciones.any { !it.leida }) {
                        IconButton(onClick = { viewModel.marcarTodasLeidas() }) {
                            Icon(Icons.Default.DoneAll, contentDescription = "Marcar leídas")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (notificaciones.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsOff, null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Text("Sin notificaciones", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(notificaciones) { notificacion ->
                    NotificacionItem(
                        notificacion = notificacion,
                        onDelete = { viewModel.eliminar(notificacion) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificacionItem(notificacion: NotificacionEntity, onDelete: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault())
    val colorFondo = if (notificacion.leida) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)

    // Icono según tipo
    val icono = when(notificacion.tipo) {
        "CREDITO" -> Icons.Default.CreditCard
        "SUSCRIPCION" -> Icons.Default.EventRepeat
        "META" -> Icons.Default.Savings
        else -> Icons.Default.Notifications
    }
    val colorIcono = when(notificacion.tipo) {
        "CREDITO" -> Color(0xFFFF1744) // Rojo alerta
        "SUSCRIPCION" -> Color(0xFF2979FF) // Azul
        else -> MaterialTheme.colorScheme.primary
    }

    ListItem(
        modifier = Modifier.background(colorFondo),
        headlineContent = {
            Text(notificacion.titulo, fontWeight = if(notificacion.leida) FontWeight.Normal else FontWeight.Bold)
        },
        supportingContent = {
            Column {
                Text(notificacion.mensaje, maxLines = 2)
                Text(dateFormat.format(notificacion.fecha), fontSize = 12.sp, color = Color.Gray)
            }
        },
        leadingContent = {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(colorIcono.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                Icon(icono, null, tint = colorIcono)
            }
        },
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
            }
        }
    )
    Divider()
}