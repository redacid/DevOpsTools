package ua.`in`.ios.devopstools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch


//// Приклад використання
//val logger = Logger.getInstance()
//
//// Логування на різних рівнях
//logger.d("TasksManager", "Завантаження завдань...")
//logger.i("TasksManager", "Завдання успішно завантажені: ${tasks.size} елементів")
//logger.w("TasksManager", "Деякі завдання не мають обов'язкових полів")
//
//try {
//    // Якийсь код, що може викликати помилку
//} catch (e: Exception) {
//    logger.e("TasksManager", "Помилка при завантаженні завдань", e)
//}



@Composable
fun LogViewer(modifier: Modifier = Modifier) {
    val logger = Logger.getInstance()
    val logs by remember {
        derivedStateOf { logger.getLogs() }
    }

    var selectedLogLevel by remember { mutableStateOf(LogLevel.DEBUG) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var logsKey by remember { mutableStateOf(0) }
    LaunchedEffect(logs.size) {
        logsKey++
        if (logs.isNotEmpty()) {
            listState.scrollToItem(logs.size - 1)
        }
    }

    // Автоматичне прокручування до останнього запису
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.scrollToItem(logs.size - 1)
        }
    }

    Column(modifier = modifier) {
        // Панель фільтрів та кнопок
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Logs", style = MaterialTheme.typography.titleLarge)

            // Фільтр за рівнем логів
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Filter: ")
                LogLevel.values().forEach { level ->
                    FilterChip(
                        selected = selectedLogLevel == level,
                        onClick = { selectedLogLevel = level },
                        label = { Text(level.name) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Кнопки управління
            Row {
                Button(
                    onClick = {
                        logger.clearLogs()
                        logsKey++
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text("Clear")
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (logs.isNotEmpty()) {
                                listState.scrollToItem(logs.size - 1)
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text("Scroll to Bottom")
                }
            }
        }

        // Відображення логів
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            val filteredLogs = logs.filter { it.level.value >= selectedLogLevel.value }

            items(filteredLogs) { logEntry ->
                LogEntryRow(logEntry)
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
fun LogEntryRow(entry: LogEntry) {
    val logColor = when(entry.level) {
        LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        LogLevel.INFO -> MaterialTheme.colorScheme.primary
        LogLevel.WARNING -> MaterialTheme.colorScheme.tertiary
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
    }

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Основна інформація про запис
        SelectionContainer {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "[${entry.getFormattedTime()}]",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    entry.level.name,
                    color = logColor,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    entry.tag,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(120.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    entry.message,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        // Відображення стеку викликів, якщо є і якщо запис розгорнуто
        if (entry.throwable != null) {
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Hide Stack Trace" else "Show Stack Trace")
            }

            if (expanded) {
                Text(
                    entry.throwable.stackTraceToString(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                        .padding(8.dp)
                )
            }
        }
    }
}