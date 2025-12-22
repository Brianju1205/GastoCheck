package com.example.gastocheck.ui.theme.screens.cuentas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.SuscripcionEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import com.example.gastocheck.data.repository.SuscripcionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import kotlin.math.abs
import javax.inject.Inject

@HiltViewModel
class CrearCuentaViewModel @Inject constructor(
    private val cuentaDao: CuentaDao,
    private val transaccionDao: TransaccionDao,
    private val suscripcionRepository: SuscripcionRepository
) : ViewModel() {

    // --- ESTADOS ---
    private val _nombre = MutableStateFlow("")
    val nombre = _nombre.asStateFlow()

    // EN CRÉDITO: Esta variable almacena el LÍMITE DE CRÉDITO (ej: 25,000)
    // EN DÉBITO: Almacena el SALDO TOTAL
    private val _saldo = MutableStateFlow("")
    val saldo = _saldo.asStateFlow()

    private val _deudaInicial = MutableStateFlow("") // Deuda manual (ej: 5,000)
    val deudaInicial = _deudaInicial.asStateFlow()

    private val _colorSeleccionado = MutableStateFlow("#00E676")
    val colorSeleccionado = _colorSeleccionado.asStateFlow()

    private val _iconoSeleccionado = MutableStateFlow("Wallet")
    val iconoSeleccionado = _iconoSeleccionado.asStateFlow()

    private val _tipo = MutableStateFlow("Efectivo")
    val tipo = _tipo.asStateFlow()

    private val _esCredito = MutableStateFlow(false)
    val esCredito = _esCredito.asStateFlow()

    private val _diaCorte = MutableStateFlow("")
    val diaCorte = _diaCorte.asStateFlow()

    private val _diaPago = MutableStateFlow("")
    val diaPago = _diaPago.asStateFlow()

    private val _tasaInteres = MutableStateFlow("")
    val tasaInteres = _tasaInteres.asStateFlow()

    private val _crearRecordatorios = MutableStateFlow(true)
    val crearRecordatorios = _crearRecordatorios.asStateFlow()

    // Listas
    val listaColores = listOf("#00E676", "#2979FF", "#FFD700", "#FF1744", "#AA00FF", "#FF9100", "#00B0FF", "#00C853", "#607D8B", "#795548")
    val listaIconos = listOf("Wallet", "CreditCard", "Savings", "AttachMoney", "AccountBalance", "ShoppingCart", "Work", "TrendingUp", "Home", "School", "Restaurant", "DirectionsCar", "LocalHospital", "SportsEsports", "Checkroom")
    val listaTipos = listOf("Efectivo", "Débito", "Ahorro", "Vales", "Inversión", "Otro")

    private var cuentaIdEditar: Int = -1
    private var variacionSaldoHistorico: Double = 0.0
    private var deudaRealActualBD: Double = 0.0

    // --- CARGAR DATOS ---
    fun inicializar(id: Int) {
        cuentaIdEditar = id
        if (id != -1 && id != 0) {
            viewModelScope.launch {
                val cuenta = cuentaDao.getCuentaById(id)
                if (cuenta != null) {
                    _nombre.value = cuenta.nombre
                    _colorSeleccionado.value = cuenta.colorHex
                    _iconoSeleccionado.value = cuenta.icono
                    _esCredito.value = cuenta.esCredito
                    _tipo.value = cuenta.tipo

                    val transacciones = transaccionDao.getTransaccionesByCuentaList(id)

                    if (cuenta.esCredito) {
                        // === MODO EDICIÓN CRÉDITO ===
                        // 1. Cargamos el LÍMITE en el campo principal (ej: 25,000)
                        _saldo.value = cuenta.limiteCredito.toString()

                        // 2. Calculamos la DEUDA REAL actual (Gastos - Pagos)
                        val gastos = transacciones.filter { !it.esIngreso }.sumOf { it.monto }
                        val pagos = transacciones.filter { it.esIngreso }.sumOf { it.monto }

                        deudaRealActualBD = gastos - pagos
                        if (deudaRealActualBD < 0) deudaRealActualBD = 0.0

                        // 3. Cargamos la DEUDA en el campo secundario (ej: 5,000)
                        _deudaInicial.value = deudaRealActualBD.toString()

                        // Otros campos
                        _diaCorte.value = if (cuenta.diaCorte > 0) cuenta.diaCorte.toString() else ""
                        _diaPago.value = if (cuenta.diaPago > 0) cuenta.diaPago.toString() else ""
                        _tasaInteres.value = if (cuenta.tasaInteres > 0.0) cuenta.tasaInteres.toString() else ""
                        _crearRecordatorios.value = cuenta.recordatoriosActivos

                    } else {
                        // === MODO EDICIÓN DÉBITO ===
                        variacionSaldoHistorico = transacciones.sumOf { if (it.esIngreso) it.monto else -it.monto }
                        _saldo.value = (cuenta.saldoInicial + variacionSaldoHistorico).toString()
                        _deudaInicial.value = ""
                    }
                }
            }
        } else {
            limpiarFormulario()
        }
    }

    private fun limpiarFormulario() {
        cuentaIdEditar = -1
        variacionSaldoHistorico = 0.0
        deudaRealActualBD = 0.0
        _nombre.value = ""
        _saldo.value = ""
        _deudaInicial.value = ""
        _colorSeleccionado.value = "#00E676"
        _iconoSeleccionado.value = "Wallet"
        _tipo.value = "Efectivo"
        _esCredito.value = false
        _diaCorte.value = ""
        _diaPago.value = ""
        _tasaInteres.value = ""
        _crearRecordatorios.value = true
    }

    // --- SETTERS ---
    fun onNombreChange(v: String) { _nombre.value = v }
    fun onSaldoChange(v: String) { _saldo.value = v }
    fun onDeudaChange(v: String) { _deudaInicial.value = v }
    fun onColorChange(v: String) { _colorSeleccionado.value = v }
    fun onIconoChange(v: String) { _iconoSeleccionado.value = v }

    fun onDiaCorteSelected(millis: Long?) {
        millis?.let {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it
            _diaCorte.value = cal.get(Calendar.DAY_OF_MONTH).toString()
        }
    }

    fun onDiaPagoSelected(millis: Long?) {
        millis?.let {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it
            _diaPago.value = cal.get(Calendar.DAY_OF_MONTH).toString()
        }
    }

    fun onTasaInteresChange(v: String) { _tasaInteres.value = v }
    fun onRecordatoriosChange(v: Boolean) { _crearRecordatorios.value = v }
    fun onTipoChange(v: String) { _tipo.value = v }
    fun onEsCreditoChange(v: Boolean) {
        _esCredito.value = v
        if(v) {
            _tipo.value = "Crédito"
            if(_iconoSeleccionado.value == "Wallet") _iconoSeleccionado.value = "CreditCard"
        } else {
            _tipo.value = "Efectivo"
            if(_iconoSeleccionado.value == "CreditCard") _iconoSeleccionado.value = "Wallet"
        }
    }

    // --- GUARDAR (LÓGICA CORREGIDA) ---
    fun guardarCuenta(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val montoInput = _saldo.value.toDoubleOrNull() ?: 0.0 // Límite o Saldo
            val deudaInput = _deudaInicial.value.toDoubleOrNull() ?: 0.0

            val saldoInicialBD: Double
            val limiteCreditoBD: Double

            if (_esCredito.value) {
                // CORRECCIÓN PRINCIPAL AQUÍ:
                // Para que el saldo disponible se calcule bien (Disponible = Inicial - Gastos),
                // el saldoInicial debe ser igual al Límite de Crédito.
                limiteCreditoBD = montoInput
                saldoInicialBD = montoInput // <--- ANTES ERA 0.0, AHORA ES IGUAL AL LÍMITE
            } else {
                saldoInicialBD = montoInput - variacionSaldoHistorico
                limiteCreditoBD = 0.0
            }

            val nuevaCuenta = CuentaEntity(
                id = if (cuentaIdEditar == -1) 0 else cuentaIdEditar,
                nombre = _nombre.value,
                tipo = _tipo.value,
                saldoInicial = saldoInicialBD,
                colorHex = _colorSeleccionado.value,
                icono = _iconoSeleccionado.value,
                esArchivada = false,
                esCredito = _esCredito.value,
                limiteCredito = limiteCreditoBD,
                diaCorte = _diaCorte.value.toIntOrNull() ?: 0,
                diaPago = _diaPago.value.toIntOrNull() ?: 0,
                tasaInteres = _tasaInteres.value.toDoubleOrNull() ?: 0.0,
                recordatoriosActivos = _esCredito.value && _crearRecordatorios.value
            )

            val cuentaIdGuardada: Int

            if (cuentaIdEditar == -1) {
                // --- CREAR NUEVA ---
                val newIdLong = cuentaDao.insertCuenta(nuevaCuenta)
                cuentaIdGuardada = newIdLong.toInt()

                // Si hay deuda inicial, creamos el gasto
                if (_esCredito.value && deudaInput > 0.0) {
                    crearTransaccionAjusteDeuda(cuentaIdGuardada, deudaInput, "Deuda Inicial")
                }

                if (_esCredito.value && _crearRecordatorios.value) {
                    generarRecordatoriosAutomaticos(cuentaIdGuardada, nuevaCuenta)
                }

            } else {
                // --- EDITAR EXISTENTE ---
                cuentaDao.updateCuenta(nuevaCuenta)
                cuentaIdGuardada = cuentaIdEditar

                // Ajuste de Deuda al editar
                if (_esCredito.value) {
                    val diferencia = deudaInput - deudaRealActualBD
                    if (abs(diferencia) > 0.01) {
                        crearTransaccionAjusteDeuda(cuentaIdGuardada, diferencia, "Ajuste manual de deuda")
                    }
                }

                if (_esCredito.value && _crearRecordatorios.value) {
                    generarRecordatoriosAutomaticos(cuentaIdGuardada, nuevaCuenta)
                }
            }
            onSuccess()
        }
    }

    private suspend fun crearTransaccionAjusteDeuda(cuentaId: Int, monto: Double, nota: String) {
        val esAumentoDeuda = monto > 0
        val valorAbsoluto = abs(monto)

        val transaccion = TransaccionEntity(
            monto = valorAbsoluto,
            categoria = "Ajuste",
            notaCompleta = nota,
            notaResumen = "Ajuste Deuda",
            fecha = Date(),
            esIngreso = !esAumentoDeuda, // Gasto (False) si aumenta deuda
            cuentaId = cuentaId
        )
        transaccionDao.insertTransaccion(transaccion)
    }

    private suspend fun generarRecordatoriosAutomaticos(cuentaId: Int, cuenta: CuentaEntity) {
        if (cuenta.diaCorte > 0) {
            val fechaCorte = calcularProximaFecha(cuenta.diaCorte)
            val subCorte = SuscripcionEntity(
                nombre = "Corte: ${cuenta.nombre}",
                monto = 0.0,
                fechaPago = fechaCorte,
                frecuencia = "Mensual",
                icono = "CreditCard",
                cuentaId = cuentaId,
                nota = "Cierre de tarjeta. Revisar estado de cuenta.",
                recordatorio = "1 día antes",
                horaRecordatorio = "09:00",
                estadoActual = null
            )
            suscripcionRepository.insertSuscripcion(subCorte)
        }

        if (cuenta.diaPago > 0) {
            val fechaPago = calcularProximaFecha(cuenta.diaPago)
            val subPago = SuscripcionEntity(
                nombre = "Pagar: ${cuenta.nombre}",
                monto = 0.0,
                fechaPago = fechaPago,
                frecuencia = "Mensual",
                icono = "Payments",
                cuentaId = cuentaId,
                nota = "Fecha límite para no generar intereses.",
                recordatorio = "1 día antes",
                horaRecordatorio = "09:00",
                estadoActual = null
            )
            suscripcionRepository.insertSuscripcion(subPago)
        }
    }

    private fun calcularProximaFecha(diaObjetivo: Int): Long {
        val cal = Calendar.getInstance()
        val diaHoy = cal.get(Calendar.DAY_OF_MONTH)

        if (diaObjetivo <= diaHoy) {
            cal.add(Calendar.MONTH, 1)
        }

        val maxDiaMes = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val diaFinal = if (diaObjetivo > maxDiaMes) maxDiaMes else diaObjetivo

        cal.set(Calendar.DAY_OF_MONTH, diaFinal)
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return cal.timeInMillis
    }
}