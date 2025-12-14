package com.example.gastocheck.ui.theme.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastocheck.ui.theme.util.CurrencyUtils
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun CampoMontoOriginal(
    valor: String,
    onValorChange: (String) -> Unit,
    monedaSeleccionada: String,
    onMonedaChange: (String) -> Unit,
    colorTexto: Color,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val monedas = CurrencyUtils.obtenerMonedasDisponibles()

    // Manejo del cursor y formato con comas
    var textFieldValue by remember(valor) {
        val formatted = formatearConComas(valor)
        mutableStateOf(
            TextFieldValue(
                text = formatted,
                selection = TextRange(formatted.length)
            )
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. SELECTOR DE MONEDA (Sutil, encima del monto)
        Box(contentAlignment = Alignment.Center) {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.height(32.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monedaSeleccionada,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                monedas.forEach { m ->
                    DropdownMenuItem(text = { Text(m) }, onClick = { onMonedaChange(m); expanded = false })
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. INPUT GIGANTE (BasicTextField) - DISEÑO ORIGINAL
        Box(contentAlignment = Alignment.Center) {
            BasicTextField(
                value = textFieldValue,
                onValueChange = { newValue ->
                    val cleanString = newValue.text.replace(",", "").replace(".", "")
                    if (cleanString.all { it.isDigit() } && cleanString.length < 15) {
                        onValorChange(cleanString)
                        val formatted = formatearConComas(cleanString)
                        textFieldValue = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                    }
                },
                textStyle = TextStyle(
                    color = colorTexto,
                    fontSize = 56.sp, // TAMAÑO ORIGINAL
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                cursorBrush = SolidColor(colorTexto),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (valor.isEmpty()) {
                        Text(
                            "$ 0",
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

// Función auxiliar privada
private fun formatearConComas(input: String): String {
    if (input.isEmpty()) return ""
    return try {
        val number = input.replace(",", "").toDouble()
        val symbols = DecimalFormatSymbols(Locale.US)
        val formatter = DecimalFormat("#,###", symbols)
        formatter.format(number)
    } catch (e: Exception) { input }
}