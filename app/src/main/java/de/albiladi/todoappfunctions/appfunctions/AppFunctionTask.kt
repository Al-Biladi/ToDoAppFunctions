package de.albiladi.todoappfunctions.appfunctions

import androidx.appfunctions.AppFunctionSerializable
import de.albiladi.todoappfunctions.data.Task

/**
 * A task exposed through ApPFunctions.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AppFunctionTask(
    /** The unique identifier of the task. */
    val id: Long,
    /** The title of the task. */
    val title: String,
    /** The due date in ISO 8601 format (YYYY-MM-DD), or null if none is set. */
    val date: String? = null,
    /** The due time in 24-hour format (HH:mm), or null if none is set. */
    val time: String? = null,
    /** Whether the task has been completed. */
    val completed: Boolean,
)

internal fun Task.toAppFunctionTask(): AppFunctionTask {
    return AppFunctionTask(
        id = id,
        title = title,
        date = date,
        time = time,
        completed = isCompleted,
    )
}