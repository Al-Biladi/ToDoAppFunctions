package de.albiladi.todoappfunctions.appfunctions


import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import de.albiladi.todoappfunctions.data.Task
import de.albiladi.todoappfunctions.data.TaskDatabase
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "TodoAppFunctionService",
    appFunctionXmlFileName = "todo_app_function_service",
)
abstract class BaseTodoAppFunctionService : AppFunctionService() {

    private val taskDao by lazy {
        TaskDatabase.getInstance(applicationContext).taskDao()
    }

    /**
     * Creates a new task in the user's task list.
     *
     * @param title The required title of the task. It must not be empty.
     * @param date The optional due date of the task.
     * @param time The optional due time of the task.
     * @return The newly created task.
     * @throws AppFunctionInvalidArgumentException If the title is empty.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createTask(
        title: String,
        date: LocalDate? = null,
        time: LocalTime? = null,
    ): AppFunctionTask = withContext(Dispatchers.IO) {
        if (title.isBlank()) {
            throw AppFunctionInvalidArgumentException(
                "The task title must not be empty."
            )
        }

        val task = Task(
            title = title.trim(),
            date = date?.toString(),
            time = time?.toString(),
            isCompleted = false,
        )

        val taskId = taskDao.insert(task)

        val createdTask = taskDao.getById(taskId)
            ?: throw AppFunctionElementNotFoundException(
                "The newly created task with ID $taskId could not be loaded."
            )

        return@withContext createdTask.toAppFunctionTask()
    }

    /**
     * Returns open tasks, optionally restricted to a date range.
     *
     * When no date is provided, all open tasks, including tasks without
     * a due date, are returned. When only a start date is provided, only
     * tasks due on that date are returned.
     *
     * @param fromDate The optional first date to include.
     * @param toDate The optional last date to include.
     * @return The open tasks matching the specified date range.
     * @throws AppFunctionInvalidArgumentException If the date range is
     * invalid because the start date is missing or the end date is
     * before the start date.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getOpenTasks(
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
    ): List<AppFunctionTask> = withContext(Dispatchers.IO) {
        if (toDate != null && fromDate == null) {
            throw AppFunctionInvalidArgumentException(
                "A start date is required when an end date is provided."
            )
        }

        val tasks = if (fromDate == null) {
            taskDao.getAllOpenTasks()
        } else {
            val effectiveToDate = toDate ?: fromDate

            if (effectiveToDate.isBefore(fromDate)) {
                throw AppFunctionInvalidArgumentException(
                    "The end date must not be before the start date."
                )
            }

            taskDao.observeForPeriod(
                fromDate = fromDate.toString(),
                toDate = effectiveToDate.toString(),
                completed = false,
            ).first()
        }

        tasks.map(Task::toAppFunctionTask)
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
        val changedRows = taskDao.setCompleted(
            taskId = taskId,
            completed = completed,
        )

        if (changedRows == 0) {
            throw AppFunctionElementNotFoundException(
                "No task exists with ID $taskId."
            )
        }

        val updatedTask = taskDao.getById(taskId)
            ?: throw AppFunctionElementNotFoundException(
                "The updated task with ID $taskId could not be loaded."
            )

        return@withContext updatedTask.toAppFunctionTask()
    }
}