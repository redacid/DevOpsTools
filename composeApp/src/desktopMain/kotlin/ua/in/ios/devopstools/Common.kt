package ua.`in`.ios.devopstools

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Універсальне діалогове вікно підтвердження дії.
 *
 * @param isOpen Стан, чи відкрито діалогове вікно
 * @param onDismissRequest Функція, яка викликається при закритті вікна без підтвердження
 * @param onConfirm Функція, яка викликається при підтвердженні дії
 * @param title Заголовок діалогового вікна
 * @param text Основний текст діалогового вікна
 * @param confirmButtonText Текст кнопки підтвердження
 * @param dismissButtonText Текст кнопки відміни
 */
@Composable
fun ConfirmationDialog(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    text: String,
    confirmButtonText: String = "Confirm",
    dismissButtonText: String = "Cancel"
) {
    if (isOpen) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(title) },
            text = { Text(text) },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        onDismissRequest()
                    }
                ) {
                    Text(confirmButtonText)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissRequest) {
                    Text(dismissButtonText)
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        )
    }
}

/**
 * Діалогове вікно для вибору стратегії завантаження завдань.
 */
@Composable
fun TaskLoadStrategyDialog(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    onStrategySelected: (TasksManager.LoadStrategy) -> Unit
) {
    if (isOpen) {
        var selectedStrategy by remember { mutableStateOf(TasksManager.LoadStrategy.REPLACE_ALL) }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text("Choosing a download strategy") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Choose a way to download tasks:", modifier = Modifier.padding(bottom = 16.dp))

                    // Радіо-кнопки для вибору стратегії
                    Column {
                        TasksManager.LoadStrategy.values().forEach { strategy ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedStrategy == strategy,
                                    onClick = { selectedStrategy = strategy }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (strategy) {
                                        TasksManager.LoadStrategy.REPLACE_ALL -> "Download and replace all"
                                        TasksManager.LoadStrategy.ADD_TO_EXISTING -> "Download and add to current"
                                        TasksManager.LoadStrategy.ADD_MISSING -> "Download and add only missing"
                                        TasksManager.LoadStrategy.UPDATE_AND_ADD -> "Update existing and add new"
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onStrategySelected(selectedStrategy)
                        onDismissRequest()
                    }
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        )
    }
}
