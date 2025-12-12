package com.example.gastocheck.ui.theme.screens.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ajustes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. HEADER PERFIL
            item { HeaderPerfil() }

            // 2. TARJETA PLAN PREMIUM
            item { CardPlanPremium() }

            // 3. SECCIÓN: AUTOMATIZACIÓN
            item { SeccionHeader("AUTOMATIZACIÓN Y PERMISOS") }
            item {
                ItemAjusteSwitch(
                    titulo = "Detección de notificaciones",
                    subtitulo = "Registra gastos bancarios automáticamente",
                    checked = true,
                    onCheckedChange = {}
                )
            }
            item {
                ItemAjusteSwitch(
                    titulo = "Auto-registrar suscripciones",
                    subtitulo = "Agregar como gasto al detectar pago",
                    checked = false,
                    onCheckedChange = {}
                )
            }
            item {
                ItemAjusteNavegacion(
                    titulo = "Reglas inteligentes",
                    icono = Icons.Default.SmartToy
                )
            }

            // 4. SECCIÓN: NOTIFICACIONES
            item { SeccionHeader("NOTIFICACIONES") }
            item {
                ItemAjusteSwitch(titulo = "Alertas de suscripciones", checked = true, onCheckedChange = {})
            }
            item {
                ItemAjusteSwitch(titulo = "Gastos grandes detectados", checked = true, onCheckedChange = {})
            }
            item {
                ItemAjusteSwitch(titulo = "Límites de presupuesto", checked = false, onCheckedChange = {})
            }

            // 5. SECCIÓN: INTEGRACIONES
            item { SeccionHeader("INTEGRACIONES Y DATOS") }

            // --- NUEVO ITEM: CUENTA COMPARTIDA ---
            item {
                ItemAjusteNavegacion(
                    titulo = "Cuenta Compartida",
                    icono = Icons.Default.Group, // Icono de Grupo/Personas
                    estadoTexto = "Vincular",    // Estado inicial
                    onClick = { /* Navegar a pantalla de vinculación */ }
                )
            }
            // -------------------------------------

            item {
                ItemAjusteNavegacion(
                    titulo = "Conexiones Bancarias",
                    icono = Icons.Default.AccountBalance,
                    badgeText = "PREMIUM",
                    isLocked = true
                )
            }
            item {
                ItemAjusteNavegacion(
                    titulo = "PayPal / Mercado Pago",
                    icono = Icons.Default.Payment,
                    estadoTexto = "Desconectado"
                )
            }
            item {
                ItemAjusteNavegacion(
                    titulo = "Exportar (CSV/PDF)",
                    icono = Icons.Default.Download
                )
            }

            // 6. SECCIÓN: SEGURIDAD
            item { SeccionHeader("SEGURIDAD") }
            item {
                ItemAjusteSwitch(
                    titulo = "Biometría / FaceID",
                    icono = Icons.Default.Fingerprint,
                    checked = true,
                    onCheckedChange = {}
                )
            }
            item {
                ItemAjusteSwitch(
                    titulo = "Ocultar saldos",
                    icono = Icons.Default.VisibilityOff,
                    checked = false,
                    onCheckedChange = {}
                )
            }

            // 7. SECCIÓN: PERSONALIZACIÓN
            item { SeccionHeader("PERSONALIZACIÓN") }
            item {
                ItemAjusteNavegacion(
                    titulo = "Tema",
                    icono = Icons.Default.Palette,
                    estadoTexto = "Oscuro"
                )
            }
            item {
                ItemAjusteNavegacion(
                    titulo = "Orden de pestañas",
                    icono = Icons.Default.ViewColumn
                )
            }

            // 8. FOOTER
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                ItemAjusteSimple(titulo = "Centro de ayuda", iconoEnd = Icons.Default.OpenInNew)
            }
            item {
                ItemAjusteSimple(titulo = "Acerca de la app", estadoTexto = "v2.4.0 (Build 302)")
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp)
                        .clickable { onLogout() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cerrar sesión", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Text(
                    text = "Política de Privacidad • Términos y Condiciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// ------------------------------------
// COMPONENTES UI (Iguales que antes)
// ------------------------------------

@Composable
fun HeaderPerfil() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E676))
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp), tint = Color.Black)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = "Juan Pérez",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Moneda: MXN ($)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CardPlanPremium() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1B5E20).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = Color(0xFF00E676))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Plan Premium", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Desbloquea detección automática de notificaciones, control de suscripciones ilimitado y exportación de reportes.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = Color.Black
                )
            ) {
                Text("Ver beneficios", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SeccionHeader(titulo: String) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.labelMedium,
        color = Color(0xFF00E676),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun ItemAjusteSwitch(
    titulo: String,
    subtitulo: String? = null,
    icono: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (icono != null) {
                Icon(icono, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column {
                Text(text = titulo, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                if (subtitulo != null) {
                    Text(text = subtitulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF00E676)
            )
        )
    }
}

@Composable
fun ItemAjusteNavegacion(
    titulo: String,
    icono: ImageVector? = null,
    badgeText: String? = null,
    estadoTexto: String? = null,
    isLocked: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icono != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icono, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(text = titulo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)

        if (badgeText != null) {
            Surface(color = Color(0xFF1B5E20), shape = RoundedCornerShape(4.dp)) {
                Text(
                    text = badgeText,
                    color = Color(0xFF00E676),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (estadoTexto != null) {
            Text(text = estadoTexto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (isLocked) {
            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        } else {
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ItemAjusteSimple(
    titulo: String,
    iconoEnd: ImageVector? = null,
    estadoTexto: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = titulo, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (estadoTexto != null) {
                Text(text = estadoTexto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (iconoEnd != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(iconoEnd, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
}