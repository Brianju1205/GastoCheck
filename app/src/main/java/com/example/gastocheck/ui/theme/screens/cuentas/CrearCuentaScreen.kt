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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearCuentaScreen(
    idCuenta: Int = -1, // Parámetro para saber si es edición
    onBack: () -> Unit,
    viewModel: CrearCuentaViewModel = hiltViewModel()
) {
    // Cargar datos al entrar
    LaunchedEffect(idCuenta) {
        viewModel.inicializar(idCuenta)
    }

    val nombre by viewModel.nombre.collectAsState()
    val saldo by viewModel.saldo.collectAsState()
    val tipoSeleccionado by viewModel.tipo.collectAsState()
    val colorSeleccionado by viewModel.colorSeleccionado.collectAsState()
    val iconoSeleccionado by viewModel.iconoSeleccionado.collectAsState()

    // Colores del tema
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
            // 1. NOMBRE
            Column {
                Text("Nombre de la cuenta", color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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

            // 2. TIPO
            Column {
                Text("Tipo de cuenta", color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                    DropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }) {
                        viewModel.listaTipos.forEach { tipo ->
                            DropdownMenuItem(text = { Text(tipo) }, onClick = { viewModel.onTipoChange(tipo); expandedTipo = false })
                        }
                    }
                }
            }

            // 3. SALDO
            Column {
                Text("Saldo inicial", color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                                .clickable { viewModel.onColorChange(colorHex) }
                        )
                    }
                }
            }

            // 5. ICONO
            Column {
                Text("Icono", color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(140.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.listaIconos) { iconName ->
                        val isSelected = iconoSeleccionado == iconName
                        val iconVector = getIconByName(iconName)
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
                            Icon(imageVector = iconVector, contentDescription = null, tint = if (isSelected) primaryColor else outline)
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

// Función auxiliar para iconos (asegúrate de que esté en este archivo o en IconoUtils)
fun getIconByName(name: String): ImageVector {
    return when(name) {
        "Wallet" -> Icons.Default.Wallet
        "CreditCard" -> Icons.Default.CreditCard
        "Savings" -> Icons.Default.Savings
        "AttachMoney" -> Icons.Default.AttachMoney
        "AccountBalance" -> Icons.Default.AccountBalance
        "ShoppingCart" -> Icons.Default.ShoppingCart
        "Work" -> Icons.Default.Work
        "TrendingUp" -> Icons.Default.TrendingUp
        "Home" -> Icons.Default.Home
        "School" -> Icons.Default.School
        else -> Icons.Default.Wallet
    }
}