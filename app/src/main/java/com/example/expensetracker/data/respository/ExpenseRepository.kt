package com.example.expensetracker.data.respository

import com.example.expensetracker.data.local.MedicamentoDao
import com.example.expensetracker.data.local.MedicamentoEntity
import com.example.expensetracker.data.local.RecordatorioEntity

class MedicamentoRepository(private val dao: MedicamentoDao) {
    fun obtenerMedicamentosConRecordatorios() = dao.obtenerMedicamentosConRecordatorios()
    suspend fun agregarMedicamento(medicamento: MedicamentoEntity) = dao.insertarMedicamento(medicamento)
    suspend fun actualizarMedicamento(medicamento: MedicamentoEntity) = dao.actualizarMedicamento(medicamento)
    suspend fun eliminarMedicamento(medicamento: MedicamentoEntity) = dao.eliminarMedicamento(medicamento)

    fun obtenerRecordatoriosDeMedicamento(medicamentoId: Int) = dao.obtenerRecordatoriosDeMedicamento(medicamentoId)
    suspend fun agregarRecordatorio(recordatorio: RecordatorioEntity) = dao.insertarRecordatorio(recordatorio)
    suspend fun actualizarRecordatorio(recordatorio: RecordatorioEntity) = dao.actualizarRecordatorio(recordatorio)
    suspend fun eliminarRecordatorio(recordatorio: RecordatorioEntity) = dao.eliminarRecordatorio(recordatorio)
}
