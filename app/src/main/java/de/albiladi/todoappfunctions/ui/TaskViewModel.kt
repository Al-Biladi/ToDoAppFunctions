package de.albiladi.todoappfunctions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.albiladi.todoappfunctions.data.TaskRepository
import de.albiladi.todoappfunctions.data.Task
import de.albiladi.todoappfunctions.data.TaskDatabase
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskTab {
    OPEN,
    COMPLETED
}

data class TaskUiState(
    val selectedDate: LocalDate? = LocalDate.now(),
    val selectedTab: TaskTab = TaskTab.OPEN,
    val visibleTasks: List<Task> = emptyList()
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaskRepository(
        TaskDatabase.getInstance(application).taskDao()
    )

    private val selectedDate = MutableStateFlow<LocalDate?>(LocalDate.now())
    private val selectedTab = MutableStateFlow(TaskTab.OPEN)

    val uiState = combine(
        repository.observeAll(),
        selectedDate,
        selectedTab
    ) { tasks, date, tab ->
        val completed = tab == TaskTab.COMPLETED

        val visibleTasks = tasks
            .asSequence()
            .filter { it.isCompleted == completed }
            .filter { task ->
                date == null || task.date == null || task.date == date.toString()
            }
            .sortedWith(taskComparator)
            .toList()

        TaskUiState(
            selectedDate = date,
            selectedTab = tab,
            visibleTasks = visibleTasks
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TaskUiState()
    )

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun showAllDates() {
        selectedDate.value = null
    }

    fun selectTab(tab: TaskTab) {
        selectedTab.value = tab
    }

    fun createTask(
        title: String,
        date: LocalDate?,
        time: LocalTime?
    ) {
        if (title.isBlank()) return

        viewModelScope.launch {
            repository.createTask(
                title = title,
                date = date?.toString(),
                time = time.takeIf { date != null }?.toString()
            )
        }
    }

    fun updateTask(
        task: Task,
        title: String,
        date: LocalDate?,
        time: LocalTime?
    ) {
        if (title.isBlank()) return

        viewModelScope.launch {
            repository.updateTask(
                task.copy(
                    title = title,
                    date = date?.toString(),
                    time = time.takeIf { date != null }?.toString()
                )
            )
        }
    }

    fun setCompleted(task: Task, completed: Boolean) {
        viewModelScope.launch {
            repository.setCompleted(task.id, completed)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    private companion object {
        val taskComparator = compareBy<Task> { it.date == null }
            .thenBy { it.date }
            .thenBy { it.date != null && it.time == null }
            .thenBy { it.time }
            .thenBy { it.title.lowercase() }
    }
}