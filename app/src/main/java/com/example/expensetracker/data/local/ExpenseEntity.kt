package com.example.expensetracker.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity (tableName = "gastos")
data class ExpenseEntity(

    // la clave primaria identifica cada registro de forma unica
    // autoGenerate = true hace que room asigne el ID automaticamente
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // monto del gasto
    val monto: Double,

    // descripcion
    val descripcion: String,

    // categoria que ayuda a organizar: comida, transporte, entretenimiento, etc
    val categoria: String,

    // GUardamos la fecha como timestamp en milisegundos
    // Es mas facil de manejar y comparar fechas asi
    val fecha: Long = System.currentTimeMillis()
)