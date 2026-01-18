package com.example.gastocheck.ui.theme.screens.cuentas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastocheck.data.database.dao.CuentaDao
import com.example.gastocheck.data.database.dao.TransaccionDao
import com.example.gastocheck.data.database.entity.CuentaEntity
import com.example.gastocheck.data.database.entity.TransaccionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import kotlin.math.abs
import javax.inject.Inject
import java.util.TimeZone

@HiltViewModel
class CrearCuentaViewModel @Inject constructor(
    private val cuentaDao: CuentaDao,
    private val transaccionDao: TransaccionDao
    // ELIMINADO: private val suscripcionRepository: SuscripcionRepository
) : ViewModel() {

    // --- ESTADOS (Se mantienen igual) ---
    private val _nombre = MutableStateFlow("")
    val nombre = _nombre.asStateFlow()

    private val _saldo = MutableStateFlow("")
    val saldo = _saldo.asStateFlow()

    private val _deudaInicial = MutableStateFlow("")
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

    // Listas (Se mantienen igual)
    val listaColores = listOf("#00E676", "#2979FF", "#FFD700", "#FF1744", "#AA00FF", "#FF9100", "#00B0FF", "#00C853", "#607D8B", "#795548")
    val listaIconos = listOf("Wallet", "CreditCard", "Savings", "AttachMoney", "AccountBalance", "ShoppingCart", "Work", "TrendingUp", "Home", "School", "Restaurant", "DirectionsCar", "LocalHospital", "SportsEsports", "Checkroom")
    val listaTipos = listOf("Efectivo", "Débito", "Ahorro", "Vales", "Inversión", "Otro")

    private var cuentaIdEditar: Int = -1
    private var variacionSaldoHistorico: Double = 0.0
    private var deudaRealActualBD: Double = 0.0

    // --- INICIALIZAR (Lógica de carga se mantiene igual) ---
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
                        _saldo.value = cuenta.limiteCredito.toString()
                        val gastos = transacciones.filter { !it.esIngreso }.sumOf { it.monto }
                        val pagos = transacciones.filter { it.esIngreso }.sumOf { it.monto }
                        deudaRealActualBD = (gastos - pagos).coerceAtLeast(0.0)
                        _deudaInicial.value = deudaRealActualBD.toString()

                        _diaCorte.value = if (cuenta.diaCorte > 0) cuenta.diaCorte.toString() else ""
                        _diaPago.value = if (cuenta.diaPago > 0) cuenta.diaPago.toString() else ""
                        _tasaInteres.value = if (cuenta.tasaInteres > 0.0) cuenta.tasaInteres.toString() else ""
                        _crearRecordatorios.value = cuenta.recordatoriosActivos
                    } else {
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
    fun onTasaInteresChange(v: String) { _tasaInteres.value = v }
    fun onRecordatoriosChange(v: Boolean) { _crearRecordatorios.value = v }
    fun onTipoChange(v: String) { _tipo.value = v }

    fun onDiaCorteSelected(millis: Long?) {
        millis?.let {
            // CORRECCIÓN: Usamos TimeZone.getTimeZone("UTC")
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.timeInMillis = it
            _diaCorte.value = cal.get(Calendar.DAY_OF_MONTH).toString()
        }
    }

    fun onDiaPagoSelected(millis: Long?) {
        millis?.let {
            // CORRECCIÓN: Usamos TimeZone.getTimeZone("UTC")
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.timeInMillis = it
            _diaPago.value = cal.get(Calendar.DAY_OF_MONTH).toString()
        }
    }

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

    // --- GUARDAR REFACTORIZADO ---
    fun guardarCuenta(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val montoInput = _saldo.value.toDoubleOrNull() ?: 0.0
            val deudaInput = _deudaInicial.value.toDoubleOrNull() ?: 0.0

            val saldoInicialBD: Double
            val limiteCreditoBD: Double

            if (_esCredito.value) {
                limiteCreditoBD = montoInput
                saldoInicialBD = montoInput
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
                // Guardamos la preferencia, pero NO creamos suscripciones
                recordatoriosActivos = _esCredito.value && _crearRecordatorios.value
            )

            val cuentaIdGuardada: Int

            if (cuentaIdEditar == -1) {
                // INSERTAR
                val newIdLong = cuentaDao.insertCuenta(nuevaCuenta)
                cuentaIdGuardada = newIdLong.toInt()

                if (_esCredito.value && deudaInput > 0.0) {
                    crearTransaccionAjusteDeuda(cuentaIdGuardada, deudaInput, "Deuda Inicial")
                }
            } else {
                // ACTUALIZAR
                cuentaDao.updateCuenta(nuevaCuenta)
                cuentaIdGuardada = cuentaIdEditar

                if (_esCredito.value) {
                    val diferencia = deudaInput - deudaRealActualBD
                    if (abs(diferencia) > 0.01) {
                        crearTransaccionAjusteDeuda(cuentaIdGuardada, diferencia, "Ajuste manual de deuda")
                    }
                }
            }

            // NOTA: Se ha eliminado completamente la llamada a generarRecordatoriosAutomaticos
            // Las notificaciones ahora serán manejadas por el Worker independiente.

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
            esIngreso = !esAumentoDeuda,
            cuentaId = cuentaId
        )
        transaccionDao.insertTransaccion(transaccion)
    }
}