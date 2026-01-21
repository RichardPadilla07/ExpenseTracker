package com.example.expensetracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicamentoDao {
    @Query("SELECT * FROM medicamentos ORDER BY nombre ASC")
    fun obtenerTodosMedicamentos(): Flow<List<MedicamentoEntity>>

    @Insert
    suspend fun insertarMedicamento(medicamento: MedicamentoEntity): Long

    @Update
    suspend fun actualizarMedicamento(medicamento: MedicamentoEntity)

    @Delete
    suspend fun eliminarMedicamento(medicamento: MedicamentoEntity)

    // Recordatorios
    @Query("SELECT * FROM recordatorios WHERE medicamentoId = :medicamentoId")
    fun obtenerRecordatoriosDeMedicamento(medicamentoId: Int): Flow<List<RecordatorioEntity>>

    @Insert
    suspend fun insertarRecordatorio(recordatorio: RecordatorioEntity): Long

    @Update
    suspend fun actualizarRecordatorio(recordatorio: RecordatorioEntity)

    @Delete
    suspend fun eliminarRecordatorio(recordatorio: RecordatorioEntity)

    // Consulta para obtener medicamentos con sus recordatorios
    @Transaction
    @Query("SELECT * FROM medicamentos")
    fun obtenerMedicamentosConRecordatorios(): Flow<List<MedicamentoConRecordatorios>>
}

data class MedicamentoConRecordatorios(
    @Embedded val medicamento: MedicamentoEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "medicamentoId"
    )
    val recordatorios: List<RecordatorioEntity>
)
