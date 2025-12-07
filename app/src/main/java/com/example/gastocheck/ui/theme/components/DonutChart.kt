package com.example.gastocheck.ui.theme.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.ui.theme.util.CategoriaUtils
import com.example.gastocheck.ui.theme.util.CurrencyUtils

@Composable
fun DonutChart(
    transacciones: List<TransaccionEntity>,
    size: Dp = 200.dp,
    thickness: Dp = 20.dp
) {
    // Por defecto agrupa por categoría y usa colores de categoría
    val datos = remember(transacciones) {
        transacciones.groupBy { it.categoria }
            .mapValues { entry -> entry.value.sumOf { it.monto } }
    }
    DonutChartGeneric(
        datos = datos,
        size = size,
        thickness = thickness,
        colorProvider = { CategoriaUtils.getColor(it) }
    )
}

@Composable
fun DonutChartGeneric(
    datos: Map<String, Double>,
    size: Dp = 200.dp,
    thickness: Dp = 20.dp,
    colorProvider: (String) -> Color
) {
    val totalReal = datos.values.sum()
    var animationPlayed by remember { mutableStateOf(false) }
    val currentAngle by animateFloatAsState(
        targetValue = if (animationPlayed) 360f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "chartAnimation"
    )
    val animatedTotal by animateIntAsState(
        targetValue = totalReal.toInt(),
        animationSpec = tween(durationMillis = 800),
        label = "numberAnimation"
    )

    LaunchedEffect(key1 = datos) { animationPlayed = true }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            var startAngle = -90f

            // --- AQUÍ ESTABA EL ERROR: AGREGAMOS "style =" ---
            if (datos.isEmpty()) {
                drawArc(
                    color = Color.Gray.copy(alpha = 0.2f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round) // <--- CORREGIDO
                )
            }

            datos.forEach { (clave, monto) ->
                val colorBase = try { colorProvider(clave) } catch (e: Exception) { Color.Gray }
                val finalColor = if (colorBase == Color.Gray) Color(0xFF2979FF) else colorBase

                val sweepAngle = if (totalReal > 0) (monto / totalReal).toFloat() * 360f else 0f

                if (animationPlayed) {
                    drawArc(
                        color = finalColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * (currentAngle / 360f),
                        useCenter = false,
                        style = Stroke(width = thickness.toPx(), cap = StrokeCap.Butt) // <--- CORREGIDO
                    )
                    startAngle += sweepAngle
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total", style = MaterialTheme.typography.labelMedium)
            Text(
                text = "$${CurrencyUtils.formatWithCommas(animatedTotal)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
        }
    }
}