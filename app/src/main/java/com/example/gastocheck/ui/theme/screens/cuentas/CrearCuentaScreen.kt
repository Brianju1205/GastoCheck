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
    onBack: () -> Unit,
    viewModel: CrearCuentaViewModel = hiltViewModel()
) {
    val nombre by viewModel.nombre.collectAsState()
    val saldo by viewModel.saldo.collectAsState()
    val tipoSeleccionado by viewModel.tipo.collectAsState()
    val colorSeleccionado by viewModel.colorSeleccionado.collectAsState()
    val iconoSeleccionado by viewModel.iconoSeleccionado.collectAsState()

    // Colores del tema oscuro manuales para igualar la imagen
    val BackgroundDark = Color(0xFF191C1A)
    val SurfaceDark = Color(0xFF232624) // Para los inputs
    val PrimaryGreen = Color(0xFF00E676)
    val TextWhite = Color.White
    val TextGrey = Color(0xFFB0BEC5)

    // Estado del dropdown
    var expandedTipo by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Crear Cuenta", color = TextWhite, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = BackgroundDark)
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
                Text("Nombre de la cuenta", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { viewModel.onNombreChange(it) },
                    placeholder = { Text("Ej: Cartera", color = TextGrey) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )
            }

            // 2. TIPO DE CUENTA (DROPDOWN)
            Column {
                Text("Tipo de cuenta", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = tipoSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedTipo = true },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = TextGrey) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark,
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        ),
                        enabled = false // Deshabilitamos input texto, manejamos click con el Box overlay o modifier clickable
                    )
                    // Overlay transparente para asegurar click
                    Box(modifier = Modifier.matchParentSize().clickable { expandedTipo = true })

                    DropdownMenu(
                        expanded = expandedTipo,
                        onDismissRequest = { expandedTipo = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        viewModel.listaTipos.forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo, color = TextWhite) },
                                onClick = {
                                    viewModel.onTipoChange(tipo)
                                    expandedTipo = false
                                }
                            )
                        }
                    }
                }
            }

            // 3. SALDO INICIAL
            Column {
                Text("Saldo inicial", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = saldo,
                    onValueChange = { viewModel.onSaldoChange(it) },
                    placeholder = { Text("$ 0.00", color = TextGrey) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    singleLine = true
                )
            }

            // 4. COLOR
            Column {
                Text("Color", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.onColorChange(colorHex) }
                        )
                    }
                }
            }

            // 5. ICONO
            Column {
                Text("Icono", color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                // Grid de iconos (altura fija calculada o peso)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(140.dp), // Altura suficiente para 2 filas
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.listaIconos) { iconName ->
                        val isSelected = iconoSeleccionado == iconName
                        val iconVector = getIconByName(iconName)
                        val primaryColor = try { Color(android.graphics.Color.parseColor(colorSeleccionado)) } catch(e:Exception){ PrimaryGreen }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceDark)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) primaryColor else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.onIconoChange(iconName) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = if (isSelected) primaryColor else TextGrey
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. BOTÓN GUARDAR
            Button(
                onClick = { viewModel.guardarCuenta { onBack() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = Color.Black
                )
            ) {
                Text("Guardar Cuenta", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Helper para obtener iconos
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