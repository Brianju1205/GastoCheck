package com.example.gastocheck.ui.theme.screens.agregar

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.gastocheck.ui.theme.util.CategoriaUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarScreen(
    viewModel: AgregarViewModel = hiltViewModel(),
    alRegresar: () -> Unit
) {
    val monto by viewModel.monto.collectAsState()
    val descripcion by viewModel.descripcion.collectAsState()
    val esIngreso by viewModel.esIngreso.collectAsState()
    val categoriaSeleccionada by viewModel.categoria.collectAsState()

    val context = LocalContext.current
    val launcherVoz = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val resultados = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val textoHablado = resultados?.get(0)
            if (!textoHablado.isNullOrEmpty()) {
                viewModel.procesarVoz(textoHablado)
            }
        }
    }

    fun iniciarDictado() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di algo como: '1200 en comida'")
        }
        try { launcherVoz.launch(intent) } catch (e: Exception) { }
    }

    // Definimos título y color según el tipo que YA viene decidido por navegación
    val tituloPantalla = if (esIngreso) "Nuevo Ingreso" else "Nuevo Gasto"
    val colorBoton = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text(tituloPantalla) }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = monto,
                onValueChange = { viewModel.onMontoChange(it) },
                label = { Text("Monto ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = { viewModel.onDescripcionChange(it) },
                label = { Text("Nota (Opcional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // --- SE ELIMINÓ EL SWITCH DE TIPO ---

            Divider()

            Text("Selecciona Categoría:", style = MaterialTheme.typography.labelLarge)

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 70.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(CategoriaUtils.listaCategorias) { cat ->
                    val seleccionado = categoriaSeleccionada == cat.nombre
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp).clip(MaterialTheme.shapes.medium)
                            .clickable { viewModel.onCategoriaChange(cat.nombre) }
                            .background(if (seleccionado) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .padding(8.dp)
                    ) {
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(cat.color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Icon(imageVector = cat.icono, contentDescription = null, tint = cat.color)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = cat.nombre, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }

            // --- BOTÓN DE VOZ ARRIBA DE GUARDAR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                SmallFloatingActionButton(
                    onClick = { iniciarDictado() },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Dictar")
                }
            }

            Button(
                onClick = { viewModel.guardarTransaccion { alRegresar() } },
                modifier = Modifier.fillMaxWidth(),
                enabled = monto.isNotEmpty()
            ) {
                Text("Guardar")
            }
        }
    }
}
