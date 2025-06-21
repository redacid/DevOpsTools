package ua.`in`.ios.devopstools

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.DialogProperties

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
    confirmButtonText: String = "Підтвердити",
    dismissButtonText: String = "Скасувати"
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