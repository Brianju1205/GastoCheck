package com.example.gastocheck.ui.theme.screens.cuentas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.dao.TransaccionDao // <--- NUEVO IMPORT
import com.example.gastocheck.data.database.entity.CuentaEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CrearCuentaViewModel @Inject constructor(
    private val cuentaDao: CuentaDao,
    private val transaccionDao: TransaccionDao // <--- Necesario para calcular el balance
) : ViewModel() {

    // Estados del formulario
    private val _nombre = MutableStateFlow("")
    val nombre = _nombre.asStateFlow()

    private val _saldo = MutableStateFlow("")
    val saldo = _saldo.asStateFlow()

    private val _colorSeleccionado = MutableStateFlow("#00E676")
    val colorSeleccionado = _colorSeleccionado.asStateFlow()

    private val _iconoSeleccionado = MutableStateFlow("Wallet")
    val iconoSeleccionado = _iconoSeleccionado.asStateFlow()

    private val _tipo = MutableStateFlow("Efectivo")
    val tipo = _tipo.asStateFlow()

    val listaColores = listOf("#00E676", "#2979FF", "#FFD700", "#FF1744", "#AA00FF", "#FF9100", "#00B0FF", "#00C853", "#607D8B", "#795548")
    val listaIconos = listOf("Wallet", "CreditCard", "Savings", "AttachMoney", "AccountBalance", "ShoppingCart", "Work", "TrendingUp", "Home", "School", "Restaurant", "DirectionsCar", "LocalHospital", "SportsEsports", "Checkroom")
    val listaTipos = listOf("Efectivo", "Debito", "Credito", "Ahorro", "Vales")

    private var cuentaIdEditar: Int = -1

    // Variable para guardar la suma de transacciones y no perderla al guardar
    private var sumaMovimientosExistentes: Double = 0.0

    // --- CARGAR DATOS (LÓGICA CORREGIDA) ---
    fun inicializar(id: Int) {
        cuentaIdEditar = id
        if (id != -1) {
            // MODO EDICIÓN
            viewModelScope.launch {
                val cuenta = cuentaDao.getCuentaById(id)
                if (cuenta != null) {
                    _nombre.value = cuenta.nombre
                    _colorSeleccionado.value = cuenta.colorHex
                    _iconoSeleccionado.value = cuenta.icono
                    _tipo.value = cuenta.tipo

                    // 1. OBTENER MOVIMIENTOS DE LA CUENTA
                    val transacciones = transaccionDao.getTransaccionesByCuentaList(id)

                    // 2. CALCULAR EL IMPACTO DE LOS MOVIMIENTOS (Ingresos - Gastos)
                    sumaMovimientosExistentes = transacciones.sumOf {
                        if (it.esIngreso) it.monto else -it.monto
                    }

                    // 3. MOSTRAR EL SALDO TOTAL REAL (Inicial + Movimientos)
                    // Esto soluciona que veas 150 cuando deberías ver 250
                    val saldoTotalReal = cuenta.saldoInicial + sumaMovimientosExistentes
                    _saldo.value = saldoTotalReal.toString()
                }
            }
        } else {
            // MODO CREACIÓN
            sumaMovimientosExistentes = 0.0
            _nombre.value = ""
            _saldo.value = ""
            _colorSeleccionado.value = "#00E676"
            _iconoSeleccionado.value = "Wallet"
            _tipo.value = "Efectivo"
        }
    }

    // --- EVENTOS ---
    fun onNombreChange(texto: String) { _nombre.value = texto }
    fun onSaldoChange(texto: String) { _saldo.value = texto }
    fun onColorChange(color: String) { _colorSeleccionado.value = color }
    fun onTipoChange(nuevoTipo: String) { _tipo.value = nuevoTipo }
    fun onIconoChange(nuevoIcono: String) { _iconoSeleccionado.value = nuevoIcono }

    // --- GUARDAR (LÓGICA MATEMÁTICA CORREGIDA) ---
    fun guardarCuenta(onSuccess: () -> Unit) {
        viewModelScope.launch {
            // El usuario edita el SALDO FINAL que quiere ver
            val saldoFinalDeseado = _saldo.value.toDoubleOrNull() ?: 0.0

            // Calculamos qué saldo inicial se necesita para llegar a ese resultado
            // Ecuación: SaldoFinal = NuevoInicial + Movimientos
            // Despeje:  NuevoInicial = SaldoFinal - Movimientos
            val nuevoSaldoInicial = saldoFinalDeseado - sumaMovimientosExistentes

            val cuentaFinal = CuentaEntity(
                id = if (cuentaIdEditar == -1) 0 else cuentaIdEditar,
                nombre = _nombre.value,
                tipo = _tipo.value,
                saldoInicial = nuevoSaldoInicial, // Guardamos el valor calculado
                colorHex = _colorSeleccionado.value,
                icono = _iconoSeleccionado.value,
                esArchivada = false
            )

            if (cuentaIdEditar == -1) {
                cuentaDao.insertCuenta(cuentaFinal)
            } else {
                cuentaDao.updateCuenta(cuentaFinal)
            }
            onSuccess()
        }
    }
}