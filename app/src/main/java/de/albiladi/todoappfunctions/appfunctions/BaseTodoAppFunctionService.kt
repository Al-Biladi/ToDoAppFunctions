package de.albiladi.todoappfunctions.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import de.albiladi.todoappfunctions.data.TaskDatabase
import de.albiladi.todoappfunctions.data.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "TodoAppFunctionService",
    appFunctionXmlFileName = "todo_app_function_service",
)
abstract class BaseTodoAppFunctionService : AppFunctionService() {

    private val taskRepository by lazy {
        TaskRepository(
            TaskDatabase.getInstance(applicationContext).taskDao()
        )
    }

    /**
     * Creates a new task in the user's task list.
     *
     * @param title The required title of the task. It must not be empty.
     * @param date The optional due date in ISO format (YYYY-MM-DD).
     * @param time The optional due time in ISO format (HH:mm or HH:mm:ss).
     * @return The newly created task.
     * @throws AppFunctionInvalidArgumentException If an argument is invalid.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createTask(
        title: String,
        date: String? = null,
        time: String? = null,
    ): AppFunctionTask = withContext(Dispatchers.IO) {
        try {
            taskRepository.createTask(
                title = title,
                date = date,
                time = time,
            ).toAppFunctionTask()
        } catch (e: IllegalArgumentException) {
            throw AppFunctionInvalidArgumentException(
                e.message ?: "Invalid task data."
            )
        }
    }

    /**
     * Returns open tasks, optionally restricted to a date range.
     *
     * When no date is provided, all open tasks, including tasks without
     * a due date, are returned. When only a start date is provided, only
     * tasks due on that date are returned.
     *
     * @param fromDate The optional first date to include in ISO format (YYYY-MM-DD).
     * @param toDate The optional last date to include in ISO format (YYYY-MM-DD).
     * @return The open tasks matching the specified date range.
     * @throws AppFunctionInvalidArgumentException If the date range is invalid.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getOpenTasks(
        fromDate: String? = null,
        toDate: String? = null,
    ): List<AppFunctionTask> = withContext(Dispatchers.IO) {
        if (toDate != null && fromDate == null) {
            throw AppFunctionInvalidArgumentException(
                "A start date is required when an end date is provided."
            )
        }

        try {
            val tasks = if (fromDate == null) {
                taskRepository.getAllOpenTasks()
            } else {
                taskRepository.getOpenTasks(
                    fromDate = fromDate,
                    toDate = toDate ?: fromDate,
                )
            }

            tasks.map { it.toAppFunctionTask() }
        } catch (e: IllegalArgumentException) {
            throw AppFunctionInvalidArgumentException(
                e.message ?: "Invalid date range."
            )
        }
    }

    /**
     * Changes the completion state of a task.
     *
     * Call getOpenTasks first to obtain the unique task ID. Do not
     * select a task when several tasks could match the user's request.
     *
     * @param taskId The unique identifier obtained from getOpenTasks.
     * @param completed Whether the task should be marked as completed.
     * @return The updated task.
     * @throws AppFunctionElementNotFoundException If no task with the
     * specified ID exists.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun setTaskCompleted(
        taskId: Long,
        completed: Boolean = true,
    ): AppFunctionTask = withContext(Dispatchers.IO) {
        val updatedTask = taskRepository.setCompleted(
            taskId = taskId,
            completed = completed,
        ) ?: throw AppFunctionElementNotFoundException(
            "No task exists with ID $taskId."
        )

        updatedTask.toAppFunctionTask()
    }
}