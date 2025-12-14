package com.example.gastocheck.ui.theme.screens.saldo

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SaldoScreen(
    state: SaldoUiState,
    onPeriodoSelected: (PeriodoFiltro) -> Unit,
) {
    val scrollState = rememberScrollState()

    // Estado local para la interactividad inmediata (más fluido)
    var puntoInteractivo by remember { mutableStateOf<Pair<LocalDate, Double>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = "Tendencia de saldo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selector de Periodo
        PeriodoSelector(selected = state.periodoSeleccionado, onSelect = onPeriodoSelected)

        Spacer(modifier = Modifier.height(24.dp))

        // Header cambia dinámicamente si estás tocando la gráfica
        if (puntoInteractivo != null) {
            PuntoDetalleHeader(punto = puntoInteractivo!!)
        } else {
            SaldoHeader(state)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Gráfica Interactiva
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(320.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Evolución", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(16.dp))

                if (state.historialPuntos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Sin datos suficientes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    SaldoTrendChart(
                        puntos = state.historialPuntos,
                        colorLinea = MaterialTheme.colorScheme.primary,
                        isDark = isSystemInDarkTheme(),
                        onPointSelected = { puntoInteractivo = it },
                        onTouchEnd = { puntoInteractivo = null }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // KPIs
        Text("Indicadores Clave", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        KpiGrid(state)

        Spacer(modifier = Modifier.height(24.dp))

        // Alertas
        if (state.alertaTexto != null) {
            AlertaCard(mensaje = state.alertaTexto!!)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Recomendación Inteligente (Nuevo)
        if (state.recomendacion != null) {
            RecomendacionCard(texto = state.recomendacion!!)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Proyecciones
        ProyeccionesSection(state)

        Spacer(modifier = Modifier.height(16.dp))

        // Insights
        InsightCard(text = state.insightIa)

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// --- COMPONENTES ---

@Composable
fun PuntoDetalleHeader(punto: Pair<LocalDate, Double>) {
    Column {
        Text("Saldo el ${formatFechaAmigable(punto.first)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            text = CurrencyUtils.formatCurrency(punto.second),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text("Seleccionado en gráfica", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PeriodoSelector(selected: PeriodoFiltro, onSelect: (PeriodoFiltro) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(PeriodoFiltro.values()) { periodo ->
            FilterChip(
                selected = periodo == selected,
                onClick = { onSelect(periodo) },
                label = { Text(periodo.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun SaldoHeader(state: SaldoUiState) {
    Column {
        Text("Saldo actual", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = CurrencyUtils.formatCurrency(state.saldoActual),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            val isPositive = state.variacionPorcentaje >= 0
            val color = if (isPositive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
            val icon = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${if(isPositive) "+" else ""}${String.format("%.1f", state.variacionPorcentaje)}% vs periodo anterior",
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun SaldoTrendChart(
    puntos: List<Pair<LocalDate, Double>>,
    colorLinea: Color,
    isDark: Boolean,
    onPointSelected: (Pair<LocalDate, Double>) -> Unit,
    onTouchEnd: () -> Unit
) {
    val textColor = if (isDark) Color.White.toArgb() else Color.Black.toArgb()
    val tooltipBgColor = if (isDark) Color(0xFF333333).toArgb() else Color(0xFFEEEEEE).toArgb()

    // Estado local para dibujar el indicador de selección (línea vertical)
    var touchX by remember { mutableStateOf<Float?>(null) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            // Detector de TOQUES (Tap)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        touchX = offset.x
                        val width = size.width - 80f
                        val index = ((offset.x - 40f) / width * (puntos.size - 1)).toInt().coerceIn(0, puntos.size - 1)
                        onPointSelected(puntos[index])
                        tryAwaitRelease()
                        touchX = null
                        onTouchEnd()
                    }
                )
            }
            // Detector de ARRASTRE (Drag)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        touchX = null
                        onTouchEnd()
                    },
                    onDragCancel = {
                        touchX = null
                        onTouchEnd()
                    }
                ) { change, _ ->
                    touchX = change.position.x
                    val width = size.width - 80f
                    val index = ((change.position.x - 40f) / width * (puntos.size - 1)).toInt().coerceIn(0, puntos.size - 1)
                    onPointSelected(puntos[index])
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val padding = 40f
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding

        // Escalas
        val maxVal = puntos.maxOf { it.second } * 1.05
        val minVal = puntos.minOf { it.second } * 0.95
        val range = (maxVal - minVal).coerceAtLeast(1.0)

        // Coordenadas
        val coordinates = puntos.mapIndexed { index, pair ->
            val x = padding + (index.toFloat() / (puntos.size - 1).coerceAtLeast(1)) * chartWidth
            val y = height - padding - ((pair.second - minVal) / range * chartHeight).toFloat()
            x to y
        }

        // 1. Dibujar Línea y Fondo
        val path = Path().apply {
            moveTo(coordinates.first().first, height - padding)
            coordinates.forEach { (x, y) -> lineTo(x, y) }
            lineTo(coordinates.last().first, height - padding)
            close()
        }
        drawPath(path, Brush.verticalGradient(colors = listOf(colorLinea.copy(alpha = 0.3f), Color.Transparent), startY = 0f, endY = height))

        val strokePath = Path().apply {
            moveTo(coordinates.first().first, coordinates.first().second)
            for (i in 0 until coordinates.size - 1) {
                val (x1, y1) = coordinates[i]
                val (x2, y2) = coordinates[i+1]
                cubicTo((x1 + x2)/2, y1, (x1 + x2)/2, y2, x2, y2)
            }
        }
        drawPath(strokePath, colorLinea, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        // 2. Ejes y Fechas
        val paintText = Paint().apply {
            color = textColor
            textSize = 10.sp.toPx()
            textAlign = Paint.Align.CENTER
        }

        val step = (puntos.size / 5).coerceAtLeast(1)
        coordinates.forEachIndexed { index, (x, _) ->
            if (index % step == 0 || index == puntos.size - 1) {
                val label = formatFechaAmigable(puntos[index].first)
                drawContext.canvas.nativeCanvas.drawText(label, x, height, paintText)
            }
        }

        // 3. INTERACTIVIDAD (Dibujar selección si hay toque)
        touchX?.let { tx ->
            val index = ((tx - padding) / chartWidth * (puntos.size - 1)).toInt().coerceIn(0, puntos.size - 1)
            val (px, py) = coordinates[index]

            // Línea vertical
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(px, padding),
                end = Offset(px, height - padding),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )

            // Círculo
            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = Offset(px, py))
            drawCircle(color = colorLinea, radius = 4.dp.toPx(), center = Offset(px, py))

            // Tooltip
            val textValue = CurrencyUtils.formatCurrency(puntos[index].second)
            val textDate = formatFechaAmigable(puntos[index].first)
            val tooltipText = "$textDate: $textValue"

            val paintTooltip = Paint().apply {
                color = textColor
                textSize = 12.sp.toPx()
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }

            val textWidth = paintTooltip.measureText(tooltipText)
            val rectWidth = textWidth + 40f
            val rectHeight = 60f
            var rectX = px - rectWidth / 2
            if (rectX < 0) rectX = 10f
            if (rectX + rectWidth > width) rectX = width - rectWidth - 10f

            val rectY = py - 80f

            drawRoundRect(
                color = Color(tooltipBgColor).copy(alpha = 0.9f),
                topLeft = Offset(rectX, rectY),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(16f),
            )
            drawContext.canvas.nativeCanvas.drawText(tooltipText, rectX + rectWidth/2, rectY + 40f, paintTooltip)
        }
    }
}

// Función auxiliar para fechas bonitas
fun formatFechaAmigable(date: LocalDate): String {
    val hoy = LocalDate.now()
    return when {
        date.isEqual(hoy) -> "Hoy"
        date.isEqual(hoy.minusDays(1)) -> "Ayer"
        else -> {
            val formatter = DateTimeFormatter.ofPattern("d MMM", Locale("es", "ES"))
            date.format(formatter).replace(".", "")
        }
    }
}

@Composable
fun KpiGrid(state: SaldoUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiCard(Modifier.weight(1f), "Promedio", state.kpiPromedio, Icons.Rounded.AutoGraph)
        KpiCard(Modifier.weight(1f), "Máximo", state.kpiMaximo, Icons.Rounded.TrendingUp, Color(0xFF4CAF50))
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiCard(Modifier.weight(1f), "Mínimo", state.kpiMinimo, Icons.Rounded.TrendingDown, MaterialTheme.colorScheme.error)
        val variacion = state.saldoActual - (state.historialPuntos.firstOrNull()?.second ?: 0.0)
        KpiCard(Modifier.weight(1f), "Variación Neta", variacion, Icons.Rounded.AutoGraph, isNeutral = true)
    }
}

@Composable
fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    isNeutral: Boolean = false
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(CurrencyUtils.formatCurrency(amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ProyeccionesSection(state: SaldoUiState) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Proyección Inteligente", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Proyección cierre mes: ${CurrencyUtils.formatCurrency(state.proyeccionFinMes)}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AlertaCard(mensaje: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(modifier = Modifier.width(12.dp))
            Text(mensaje, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun RecomendacionCard(texto: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Recomendación para ti",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = texto,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun InsightCard(text: String) {
    Column {
        Text("Insights", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
    }
}