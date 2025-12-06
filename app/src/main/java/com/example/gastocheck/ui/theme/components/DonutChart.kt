package com.example.gastocheck.ui.theme.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val totalReal = transacciones.sumOf { it.monto }

    // Agrupamos y sumamos
    val datosPorCategoria = remember(transacciones) {
        transacciones.groupBy { it.categoria }
            .mapValues { entry -> entry.value.sumOf { it.monto } }
    }

    // Animación de Entrada (Giro de la gráfica)
    var animationPlayed by remember { mutableStateOf(false) }
    val currentAngle by animateFloatAsState(
        targetValue = if (animationPlayed) 360f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "chartAnimation"
    )

    // Animación del Número Central (Cuenta progresiva)
    val animatedTotal by animateIntAsState(
        targetValue = totalReal.toInt(),
        animationSpec = tween(durationMillis = 800),
        label = "numberAnimation"
    )

    LaunchedEffect(key1 = transacciones) {
        animationPlayed = true
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            var startAngle = -90f

            // Si no hay datos, dibujamos un círculo gris tenue de fondo
            if (datosPorCategoria.isEmpty()) {
                drawArc(
                    color = androidx.compose.ui.graphics.Color.Gray.copy(alpha = 0.2f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = thickness.toPx(), cap = StrokeCap.Round)
                )
            }

            datosPorCategoria.forEach { (categoria, monto) ->
                val color = CategoriaUtils.getColor(categoria)
                val sweepAngle = if (totalReal > 0) (monto / totalReal).toFloat() * 360f else 0f

                if (animationPlayed) {
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle * (currentAngle / 360f),
                        useCenter = false,
                        style = Stroke(width = thickness.toPx(), cap = StrokeCap.Butt)
                    )
                    startAngle += sweepAngle
                }
            }
        }

        // Texto Central Animado
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total", style = MaterialTheme.typography.labelMedium)

            // Usamos CurrencyUtils para el formato $1,000
            // Usamos animatedTotal para ver subir el número
            Text(
                text = "$${CurrencyUtils.formatWithCommas(animatedTotal)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
        }
    }
}