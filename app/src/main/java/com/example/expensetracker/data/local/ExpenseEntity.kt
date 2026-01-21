package com.example.expensetracker.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "medicamentos")
data class MedicamentoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val descripcion: String = ""
)

@Entity(
    tableName = "recordatorios",
    foreignKeys = [
        ForeignKey(
            entity = MedicamentoEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicamentoId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RecordatorioEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val medicamentoId: Int,
    val hora: Int, // 0-23
    val minuto: Int, // 0-59
    val dias: String = "todos" // para futuras mejoras: días específicos
)