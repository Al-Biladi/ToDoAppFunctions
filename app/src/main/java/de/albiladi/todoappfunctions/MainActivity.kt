package de.albiladi.todoappfunctions

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.albiladi.todoappfunctions.data.Task
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TaskApp(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskApp(viewModel: TaskViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var editedTask by remember { mutableStateOf<Task?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showDayPicker by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<Task?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editedTask = null
                    showEditor = true
                }
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDayPicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        uiState.selectedDate?.format(DISPLAY_DATE_FORMAT)
                            ?: "Alle Aufgaben"
                    )
                }

                TextButton(onClick = viewModel::showAllDates) {
                    Text("Alle")
                }
            }

            TabRow(
                selectedTabIndex = if (uiState.selectedTab == TaskTab.OPEN) 0 else 1
            ) {
                Tab(
                    selected = uiState.selectedTab == TaskTab.OPEN,
                    onClick = { viewModel.selectTab(TaskTab.OPEN) },
                    text = { Text("Offen") }
                )
                Tab(
                    selected = uiState.selectedTab == TaskTab.COMPLETED,
                    onClick = { viewModel.selectTab(TaskTab.COMPLETED) },
                    text = { Text("Erledigt") }
                )
            }

            TaskList(
                uiState = uiState,
                onCompletedChange = viewModel::setCompleted,
                onEdit = {
                    editedTask = it
                    showEditor = true
                },
                onDeleteRequest = { deleteCandidate = it }
            )
        }
    }

    if (showDayPicker) {
        TaskDatePicker(
            initialDate = uiState.selectedDate ?: LocalDate.now(),
            onDismiss = { showDayPicker = false },
            onDateSelected = {
                viewModel.selectDate(it)
                showDayPicker = false
            }
        )
    }

    if (showEditor) {
        TaskEditorDialog(
            task = editedTask,
            defaultDate = if (editedTask == null) uiState.selectedDate else null,
            onDismiss = { showEditor = false },
            onConfirm = { title, date, time ->
                val task = editedTask
                if (task == null) {
                    viewModel.createTask(title, date, time)
                } else {
                    viewModel.updateTask(task, title, date, time)
                }
                showEditor = false
            }
        )
    }

    deleteCandidate?.let { task ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Aufgabe löschen?") },
            text = { Text("„${task.title}“ wird dauerhaft gelöscht.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask(task)
                        deleteCandidate = null
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun TaskList(
    uiState: TaskUiState,
    onCompletedChange: (Task, Boolean) -> Unit,
    onEdit: (Task) -> Unit,
    onDeleteRequest: (Task) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (uiState.visibleTasks.isEmpty()) {
            item {
                val status = if (uiState.selectedTab == TaskTab.OPEN) {
                    "offenen"
                } else {
                    "erledigten"
                }
                val scope = if (uiState.selectedDate == null) {
                    ""
                } else {
                    " für diesen Tag"
                }

                Text(
                    text = "Keine $status Aufgaben$scope vorhanden.",
                    modifier = Modifier.padding(24.dp)
                )
            }
        }

        items(uiState.visibleTasks, key = { it.id }) { task ->
            TaskCard(
                task = task,
                onCompletedChange = onCompletedChange,
                onEdit = onEdit,
                onDeleteRequest = onDeleteRequest,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item { Spacer(Modifier.padding(bottom = 48.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskCard(
    task: Task,
    onCompletedChange: (Task, Boolean) -> Unit,
    onEdit: (Task) -> Unit,
    onDeleteRequest: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val cardColor: Color = when {
        task.date == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        task.time != null -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f)
    }

    Box(modifier = modifier) {
        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = { menuExpanded = true }
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onCompletedChange(task, it) }
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium
                    )

                    task.date?.let { storedDate ->
                        val dateText = LocalDate.parse(storedDate)
                            .format(DISPLAY_DATE_FORMAT)
                        val timeText = task.time
                            ?.let(LocalTime::parse)
                            ?.format(TIME_FORMAT)

                        Text(
                            text = if (timeText == null) {
                                dateText
                            } else {
                                "$dateText · $timeText Uhr"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                TextButton(onClick = { onEdit(task) }) {
                    Text("✎")
                }
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Löschen") },
                onClick = {
                    menuExpanded = false
                    onDeleteRequest(task)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorDialog(
    task: Task?,
    defaultDate: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate?, LocalTime?) -> Unit
) {
    var title by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var date by remember(task?.id) {
        mutableStateOf(task?.date?.let(LocalDate::parse) ?: defaultDate)
    }
    var time by remember(task?.id) {
        mutableStateOf(task?.time?.let(LocalTime::parse))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (task == null) "Aufgabe hinzufügen" else "Aufgabe bearbeiten")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(date?.format(DISPLAY_DATE_FORMAT) ?: "Datum auswählen")
                }
                if (date != null) {
                    TextButton(
                        onClick = {
                            date = null
                            time = null
                        }
                    ) {
                        Text("Kein Datum")
                    }
                }

                OutlinedButton(
                    onClick = { showTimePicker = true },
                    enabled = date != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            date == null -> "Zuerst Datum auswählen"
                            time == null -> "Uhrzeit auswählen"
                            else -> time!!.format(TIME_FORMAT)
                        }
                    )
                }
                if (time != null) {
                    TextButton(onClick = { time = null }) {
                        Text("Keine Uhrzeit")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title.trim(), date, time) },
                enabled = title.isNotBlank()
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )

    if (showDatePicker) {
        TaskDatePicker(
            initialDate = date ?: LocalDate.now(),
            onDismiss = { showDatePicker = false },
            onDateSelected = {
                date = it
                showDatePicker = false
            }
        )
    }

    if (showTimePicker) {
        val initialTime = time ?: LocalTime.now()
        val pickerState = rememberTimePickerState(
            initialHour = initialTime.hour,
            initialMinute = initialTime.minute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Uhrzeit auswählen") },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        time = LocalTime.of(
                            pickerState.hour,
                            pickerState.minute
                        )
                        showTimePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDatePicker(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(
                            Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        )
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

private val DISPLAY_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.GERMANY)

private val TIME_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm")