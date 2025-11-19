package ua.`in`.ios.devopstools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.Base64

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Base64Tool(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(0) }
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    fun performEncode() {
        try {
            if (inputText.isBlank()) {
                errorMessage = "Please enter the text for encoding"
                outputText = ""
                return
            }

            val encoded = Base64.getEncoder().encodeToString(inputText.toByteArray(Charsets.UTF_8))
            outputText = encoded
            errorMessage = ""
        } catch (e: Exception) {
            errorMessage = "Error when encoding: ${e.message}"
            outputText = ""
        }
    }

    fun performDecode() {
        try {
            if (inputText.isBlank()) {
                errorMessage = "Please enter Base64 text for decoding"
                outputText = ""
                return
            }

            // Очищуємо від можливих пробілів та переносів рядків
            val cleanInput = inputText.trim().replace(Regex("\\s+"), "")
            val decoded = Base64.getDecoder().decode(cleanInput)
            outputText = String(decoded, Charsets.UTF_8)
            errorMessage = ""
        } catch (e: Exception) {
            errorMessage = "Error when decoding: ${e.message}. Make sure the text entered is valid Base64."
            outputText = ""
        }
    }

    fun clearAll() {
        inputText = ""
        outputText = ""
        errorMessage = ""
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Base64 Encoder/Decoder",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Вкладки для перемикання між кодуванням та декодуванням
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    selectedTab = 0
                    clearAll()
                },
                text = { Text("Encode") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    clearAll()
                },
                text = { Text("Decode") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Поле для введення
            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    errorMessage = ""
                },
                label = {
                    Text(if (selectedTab == 0) "Text for encoding" else "Base64 for decoding")
                },
                placeholder = {
                    Text(if (selectedTab == 0) "Enter the usual text..." else "Enter BASE64 text...")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                maxLines = 6
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопки управління
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (selectedTab == 0) {
                                performEncode()
                            } else {
                                performDecode()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (selectedTab == 0) "Encode to Base64" else "Decode from Base64")
                }

                OutlinedButton(
                    onClick = { clearAll() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Відображення помилок
            if (errorMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Поле для результату
            if (outputText.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedTab == 0) "Encoded Base64:" else "Decoded text:",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Row {
                                TextButton(
                                    onClick = {
                                        // Копіюємо результат назад в поле введення для зворотної операції
                                        inputText = outputText
                                        selectedTab = if (selectedTab == 0) 1 else 0
                                        outputText = ""
                                    }
                                ) {
                                    Text("Reverse operation")
                                }

                                TextButton(
                                    onClick = { outputText = "" }
                                ) {
                                    Text("Clean")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        SelectionContainer {
                            Text(
                                text = outputText,
                                fontFamily = if (selectedTab == 0) FontFamily.Monospace else FontFamily.Default,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Інформація про довжину
                        Text(
                            text = buildString {
                                if (selectedTab == 0) {
                                    append("Length of original: ${inputText.length} symbols\n")
                                    append("Length of Base64: ${outputText.length} symbols")
                                } else {
                                    append("Length of Base64: ${inputText.length} symbols\n")
                                    append("Length of decoded: ${outputText.length} symbols")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}