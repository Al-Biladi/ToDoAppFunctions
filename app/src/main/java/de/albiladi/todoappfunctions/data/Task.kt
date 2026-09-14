package de.albiladi.todoappfunctions.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @property date Optionales Datum im ISO-Format yyyy-MM-dd.
 * @property time Optionale Uhrzeit im Format HH:mm.
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,

    val date: String? = null,

    val time: String? = null,

    val isCompleted: Boolean = false
)