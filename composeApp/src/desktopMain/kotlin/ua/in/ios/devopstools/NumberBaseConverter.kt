
package ua.`in`.ios.devopstools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberBaseConverter(modifier: Modifier = Modifier) {
    var inputNumber by remember { mutableStateOf("") }
    var inputBase by remember { mutableStateOf(10) }
    var errorMessage by remember { mutableStateOf("") }
    var conversionResults by remember { mutableStateOf<Map<Int, String>?>(null) }
    var decimalValue by remember { mutableStateOf<Long?>(null) }

    val bases = listOf(2, 8, 10, 16, 32)
    val baseNames = mapOf(
        2 to "Binary",
        8 to "Octal",
        10 to "Decimal",
        16 to "Hexadecimal",
        32 to "Base32"
    )

    // Perform conversion when inputNumber or inputBase changes
    LaunchedEffect(inputNumber, inputBase) {
        if (inputNumber.isNotEmpty()) {
            try {
                val decimal = convertToDecimal(inputNumber, inputBase)
                decimalValue = decimal

                val results = bases.associateWith { base ->
                    convertFromDecimal(decimal, base)
                }
                conversionResults = results
                errorMessage = ""
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
                conversionResults = null
                decimalValue = null
            }
        } else {
            conversionResults = null
            decimalValue = null
            errorMessage = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Number Base Converter",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Number input field
        OutlinedTextField(
            value = inputNumber,
            onValueChange = { inputNumber = it },
            label = { Text("Enter number") },
            placeholder = { Text("255") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Input base selection
        Text(
            text = "Input number base:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            items(bases.size) { index ->
                val base = bases[index]
                FilterChip(
                    onClick = { inputBase = base },
                    label = { Text("Base $base") },
                    selected = inputBase == base
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error display
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

        // Conversion results
        conversionResults?.let { results ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Conversion Results",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    SelectionContainer {
                        Column {
                            bases.forEach { base ->
                                val convertedValue = results[base] ?: ""
                                val baseName = baseNames[base] ?: "Base $base"

                                NumberBaseRow(
                                    label = baseName,
                                    value = convertedValue,
                                    isSelected = base == inputBase
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            // Additional information
//                            decimalValue?.let { decimal ->
//                                Text(
//                                    text = "Decimal value: $decimal",
//                                    style = MaterialTheme.typography.bodyMedium,
//                                    fontFamily = FontFamily.Monospace
//                                )
//                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Reference information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Reference",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = buildString {
                        append("• Binary (2): only digits 0, 1\n")
                        append("• Octal (8): digits 0-7\n")
                        append("• Decimal (10): digits 0-9\n")
                        append("• Hexadecimal (16): digits 0-9, A-F\n")
                        append("• Base32 (32): digits 0-9, A-V")
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun NumberBaseRow(label: String, value: String, isSelected: Boolean) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun convertToDecimal(number: String, fromBase: Int): Long {
    if (number.isBlank()) throw IllegalArgumentException("Number cannot be empty")

    return try {
        when (fromBase) {
            2 -> {
                if (!number.all { it in '0'..'1' }) {
                    throw IllegalArgumentException("Binary base allows only digits 0 and 1")
                }
                number.toLong(2)
            }
            8 -> {
                if (!number.all { it in '0'..'7' }) {
                    throw IllegalArgumentException("Octal base allows only digits 0-7")
                }
                number.toLong(8)
            }
            10 -> {
                if (!number.all { it.isDigit() }) {
                    throw IllegalArgumentException("Decimal base allows only digits 0-9")
                }
                number.toLong(10)
            }
            16 -> {
                if (!number.all { it.isDigit() || it.uppercaseChar() in 'A'..'F' }) {
                    throw IllegalArgumentException("Hexadecimal base allows digits 0-9 and letters A-F")
                }
                number.toLong(16)
            }
            32 -> {
                if (!number.all { it.isDigit() || it.uppercaseChar() in 'A'..'V' }) {
                    throw IllegalArgumentException("Base32 allows digits 0-9 and letters A-V")
                }
                number.toLong(32)
            }
            else -> throw IllegalArgumentException("Unsupported number base: $fromBase")
        }
    } catch (e: NumberFormatException) {
        throw IllegalArgumentException("Invalid number format for base $fromBase")
    }
}

private fun convertFromDecimal(decimal: Long, toBase: Int): String {
    return when (toBase) {
        2 -> decimal.toString(2)
        8 -> decimal.toString(8)
        10 -> decimal.toString(10)
        16 -> decimal.toString(16).uppercase()
        32 -> decimal.toString(32).uppercase()
        else -> throw IllegalArgumentException("Unsupported number base: $toBase")
    }
}