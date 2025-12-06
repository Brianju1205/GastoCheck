package com.example.gastocheck.ui.theme.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastocheck.data.database.entity.BalanceSnapshotEntity
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import com.example.gastocheck.ui.theme.util.DateUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BalanceCarousel(
    saldoActual: Double,
    historial: List<BalanceSnapshotEntity>,
    onVerMasClick: () -> Unit = {}
) {
    // 1. FILTRADO: Ignoramos el primero (índice 0) porque es el saldo actual repetido.
    val historialPasado = if (historial.isNotEmpty()) historial.drop(1) else emptyList()

    // 2. LÍMITE: Solo mostramos 3 tarjetas de historia en el carrusel.
    val maxItems = 3
    val itemsMostrados = historialPasado.take(maxItems)

    // 3. ¿HAY MÁS? Si la lista filtrada es mayor a 3, activamos el botón "Ver más".
    val hayMas = historialPasado.size > maxItems

    // Total de páginas = 1 (Actual) + Items Mostrados (Max 3) + 1 (Botón "Ver más" si corresponde)
    val totalPages = 1 + itemsMostrados.size + (if (hayMas) 1 else 0)

    val pagerState = rememberPagerState(pageCount = { totalPages })

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
            verticalAlignment = Alignment.Top
        ) { page ->

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (page == 0) {
                    // --- PÁGINA 0: SALDO ACTUAL (Siempre visible) ---
                    val colorSaldo = if (saldoActual < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

                    Text(
                        text = CurrencyUtils.formatCurrency(saldoActual),
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        fontSize = 56.sp,
                        color = colorSaldo,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Disponible Ahora",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (page <= itemsMostrados.size) {
                    // --- PÁGINAS 1 a 3: HISTORIAL PREVIO ---
                    // Ajustamos el índice: page 1 corresponde al item 0 de la lista filtrada
                    val snapshot = itemsMostrados[page - 1]
                    val colorSaldoHist = if (snapshot.saldo < 0) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)

                    Text(
                        text = CurrencyUtils.formatCurrency(snapshot.saldo),
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        fontSize = 56.sp,
                        color = colorSaldoHist,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = DateUtils.formatearFechaCompleta(snapshot.fecha), // "12 Oct, 10:30 AM"
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // Nota: Eliminamos el motivo/categoría aquí también para ser consistentes
                    Text(
                        text = "Saldo anterior",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                } else {
                    // --- PÁGINA FINAL: BOTÓN VER TODO ---
                    Box(
                        modifier = Modifier
                            .height(80.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            onClick = onVerMasClick,
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Ver historial completo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Indicadores de página
        if (totalPages > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                val maxDots = totalPages.coerceAtMost(5)
                repeat(maxDots) { iteration ->
                    val color = if (pagerState.currentPage == iteration) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    }

                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(6.dp)
                    )
                }
            }
        }
    }
}