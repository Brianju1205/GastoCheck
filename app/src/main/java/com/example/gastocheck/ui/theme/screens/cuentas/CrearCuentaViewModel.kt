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

    private val _colorSeleccionado = MutableStateFlow("#00E676")
    val colorSeleccionado = _colorSeleccionado.asStateFlow()

    private val _iconoSeleccionado = MutableStateFlow("Wallet")
    val iconoSeleccionado = _iconoSeleccionado.asStateFlow()

    // Variable para saber si estamos editando
    private var cuentaIdEdicion: Int = -1

    val listaTipos = listOf("Efectivo", "Tarjeta Débito", "Tarjeta Crédito", "Ahorros", "Vales", "Inversión")
    val listaColores = listOf("#00E676", "#6200EA", "#FF6D00", "#F50057", "#2962FF", "#AA00FF", "#FFD600")
    val listaIconos = listOf("Wallet", "CreditCard", "Savings", "AttachMoney", "AccountBalance", "ShoppingCart", "Work", "TrendingUp", "Home", "School")

    fun onNombreChange(nuevo: String) { _nombre.value = nuevo }
    fun onSaldoChange(nuevo: String) { _saldo.value = nuevo }
    fun onTipoChange(nuevo: String) { _tipo.value = nuevo }
    fun onColorChange(nuevo: String) { _colorSeleccionado.value = nuevo }
    fun onIconoChange(nuevo: String) { _iconoSeleccionado.value = nuevo }

    // --- CARGAR DATOS PARA EDITAR ---
    fun inicializar(id: Int) {
        if (id != -1) {
            cuentaIdEdicion = id
            viewModelScope.launch {
                val cuenta = repository.getCuentaById(id)
                if (cuenta != null) {
                    _nombre.value = cuenta.nombre
                    _saldo.value = cuenta.saldoInicial.toString()
                    _tipo.value = cuenta.tipo
                    _colorSeleccionado.value = cuenta.colorHex
                    _iconoSeleccionado.value = cuenta.icono
                }
            }
        } else {
            // Reset para crear nueva
            cuentaIdEdicion = -1
            _nombre.value = ""
            _saldo.value = ""
            _tipo.value = "Efectivo"
            _colorSeleccionado.value = "#00E676"
            _iconoSeleccionado.value = "Wallet"
        }
    }

    fun guardarCuenta(onSuccess: () -> Unit) {
        val saldoVal = _saldo.value.toDoubleOrNull() ?: 0.0
        val nombreVal = _nombre.value.trim()

        if (nombreVal.isNotEmpty()) {
            viewModelScope.launch {
                val cuenta = CuentaEntity(
                    id = if (cuentaIdEdicion == -1) 0 else cuentaIdEdicion, // Usa el ID existente si edita
                    nombre = nombreVal,
                    tipo = _tipo.value,
                    saldoInicial = saldoVal,
                    colorHex = _colorSeleccionado.value,
                    icono = _iconoSeleccionado.value
                )

                // insertCuenta usa OnConflictStrategy.REPLACE, así que actualiza si el ID existe
                repository.insertCuenta(cuenta)
                onSuccess()
            }
        }
    }
}