package ua.`in`.ios.devopstools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject

@Composable
fun TasksTable() {
    val tasksManager = TasksManager.getInstance()
    val tasksArray = tasksManager.getTasksArray()
    var tasks by remember { mutableStateOf(emptyList<JsonObject>()) }

    // Завантажуємо завдання при першому відображенні компонента
    LaunchedEffect(tasksArray) {
        tasks = buildList {
            if (tasksArray != null) {
                for (i in 0 until tasksArray.size()) {
                    add(tasksArray.get(i).asJsonObject)
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Заголовок
        Text(
            "Список завдань",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Заголовки таблиці
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Назва",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Опис",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(2f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Тип встановлення",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Статус",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(0.5f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(48.dp)) // Місце для кнопок дій
        }

        // Список завдань
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Завдання відсутні або завантажуються...")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tasks) { task ->
                    TaskRow(task = task, onDeleteClick = {
                        // Видаляємо завдання та оновлюємо список
                        val taskName = task.get("name")?.asString ?: return@TaskRow
                        tasksManager.removeTask(taskName)

                        // Оновлюємо список завдань
                        val updatedTasksArray = tasksManager.getTasksArray()
                        tasks = buildList {
                            if (updatedTasksArray != null) {
                                for (i in 0 until updatedTasksArray.size()) {
                                    add(updatedTasksArray.get(i).asJsonObject)
                                }
                            }
                        }
                    })
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                }
            }
        }
    }
}

@Composable
fun TaskRow(task: JsonObject, onDeleteClick: () -> Unit) {
    val tasksManager = TasksManager.getInstance()
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Назва
        Text(
            text = task.get("name")?.asString ?: "Невідома назва",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // Опис
        Text(
            text = task.get("description")?.asString ?: "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(2f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Тип установлення
        Text(
            text = task.get("install_type")?.asString ?: "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // Статус (enabled) як чекбокс
        var enabled by remember { mutableStateOf(task.get("enabled")?.asBoolean ?: false) }

        Checkbox(
            checked = enabled,
            onCheckedChange = { isChecked ->
                enabled = isChecked

                // Оновлюємо значення в об'єкті завдання
                task.addProperty("enabled", isChecked)

                // Зберігаємо зміни
                val taskName = task.get("name")?.asString ?: return@Checkbox
                tasksManager.updateTask(taskName, task)
            },
            modifier = Modifier.weight(0.5f)
        )

        // Кнопки дій
        IconButton(
            onClick = { showDeleteConfirmation = true }
        ) {
            Icon(ICON_DELETE, contentDescription = "Видалити")
        }
        val taskName = task.get("name")?.asString ?: "Невідоме завдання"
        ConfirmationDialog(
            isOpen = showDeleteConfirmation,
            onDismissRequest = { showDeleteConfirmation = false },
            onConfirm = onDeleteClick,
            title = "Підтвердження видалення",
            text = "Ви дійсно бажаєте видалити завдання \"$taskName\"?",
            confirmButtonText = "Видалити",
            dismissButtonText = "Скасувати"
        )

    }
}