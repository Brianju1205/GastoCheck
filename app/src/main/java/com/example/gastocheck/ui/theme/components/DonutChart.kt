package com.example.gastocheck.ui.theme.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.ui.theme.util.CategoriaUtils

@Composable
fun DonutChart(
    transacciones: List<TransaccionEntity>,
    size: Dp = 200.dp,
    thickness: Dp = 20.dp
) {
    val total = transacciones.sumOf { it.monto }
    val datosPorCategoria = transacciones.groupBy { it.categoria }
        .mapValues { entry -> entry.value.sumOf { it.monto } }

    var animationPlayed by remember { mutableStateOf(false) }
    val currentAngle by animateFloatAsState(
        targetValue = if (animationPlayed) 360f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "chartAnimation"
    )

    LaunchedEffect(key1 = transacciones) {
        animationPlayed = true
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(modifier = Modifier.size(size)) {
            var startAngle = -90f
            datosPorCategoria.forEach { (categoria, monto) ->
                val color = CategoriaUtils.getColor(categoria)
                val sweepAngle = if (total > 0) (monto / total).toFloat() * 360f else 0f
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total", style = MaterialTheme.typography.labelMedium)
            Text(
                text = "$${total.toInt()}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
