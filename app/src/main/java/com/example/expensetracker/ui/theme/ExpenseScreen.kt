package com.example.expensetracker.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.local.MedicamentoEntity
import com.example.expensetracker.data.local.RecordatorioEntity
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicamentoScreen(viewModel: MedicamentoViewModel) {
    val nombre by viewModel.nombre.collectAsState()
    val descripcion by viewModel.descripcion.collectAsState()
    val horarios by viewModel.horarios.collectAsState()
    val medicamentosConRecordatorios by viewModel.medicamentosConRecordatorios.collectAsState(initial = emptyList())

    var showTimePicker by remember { mutableStateOf(false) }
    var timePickerIndex by remember { mutableStateOf(-1) }
    var notificacionesActivas by remember { mutableStateOf(false) }
    val context = LocalContext.current

    var editando by remember { mutableStateOf(false) }
    var medicamentoEdit by remember { mutableStateOf<MedicamentoEntity?>(null) }
    var recordatoriosEdit by remember { mutableStateOf<List<RecordatorioEntity>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Medicamentos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Formulario para agregar medicamento
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { viewModel.actualizarNombre(it) },
                        label = { Text("Nombre del medicamento") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = descripcion,
                        onValueChange = { viewModel.actualizarDescripcion(it) },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Horarios de toma:", style = MaterialTheme.typography.bodyMedium)
                    horarios.forEachIndexed { idx, (hora, minuto) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(String.format(Locale.getDefault(), "%02d:%02d", hora, minuto),
                                modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.eliminarHorario(idx) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar horario")
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(onClick = { showTimePicker = true; timePickerIndex = horarios.size }, modifier = Modifier.weight(1f)) {
                            Text("Agregar horario")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // Switch para activar/desactivar notificaciones
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (notificacionesActivas) "ON" else "OFF", modifier = Modifier.padding(end = 4.dp))
                            Switch(
                                checked = notificacionesActivas,
                                onCheckedChange = { checked ->
                                    notificacionesActivas = checked
                                    if (checked && android.os.Build.VERSION.SDK_INT >= 33) {
                                        val permission = android.Manifest.permission.POST_NOTIFICATIONS
                                        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                                            ActivityCompat.requestPermissions((context as android.app.Activity), arrayOf(permission), 100)
                                        }
                                    }
                                }
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                viewModel.guardarMedicamento()
                                editando = false
                                medicamentoEdit = null
                                recordatoriosEdit = emptyList()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (viewModel.enEdicion) "Guardar cambios" else "Guardar medicamento")
                        }
                        if (viewModel.enEdicion) {
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    viewModel.cancelarEdicion()
                                    editando = false
                                    medicamentoEdit = null
                                    recordatoriosEdit = emptyList()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar edición")
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Medicamentos y recordatorios", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (medicamentosConRecordatorios.isEmpty()) {
                Text("No hay medicamentos registrados", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                medicamentosConRecordatorios.forEach { mcr ->
                    MedicamentoItem(
                        medicamento = mcr.medicamento,
                        recordatorios = mcr.recordatorios,
                        onEliminarMedicamento = { viewModel.eliminarMedicamento(mcr.medicamento) },
                        onEliminarRecordatorio = { viewModel.eliminarRecordatorio(it) },
                        onEditar = {
                            viewModel.iniciarEdicion(mcr.medicamento, mcr.recordatorios)
                            editando = true
                            medicamentoEdit = mcr.medicamento
                            recordatoriosEdit = mcr.recordatorios
                        }
                    )
                }
            }
        }
    }
    if (showTimePicker) {
        val now = java.util.Calendar.getInstance()
        TimePickerDialog(
            horaInicial = now.get(java.util.Calendar.HOUR_OF_DAY),
            minutoInicial = now.get(java.util.Calendar.MINUTE),
            onConfirm = { h, m ->
                viewModel.agregarHorario(h, m)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

@Composable
fun MedicamentoItem(
    medicamento: MedicamentoEntity,
    recordatorios: List<RecordatorioEntity>,
    onEliminarMedicamento: () -> Unit,
    onEliminarRecordatorio: (RecordatorioEntity) -> Unit,
    onEditar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(medicamento.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar medicamento", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onEliminarMedicamento) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar medicamento", tint = MaterialTheme.colorScheme.error)
                }
            }
            if (medicamento.descripcion.isNotBlank()) {
                Text(medicamento.descripcion, style = MaterialTheme.typography.bodySmall)
            }
            if (recordatorios.isNotEmpty()) {
                Text("Recordatorios:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                recordatorios.forEach { rec ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(String.format(Locale.getDefault(), "%02d:%02d", rec.hora, rec.minuto), modifier = Modifier.weight(1f))
                        IconButton(onClick = { onEliminarRecordatorio(rec) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar recordatorio", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    horaInicial: Int,
    minutoInicial: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = horaInicial,
        initialMinute = minutoInicial,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar hora") },
        text = { TimePicker(state = timePickerState) },
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
