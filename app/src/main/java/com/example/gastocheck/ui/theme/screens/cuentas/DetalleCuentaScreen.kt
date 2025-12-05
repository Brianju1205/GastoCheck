package com.example.gastocheck.ui.theme.screens.cuentas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingBag
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
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.ui.theme.util.CategoriaUtils
import com.example.gastocheck.ui.theme.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleCuentaScreen(
    accountId: Int, // Se usa en el ViewModel internamente
    viewModel: DetalleCuentaViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // COLORES PERSONALIZADOS SEGÚN IMAGEN
    val BackgroundDark = Color(0xFF0D1611) // Fondo casi negro verdoso
    val CardBackground = Color(0xFF15201A) // Fondo de tarjeta un poco mas claro
    val PrimaryGreen = Color(0xFF00E676)   // Verde neón
    val TextWhite = Color.White
    val TextGrey = Color(0xFFB0BEC5)
    val ExpenseRed = Color(0xFFEF5350)

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Cuenta: ${state.cuenta?.nombre ?: "Cargando..."}",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BackgroundDark
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- SECCIÓN SUPERIOR (TARJETA DE BALANCE) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Balance actual", color = TextGrey, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = CurrencyUtils.formatCurrency(state.saldoActual),
                        color = TextWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sección "Te queda para gastar hoy"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Te queda para gastar hoy", color = TextGrey, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyUtils.formatCurrency(state.presupuestoDiario),
                                color = TextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Icono de bolsa
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = PrimaryGreen
                            )
                        }
                    }
                }
            }

            // --- TÍTULO LISTA ---
            Text(
                text = "Movimientos de esta cuenta",
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // --- LISTA DE MOVIMIENTOS ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.transacciones) { transaccion ->
                    ItemMovimientoCuenta(transaccion, PrimaryGreen, ExpenseRed, TextWhite, TextGrey)
                }
            }

            // --- BOTONES INFERIORES ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botón VER REPORTE
                Button(
                    onClick = { /* Pendiente */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Ver reporte", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                // Botón EDITAR CUENTA
                Button(
                    onClick = { /* Pendiente */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Editar cuenta", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ItemMovimientoCuenta(
    t: TransaccionEntity,
    greenColor: Color,
    redColor: Color,
    textColor: Color,
    subTextColor: Color
) {
    val icon = CategoriaUtils.getIcono(t.categoria)
    // El color del icono depende si es ingreso o gasto
    val iconTint = if (t.esIngreso) greenColor else Color(0xFFFF8A80)
    val bgIcon = iconTint.copy(alpha = 0.1f)
    val signo = if (t.esIngreso) "+" else "-"
    val colorMonto = if (t.esIngreso) greenColor else redColor

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icono Circular
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(bgIcon),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Texto
        Column(modifier = Modifier.weight(1f)) {
            // CORRECCIÓN: Usamos t.notaResumen en lugar de t.descripcion
            // Como fallback usamos la categoría si el resumen estuviera vacío
            Text(
                text = t.notaResumen.ifEmpty { t.categoria },
                color = textColor,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
            // Opcional: Mostrar fecha aquí si quieres
            // Text(DateUtils.formatearFecha(t.fecha), color = subTextColor, fontSize = 12.sp)
        }

        // Monto
        Text(
            text = "$signo${CurrencyUtils.formatCurrency(t.monto)}",
            color = colorMonto,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}