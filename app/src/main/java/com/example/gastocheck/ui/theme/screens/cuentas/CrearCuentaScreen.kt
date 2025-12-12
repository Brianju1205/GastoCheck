package com.example.gastocheck.ui.theme.screens.cuentas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
// Importa tu utilidad de iconos
import com.example.gastocheck.ui.theme.util.IconoUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearCuentaScreen(
    idCuenta: Int = -1,
    onBack: () -> Unit,
    viewModel: CrearCuentaViewModel = hiltViewModel()
) {
    // Cargar datos una sola vez al entrar
    LaunchedEffect(idCuenta) {
        viewModel.inicializar(idCuenta)
    }

    val nombre by viewModel.nombre.collectAsState()
    val saldo by viewModel.saldo.collectAsState()
    val tipoSeleccionado by viewModel.tipo.collectAsState()
    val colorSeleccionado by viewModel.colorSeleccionado.collectAsState()
    val iconoSeleccionado by viewModel.iconoSeleccionado.collectAsState()

    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    var expandedTipo by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if(idCuenta == -1) "Crear Cuenta" else "Editar Cuenta",
                        color = onSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = onSurface)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // --- VISTA PREVIA (Tarjeta) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = try { Color(android.graphics.Color.parseColor(colorSeleccionado)) } catch (e: Exception) { primary }
                )
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Text(
                        text = tipoSeleccionado.uppercase(),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopStart)
                    )

                    // Icono seleccionado en la vista previa
                    Icon(
                        imageVector = IconoUtils.getIconoByName(iconoSeleccionado),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.align(Alignment.TopEnd).size(48.dp)
                    )

                    Column(modifier = Modifier.align(Alignment.BottomStart)) {
                        Text(
                            text = if (nombre.isEmpty()) "Nombre" else nombre,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$ ${if (saldo.isEmpty()) "0.00" else saldo}",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 1. NOMBRE
            Column {
                Text("Nombre", color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { viewModel.onNombreChange(it) },
                    placeholder = { Text("Ej: Cartera", color = outline) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = surface,
                        unfocusedContainerColor = surface,
                        focusedBorderColor = primary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            // 2. SALDO
            Column {
                Text("Saldo Actual", color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = saldo,
                    onValueChange = { viewModel.onSaldoChange(it) },
                    placeholder = { Text("$ 0.00", color = outline) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = surface,
                        unfocusedContainerColor = surface,
                        focusedBorderColor = primary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            // 3. TIPO
            Column {
                Text("Tipo", color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = tipoSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = onSurface) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = surface,
                            unfocusedContainerColor = surface,
                            focusedBorderColor = primary,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { expandedTipo = true })
                    DropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }, modifier = Modifier.background(surface)) {
                        viewModel.listaTipos.forEach { tipo ->
                            DropdownMenuItem(text = { Text(tipo, color = onSurface) }, onClick = { viewModel.onTipoChange(tipo); expandedTipo = false })
                        }
                    }
                }
            }

            // 4. COLOR
            Column {
                Text("Color", color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(viewModel.listaColores) { colorHex ->
                        val isSelected = colorSeleccionado == colorHex
                        val color = try { Color(android.graphics.Color.parseColor(colorHex)) } catch(e:Exception){ Color.Gray }
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(width = if (isSelected) 3.dp else 0.dp, color = onSurface, shape = CircleShape)
                                .clickable { viewModel.onColorChange(colorHex) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White)
                        }
                    }
                }
            }

            // 5. ICONO
            Column {
                Text("Icono", color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                // Usamos Box con altura fija para que el Grid scrollee dentro del Column principal si es necesario
                // o simplemente ocupe espacio fijo
                Box(modifier = Modifier.height(200.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(viewModel.listaIconos) { iconName ->
                            val isSelected = iconoSeleccionado == iconName
                            val iconVector = IconoUtils.getIconoByName(iconName)
                            val primaryColor = try { Color(android.graphics.Color.parseColor(colorSeleccionado)) } catch(e:Exception){ primary }

                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(surface)
                                    .border(width = if (isSelected) 2.dp else 0.dp, color = if (isSelected) primaryColor else Color.Transparent, shape = RoundedCornerShape(12.dp))
                                    .clickable { viewModel.onIconoChange(iconName) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = if (isSelected) primaryColor else outline
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BOTÓN
            Button(
                onClick = { viewModel.guardarCuenta { onBack() } },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text(if(idCuenta == -1) "Guardar Cuenta" else "Guardar Cambios", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}