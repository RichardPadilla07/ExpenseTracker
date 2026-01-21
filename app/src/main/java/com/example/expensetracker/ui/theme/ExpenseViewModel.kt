package com.example.expensetracker.ui.theme

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.local.MedicamentoEntity
import com.example.expensetracker.data.local.RecordatorioEntity
import com.example.expensetracker.data.respository.MedicamentoRepository
import com.example.expensetracker.notifications.RecordatorioReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión de medicamentos y recordatorios.
 */
class MedicamentoViewModel(
    private val repository: MedicamentoRepository,
    private val appContext: Context // Se requiere contexto para AlarmManager
) : ViewModel() {
    // Estado del formulario de nuevo medicamento
    private val _nombre = MutableStateFlow("")
    val nombre: StateFlow<String> = _nombre.asStateFlow()

    private val _descripcion = MutableStateFlow("")
    val descripcion: StateFlow<String> = _descripcion.asStateFlow()

    // Lista de horarios (pares hora:minuto)
    private val _horarios = MutableStateFlow<List<Pair<Int, Int>>>(emptyList())
    val horarios: StateFlow<List<Pair<Int, Int>>> = _horarios.asStateFlow()

    // Lista de medicamentos con recordatorios
    val medicamentosConRecordatorios = repository.obtenerMedicamentosConRecordatorios()

    // Funciones para actualizar el formulario
    fun actualizarNombre(valor: String) { _nombre.value = valor }
    fun actualizarDescripcion(valor: String) { _descripcion.value = valor }
    fun agregarHorario(hora: Int, minuto: Int) {
        _horarios.value = _horarios.value + (hora to minuto)
    }
    fun eliminarHorario(index: Int) {
        _horarios.value = _horarios.value.toMutableList().apply { removeAt(index) }
    }
    fun limpiarFormulario() {
        _nombre.value = ""
        _descripcion.value = ""
        _horarios.value = emptyList()
    }

    private var medicamentoEditando: MedicamentoEntity? = null
    private var recordatoriosEditando: List<RecordatorioEntity> = emptyList()
    val enEdicion: Boolean get() = medicamentoEditando != null

    fun iniciarEdicion(medicamento: MedicamentoEntity, recordatorios: List<RecordatorioEntity>) {
        medicamentoEditando = medicamento
        recordatoriosEditando = recordatorios
        _nombre.value = medicamento.nombre
        _descripcion.value = medicamento.descripcion
        _horarios.value = recordatorios.map { it.hora to it.minuto }
    }

    fun cancelarEdicion() {
        medicamentoEditando = null
        recordatoriosEditando = emptyList()
        limpiarFormulario()
    }

    // Guardar un nuevo medicamento y sus recordatorios
    fun guardarMedicamento() {
        if (_nombre.value.isBlank()) return
        viewModelScope.launch {
            if (medicamentoEditando == null) {
                // Crear nuevo
                val medicamento = MedicamentoEntity(nombre = _nombre.value.trim(), descripcion = _descripcion.value.trim())
                val id = repository.agregarMedicamento(medicamento)
                _horarios.value.forEach { horario ->
                    val (hora, minuto) = horario
                    val recordatorio = RecordatorioEntity(medicamentoId = id.toInt(), hora = hora, minuto = minuto)
                    val recId = repository.agregarRecordatorio(recordatorio)
                    programarAlarma(medicamento.nombre, hora, minuto, recId.toInt())
                }
            } else {
                // Editar existente
                val medicamentoActualizado = medicamentoEditando!!.copy(
                    nombre = _nombre.value.trim(),
                    descripcion = _descripcion.value.trim()
                )
                repository.actualizarMedicamento(medicamentoActualizado)
                // Eliminar recordatorios y alarmas antiguas
                recordatoriosEditando.forEach {
                    cancelarAlarma(it.id)
                    repository.eliminarRecordatorio(it)
                }
                // Insertar nuevos recordatorios y alarmas
                _horarios.value.forEach { horario ->
                    val (hora, minuto) = horario
                    val recordatorio = RecordatorioEntity(medicamentoId = medicamentoActualizado.id, hora = hora, minuto = minuto)
                    val recId = repository.agregarRecordatorio(recordatorio)
                    programarAlarma(medicamentoActualizado.nombre, hora, minuto, recId.toInt())
                }
                medicamentoEditando = null
                recordatoriosEditando = emptyList()
            }
            limpiarFormulario()
        }
    }

    private fun programarAlarma(nombre: String, hora: Int, minuto: Int, notificationId: Int) {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(appContext, RecordatorioReceiver::class.java).apply {
            putExtra("medicamento", nombre)
            putExtra("hora", hora)
            putExtra("minuto", minuto)
            putExtra("notificationId", notificationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hora)
            set(java.util.Calendar.MINUTE, minuto)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    private fun cancelarAlarma(notificationId: Int) {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(appContext, RecordatorioReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun eliminarMedicamento(medicamento: MedicamentoEntity) {
        viewModelScope.launch {
            // Obtener recordatorios antes de eliminar
            val recordatorios = repository.obtenerRecordatoriosDeMedicamento(medicamento.id)
            recordatorios.collect { lista ->
                lista.forEach { cancelarAlarma(it.id) }
            }
            repository.eliminarMedicamento(medicamento)
        }
    }

    fun eliminarRecordatorio(recordatorio: RecordatorioEntity) {
        viewModelScope.launch {
            cancelarAlarma(recordatorio.id)
            repository.eliminarRecordatorio(recordatorio)
        }
    }
}

class MedicamentoViewModelFactory(
    private val repository: MedicamentoRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MedicamentoViewModel::class.java)) {
            return MedicamentoViewModel(repository, appContext) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}
