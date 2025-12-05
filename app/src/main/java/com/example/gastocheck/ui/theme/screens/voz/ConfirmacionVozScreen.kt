package com.example.gastocheck.ui.theme.screens.voz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.ui.theme.screens.agregar.AgregarViewModel
import com.example.gastocheck.ui.theme.util.CategoriaUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmacionVozScreen(
    textoDetectado: String,
    viewModel: AgregarViewModel = hiltViewModel(),
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {

    /**
     * Procesamos el texto de voz UNA SOLA VEZ al iniciar.
     */
    LaunchedEffect(key1 = textoDetectado) {
        if (textoDetectado.isNotEmpty()) {
            viewModel.procesarVoz(textoDetectado)
        }
    }

    // Estados provenientes del ViewModel
    val monto by viewModel.monto.collectAsState()
    val descripcion by viewModel.descripcion.collectAsState()
    val esIngreso by viewModel.esIngreso.collectAsState()
    val categoria by viewModel.categoria.collectAsState()
    val esMeta by viewModel.esMeta.collectAsState()

    // --- DETECCIÓN DE CUENTA ---
    val cuentas by viewModel.cuentas.collectAsState()
    val cuentaId by viewModel.cuentaIdSeleccionada.collectAsState()

    val nombreCuenta = remember(cuentas, cuentaId) {
        cuentas.find { it.id == cuentaId }?.nombre ?: "Efectivo"
    }

    val colorTema = when {
        esMeta -> Color(0xFFFFD700)
        esIngreso -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }

    val textoTipo = when {
        esMeta -> "Meta Detectada"
        esIngreso -> "Ingreso Detectado"
        else -> "Gasto Detectado"
    }

    val iconoCategoria = if (esMeta) {
        Icons.Default.Star
    } else {
        CategoriaUtils.getIcono(categoria)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                "Confirmar Registro",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // --- TARJETA DE CONFIRMACIÓN ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Icono principal circular
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(colorTema.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconoCategoria,
                            contentDescription = null,
                            tint = colorTema,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = textoTipo,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "$$monto",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorTema
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Categoría
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Categoría:", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
                        Text(
                            categoria,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Cuenta
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Cuenta:", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            nombreCuenta,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Nota (CORREGIDO AQUÍ)
                    // Usamos verticalAlignment = Alignment.Top para que si el texto es largo, la etiqueta "Nota:" se quede arriba
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                        Text("Nota:", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
                        Text(
                            text = descripcion.ifEmpty { "-" },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Escuché: \"$textoDetectado\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onCancelar,
                    modifier = Modifier.weight(1f).height(56.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cancelar")
                }

                Button(
                    onClick = {
                        viewModel.guardarTransaccion { onConfirmar() }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorTema)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar")
                }
            }
        }
    }
}