package de.albiladi.todoappfunctions.data


import de.albiladi.todoappfunctions.data.Task
import de.albiladi.todoappfunctions.data.TaskDao

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class TaskRepository(
    private val taskDao: TaskDao
) {

    fun observeAll(): Flow<List<Task>> {
        return taskDao.observeAll()
    }

    fun observeOpenTasks(
        fromDate: String,
        toDate: String
    ): Flow<List<Task>> {
        val range = validateRange(fromDate, toDate)

        return taskDao.observeForPeriod(
            fromDate = range.first,
            toDate = range.second,
            completed = false
        )
    }

    fun observeCompletedTasks(
        fromDate: String,
        toDate: String
    ): Flow<List<Task>> {
        val range = validateRange(fromDate, toDate)

        return taskDao.observeForPeriod(
            fromDate = range.first,
            toDate = range.second,
            completed = true
        )
    }

    suspend fun getOpenTasks(
        fromDate: String,
        toDate: String
    ): List<Task> {
        return observeOpenTasks(fromDate, toDate).first()
    }

    suspend fun getCompletedTasks(
        fromDate: String,
        toDate: String
    ): List<Task> {
        return observeCompletedTasks(fromDate, toDate).first()
    }

    suspend fun getTask(taskId: Long): Task? {
        return taskDao.getById(taskId)
    }

    suspend fun createTask(
        title: String,
        date: String? = null,
        time: String? = null
    ): Task {
        val normalizedTitle = title.trim()

        require(normalizedTitle.isNotEmpty()) {
            "Der Titel darf nicht leer sein."
        }

        val task = Task(
            title = normalizedTitle,
            date = normalizeDate(date),
            time = normalizeTime(time)
        )

        val taskId = taskDao.insert(task)

        return task.copy(id = taskId)
    }

    suspend fun updateTask(task: Task) {
        val normalizedTitle = task.title.trim()

        require(normalizedTitle.isNotEmpty()) {
            "Der Titel darf nicht leer sein."
        }

        taskDao.update(
            task.copy(
                title = normalizedTitle,
                date = normalizeDate(task.date),
                time = normalizeTime(task.time)
            )
        )
    }

    suspend fun deleteTask(task: Task) {
        taskDao.delete(task)
    }

    suspend fun setCompleted(
        taskId: Long,
        completed: Boolean
    ): Task? {
        val changedRows = taskDao.setCompleted(
            taskId = taskId,
            completed = completed
        )

        if (changedRows == 0) {
            return null
        }

        return taskDao.getById(taskId)
    }

    private fun normalizeDate(date: String?): String? {
        if (date.isNullOrBlank()) {
            return null
        }

        return LocalDate.parse(date).toString()
    }

    private fun normalizeTime(time: String?): String? {
        if (time.isNullOrBlank()) {
            return null
        }

        return LocalTime.parse(time).format(TIME_FORMAT)
    }

    private fun validateRange(
        fromDate: String,
        toDate: String
    ): Pair<String, String> {
        val from = LocalDate.parse(fromDate)
        val to = LocalDate.parse(toDate)

        require(!to.isBefore(from)) {
            "Das Enddatum darf nicht vor dem Startdatum liegen."
        }

        return from.toString() to to.toString()
    }

    companion object {
        private val TIME_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("HH:mm")
    }
}