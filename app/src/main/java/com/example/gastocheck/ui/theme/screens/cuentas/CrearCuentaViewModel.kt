package com.example.gastocheck.ui.theme.screens.cuentas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.repository.TransaccionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrearCuentaViewModel @Inject constructor(
    private val repository: TransaccionRepository
) : ViewModel() {

    private val _nombre = MutableStateFlow("")
    val nombre = _nombre.asStateFlow()

    private val _saldo = MutableStateFlow("")
    val saldo = _saldo.asStateFlow()

    private val _tipo = MutableStateFlow("Efectivo")
    val tipo = _tipo.asStateFlow()

    private val _colorSeleccionado = MutableStateFlow("#00E676") // Verde por defecto
    val colorSeleccionado = _colorSeleccionado.asStateFlow()

    private val _iconoSeleccionado = MutableStateFlow("Wallet")
    val iconoSeleccionado = _iconoSeleccionado.asStateFlow()

    // Opciones para la UI
    val listaTipos = listOf("Efectivo", "Tarjeta Débito", "Tarjeta Crédito", "Ahorros", "Vales", "Inversión")

    // Lista de colores (Hex)
    val listaColores = listOf(
        "#00E676", // Verde (Default)
        "#6200EA", // Morado Profundo
        "#FF6D00", // Naranja
        "#F50057", // Rosa/Rojo
        "#2962FF", // Azul Fuerte
        "#AA00FF", // Violeta
        "#FFD600"  // Amarillo
    )

    // Lista de nombres de iconos (Los mapearemos en la UI)
    val listaIconos = listOf(
        "Wallet", "CreditCard", "Savings", "AttachMoney",
        "AccountBalance", "ShoppingCart", "Work", "TrendingUp",
        "Home", "School"
    )

    fun onNombreChange(nuevo: String) { _nombre.value = nuevo }
    fun onSaldoChange(nuevo: String) { _saldo.value = nuevo }
    fun onTipoChange(nuevo: String) { _tipo.value = nuevo }
    fun onColorChange(nuevo: String) { _colorSeleccionado.value = nuevo }
    fun onIconoChange(nuevo: String) { _iconoSeleccionado.value = nuevo }

    fun guardarCuenta(onSuccess: () -> Unit) {
        val saldoVal = _saldo.value.toDoubleOrNull() ?: 0.0
        val nombreVal = _nombre.value.trim()

        if (nombreVal.isNotEmpty()) {
            viewModelScope.launch {
                val nuevaCuenta = CuentaEntity(
                    nombre = nombreVal,
                    tipo = _tipo.value,
                    saldoInicial = saldoVal,
                    colorHex = _colorSeleccionado.value
                    // Nota: Si agregas campo 'icono' a la entidad Cuenta, úsalo aquí.
                    // Por ahora guardamos lo básico.
                )
                repository.insertCuenta(nuevaCuenta)
                onSuccess()
            }
        }
    }
}